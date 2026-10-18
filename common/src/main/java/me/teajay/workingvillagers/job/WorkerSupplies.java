package me.teajay.workingvillagers.job;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.carrier.CarrierData;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.TorchBlock;
import org.jspecify.annotations.Nullable;

/**
 * What a worker carries for its work besides the material: its wage ({@link Config#DAILY_WAGE}
 * emeralds per work day) and its tools (the lumberjack's axe, the farmer's hoe, the pit
 * digger's shovel and pickaxe), plus what it uses up (saplings, seeds, ladders, torches). The player gives these to the worker directly, like food to
 * a villager: right-click the worker with them, or throw them to it. Or the worker takes
 * them from a {@link VillageChests village chest} nearby: before the first work of the day
 * whatever runs low, during the day only what it cannot work without. Without its wage for
 * the day, or without its tool, a worker does not work; it waits at its workstation and
 * shakes its head.
 *
 * <p>An instance belongs to one work behavior and runs the waiting; the static part decides
 * what a worker accepts.
 */
public final class WorkerSupplies {
	public static final Item WAGE = Items.EMERALD;
	/** At most this many emeralds a worker carries. */
	public static final int MAX_EMERALDS = 64;
	/** At most this many tools of its kind a worker carries (one in use, one spare). */
	private static final int MAX_TOOLS = 2;
	private static final float SPEED = 0.5F;
	/** How long the worker waits before it looks again. */
	private static final int RETRY_TICKS = 100;
	/** How long the worker tries to get to a village chest; they may be farther off than its own chests. */
	private static final int VILLAGE_CHEST_WALK_TICKS = 1200;

	/** Something the worker has to carry for its work, and the status while it has none. */
	public interface Need {
		Predicate<ItemStack> item();

		String missingKey();
	}

	/**
	 * A tool the worker needs, the status while it has none and its kind for the menus
	 * ({@code "axe"}, ...).
	 */
	public record Tool(String kind, Predicate<ItemStack> item, String missingKey) implements Need {
	}

	/**
	 * Something the worker uses up at work (saplings, seeds, ladders, ...): it keeps up to
	 * {@code keep} of each kind. {@code required} ones keep it from working while it has none
	 * (the work checks that itself, see {@link #busy}).
	 */
	public record Stock(String kind, Predicate<ItemStack> item, IntSupplier keep, String missingKey, boolean required) implements Need {
	}

	public static final Tool AXE = new Tool("axe", stack -> stack.is(ItemTags.AXES), "lumberjack.no_axe");
	public static final Tool HOE = new Tool("hoe", stack -> stack.is(ItemTags.HOES), "farmer.no_hoe");
	public static final Tool SHOVEL = new Tool("shovel", stack -> stack.is(ItemTags.SHOVELS), "pit_digger.no_shovel");
	public static final Tool PICKAXE = new Tool("pickaxe", stack -> stack.is(ItemTags.PICKAXES), "pit_digger.no_pickaxe");
	public static final Stock SAPLINGS = new Stock("saplings", stack -> stack.is(ItemTags.SAPLINGS), Config.KEEP_SAPLINGS::get, "", false);
	public static final Stock SEEDS = new Stock("seeds", FarmFields::isSeed, Config.KEEP_SEEDS::get, "farmer.no_seeds", false);
	public static final Stock LADDERS = new Stock("ladders", stack -> stack.is(Items.LADDER), Config.KEEP_LADDERS::get, "pit_digger.no_ladders", true);
	public static final Stock TORCHES = new Stock("torches", WorkerSupplies::isTorch, Config.KEEP_TORCHES::get, "", false);

	private int waitTicks;
	/** On the way to a village chest, null otherwise. */
	private @Nullable ChestTrip trip;
	/** The day the worker last looked for what runs low (see {@link VillageChests}). */
	private long checkedDay = -1;
	/** Village chests it could not get at or got nothing from, with the day; it tries them again the next day. */
	private final Map<BlockPos, Long> skipped = new HashMap<>();

