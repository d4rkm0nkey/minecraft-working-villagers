package me.teajay.workingvillagers.job;

import java.util.Optional;
import java.util.function.Predicate;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.carrier.CarrierData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * What a worker carries for its work besides the material: its wage ({@link Config#DAILY_WAGE}
 * emeralds per work day) and its tool (the lumberjack's axe, the farmer's hoe), plus what
 * it plants (saplings, seeds). The player gives these to the worker directly, like food to
 * a villager: right-click the worker with them, or throw them to it. Without its wage for
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

	/** A tool the worker needs, and the status while it has none. */
	public record Tool(Predicate<ItemStack> item, String missingKey) {
	}

	public static final Tool AXE = new Tool(stack -> stack.is(ItemTags.AXES), "lumberjack.no_axe");
	public static final Tool HOE = new Tool(stack -> stack.is(ItemTags.HOES), "farmer.no_hoe");

	private int waitTicks;

	/**
	 * Takes the wage of the day before the next piece of work and checks the tool. Returns
	 * true while that keeps the worker waiting (no wage, no tool), so the work has to wait;
	 * false when the worker may work.
	 */
	public boolean busy(ServerLevel level, Villager villager, @Nullable Tool tool) {
		if (this.waitTicks > 0) {
			this.waitTicks--;
			return true;
		}
		int wage = Config.DAILY_WAGE.get();
		SimpleContainer inventory = villager.getInventory();
		if (wage > 0 && !isPaid(level, villager)) {
			if (count(inventory, WorkerSupplies::isWage) < wage) {
				this.waitFor(level, villager, "no_wage");
				return true;
			}
			inventory.removeItemType(WAGE, wage);
			((HiredVillager) villager).workingvillagers$setPaidDay(ChopTrees.today(level));
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getY() + 1.8, villager.getZ(), 5, 0.3, 0.2, 0.3, 0.0);
		}
		if (tool != null && !inventory.hasAnyMatching(tool.item())) {
			this.waitFor(level, villager, tool.missingKey());
			return true;
		}
		return false;
	}

	/** Breaks off the waiting, e.g. when the work hours are over. */
	public void stop() {
		this.waitTicks = 0;
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

	/** The tool the worker needs for its job, null if none. */
	public static @Nullable Tool toolOf(Villager villager) {
		if (villager.getVillagerData().profession().is(WorkingVillagers.LUMBERJACK_PROFESSION)) {
			return AXE;
		}
		if (villager.getVillagerData().profession().is(WorkingVillagers.FARMER_PROFESSION)) {
			return HOE;
		}
		return null;
	}

	/** What the worker plants (saplings, seeds), null if nothing. */
	public static @Nullable Predicate<ItemStack> stockOf(Villager villager) {
		if (villager.getVillagerData().profession().is(WorkingVillagers.LUMBERJACK_PROFESSION)) {
			return stack -> stack.is(ItemTags.SAPLINGS);
		}
		if (villager.getVillagerData().profession().is(WorkingVillagers.FARMER_PROFESSION)) {
			return FarmFields::isSeed;
		}
		return null;
	}

	/**
	 * How many of the stack the worker takes: emeralds up to {@link #MAX_EMERALDS}, the tool
	 * of its job up to two, and what it plants up to the amount it keeps of each kind. 0 if
	 * it does not want it or has no room.
	 */
	public static int accepts(Villager villager, ItemStack stack) {
		if (stack.isEmpty() || !((HiredVillager) villager).workingvillagers$isHired() || villager.isBaby()) {
			return 0;
		}
		SimpleContainer inventory = villager.getInventory();
		int wanted;
		if (isWage(stack)) {
			wanted = MAX_EMERALDS - count(inventory, WorkerSupplies::isWage);
		} else {
			Tool tool = toolOf(villager);
			Predicate<ItemStack> stock = stockOf(villager);
			if (tool != null && tool.item().test(stack)) {
				wanted = MAX_TOOLS - count(inventory, tool.item());
			} else if (stock != null && stock.test(stack)) {
				int keep = villager.getVillagerData().profession().is(WorkingVillagers.FARMER_PROFESSION) ? Config.KEEP_SEEDS.get() : Config.KEEP_SAPLINGS.get();
				wanted = keep - count(inventory, other -> other.is(stack.getItem()));
			} else {
				return 0;
			}
		}
		int amount = Math.min(stack.getCount(), Math.max(0, wanted));
		return amount > 0 && inventory.canAddItem(stack.copyWithCount(amount)) ? amount : 0;
	}

	/** Whether the item is something the worker carries for its work: wage, tool or what it plants. */
	public static boolean isSupply(Villager villager, ItemStack stack) {
		if (!((HiredVillager) villager).workingvillagers$isHired()) {
			return false;
		}
		Tool tool = toolOf(villager);
		Predicate<ItemStack> stock = stockOf(villager);
		return isWage(stack) || tool != null && tool.item().test(stack) || stock != null && stock.test(stack);
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
