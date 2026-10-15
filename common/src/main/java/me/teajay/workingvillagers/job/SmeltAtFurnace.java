package me.teajay.workingvillagers.job;

import com.google.common.collect.ImmutableMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import org.jspecify.annotations.Nullable;

/**
 * The work of the smith at a blast furnace: fetch what can be smelted and fuel from the
 * input chests, load the blast furnace, take out what it made and bring that to an output
 * chest. The blast furnace itself does the smelting, at its usual speed; the experience
 * stays in it, the player gets it when taking something out.
 *
 * <p>There is no order: the smith smelts everything the blast furnace accepts. Fuel is
 * everything that burns (coal, charcoal, blocks of coal, ...). Without an input and an
 * output chest the smith does not work.
 */
public class SmeltAtFurnace extends Behavior<Villager> {
	private static final float SPEED = 0.5F;
	private static final int SLOT_INPUT = 0;
	private static final int SLOT_FUEL = 1;
	private static final int SLOT_RESULT = 2;
	private static final int WALK_TIMEOUT = 400;
	/** Ticks at the furnace before the items are moved. */
	private static final int TEND_TICKS = 20;
	private static final int IDLE_TICKS = 100;
	/** Fallback if there is no free spot in front of the furnace. */
	private static final double FURNACE_REACH = 2.0;
	/** The smith hands in what it carries once it has this much, even if there is more to do. */
	private static final int DELIVER_AT = 32;
	private static final long RECIPE_REFRESH_TICKS = 1200L;

	private enum State { PLAN, FETCH, WALK_TO_FURNACE, TEND, DELIVER, WAIT }

	private State state = State.PLAN;
	private int stateTicks;
	private @Nullable ChestTrip trip;
	private @Nullable BlockPos furnaceSpot;
	/** What the current trip to an input chest fetches: material (matching the furnace) and fuel. */
	private @Nullable Predicate<ItemStack> fetchOre;
	private boolean fetchFuel;
	private String waitReason = "smith.no_ore";
	private final WorkerSupplies supplies = new WorkerSupplies();
	/** Whether the blast furnace can smelt an item, looked up once per item. */
	private final Map<Item, Boolean> smeltable = new HashMap<>();
	private long smeltableTime;

