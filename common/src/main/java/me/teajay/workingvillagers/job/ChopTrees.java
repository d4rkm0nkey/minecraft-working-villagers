package me.teajay.workingvillagers.job;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

/**
 * The work of the lumberjack: find a tree near the job site, walk up to the trunk, hack at
 * it (slower than a player, see {@link #chopTicks}), fell it, clear the leaves left without
 * a log (with what they drop: saplings, sticks, apples), replant a sapling and bring the wood
 * to one of the chests linked to the job site. After every delivery the lumberjack takes a break,
 * and it stops for the day after {@link Config#DAILY_LOGS} logs. Without a linked chest the
 * lumberjack does not work at all.
 *
 * <p>It needs its wage and an axe, which the player gives it (see {@link WorkerSupplies}).
 * The axe wears out like a player's, one use per log; without an axe it waits. It only
 * replants with saplings it carries, and keeps up to {@link Config#KEEP_SAPLINGS} of each kind
 * (given to it, or from the leaves), handing in the rest. Where it had too few saplings to
 * replant, it plants later, once it has them (see {@link Planting}).
 *
 * <p>If the chopping block says so, it strips every log while felling the tree, the way
 * its axe strips a log for a player (the axe's block transformer, so wood from data packs
 * and mods works too). That takes longer and wears the axe once more per log.
 */
public class ChopTrees extends Behavior<Villager> {
	private static final float SPEED = 0.5F;
	/**
	 * A master (level 5) needs this much of the time of a novice (level 1), whose slowdown
	 * is {@link Config#CHOP_SLOWDOWN}.
	 */
	private static final float MASTER_SHARE = 5.0F / 8.0F;
	/** Ticks between two logs while the tree comes down. */
	private static final int FELL_INTERVAL = 2;
	/** Ticks between two logs while the tree comes down and the logs are stripped. */
	private static final int STRIP_INTERVAL = 10;
	private static final int WALK_TIMEOUT = 400;
	private static final int SEARCH_COOLDOWN = 100;
	/** How long a tree that could not be reached is ignored. */
	private static final long UNREACHABLE_COOLDOWN = 2400L;
	/** How close the lumberjack gets to a trunk without a free spot next to it. */
	private static final double TREE_CLOSE = 2.5;
	/** How close is enough when it cannot get closer than that (its path ends there). */
	private static final double TREE_REACH = 4.5;
	/** Ticks after the last log until the leaves know they lost their tree (vanilla's leaf distance). */
	private static final int LEAVES_SETTLE = 20;
	private static final int LEAVES_PER_TICK = 2;
	/** How many places to plant later the lumberjack remembers; it forgets the oldest. */
	private static final int MAX_PLANTINGS = 64;

	private enum State { SEARCH, WALK_TO_TREE, CHOP, FELL, LEAVES, PLANT, DELIVER, WAIT }

