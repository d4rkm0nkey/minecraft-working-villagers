package me.teajay.workingvillagers.job;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.crafting.BrewingPaths;
import me.teajay.workingvillagers.mixin.BrewingStandBlockEntityAccessor;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The work of the apothecary at the brewing stand: it makes the ordered product (glass
 * bottles, water bottles or a potion) and keeps a stock of it in the output chests.
 *
 * <ul>
 * <li>Glass bottles: 3 glass make 3 bottles, like the vanilla recipe.</li>
 * <li>Water bottles: it fills glass bottles at water near the brewing stand.</li>
 * <li>Potions: it brews at the real brewing stand, 3 bottles at a time, step by step along
 * the brewing recipes (water, awkward potion, potion of healing, ...; see
 * {@link BrewingPaths}). Fuel (blaze powder) and reagents come from the input chests.</li>
 * </ul>
 *
 * <p>Besides, it heals injured hired villagers around the brewing stand with potions of
 * healing (from its inventory or the chests of the station), and it harvests and replants
 * nether wart on soul sand near the brewing stand.
 */
public class BrewPotions extends Behavior<Villager> {
	private static final float SPEED = 0.5F;
	private static final int WALK_TIMEOUT = 400;
	private static final int WAIT_TICKS = 100;
	/** Three glass bottles per second. */
	private static final int CRAFT_TICKS = 20;
	/** After fetching from the chests found nothing, how long it does not try again. */
	private static final long FETCH_COOLDOWN = 600L;
	/** The brewing stand holds three bottles. */
	private static final int BATCH = 3;
	/** How long a loaded brewing stand may take to start brewing, and to finish. */
	private static final int START_TICKS = 40;
	private static final int BREW_TIMEOUT = 800;
	/** How much health a worker has to miss to be healed (two hearts). */
	private static final float INJURED = 4.0F;
	/** How long a worker that could not be reached is left alone. */
	private static final long HEAL_RETRY = 1200L;

	private enum State { PLAN, FETCH, WALK, CRAFT, WATER, BREW, COLLECT, DELIVER, HEAL, TEND, WAIT }

	/** What it does once it stands at the brewing stand. */
	private enum AtStand { CRAFT, BREW, COLLECT }

	private State state = State.PLAN;
	private AtStand atStand = AtStand.BREW;
	private int stateTicks;
	private String waitReason = "apothecary.no_order";
	private Object[] waitArgs = new Object[0];
	private @Nullable ChestTrip trip;
	private @Nullable BlockPos fetchFrom;
	/** Whether the fetch is for a potion of healing (from any chest of the station). */
	private boolean fetchHeal;
	private long fetchFailedAt = Long.MIN_VALUE / 2;
	private long tendFailedAt = Long.MIN_VALUE / 2;
	private @Nullable UUID healTarget;
	private final Map<UUID, Long> healFailed = new HashMap<>();
	private @Nullable BlockPos tendPos;
	private @Nullable BlockPos waterSpot;
	/** The brew under way at the brewing stand, null until loaded. */
	private BrewingPaths.@Nullable Step brewing;
	private final WorkerSupplies supplies = new WorkerSupplies();

	public BrewPotions() {
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
		if (!villager.getVillagerData().profession().is(WorkingVillagers.APOTHECARY_PROFESSION)) {
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
		this.brewing = null;
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
			case CRAFT -> this.craft(level, villager, workplace);
			case WATER -> this.water(level, villager, workplace);
			case BREW -> this.brew(level, villager, workplace);
			case COLLECT -> {
				this.collect(level, villager, workplace);
				this.setState(State.PLAN);
			}
			case DELIVER -> this.deliver(level, villager, workplace);
			case HEAL -> this.heal(level, villager, timestamp);
			case TEND -> this.tend(level, villager, timestamp);
			case WAIT -> {
				if (this.stateTicks >= WAIT_TICKS) {
					this.setState(State.PLAN);
				}
			}
		}
		this.report(villager);
	}

	// --- The order ---

	/** What the station is ordered, empty for none. */
	private static ItemStack order(Workstations.Station workplace) {
		return BrewingPaths.order(workplace.order(), workplace.potion());
	}

	/**
	 * The order and the way there: the bottles go through stages, stage 0 is a water bottle,
	 * the last stage the ordered potion. Null for glass bottles and potions no recipe leads to.
	 */
	private record Plan(ItemStack target, List<BrewingPaths.Step> steps) {
		ItemStack stage(int stage) {
			return stage == 0 ? BrewingPaths.water() : this.steps.get(stage - 1).output();
		}