	public SmeltAtFurnace() {
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
		if (!villager.getVillagerData().profession().is(WorkingVillagers.SMITH_PROFESSION)) {
			return false;
		}
		Workstations.Station station = this.getWorkplace(level, villager);
		return station != null && !station.validInputs(level).isEmpty() && !station.validOutputs(level).isEmpty();
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
		Workstations.Station station = this.getWorkplace(level, villager);
		Container furnace = station == null ? null : Workstations.getContainer(level, station.pos());
		if (furnace == null || furnace.getContainerSize() <= SLOT_RESULT) {
			return;
		}
		this.stateTicks++;
		if (this.state == State.PLAN && this.supplies.busy(level, villager, null)) {
			return; // wage first, the chest sets the status
		}
		if (timestamp - this.smeltableTime > RECIPE_REFRESH_TICKS) {
			this.smeltable.clear();
			this.smeltableTime = timestamp;
		}

		switch (this.state) {
			case PLAN -> this.plan(level, villager, station, furnace);
			case FETCH -> this.fetch(level, villager, furnace);
			case WALK_TO_FURNACE -> this.walkToFurnace(villager, station);
			case TEND -> this.tend(level, villager, station, furnace);
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
		switch (this.state) {
			case PLAN, FETCH -> WorkerStatus.task(villager, "smith.fetch");
			case WALK_TO_FURNACE, TEND -> WorkerStatus.task(villager, "smith.load");
			case DELIVER -> WorkerStatus.task(villager, "deliver");
			case WAIT -> WorkerStatus.task(villager, this.waitReason);
		}
	}

	private boolean isSmeltable(ServerLevel level, ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		return this.smeltable.computeIfAbsent(stack.getItem(), item ->
			level.recipeAccess().getRecipeFor(RecipeType.BLASTING, new SingleRecipeInput(stack), level).isPresent());
	}

	/** Everything that burns, but nothing the smith would rather smelt. */
	private boolean isFuel(ServerLevel level, ItemStack stack) {
		return stack.has(DataComponents.COOKING_FUEL) && !this.isSmeltable(level, stack);
	}

	/** What the smith hands in: the results and anything else that is neither material nor food. */
	private Predicate<ItemStack> isProduct(ServerLevel level) {
		return stack -> Chests.isHandedIn(stack) && !this.isSmeltable(level, stack) && !this.isFuel(level, stack);
	}

	private void plan(ServerLevel level, Villager villager, Workstations.Station station, Container furnace) {
		SimpleContainer inventory = villager.getInventory();
		Predicate<ItemStack> isProduct = this.isProduct(level);
		int products = count(inventory, isProduct);
		ItemStack result = furnace.getItem(SLOT_RESULT);
		if (products >= DELIVER_AT || products > 0 && !result.isEmpty() && !inventory.canAddItem(result)) {
			this.setState(State.DELIVER);
			return;
		}
		if (this.hasWorkAtFurnace(level, inventory, furnace)) {
			this.furnaceSpot = StandSpots.find(level, station.pos(), villager).orElse(null);
			this.setState(State.WALK_TO_FURNACE);
			return;
		}

		ItemStack input = furnace.getItem(SLOT_INPUT);
		boolean carriesOre = inventory.hasAnyMatching(stack -> this.isSmeltable(level, stack));
		boolean wantOre = !carriesOre && input.getCount() < input.getMaxStackSize();
		Predicate<ItemStack> isOre = stack -> this.isSmeltable(level, stack) && (input.isEmpty() || ItemStack.isSameItemSameComponents(stack, input));
		Optional<BlockPos> oreChest = wantOre ? this.closestInput(level, villager, station, isOre) : Optional.empty();
		boolean smelting = !input.isEmpty() || carriesOre || oreChest.isPresent();
		boolean wantFuel = smelting && count(inventory, stack -> this.isFuel(level, stack)) < Config.FUEL_BATCH.get()
			&& furnace.getItem(SLOT_FUEL).getCount() < Config.FUEL_BATCH.get();
		Optional<BlockPos> fuelChest = wantFuel ? this.closestInput(level, villager, station, stack -> this.isFuel(level, stack)) : Optional.empty();

		Optional<BlockPos> chest = oreChest.isPresent() ? oreChest : fuelChest;
		if (chest.isPresent()) {
			this.trip = ChestTrip.to(level, villager, chest.get());
			this.fetchOre = oreChest.isPresent() ? isOre : null;
			this.fetchFuel = wantFuel;
			this.setState(State.FETCH);
		} else if (products > 0) {
			this.setState(State.DELIVER);
		} else {
			boolean lit = level.getBlockState(station.pos()).getOptionalValue(AbstractFurnaceBlock.LIT).orElse(false);
			this.idle(villager, station, input.isEmpty() ? "smith.no_ore" : lit ? "smith.smelting" : "smith.no_fuel");
		}
	}

	/**
	 * Whether there is something to do at the blast furnace: take out results (or an empty
	 * bucket from the fuel slot), or put in carried material or fuel that fits.
	 */
	private boolean hasWorkAtFurnace(ServerLevel level, SimpleContainer inventory, Container furnace) {
		ItemStack fuel = furnace.getItem(SLOT_FUEL);
		if (!furnace.getItem(SLOT_RESULT).isEmpty() || !fuel.isEmpty() && !this.isFuel(level, fuel)) {
			return true;
		}
		ItemStack input = furnace.getItem(SLOT_INPUT);
		boolean ore = inventory.hasAnyMatching(stack -> this.isSmeltable(level, stack) && fits(stack, input));
		boolean moreFuel = (ore || !input.isEmpty()) && fuel.getCount() < Config.FUEL_BATCH.get()
			&& inventory.hasAnyMatching(stack -> this.isFuel(level, stack) && fits(stack, fuel));
		return ore || moreFuel;
	}

	private static boolean fits(ItemStack stack, ItemStack slot) {
		return slot.isEmpty() || ItemStack.isSameItemSameComponents(stack, slot) && slot.getCount() < slot.getMaxStackSize();
	}

	private Optional<BlockPos> closestInput(ServerLevel level, Villager villager, Workstations.Station station, Predicate<ItemStack> filter) {
		return station.validInputs(level).stream()
			.filter(pos -> Chests.contains(level, pos, filter))
			.min(Comparator.comparingDouble(pos -> pos.distToCenterSqr(villager.position())));
	}

	private void fetch(ServerLevel level, Villager villager, Container furnace) {
		if (this.trip == null) {
			this.setState(State.PLAN);
			return;
		}
		ChestTrip.Result result = this.trip.tick(level, villager, container -> {
			SimpleContainer inventory = villager.getInventory();
			if (this.fetchOre != null) {
				Chests.take(container, inventory, this.fetchOre, Config.SMELT_BATCH.get());
			}
			if (this.fetchFuel) {
				int missing = Config.FUEL_BATCH.get() - count(inventory, stack -> this.isFuel(level, stack));
				if (missing > 0) {
					Chests.take(container, inventory, stack -> this.isFuel(level, stack), missing);
				}
			}
		});
		if (result != ChestTrip.Result.RUNNING) {
			this.trip = null;
			this.waitReason = "chest_unreachable";
			this.setState(result == ChestTrip.Result.DONE ? State.PLAN : State.WAIT);
		}
	}

	private void walkToFurnace(Villager villager, Workstations.Station station) {
		boolean there = this.furnaceSpot != null ? StandSpots.isAt(villager, this.furnaceSpot) : station.pos().closerToCenterThan(villager.position(), FURNACE_REACH);
		if (there) {
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			this.setState(State.TEND);
		} else if (this.stateTicks > WALK_TIMEOUT) {
			this.waitReason = "smith.furnace_unreachable";
			this.setState(State.WAIT);
		} else if (this.stateTicks % 20 == 1) {
			BlockPos target = this.furnaceSpot != null ? this.furnaceSpot : station.pos();
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, SPEED, this.furnaceSpot != null ? 0 : 1));
			villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(station.pos()));
		}
	}

	/** Takes out the results, puts in fuel and material. */
	private void tend(ServerLevel level, Villager villager, Workstations.Station station, Container furnace) {
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(station.pos()));
		if (this.stateTicks < TEND_TICKS) {
			return;
		}
		SimpleContainer inventory = villager.getInventory();
		takeOut(furnace, SLOT_RESULT, inventory);
		ItemStack fuel = furnace.getItem(SLOT_FUEL);
		if (!fuel.isEmpty() && !this.isFuel(level, fuel)) {
			takeOut(furnace, SLOT_FUEL, inventory); // the empty bucket of a lava bucket
		}
		putIn(inventory, furnace, SLOT_FUEL, stack -> this.isFuel(level, stack));
		putIn(inventory, furnace, SLOT_INPUT, stack -> this.isSmeltable(level, stack));
		furnace.setChanged();
		villager.swingForAttack(InteractionHand.MAIN_HAND);
		level.playSound(null, station.pos(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 0.6F, 1.0F);
		this.setState(State.PLAN);
	}

	private static void takeOut(Container furnace, int slot, SimpleContainer inventory) {
		ItemStack stack = furnace.getItem(slot);
		if (!stack.isEmpty()) {
			furnace.setItem(slot, inventory.addItem(stack.copy()));
		}
	}

	/** Moves matching items from the inventory into the furnace slot, as many as fit. */
	private static void putIn(SimpleContainer inventory, Container furnace, int target, Predicate<ItemStack> filter) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			ItemStack inFurnace = furnace.getItem(target);
			if (stack.isEmpty() || !filter.test(stack) || !fits(stack, inFurnace)) {
				continue;
			}
			int moved = Math.min(stack.getCount(), (inFurnace.isEmpty() ? stack.getMaxStackSize() : inFurnace.getMaxStackSize()) - inFurnace.getCount());
			furnace.setItem(target, stack.copyWithCount(inFurnace.getCount() + moved));
			stack.shrink(moved);
			inventory.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
		}
	}

	private void deliver(ServerLevel level, Villager villager, Workstations.Station station) {
		Predicate<ItemStack> isProduct = this.isProduct(level);
		if (this.trip == null) {
			if (!villager.getInventory().hasAnyMatching(isProduct)) {
				this.setState(State.PLAN);
				return;
			}
			Optional<BlockPos> chest = Chests.closest(station.validOutputs(level).stream().filter(pos -> Chests.hasRoom(level, pos)).toList(), villager);
			if (chest.isEmpty()) {
				this.idle(villager, station, "chests_full");
				return;
			}
			this.trip = ChestTrip.to(level, villager, chest.get());
		}
		ChestTrip.Result result = this.trip.tick(level, villager, container -> Chests.deposit(villager.getInventory(), container, isProduct));
		if (result != ChestTrip.Result.RUNNING) {
			this.trip = null;
			this.waitReason = "chest_unreachable";
			this.setState(result == ChestTrip.Result.DONE ? State.PLAN : State.WAIT);
		}
	}

	/** Nothing to do right now: wait at the blast furnace. */
	private void idle(Villager villager, Workstations.Station station, String reason) {
		this.waitReason = reason;
		villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(station.pos(), SPEED, 2));
		this.setState(State.WAIT);
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

	private void setState(State state) {
		this.state = state;
		this.stateTicks = 0;
	}
}
