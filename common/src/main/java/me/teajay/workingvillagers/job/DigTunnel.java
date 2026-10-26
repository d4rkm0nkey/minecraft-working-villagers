package me.teajay.workingvillagers.job;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.station.Tunnel;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
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
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The work of the miner: dig the tunnel picked at the mining bench (see {@link Tunnel}),
 * section by section, and bring what it digs to the chests of the bench.
 *
 * <p>Before it digs a section it seals the water and lava around it with cobblestone, and it
 * closes holes in the floor; so the tunnel never floods and never ends in a drop. It only
 * digs natural blocks (the block tag {@code workingvillagers:mine_diggable}) its pickaxe is
 * good enough for; anything else blocks the tunnel. Ores next to the tunnel, and the ores
 * that hang on them, it digs too, as far as it reaches them from the tunnel.
 *
 * <p>Every 4 blocks of the tunnel it puts up a support like in a mineshaft (two fences and
 * three planks), every 8 blocks a torch. Without fences, planks or torches it digs on and
 * puts them up when it has some again.
 *
 * <p>It keeps a few cobblestone for sealing ({@link Config#KEEP_SEAL_BLOCKS}) and delivers
 * everything else once the next block does not fit any more, after its blocks for the day
 * ({@link Config#MINER_DAILY_BLOCKS}) and when the tunnel is done.
 */
public class DigTunnel extends Behavior<Villager> {
	private static final float SPEED = 0.5F;
	private static final int WALK_TIMEOUT = 400;
	private static final int IDLE_TICKS = 100;
	/** How long it takes to put a block, a fence or a torch. */
	private static final int PLACE_TICKS = 10;
	/** How far (from its eyes to the middle of the block) the miner reaches. */
	private static final double REACH = 4.5;
	private static final double EYES = 1.5;
	/** How long a block it could not get at is left out. */
	private static final long UNREACHABLE_COOLDOWN = 400L;
	/** How long a spot it could not walk to is left out. */
	private static final long BAD_SPOT_COOLDOWN = 1200L;
	/** At most this many ores wait to be dug, so a huge vein does not keep it from the tunnel. */
	private static final int MAX_PENDING_ORES = 64;

	private enum State { PLAN, MOVE, WORK, DELIVER, WAIT }

	/** What to do at a block. */
	private enum Kind { DIG, ORE, SEAL, FLOOR, FIXTURE }

	private record Action(Kind kind, BlockPos pos, Tunnel.@Nullable Fixture fixture, int section) {
	}

	/** Why the miner cannot go on with the tunnel. */
	private enum Blocked { NONE, BUILD, TOOL, SEAL, UNREACHABLE, DONE }

	private record Plan(@Nullable Action action, Blocked blocked, @Nullable BlockPos at) {
		static Plan of(Action action) {
			return new Plan(action, Blocked.NONE, null);
		}

		static Plan blocked(Blocked why, @Nullable BlockPos at) {
			return new Plan(null, why, at);
		}
	}

	private State state = State.PLAN;
	private int stateTicks;
	private String waitReason = "miner.no_tunnel";
	private Object[] waitArgs = {};
	private final WorkerSupplies supplies = new WorkerSupplies();
	private @Nullable Action action;
	private @Nullable BlockPos spot;
	private int workTicks;
	private @Nullable ChestTrip trip;
	private final Map<BlockPos, Long> unreachable = new HashMap<>();
	private final Map<BlockPos, Long> badSpots = new HashMap<>();
	private boolean waitingForSupplies;
	/** The tunnel the sections were made for. */
	private @Nullable Tunnel tunnel;
	private boolean supports;
	private List<Tunnel.Section> sections = List.of();
	/** All cells of the sections, to look them up quickly. */
	private Set<BlockPos> cells = Set.of();
	/** Ores next to the dug part of the tunnel, waiting to be dug. */
	private final Set<BlockPos> ores = new LinkedHashSet<>();
	/** The sections up to which the walls were searched for ores. */
	private int scanned;

	public DigTunnel() {
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
		if (!villager.getVillagerData().profession().is(WorkingVillagers.MINER_PROFESSION)) {
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
		this.action = null;
		this.setState(State.PLAN);
	}

	@Override
	protected void stop(ServerLevel level, Villager villager, long timestamp) {
		WorkerStatus.clear(villager);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
		villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		this.supplies.stop(villager);
		if (this.action != null) {
			level.destroyBlockProgress(villager.getId(), this.action.pos(), -1);
			this.action = null;
		}
		if (this.trip != null) {
			this.trip.close(villager);
			this.trip = null;
		}
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
		this.waitingForSupplies = false;
		switch (this.state) {
			case PLAN -> this.plan(level, villager, station);
			case MOVE -> this.move(level, villager);
			case WORK -> this.work(level, villager);
			case DELIVER -> this.deliver(level, villager, station);
			case WAIT -> {
				if (this.stateTicks >= IDLE_TICKS) {
					this.setState(State.PLAN);
				}
			}
		}
		this.report(villager);
	}

	private void report(Villager villager) {
		if (this.waitingForSupplies) {
			return;
		}
		switch (this.state) {
			case PLAN -> WorkerStatus.task(villager, "miner.plan");
			case MOVE -> WorkerStatus.task(villager, "miner.walk");
			case WORK -> {
				Action current = this.action;
				if (current == null) {
					WorkerStatus.task(villager, "miner.plan");
					return;
				}
				switch (current.kind()) {
					case DIG -> WorkerStatus.task(villager, "miner.dig", current.section() + 1, this.sections.size());
					case ORE -> WorkerStatus.task(villager, "miner.ore");
					case SEAL -> WorkerStatus.task(villager, "miner.seal");
					case FLOOR -> WorkerStatus.task(villager, "miner.floor");
					case FIXTURE -> WorkerStatus.task(villager, current.fixture() == Tunnel.Fixture.TORCH ? "miner.torch" : "miner.support");
				}
			}
			case DELIVER -> WorkerStatus.task(villager, "deliver");
			case WAIT -> WorkerStatus.task(villager, this.waitReason, this.waitArgs);
		}
	}

	// --- Planning ---

	private void plan(ServerLevel level, Villager villager, Workstations.Station station) {
		if (!villager.getBrain().isActive(Activity.WORK)) {
			return;
		}
		Tunnel tunnel = station.tunnel();
		if (tunnel == null) {
			this.rest(villager, station, "miner.no_tunnel");
			return;
		}
		if (tunnel.stairs() && !Unlocks.can(villager, Unlocks.Ability.STAIRS)) {
			this.rest(villager, station, "locked", WorkerLevels.name(Unlocks.Ability.STAIRS.level()));
			return;
		}
		// As long and with side tunnels as long as its level lets it
		tunnel = tunnel.limitedTo(WorkerLevels.area(villager, Config.TUNNEL_MAX_LENGTH.get()),
			Unlocks.can(villager, Unlocks.Ability.BRANCHES) ? WorkerLevels.area(villager, Config.TUNNEL_MAX_BRANCH.get()) : 0);
		this.update(tunnel);
		Tunnel.Section last = this.sections.getLast();
		if (!level.isLoaded(tunnel.start()) || !level.isLoaded(last.cells().getLast())) {
			this.rest(villager, station, "miner.not_loaded");
			return;
		}
		boolean tired = ((HiredVillager) villager).workingvillagers$getWorkDone(ChopTrees.today(level)) >= Config.MINER_DAILY_BLOCKS.get();
		if (tired) {
			if (this.hasDeliverables(villager)) {
				this.setState(State.DELIVER);
			} else {
				this.rest(villager, station, "miner.done_for_today");
			}
			return;
		}
		Plan plan = this.next(level, villager);
		Action next = plan.action();
		if (next == null) {
			if (this.hasDeliverables(villager)) {
				this.setState(State.DELIVER);
				return;
			}
			// Its pickaxe is worn or gone: give it back and fetch another before saying it is too weak
			if (plan.blocked() == Blocked.TOOL && WorkerSupplies.count(villager.getInventory(), WorkerSupplies.PICKAXE.ready()) == 0) {
				this.waitingForSupplies = this.supplies.busy(level, villager, WorkerSupplies.PICKAXE);
				return;
			}
			BlockPos at = plan.at();
			this.rest(villager, station, switch (plan.blocked()) {
				case BUILD -> "miner.blocked";
				case TOOL -> "miner.blocked_tool";
				case SEAL -> "miner.no_seal_blocks";
				case UNREACHABLE -> "miner.unreachable";
				case NONE, DONE -> "miner.done";
			});
			if (at != null && (plan.blocked() == Blocked.BUILD || plan.blocked() == Blocked.UNREACHABLE)) {
				this.waitArgs = new Object[] {at.getX(), at.getY(), at.getZ()};
			}
			return;
		}
		boolean digs = next.kind() == Kind.DIG || next.kind() == Kind.ORE;
		if (digs && this.hasDeliverables(villager) && !Digging.fits(villager.getInventory(), Digging.drops(level, villager, next.pos()))) {
			this.setState(State.DELIVER);
			return;
		}
		if (this.missingSupplies(level, villager)) {
			this.waitingForSupplies = this.supplies.busy(level, villager, WorkerSupplies.PICKAXE);
			return;
		}
		Optional<BlockPos> found = this.standSpot(level, villager, next.pos());
		if (found.isEmpty()) {
			this.unreachable.put(next.pos(), level.getGameTime() + UNREACHABLE_COOLDOWN);
			if (next.kind() == Kind.ORE) {
				this.ores.remove(next.pos());
			}
			return;
		}
		this.action = next;
		this.spot = found.get();
		if (StandSpots.isAt(villager, this.spot)) {
			this.startWork(level, villager);
		} else {
			this.setState(State.MOVE);
		}
	}

	private boolean missingSupplies(ServerLevel level, Villager villager) {
		if (WorkerSupplies.lacksWage(level, villager) || this.supplies.refillDue(level, villager)) {
			return true;
		}
		return WorkerSupplies.count(villager.getInventory(), WorkerSupplies.PICKAXE.ready()) == 0;
	}

	/** Makes the sections anew when the tunnel or its settings changed. */
	private void update(Tunnel tunnel) {
		boolean supports = Config.TUNNEL_SUPPORTS.get();
		if (tunnel.equals(this.tunnel) && supports == this.supports) {
			return;
		}
		boolean moved = this.tunnel == null || !tunnel.start().equals(this.tunnel.start()) || tunnel.direction() != this.tunnel.direction();
		this.tunnel = tunnel;
		this.supports = supports;
		this.sections = tunnel.sections(supports);
		Set<BlockPos> cells = new HashSet<>();
		this.sections.forEach(section -> cells.addAll(section.cells()));
		this.cells = cells;
		if (moved) {
			this.ores.clear();
		}
		this.scanned = 0;
	}

	/**
	 * What to do next: in the dug part of the tunnel close holes in the floor, dig the ores
	 * and put up what is missing (supports, torches); then go on with the first section that
	 * is not done: seal water and lava around it, dig it from the top down, close its floor.
	 */
	private Plan next(ServerLevel level, Villager villager) {
		int frontier = this.sections.size();
		for (Tunnel.Section section : this.sections) {
			if (!this.isDone(level, section)) {
				frontier = section.index();
				break;
			}
		}
		this.scanOres(level, frontier);
		SimpleContainer inventory = villager.getInventory();
		boolean seal = WorkerSupplies.count(inventory, WorkerSupplies::isSealBlock) > 0;
		for (Tunnel.Section section : this.sections.subList(0, frontier)) {
			for (BlockPos floor : section.floor()) {
				if (this.needsFloor(level, floor) && seal && this.mayTry(floor)) {
					return Plan.of(new Action(Kind.FLOOR, floor, null, section.index()));
				}
			}
		}
		Action ore = this.nextOre(level, villager);
		if (ore != null) {
			return Plan.of(ore);
		}
		for (Tunnel.Section section : this.sections.subList(0, frontier)) {
			for (Map.Entry<BlockPos, Tunnel.Fixture> fixture : section.fixtures().entrySet()) {
				BlockPos pos = fixture.getKey();
				if (level.getBlockState(pos).isAir() && this.has(inventory, fixture.getValue()) && this.mayTry(pos)) {
					return Plan.of(new Action(Kind.FIXTURE, pos, fixture.getValue(), section.index()));
				}
			}
		}
		if (frontier >= this.sections.size()) {
			return Plan.blocked(Blocked.DONE, null);
		}
		Tunnel.Section section = this.sections.get(frontier);
		List<BlockPos> wet = new ArrayList<>(section.shell());
		wet.addAll(section.cells());
		wet.addAll(section.floor());
		for (BlockPos pos : wet) {
			if (isLiquid(level, pos)) {
				if (!seal) {
					return Plan.blocked(Blocked.SEAL, pos);
				}
				return this.mayTry(pos) ? Plan.of(new Action(Kind.SEAL, pos, null, frontier)) : Plan.blocked(Blocked.UNREACHABLE, pos);
			}
		}
		for (BlockPos pos : section.cells()) {
			BlockState state = level.getBlockState(pos);
			if (!this.needsDigging(state, section.fixtures().get(pos))) {
				continue;
			}
			Blocked why = whyNot(level, villager, pos, state);
			if (why != Blocked.NONE) {
				return Plan.blocked(why, pos);
			}
			return this.mayTry(pos) ? Plan.of(new Action(Kind.DIG, pos, null, frontier)) : Plan.blocked(Blocked.UNREACHABLE, pos);
		}
		for (BlockPos floor : section.floor()) {
			if (this.needsFloor(level, floor)) {
				if (!level.getBlockState(floor).canBeReplaced()) {
					return Plan.blocked(Blocked.BUILD, floor);
				}
				if (!seal) {
					return Plan.blocked(Blocked.SEAL, floor);
				}
				return this.mayTry(floor) ? Plan.of(new Action(Kind.FLOOR, floor, null, frontier)) : Plan.blocked(Blocked.UNREACHABLE, floor);
			}
		}
		return Plan.blocked(Blocked.DONE, null);
	}

	private boolean mayTry(BlockPos pos) {
		return !this.unreachable.containsKey(pos);
	}

	/** Dug out, no water or lava around it, the floor closed. */
	private boolean isDone(ServerLevel level, Tunnel.Section section) {
		for (BlockPos pos : section.cells()) {
			BlockState state = level.getBlockState(pos);
			if (this.needsDigging(state, section.fixtures().get(pos)) || isLiquid(level, pos)) {
				return false;
			}
		}
		for (BlockPos pos : section.shell()) {
			if (isLiquid(level, pos)) {
				return false;
			}
		}
		for (BlockPos pos : section.floor()) {
			if (this.needsFloor(level, pos)) {
				return false;
			}
		}
		return true;
	}

	/** Whether the cell still has something in it: anything but air, its own support or torch and water or lava (that is sealed). */
	private boolean needsDigging(BlockState state, Tunnel.@Nullable Fixture fixture) {
		if (state.isAir() || state.getBlock() instanceof LiquidBlock) {
			return false;
		}
		return fixture == null || !matches(state, fixture);
	}

	private static boolean matches(BlockState state, Tunnel.Fixture fixture) {
		return switch (fixture) {
			case FENCE -> state.is(BlockTags.WOODEN_FENCES);
			case PLANKS -> state.is(BlockTags.PLANKS);
			case TORCH -> state.getBlock() instanceof BaseTorchBlock;
		};
	}

	/** Whether the floor block has a hole (air, water, a plant) the miner closes with cobblestone. */
	private boolean needsFloor(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return !state.isFaceSturdy(level, pos, Direction.UP) && !this.cells.contains(pos);
	}

	private static boolean isLiquid(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos).getBlock() instanceof LiquidBlock;
	}

	/** Why the miner cannot dig the block, NONE if it can. */
	private static Blocked whyNot(ServerLevel level, Villager villager, BlockPos pos, BlockState state) {
		if (state.getBlock() instanceof BaseTorchBlock) {
			return Blocked.NONE; // a torch in the way, picked up
		}
		if (!state.is(WorkingVillagers.MINE_DIGGABLE) || state.getDestroySpeed(level, pos) < 0.0F) {
			return Blocked.BUILD;
		}
		if (Digging.toolTooWeak(villager.getInventory(), state)) {
			return Blocked.TOOL;
		}
		return Blocked.NONE;
	}

	private boolean has(SimpleContainer inventory, Tunnel.Fixture fixture) {
		return !this.fixtureItem(inventory, fixture).isEmpty();
	}

	private ItemStack fixtureItem(SimpleContainer inventory, Tunnel.Fixture fixture) {
		for (ItemStack stack : inventory) {
			if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
				continue;
			}
			boolean fits = switch (fixture) {
				case FENCE -> WorkerSupplies.FENCES.item().test(stack);
				case PLANKS -> WorkerSupplies.PLANKS.item().test(stack);
				case TORCH -> WorkerSupplies.isTorch(stack);
			};
			if (fits) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	// --- Ores ---

	/** Looks for ores in the walls, ceiling and floor of the sections that are dug now. */
	private void scanOres(ServerLevel level, int frontier) {
		for (int i = this.scanned; i < frontier && this.ores.size() < MAX_PENDING_ORES; i++) {
			for (BlockPos cell : this.sections.get(i).cells()) {
				this.addOresAround(level, cell);
			}
		}
		this.scanned = Math.max(this.scanned, frontier);
	}

	private void addOresAround(ServerLevel level, BlockPos pos) {
		for (Direction side : Direction.values()) {
			BlockPos near = pos.relative(side);
			if (this.ores.size() < MAX_PENDING_ORES && level.getBlockState(near).is(WorkingVillagers.MINER_ORES) && !this.cells.contains(near)) {
				this.ores.add(near.immutable());
			}
		}
	}

	/** The closest ore it can dig; ores that are gone or too hard are forgotten. */
	private @Nullable Action nextOre(ServerLevel level, Villager villager) {
		this.ores.removeIf(pos -> {
			BlockState state = level.getBlockState(pos);
			return !state.is(WorkingVillagers.MINER_ORES) || whyNot(level, villager, pos, state) != Blocked.NONE || this.touchesLiquid(level, pos);
		});
		return this.ores.stream()
			.filter(this::mayTry)
			.min(Comparator.comparingDouble(pos -> pos.distToCenterSqr(villager.position())))
			.map(pos -> new Action(Kind.ORE, pos, null, -1))
			.orElse(null);
	}

	/** An ore next to water or lava stays, digging it would let the liquid into the tunnel. */
	private boolean touchesLiquid(ServerLevel level, BlockPos pos) {
		for (Direction side : Direction.values()) {
			if (!level.getFluidState(pos.relative(side)).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	// --- Moving ---

	/**
	 * Where to stand for the block: in a dug section (or in front of the tunnel), two free
	 * blocks above solid ground, within reach, not on or under the block. The closest to
	 * where the miner is.
	 */
	private Optional<BlockPos> standSpot(ServerLevel level, Villager villager, BlockPos target) {
		List<BlockPos> candidates = new ArrayList<>(this.tunnel == null ? List.of() : this.tunnel.entrance());
		for (Tunnel.Section section : this.sections) {
			if (!this.isDone(level, section)) {
				break;
			}
			candidates.addAll(section.stand());
		}
		Vec3 aim = Vec3.atCenterOf(target);
		Vec3 at = villager.position();
		return candidates.stream()
			.filter(spot -> !spot.equals(target) && !spot.above().equals(target) && !spot.below().equals(target) && !this.badSpots.containsKey(spot))
			.filter(spot -> Vec3.atBottomCenterOf(spot).add(0.0, EYES, 0.0).closerThan(aim, REACH))
			.filter(spot -> Digging.isStandable(level, spot))
			.min(Comparator.comparingDouble(spot -> Vec3.atBottomCenterOf(spot).distanceToSqr(at)));
	}

	private void move(ServerLevel level, Villager villager) {
		BlockPos spot = this.spot;
		if (spot == null || this.action == null) {
			this.setState(State.PLAN);
			return;
		}
		if (StandSpots.isAt(villager, spot)) {
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			this.startWork(level, villager);
		} else if (this.stateTicks > WALK_TIMEOUT) {
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			this.badSpots.put(spot, level.getGameTime() + BAD_SPOT_COOLDOWN);
			this.action = null;
			this.setState(State.PLAN);
		} else if (this.stateTicks % 20 == 1) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(spot, SPEED, 0));
		}
	}

	// --- Working ---

	private void startWork(ServerLevel level, Villager villager) {
		Action current = this.action;
		if (current == null) {
			this.setState(State.PLAN);
			return;
		}
		ItemStack inHand;
		if (current.kind() == Kind.DIG || current.kind() == Kind.ORE) {
			BlockState state = level.getBlockState(current.pos());
			inHand = Digging.findTool(villager.getInventory(), state);
			this.workTicks = Digging.digTicks(level, villager, current.pos(), state, inHand, Config.MINER_DIG_SLOWDOWN.get(), Config.MINER_MIN_DIG_SECONDS.get());
		} else {
			inHand = current.kind() == Kind.FIXTURE ? this.fixtureItem(villager.getInventory(), current.fixture()) : this.sealItem(villager.getInventory());
			this.workTicks = PLACE_TICKS;
		}
		villager.setItemSlot(EquipmentSlot.MAINHAND, inHand.copyWithCount(1));
		villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		this.setState(State.WORK);
	}

	private void work(ServerLevel level, Villager villager) {
		Action current = this.action;
		if (current == null || this.spot == null || !Vec3.atBottomCenterOf(this.spot).add(0.0, EYES, 0.0).closerThan(Vec3.atCenterOf(current.pos()), REACH + 1.0)) {
			this.finish(level, villager);
			return;
		}
		BlockPos pos = current.pos();
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(pos));
		boolean digs = current.kind() == Kind.DIG || current.kind() == Kind.ORE;
		BlockState state = level.getBlockState(pos);
		if (digs && (state.isAir() || whyNot(level, villager, pos, state) != Blocked.NONE)) {
			this.finish(level, villager);
			return;
		}
		if (this.stateTicks % 10 == 0) {
			villager.swingForAttack(InteractionHand.MAIN_HAND);
			if (digs) {
				level.playSound(null, pos, state.getSoundType().getHitSound(), SoundSource.NEUTRAL, 0.8F, 0.8F);
			}
		}
		if (digs) {
			level.destroyBlockProgress(villager.getId(), pos, Math.min(9, this.stateTicks * 10 / Math.max(1, this.workTicks)));
		}
		if (this.stateTicks < this.workTicks) {
			return;
		}
		switch (current.kind()) {
			case DIG, ORE -> this.dig(level, villager, pos, state, current.kind() == Kind.ORE);
			case SEAL, FLOOR -> this.seal(level, villager, pos);
			case FIXTURE -> this.putFixture(level, villager, pos, current.fixture());
		}
		this.finish(level, villager);
	}

	private void finish(ServerLevel level, Villager villager) {
		if (this.action != null) {
			level.destroyBlockProgress(villager.getId(), this.action.pos(), -1);
		}
		this.action = null;
		villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		this.setState(State.PLAN);
	}

	private void dig(ServerLevel level, Villager villager, BlockPos pos, BlockState state, boolean ore) {
		ItemStack tool = Digging.findTool(villager.getInventory(), state);
		List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), villager, tool);
		level.destroyBlock(pos, false, villager);
		((HiredVillager) villager).workingvillagers$addWorkDone(ChopTrees.today(level), 1);
		if (state.getDestroySpeed(level, pos) > 0.0F) {
			Digging.wearTool(level, villager, tool);
		}
		for (ItemStack drop : drops) {
			ItemStack rest = villager.getInventory().addItem(drop);
			if (!rest.isEmpty()) {
				villager.spawnAtLocation(level, rest);
			}
		}
		if (ore) {
			this.ores.remove(pos);
			// The rest of the vein, from expert on
			if (Unlocks.can(villager, Unlocks.Ability.FOLLOW_ORES)) {
				this.addOresAround(level, pos);
			}
		}
	}

	/** Water, lava or a hole in the floor: a block of cobblestone into it. */
	private void seal(ServerLevel level, Villager villager, BlockPos pos) {
		ItemStack stack = this.sealItem(villager.getInventory());
		BlockState current = level.getBlockState(pos);
		if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item) || !current.canBeReplaced() && !(current.getBlock() instanceof LiquidBlock)) {
			return;
		}
		stack.shrink(1);
		villager.getInventory().setChanged();
		level.setBlockAndUpdate(pos, item.getBlock().defaultBlockState());
		level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	private ItemStack sealItem(SimpleContainer inventory) {
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && WorkerSupplies.isSealBlock(stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	/** A fence or planks of a support, or a torch on the wall. */
	private void putFixture(ServerLevel level, Villager villager, BlockPos pos, Tunnel.@Nullable Fixture fixture) {
		if (fixture == null || !level.getBlockState(pos).isAir()) {
			return;
		}
		ItemStack stack = this.fixtureItem(villager.getInventory(), fixture);
		if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) {
			return;
		}
		BlockState state = fixture == Tunnel.Fixture.TORCH ? torch(level, pos, item.getBlock()) : item.getBlock().defaultBlockState();
		if (state == null) {
			return;
		}
		stack.shrink(1);
		villager.getInventory().setChanged();
		level.setBlockAndUpdate(pos, Block.updateFromNeighbourShapes(state, level, pos));
		level.playSound(null, pos, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	/** The torch on a wall next to the position (the wall torch of the torch's kind), or standing on the floor; null if neither holds. */
	private static @Nullable BlockState torch(ServerLevel level, BlockPos pos, Block standing) {
		Identifier id = BuiltInRegistries.BLOCK.getKey(standing);
		Block wall = BuiltInRegistries.BLOCK.getOptional(id.withPath(id.getPath().replace("torch", "wall_torch"))).orElse(null);
		if (wall != null && wall.defaultBlockState().hasProperty(WallTorchBlock.FACING)) {
			for (Direction facing : Direction.Plane.HORIZONTAL) {
				BlockState state = wall.defaultBlockState().setValue(WallTorchBlock.FACING, facing);
				if (state.canSurvive(level, pos)) {
					return state;
				}
			}
		}
		BlockState state = standing.defaultBlockState();
		return state.canSurvive(level, pos) ? state : null;
	}

	// --- Delivering and waiting ---

	/** Hands in everything but its wage, pickaxe, torches and supports; of the cobblestone it keeps a few. */
	private static boolean isHandedIn(Villager villager, ItemStack stack) {
		return Chests.isHandedIn(stack) && !WorkerSupplies.isSupply(villager, stack);
	}

	private static int sealExcess(Villager villager) {
		return WorkerSupplies.count(villager.getInventory(), WorkerSupplies::isSealBlock) - Config.KEEP_SEAL_BLOCKS.get();
	}

	private boolean hasDeliverables(Villager villager) {
		return villager.getInventory().hasAnyMatching(stack -> isHandedIn(villager, stack)) || sealExcess(villager) > 0;
	}

	private void deliver(ServerLevel level, Villager villager, Workstations.Station station) {
		if (this.trip == null) {
			if (!this.hasDeliverables(villager)) {
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
		ChestTrip.Result result = this.trip.tick(level, villager, container -> deposit(villager, container));
		if (result == ChestTrip.Result.RUNNING) {
			return;
		}
		this.trip = null;
		if (result == ChestTrip.Result.FAILED) {
			this.rest(villager, station, "chest_unreachable");
		} else {
			this.setState(this.hasDeliverables(villager) ? State.DELIVER : State.PLAN);
		}
	}

	private static void deposit(Villager villager, Container container) {
		SimpleContainer inventory = villager.getInventory();
		Chests.deposit(inventory, container, stack -> isHandedIn(villager, stack));
		int excess = sealExcess(villager);
		for (int slot = 0; slot < inventory.getContainerSize() && excess > 0; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.isEmpty() || !WorkerSupplies.isSealBlock(stack)) {
				continue;
			}
			int amount = Math.min(excess, stack.getCount());
			ItemStack rest = Chests.store(container, stack.split(amount));
			excess -= amount - rest.getCount();
			stack.grow(rest.getCount());
			if (!rest.isEmpty()) {
				break; // the chest is full
			}
		}
		inventory.setChanged();
	}

	private void rest(Villager villager, Workstations.Station station, String reason, Object... args) {
		villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(station.pos(), SPEED, 2));
		this.waitReason = reason;
		this.waitArgs = args;
		this.setState(State.WAIT);
	}

	private void setState(State state) {
		this.state = state;
		this.stateTicks = 0;
	}
}
