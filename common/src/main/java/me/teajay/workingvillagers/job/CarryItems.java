package me.teajay.workingvillagers.job;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.function.Consumer;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.carrier.CarrierData;
import me.teajay.workingvillagers.carrier.Carriers;
import me.teajay.workingvillagers.carrier.Route;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The work of the carrier: go through its routes in turn, fill the basket at the source of a
 * route that has something to carry (and room at its target), bring it to the target. What
 * does not fit into the target goes back to the source. Before work it takes its wage (see
 * {@link WorkerSupplies}).
 *
 * <p>The basket's contents are stored with the villager, so nothing is lost when the work is
 * interrupted; the next work session starts by delivering them.
 */
public class CarryItems extends Behavior<Villager> {
	private static final float SPEED = 0.5F;
	private static final int IDLE_TICKS = 100;
	/** While idle, the carrier strolls around within this distance of its first source. */
	private static final int IDLE_RANGE = 6;
	private static final float STROLL_SPEED = 0.4F;

	private enum State { PLAN, FETCH, DELIVER, RETURN, WAIT }

	private State state = State.PLAN;
	private int stateTicks;
	private @Nullable ChestTrip trip;
	/** Why the carrier waits, for the status in the menu. */
	private String waitReason = "carrier.nothing";
	/** The route to look at first next time, so all routes get their turn. */
	private int nextRoute;
	private final WorkerSupplies supplies = new WorkerSupplies();

	public CarryItems() {
		super(ImmutableMap.of(), 24000);
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
		return this.canWork(villager);
	}

	@Override
	protected boolean canStillUse(ServerLevel level, Villager villager, long timestamp) {
		return this.canWork(villager) && villager.getBrain().isActive(Activity.WORK);
	}

	private boolean canWork(Villager villager) {
		CarrierData data = ((HiredVillager) villager).workingvillagers$getCarrier();
		return Carriers.isCarrier(villager) && data != null && !data.routes().isEmpty();
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
		this.supplies.stop();
		if (this.trip != null) {
			this.trip.close(villager);
			this.trip = null;
		}
	}

	@Override
	protected void tick(ServerLevel level, Villager villager, long timestamp) {
		CarrierData data = ((HiredVillager) villager).workingvillagers$getCarrier();
		if (data == null) {
			return;
		}
		this.stateTicks++;
		if (this.state == State.PLAN && this.supplies.busy(level, villager, null)) {
			return; // wage first, the chest sets the status
		}
		switch (this.state) {
			case PLAN -> this.plan(level, villager, data);
			case FETCH -> this.visit(level, villager, container -> {
				Route route = loadRoute(data);
				int stacks = route == null ? 0 : Math.max(1, Chests.emptySlots(level, route.target()));
				takeStacks(container, data.basket(), Math.min(stacks, Config.STACKS_PER_TRIP.get()));
			});
			case DELIVER -> this.visit(level, villager, container -> Chests.deposit(data.basket(), container, stack -> true));
			case RETURN -> this.visit(level, villager, container -> Chests.deposit(data.basket(), container, stack -> true));
			case WAIT -> {
				if (this.stateTicks >= IDLE_TICKS) {
					this.setState(State.PLAN);
				} else if (this.stateTicks == IDLE_TICKS / 2 && villager.getRandom().nextBoolean()) {
					this.stroll(villager, data.routes().getFirst().source());
				}
			}
		}
		this.report(villager, data);
	}

	/** Tells the menu what the carrier is doing. */
	private void report(Villager villager, CarrierData data) {
		Route route = loadRoute(data);
		Component title = route == null ? Component.literal("?") : route.title(data.loadRoute());
		switch (this.state) {
			case PLAN, WAIT -> WorkerStatus.task(villager, this.state == State.WAIT ? this.waitReason : "carrier.nothing");
			case FETCH -> WorkerStatus.task(villager, "carrier.fetch", title);
			case DELIVER -> WorkerStatus.task(villager, "carrier.deliver", title);
			case RETURN -> WorkerStatus.task(villager, "carrier.return", title);
		}
	}

	private void plan(ServerLevel level, Villager villager, CarrierData data) {
		List<Route> routes = data.routes();
		if (!data.basket().isEmpty()) {
			// Something left from last time: deliver it first
			Route route = loadRoute(data);
			if (route == null) {
				route = routes.getFirst();
				data.setLoadRoute(0);
			}
			this.go(level, villager, route.target(), State.DELIVER);
			return;
		}
		for (int i = 0; i < routes.size(); i++) {
			int index = (this.nextRoute + i) % routes.size();
			Route route = routes.get(index);
			if (Chests.contains(level, route.source(), stack -> true) && Chests.hasRoom(level, route.target())) {
				data.setLoadRoute(index);
				this.nextRoute = index + 1;
				this.go(level, villager, route.source(), State.FETCH);
				return;
			}
		}
		// Nothing to carry: stay around the first source and look again later
		this.waitReason = "carrier.nothing";
		this.stroll(villager, routes.getFirst().source());
		this.setState(State.WAIT);
	}

	/** Walks back to the source if it is too far away, otherwise to a random spot near it. */
	private void stroll(Villager villager, BlockPos source) {
		if (!source.closerToCenterThan(villager.position(), IDLE_RANGE)) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(source, SPEED, 3));
			return;
		}
		Vec3 spot = LandRandomPos.getPos(villager, IDLE_RANGE / 2, 2);
		if (spot != null && source.closerToCenterThan(spot, IDLE_RANGE)) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(spot, STROLL_SPEED, 0));
		}
	}

	private void go(ServerLevel level, Villager villager, BlockPos chest, State state) {
		this.trip = ChestTrip.to(level, villager, chest);
		this.setState(state);
	}

	private void visit(ServerLevel level, Villager villager, Consumer<Container> transfer) {
		if (this.trip == null) {
			this.setState(State.PLAN);
			return;
		}
		ChestTrip.Result result = this.trip.tick(level, villager, transfer);
		if (result == ChestTrip.Result.RUNNING) {
			return;
		}
		this.trip = null;
		CarrierData data = ((HiredVillager) villager).workingvillagers$getCarrier();
		if (result == ChestTrip.Result.FAILED || data == null) {
			this.waitReason = "chest_unreachable";
			this.setState(State.WAIT);
		} else if (this.state == State.DELIVER && !data.basket().isEmpty() && loadRoute(data) != null) {
			// The target is full: bring the rest back
			this.go(level, villager, loadRoute(data).source(), State.RETURN);
		} else if (this.state == State.RETURN && !data.basket().isEmpty()) {
			this.waitReason = "chests_full";
			this.setState(State.WAIT);
		} else {
			this.setState(State.PLAN);
		}
	}

	private static @Nullable Route loadRoute(CarrierData data) {
		int index = data.loadRoute();
		return index >= 0 && index < data.routes().size() ? data.routes().get(index) : null;
	}

	/**
	 * Moves up to {@code maxStacks} stacks from the container into the basket.
	 */
	private static void takeStacks(Container container, SimpleContainer basket, int maxStacks) {
		int stacks = 0;
		for (int slot = 0; slot < container.getContainerSize() && stacks < maxStacks; slot++) {
			ItemStack stack = container.getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}
			ItemStack rest = basket.addItem(stack.copy());
			if (rest.getCount() == stack.getCount()) {
				break; // basket full
			}
			container.setItem(slot, rest);
			stacks++;
		}
	}

	private void setState(State state) {
		this.state = state;
		this.stateTicks = 0;
	}
}
