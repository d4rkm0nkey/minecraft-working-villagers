package me.teajay.workingvillagers.job;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.mixin.BoatItemAccessor;
import me.teajay.workingvillagers.station.Voyage;
import me.teajay.workingvillagers.station.VoyageRecorder;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The work of the hauler: it rows a chest boat from its jetty to the jetty at the other end
 * of its route ({@link Voyage}) and back. At home it puts the boat on the water where the route
 * starts and loads it with what lies in the input chests of its jetty; at the other jetty it
 * unloads into the output chests there and loads what lies in the input chests there; back
 * home it unloads into its own output chests. It rows straight from waypoint to waypoint, like
 * the player rowed the route.
 *
 * <p>It only sets out if it is back before dusk ({@link #DUSK}), so a long route makes one
 * trip a day. Once it set out it does not stop at the end of the work hours: it comes home
 * first. Where it is on its trip (and its boat) is kept with its jetty
 * ({@link Workstations.Station#leg}), so a hauler loaded again in its boat rows on. When there
 * is nothing to carry it takes its boat out of the water again; a loaded boat stays moored.
 */
public class HaulCargo extends Behavior<Villager> {
	private static final float SPEED = 0.5F;
	/** How fast the hauler rows (blocks per tick), a little faster than the fisher. */
	private static final double ROW_SPEED = 0.18;
	private static final int WAIT_TICKS = 100;
	private static final int WALK_TIMEOUT = 600;
	/** How often (ticks) rowing is checked for progress, and how many failed checks make it skip a waypoint. */
	private static final int PROGRESS_TICKS = 60;
	private static final int STUCK_CHECKS = 3;
	/** How close the boat comes to a waypoint before it heads for the next one. */
	private static final double WAYPOINT_REACH = 2.0;
	/** How far the moored boat may drift from where it lies before it is pulled back. */
	private static final double MOOR_DRIFT = 1.5;
	/** How far from where it should lie the boat is still the hauler's boat at that jetty. */
	private static final double BOAT_RANGE = 48.0;
	/** How far the hauler looks around the water at a jetty for a place on the shore. */
	private static final int SHORE_RADIUS = 4;
	/** Loading and unloading at both ends, on top of the rowing. */
	private static final int HANDLING_TICKS = 1200;
	/** Time of day: the hauler is back home by then (it gets dark soon after). */
	private static final long DUSK = 12000L;
	private static final long WORK_START = 2000L;

	private enum State { PLAN, LAUNCH, LOAD, UNLOAD, BOARD, ROW, STOW, WAIT }

	private State state = State.PLAN;
	private int stateTicks;
	private @Nullable ChestTrip trip;
	private final WorkerSupplies supplies = new WorkerSupplies();
	private String waitReason = "hauler.nothing";
	private Object[] waitArgs = {};
	/** Rowing: the index of the waypoint the boat heads for, and which way along the route. */
	private int next;
	private boolean homeward;
	private @Nullable Vec3 lastPosition;
	private int stuckChecks;
	/** Rowing: the water ahead is not loaded, it waits on the water. */
	private boolean waitingForChunks;
	/** At this jetty nothing more fit into the boat; it sets out with what it has. */
	private boolean boatFull;
	/** Items the last visit to a chest moved. */
	private int moved;

	public HaulCargo() {
		super(ImmutableMap.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT), 24000);
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
		return isHauler(villager) && getWorkplace(level, villager) != null;
	}

	/** Once it set out it goes on until it is back home, also after work. */
	@Override
	protected boolean canStillUse(ServerLevel level, Villager villager, long timestamp) {
		Workstations.Station workplace = getWorkplace(level, villager);
		if (!isHauler(villager) || workplace == null) {
			return false;
		}
		return workplace.leg() != Workstations.LEG_HOME || this.riding(villager, workplace) != null || villager.getBrain().isActive(Activity.WORK);
	}

	private static boolean isHauler(Villager villager) {
		return villager.getVillagerData().profession().is(WorkingVillagers.HAULER_PROFESSION);
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
		if (!isHauler(villager) && villager.getVehicle() instanceof AbstractChestBoat) {
			villager.stopRiding();
		}
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
		Voyage voyage = workplace.voyage();
		if (voyage == null) {
			if (this.state != State.WAIT) {
				this.rest(level, villager, workplace.pos(), "hauler.no_route");
			}
		} else {
			switch (this.state) {
				case PLAN -> this.plan(level, villager, workplace, voyage);
				case LAUNCH -> this.launch(level, villager, workplace, voyage);
				case LOAD, UNLOAD -> this.visit(level, villager, workplace, voyage);
				case BOARD -> this.board(level, villager, workplace, voyage);
				case ROW -> this.row(level, villager, workplace, voyage);
				case STOW -> this.stow(level, villager, workplace, voyage);
				case WAIT -> { }
			}
		}
		if (this.state == State.WAIT && this.stateTicks >= WAIT_TICKS) {
			this.setState(State.PLAN);
		}
		this.report(villager);
	}

	// --- Deciding what to do next ---

	private void plan(ServerLevel level, Villager villager, Workstations.Station workplace, Voyage voyage) {
		AbstractChestBoat riding = this.riding(villager, workplace);
		int leg = workplace.leg();
		if (riding != null) {
			if (leg == Workstations.LEG_OUTBOUND || leg == Workstations.LEG_INBOUND) {
				// Loaded again in its boat: rows on
				this.setOff(villager, riding, voyage, leg == Workstations.LEG_INBOUND);
			} else {
				// Got in by walking into it
				this.getOut(villager, riding, this.shore(level, this.water(voyage, leg)));
			}
			return;
		}
		if (villager.isPassenger()) {
			villager.stopRiding();
		}
		AbstractChestBoat boat = this.boat(level, workplace);
		if (leg == Workstations.LEG_OUTBOUND || leg == Workstations.LEG_INBOUND) {
			// Out of the boat while rowing (it broke, it was loaded again without it): at the jetty it is closest to
			boolean there = villager.distanceToSqr(Vec3.atCenterOf(voyage.destination())) < villager.distanceToSqr(Vec3.atCenterOf(workplace.pos()));
			leg = there ? Workstations.LEG_THERE : Workstations.LEG_HOME;
			workplace.setLeg(leg);
		}
		boolean home = leg == Workstations.LEG_HOME;
		BlockPos water = this.water(voyage, leg);
		Workstations.Station jetty = home ? workplace : this.otherJetty(level, voyage);
		if (jetty == null) {
			this.rest(level, villager, workplace.pos(), "hauler.no_jetty");
			return;
		}
		if (boat != null && !boat.position().closerThan(Vec3.atCenterOf(water), BOAT_RANGE)) {
			// Its boat lies at the other jetty, or drifted away
			boat = null;
			workplace.setHaulBoat(null);
		}
		if (boat != null) {
			moor(boat, water);
		}
		// The wage and a boat only at home: the village chests are there
		if (home && this.supplies.busy(level, villager)) {
			return;
		}
		if (workplace.unloading()) {
			// What it brought goes into the output chests first; it waits for room rather than taking it back
			if (boat != null && !boat.isEmpty()) {
				Optional<BlockPos> output = Chests.closest(jetty.validOutputs(level).stream().filter(pos -> Chests.hasRoom(level, pos)).toList(), villager);
				if (output.isPresent()) {
					this.go(level, villager, output.get(), State.UNLOAD);
				} else {
					this.rest(level, villager, jetty.pos(), jetty.validOutputs(level).isEmpty() ? "hauler.no_outputs" : "chests_full");
				}
				return;
			}
			workplace.setUnloading(false);
		}
		boolean late = !this.timeLeft(level, voyage, home);
		boolean cargo = jetty.validInputs(level).stream().anyMatch(pos -> Chests.contains(level, pos, stack -> true));
		if (home && (late || !cargo && !this.cargoThere(level, voyage) && (boat == null || boat.isEmpty()))) {
			// Nothing to carry today: takes the boat out of the water. A loaded boat stays moored until tomorrow
			if (boat != null && boat.isEmpty()) {
				this.setState(State.STOW);
			} else {
				this.rest(level, villager, workplace.pos(), late ? "hauler.done_for_today" : "hauler.nothing");
			}
			return;
		}
		if (boat == null) {
			if (boatItem(villager.getInventory()).isEmpty()) {
				if (!home || !this.supplies.busy(level, villager, WorkerSupplies.CHEST_BOAT)) {
					this.rest(level, villager, home ? workplace.pos() : voyage.destination(), "hauler.no_boat");
				}
				return;
			}
			this.setState(State.LAUNCH);
			return;
		}
		if (cargo && hasRoom(boat) && !this.boatFull) {
			Optional<BlockPos> input = Chests.closest(jetty.validInputs(level).stream().filter(pos -> Chests.contains(level, pos, stack -> true)).toList(), villager);
			if (input.isPresent()) {
				this.go(level, villager, input.get(), State.LOAD);
				return;
			}
		}
		this.setState(State.BOARD);
	}

	/**
	 * Whether a trip set out now is back by dusk (at home), or the trip home now arrives by
	 * dusk (at the other jetty; it goes home anyway, but not before it unloaded).
	 */
	private boolean timeLeft(ServerLevel level, Voyage voyage, boolean home) {
		long time = Math.floorMod(level.getOverworldClockTime(), 24000L);
		long rowing = (long) (voyage.length() / ROW_SPEED);
		return home ? time >= WORK_START && time + 2 * rowing + HANDLING_TICKS <= DUSK : true;
	}

	/** Whether something waits in the input chests of the other jetty, as far as it is loaded. */
	private boolean cargoThere(ServerLevel level, Voyage voyage) {
		Workstations.Station other = this.otherJetty(level, voyage);
		return other != null && other.validInputs(level).stream().anyMatch(pos -> Chests.contains(level, pos, stack -> true));
	}

	private Workstations.@Nullable Station otherJetty(ServerLevel level, Voyage voyage) {
		return level.isLoaded(voyage.destination()) ? Workstations.find(level, voyage.destination()).orElse(null) : null;
	}

	/** Where the boat lies on this leg of the trip: at the start of the route at home, at its end at the other jetty. */
	private BlockPos water(Voyage voyage, int leg) {
		return leg == Workstations.LEG_THERE || leg == Workstations.LEG_OUTBOUND ? voyage.end() : voyage.start();
	}

	// --- The boat ---

	private @Nullable AbstractChestBoat boat(ServerLevel level, Workstations.Station workplace) {
		UUID id = workplace.haulBoat();
		if (id == null) {
			return null;
		}
		if (level.getEntity(id) instanceof AbstractChestBoat boat && boat.isAlive()) {
			return boat;
		}
		return null;
	}

	private @Nullable AbstractChestBoat riding(Villager villager, Workstations.Station workplace) {
		return villager.getVehicle() instanceof AbstractChestBoat boat && boat.getUUID().equals(workplace.haulBoat()) ? boat : null;
	}

	/** Keeps the moored boat where it lies, against the current and against being pushed. */
	private static void moor(AbstractBoat boat, BlockPos water) {
		if (boat.getPassengers().isEmpty() && !boat.position().closerThan(floatAt(water), MOOR_DRIFT)) {
			Vec3 at = floatAt(water);
			boat.snapTo(at.x, at.y, at.z, boat.getYRot(), 0.0F);
			boat.setDeltaMovement(Vec3.ZERO);
		}
	}

	private static Vec3 floatAt(BlockPos water) {
		return new Vec3(water.getX() + 0.5, water.getY() + 1.0, water.getZ() + 0.5);
	}

	private static boolean hasRoom(Container container) {
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot);
			if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
				return true;
			}
		}
		return false;
	}

	private static ItemStack boatItem(SimpleContainer inventory) {
		for (ItemStack stack : inventory) {
			if (WorkerSupplies.CHEST_BOAT.item().test(stack) && stack.getItem() instanceof BoatItem) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	/**
	 * A place on the shore next to the water where the boat lies: there the hauler gets in and
	 * out, and stands while it puts the boat on the water and takes it out again.
	 */
	private @Nullable BlockPos shore(ServerLevel level, BlockPos water) {
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (BlockPos pos : BlockPos.betweenClosed(water.offset(-SHORE_RADIUS, -1, -SHORE_RADIUS), water.offset(SHORE_RADIUS, 2, SHORE_RADIUS))) {
			double distance = pos.distSqr(water);
			if (distance < bestDistance && StandSpots.isStandable(level, pos) && !level.getFluidState(pos).is(FluidTags.WATER)) {
				best = pos.immutable();
				bestDistance = distance;
			}
		}
		return best;
	}

	/** Walks to the shore next to the boat; true once it is there. */
	private boolean walkToShore(ServerLevel level, Villager villager, @Nullable BlockPos shore, BlockPos water) {
		if (shore == null) {
			this.rest(level, villager, villager.blockPosition(), "hauler.no_shore");
			return false;
		}
		if (StandSpots.isAt(villager, shore)) {
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(water));
			return true;
		}
		if (this.stateTicks > WALK_TIMEOUT) {
			this.rest(level, villager, villager.blockPosition(), "hauler.shore_unreachable");
		} else if (this.stateTicks % 20 == 1) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(shore, SPEED, 0));
			villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(water));
		}
		return false;
	}

	/** Puts the chest boat from its inventory on the water where the route starts. */
	private void launch(ServerLevel level, Villager villager, Workstations.Station workplace, Voyage voyage) {
		BlockPos water = this.water(voyage, workplace.leg());
		if (!this.walkToShore(level, villager, this.shore(level, water), water)) {
			return;
		}
		ItemStack item = boatItem(villager.getInventory());
		AbstractBoat created = item.isEmpty() ? null
			: ((BoatItemAccessor) item.getItem()).workingvillagers$getEntityType().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
		if (!(created instanceof AbstractChestBoat boat)) {
			this.rest(level, villager, workplace.pos(), "hauler.no_boat");
			return;
		}
		Vec3 at = floatAt(water);
		List<BlockPos> waypoints = voyage.waypoints();
		BlockPos ahead = workplace.leg() == Workstations.LEG_THERE ? waypoints.get(waypoints.size() - 2) : waypoints.get(1);
		boat.snapTo(at.x, at.y, at.z, yaw(at, floatAt(ahead)), 0.0F);
		if (!level.addFreshEntity(boat)) {
			this.rest(level, villager, workplace.pos(), "hauler.no_boat");
			return;
		}
		item.shrink(1);
		villager.getInventory().setChanged();
		workplace.setHaulBoat(boat.getUUID());
		this.setState(State.PLAN);
	}

	/** Takes the empty boat out of the water again. */
	private void stow(ServerLevel level, Villager villager, Workstations.Station workplace, Voyage voyage) {
		AbstractChestBoat boat = this.boat(level, workplace);
		if (boat == null || !boat.isEmpty()) {
			this.setState(State.PLAN);
			return;
		}
		BlockPos water = this.water(voyage, workplace.leg());
		if (!this.walkToShore(level, villager, this.shore(level, water), water)) {
			return;
		}
		boat.ejectPassengers();
		ItemStack item = boat.getPickResult();
		boat.discard();
		ItemStack rest = villager.getInventory().addItem(item);
		if (!rest.isEmpty()) {
			villager.spawnAtLocation(level, rest);
		}
		workplace.setHaulBoat(null);
		this.rest(level, villager, workplace.pos(), "hauler.nothing");
	}

	/** Gets into the boat at the shore and sets out. */
	private void board(ServerLevel level, Villager villager, Workstations.Station workplace, Voyage voyage) {
		AbstractChestBoat boat = this.boat(level, workplace);
		if (boat == null) {
			this.setState(State.PLAN);
			return;
		}
		int leg = workplace.leg();
		BlockPos water = this.water(voyage, leg);
		moor(boat, water);
		if (!this.walkToShore(level, villager, this.shore(level, water), water)) {
			return;
		}
		// Whatever got in while it lay there gets out
		boat.ejectPassengers();
		if (!villager.startRiding(boat)) {
			this.rest(level, villager, villager.blockPosition(), "hauler.boat_unreachable");
			return;
		}
		boolean homeward = leg == Workstations.LEG_THERE;
		workplace.setLeg(homeward ? Workstations.LEG_INBOUND : Workstations.LEG_OUTBOUND);
		this.setOff(villager, boat, voyage, homeward);
	}

	/** Rows on from where the boat is: towards the waypoint after the closest one. */
	private void setOff(Villager villager, AbstractChestBoat boat, Voyage voyage, boolean homeward) {
		List<BlockPos> waypoints = voyage.waypoints();
		int closest = 0;
		for (int i = 1; i < waypoints.size(); i++) {
			if (floatAt(waypoints.get(i)).distanceToSqr(boat.position()) < floatAt(waypoints.get(closest)).distanceToSqr(boat.position())) {
				closest = i;
			}
		}
		this.homeward = homeward;
		this.boatFull = false;
		this.next = Mth.clamp(homeward ? closest - 1 : closest + 1, 0, waypoints.size() - 1);
		this.lastPosition = boat.position();
		this.stuckChecks = 0;
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		this.setState(State.ROW);
	}

	/** Rows from waypoint to waypoint; at the last one it gets out at the shore. */
	private void row(ServerLevel level, Villager villager, Workstations.Station workplace, Voyage voyage) {
		AbstractChestBoat boat = this.riding(villager, workplace);
		if (boat == null) {
			this.setState(State.PLAN);
			return;
		}
		List<BlockPos> waypoints = voyage.waypoints();
		int last = this.homeward ? 0 : waypoints.size() - 1;
		this.next = Mth.clamp(this.next, 0, waypoints.size() - 1);
		BlockPos waypoint = waypoints.get(this.next);
		// The water ahead is not loaded (nobody nearby, no chunk loading): it waits there
		this.waitingForChunks = !level.isLoaded(waypoint);
		if (this.waitingForChunks) {
			boat.setDeltaMovement(0.0, boat.getDeltaMovement().y, 0.0);
			return;
		}
		Vec3 to = floatAt(waypoint);
		double dx = to.x - boat.getX();
		double dz = to.z - boat.getZ();
		double distance = Math.sqrt(dx * dx + dz * dz);
		if (distance < (this.next == last ? 1.3 : WAYPOINT_REACH)) {
			if (this.next == last) {
				boat.setDeltaMovement(0.0, boat.getDeltaMovement().y, 0.0);
				this.arrive(level, villager, workplace, voyage, boat);
				return;
			}
			this.next += this.homeward ? -1 : 1;
			this.stuckChecks = 0;
			return;
		}
		boat.setYRot(yaw(boat.position(), to));
		double speed = this.next == last ? Math.min(ROW_SPEED, distance / 4.0 + 0.04) : ROW_SPEED;
		boat.setDeltaMovement(dx / distance * speed, boat.getDeltaMovement().y, dz / distance * speed);
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(waypoint));
		if (this.stateTicks % PROGRESS_TICKS == 0) {
			Vec3 before = this.lastPosition;
			this.lastPosition = boat.position();
			if (before != null && before.distanceTo(boat.position()) < 1.0) {
				this.stuckChecks++;
				if (this.stuckChecks == STUCK_CHECKS && this.next != last) {
					// Caught on a bank: heads for the waypoint after
					this.next += this.homeward ? -1 : 1;
				}
			} else {
				this.stuckChecks = 0;
			}
		}
	}

	/** At the end of the route: gets out at the shore, the boat stays moored there. */
	private void arrive(ServerLevel level, Villager villager, Workstations.Station workplace, Voyage voyage, AbstractChestBoat boat) {
		int leg = this.homeward ? Workstations.LEG_HOME : Workstations.LEG_THERE;
		workplace.setLeg(leg);
		workplace.setUnloading(true);
		this.getOut(villager, boat, this.shore(level, this.water(voyage, leg)));
	}

	private void getOut(Villager villager, AbstractBoat boat, @Nullable BlockPos shore) {
		villager.stopRiding();
		if (shore != null) {
			villager.teleportTo(shore.getX() + 0.5, shore.getY(), shore.getZ() + 0.5);
		}
		boat.setDeltaMovement(Vec3.ZERO);
		this.setState(State.PLAN);
	}

	/** The yaw of a boat heading from one point to another. */
	private static float yaw(Vec3 from, Vec3 to) {
		return (float) (Mth.atan2(-(to.x - from.x), to.z - from.z) * Mth.RAD_TO_DEG);
	}

	// --- Loading and unloading ---

	private void go(ServerLevel level, Villager villager, BlockPos chest, State state) {
		this.trip = ChestTrip.to(level, villager, chest);
		this.moved = 0;
		this.setState(state);
	}

	/** At a chest of the jetty it is at: loads the boat from an input chest, or unloads it into an output chest. */
	private void visit(ServerLevel level, Villager villager, Workstations.Station workplace, Voyage voyage) {
		AbstractChestBoat boat = this.boat(level, workplace);
		if (this.trip == null || boat == null) {
			if (this.trip != null) {
				this.trip.close(villager);
				this.trip = null;
			}
			this.setState(State.PLAN);
			return;
		}
		moor(boat, this.water(voyage, workplace.leg()));
		boolean loading = this.state == State.LOAD;
		ChestTrip.Result result = this.trip.tick(level, villager, container -> {
			this.moved = loading ? move(container, boat) : move(boat, container);
			if (loading && this.moved > 0) {
				((HiredVillager) villager).workingvillagers$addWorkDone(ChopTrees.today(level), this.moved);
			}
		});
		if (result == ChestTrip.Result.RUNNING) {
			return;
		}
		this.trip = null;
		if (result == ChestTrip.Result.FAILED) {
			this.rest(level, villager, villager.blockPosition(), "chest_unreachable");
		} else if (this.moved == 0 && !loading) {
			// The chest only had room for other items
			this.rest(level, villager, villager.blockPosition(), "chests_full");
		} else {
			// Nothing fit into the boat (any more): off it goes with what it has
			this.boatFull |= loading && this.moved == 0;
			this.setState(State.PLAN);
		}
	}

	/** Moves what fits from one container into the other, like a hopper. Returns how many stacks moved (also in part). */
	private static int move(Container from, Container to) {
		int moved = 0;
		for (int slot = 0; slot < from.getContainerSize(); slot++) {
			ItemStack stack = from.getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}
			ItemStack rest = Chests.store(to, stack.copy());
			if (rest.getCount() != stack.getCount()) {
				from.setItem(slot, rest);
				moved++;
			}
		}
		from.setChanged();
		to.setChanged();
		return moved;
	}

	// --- Waiting and the status ---

	/** Waits a while next to the given place (its jetty, or where it is). */
	private void rest(ServerLevel level, Villager villager, BlockPos near, String reason, Object... args) {
		Optional<BlockPos> spot = StandSpots.find(level, near, villager);
		if (spot.isPresent() && !StandSpots.isAt(villager, spot.get()) && spot.get().closerToCenterThan(villager.position(), 32.0)) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(spot.get(), SPEED, 0));
		}
		this.waitReason = reason;
		this.waitArgs = args;
		this.setState(State.WAIT);
	}

	private void report(Villager villager) {
		switch (this.state) {
			// Still planning: the wage and supplies set the status (see WorkerSupplies#busy)
			case PLAN -> { }
			case BOARD -> WorkerStatus.task(villager, "hauler.board");
			case LAUNCH -> WorkerStatus.task(villager, "hauler.launch");
			case STOW -> WorkerStatus.task(villager, "hauler.stow");
			case LOAD -> WorkerStatus.task(villager, "hauler.load");
			case UNLOAD -> WorkerStatus.task(villager, "hauler.unload");
			case ROW -> {
				if (this.waitingForChunks) {
					WorkerStatus.task(villager, "hauler.not_loaded");
				} else if (this.stuckChecks >= STUCK_CHECKS) {
					WorkerStatus.task(villager, "hauler.blocked");
				} else {
					WorkerStatus.task(villager, this.homeward ? "hauler.row_home" : "hauler.row_out");
				}
			}
			case WAIT -> WorkerStatus.task(villager, this.waitReason, this.waitArgs);
		}
	}

	private void setState(State state) {
		this.state = state;
		this.stateTicks = 0;
		this.waitingForChunks = false;
	}
}
