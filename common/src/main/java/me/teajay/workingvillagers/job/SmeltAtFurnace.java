package me.teajay.workingvillagers.job;

import com.google.common.collect.ImmutableMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.crafting.KilnRecipes;
import me.teajay.workingvillagers.station.StationType;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import org.jspecify.annotations.Nullable;

/**
 * The work at a furnace: fetch what can be smelted and fuel from the input chests, load the
 * furnace, take out what it made and bring that to an output chest. The furnace itself does
 * the smelting, at its usual speed; the experience stays in it, the player gets it when
 * taking something out. Without an input and an output chest the worker does not work.
 *
 * <p>The smith at the blast furnace has no order: it smelts everything the blast furnace
 * accepts. The kiln operator fires everything the kiln accepts (see {@link KilnRecipes}),
 * or only what makes the products of the kiln's order; it fires that again until it is the
 * product (cobblestone to stone to smooth stone), and stops making a product while the
 * output chests hold the stock set in the overview, or once it made as many as ordered. With
 * several products it loads the kiln for the most needed one there is material for
 * ({@link Workstations.Station#ordersByNeed}). With an order it also takes material and fuel
 * from the village chests ({@link Orders#sources}); firing everything only from its input chests.
 *
 * <p>Fuel is everything that burns (coal, charcoal, blocks of coal, ...) but nothing the
 * worker would rather smelt: with the order "charcoal" logs are material, not fuel. What
 * the worker took out of the furnace is noted, so it delivers its products even if they
 * burn (charcoal) or could be fired once more (stone).
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
	/** The worker hands in what it carries once it has this much, even if there is more to do. */
	private static final int DELIVER_AT = 32;
	private static final long RECIPE_REFRESH_TICKS = 1200L;

	/** The smith at a blast furnace or the kiln operator at a kiln. */
	public enum Kind {
		SMITH(WorkingVillagers.SMITH_PROFESSION, StationType.BLAST_FURNACE, "smith"),
		KILN(WorkingVillagers.KILN_OPERATOR_PROFESSION, StationType.KILN, "kiln");

		private final ResourceKey<VillagerProfession> profession;
		/** The furnace; the smith repairs at the smithing table instead (see {@link RepairTools}). */
		private final StationType station;
		/** The first part of the status keys, e.g. {@code smith.fetch}. */
		private final String status;

		Kind(ResourceKey<VillagerProfession> profession, StationType station, String status) {
			this.profession = profession;
			this.station = station;
			this.status = status;
		}

		private int batch() {
			return this == SMITH ? Config.SMELT_BATCH.get() : Config.KILN_BATCH.get();
		}

		private int fuelBatch() {
			return this == SMITH ? Config.FUEL_BATCH.get() : Config.KILN_FUEL_BATCH.get();
		}
	}

	private enum State { PLAN, FETCH, WALK_TO_FURNACE, TEND, DELIVER, WAIT }

	private final Kind kind;
	private State state = State.PLAN;
	private int stateTicks;
	private @Nullable ChestTrip trip;
	private @Nullable BlockPos furnaceSpot;
	/** What the current trip to an input chest fetches: material (matching the furnace) and fuel. */
	private @Nullable Predicate<ItemStack> fetchOre;
	private boolean fetchFuel;
	private String waitReason = "no_ore";
	/** The worker's level the cache of what it smelts was made for. */
	private int workerLevel;
	private Object[] waitArgs = {};
	private final WorkerSupplies supplies = new WorkerSupplies();
	/** Whether the blast furnace can smelt an item, looked up once per item. */
	private final Map<Item, Boolean> smeltable = new HashMap<>();
	private long smeltableTime;
	/** The products of the kiln's order while the worker is at work, empty for everything. */
	private List<Item> orders = List.of();
	/** The product the material in the kiln is fired for, null if none or unknown. */
	private @Nullable Item target;
	/** How much material the current trip fetches at most: what the order of the product still needs. */
	private int oreLimit = Integer.MAX_VALUE;
	/**
	 * How many of each item the worker took out of the furnace as products and still has to
	 * deliver. Not saved: after a restart they count as material again if they can be smelted.
	 */
	private final Map<Item, Integer> products = new HashMap<>();

	public SmeltAtFurnace(Kind kind) {
		super(ImmutableMap.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT), 24000);
		this.kind = kind;
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
		if (!villager.getVillagerData().profession().is(this.kind.profession)) {
			return false;
		}
		Workstations.Station station = this.getWorkplace(level, villager);
		return station != null && station.type() == this.kind.station && !station.validOutputs(level).isEmpty()
			&& (this.kind == Kind.KILN && station.hasOrder() ? Orders.hasSources(level, station) : !station.validInputs(level).isEmpty());
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
		this.supplies.stop(villager);
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
		if (this.state == State.PLAN && this.supplies.busy(level, villager)) {
			return; // wage first, the chest sets the status
		}
		if (timestamp - this.smeltableTime > RECIPE_REFRESH_TICKS) {
			this.smeltable.clear();
			this.smeltableTime = timestamp;
		}
		List<Item> orders = this.kind == Kind.KILN ? station.orders() : List.of();
		int workerLevel = villager.getVillagerData().level();
		if (workerLevel != this.workerLevel) {
			// What it fires depends on its level
			this.workerLevel = workerLevel;
			this.smeltable.clear();
		}
		if (!orders.equals(this.orders)) {
			this.orders = List.copyOf(orders);
			this.smeltable.clear();
			if (this.target != null && !orders.contains(this.target)) {
				this.target = null;
			}
		}
		this.products.entrySet().removeIf(entry -> {
			entry.setValue(Math.min(entry.getValue(), villager.getInventory().countItem(entry.getKey())));
			return entry.getValue() <= 0;
		});

		int lockedUntil = this.orders.stream().mapToInt(order -> Unlocks.level(this.kind.profession, order)).min().orElse(WorkerLevels.NOVICE);
		if (this.state == State.PLAN && workerLevel < lockedUntil) {
			this.idle(villager, station, "locked", WorkerLevels.name(lockedUntil));
		}
		switch (this.state) {
			case PLAN -> this.plan(level, villager, station, furnace);
			case FETCH -> this.fetch(level, villager);
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
			case PLAN, FETCH -> WorkerStatus.task(villager, this.kind.status + ".fetch");
			case WALK_TO_FURNACE, TEND -> WorkerStatus.task(villager, this.kind.status + ".load");
			case DELIVER -> WorkerStatus.task(villager, "deliver");
			case WAIT -> WorkerStatus.task(villager, this.waitReason, this.waitArgs);
		}
	}

	/** Whether the kiln operator's level lets it fire the item into what it makes (glazed terracotta from journeyman, see {@link Unlocks}). */
	private boolean mayFire(ServerLevel level, ItemStack stack) {
		Item result = KilnRecipes.resultOf(level, stack);
		return result == null || this.workerLevel >= Unlocks.level(this.kind.profession, result);
	}

	/**
	 * Whether the worker puts the item into the furnace: the blast furnace can smelt it, or
	 * the kiln can fire it and that makes a product of the order (at once or after more firing).
	 */
	private boolean isSmeltable(ServerLevel level, ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		return this.smeltable.computeIfAbsent(stack.getItem(), item -> switch (this.kind) {
			case SMITH -> level.recipeAccess().getRecipeFor(RecipeType.BLASTING, new SingleRecipeInput(stack), level).isPresent();
			case KILN -> KilnRecipes.accepts(level, stack) && this.mayFire(level, stack)
				&& (this.orders.isEmpty() || this.orders.stream().anyMatch(order -> KilnRecipes.leadsTo(level, stack, order)));
		});
	}

	/** Everything that burns, but nothing the worker would rather smelt. */
	private boolean isFuel(ServerLevel level, ItemStack stack) {
		return stack.has(DataComponents.COOKING_FUEL) && !this.isSmeltable(level, stack);
	}

	/**
	 * How much of each inventory slot the worker hands in: the products it took out of the
	 * furnace, and anything else that is neither material nor fuel (an empty bucket).
	 */
	private int[] amountsToHandIn(ServerLevel level, SimpleContainer inventory) {
		int[] amounts = new int[inventory.getContainerSize()];
		Map<Item, Integer> owed = new HashMap<>(this.products);
		for (int slot = 0; slot < amounts.length; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!Chests.isHandedIn(stack)) {
				continue;
			}
			if (!this.isSmeltable(level, stack) && !this.isFuel(level, stack)) {
				amounts[slot] = stack.getCount();
				continue;
			}
			int product = Math.min(stack.getCount(), owed.getOrDefault(stack.getItem(), 0));
			owed.merge(stack.getItem(), -product, Integer::sum);
			amounts[slot] = product;
		}
		return amounts;
	}

	private int productCount(ServerLevel level, SimpleContainer inventory) {
		int count = 0;
		for (int amount : this.amountsToHandIn(level, inventory)) {
			count += amount;
		}
		return count;
	}

	/** How many of the item the worker carries that are not products, so it may use them. */
	private int spare(SimpleContainer inventory, Item item) {
		return inventory.countItem(item) - this.products.getOrDefault(item, 0);
	}

	private boolean carries(SimpleContainer inventory, Predicate<ItemStack> filter) {
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && filter.test(stack) && this.spare(inventory, stack.getItem()) > 0) {
				return true;
			}
		}
		return false;
	}

	private int countSpare(SimpleContainer inventory, Predicate<ItemStack> filter) {
		Map<Item, Integer> counted = new HashMap<>();
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && filter.test(stack)) {
				counted.putIfAbsent(stack.getItem(), Math.max(0, this.spare(inventory, stack.getItem())));
			}
		}
		return counted.values().stream().mapToInt(Integer::intValue).sum();
	}

	private void plan(ServerLevel level, Villager villager, Workstations.Station station, Container furnace) {
		SimpleContainer inventory = villager.getInventory();
		int products = this.productCount(level, inventory);
		ItemStack result = furnace.getItem(SLOT_RESULT);
		if (products >= DELIVER_AT || products > 0 && !result.isEmpty() && !inventory.canAddItem(result)) {
			this.setState(State.DELIVER);
			return;
		}
		if (products == 0 && this.kind == Kind.KILN) {
			// Everything made is handed in: the orders that are done are done
			Orders.finish(level, villager, station);
		}
		if (this.hasWorkAtFurnace(level, inventory, furnace)) {
			this.furnaceSpot = StandSpots.find(level, station.pos(), villager).orElse(null);
			this.setState(State.WALK_TO_FURNACE);
			return;
		}

		List<Item> byNeed = this.orders.isEmpty() ? List.of() : station.ordersByNeed(order -> station.inOutputs(level, order));
		boolean stockFull = !this.orders.isEmpty() && byNeed.isEmpty();
		ItemStack input = furnace.getItem(SLOT_INPUT);
		boolean carriesOre = this.carries(inventory, stack -> this.isSmeltable(level, stack));
		// What is in the kiln for the product it fires for counts towards its order
		int inKiln = input.getCount() + result.getCount();
		this.oreLimit = this.target != null && byNeed.contains(this.target) && station.limited(this.target)
			? Orders.wanted(level, station, this.target) - inKiln
			: Integer.MAX_VALUE;
		boolean wantOre = !carriesOre && input.getCount() < input.getMaxStackSize() && !stockFull && this.oreLimit > 0;
		Predicate<ItemStack> isOre = stack -> this.isSmeltable(level, stack) && (input.isEmpty() || ItemStack.isSameItemSameComponents(stack, input));
		Optional<BlockPos> oreChest = Optional.empty();
		Item oreFor = null;
		if (wantOre && !input.isEmpty() || wantOre && this.orders.isEmpty()) {
			oreChest = this.closestInput(level, villager, station, isOre);
		} else if (wantOre) {
			// An empty kiln: material for the most needed product there is some for
			for (Item order : byNeed) {
				Predicate<ItemStack> leads = stack -> this.isSmeltable(level, stack) && KilnRecipes.leadsTo(level, stack, order);
				oreChest = this.closestInput(level, villager, station, leads);
				if (oreChest.isPresent()) {
					isOre = leads;
					oreFor = order;
					this.oreLimit = station.limited(order) ? Orders.wanted(level, station, order) - inKiln : Integer.MAX_VALUE;
					break;
				}
			}
		}
		boolean smelting = !input.isEmpty() || carriesOre || oreChest.isPresent();
		boolean wantFuel = smelting && this.countSpare(inventory, stack -> this.isFuel(level, stack)) < this.kind.fuelBatch()
			&& furnace.getItem(SLOT_FUEL).getCount() < this.kind.fuelBatch();
		Optional<BlockPos> fuelChest = wantFuel ? this.closestInput(level, villager, station, stack -> this.isFuel(level, stack)) : Optional.empty();

		Optional<BlockPos> chest = oreChest.isPresent() ? oreChest : fuelChest;
		if (chest.isPresent()) {
			this.trip = ChestTrip.to(level, villager, chest.get());
			this.fetchOre = oreChest.isPresent() ? isOre : null;
			if (oreFor != null) {
				this.target = oreFor;
			}
			this.fetchFuel = wantFuel;
			this.setState(State.FETCH);
		} else if (products > 0) {
			this.setState(State.DELIVER);
		} else if (stockFull && input.isEmpty()) {
			Orders.Idle idle = Orders.nothingToMake(level, station, this.kind.status);
			this.idle(villager, station, idle.key(), idle.args());
		} else {
			boolean lit = level.getBlockState(station.pos()).getOptionalValue(AbstractFurnaceBlock.LIT).orElse(false);
			this.idle(villager, station, this.kind.status + (input.isEmpty() ? ".no_ore" : lit ? ".smelting" : ".no_fuel"));
		}
	}

	/**
	 * Whether there is something to do at the furnace: take out results (or an empty bucket
	 * from the fuel slot), or put in carried material or fuel that fits.
	 */
	private boolean hasWorkAtFurnace(ServerLevel level, SimpleContainer inventory, Container furnace) {
		ItemStack fuel = furnace.getItem(SLOT_FUEL);
		if (!furnace.getItem(SLOT_RESULT).isEmpty() || !fuel.isEmpty() && !this.isFuel(level, fuel)) {
			return true;
		}
		ItemStack input = furnace.getItem(SLOT_INPUT);
		boolean ore = this.carries(inventory, stack -> this.isSmeltable(level, stack) && fits(stack, input));
		boolean moreFuel = (ore || !input.isEmpty()) && fuel.getCount() < this.kind.fuelBatch()
			&& this.carries(inventory, stack -> this.isFuel(level, stack) && fits(stack, fuel));
		return ore || moreFuel;
	}

	private static boolean fits(ItemStack stack, ItemStack slot) {
		return slot.isEmpty() || ItemStack.isSameItemSameComponents(stack, slot) && slot.getCount() < slot.getMaxStackSize();
	}

	/** The closest chest with something the filter accepts: an input chest, with an order also a village chest. */
	private Optional<BlockPos> closestInput(ServerLevel level, Villager villager, Workstations.Station station, Predicate<ItemStack> filter) {
		return (this.orders.isEmpty() ? station.validInputs(level) : Orders.sources(level, station)).stream()
			.filter(pos -> Chests.contains(level, pos, filter))
			.min(Comparator.comparingDouble(pos -> pos.distToCenterSqr(villager.position())));
	}

	private void fetch(ServerLevel level, Villager villager) {
		if (this.trip == null) {
			this.setState(State.PLAN);
			return;
		}
		ChestTrip.Result result = this.trip.tick(level, villager, container -> {
			SimpleContainer inventory = villager.getInventory();
			if (this.fetchOre != null) {
				Chests.take(container, inventory, this.fetchOre, Math.min(this.oreLimit, Math.round(this.kind.batch() * WorkerLevels.between(villager, 1.0F, 2.0F))));
			}
			if (this.fetchFuel) {
				int missing = this.kind.fuelBatch() - this.countSpare(inventory, stack -> this.isFuel(level, stack));
				if (missing > 0) {
					Chests.take(container, inventory, stack -> this.isFuel(level, stack), missing);
				}
			}
		});
		if (result != ChestTrip.Result.RUNNING) {
			this.trip = null;
			this.waitReason = "chest_unreachable";
			this.waitArgs = new Object[0];
			this.setState(result == ChestTrip.Result.DONE ? State.PLAN : State.WAIT);
		}
	}

	private void walkToFurnace(Villager villager, Workstations.Station station) {
		boolean there = this.furnaceSpot != null ? StandSpots.isAt(villager, this.furnaceSpot) : station.pos().closerToCenterThan(villager.position(), FURNACE_REACH);
		if (there) {
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			this.setState(State.TEND);
		} else if (this.stateTicks > WALK_TIMEOUT) {
			this.waitReason = this.kind.status + ".furnace_unreachable";
			this.waitArgs = new Object[0];
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
		ItemStack result = furnace.getItem(SLOT_RESULT).copy();
		int taken = takeOut(furnace, SLOT_RESULT, inventory);
		// Fired once more on the way to the product (stone for smooth stone): material, not a product
		boolean again = !result.isEmpty() && (this.target != null
			? !result.is(this.target) && KilnRecipes.leadsTo(level, result, this.target)
			: !this.orders.contains(result.getItem()) && this.orders.stream().anyMatch(order -> KilnRecipes.leadsTo(level, result, order)));
		if (taken > 0 && !again) {
			this.products.merge(result.getItem(), taken, Integer::sum);
			station.addMade(result.getItem(), taken);
			((HiredVillager) villager).workingvillagers$addWorkDone(ChopTrees.today(level), taken);
		}
		ItemStack fuel = furnace.getItem(SLOT_FUEL);
		if (!fuel.isEmpty() && !this.isFuel(level, fuel)) {
			takeOut(furnace, SLOT_FUEL, inventory); // the empty bucket of a lava bucket
		}
		this.putIn(inventory, furnace, SLOT_FUEL, stack -> this.isFuel(level, stack));
		this.putIn(inventory, furnace, SLOT_INPUT, stack -> this.isSmeltable(level, stack));
		furnace.setChanged();
		villager.swingForAttack(InteractionHand.MAIN_HAND);
		level.playSound(null, station.pos(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 0.6F, 1.0F);
		this.setState(State.PLAN);
	}

	/** Moves the slot into the inventory, as much as fits. Returns how many were moved. */
	private static int takeOut(Container furnace, int slot, SimpleContainer inventory) {
		ItemStack stack = furnace.getItem(slot);
		if (stack.isEmpty()) {
			return 0;
		}
		ItemStack rest = inventory.addItem(stack.copy());
		furnace.setItem(slot, rest);
		return stack.getCount() - rest.getCount();
	}

	/**
	 * Moves matching items from the inventory into the furnace slot, as many as fit; products
	 * stay in the inventory.
	 */
	private void putIn(SimpleContainer inventory, Container furnace, int target, Predicate<ItemStack> filter) {
		Map<Item, Integer> spare = new HashMap<>();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			ItemStack inFurnace = furnace.getItem(target);
			if (stack.isEmpty() || !filter.test(stack) || !fits(stack, inFurnace)) {
				continue;
			}
			int usable = spare.computeIfAbsent(stack.getItem(), item -> this.spare(inventory, item));
			int room = (inFurnace.isEmpty() ? stack.getMaxStackSize() : inFurnace.getMaxStackSize()) - inFurnace.getCount();
			int moved = Math.min(Math.min(stack.getCount(), room), usable);
			if (moved <= 0) {
				continue;
			}
			spare.put(stack.getItem(), usable - moved);
			furnace.setItem(target, stack.copyWithCount(inFurnace.getCount() + moved));
			stack.shrink(moved);
			inventory.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
		}
	}

	private void deliver(ServerLevel level, Villager villager, Workstations.Station station) {
		if (this.trip == null) {
			if (this.productCount(level, villager.getInventory()) == 0) {
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
		ChestTrip.Result result = this.trip.tick(level, villager, container -> this.handIn(level, villager.getInventory(), container));
		if (result != ChestTrip.Result.RUNNING) {
			this.trip = null;
			this.waitReason = "chest_unreachable";
			this.waitArgs = new Object[0];
			this.setState(result == ChestTrip.Result.DONE ? State.PLAN : State.WAIT);
		}
	}

	private void handIn(ServerLevel level, SimpleContainer inventory, Container container) {
		int[] amounts = this.amountsToHandIn(level, inventory);
		for (int slot = 0; slot < amounts.length; slot++) {
			if (amounts[slot] <= 0) {
				continue;
			}
			ItemStack stack = inventory.getItem(slot);
			int stored = amounts[slot] - Chests.store(container, stack.copyWithCount(amounts[slot])).getCount();
			this.products.computeIfPresent(stack.getItem(), (item, owed) -> owed - stored > 0 ? owed - stored : null);
			inventory.setItem(slot, stack.getCount() - stored <= 0 ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - stored));
		}
	}

	/** Nothing to do right now: wait at the furnace. */
	private void idle(Villager villager, Workstations.Station station, String reason, Object... args) {
		this.waitReason = reason;
		this.waitArgs = args;
		villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(station.pos(), SPEED, 2));
		this.setState(State.WAIT);
	}

	private void setState(State state) {
		this.state = state;
		this.stateTicks = 0;
	}
}