		/** The stage of a bottle on the way to the order, -1 if it is none (also the ordered potion itself). */
		int stageOf(ItemStack stack) {
			for (int stage = this.steps.size() - 1; stage >= 0; stage--) {
				if (BrewingPaths.same(stack, this.stage(stage))) {
					return stage;
				}
			}
			return -1;
		}
	}

	private static @Nullable Plan plan(ServerLevel level, ItemStack target) {
		if (target.isEmpty() || target.is(Items.GLASS_BOTTLE)) {
			return null;
		}
		List<BrewingPaths.Step> steps = BrewingPaths.path(level, target);
		return steps == null ? null : new Plan(target, steps);
	}

	private static boolean isFuel(ItemStack stack) {
		return stack.has(DataComponents.BREWING_FUEL);
	}

	private static boolean isPotion(ItemStack stack) {
		return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
	}

	private static int count(SimpleContainer inventory, Predicate<ItemStack> filter) {
		int count = 0;
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && filter.test(stack)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static int freeSlots(SimpleContainer inventory) {
		int free = 0;
		for (ItemStack stack : inventory) {
			if (stack.isEmpty()) {
				free++;
			}
		}
		return free;
	}

	/** How many bottles of each stage it carries, the highest stage it has, -1 for none. */
	private static int highestStage(SimpleContainer inventory, Plan plan) {
		int highest = -1;
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty()) {
				highest = Math.max(highest, plan.stageOf(stack));
			}
		}
		return highest;
	}

	private static int lowestStage(SimpleContainer inventory, Plan plan) {
		int lowest = Integer.MAX_VALUE;
		for (ItemStack stack : inventory) {
			int stage = stack.isEmpty() ? -1 : plan.stageOf(stack);
			if (stage >= 0) {
				lowest = Math.min(lowest, stage);
			}
		}
		return lowest == Integer.MAX_VALUE ? 0 : lowest;
	}

	private static int inProgress(SimpleContainer inventory, Plan plan) {
		return count(inventory, stack -> plan.stageOf(stack) >= 0);
	}

	private static int inOutputs(ServerLevel level, Workstations.Station workplace, ItemStack product) {
		int count = 0;
		for (BlockPos pos : workplace.validOutputs(level)) {
			Container container = Workstations.getContainer(level, pos);
			if (container != null) {
				count += countSame(container, product);
			}
		}
		return count;
	}

	private static int countSame(Container container, ItemStack product) {
		int count = 0;
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot);
			if (BrewingPaths.same(stack, product)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	/**
	 * What the apothecary brings to the output chests: the order, and potions it does not
	 * brew with (of an earlier order, potions of healing it did not need).
	 */
	private static boolean isProduct(ItemStack stack, ItemStack target, @Nullable Plan plan) {
		if (stack.isEmpty()) {
			return false;
		}
		if (target.is(Items.GLASS_BOTTLE)) {
			return stack.is(Items.GLASS_BOTTLE) || isPotion(stack);
		}
		return isPotion(stack) && (plan == null || plan.stageOf(stack) < 0);
	}

	private static boolean hasProducts(SimpleContainer inventory, ItemStack target, @Nullable Plan plan) {
		for (ItemStack stack : inventory) {
			if (isProduct(stack, target, plan)) {
				return true;
			}
		}
		return false;
	}

	/** What it needs for the order and keeps: the rest goes back into an input chest. */
	private static boolean isNeeded(ItemStack stack, ItemStack target, @Nullable Plan plan) {
		if (WorkerSupplies.isWage(stack) || isPotion(stack) || stack.is(Items.NETHER_WART)) {
			return true; // potions are delivered, nether wart is planted
		}
		if (target.is(Items.GLASS_BOTTLE)) {
			return stack.is(Items.GLASS);
		}
		if (plan == null) {
			return false;
		}
		return stack.is(Items.GLASS) || stack.is(Items.GLASS_BOTTLE) || isFuel(stack)
			|| plan.steps().stream().anyMatch(step -> step.reagent().test(stack));
	}

	// --- Planning ---

	private void plan(ServerLevel level, Villager villager, Workstations.Station workplace, long timestamp) {
		SimpleContainer inventory = villager.getInventory();
		if (this.planHealing(level, villager, workplace, timestamp) || this.planTending(level, villager, workplace, timestamp)) {
			return;
		}
		if (this.standHasBottles(level, workplace)) {
			this.goToStand(AtStand.COLLECT);
			return;
		}
		ItemStack target = order(workplace);
		Plan plan = plan(level, target);
		if (target.isEmpty() || plan == null && !target.is(Items.GLASS_BOTTLE)) {
			if (hasProducts(inventory, target, null)) {
				this.setState(State.DELIVER);
			} else {
				this.idle(villager, workplace, target.isEmpty() ? "apothecary.no_order" : "apothecary.no_recipe");
			}
			return;
		}
		if (hasProducts(inventory, target, plan)) {
			this.setState(State.DELIVER);
			return;
		}
		int stock = workplace.stock();
		int have = inOutputs(level, workplace, target);
		if (stock > 0 && have >= stock) {
			this.idle(villager, workplace, "apothecary.stock_full", have, stock);
			return;
		}
		if (plan == null) {
			this.planBottles(level, villager, workplace, timestamp);
		} else {
			this.planPotion(level, villager, workplace, plan, timestamp);
		}
	}

	/** Glass bottles: 3 glass, 3 bottles. */
	private void planBottles(ServerLevel level, Villager villager, Workstations.Station workplace, long timestamp) {
		SimpleContainer inventory = villager.getInventory();
		if (count(inventory, stack -> stack.is(Items.GLASS)) >= BATCH) {
			this.goToStand(AtStand.CRAFT);
		} else if (!this.fetchFromInputs(level, villager, workplace, stack -> stack.is(Items.GLASS), timestamp)) {
			this.idle(villager, workplace, "apothecary.no_material", new ItemStack(Items.GLASS).getHoverName());
		}
	}

	private void planPotion(ServerLevel level, Villager villager, Workstations.Station workplace, Plan plan, long timestamp) {
		SimpleContainer inventory = villager.getInventory();
		int stage = highestStage(inventory, plan);
		boolean glassBottles = count(inventory, stack -> stack.is(Items.GLASS_BOTTLE)) > 0;
		// More water first, so the brewing stand gets three bottles
		if (glassBottles && (stage < 0 || stage == 0 && inProgress(inventory, plan) < BATCH)) {
			this.waterSpot = null;
			this.setState(State.WATER);
			return;
		}
		if (stage >= 0) {
			BrewingPaths.Step step = plan.steps().get(stage);
			boolean reagent = count(inventory, step.reagent()) > 0;
			boolean fuel = count(inventory, BrewPotions::isFuel) > 0 || this.standHasFuel(level, workplace);
			if (reagent && fuel) {
				this.goToStand(AtStand.BREW);
				return;
			}
			if (this.fetchFromInputs(level, villager, workplace, stack -> !reagent && step.reagent().test(stack) || !fuel && isFuel(stack), timestamp)) {
				return;
			}
			if (reagent) {
				this.idle(villager, workplace, "apothecary.no_fuel");
			} else {
				this.idle(villager, workplace, "apothecary.no_material", step.shownReagent().getHoverName());
			}
			return;
		}
		if (count(inventory, stack -> stack.is(Items.GLASS)) >= BATCH) {
			this.goToStand(AtStand.CRAFT);
			return;
		}
		Predicate<ItemStack> start = stack -> plan.stageOf(stack) >= 0 || stack.is(Items.GLASS_BOTTLE) || stack.is(Items.GLASS);
		if (!this.fetchFromInputs(level, villager, workplace, start, timestamp)) {
			this.idle(villager, workplace, "apothecary.no_material", new ItemStack(Items.GLASS_BOTTLE).getHoverName());
		}
	}

	/** Goes to fetch from the closest input chest with something it lacks; false if there is none (or it just tried). */
	private boolean fetchFromInputs(ServerLevel level, Villager villager, Workstations.Station workplace, Predicate<ItemStack> lacking, long timestamp) {
		if (timestamp - this.fetchFailedAt <= FETCH_COOLDOWN) {
			return false;
		}
		Optional<BlockPos> chest = Chests.closest(workplace.validInputs(level).stream().filter(pos -> Chests.contains(level, pos, lacking)).toList(), villager);
		if (chest.isEmpty()) {
			return false;
		}
		this.fetchFrom = chest.get();
		this.fetchHeal = false;
		this.setState(State.FETCH);
		return true;
	}

	private void goToStand(AtStand what) {
		this.atStand = what;
		this.setState(State.WALK);
	}

	private void fetch(ServerLevel level, Villager villager, Workstations.Station workplace, long timestamp) {
		ItemStack target = order(workplace);
		Plan plan = plan(level, target);
		if (this.trip == null) {
			if (this.fetchFrom == null) {
				this.setState(State.PLAN);
				return;
			}
			this.trip = ChestTrip.to(level, villager, this.fetchFrom);
		}
		SimpleContainer inventory = villager.getInventory();
		int[] taken = {0};
		ChestTrip.Result result = this.trip.tick(level, villager, container -> {
			if (this.fetchHeal) {
				taken[0] += Chests.take(container, inventory, BrewingPaths::heals, 1);
				return;
			}
			// Back with what the order does not need
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				ItemStack stack = inventory.getItem(slot);
				if (!stack.isEmpty() && !isNeeded(stack, target, plan)) {
					inventory.setItem(slot, HopperBlockEntity.addItem(null, container, stack, null));
				}
			}
			taken[0] += plan == null ? takeForBottles(container, inventory) : takeForPotion(level, workplace, container, inventory, plan);
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

	private static int takeForBottles(Container container, SimpleContainer inventory) {
		int glass = count(inventory, stack -> stack.is(Items.GLASS));
		return Chests.take(container, inventory, stack -> stack.is(Items.GLASS), Math.max(0, 3 * BATCH - glass));
	}

	/** Bottles for three (furthest along first, then glass bottles or glass), a reagent for each step still to go, fuel. */
	private int takeForPotion(ServerLevel level, Workstations.Station workplace, Container container, SimpleContainer inventory, Plan plan) {
		int taken = 0;
		int bottles = BATCH - inProgress(inventory, plan);
		for (int stage = plan.steps().size() - 1; stage >= 0 && bottles > 0; stage--) {
			ItemStack wanted = plan.stage(stage);
			int took = Chests.take(container, inventory, stack -> BrewingPaths.same(stack, wanted), bottles);
			bottles -= took;
			taken += took;
		}
		bottles -= count(inventory, stack -> stack.is(Items.GLASS_BOTTLE));
		if (bottles > 0) {
			int took = Chests.take(container, inventory, stack -> stack.is(Items.GLASS_BOTTLE), bottles);
			bottles -= took;
			taken += took;
		}
		int glass = count(inventory, stack -> stack.is(Items.GLASS));
		if (bottles > 0 && glass < BATCH) {
			taken += Chests.take(container, inventory, stack -> stack.is(Items.GLASS), BATCH - glass);
		}
		for (int stage = lowestStage(inventory, plan); stage < plan.steps().size(); stage++) {
			BrewingPaths.Step step = plan.steps().get(stage);
			if (count(inventory, step.reagent()) == 0) {
				taken += Chests.take(container, inventory, step.reagent(), 1);
			}
		}
		if (count(inventory, BrewPotions::isFuel) == 0 && !this.standHasFuel(level, workplace)) {
			taken += Chests.take(container, inventory, BrewPotions::isFuel, 1);
		}
		return taken;
	}

	// --- At the brewing stand ---

	private static @Nullable BrewingStandBlockEntity stand(ServerLevel level, Workstations.Station workplace) {
		return level.getBlockEntity(workplace.pos()) instanceof BrewingStandBlockEntity stand ? stand : null;
	}

	private boolean standHasFuel(ServerLevel level, Workstations.Station workplace) {
		BrewingStandBlockEntity stand = stand(level, workplace);
		return stand != null && (((BrewingStandBlockEntityAccessor) stand).workingvillagers$getFuel() > 0 || isFuel(stand.getItem(4)));
	}

	/** Whether finished bottles wait in the brewing stand (it was stopped while brewing). */
	private boolean standHasBottles(ServerLevel level, Workstations.Station workplace) {
		BrewingStandBlockEntity stand = stand(level, workplace);
		if (stand == null || ((BrewingStandBlockEntityAccessor) stand).workingvillagers$getBrewTime() > 0 || !stand.getItem(3).isEmpty()) {
			return false;
		}
		for (int slot = 0; slot < 3; slot++) {
			if (isPotion(stand.getItem(slot))) {
				return true;
			}
		}
		return false;
	}

	private void walk(ServerLevel level, Villager villager, Workstations.Station workplace) {
		Optional<BlockPos> spot = StandSpots.find(level, workplace.pos(), villager);
		boolean there = spot.map(pos -> StandSpots.isAt(villager, pos)).orElse(workplace.pos().closerToCenterThan(villager.position(), 2.5));
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(workplace.pos()));
		if (there) {
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			this.brewing = null;
			this.setState(switch (this.atStand) {
				case CRAFT -> State.CRAFT;
				case BREW -> State.BREW;
				case COLLECT -> State.COLLECT;
			});
		} else if (this.stateTicks > WALK_TIMEOUT) {
			this.idle(villager, workplace, "apothecary.stand_unreachable");
		} else if (this.stateTicks % 20 == 1) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(spot.orElse(workplace.pos()), SPEED, spot.isPresent() ? 0 : 1));
		}
	}

	/** Three glass bottles a second from three glass; for a potion only as many as it needs. */
	private void craft(ServerLevel level, Villager villager, Workstations.Station workplace) {
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(workplace.pos()));
		if (this.stateTicks % CRAFT_TICKS != 0) {
			return;
		}
		SimpleContainer inventory = villager.getInventory();
		ItemStack target = order(workplace);
		boolean bottleOrder = target.is(Items.GLASS_BOTTLE);
		int stock = workplace.stock();
		if (count(inventory, stack -> stack.is(Items.GLASS)) < BATCH || !inventory.canAddItem(new ItemStack(Items.GLASS_BOTTLE, BATCH))
			|| bottleOrder && stock > 0 && inOutputs(level, workplace, target) + count(inventory, stack -> stack.is(Items.GLASS_BOTTLE)) >= stock
			|| !bottleOrder && count(inventory, stack -> stack.is(Items.GLASS_BOTTLE)) >= BATCH) {
			this.setState(State.PLAN);
			return;
		}
		take(inventory, stack -> stack.is(Items.GLASS), BATCH);
		inventory.addItem(new ItemStack(Items.GLASS_BOTTLE, BATCH));
		if (bottleOrder) {
			((HiredVillager) villager).workingvillagers$addWorkDone(ChopTrees.today(level), BATCH);
		}
		villager.swingForAttack(InteractionHand.MAIN_HAND);
		level.playSound(null, workplace.pos(), SoundEvents.VILLAGER_WORK_CLERIC, SoundSource.BLOCKS, 0.6F, 1.0F);
	}

	/**
	 * Brews once: puts up to three bottles of the furthest stage, the reagent of the next step
	 * and, if the stand has no fuel left, blaze powder into the brewing stand, waits for it and
	 * takes the bottles back.
	 */
	private void brew(ServerLevel level, Villager villager, Workstations.Station workplace) {
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(workplace.pos()));
		BrewingStandBlockEntity stand = stand(level, workplace);
		Plan plan = plan(level, order(workplace));
		if (stand == null || plan == null) {
			this.setState(State.PLAN);
			return;
		}
		BrewingStandBlockEntityAccessor data = (BrewingStandBlockEntityAccessor) stand;
		SimpleContainer inventory = villager.getInventory();
		if (this.brewing == null) {
			if (data.workingvillagers$getBrewTime() > 0) {
				// Someone else brews: wait for it
				if (this.stateTicks > BREW_TIMEOUT) {
					this.idle(villager, workplace, "apothecary.stand_busy");
				}
				return;
			}
			if (!this.collect(level, villager, workplace)) {
				this.idle(villager, workplace, "apothecary.stand_busy");
				return;
			}
			int stage = highestStage(inventory, plan);
			if (stage < 0) {
				this.setState(State.PLAN);
				return;
			}
			BrewingPaths.Step step = plan.steps().get(stage);
			ItemStack from = plan.stage(stage);
			int loaded = 0;
			for (int slot = 0; slot < inventory.getContainerSize() && loaded < BATCH; slot++) {
				ItemStack stack = inventory.getItem(slot);
				if (BrewingPaths.same(stack, from)) {
					stand.setItem(loaded++, stack.split(1));
					inventory.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
				}
			}
			if (take(inventory, step.reagent(), 1, stand, 3) == 0) {
				this.collect(level, villager, workplace);
				this.setState(State.PLAN);
				return;
			}
			if (data.workingvillagers$getFuel() <= 0 && stand.getItem(4).isEmpty()) {
				take(inventory, BrewPotions::isFuel, 1, stand, 4);
			}
			stand.setChanged();
			this.brewing = step;
			this.stateTicks = 0;
			villager.swingForAttack(InteractionHand.MAIN_HAND);
			level.playSound(null, workplace.pos(), SoundEvents.VILLAGER_WORK_CLERIC, SoundSource.BLOCKS, 0.6F, 1.0F);
			return;
		}
		boolean brewing = data.workingvillagers$getBrewTime() > 0;
		boolean reagentLeft = this.brewing.reagent().test(stand.getItem(3));
		if (!brewing && !reagentLeft) {
			// Done: the bottles are one stage further
			int made = 0;
			for (int slot = 0; slot < 3; slot++) {
				if (BrewingPaths.same(stand.getItem(slot), plan.target())) {
					made++;
				}
			}
			this.collect(level, villager, workplace);
			if (made > 0) {
				((HiredVillager) villager).workingvillagers$addWorkDone(ChopTrees.today(level), made);
			}
			this.brewing = null;
			this.setState(State.PLAN);
		} else if (!brewing && this.stateTicks > START_TICKS || this.stateTicks > BREW_TIMEOUT) {
			// It does not brew: no fuel
			boolean fuel = this.standHasFuel(level, workplace);
			this.collect(level, villager, workplace);
			this.brewing = null;
			this.idle(villager, workplace, fuel ? "apothecary.stand_busy" : "apothecary.no_fuel");
		}
	}

	/** Takes the bottles and the reagent out of the brewing stand; false if they do not fit into the inventory. */
	private boolean collect(ServerLevel level, Villager villager, Workstations.Station workplace) {
		BrewingStandBlockEntity stand = stand(level, workplace);
		if (stand == null) {
			return true;
		}
		boolean all = true;
		for (int slot = 0; slot < 4; slot++) {
			ItemStack stack = stand.getItem(slot);
			if (!stack.isEmpty()) {
				ItemStack rest = villager.getInventory().addItem(stack);
				stand.setItem(slot, rest);
				all &= rest.isEmpty();
			}
		}
		stand.setChanged();
		return all;
	}

	private static int take(SimpleContainer inventory, Predicate<ItemStack> filter, int amount) {
		int taken = 0;
		for (int slot = 0; slot < inventory.getContainerSize() && taken < amount; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty() && filter.test(stack)) {
				int now = Math.min(amount - taken, stack.getCount());
				stack.shrink(now);
				taken += now;
				inventory.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
			}
		}
		return taken;
	}

	/** Moves up to {@code amount} matching items into the slot of the brewing stand. */
	private static int take(SimpleContainer inventory, Predicate<ItemStack> filter, int amount, Container stand, int standSlot) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty() && filter.test(stack)) {
				stand.setItem(standSlot, stack.split(amount));
				inventory.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
				return stand.getItem(standSlot).getCount();
			}
		}
		return 0;
	}

	// --- Water ---

	/** Fills glass bottles at water near the brewing stand, as many as the next brew takes. */
	private void water(ServerLevel level, Villager villager, Workstations.Station workplace) {
		if (this.waterSpot == null) {
			Optional<BlockPos> spot = findShore(level, workplace.pos(), villager);
			if (spot.isEmpty()) {
				this.idle(villager, workplace, "apothecary.no_water");
				return;
			}
			this.waterSpot = spot.get();
		}
		BlockPos water = closestWater(level, this.waterSpot);
		if (water == null) {
			this.waterSpot = null;
			return;
		}
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(water));
		if (StandSpots.isAt(villager, this.waterSpot) || water.closerToCenterThan(villager.position(), 2.0)) {
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			SimpleContainer inventory = villager.getInventory();
			ItemStack target = order(workplace);
			Plan plan = plan(level, target);
			int wanted = plan == null || plan.steps().isEmpty() ? Integer.MAX_VALUE : BATCH - inProgress(inventory, plan);
			int filled = 0;
			while (filled < wanted && count(inventory, stack -> stack.is(Items.GLASS_BOTTLE)) > 0) {
				// A water bottle needs a slot of its own, unless the last glass bottle frees one
				if (freeSlots(inventory) == 0 && count(inventory, stack -> stack.is(Items.GLASS_BOTTLE)) > 1) {
					break;
				}
				take(inventory, stack -> stack.is(Items.GLASS_BOTTLE), 1);
				inventory.addItem(BrewingPaths.water());
				filled++;
			}
			if (filled > 0) {
				level.playSound(null, villager, SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
				villager.swingForAttack(InteractionHand.MAIN_HAND);
				if (plan != null && plan.steps().isEmpty()) {
					((HiredVillager) villager).workingvillagers$addWorkDone(ChopTrees.today(level), filled);
				}
			}
			this.setState(State.PLAN);
		} else if (this.stateTicks > WALK_TIMEOUT) {
			this.idle(villager, workplace, "apothecary.no_water");
		} else if (this.stateTicks % 20 == 1) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.waterSpot, SPEED, 0));
		}
	}

	/** A spot next to water (a source block) around the brewing stand, the closest to the villager. */
	private static Optional<BlockPos> findShore(ServerLevel level, BlockPos stand, Villager villager) {
		int radius = Config.APOTHECARY_WATER_RADIUS.get();
		List<BlockPos> spots = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(stand.offset(-radius, -3, -radius), stand.offset(radius, 2, radius))) {
			if (!isWater(level, pos)) {
				continue;
			}
			for (Direction side : Direction.Plane.HORIZONTAL) {
				for (BlockPos candidate : List.of(pos.relative(side), pos.relative(side).above())) {
					if (StandSpots.isStandable(level, candidate)) {
						spots.add(candidate.immutable());
					}
				}
			}
		}
		return spots.stream().min(Comparator.comparingDouble(spot -> spot.distToCenterSqr(villager.position())));
	}

	private static @Nullable BlockPos closestWater(ServerLevel level, BlockPos spot) {
		for (Direction side : Direction.Plane.HORIZONTAL) {
			for (BlockPos pos : List.of(spot.relative(side), spot.relative(side).below())) {
				if (isWater(level, pos)) {
					return pos;
				}
			}
		}
		return null;
	}

	private static boolean isWater(ServerLevel level, BlockPos pos) {
		return level.getFluidState(pos).is(FluidTags.WATER) && level.getFluidState(pos).isSource();
	}

	// --- Healing ---

	/** Goes to heal an injured worker if there is one and a potion of healing, from its inventory or a chest of the station. */
	private boolean planHealing(ServerLevel level, Villager villager, Workstations.Station workplace, long timestamp) {
		Villager injured = this.findInjured(level, villager, workplace, timestamp);
		if (injured == null) {
			return false;
		}
		if (count(villager.getInventory(), BrewingPaths::heals) > 0) {
			this.healTarget = injured.getUUID();
			this.setState(State.HEAL);
			return true;
		}
		if (timestamp - this.fetchFailedAt <= FETCH_COOLDOWN) {
			return false;
		}
		List<BlockPos> chests = new ArrayList<>(workplace.validOutputs(level));
		chests.addAll(workplace.validInputs(level));
		Optional<BlockPos> chest = Chests.closest(chests.stream().filter(pos -> Chests.contains(level, pos, BrewingPaths::heals)).toList(), villager);
		if (chest.isEmpty()) {
			return false;
		}
		this.fetchFrom = chest.get();
		this.fetchHeal = true;
		this.setState(State.FETCH);
		return true;
	}

	private @Nullable Villager findInjured(ServerLevel level, Villager villager, Workstations.Station workplace, long timestamp) {
		this.healFailed.values().removeIf(at -> timestamp - at > HEAL_RETRY);
		int radius = Config.HEAL_RADIUS.get();
		return level.getEntitiesOfClass(Villager.class, new AABB(workplace.pos()).inflate(radius), other ->
				other != villager && other.isAlive() && ((HiredVillager) other).workingvillagers$isHired() && isInjured(other)
					&& !this.healFailed.containsKey(other.getUUID()))
			.stream()
			.min(Comparator.comparingDouble(other -> other.distanceToSqr(villager)))
			.orElse(null);
	}

	private static boolean isInjured(Villager villager) {
		return villager.getHealth() <= villager.getMaxHealth() - INJURED;
	}

	private void heal(ServerLevel level, Villager villager, long timestamp) {
		Villager target = this.healTarget != null && level.getEntity(this.healTarget) instanceof Villager found ? found : null;
		SimpleContainer inventory = villager.getInventory();
		if (target == null || !target.isAlive() || !isInjured(target) || count(inventory, BrewingPaths::heals) == 0) {
			this.setState(State.PLAN);
			return;
		}
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
		if (villager.distanceToSqr(target) < 2.5 * 2.5) {
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				ItemStack stack = inventory.getItem(slot);
				if (BrewingPaths.heals(stack)) {
					PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
					boolean drunk = stack.is(Items.POTION);
					inventory.setItem(slot, drunk ? new ItemStack(Items.GLASS_BOTTLE) : ItemStack.EMPTY);
					contents.applyToLivingEntity(target, 1.0F);
					level.playSound(null, target, drunk ? SoundEvents.GENERIC_DRINK.value() : SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
					villager.swingForAttack(InteractionHand.MAIN_HAND);
					break;
				}
			}
			this.setState(State.PLAN);
		} else if (this.stateTicks > WALK_TIMEOUT) {
			this.healFailed.put(target.getUUID(), timestamp);
			this.setState(State.PLAN);
		} else if (this.stateTicks % 20 == 1) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, SPEED, 1));
		}
	}

	// --- Nether wart ---

	/**
	 * Goes to grown nether wart near the brewing stand to harvest it, or to free soul sand to
	 * plant nether wart, if it carries more than one (one it keeps for brewing).
	 */
	private boolean planTending(ServerLevel level, Villager villager, Workstations.Station workplace, long timestamp) {
		if (timestamp - this.tendFailedAt <= FETCH_COOLDOWN) {
			return false;
		}
		boolean plant = count(villager.getInventory(), stack -> stack.is(Items.NETHER_WART)) > 1;
		int radius = Config.WART_RADIUS.get();
		BlockPos stand = workplace.pos();
		BlockPos best = null;
		for (BlockPos pos : BlockPos.betweenClosed(stand.offset(-radius, -2, -radius), stand.offset(radius, 2, radius))) {
			if ((isRipe(level.getBlockState(pos)) || plant && isPlantable(level, pos))
				&& (best == null || pos.distToCenterSqr(villager.position()) < best.distToCenterSqr(villager.position()))) {
				best = pos.immutable();
			}
		}
		if (best == null) {
			return false;
		}
		this.tendPos = best;
		this.setState(State.TEND);
		return true;
	}

	private static boolean isRipe(BlockState state) {
		return state.is(Blocks.NETHER_WART) && state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
	}

	private static boolean isPlantable(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).is(Blocks.SOUL_SAND);
	}

	private void tend(ServerLevel level, Villager villager, long timestamp) {
		BlockPos pos = this.tendPos;
		if (pos == null) {
			this.setState(State.PLAN);
			return;
		}
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(pos));
		if (pos.closerToCenterThan(villager.position(), 2.0)) {
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			SimpleContainer inventory = villager.getInventory();
			BlockState state = level.getBlockState(pos);
			if (isRipe(state)) {
				for (ItemStack drop : Block.getDrops(state, level, pos, null)) {
					ItemStack rest = inventory.addItem(drop);
					if (!rest.isEmpty()) {
						villager.spawnAtLocation(level, rest);
					}
				}
				level.setBlock(pos, Blocks.NETHER_WART.defaultBlockState(), Block.UPDATE_ALL);
				take(inventory, stack -> stack.is(Items.NETHER_WART), 1);
				level.playSound(null, pos, SoundEvents.NETHER_WART_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
				villager.swingForAttack(InteractionHand.MAIN_HAND);
			} else if (isPlantable(level, pos) && take(inventory, stack -> stack.is(Items.NETHER_WART), 1) == 1) {
				level.setBlock(pos, Blocks.NETHER_WART.defaultBlockState(), Block.UPDATE_ALL);
				level.playSound(null, pos, SoundEvents.NETHER_WART_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
				villager.swingForAttack(InteractionHand.MAIN_HAND);
			}
			this.setState(State.PLAN);
		} else if (this.stateTicks > WALK_TIMEOUT) {
			this.tendFailedAt = timestamp;
			this.setState(State.PLAN);
		} else if (this.stateTicks % 20 == 1) {
			Optional<BlockPos> spot = StandSpots.findNextTo(level, List.of(pos), villager);
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(spot.orElse(pos), SPEED, spot.isPresent() ? 0 : 1));
		}
	}

	// --- Delivering ---

	private void deliver(ServerLevel level, Villager villager, Workstations.Station workplace) {
		ItemStack target = order(workplace);
		Plan plan = plan(level, target);
		if (this.trip == null) {
			if (!hasProducts(villager.getInventory(), target, plan)) {
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
				if (isProduct(stack, target, plan)) {
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
			case PLAN, WALK, COLLECT -> WorkerStatus.task(villager, "apothecary.plan");
			case FETCH -> WorkerStatus.task(villager, "apothecary.fetch");
			case CRAFT -> WorkerStatus.task(villager, "apothecary.bottles");
			case WATER -> WorkerStatus.task(villager, "apothecary.water");
			case BREW -> WorkerStatus.task(villager, "apothecary.brew");
			case HEAL -> WorkerStatus.task(villager, "apothecary.heal");
			case TEND -> WorkerStatus.task(villager, "apothecary.wart");
			case DELIVER -> WorkerStatus.task(villager, "deliver");
			case WAIT -> WorkerStatus.task(villager, this.waitReason, this.waitArgs);
		}
	}

	private void setState(State state) {
		this.state = state;
		this.stateTicks = 0;
	}
}
