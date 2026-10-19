package me.teajay.workingvillagers.job;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.station.Pit;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The work of the pit digger: dig out the pit the player marked with a ring of string (see
 * {@link Pit}), layer by layer from the top down to the depth set at the pit stake, and
 * bring everything it digs to the chests of the stake.
 *
 * <p>It only digs natural blocks (the block tag {@code workingvillagers:pit_diggable}),
 * never one next to water or lava, nothing below a block it may not dig (a column under a
 * build stays as a pillar) and nothing its pickaxe is too weak for. It takes the shovel or
 * the pickaxe, whichever is faster for the block; both wear out like a player's.
 *
 * <p>The pit digger gets in and out of the pit only by the ladder: it digs itself down the
 * ladder column (standing on the block it digs, the ladder holds it) and puts a ladder
 * against the wall as soon as a block of it is dug, so the ladder always reaches the bottom. Vanilla villagers do not plan paths over ladders, so climbing is a
 * step of its own here: to the top of the ladder, then straight up or down. With torches
 * it lights the floor of the pit, about every six blocks.
 *
 * <p>It delivers once what the next block drops does not fit any more, when it has dug its
 * blocks for the day ({@link Config#PIT_DAILY_BLOCKS}) or the pit is done. When the work
 * hours end it first climbs out of the pit.
 */
public class DigPit extends Behavior<Villager> {
	private static final float SPEED = 0.5F;
	private static final int WALK_TIMEOUT = 400;
	private static final int IDLE_TICKS = 100;
	/** How long a block that could not be reached (from no spot at all) is left out. */
	private static final long UNREACHABLE_COOLDOWN = 400L;
	/** How long a spot it could not walk to is left out. */
	private static final long BAD_SPOT_COOLDOWN = 1200L;
	/** How far (from its feet to the middle of the block) the pit digger reaches. */
	private static final double REACH = 4.0;
	/** How far above the highest string it digs away a hill inside the ring. */
	private static final int HILL = 8;
	/** How fast it climbs, in blocks per tick. */
	private static final double CLIMB_SPEED = 0.12;
	/** No torch closer than this to another one on the floor of the pit. */
	private static final int TORCH_SPACING = 5;
	private static final int RING_CHECK_INTERVAL = 40;

	private enum State { PLAN, MOVE, DIG, DELIVER, WAIT }

	/** A part of the way somewhere: a walk, or climbing the ladder. */
	private record Leg(Kind kind, BlockPos pos) {
		enum Kind { WALK, DOWN, UP }
	}

	/** Why a column is not dug further down. */
	private enum Blocked { NONE, LIQUID, TOOL, BUILD }

	/** A block to dig, from where, and why some columns stay. */
	private record Plan(@Nullable BlockPos target, Blocked blocked) {
	}

	private State state = State.PLAN;
	private int stateTicks;
	private String waitReason = "pit_digger.no_pit";
	private Object[] waitArgs = {};
	private final WorkerSupplies supplies = new WorkerSupplies();
	private final Deque<Leg> legs = new ArrayDeque<>();
	/** What comes after the legs: DIG or PLAN. */
	private State afterMove = State.PLAN;
	/** Climbing: the points to move through, one after the other. */
	private final Deque<Vec3> climb = new ArrayDeque<>();
	private boolean climbing;
	private @Nullable BlockPos target;
	private int digTicks;
	private @Nullable ChestTrip trip;
	private final Map<BlockPos, Long> unreachable = new HashMap<>();
	/** Spots it could not walk to, tried again later. */
	private final Map<BlockPos, Long> badSpots = new HashMap<>();
	/** The last block it could not reach, for the status. */
	private @Nullable BlockPos lastUnreachable;
	private boolean ringIntact = true;
	/** Set while the wage or a tool is missing: the supplies set the status then. */
	private boolean waitingForSupplies;
	/** Digging the ladder column down to get out of the pit, also after the work hours. */
	private boolean escaping;

	public DigPit() {
		super(ImmutableMap.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT), 24000);
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
		return this.canWork(level, villager);
	}

	/** After the work hours it goes on until it is out of the pit. */
	@Override
	protected boolean canStillUse(ServerLevel level, Villager villager, long timestamp) {
		if (!this.canWork(level, villager)) {
			return false;
		}
		if (villager.getBrain().isActive(Activity.WORK)) {
			return true;
		}
		Workstations.Station station = this.getWorkplace(level, villager);
		Pit pit = station == null ? null : station.pit();
		return pit != null && (this.climbing || isDeep(pit, villager.blockPosition()));
	}

	private boolean canWork(ServerLevel level, Villager villager) {
		if (!villager.getVillagerData().profession().is(WorkingVillagers.PIT_DIGGER_PROFESSION)) {
			return false;
		}
		Workstations.Station station = this.getWorkplace(level, villager);
		return station != null && !station.validOutputs(level).isEmpty();
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
		this.legs.clear();
		this.climb.clear();
		this.climbing = false;
		this.target = null;
		this.setState(State.PLAN);
	}

	@Override
	protected void stop(ServerLevel level, Villager villager, long timestamp) {
		WorkerStatus.clear(villager);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
		villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		this.supplies.stop(villager);
		if (this.target != null) {
			level.destroyBlockProgress(villager.getId(), this.target, -1);
			this.target = null;
		}
		if (this.trip != null) {
			this.trip.close(villager);
			this.trip = null;
		}
		this.climbing = false;
	}

	@Override
	protected void tick(ServerLevel level, Villager villager, long timestamp) {
		Workstations.Station station = this.getWorkplace(level, villager);
		if (station == null) {
			return;
		}
		this.stateTicks++;
		this.unreachable.values().removeIf(until -> until < timestamp);
		this.badSpots.values().removeIf(until -> until < timestamp);
		if (villager.tickCount % RING_CHECK_INTERVAL == 0) {
			// The ring at the stake is the pit: a new or changed ring is taken over
			Pit.attach(level, station);
		}
		Pit pit = station.pit();
		if (pit != null && villager.tickCount % RING_CHECK_INTERVAL == 0) {
			this.ringIntact = pit.isIntact(level);
		}
		boolean towardsWork = this.state == State.DIG || this.state == State.MOVE && this.afterMove == State.DIG;
		if (!villager.getBrain().isActive(Activity.WORK) && towardsWork && !this.climbing && !this.escaping) {
			// The work hours are over: no more digging, out of the pit
			if (this.target != null) {
				level.destroyBlockProgress(villager.getId(), this.target, -1);
				this.target = null;
			}
			villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			this.legs.clear();
			this.setState(State.PLAN);
		}
		this.waitingForSupplies = false;
		switch (this.state) {
			case PLAN -> this.plan(level, villager, station, pit);
			case MOVE -> this.move(level, villager, pit);
			case DIG -> this.dig(level, villager, station, pit);
			case DELIVER -> this.deliver(level, villager, station);
			case WAIT -> {
				if (this.stateTicks >= IDLE_TICKS) {
					this.setState(State.PLAN);
				}
			}
		}
		this.report(villager, station, pit);
	}

	private void report(Villager villager, Workstations.Station station, @Nullable Pit pit) {
		if (this.waitingForSupplies) {
			return;
		}
		switch (this.state) {
			case PLAN -> WorkerStatus.task(villager, "pit_digger.plan");
			case MOVE -> {
				Leg leg = this.legs.peek();
				String key = leg == null || leg.kind() == Leg.Kind.WALK ? "pit_digger.walk" : leg.kind() == Leg.Kind.DOWN ? "pit_digger.climb_down" : "pit_digger.climb_up";
				WorkerStatus.task(villager, key);
			}
			case DIG -> {
				int layer = pit == null || this.target == null ? 0 : pit.top() - this.target.getY();
				if (layer <= 0) {
					WorkerStatus.task(villager, "pit_digger.level");
				} else {
					WorkerStatus.task(villager, "pit_digger.dig", layer, station.depth());
				}
			}
			case DELIVER -> WorkerStatus.task(villager, "deliver");
			case WAIT -> WorkerStatus.task(villager, this.waitReason, this.waitArgs);
		}
	}

	// --- Planning ---

	private void plan(ServerLevel level, Villager villager, Workstations.Station station, @Nullable Pit pit) {
		this.escaping = false;
		SimpleContainer inventory = villager.getInventory();
		boolean deep = pit != null && isDeep(pit, villager.blockPosition());
		if (!villager.getBrain().isActive(Activity.WORK)) {
			// After work: out of the pit, then the behavior ends
			if (deep) {
				this.leavePit(level, villager, pit);
			}
			return;
		}
		if (pit == null) {
			this.rest(villager, station, "pit_digger.no_pit");
			return;
		}
		if (!pit.isLoaded(level)) {
			this.rest(villager, station, "pit_digger.not_loaded");
			return;
		}
		if (!this.ringIntact) {
			this.restOutside(level, villager, station, pit, "pit_digger.ring_broken");
			return;
		}
		boolean tired = ((HiredVillager) villager).workingvillagers$getWorkDone(ChopTrees.today(level)) >= Config.PIT_DAILY_BLOCKS.get();
		Plan plan = tired ? new Plan(null, Blocked.NONE) : this.nextBlock(level, villager, station, pit);
		// Its pockets are full when what the next block drops does not fit any more
		boolean full = plan.target() != null && !Digging.fits(inventory, Digging.drops(level, villager, plan.target()));
		if (hasDeliverables(villager) && (tired || full || plan.target() == null)) {
			if (deep) {
				this.leavePit(level, villager, pit);
			} else {
				this.setState(State.DELIVER);
			}
			return;
		}
		if (plan.target() == null) {
			String reason = tired ? "pit_digger.done_for_today" : switch (plan.blocked()) {
				case NONE -> this.unreachable.isEmpty() ? "pit_digger.done" : "pit_digger.unreachable";
				case LIQUID -> "pit_digger.blocked_liquid";
				case TOOL -> "pit_digger.blocked_tool";
				case BUILD -> "pit_digger.done_pillars";
			};
			this.restOutside(level, villager, station, pit, reason);
			if (this.state == State.WAIT && reason.equals("pit_digger.unreachable")) {
				this.waitArgs = this.unreachableArgs();
			}
			return;
		}
		// Wage and tools before the next block; out of the pit to wait for them
		if (this.missingSupplies(level, villager, pit, plan.target())) {
			if (deep) {
				this.leavePit(level, villager, pit);
			} else {
				this.waitingForSupplies = this.supplies.busy(level, villager, this.needs(pit, plan.target()));
			}
			return;
		}
		if (this.placeMissingLadders(level, villager, station, pit)) {
			return;
		}
		Optional<BlockPos> spot = this.standSpot(level, villager, pit, plan.target());
		// Down the ladder column it digs itself down the shaft (on the block, under its feet),
		// unless it gets at the block from where it is without climbing (down in a hole)
		boolean shaft = isLadderColumn(pit, plan.target()) && plan.target().getY() < pit.ladder().getY()
			&& (spot.isEmpty() || isDeep(pit, spot.get()) != isDeep(pit, villager.blockPosition()));
		if (shaft) {
			spot = Optional.of(plan.target().above());
		}
		if (spot.isEmpty()) {
			this.unreachable.put(plan.target(), level.getGameTime() + UNREACHABLE_COOLDOWN);
			this.lastUnreachable = plan.target();
			return;
		}
		this.target = plan.target();
		if (StandSpots.isAt(villager, spot.get())) {
			this.startDigging(level, villager);
		} else {
			this.goTo(level, villager, pit, spot.get(), State.DIG);
		}
	}

	/** The tools it needs for the block (and ladders once the pit gets deep). */
	private WorkerSupplies.Need[] needs(Pit pit, BlockPos target) {
		boolean ladder = target.getY() <= pit.exit().getY() - 2;
		return ladder
			? new WorkerSupplies.Need[] {WorkerSupplies.SHOVEL, WorkerSupplies.PICKAXE, WorkerSupplies.LADDERS}
			: new WorkerSupplies.Need[] {WorkerSupplies.SHOVEL, WorkerSupplies.PICKAXE};
	}

	private boolean missingSupplies(ServerLevel level, Villager villager, Pit pit, BlockPos target) {
		SimpleContainer inventory = villager.getInventory();
		if (Config.DAILY_WAGE.get() > 0 && !WorkerSupplies.isPaid(level, villager) || this.supplies.refillDue(level, villager)) {
			return true;
		}
		for (WorkerSupplies.Need need : this.needs(pit, target)) {
			if (!inventory.hasAnyMatching(stack -> !stack.isEmpty() && need.ready().test(stack))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The next block to dig. Inside the pit strictly layer by layer: nothing deeper while a
	 * block of the current layer is left (also the one it stands on, it moves for that, and
	 * one it could not reach, it waits for that), so the pit never has holes it cannot climb
	 * out of; in each layer the ladder column first, then outwards. A hill above the rim is
	 * dug first, from the top, skipping what it cannot reach. Columns are dug from the top;
	 * a column stops at the first block that has to stay.
	 */
	private Plan nextBlock(ServerLevel level, Villager villager, Workstations.Station station, Pit pit) {
		int bottom = pit.bottom(level, station.depth());
		List<BlockPos> candidates = new ArrayList<>();
		Blocked blocked = Blocked.NONE;
		for (BlockPos column : pit.inside()) {
			// A block that stays only holds up the column if it rests on it: a bridge above is fine
			Blocked above = Blocked.NONE;
			for (int y = pit.top() + HILL; y >= bottom; y--) {
				BlockPos pos = new BlockPos(column.getX(), y, column.getZ());
				BlockState state = level.getBlockState(pos);
				// Its own ladders, torches on the floor (torches higher up are picked up again), and
				// above the rim what hangs in the air (the crown of a tree): only a hill is dug away
				if (state.isAir() || state.is(Blocks.LADDER) && isLadderColumn(pit, pos) || y == bottom && state.getBlock() instanceof BaseTorchBlock
					|| y >= pit.top() && !restsOnGround(level, pos, pit)) {
					above = Blocked.NONE;
					continue;
				}
				Blocked why = this.whyNot(level, villager, pos, state);
				if (why == Blocked.BUILD && y > bottom) {
					above = why;
					continue;
				}
				if (why == Blocked.NONE && above != Blocked.NONE) {
					why = above;
				}
				if (why != Blocked.NONE) {
					if (why.ordinal() > blocked.ordinal() || blocked == Blocked.NONE) {
						blocked = why;
					}
					break;
				}
				candidates.add(pos);
				break;
			}
		}
		BlockPos best = null;
		for (BlockPos pos : candidates) {
			if (pos.getY() >= pit.top() && !this.unreachable.containsKey(pos) && (best == null || isBefore(pit, pos, best))) {
				best = pos;
			}
		}
		if (best == null) {
			int layer = candidates.stream().mapToInt(BlockPos::getY).filter(y -> y < pit.top()).max().orElse(Integer.MIN_VALUE);
			for (BlockPos pos : candidates) {
				if (pos.getY() == layer && !this.unreachable.containsKey(pos) && (best == null || isBefore(pit, pos, best))) {
					best = pos;
				}
			}
		}
		return new Plan(best, blocked);
	}

	/** Whether the block above the rim stands on the ground without a gap, like a hill. */
	private static boolean restsOnGround(ServerLevel level, BlockPos pos, Pit pit) {
		for (int y = pos.getY() - 1; y >= pit.top() - 1; y--) {
			if (level.getBlockState(pos.atY(y)).isAir()) {
				return false;
			}
		}
		return true;
	}

	/** Higher first, then the ladder column, then nearer to the ladder. */
	private static boolean isBefore(Pit pit, BlockPos pos, BlockPos other) {
		if (pos.getY() != other.getY()) {
			return pos.getY() > other.getY();
		}
		return horizontalDistance(pos, pit.ladder()) < horizontalDistance(other, pit.ladder());
	}

	private static double horizontalDistance(BlockPos a, BlockPos b) {
		double dx = a.getX() - b.getX();
		double dz = a.getZ() - b.getZ();
		return dx * dx + dz * dz;
	}

	/** Why the pit digger leaves the block (and everything below it), NONE if it digs it. */
	private Blocked whyNot(ServerLevel level, Villager villager, BlockPos pos, BlockState state) {
		if (state.getBlock() instanceof BaseTorchBlock) {
			return Blocked.NONE; // a torch of an earlier floor, picked up again
		}
		if (!state.is(WorkingVillagers.PIT_DIGGABLE) || state.getDestroySpeed(level, pos) < 0.0F || !state.getFluidState().isEmpty()) {
			return state.getFluidState().isEmpty() ? Blocked.BUILD : Blocked.LIQUID;
		}
		for (Direction side : Direction.values()) {
			if (side != Direction.UP && !level.getFluidState(pos.relative(side)).isEmpty()) {
				return Blocked.LIQUID;
			}
		}
		if (Digging.toolTooWeak(villager.getInventory(), state)) {
			return Blocked.TOOL;
		}
		return Blocked.NONE;
	}

	/**
	 * Where to stand to dig the block: two free blocks above solid ground, inside the ring or
	 * on it, within reach, not on the block itself. The closest to where it is now.
	 */
	private Optional<BlockPos> standSpot(ServerLevel level, Villager villager, Pit pit, BlockPos target) {
		List<BlockPos> candidates = new ArrayList<>();
		int range = (int) Math.ceil(REACH);
		for (int dx = -range; dx <= range; dx++) {
			for (int dz = -range; dz <= range; dz++) {
				BlockPos column = target.offset(dx, 0, dz);
				if (!pit.contains(column) && pit.ringAt(column).isEmpty()) {
					continue;
				}
				for (int dy = -2; dy <= 2; dy++) {
					BlockPos spot = column.above(dy);
					if (!spot.below().equals(target) && !this.badSpots.containsKey(spot)
						&& Vec3.atBottomCenterOf(spot).closerThan(Vec3.atCenterOf(target), REACH) && Digging.isStandable(level, spot)) {
						candidates.add(spot);
					}
				}
			}
		}
		Vec3 at = villager.position();
		return candidates.stream().min(Comparator.comparingDouble(spot -> Vec3.atBottomCenterOf(spot).distanceToSqr(at)
			// Staying in the pit or out of it saves a climb
			+ (isDeep(pit, spot) != isDeep(pit, villager.blockPosition()) ? 64.0 : 0.0)));
	}

	/** Whether the position is so deep in the pit that the only way out is the ladder. */
	private static boolean isDeep(Pit pit, BlockPos pos) {
		return pos.getY() <= pit.exit().getY() - 2 && (pit.contains(pos));
	}

	private static boolean isLadderColumn(Pit pit, BlockPos pos) {
		return pos.getX() == pit.ladder().getX() && pos.getZ() == pit.ladder().getZ() && pos.getY() <= pit.ladder().getY();
	}

	// --- Moving ---

	/** Sets out for the spot, by the ladder if the spot or the pit digger is deep in the pit. */
	private void goTo(ServerLevel level, Villager villager, Pit pit, BlockPos spot, State then) {
		this.legs.clear();
		boolean fromDeep = isDeep(pit, villager.blockPosition());
		boolean toDeep = isDeep(pit, spot);
		if (fromDeep && !toDeep) {
			this.legs.add(new Leg(Leg.Kind.WALK, ladderBottom(level, pit)));
			this.legs.add(new Leg(Leg.Kind.UP, pit.exit()));
		} else if (!fromDeep && toDeep) {
			this.legs.add(new Leg(Leg.Kind.WALK, pit.exit()));
			this.legs.add(new Leg(Leg.Kind.DOWN, ladderBottom(level, pit)));
		}
		this.legs.add(new Leg(Leg.Kind.WALK, spot));
		this.afterMove = then;
		this.setState(State.MOVE);
	}

	/**
	 * Climbs out of the pit, then plans again. If the ladder column does not reach down to
	 * the pit digger yet (it is in a hole beside it), it first digs the ladder column down
	 * from the side.
	 */
	private void leavePit(ServerLevel level, Villager villager, Pit pit) {
		BlockPos foot = ladderBottom(level, pit);
		if (foot.getY() > villager.getBlockY() + 1) {
			BlockPos block = foot.below();
			Optional<BlockPos> spot = this.standSpot(level, villager, pit, block);
			if (spot.isPresent() && this.whyNot(level, villager, block, level.getBlockState(block)) == Blocked.NONE) {
				this.target = block;
				this.escaping = true;
				if (StandSpots.isAt(villager, spot.get())) {
					this.startDigging(level, villager);
				} else {
					this.goTo(level, villager, pit, spot.get(), State.DIG);
				}
				return;
			}
		}
		this.legs.clear();
		this.legs.add(new Leg(Leg.Kind.WALK, ladderBottom(level, pit)));
		this.legs.add(new Leg(Leg.Kind.UP, pit.exit()));
		this.afterMove = State.PLAN;
		this.setState(State.MOVE);
	}

	/** Where it stands at the foot of the ladder: the lowest free block of the ladder column. */
	private static BlockPos ladderBottom(ServerLevel level, Pit pit) {
		BlockPos pos = new BlockPos(pit.ladder().getX(), pit.exit().getY(), pit.ladder().getZ());
		while (pos.getY() > level.getMinY() + 1 && Digging.isPassable(level, pos.below())) {
			pos = pos.below();
		}
		return pos;
	}

	private void move(ServerLevel level, Villager villager, @Nullable Pit pit) {
		Leg leg = this.legs.peek();
		if (leg == null || pit == null) {
			this.climbing = false;
			if (this.afterMove == State.DIG && this.target != null) {
				this.startDigging(level, villager);
			} else {
				this.setState(State.PLAN);
			}
			return;
		}
		switch (leg.kind()) {
			case WALK -> {
				if (StandSpots.isAt(villager, leg.pos())) {
					villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
					this.nextLeg();
				} else if (this.stateTicks > WALK_TIMEOUT) {
					boolean toSpot = this.legs.size() == 1 && this.afterMove == State.DIG;
					this.legs.clear();
					villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
					if (toSpot) {
						// Another spot next time; the block itself may well be reachable
						this.badSpots.put(leg.pos(), level.getGameTime() + BAD_SPOT_COOLDOWN);
						this.target = null;
						this.setState(State.PLAN);
						return;
					}
					if (this.target != null) {
						this.unreachable.put(this.target, level.getGameTime() + UNREACHABLE_COOLDOWN);
						this.lastUnreachable = this.target;
						this.target = null;
					}
					this.waitReason = "pit_digger.unreachable";
					this.waitArgs = this.unreachableArgs();
					this.setState(State.WAIT);
				} else if (this.stateTicks % 20 == 1) {
					villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(leg.pos(), SPEED, 0));
				}
			}
			case DOWN, UP -> this.climb(level, villager, pit, leg);
		}
	}

	/** The block it could not reach, for the status ("cannot reach x y z"). */
	private Object[] unreachableArgs() {
		BlockPos pos = this.lastUnreachable;
		return pos == null ? new Object[] {"?", "?", "?"} : new Object[] {pos.getX(), pos.getY(), pos.getZ()};
	}

	private void nextLeg() {
		this.legs.poll();
		this.climb.clear();
		this.stateTicks = 0;
	}

	/**
	 * Climbs the ladder: across to the ladder column and down, or up and across to the exit.
	 * The pit digger is moved straight there; vanilla movement does not climb on purpose.
	 */
	private void climb(ServerLevel level, Villager villager, Pit pit, Leg leg) {
		if (this.climb.isEmpty() && !this.climbing) {
			Vec3 above = new Vec3(pit.ladder().getX() + 0.5, pit.exit().getY(), pit.ladder().getZ() + 0.5);
			Vec3 end = Vec3.atBottomCenterOf(leg.pos());
			if (!this.isClimbable(level, pit, leg.kind() == Leg.Kind.DOWN ? leg.pos() : BlockPos.containing(villager.position()))) {
				this.legs.clear();
				this.waitReason = "pit_digger.ladder_blocked";
				this.waitArgs = new Object[0];
				this.setState(State.WAIT);
				return;
			}
			// Down: across to the ladder column, then down. Up: up the column, then across.
			this.climb.add(above);
			this.climb.add(end);
			this.climbing = true;
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			villager.getBrain().eraseMemory(MemoryModuleType.PATH);
		}
		Vec3 point = this.climb.peek();
		if (point == null) {
			this.climbing = false;
			this.nextLeg();
			return;
		}
		villager.getNavigation().stop();
		villager.setDeltaMovement(Vec3.ZERO);
		villager.resetFallDistance();
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(pit.exit()));
		Vec3 at = villager.position();
		Vec3 step = point.subtract(at);
		double length = step.length();
		if (length <= CLIMB_SPEED) {
			villager.setPos(point);
			this.climb.poll();
		} else {
			villager.setPos(at.add(step.scale(CLIMB_SPEED / length)));
		}
		if (villager.tickCount % 8 == 0) {
			level.playSound(null, villager.blockPosition(), SoundEvents.LADDER_STEP, SoundSource.NEUTRAL, 0.5F, 1.0F);
		}
	}

	/** Whether the ladder column is free from the exit down to the foot of the ladder. */
	private boolean isClimbable(ServerLevel level, Pit pit, BlockPos bottom) {
		for (int y = pit.exit().getY() + 1; y >= bottom.getY(); y--) {
			if (!Digging.isPassable(level, new BlockPos(pit.ladder().getX(), y, pit.ladder().getZ()))) {
				return false;
			}
		}
		return Digging.isPassable(level, pit.exit()) && Digging.isPassable(level, pit.exit().above());
	}

	// --- Digging ---

	private void startDigging(ServerLevel level, Villager villager) {
		BlockState state = level.getBlockState(this.target);
		ItemStack tool = Digging.findTool(villager.getInventory(), state);
		villager.setItemSlot(EquipmentSlot.MAINHAND, tool.copy());
		villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
		this.digTicks = Digging.digTicks(level, villager, this.target, state, tool, Config.DIG_SLOWDOWN.get(), Config.MIN_DIG_SECONDS.get());
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		this.setState(State.DIG);
	}

	private void dig(ServerLevel level, Villager villager, Workstations.Station station, @Nullable Pit pit) {
		BlockPos pos = this.target;
		if (pos == null || pit == null) {
			this.setState(State.PLAN);
			return;
		}
		BlockState state = level.getBlockState(pos);
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(pos));
		if (state.isAir() || this.whyNot(level, villager, pos, state) != Blocked.NONE
			|| !Vec3.atCenterOf(pos).closerThan(villager.position(), REACH + 1.0)) {
			level.destroyBlockProgress(villager.getId(), pos, -1);
			this.target = null;
			this.setState(State.PLAN);
			return;
		}
		if (this.stateTicks % 6 == 0) {
			villager.swingForAttack(InteractionHand.MAIN_HAND);
			level.playSound(null, pos, state.getSoundType().getHitSound(), SoundSource.NEUTRAL, 0.8F, 0.8F);
		}
		level.destroyBlockProgress(villager.getId(), pos, Math.min(9, this.stateTicks * 10 / Math.max(1, this.digTicks)));
		if (this.stateTicks < this.digTicks) {
			return;
		}
		level.destroyBlockProgress(villager.getId(), pos, -1);
		ItemStack tool = Digging.findTool(villager.getInventory(), state);
		List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), villager, tool);
		level.destroyBlock(pos, false, villager);
		if (!(state.getBlock() instanceof BaseTorchBlock)) {
			((HiredVillager) villager).workingvillagers$addWorkDone(ChopTrees.today(level), 1);
			if (state.getDestroySpeed(level, pos) > 0.0F) {
				Digging.wearTool(level, villager, tool);
			}
		}
		for (ItemStack drop : drops) {
			ItemStack rest = villager.getInventory().addItem(drop);
			if (!rest.isEmpty()) {
				villager.spawnAtLocation(level, rest);
			}
		}
		this.target = null;
		if (isLadderColumn(pit, pos)) {
			this.placeLadder(level, villager, pit, pos);
		} else if (pos.getY() == pit.bottom(level, station.depth())) {
			this.placeTorch(level, villager, pit, pos);
		}
		villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		this.setState(State.PLAN);
	}

	/** Puts a ladder into the dug block of the ladder column, against the wall below the exit. */
	private void placeLadder(ServerLevel level, Villager villager, Pit pit, BlockPos pos) {
		BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, pit.ladderFacing());
		SimpleContainer inventory = villager.getInventory();
		if (!level.getBlockState(pos).isAir() || !ladder.canSurvive(level, pos) || inventory.countItem(Blocks.LADDER.asItem()) <= 0) {
			return;
		}
		inventory.removeItemType(Blocks.LADDER.asItem(), 1);
		level.setBlockAndUpdate(pos, ladder);
		level.playSound(null, pos, SoundEvents.LADDER_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	/**
	 * Puts missing ladders into the ladder column (after a break in the work, or when the
	 * pit digger had none). Returns true while it is busy with that.
	 */
	private boolean placeMissingLadders(ServerLevel level, Villager villager, Workstations.Station station, Pit pit) {
		if (villager.getInventory().countItem(Blocks.LADDER.asItem()) <= 0) {
			return false;
		}
		BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, pit.ladderFacing());
		for (int y = pit.ladder().getY(); y >= pit.bottom(level, station.depth()); y--) {
			BlockPos pos = new BlockPos(pit.ladder().getX(), y, pit.ladder().getZ());
			BlockState state = level.getBlockState(pos);
			if (!state.isAir()) {
				if (state.is(Blocks.LADDER)) {
					continue;
				}
				break;
			}
			if (!ladder.canSurvive(level, pos)) {
				continue;
			}
			if (Vec3.atCenterOf(pos).closerThan(villager.position(), REACH + 1.0)) {
				this.placeLadder(level, villager, pit, pos);
				villager.swingForAttack(InteractionHand.MAIN_HAND);
				return true;
			}
			return false;
		}
		return false;
	}

	/** Lights the floor of the pit: a torch where there is none nearby yet. */
	private void placeTorch(ServerLevel level, Villager villager, Pit pit, BlockPos pos) {
		SimpleContainer inventory = villager.getInventory();
		ItemStack torch = ItemStack.EMPTY;
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && WorkerSupplies.isTorch(stack)) {
				torch = stack;
				break;
			}
		}
		if (torch.isEmpty() || !(torch.getItem() instanceof BlockItem item)) {
			return;
		}
		BlockState state = item.getBlock().defaultBlockState();
		if (!level.getBlockState(pos).isAir() || !state.canSurvive(level, pos)) {
			return;
		}
		for (BlockPos near : BlockPos.betweenClosed(pos.offset(-TORCH_SPACING, -1, -TORCH_SPACING), pos.offset(TORCH_SPACING, 1, TORCH_SPACING))) {
			if (level.getBlockState(near).getBlock() instanceof BaseTorchBlock) {
				return;
			}
		}
		torch.shrink(1);
		inventory.setChanged();
		level.setBlockAndUpdate(pos, state);
		level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	// --- Delivering and waiting ---

	/** Hands in everything but its wage, food, tools, ladders and torches. */
	private static boolean isHandedIn(Villager villager, ItemStack stack) {
		return Chests.isHandedIn(stack) && !WorkerSupplies.isSupply(villager, stack);
	}

	private static boolean hasDeliverables(Villager villager) {
		return villager.getInventory().hasAnyMatching(stack -> isHandedIn(villager, stack));
	}

	private void deliver(ServerLevel level, Villager villager, Workstations.Station station) {
		if (this.trip == null) {
			if (!hasDeliverables(villager)) {
				this.setState(State.PLAN);
				return;
			}
			Optional<BlockPos> chest = Chests.closest(station.validOutputs(level).stream().filter(pos -> Chests.hasRoom(level, pos)).toList(), villager);
			if (chest.isEmpty()) {
				this.rest(villager, station, "chests_full");
				return;
			}
			this.trip = ChestTrip.to(level, villager, chest.get());
		}
		ChestTrip.Result result = this.trip.tick(level, villager, container -> Chests.deposit(villager.getInventory(), container, stack -> isHandedIn(villager, stack)));
		if (result == ChestTrip.Result.RUNNING) {
			return;
		}
		this.trip = null;
		if (result == ChestTrip.Result.FAILED) {
			this.rest(villager, station, "chest_unreachable");
		} else {
			this.setState(hasDeliverables(villager) ? State.DELIVER : State.PLAN);
		}
	}

	/** Waits at the stake, out of the pit first. */
	private void restOutside(ServerLevel level, Villager villager, Workstations.Station station, Pit pit, String reason) {
		if (isDeep(pit, villager.blockPosition()) && !this.climbing) {
			this.leavePit(level, villager, pit);
			return;
		}
		this.climbing = false;
		villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(station.pos(), SPEED, 2));
		this.waitReason = reason;
		this.waitArgs = new Object[0];
		this.setState(State.WAIT);
	}

	private void rest(Villager villager, Workstations.Station station, String reason) {
		villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(station.pos(), SPEED, 2));
		this.waitReason = reason;
		this.waitArgs = new Object[0];
		this.setState(State.WAIT);
	}

	private void setState(State state) {
		this.state = state;
		this.stateTicks = 0;
	}
}
