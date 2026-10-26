package me.teajay.workingvillagers.job;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * The work of the librarian: it enchants what lies in its input chests (tools, weapons, armor,
 * books) at an enchanting table near its lectern, and brings it to the output chests.
 *
 * <p>Like a player taking the best offer of the table: the level is up to 30 with 15
 * bookshelves around the table (see {@link #highestLevel}), or lower if set at the lectern.
 * Instead of the player's levels it pays with bottles o' enchanting (one per
 * {@link Config#LEVELS_PER_BOTTLE} levels), from the guards and the weaponsmith, and with 1 to
 * 3 lapis lazuli like the three offers of the table. Both come from its input chests or a
 * village chest in reach.
 */
public class EnchantItems extends Behavior<Villager> {
	private static final float SPEED = 0.5F;
	private static final int WALK_TIMEOUT = 400;
	private static final int WAIT_TICKS = 100;
	/** One piece every three seconds. */
	private static final int WORK_TICKS = 60;
	private static final long FETCH_COOLDOWN = 600L;
	/** At most this many pieces per trip. */
	private static final int BATCH = 3;
	/** The highest level without bookshelves, like the third offer of a bare table. */
	private static final int BARE_TABLE_LEVEL = 8;

	private enum State { PLAN, FETCH, WALK, WORK, DELIVER, WAIT }

	private State state = State.PLAN;
	private int stateTicks;
	private String waitReason = "librarian.no_items";
	private Object[] waitArgs = new Object[0];
	private @Nullable ChestTrip trip;
	private @Nullable BlockPos fetchFrom;
	private long fetchFailedAt = Long.MIN_VALUE / 2;
	private final WorkerSupplies supplies = new WorkerSupplies();

	public EnchantItems() {
		super(ImmutableMap.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT), 24000);
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
		return this.canWork(level, villager);
	}

	@Override
	protected boolean canStillUse(ServerLevel level, Villager villager, long timestamp) {
		return this.canWork(level, villager) && villager.getBrain().isActive(Activity.WORK);
	}

	private boolean canWork(ServerLevel level, Villager villager) {
		if (!villager.getVillagerData().profession().is(WorkingVillagers.LIBRARIAN_PROFESSION)) {
			return false;
		}
		Workstations.Station workplace = getWorkplace(level, villager);
		return workplace != null && !workplace.validOutputs(level).isEmpty();
	}

	private static Workstations.@Nullable Station getWorkplace(ServerLevel level, Villager villager) {
		Optional<GlobalPos> jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
		if (jobSite.isEmpty() || jobSite.get().dimension() != level.dimension()) {
			return null;
		}
		return Workstations.find(level, jobSite.get().pos()).orElse(null);
	}

	@Override
	protected void start(ServerLevel level, Villager villager, long timestamp) {
		this.setState(State.PLAN);
	}

	@Override
	protected void stop(ServerLevel level, Villager villager, long timestamp) {
		WorkerStatus.clear(villager);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
		this.supplies.stop(villager);
		if (this.trip != null) {
			this.trip.close(villager);
			this.trip = null;
		}
	}

	@Override
	protected void tick(ServerLevel level, Villager villager, long timestamp) {
		Workstations.Station workplace = getWorkplace(level, villager);
		if (workplace == null) {
			return;
		}
		this.stateTicks++;
		if (this.state == State.PLAN && this.supplies.busy(level, villager)) {
			return; // the chest sets the status
		}
		switch (this.state) {
			case PLAN -> this.plan(level, villager, workplace, timestamp);
			case FETCH -> this.fetch(level, villager, workplace, timestamp);
			case WALK -> this.walk(level, villager, workplace);
			case WORK -> this.work(level, villager, workplace);
			case DELIVER -> this.deliver(level, villager, workplace);
			case WAIT -> {
				if (this.stateTicks >= WAIT_TICKS) {
					this.setState(State.PLAN);
				}
			}
		}
		this.report(villager);
	}

	// --- The enchanting table ---

	/** The enchanting table closest to the lectern, within {@link Config#TABLE_RADIUS}. */
	public static @Nullable BlockPos table(ServerLevel level, BlockPos lectern) {
		int radius = Config.TABLE_RADIUS.get();
		BlockPos best = null;
		for (BlockPos pos : BlockPos.betweenClosed(lectern.offset(-radius, -2, -radius), lectern.offset(radius, 2, radius))) {
			if (level.getBlockState(pos).is(Blocks.ENCHANTING_TABLE) && (best == null || pos.distSqr(lectern) < best.distSqr(lectern))) {
				best = pos.immutable();
			}
		}
		return best;
	}

	/** The bookshelves around the table that count, like for a player (at most 15). */
	public static int bookshelves(ServerLevel level, BlockPos table) {
		int count = 0;
		for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
			if (EnchantingTableBlock.isValidBookShelf(level, table, offset)) {
				count++;
			}
		}
		return Math.min(count, 15);
	}

	/** The highest level the table offers: 30 with 15 bookshelves, two per bookshelf, at least that of a bare table. */
	public static int highestLevel(int bookshelves) {
		return Mth.clamp(bookshelves * 2, BARE_TABLE_LEVEL, Workstations.MAX_ENCHANT_LEVEL);
	}

	/**
	 * The level it enchants at: the one set at the lectern, at most the highest the table offers
	 * and the librarian's level lets it (see {@link Unlocks#enchantingLevel}).
	 */
	private static int level(ServerLevel level, Villager villager, Workstations.Station workplace, BlockPos table) {
		int highest = Math.min(highestLevel(bookshelves(level, table)), Unlocks.enchantingLevel(villager));
		return workplace.enchantLevel() == 0 ? highest : Math.min(workplace.enchantLevel(), highest);
	}

	/** Bottles o' enchanting for one piece. */
	public static int bottlesFor(int level) {
		return Mth.positiveCeilDiv(level, Config.LEVELS_PER_BOTTLE.get());
	}

	/** Lapis lazuli for one piece, like the three offers of the table. */
	public static int lapisFor(int level) {
		return level >= 20 ? 3 : level >= 10 ? 2 : 1;
	}

	// --- What things are ---

	/** What it enchants: enchantable and not enchanted yet (books too). */
	static boolean isToEnchant(ItemStack stack) {
		return !stack.isEmpty() && stack.isEnchantable();
	}

	private static boolean isPayment(ItemStack stack) {
		return stack.is(Items.LAPIS_LAZULI) || stack.is(Items.EXPERIENCE_BOTTLE);
	}

	/** What it brings to the output chests: everything but what it still enchants, pays with or its wage. */
	private static boolean isProduct(ItemStack stack) {
		return Chests.isHandedIn(stack) && !isToEnchant(stack) && !isPayment(stack);
	}

	// --- Planning ---

	private void plan(ServerLevel level, Villager villager, Workstations.Station workplace, long timestamp) {
		SimpleContainer inventory = villager.getInventory();
		BlockPos table = table(level, workplace.pos());
		if (table == null) {
			this.idle(villager, workplace, "librarian.no_table");
			return;
		}
		int enchantLevel = level(level, villager, workplace, table);
		int bottles = bottlesFor(enchantLevel);
		int lapis = lapisFor(enchantLevel);
		boolean carrying = inventory.hasAnyMatching(EnchantItems::isToEnchant);
		boolean paid = inventory.countItem(Items.EXPERIENCE_BOTTLE) >= bottles && inventory.countItem(Items.LAPIS_LAZULI) >= lapis;
		if (carrying && paid) {
			this.setState(State.WALK);
			return;
		}
		if (inventory.hasAnyMatching(EnchantItems::isProduct)) {
			this.setState(State.DELIVER);
			return;
		}
		int pieces = Math.max(1, Math.min(BATCH, WorkerSupplies.count(inventory, EnchantItems::isToEnchant)));
		Predicate<ItemStack> lacking = stack -> !carrying && isToEnchant(stack)
			|| stack.is(Items.EXPERIENCE_BOTTLE) && inventory.countItem(Items.EXPERIENCE_BOTTLE) < bottles * pieces
			|| stack.is(Items.LAPIS_LAZULI) && inventory.countItem(Items.LAPIS_LAZULI) < lapis * pieces;
		if (timestamp - this.fetchFailedAt > FETCH_COOLDOWN) {
			// The things to enchant only from its input chests; bottles and lapis also from the village chests
			List<BlockPos> sources = new ArrayList<>(workplace.validInputs(level));
			List<BlockPos> villageChests = VillageChests.inReach(level, workplace.pos());
			Optional<BlockPos> chest = Chests.closest(sources.stream().filter(pos -> Chests.contains(level, pos, lacking)).toList(), villager)
				.or(() -> carrying ? Chests.closest(villageChests.stream()
					.filter(pos -> Chests.contains(level, pos, stack -> isPayment(stack) && lacking.test(stack))).toList(), villager) : Optional.empty());
			if (chest.isPresent()) {
				this.fetchFrom = chest.get();
				this.setState(State.FETCH);
				return;
			}
		}
		if (!carrying) {
			this.idle(villager, workplace, "librarian.no_items");
		} else if (inventory.countItem(Items.EXPERIENCE_BOTTLE) < bottles) {
			this.idle(villager, workplace, "librarian.no_bottles", bottles);
		} else {
			this.idle(villager, workplace, "librarian.no_lapis", lapis);
		}
	}

	private void fetch(ServerLevel level, Villager villager, Workstations.Station workplace, long timestamp) {
		if (this.trip == null) {
			if (this.fetchFrom == null) {
				this.setState(State.PLAN);
				return;
			}
			this.trip = ChestTrip.to(level, villager, this.fetchFrom);
		}
		BlockPos table = table(level, workplace.pos());
		int enchantLevel = table == null ? Math.min(BARE_TABLE_LEVEL, Unlocks.enchantingLevel(villager)) : level(level, villager, workplace, table);
		boolean input = workplace.validInputs(level).contains(this.fetchFrom);
		SimpleContainer inventory = villager.getInventory();
		int[] taken = {0};
		ChestTrip.Result result = this.trip.tick(level, villager, container -> {
			if (input) {
				taken[0] += Chests.take(container, inventory, EnchantItems::isToEnchant, Math.max(0, BATCH - WorkerSupplies.count(inventory, EnchantItems::isToEnchant)));
			}
			int pieces = Math.max(1, WorkerSupplies.count(inventory, EnchantItems::isToEnchant));
			taken[0] += Chests.take(container, inventory, stack -> stack.is(Items.EXPERIENCE_BOTTLE),
				Math.max(0, bottlesFor(enchantLevel) * pieces - inventory.countItem(Items.EXPERIENCE_BOTTLE)));
			taken[0] += Chests.take(container, inventory, stack -> stack.is(Items.LAPIS_LAZULI),
				Math.max(0, lapisFor(enchantLevel) * pieces - inventory.countItem(Items.LAPIS_LAZULI)));
		});
		if (result == ChestTrip.Result.RUNNING) {
			return;
		}
		this.trip = null;
		if (result == ChestTrip.Result.FAILED || taken[0] == 0) {
			this.fetchFailedAt = timestamp;
		}
		this.setState(State.PLAN);
	}

	// --- At the enchanting table ---

	private void walk(ServerLevel level, Villager villager, Workstations.Station workplace) {
		BlockPos table = table(level, workplace.pos());
		if (table == null) {
			this.setState(State.PLAN);
			return;
		}
		Optional<BlockPos> spot = StandSpots.find(level, table, villager);
		boolean there = spot.map(pos -> StandSpots.isAt(villager, pos)).orElse(table.closerToCenterThan(villager.position(), 2.5));
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(table));
		if (there) {
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			this.setState(State.WORK);
		} else if (this.stateTicks > WALK_TIMEOUT) {
			this.idle(villager, workplace, "librarian.table_unreachable");
		} else if (this.stateTicks % 20 == 1) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(spot.orElse(table), SPEED, spot.isPresent() ? 0 : 1));
		}
	}

	/** One piece every three seconds, as long as it has something to enchant and can pay. */
	private void work(ServerLevel level, Villager villager, Workstations.Station workplace) {
		BlockPos table = table(level, workplace.pos());
		if (table == null) {
			this.setState(State.PLAN);
			return;
		}
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(table));
		if (this.stateTicks % 4 == 0) {
			level.sendParticles(ParticleTypes.ENCHANT, table.getX() + 0.5, table.getY() + 1.6, table.getZ() + 0.5, 4, 0.4, 0.3, 0.4, 0.5);
		}
		if (this.stateTicks % WorkerLevels.ticks(villager, WORK_TICKS) != 0) {
			return;
		}
		SimpleContainer inventory = villager.getInventory();
		int enchantLevel = level(level, villager, workplace, table);
		int bottles = bottlesFor(enchantLevel);
		int lapis = lapisFor(enchantLevel);
		if (inventory.countItem(Items.EXPERIENCE_BOTTLE) < bottles || inventory.countItem(Items.LAPIS_LAZULI) < lapis) {
			this.setState(State.PLAN);
			return;
		}
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!isToEnchant(stack)) {
				continue;
			}
			ItemStack one = stack.split(1);
			ItemStack enchanted = EnchantmentHelper.enchantItem(level.getRandom(), one, enchantLevel, level.registryAccess(),
				level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.IN_ENCHANTING_TABLE));
			inventory.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
			inventory.removeItemType(Items.EXPERIENCE_BOTTLE, bottles);
			inventory.removeItemType(Items.LAPIS_LAZULI, lapis);
			ItemStack rest = inventory.addItem(enchanted);
			if (!rest.isEmpty()) {
				villager.spawnAtLocation(level, rest);
			}
			((HiredVillager) villager).workingvillagers$addWorkDone(ChopTrees.today(level), 1);
			villager.swingForAttack(InteractionHand.MAIN_HAND);
			level.playSound(null, table, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
			return;
		}
		this.setState(State.PLAN);
	}

	// --- Delivering ---

	private void deliver(ServerLevel level, Villager villager, Workstations.Station workplace) {
		if (this.trip == null) {
			if (!villager.getInventory().hasAnyMatching(EnchantItems::isProduct)) {
				this.setState(State.PLAN);
				return;
			}
			Optional<BlockPos> chest = Chests.closest(workplace.validOutputs(level).stream().filter(pos -> Chests.hasRoom(level, pos)).toList(), villager);
			if (chest.isEmpty()) {
				this.idle(villager, workplace, "chests_full");
				return;
			}
			this.trip = ChestTrip.to(level, villager, chest.get());
		}
		ChestTrip.Result result = this.trip.tick(level, villager, container -> {
			SimpleContainer inventory = villager.getInventory();
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				ItemStack stack = inventory.getItem(slot);
				if (isProduct(stack)) {
					inventory.setItem(slot, HopperBlockEntity.addItem(null, container, stack, null));
				}
			}
		});
		if (result == ChestTrip.Result.RUNNING) {
			return;
		}
		this.trip = null;
		if (result == ChestTrip.Result.FAILED) {
			this.idle(villager, workplace, "chest_unreachable");
		} else {
			this.setState(State.PLAN);
		}
	}

	private void idle(Villager villager, Workstations.Station workplace, String reason, Object... args) {
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(workplace.pos()));
		this.waitReason = reason;
		this.waitArgs = args;
		this.setState(State.WAIT);
	}

	private void report(Villager villager) {
		switch (this.state) {
			case PLAN, WALK -> WorkerStatus.task(villager, "librarian.plan");
			case FETCH -> WorkerStatus.task(villager, "librarian.fetch");
			case WORK -> WorkerStatus.task(villager, "librarian.enchant");
			case DELIVER -> WorkerStatus.task(villager, "deliver");
			case WAIT -> WorkerStatus.task(villager, this.waitReason, this.waitArgs);
		}
	}

	private void setState(State state) {
		this.state = state;
		this.stateTicks = 0;
	}
}