	/**
	 * Takes the wage of the day before the next piece of work and checks the tools (and
	 * whatever else is given), fetching from a village chest what is missing or runs low.
	 * Returns true while that keeps the worker busy (on the way to a village chest, no wage,
	 * no tool), so the work has to wait; false when the worker may work.
	 */
	public boolean busy(ServerLevel level, Villager villager, Need... needs) {
		if (this.trip != null && this.fetching(level, villager)) {
			return true;
		}
		if (this.waitTicks > 0) {
			this.waitTicks--;
			return true;
		}
		int wage = Config.DAILY_WAGE.get();
		SimpleContainer inventory = villager.getInventory();
		if (wage > 0 && !isPaid(level, villager)) {
			if (count(inventory, WorkerSupplies::isWage) < wage) {
				if (!this.fetch(level, villager, WorkerSupplies::isWage)) {
					this.waitFor(level, villager, "no_wage");
				}
				return true;
			}
			inventory.removeItemType(WAGE, wage);
			((HiredVillager) villager).workingvillagers$setPaidDay(ChopTrees.today(level));
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getY() + 1.8, villager.getZ(), 5, 0.3, 0.2, 0.3, 0.0);
		}
		for (Need need : needs) {
			if (!inventory.hasAnyMatching(stack -> !stack.isEmpty() && need.item().test(stack))) {
				if (!this.fetch(level, villager, need.item())) {
					this.waitFor(level, villager, need.missingKey());
				}
				return true;
			}
		}
		return this.refillDue(level, villager) && this.fetch(level, villager, stack -> true);
	}

	/**
	 * Whether the worker wants to fetch what runs low before its first work of the day. Work
	 * that only calls {@link #busy} when something is missing asks this, too.
	 */
	public boolean refillDue(ServerLevel level, Villager villager) {
		if (this.trip != null) {
			return true;
		}
		long today = ChopTrees.today(level);
		if (this.checkedDay == today) {
			return false;
		}
		if (this.villageChest(level, villager, stack -> true) == null) {
			this.checkedDay = today;
			return false;
		}
		return true;
	}

	/** Breaks off the waiting and a walk to a village chest, e.g. when the work hours are over. */
	public void stop(Villager villager) {
		this.waitTicks = 0;
		if (this.trip != null) {
			this.trip.close(villager);
			this.trip = null;
		}
	}

	/**
	 * Sets off to the closest village chest in reach that has something the worker wants
	 * (something {@code wanted} matches). Returns false if there is none.
	 */
	private boolean fetch(ServerLevel level, Villager villager, Predicate<ItemStack> wanted) {
		BlockPos chest = this.villageChest(level, villager, wanted);
		if (chest == null) {
			return false;
		}
		this.trip = ChestTrip.to(level, villager, chest, VILLAGE_CHEST_WALK_TICKS);
		WorkerStatus.task(villager, "village_chest.fetch");
		return true;
	}

	private @Nullable BlockPos villageChest(ServerLevel level, Villager villager, Predicate<ItemStack> wanted) {
		long today = ChopTrees.today(level);
		this.skipped.values().removeIf(day -> day != today);
		if (!VillageChests.runsLow(villager)) {
			return null;
		}
		for (BlockPos chest : VillageChests.inReach(level, anchor(level, villager))) {
			Container container = this.skipped.containsKey(chest) ? null : Workstations.getContainer(level, chest);
			if (container != null && VillageChests.offers(villager, container, wanted)) {
				return chest;
			}
		}
		return null;
	}

	/** Walks to the village chest and takes what it wants. Returns true while it is on its way. */
	private boolean fetching(ServerLevel level, Villager villager) {
		ChestTrip trip = Objects.requireNonNull(this.trip);
		int[] taken = {0};
		ChestTrip.Result result = trip.tick(level, villager, container -> taken[0] = VillageChests.take(villager, container));
		if (result == ChestTrip.Result.RUNNING) {
			WorkerStatus.task(villager, "village_chest.fetch");
			return true;
		}
		this.trip = null;
		if (result == ChestTrip.Result.FAILED || taken[0] == 0) {
			this.skipped.put(trip.chest(), ChopTrees.today(level));
		} else {
			level.playSound(null, villager.getX(), villager.getY(), villager.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.3F, 1.2F);
		}
		// What runs low is fetched once a day; anything still missing waits for the next check
		this.checkedDay = ChopTrees.today(level);
		return false;
	}

	/** Waits a while next to the workstation and tells the menus why. */
	private void waitFor(ServerLevel level, Villager villager, String reason) {
		WorkerStatus.task(villager, reason);
		this.waitTicks = RETRY_TICKS;
		BlockPos workplace = anchor(level, villager);
		Optional<BlockPos> spot = StandSpots.find(level, workplace, villager);
		if (spot.isPresent() && !StandSpots.isAt(villager, spot.get())) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(spot.get(), SPEED, 0));
		} else if (spot.isEmpty() && !workplace.closerToCenterThan(villager.position(), 3.0)) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(workplace, SPEED, 2));
		}
	}

	/** Whether the worker has taken its wage for today. */
	public static boolean isPaid(ServerLevel level, Villager villager) {
		return ((HiredVillager) villager).workingvillagers$getPaidDay() >= ChopTrees.today(level);
	}

	public static boolean isWage(ItemStack stack) {
		return stack.is(WAGE);
	}

	/** The tools the worker needs for its job. */
	public static List<Tool> toolsOf(Villager villager) {
		Holder<VillagerProfession> profession = villager.getVillagerData().profession();
		if (profession.is(WorkingVillagers.LUMBERJACK_PROFESSION)) {
			return List.of(AXE);
		}
		if (profession.is(WorkingVillagers.FARMER_PROFESSION)) {
			return List.of(HOE);
		}
		if (profession.is(WorkingVillagers.PIT_DIGGER_PROFESSION)) {
			return List.of(SHOVEL, PICKAXE);
		}
		return List.of();
	}

	/** What the worker uses up at work (saplings, seeds, ladders, ...). */
	public static List<Stock> stocksOf(Villager villager) {
		Holder<VillagerProfession> profession = villager.getVillagerData().profession();
		if (profession.is(WorkingVillagers.LUMBERJACK_PROFESSION)) {
			return List.of(SAPLINGS);
		}
		if (profession.is(WorkingVillagers.FARMER_PROFESSION)) {
			return List.of(SEEDS);
		}
		if (profession.is(WorkingVillagers.PIT_DIGGER_PROFESSION)) {
			return Config.KEEP_TORCHES.get() > 0 ? List.of(LADDERS, TORCHES) : List.of(LADDERS);
		}
		return List.of();
	}

	/** A torch that stands on the ground (torch, soul torch, ...), not a redstone torch. */
	public static boolean isTorch(ItemStack stack) {
		return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof TorchBlock;
	}

	/**
	 * How many of the stack the worker takes: emeralds up to {@link #MAX_EMERALDS}, each tool
	 * of its job up to two, and what it uses up (saplings, seeds, ladders) up to the amount it
	 * keeps of each kind. 0 if it does not want it or has no room.
	 */
	public static int accepts(Villager villager, ItemStack stack) {
		if (stack.isEmpty() || !((HiredVillager) villager).workingvillagers$isHired() || villager.isBaby()) {
			return 0;
		}
		SimpleContainer inventory = villager.getInventory();
		int wanted;
		Optional<Tool> tool = toolsOf(villager).stream().filter(candidate -> candidate.item().test(stack)).findFirst();
		Optional<Stock> stock = stocksOf(villager).stream().filter(candidate -> candidate.item().test(stack)).findFirst();
		if (isWage(stack)) {
			wanted = MAX_EMERALDS - count(inventory, WorkerSupplies::isWage);
		} else if (tool.isPresent()) {
			wanted = MAX_TOOLS - count(inventory, tool.get().item());
		} else if (stock.isPresent()) {
			wanted = stock.get().keep().getAsInt() - count(inventory, other -> other.is(stack.getItem()));
		} else {
			return 0;
		}
		int amount = Math.min(stack.getCount(), Math.max(0, wanted));
		return amount > 0 && inventory.canAddItem(stack.copyWithCount(amount)) ? amount : 0;
	}

	/** Whether the item is something the worker carries for its work: wage, tool or what it uses up. */
	public static boolean isSupply(Villager villager, ItemStack stack) {
		if (!((HiredVillager) villager).workingvillagers$isHired()) {
			return false;
		}
		return isWage(stack)
			|| toolsOf(villager).stream().anyMatch(tool -> tool.item().test(stack))
			|| stocksOf(villager).stream().anyMatch(stock -> stock.item().test(stack));
	}

	/**
	 * Picks up as much of a thrown stack as the worker takes ({@link #accepts}) and leaves the
	 * rest on the ground. Vanilla would take the whole stack. Returns false if the item is no
	 * supply of the worker, so vanilla picks it up as usual.
	 */
	public static boolean pickUp(Villager villager, ItemEntity entity) {
		ItemStack stack = entity.getItem();
		if (!isSupply(villager, stack)) {
			return false;
		}
		int amount = accepts(villager, stack);
		if (amount <= 0) {
			return true;
		}
		villager.onItemPickup(entity);
		ItemStack rest = villager.getInventory().addItem(stack.split(amount));
		villager.take(entity, amount - rest.getCount());
		stack.grow(rest.getCount());
		if (stack.isEmpty()) {
			entity.discard();
		} else {
			entity.setItem(stack);
		}
		return true;
	}

	/**
	 * The player gives the worker what it holds (right-click). Returns null if the worker
	 * has no use for the item, so the click does what it does otherwise.
	 */
	public static @Nullable InteractionResult give(Villager villager, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!isSupply(villager, stack)) {
			return null;
		}
		if (!(villager.level() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		int amount = accepts(villager, stack);
		if (amount <= 0) {
			// Has enough of it
			villager.setUnhappyCounter(40);
			villager.makeSound(SoundEvents.VILLAGER_NO);
			return InteractionResult.SUCCESS_SERVER;
		}
		ItemStack given = stack.copyWithCount(amount);
		if (!player.hasInfiniteMaterials()) {
			stack.shrink(amount);
		}
		ItemStack rest = villager.getInventory().addItem(given);
		if (!rest.isEmpty() && !player.getInventory().add(rest)) {
			villager.spawnAtLocation(level, rest);
		}
		villager.makeSound(SoundEvents.VILLAGER_YES);
		level.playSound(null, villager.getX(), villager.getY(), villager.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.3F, 1.2F);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getY() + 1.8, villager.getZ(), 5, 0.3, 0.2, 0.3, 0.0);
		return InteractionResult.SUCCESS_SERVER;
	}

	public static int count(SimpleContainer inventory, Predicate<ItemStack> filter) {
		int count = 0;
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && filter.test(stack)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	/** Where the worker works: its workstation, the source of its first route, or where it is. */
	static BlockPos anchor(ServerLevel level, Villager worker) {
		Optional<GlobalPos> jobSite = worker.getBrain().getMemory(MemoryModuleType.JOB_SITE);
		if (jobSite.isPresent() && jobSite.get().dimension() == level.dimension()) {
			return jobSite.get().pos();
		}
		CarrierData carrier = ((HiredVillager) worker).workingvillagers$getCarrier();
		if (carrier != null && !carrier.routes().isEmpty()) {
			return carrier.routes().getFirst().source();
		}
		return worker.blockPosition();
	}
}