	/**
	 * Where a felled tree stood that the lumberjack could not replant (too few saplings),
	 * kept on the villager (see {@link HiredVillager#workingvillagers$getPlantings}) until it
	 * planted there, or nothing can be planted there anymore.
	 */
	public record Planting(Block sapling, List<BlockPos> spots) {
		public static final Codec<Planting> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BuiltInRegistries.BLOCK.byNameCodec().fieldOf("sapling").forGetter(Planting::sapling),
			BlockPos.CODEC.listOf().fieldOf("spots").forGetter(Planting::spots)
		).apply(instance, Planting::new));

		/** Where a sapling can go right now. */
		List<BlockPos> freeSpots(ServerLevel level) {
			BlockState state = this.sapling.defaultBlockState();
			return this.spots.stream().filter(pos -> level.getBlockState(pos).isAir() && state.canSurvive(level, pos)).toList();
		}
	}

	private State state = State.SEARCH;
	private int stateTicks;
	private int waitTicks = SEARCH_COOLDOWN;
	private Trees.@Nullable Tree tree;
	private @Nullable BlockPos treeSpot;
	private int chopTicks;
	/** Why the lumberjack waits, for the status in the menu. */
	private String waitReason = "lumberjack.no_tree";
	private final List<BlockPos> logsLeft = new ArrayList<>();
	private final List<BlockPos> leavesLeft = new ArrayList<>();
	private @Nullable Planting planting;
	private @Nullable ChestTrip trip;
	private @Nullable Block sapling;
	private final Map<BlockPos, Long> unreachable = new HashMap<>();
	private final WorkerSupplies supplies = new WorkerSupplies();
	/** Whether the logs are stripped, as set at the chopping block. */
	private boolean stripping;

	public ChopTrees() {
		super(ImmutableMap.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT), 24000);
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
		return this.canWork(level, villager);
	}

	/**
	 * Stops when the work hours are over or something else (a zombie, the bed) took the
	 * villager away from the tree; the chopping then starts over next time.
	 */
	@Override
	protected boolean canStillUse(ServerLevel level, Villager villager, long timestamp) {
		boolean awayFromTree = this.state == State.CHOP && this.tree != null
			&& !this.tree.base().closerToCenterThan(villager.position(), TREE_REACH + 1.5);
		return this.canWork(level, villager) && villager.getBrain().isActive(Activity.WORK) && !awayFromTree;
	}

	private boolean canWork(ServerLevel level, Villager villager) {
		if ((Config.REQUIRE_MOB_GRIEFING.get() && !level.getGameRules().get(GameRules.MOB_GRIEFING)) || !villager.getVillagerData().profession().is(WorkingVillagers.LUMBERJACK_PROFESSION)) {
			return false;
		}
		Workstations.Station workplace = this.getWorkplace(level, villager);
		return workplace != null && !workplace.validOutputs(level).isEmpty();
	}

	private Workstations.@Nullable Station getWorkplace(ServerLevel level, Villager villager) {
		Optional<GlobalPos> jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
		if (jobSite.isEmpty() || jobSite.get().dimension() != level.dimension()) {
			return null;
		}
		return Workstations.find(level, jobSite.get().pos()).orElse(null);
	}

	@Override
	protected void start(ServerLevel level, Villager villager, long timestamp) {
		// Resume felling a tree that was interrupted, otherwise start over
		this.setState(this.state == State.FELL || this.state == State.CHOP ? State.WALK_TO_TREE : State.SEARCH);
	}

	@Override
	protected void stop(ServerLevel level, Villager villager, long timestamp) {
		WorkerStatus.clear(villager);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
		villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		this.supplies.stop(villager);
		if (this.tree != null) {
			level.destroyBlockProgress(villager.getId(), this.tree.base(), -1);
			// Felled but not replanted yet: later
			boolean felled = this.state == State.LEAVES || this.state == State.FELL && !level.getBlockState(this.tree.base()).is(BlockTags.LOGS);
			if (felled && this.sapling != null) {
				remember(villager, new Planting(this.sapling, this.tree.bottomLogs()));
				this.tree = null;
				this.setState(State.SEARCH);
			}
		}
		this.planting = null;
		if (this.trip != null) {
			this.trip.close(villager);
			this.trip = null;
		}
	}

	@Override
	protected void tick(ServerLevel level, Villager villager, long timestamp) {
		Workstations.Station workplace = this.getWorkplace(level, villager);
		if (workplace == null) {
			return;
		}
		this.stateTicks++;
		this.stripping = workplace.strip();
		this.unreachable.values().removeIf(until -> until < timestamp);
		if (this.state == State.SEARCH && !hasDeliverables(villager.getInventory())
			&& this.supplies.busy(level, villager, WorkerSupplies.AXE)) {
			return; // the chest sets the status
		}

		switch (this.state) {
			case SEARCH -> this.search(level, villager, workplace);
			case WALK_TO_TREE -> this.walkToTree(level, villager, timestamp);
			case CHOP -> this.chop(level, villager);
			case FELL -> this.fell(level, villager);
			case LEAVES -> this.clearLeaves(level, villager);
			case PLANT -> this.walkToPlanting(level, villager, timestamp);
			case DELIVER -> this.deliver(level, villager, workplace);
			case WAIT -> {
				if (this.stateTicks >= this.waitTicks) {
					this.setState(State.SEARCH);
				}
			}
		}
		this.report(villager);
	}

	/** The number of the current day, for the daily amount. */
	public static long today(ServerLevel level) {
		return level.getOverworldClockTime() / 24000L;
	}

	private void search(ServerLevel level, Villager villager, Workstations.Station workplace) {
		boolean tired = ((HiredVillager) villager).workingvillagers$getWorkDone(today(level)) >= Config.DAILY_LOGS.get();
		if (hasDeliverables(villager.getInventory()) && (tired || !villager.getInventory().canAddItem(new ItemStack(Items.OAK_LOG, 64)))) {
			this.setState(State.DELIVER);
			return;
		}
		this.planting = nextPlanting(level, villager, this.unreachable);
		if (this.planting != null) {
			this.setState(State.PLANT);
			return;
		}
		Optional<Trees.Tree> found = tired ? Optional.empty() : Trees.findNearest(level, workplace.pos(), this.unreachable);
		if (found.isPresent()) {
			if (this.supplies.busy(level, villager, WorkerSupplies.AXE)) {
				return;
			}
			this.tree = found.get();
			this.treeSpot = StandSpots.findNextTo(level, this.tree.bottomLogs(), villager).orElse(null);
			this.setState(State.WALK_TO_TREE);
		} else if (hasDeliverables(villager.getInventory())) {
			this.setState(State.DELIVER);
		} else {
			// Nothing to do (or done for today): wait at the job site
			this.rest(villager, workplace, SEARCH_COOLDOWN, tired ? "lumberjack.done_for_today" : "lumberjack.no_tree");
		}
	}

	private void walkToTree(ServerLevel level, Villager villager, long timestamp) {
		if (this.tree == null) {
			this.setState(State.SEARCH);
			return;
		}
		// The tree may have been cut down by someone else meanwhile
		Trees.Tree current = Trees.collect(level, this.tree.base());
		if (current == null) {
			this.tree = null;
			this.setState(State.SEARCH);
			return;
		}
		this.tree = current;
		boolean there = this.treeSpot != null ? StandSpots.isAt(villager, this.treeSpot) : this.closeTo(villager, this.tree.base());
		if (there) {
			ItemStack axe = findAxe(villager.getInventory());
			if (axe.isEmpty()) {
				this.setState(State.SEARCH); // fetches a new one first
				return;
			}
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			villager.setItemSlot(EquipmentSlot.MAINHAND, axe.copy());
			villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
			this.chopTicks = chopTicks(level, villager, this.tree, axe);
			this.setState(State.CHOP);
		} else if (this.stateTicks > WALK_TIMEOUT) {
			this.unreachable.put(this.tree.base(), timestamp + UNREACHABLE_COOLDOWN);
			this.tree = null;
			this.setState(State.SEARCH);
		} else if (this.stateTicks % 20 == 1) {
			this.walkTo(villager, this.treeSpot != null ? this.treeSpot : this.tree.base(), this.treeSpot != null ? 0 : 1);
		}
	}

	/**
	 * Without a spot next to it: close to the trunk (or a place to plant), or as close as the
	 * path gets (the walk target is dropped when the villager arrives or no path gets further).
	 */
	private boolean closeTo(Villager villager, BlockPos base) {
		if (base.closerToCenterThan(villager.position(), TREE_CLOSE)) {
			return true;
		}
		boolean walking = villager.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET);
		return !walking && this.stateTicks > 20 && base.closerToCenterThan(villager.position(), TREE_REACH);
	}

	/**
	 * How long the lumberjack hacks at the trunk: the time a player with the same axe needs
	 * to break all logs of the tree, times the slowdown for the lumberjack's level. Felling
	 * the tree afterwards counts towards that time.
	 */
	private static int chopTicks(ServerLevel level, Villager villager, Trees.Tree tree, ItemStack axe) {
		float playerTicks = 0.0F;
		for (BlockPos pos : tree.logs()) {
			BlockState state = level.getBlockState(pos);
			float hardness = state.getDestroySpeed(level, pos);
			if (hardness > 0.0F) {
				// Like BlockBehaviour#getDestroyProgress for a player with the right tool
				playerTicks += (float) Math.ceil(hardness * 30.0F / axe.getDestroySpeed(state));
			}
		}
		int ticks = Math.round(playerTicks * slowdown(villager)) - tree.logs().size() * FELL_INTERVAL;
		return Math.max(20, ticks);
	}

	/** By default from 8 times as long as a player for a novice down to 5 times for a master. */
	private static float slowdown(Villager villager) {
		float progress = (villager.getVillagerData().level() - 1) / 4.0F;
		float novice = (float) Config.CHOP_SLOWDOWN.get();
		float master = novice * MASTER_SHARE;
		return novice + (master - novice) * Math.clamp(progress, 0.0F, 1.0F);
	}

	private void chop(ServerLevel level, Villager villager) {
		BlockPos base = this.tree.base();
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(base));
		BlockState state = level.getBlockState(base);
		if (!state.is(BlockTags.LOGS)) {
			level.destroyBlockProgress(villager.getId(), base, -1);
			this.setState(State.SEARCH);
			return;
		}
		if (this.stateTicks % 10 == 0) {
			villager.swingForAttack(InteractionHand.MAIN_HAND);
			level.playSound(null, base, state.getSoundType().getHitSound(), SoundSource.NEUTRAL, 1.0F, 0.8F);
		}
		level.destroyBlockProgress(villager.getId(), base, this.stateTicks * 10 / this.chopTicks);
		if (this.stateTicks >= this.chopTicks) {
			level.destroyBlockProgress(villager.getId(), base, -1);
			level.playSound(null, base, SoundEvents.AXE_STRIP.value(), SoundSource.NEUTRAL, 1.0F, 0.9F);
			this.sapling = Trees.saplingFor(state).orElse(null);
			this.logsLeft.clear();
			this.logsLeft.addAll(this.tree.logs().reversed());
			this.setState(State.FELL);
		}
	}

	private void fell(ServerLevel level, Villager villager) {
		if (this.stateTicks % (this.stripping ? STRIP_INTERVAL : FELL_INTERVAL) != 0) {
			return;
		}
		if (this.logsLeft.isEmpty()) {
			villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			this.setState(State.LEAVES);
			return;
		}
		BlockPos pos = this.logsLeft.removeFirst();
		BlockState state = level.getBlockState(pos);
		if (!state.is(BlockTags.LOGS)) {
			return;
		}
		BlockState stripped = this.stripping ? strip(level, villager, pos, state) : null;
		List<ItemStack> drops = Block.getDrops(stripped != null ? stripped : state, level, pos, null);
		level.destroyBlock(pos, false, villager);
		((HiredVillager) villager).workingvillagers$addWorkDone(today(level), 1);
		wearAxe(level, villager, 1);
		collect(level, villager, drops);
	}

	private static void collect(ServerLevel level, Villager villager, List<ItemStack> drops) {
		for (ItemStack drop : drops) {
			ItemStack rest = villager.getInventory().addItem(drop);
			if (!rest.isEmpty()) {
				villager.spawnAtLocation(level, rest);
			}
		}
	}

	/**
	 * Clears the leaves the felled tree leaves behind, before they decay with their saplings
	 * where nobody picks them up, then replants: with the saplings of this tree too.
	 */
	private void clearLeaves(ServerLevel level, Villager villager) {
		if (this.stateTicks < LEAVES_SETTLE) {
			return;
		}
		if (this.stateTicks == LEAVES_SETTLE) {
			this.leavesLeft.clear();
			this.leavesLeft.addAll(Trees.looseLeaves(level, this.tree.logs()));
		}
		for (int i = 0; i < LEAVES_PER_TICK && !this.leavesLeft.isEmpty(); i++) {
			BlockPos pos = this.leavesLeft.removeFirst();
			BlockState state = level.getBlockState(pos);
			if (Trees.isLooseLeaves(state)) {
				List<ItemStack> drops = Block.getDrops(state, level, pos, null);
				level.destroyBlock(pos, false, villager);
				collect(level, villager, drops);
			}
		}
		if (this.leavesLeft.isEmpty()) {
			if (this.sapling != null) {
				Planting planting = new Planting(this.sapling, this.tree.bottomLogs());
				if (!plant(level, villager, planting)) {
					remember(villager, planting);
				}
			}
			this.tree = null;
			this.setState(State.DELIVER);
		}
	}

	/**
	 * What the log becomes when the lumberjack's axe strips it, null if the axe cannot strip
	 * it. Like a player's right click: the first rule of the axe's block transformer that
	 * matches the log; it plays the sound and wears the axe. The block itself stays as it is,
	 * it is broken right afterwards (and a tree left standing is still a tree).
	 */
	private static @Nullable BlockState strip(ServerLevel level, Villager villager, BlockPos pos, BlockState state) {
		Holder<BlockTransformer> transformer = findAxe(villager.getInventory()).get(DataComponents.BLOCK_TRANSFORMER);
		if (transformer == null) {
			return null;
		}
		for (BlockTransformer.BlockTransformData data : WorkingVillagers.platform().blockTransforms(transformer)) {
			BlockState stripped = data.blockStateProvider().value().getOptionalState(level, villager.getRandom(), pos);
			if (stripped != null && !stripped.is(state.getBlock())) {
				villager.swingForAttack(InteractionHand.MAIN_HAND);
				level.playSound(null, pos, data.sound().value(), SoundSource.NEUTRAL, 1.0F, 1.0F);
				wearAxe(level, villager, data.itemDamagePerUse());
				return stripped;
			}
		}
		return null;
	}

	/** Uses of the axe, one per log like a player's; a broken axe is gone. */
	private static void wearAxe(ServerLevel level, Villager villager, int uses) {
		ItemStack axe = findAxe(villager.getInventory());
		if (axe.isEmpty() || uses <= 0) {
			return;
		}
		axe.hurtAndBreak(uses, level, null, broken -> {
			villager.onEquippedItemBroken(broken, EquipmentSlot.MAINHAND);
			villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		});
		villager.getInventory().setChanged();
	}

	/**
	 * Plants saplings where a felled tree stood, if the lumberjack carries enough of them
	 * (four for a 2×2 tree). False if it has too few: then it is still to do.
	 */
	private static boolean plant(ServerLevel level, Villager villager, Planting planting) {
		List<BlockPos> spots = planting.freeSpots(level);
		Item item = planting.sapling().asItem();
		SimpleContainer inventory = villager.getInventory();
		if (spots.isEmpty()) {
			return true;
		}
		if (inventory.countItem(item) < spots.size()) {
			return false;
		}
		inventory.removeItemType(item, spots.size());
		for (BlockPos pos : spots) {
			level.setBlockAndUpdate(pos, planting.sapling().defaultBlockState());
		}
		level.playSound(null, spots.getFirst(), SoundEvents.GRASS_PLACE, SoundSource.NEUTRAL, 1.0F, 1.0F);
		return true;
	}

	private static void remember(Villager villager, Planting planting) {
		List<Planting> plantings = ((HiredVillager) villager).workingvillagers$getPlantings();
		plantings.remove(planting);
		plantings.add(planting);
		while (plantings.size() > MAX_PLANTINGS) {
			plantings.removeFirst();
		}
	}

	/**
	 * The closest place to plant later that the lumberjack now has the saplings for. Forgets
	 * the places where nothing can be planted anymore (a sapling or a block is there).
	 */
	private static @Nullable Planting nextPlanting(ServerLevel level, Villager villager, Map<BlockPos, Long> ignored) {
		List<Planting> plantings = ((HiredVillager) villager).workingvillagers$getPlantings();
		plantings.removeIf(planting -> planting.freeSpots(level).isEmpty());
		Planting closest = null;
		for (Planting planting : plantings) {
			BlockPos spot = planting.spots().getFirst();
			if (ignored.containsKey(spot) || villager.getInventory().countItem(planting.sapling().asItem()) < planting.freeSpots(level).size()) {
				continue;
			}
			if (closest == null || spot.distToCenterSqr(villager.position()) < closest.spots().getFirst().distToCenterSqr(villager.position())) {
				closest = planting;
			}
		}
		return closest;
	}

	private void walkToPlanting(ServerLevel level, Villager villager, long timestamp) {
		if (this.planting == null) {
			this.setState(State.SEARCH);
			return;
		}
		BlockPos spot = this.planting.spots().getFirst();
		if (this.closeTo(villager, spot)) {
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			if (plant(level, villager, this.planting)) {
				((HiredVillager) villager).workingvillagers$getPlantings().remove(this.planting);
			}
			this.planting = null;
			this.setState(State.SEARCH);
		} else if (this.stateTicks > WALK_TIMEOUT) {
			this.unreachable.put(spot, timestamp + UNREACHABLE_COOLDOWN);
			this.planting = null;
			this.setState(State.SEARCH);
		} else if (this.stateTicks % 20 == 1) {
			this.walkTo(villager, spot, 1);
		}
	}

	static boolean isAxe(ItemStack stack) {
		return stack.is(ItemTags.AXES);
	}

	/** The axe the lumberjack works with; a worn one it gives back instead (see {@link WorkerSupplies#isWorn}). */
	private static ItemStack findAxe(SimpleContainer inventory) {
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && isAxe(stack) && !WorkerSupplies.isWorn(stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private void deliver(ServerLevel level, Villager villager, Workstations.Station workplace) {
		if (this.trip == null) {
			if (!hasDeliverables(villager.getInventory())) {
				this.setState(State.SEARCH);
				return;
			}
			Optional<BlockPos> chest = Chests.closest(workplace.validOutputs(level).stream().filter(pos -> Chests.hasRoom(level, pos)).toList(), villager);
			if (chest.isEmpty()) {
				this.rest(villager, workplace, SEARCH_COOLDOWN, "chests_full");
				return;
			}
			this.trip = ChestTrip.to(level, villager, chest.get());
		}
		ChestTrip.Result result = this.trip.tick(level, villager, container -> handIn(villager.getInventory(), container));
		if (result == ChestTrip.Result.RUNNING) {
			return;
		}
		this.trip = null;
		if (result == ChestTrip.Result.FAILED) {
			this.rest(villager, workplace, SEARCH_COOLDOWN, "chest_unreachable");
		} else if (hasDeliverables(villager.getInventory())) {
			// Did not all fit: next chest
			this.setState(State.DELIVER);
		} else {
			this.rest(villager, workplace, Config.REST_SECONDS.get() * 20, "lumberjack.break");
		}
	}

	/** Goes back to the chopping block and waits there. */
	private void rest(Villager villager, Workstations.Station workplace, int ticks, String reason) {
		this.walkTo(villager, workplace.pos(), 2);
		this.waitReason = reason;
		this.waitTicks = ticks;
		this.setState(State.WAIT);
	}

	/** Tells the menus what the lumberjack is doing. */
	private void report(Villager villager) {
		switch (this.state) {
			case SEARCH -> WorkerStatus.task(villager, "lumberjack.search");
			case WALK_TO_TREE -> WorkerStatus.task(villager, "lumberjack.walk");
			case CHOP -> WorkerStatus.task(villager, "lumberjack.chop", Math.min(99, this.stateTicks * 100 / Math.max(1, this.chopTicks)));
			case FELL -> WorkerStatus.task(villager, this.stripping ? "lumberjack.fell_strip" : "lumberjack.fell");
			case LEAVES -> WorkerStatus.task(villager, "lumberjack.leaves");
			case PLANT -> WorkerStatus.task(villager, "lumberjack.plant");
			case DELIVER -> WorkerStatus.task(villager, "deliver");
			case WAIT -> WorkerStatus.task(villager, this.waitReason);
		}
	}

	private static boolean hasDeliverables(SimpleContainer inventory) {
		int[] amounts = amountsToHandIn(inventory);
		for (int amount : amounts) {
			if (amount > 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * How much of each inventory slot the lumberjack hands in: everything but food, its axe
	 * and the saplings it keeps.
	 */
	private static int[] amountsToHandIn(SimpleContainer inventory) {
		int[] amounts = new int[inventory.getContainerSize()];
		Map<Item, Integer> keptSaplings = new HashMap<>();
		for (int slot = 0; slot < amounts.length; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!Chests.isHandedIn(stack) || isAxe(stack)) {
				continue;
			}
			int keep = 0;
			if (stack.is(ItemTags.SAPLINGS)) {
				int kept = keptSaplings.getOrDefault(stack.getItem(), 0);
				keep = Math.min(stack.getCount(), Config.KEEP_SAPLINGS.get() - kept);
				keptSaplings.put(stack.getItem(), kept + keep);
			}
			amounts[slot] = stack.getCount() - keep;
		}
		return amounts;
	}

	private static void handIn(SimpleContainer inventory, Container container) {
		int[] amounts = amountsToHandIn(inventory);
		for (int slot = 0; slot < amounts.length; slot++) {
			if (amounts[slot] <= 0) {
				continue;
			}
			ItemStack stack = inventory.getItem(slot);
			ItemStack rest = HopperBlockEntity.addItem(null, container, stack.copyWithCount(amounts[slot]), null);
			int kept = stack.getCount() - amounts[slot] + rest.getCount();
			inventory.setItem(slot, kept <= 0 ? ItemStack.EMPTY : stack.copyWithCount(kept));
		}
	}

	private void walkTo(Villager villager, BlockPos pos, int closeEnough) {
		villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(pos, SPEED, closeEnough));
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(pos));
	}

	private void setState(State state) {
		this.state = state;
		this.stateTicks = 0;
	}
}
