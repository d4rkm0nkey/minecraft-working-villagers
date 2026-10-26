package me.teajay.workingvillagers.job;

import java.util.ArrayList;
import java.util.Comparator;
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
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.enchantment.Repairable;
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
 * shakes its head. It takes the wage only with its first piece of work of the day
 * ({@link #earned}): a day it gets nothing done costs nothing.
 *
 * <p>The wage is kept in the {@link Workstations.Station#wage wage box} of the worker's
 * workstation: emeralds given to the worker, thrown to it or fetched from a village chest go
 * in there, and the worker takes the wage of the day from there, then from its pockets.
 * Emeralds it carried from before there were wage boxes it puts into the box (see
 * {@link #storeCarriedWage}); only what does not fit stays in its pockets. A carrier, which
 * has no workstation, carries its emeralds itself.
 *
 * <p>A tool that is about to break ({@link #isWorn}) counts as no tool: the worker stops
 * using it and, before its next piece of work, gives it back instead of breaking it: into a
 * village chest where a smith repairs it, or else an output chest of its workstation, or else
 * any village chest. Going to a village chest it takes a new tool at once.
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
	/** The end of the work hours of vanilla villagers (time of day); they start at 2000. */
	private static final long WORK_END = 9000L;
	/** From here on less than a third of the work hours is left, and a worker takes no wage that day. */
	private static final long FREE_FROM = WORK_END - (WORK_END - 2000L) / 3;
	private static final float SPEED = 0.5F;
	/** How long the worker waits before it looks again. */
	private static final int RETRY_TICKS = 100;
	/** How long the worker tries to get to a village chest; they may be farther off than its own chests. */
	private static final int VILLAGE_CHEST_WALK_TICKS = 1200;
	/** How long a worker leaves out a village chest that had nothing for it. */
	private static final int EMPTY_CHEST_RETRY_TICKS = 1200;
	/** How long a worker that found no food to buy waits before it looks again. */
	private static final int FOOD_RETRY_TICKS = 600;
	/** How long a worker that found no room for a worn tool waits before it looks again. */
	private static final int GIVE_BACK_RETRY_TICKS = 1200;

	/** Something the worker has to carry for its work, and the status while it has none. */
	public interface Need {
		Predicate<ItemStack> item();

		String missingKey();

		/** What counts as having it: for a tool, one that is not worn. */
		default Predicate<ItemStack> ready() {
			return this.item();
		}
	}

	/**
	 * A tool the worker needs, the status while it has none and its kind for the menus
	 * ({@code "axe"}, ...).
	 */
	public record Tool(String kind, Predicate<ItemStack> item, String missingKey) implements Need {
		@Override
		public Predicate<ItemStack> ready() {
			return stack -> this.item.test(stack) && !isWorn(stack);
		}
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
	/** The butcher's: a sword or an axe. */
	public static final Tool WEAPON = new Tool("weapon", stack -> stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES), "butcher.no_weapon");
	/** The guard's bow. */
	public static final Tool BOW = new Tool("bow", stack -> stack.is(Items.BOW), "guard.no_bow");
	public static final Tool ROD = new Tool("rod", stack -> stack.is(Items.FISHING_ROD), "fisher.no_rod");
	public static final Tool SHEARS = new Tool("shears", stack -> stack.is(Items.SHEARS), "shepherd.no_shears");
	public static final Stock SAPLINGS = new Stock("saplings", stack -> stack.is(ItemTags.SAPLINGS), Config.KEEP_SAPLINGS::get, "", false);
	public static final Stock SEEDS = new Stock("seeds", FarmFields::isSeed, Config.KEEP_SEEDS::get, "farmer.no_seeds", false);
	/** The shepherd's wheat for breeding; without it it still shears. */
	public static final Stock WHEAT = new Stock("wheat", stack -> stack.is(Items.WHEAT), Config.KEEP_WHEAT::get, "shepherd.no_wheat", false);
	/** The butcher's feed for breeding; without it it still slaughters. */
	public static final Stock FEED = new Stock("feed", TendAnimals::isFeed, Config.KEEP_FEED::get, "butcher.no_feed", false);
	/** Coal or charcoal for the smoker; without it the butcher hands in the raw meat. */
	public static final Stock FUEL = new Stock("fuel", stack -> stack.is(ItemTags.COALS), Config.KEEP_FUEL::get, "", false);
	/** The fisher's boat, to row out to open water; without it it fishes from the shore. */
	public static final Stock BOAT = new Stock("boat", stack -> stack.is(ItemTags.BOATS), () -> 1, "", false);
	/** The guard's arrows: without them it cannot do its work. */
	public static final Stock ARROWS = new Stock("arrows", stack -> stack.is(Items.ARROW), Config.KEEP_ARROWS::get, "guard.no_arrows", true);
	/** The glass bottles of the guard and the weaponsmith, to bottle experience. */
	public static final Stock BOTTLES = new Stock("bottles", stack -> stack.is(Items.GLASS_BOTTLE), () -> 16, "", false);
	/** The tanner's bucket, to fill its cauldron. */
	public static final Stock BUCKET = new Stock("bucket", stack -> stack.is(Items.BUCKET) || stack.is(Items.WATER_BUCKET), () -> 1, "", false);
	/** The pit digger's floors and pillars of a deep pit: without them it digs no deeper than the novice depth. */
	public static final Stock PIT_PLANKS = new Stock("planks", stack -> stack.is(ItemTags.PLANKS), Config.PIT_KEEP_PLANKS::get, "pit_digger.no_planks", true);
	public static final Stock PIT_LOGS = new Stock("logs", stack -> stack.is(ItemTags.LOGS), Config.PIT_KEEP_LOGS::get, "pit_digger.no_logs", true);
	public static final Stock LADDERS = new Stock("ladders", stack -> stack.is(Items.LADDER), Config.KEEP_LADDERS::get, "pit_digger.no_ladders", true);
	public static final Stock TORCHES = new Stock("torches", WorkerSupplies::isTorch, Config.KEEP_TORCHES::get, "", false);
	public static final Stock MINER_TORCHES = new Stock("torches", WorkerSupplies::isTorch, Config.MINER_KEEP_TORCHES::get, "", false);
	/** The miner's supports: fences as posts, planks as the beam above. */
	public static final Stock FENCES = new Stock("fences", stack -> stack.is(ItemTags.WOODEN_FENCES), Config.KEEP_FENCES::get, "", false);
	public static final Stock PLANKS = new Stock("planks", stack -> stack.is(ItemTags.PLANKS), Config.KEEP_PLANKS::get, "", false);
	/** Cobblestone the miner keeps of what it digs, to seal off water and lava and to close holes in the floor. */
	public static final Stock SEAL_BLOCKS = new Stock("seal", WorkerSupplies::isSealBlock, Config.KEEP_SEAL_BLOCKS::get, "", false);

	private int waitTicks;
	/** On the way to a village chest, or to an output chest with a worn tool; null otherwise. */
	private @Nullable ChestTrip trip;
	/** Whether the trip goes to a village chest (and the worker also takes from it). */
	private boolean toVillageChest;
	/** Until when (game time) a worker that found no room for a worn tool keeps it. */
	private long giveBackRetry;
	/** Until when (game time) a worker that found no food to buy does not look for it. */
	private long foodRetry;
	/** The day the worker last looked for what runs low (see {@link VillageChests}). */
	private long checkedDay = -1;
	/**
	 * Village chests it could not get at (until the next day) or got nothing from (for a
	 * minute: a carrier may fill it), with the game time until when it leaves them out.
	 */
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
		if (this.giveBackDue(level, villager) && this.giveBack(level, villager)) {
			return true;
		}
		SimpleContainer inventory = villager.getInventory();
		// The wage has to be there before the work; it is taken with the first piece of work (see earned)
		if (lacksWage(level, villager)) {
			if (!this.fetch(level, villager, WorkerSupplies::isWage)) {
				this.waitFor(level, villager, "no_wage");
			}
			return true;
		}
		for (Need need : needs) {
			if (!inventory.hasAnyMatching(stack -> !stack.isEmpty() && need.ready().test(stack))) {
				if (!this.fetch(level, villager, need.ready())) {
					this.waitFor(level, villager, need.missingKey());
				}
				return true;
			}
		}
		if (this.foodDue(level, villager)) {
			if (this.fetch(level, villager, Meals::isMeal)) {
				return true;
			}
			this.foodRetry = level.getGameTime() + FOOD_RETRY_TICKS;
		}
		return this.refillDue(level, villager) && this.fetch(level, villager, stack -> true);
	}

	/** Whether the worker wants to buy food and has not just failed to find any. */
	private boolean foodDue(ServerLevel level, Villager villager) {
		return level.getGameTime() >= this.foodRetry && Meals.wantsFood(villager);
	}

	/**
	 * Whether the worker wants to fetch what runs low before its first work of the day, or to
	 * give back a worn tool. Work that only calls {@link #busy} when something is missing asks
	 * this, too.
	 */
	public boolean refillDue(ServerLevel level, Villager villager) {
		if (this.trip != null || this.giveBackDue(level, villager) || this.foodDue(level, villager)) {
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
		this.toVillageChest = true;
		WorkerStatus.task(villager, "village_chest.fetch");
		return true;
	}

	/** Whether a village chest in reach has the worker's wage. */
	public boolean offersWage(ServerLevel level, Villager villager) {
		return this.villageChest(level, villager, WorkerSupplies::isWage) != null;
	}

	/** Whether the worker carries a worn tool to give back, and has not just failed to find room for it. */
	private boolean giveBackDue(ServerLevel level, Villager villager) {
		return level.getGameTime() >= this.giveBackRetry && villager.getInventory().hasAnyMatching(stack -> isToGiveBack(villager, stack));
	}

	/**
	 * Sets off with the worn tools to the closest village chest in reach with room where a
	 * smith repairs them, or else to the closest output chest of the workstation with room, so
	 * the player sees that the worker needs a new tool, or else to any village chest with room.
	 * Returns false if there is none: then the worker keeps them for a while.
	 */
	private boolean giveBack(ServerLevel level, Villager villager) {
		BlockPos anchor = anchor(level, villager);
		List<BlockPos> villageChests = VillageChests.inReach(level, anchor).stream()
			.filter(pos -> !this.skipped.containsKey(pos) && hasRoom(Workstations.getContainer(level, pos)))
			.toList();
		Optional<BlockPos> chest = villageChests.stream().filter(pos -> VillageChests.repairsWornTools(level, pos)).findFirst();
		this.toVillageChest = chest.isPresent();
		if (chest.isEmpty()) {
			chest = Workstations.find(level, anchor).stream()
				.flatMap(station -> station.validOutputs(level).stream())
				.filter(pos -> hasRoom(Workstations.getContainer(level, pos)))
				.min(Comparator.comparingDouble(pos -> pos.distToCenterSqr(villager.position())));
		}
		if (chest.isEmpty()) {
			chest = villageChests.stream().findFirst();
			this.toVillageChest = chest.isPresent();
		}
		if (chest.isEmpty()) {
			this.giveBackRetry = level.getGameTime() + GIVE_BACK_RETRY_TICKS;
			return false;
		}
		this.trip = ChestTrip.to(level, villager, chest.get(), VILLAGE_CHEST_WALK_TICKS);
		WorkerStatus.task(villager, "tool_return");
		return true;
	}

	private static boolean hasRoom(@Nullable Container container) {
		if (container == null) {
			return false;
		}
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			if (container.getItem(slot).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/** Puts the worn tools into the container, as many as fit. Returns how many. */
	private static int putWornTools(Villager villager, Container container) {
		SimpleContainer inventory = villager.getInventory();
		int moved = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (isToGiveBack(villager, stack)) {
				ItemStack rest = Chests.store(container, stack);
				moved += stack.getCount() - rest.getCount();
				inventory.setItem(slot, rest);
			}
		}
		if (moved > 0) {
			container.setChanged();
		}
		return moved;
	}

	private @Nullable BlockPos villageChest(ServerLevel level, Villager villager, Predicate<ItemStack> wanted) {
		long now = level.getGameTime();
		this.skipped.values().removeIf(until -> until <= now);
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

	/**
	 * Walks to the chest, puts its worn tools in and, at a village chest, takes what it wants.
	 * Returns true while it is on its way.
	 */
	private boolean fetching(ServerLevel level, Villager villager) {
		ChestTrip trip = Objects.requireNonNull(this.trip);
		boolean villageChest = this.toVillageChest;
		int[] moved = {0, 0};
		ChestTrip.Result result = trip.tick(level, villager, container -> {
			moved[0] = putWornTools(villager, container);
			moved[1] = villageChest ? VillageChests.take(villager, container) : 0;
		});
		if (result == ChestTrip.Result.RUNNING) {
			boolean worn = villager.getInventory().hasAnyMatching(stack -> isToGiveBack(villager, stack));
			WorkerStatus.task(villager, worn ? "tool_return" : "village_chest.fetch");
			return true;
		}
		this.trip = null;
		if (moved[0] + moved[1] > 0) {
			level.playSound(null, villager.getX(), villager.getY(), villager.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.3F, 1.2F);
		}
		if (villager.getInventory().hasAnyMatching(stack -> isToGiveBack(villager, stack))) {
			// No room after all, or the chest could not be reached: not again right away
			this.giveBackRetry = level.getGameTime() + GIVE_BACK_RETRY_TICKS;
		}
		if (villageChest) {
			if (result == ChestTrip.Result.FAILED) {
				this.skipped.put(trip.chest(), level.getGameTime() + 24000L - level.getOverworldClockTime() % 24000L);
			} else if (moved[0] + moved[1] == 0) {
				this.skipped.put(trip.chest(), level.getGameTime() + EMPTY_CHEST_RETRY_TICKS);
			}
			// What runs low is fetched once a day; anything still missing waits for the next check
			this.checkedDay = ChopTrees.today(level);
		}
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

	/**
	 * A villager was hired. The first time, late in the afternoon (less than a third of the
	 * work hours of vanilla villagers, 2000 to 9000, left), it works the rest of the day for
	 * free. Only the first time, so hiring again (a carrier gets its basket back) gives no
	 * free work.
	 */
	public static void onHired(ServerLevel level, Villager villager) {
		onHired(villager, ChopTrees.today(level), level.getOverworldClockTime() % 24000L);
	}

	/** {@link #onHired(ServerLevel, Villager)} at the given day and time of day, for tests. */
	public static void onHired(Villager villager, long today, long timeOfDay) {
		HiredVillager hired = (HiredVillager) villager;
		if (hired.workingvillagers$getHiredDay() >= 0) {
			return;
		}
		hired.workingvillagers$setHiredDay(today);
		if (timeOfDay >= FREE_FROM && timeOfDay < WORK_END) {
			hired.workingvillagers$setPaidDay(today);
		}
	}

	/**
	 * The worker did a piece of work (see {@link HiredVillager#workingvillagers$addWorkDone}):
	 * with the first one of the day it takes its wage, from the wage box first, then from its
	 * pockets. A worker that does nothing all day, for want of material say, costs nothing.
	 * {@link #busy} made sure the wage is there.
	 */
	public static void earned(ServerLevel level, Villager villager) {
		int wage = WorkerLevels.dailyWage(villager);
		if (wage <= 0 || isPaid(level, villager) || !((HiredVillager) villager).workingvillagers$isHired()) {
			return;
		}
		Workstations.Station box = wageBox(level, villager);
		int fromBox = box == null ? 0 : Math.min(box.wage(), wage);
		if (box != null) {
			box.setWage(box.wage() - fromBox);
		}
		villager.getInventory().removeItemType(WAGE, wage - fromBox);
		// What does not fit into the purse goes back
		ItemStack back = store(villager, new ItemStack(WAGE, Meals.paid(villager, wage)));
		if (!back.isEmpty()) {
			villager.getInventory().addItem(back);
		}
		((HiredVillager) villager).workingvillagers$setPaidDay(ChopTrees.today(level));
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getY() + 1.8, villager.getZ(), 5, 0.3, 0.2, 0.3, 0.0);
	}

	/** Whether the worker has not taken its wage for today and has not got it either. */
	public static boolean lacksWage(ServerLevel level, Villager villager) {
		int wage = WorkerLevels.dailyWage(villager);
		return wage > 0 && !isPaid(level, villager) && emeralds(level, villager) < wage;
	}

	/** Whether the worker has taken its wage for today. */
	public static boolean isPaid(ServerLevel level, Villager villager) {
		return ((HiredVillager) villager).workingvillagers$getPaidDay() >= ChopTrees.today(level);
	}

	public static boolean isWage(ItemStack stack) {
		return stack.is(WAGE);
	}

	/**
	 * The workstation whose wage box the worker uses; null for a carrier, an ordinary
	 * villager or a worker without a workstation.
	 */
	public static Workstations.@Nullable Station wageBox(ServerLevel level, Villager villager) {
		if (!((HiredVillager) villager).workingvillagers$isHired()) {
			return null;
		}
		Optional<GlobalPos> site = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
		if (site.isEmpty() || site.get().dimension() != level.dimension()) {
			return null;
		}
		return Workstations.find(level, site.get().pos()).orElse(null);
	}

	/**
	 * A worker saved by 0.3.0, before there were wage boxes, carried its wage itself: once it
	 * has a workstation it puts these emeralds into the wage box, as many as fit. Only once,
	 * so emeralds it carries for its work later (the takings of the shop assistant, the loot of
	 * a guard) stay where they are.
	 */
	public static void storeCarriedWage(ServerLevel level, Villager villager) {
		HiredVillager hired = (HiredVillager) villager;
		Workstations.Station box = hired.workingvillagers$isWageStored() ? null : wageBox(level, villager);
		if (box == null) {
			return;
		}
		int stored = box.addWage(count(villager.getInventory(), WorkerSupplies::isWage));
		if (stored > 0) {
			villager.getInventory().removeItemType(WAGE, stored);
		}
		hired.workingvillagers$setWageStored(true);
	}

	/** The emeralds the worker has for its wage: in the wage box of its workstation and in its pockets. */
	public static int emeralds(ServerLevel level, Villager villager) {
		Workstations.Station box = wageBox(level, villager);
		return count(villager.getInventory(), WorkerSupplies::isWage) + (box == null ? 0 : box.wage());
	}


	/**
	 * A tool about to break that is given back instead of used up: {@link Config#TOOL_RETURN_BELOW}
	 * percent of its durability left or less, at least 2 uses. Wooden and stone tools are used up
	 * unless {@link Config#TOOL_RETURN_CHEAP}, or they are enchanted or renamed.
	 */
	public static boolean isWorn(ItemStack stack) {
		int percent = Config.TOOL_RETURN_BELOW.get();
		if (percent <= 0 || stack.isEmpty() || !stack.isDamageableItem() || !isGivenBack(stack)) {
			return false;
		}
		int left = stack.getMaxDamage() - stack.getDamageValue();
		return left <= Math.max(2, stack.getMaxDamage() * percent / 100);
	}

	private static boolean isGivenBack(ItemStack tool) {
		if (Config.TOOL_RETURN_CHEAP.get() || tool.isEnchanted() || tool.has(DataComponents.CUSTOM_NAME)) {
			return true;
		}
		// Shears and fishing rods are cheap too, and the smith cannot repair them
		if (tool.is(Items.SHEARS) || tool.is(Items.FISHING_ROD)) {
			return false;
		}
		Repairable repairable = tool.get(DataComponents.REPAIRABLE);
		return repairable == null
			|| !repairable.isValidRepairItem(new ItemStack(Items.OAK_PLANKS)) && !repairable.isValidRepairItem(new ItemStack(Items.COBBLESTONE));
	}

	/** A worn tool of the worker's job, which it gives back. */
	public static boolean isToGiveBack(Villager villager, ItemStack stack) {
		return isWorn(stack) && (toolsOf(villager).stream().anyMatch(tool -> tool.item().test(stack)) || Uniforms.wearsArmor(villager) && Uniforms.isArmor(stack));
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
		if (profession.is(WorkingVillagers.SHEPHERD_PROFESSION)) {
			return List.of(SHEARS);
		}
		if (profession.is(WorkingVillagers.BUTCHER_PROFESSION)) {
			return List.of(WEAPON);
		}
		if (profession.is(WorkingVillagers.FISHER_PROFESSION)) {
			return List.of(ROD);
		}
		if (profession.is(WorkingVillagers.GUARD_PROFESSION)) {
			return List.of(BOW);
		}
		if (profession.is(WorkingVillagers.PIT_DIGGER_PROFESSION)) {
			return List.of(SHOVEL, PICKAXE);
		}
		if (profession.is(WorkingVillagers.MINER_PROFESSION)) {
			return List.of(PICKAXE);
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
		if (profession.is(WorkingVillagers.SHEPHERD_PROFESSION)) {
			return List.of(WHEAT);
		}
		if (profession.is(WorkingVillagers.BUTCHER_PROFESSION)) {
			return List.of(FEED, FUEL);
		}
		if (profession.is(WorkingVillagers.FISHER_PROFESSION)) {
			return List.of(BOAT);
		}
		if (profession.is(WorkingVillagers.GUARD_PROFESSION)) {
			return Config.GUARD_EXPERIENCE.get() ? List.of(ARROWS, BOTTLES) : List.of(ARROWS);
		}
		if (profession.is(WorkingVillagers.TANNER_PROFESSION)) {
			return List.of(BUCKET);
		}
		if (profession.is(WorkingVillagers.WEAPONSMITH_PROFESSION)) {
			return List.of(BOTTLES);
		}
		if (profession.is(WorkingVillagers.PIT_DIGGER_PROFESSION)) {
			List<Stock> stocks = new ArrayList<>(List.of(LADDERS));
			if (Config.KEEP_TORCHES.get() > 0) {
				stocks.add(TORCHES);
			}
			// Only while its pit has storeys: its pockets are small
			if (DigPit.hasStoreys(villager)) {
				stocks.add(PIT_PLANKS);
				stocks.add(PIT_LOGS);
			}
			return List.copyOf(stocks);
		}
		if (profession.is(WorkingVillagers.MINER_PROFESSION)) {
			List<Stock> stocks = new ArrayList<>(List.of(SEAL_BLOCKS));
			if (Config.MINER_KEEP_TORCHES.get() > 0) {
				stocks.add(MINER_TORCHES);
			}
			if (Config.TUNNEL_SUPPORTS.get() && Config.KEEP_FENCES.get() > 0 && Config.KEEP_PLANKS.get() > 0) {
				stocks.add(FENCES);
				stocks.add(PLANKS);
			}
			return List.copyOf(stocks);
		}
		return List.of();
	}

	/** Cobblestone or cobbled deepslate, what the miner seals and closes holes with. */
	public static boolean isSealBlock(ItemStack stack) {
		return stack.is(Items.COBBLESTONE) || stack.is(Items.COBBLED_DEEPSLATE);
	}

	/** A torch that stands on the ground (torch, soul torch, ...), not a redstone torch. */
	public static boolean isTorch(ItemStack stack) {
		return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof TorchBlock;
	}

	/**
	 * How many of the stack the worker takes: emeralds up to {@link #MAX_EMERALDS}, each tool
	 * of its job up to two (no worn one), and what it uses up (saplings, seeds, ladders) up to the amount it
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
		Workstations.Station box = villager.level() instanceof ServerLevel level ? wageBox(level, villager) : null;
		if (isWage(stack) && box != null) {
			// Straight into the wage box, never into the pockets
			return Math.min(stack.getCount(), Workstations.MAX_WAGE - box.wage());
		} else if (isWage(stack)) {
			wanted = MAX_EMERALDS - count(inventory, WorkerSupplies::isWage);
		} else if (Uniforms.wearsArmor(villager) && Uniforms.isArmor(stack)) {
			wanted = !WorkerSupplies.isWorn(stack) && Uniforms.lacks(villager, Uniforms.slotOf(stack)) ? 1 : 0;
		} else if (tool.isPresent()) {
			wanted = isWorn(stack) ? 0 : MAX_TOOLS - count(inventory, tool.get().ready());
		} else if (stock.isPresent()) {
			wanted = stock.get().keep().getAsInt() - count(inventory, other -> other.is(stack.getItem()));
		} else if (Meals.isBagged(villager, stack)) {
			// Into the lunch bag, as much as fits
			return Math.min(stack.getCount(), Meals.room(villager, stack));
		} else {
			return 0;
		}
		int amount = Math.min(stack.getCount(), Math.max(0, wanted));
		return amount > 0 && inventory.canAddItem(stack.copyWithCount(amount)) ? amount : 0;
	}

	/** Whether the item is something the worker carries for its work: wage, tool, what it uses up or food. */
	public static boolean isSupply(Villager villager, ItemStack stack) {
		if (!((HiredVillager) villager).workingvillagers$isHired()) {
			return false;
		}
		return isWage(stack)
			|| toolsOf(villager).stream().anyMatch(tool -> tool.item().test(stack))
			|| stocksOf(villager).stream().anyMatch(stock -> stock.item().test(stack))
			|| Uniforms.wearsArmor(villager) && Uniforms.isArmor(stack)
			|| Meals.isBagged(villager, stack);
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
		ItemStack rest = store(villager, stack.split(amount));
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
		ItemStack rest = store(villager, given);
		if (!rest.isEmpty() && !player.getInventory().add(rest)) {
			villager.spawnAtLocation(level, rest);
		}
		villager.makeSound(SoundEvents.VILLAGER_YES);
		level.playSound(null, villager.getX(), villager.getY(), villager.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.3F, 1.2F);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getY() + 1.8, villager.getZ(), 5, 0.3, 0.2, 0.3, 0.0);
		return InteractionResult.SUCCESS_SERVER;
	}

	/**
	 * Gives the worker the stack: emeralds into the wage box of its workstation, food into its
	 * lunch bag, anything else into its inventory. Returns what is left.
	 */
	static ItemStack store(Villager villager, ItemStack stack) {
		if (Meals.isBagged(villager, stack)) {
			return Meals.of(villager).bag().addItem(stack);
		}
		Workstations.Station box = isWage(stack) && villager.level() instanceof ServerLevel level ? wageBox(level, villager) : null;
		if (box == null) {
			return villager.getInventory().addItem(stack);
		}
		stack.shrink(box.addWage(stack.getCount()));
		return stack;
	}

	public static int count(Container inventory, Predicate<ItemStack> filter) {
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
