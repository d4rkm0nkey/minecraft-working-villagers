package me.teajay.workingvillagers.job;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.crafting.CarpenterRecipes;
import me.teajay.workingvillagers.station.StationType;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The work of the carpenter: fetch the ingredients for the order of the carpenter's bench
 * from the input chests, make the products at the bench and bring them to an output chest.
 * Ingredients may come from several input chests; the worker only sets off when all of
 * them together are enough for at least one piece. With a stock set, it only works while
 * the output chests hold fewer products than that.
 *
 * <p>Without an order, input chest or output chest the worker does not work, nor before it
 * took its wage from its {@link PersonalChest}. Material and products are kept in the
 * villager's inventory, so nothing is lost when the work is interrupted.
 */
public class CraftAtBench extends Behavior<Villager> {
	private static final float SPEED = 0.5F;
	private static final int MIN_CRAFT_TICKS = 20;
	private static final int WALK_TIMEOUT = 400;
	private static final int IDLE_TICKS = 100;
	private static final double STATION_REACH = 2.0;
	private static final long RECIPE_REFRESH_TICKS = 1200L;

	private enum State { PLAN, FETCH, WALK_TO_STATION, CRAFT, DELIVER, RETURN, WAIT }

	private State state = State.PLAN;
	private int stateTicks;
	private @Nullable ChestTrip trip;
	private @Nullable BlockPos stationSpot;
	/** Ticks the current piece takes, 0 if the worker is not making anything. */
	private int craftTicks;
	/** The recipe the worker fetches material for, kept as long as there is material for it. */
	private @Nullable CraftingRecipe planned;
	/** What the worker still has to take out of the chest it is going to. */
	private Map<Item, Integer> missing = Map.of();
	private List<Item> recipesFor = List.of();
	private List<CraftingRecipe> recipes = List.of();
	private long recipesTime;
	/** Why the worker waits, for the status in the menu. */
	private Component waitReason = Component.empty();
	private ItemStack crafting = ItemStack.EMPTY;
	private final PersonalChest personalChest = new PersonalChest();

	public CraftAtBench() {
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
		if (!villager.getVillagerData().profession().is(WorkingVillagers.CARPENTER_PROFESSION)) {
			return false;
		}
		Workstations.Station station = this.getWorkplace(level, villager);
		return station != null
			&& station.order() != null
			&& !station.validInputs(level).isEmpty()
			&& !station.validOutputs(level).isEmpty();
	}

	private Workstations.@Nullable Station getWorkplace(ServerLevel level, Villager villager) {
		Optional<GlobalPos> jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
		if (jobSite.isEmpty() || jobSite.get().dimension() != level.dimension()) {
			return null;
		}
		return Workstations.find(level, jobSite.get().pos()).filter(station -> station.type() == StationType.CARPENTER_BENCH).orElse(null);
	}

	@Override
	protected void start(ServerLevel level, Villager villager, long timestamp) {
		this.planned = null;
		this.setState(State.PLAN);
	}

	@Override
	protected void stop(ServerLevel level, Villager villager, long timestamp) {
		WorkerStatus.clear(villager);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
		villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		this.personalChest.stop(villager);
		if (this.trip != null) {
			this.trip.close(villager);
			this.trip = null;
		}
	}

	@Override
	protected void tick(ServerLevel level, Villager villager, long timestamp) {
		Workstations.Station station = this.getWorkplace(level, villager);
		if (station == null || station.order() == null) {
			return;
		}
		this.stateTicks++;
		if (this.state == State.PLAN && this.personalChest.busy(level, villager, List.of())) {
			return; // wage first, the chest sets the status
		}
		List<CraftingRecipe> recipes = this.recipesFor(level, station.products(), timestamp);
		Predicate<ItemStack> isMaterial = stack -> station.acceptsMaterial(stack)
			&& recipes.stream().anyMatch(recipe -> recipe.placementInfo().ingredients().stream().anyMatch(ingredient -> ingredient.test(stack)));

		switch (this.state) {
			case PLAN -> this.plan(level, villager, station, recipes, isMaterial);
			case FETCH -> this.fetch(level, villager, isMaterial);
			case WALK_TO_STATION -> this.walkToStation(villager, station);
			case CRAFT -> this.craft(level, villager, station, recipes, isMaterial);
			case DELIVER -> this.deliver(level, villager, station, isMaterial);
			case RETURN -> this.returnMaterial(level, villager, station, isMaterial);
			case WAIT -> {
				if (this.stateTicks >= IDLE_TICKS) {
					this.setState(State.PLAN);
				}
			}
		}
		this.report(villager);
	}

	/** Tells the menus what the worker is doing. */
	private void report(Villager villager) {
		switch (this.state) {
			case PLAN, FETCH -> WorkerStatus.task(villager, "carpenter.fetch");
			case WALK_TO_STATION -> WorkerStatus.task(villager, "carpenter.walk");
			case CRAFT -> WorkerStatus.task(villager, "carpenter.craft", this.crafting.isEmpty() ? Component.literal("…") : this.crafting.getHoverName());
			case DELIVER -> WorkerStatus.task(villager, "deliver");
			case RETURN -> WorkerStatus.task(villager, "carpenter.return");
			case WAIT -> ((HiredVillager) villager).workingvillagers$setTask(this.waitReason);
		}
	}

	private void plan(ServerLevel level, Villager villager, Workstations.Station station, List<CraftingRecipe> recipes, Predicate<ItemStack> isMaterial) {
		SimpleContainer inventory = villager.getInventory();
		if (inventory.hasAnyMatching(isProduct(isMaterial))) {
			this.setState(State.DELIVER);
			return;
		}
		if (this.planned != null && !recipes.contains(this.planned)) {
			this.planned = null; // the order changed
		}
		int wanted = Integer.MAX_VALUE;
		if (station.stock() > 0) {
			int stocked = this.countStock(level, station);
			if (stocked >= station.stock()) {
				this.idle(villager, station, isMaterial, WorkerStatus.component("carpenter.stock_full", stocked, station.stock()));
				return;
			}
			wanted = station.stock() - stocked;
		}

		StackedItemContents carried = new StackedItemContents();
		CarpenterRecipes.account(carried, inventory, inventory.getContainerSize(), isMaterial);
		if (this.planned != null && carried.canCraft(this.planned, null) || CarpenterRecipes.firstCraftable(carried, recipes) != null) {
			this.goToStation(level, villager, station);
			return;
		}

		StackedItemContents available = new StackedItemContents();
		CarpenterRecipes.account(available, inventory, inventory.getContainerSize(), isMaterial);
		List<BlockPos> inputs = station.validInputs(level);
		for (BlockPos pos : inputs) {
			Container container = Workstations.getContainer(level, pos);
			if (container != null) {
				CarpenterRecipes.account(available, container, container.getContainerSize(), isMaterial);
			}
		}
		if (this.planned == null || !available.canCraft(this.planned, null)) {
			this.planned = CarpenterRecipes.firstCraftable(available, recipes);
		}
		if (this.planned == null) {
			this.idle(villager, station, isMaterial, this.missingMaterial(level, villager, station, recipes, isMaterial));
			return;
		}

		int perCraft = CarpenterRecipes.preview(this.planned).getCount();
		int limit = Math.max(1, Config.CRAFT_INGREDIENTS_PER_TRIP.get() / Math.max(1, CarpenterRecipes.ingredientCount(this.planned)));
		limit = Math.min(limit, (int) Math.min(Integer.MAX_VALUE, ((long) wanted + perCraft - 1) / perCraft));
		int crafts = CarpenterRecipes.maxCrafts(available, this.planned, limit);
		Map<Item, Integer> needed = CarpenterRecipes.pick(available, this.planned, crafts).orElse(Map.of());
		Map<Item, Integer> stillMissing = new LinkedHashMap<>();
		needed.forEach((item, count) -> {
			int have = countIn(inventory, stack -> stack.is(item) && isMaterial.test(stack));
			if (count > have) {
				stillMissing.put(item, count - have);
			}
		});
		Optional<BlockPos> chest = Chests.closest(
			inputs.stream().filter(pos -> Chests.contains(level, pos, stack -> stillMissing.containsKey(stack.getItem()) && isMaterial.test(stack))).toList(),
			villager
		);
		if (stillMissing.isEmpty() || chest.isEmpty()) {
			// Can only happen if the chests changed in between
			this.idle(villager, station, isMaterial, WorkerStatus.component("saw.no_material"));
			return;
		}
		this.missing = stillMissing;
		this.trip = ChestTrip.to(level, villager, chest.get());
		this.setState(State.FETCH);
	}

	private void fetch(ServerLevel level, Villager villager, Predicate<ItemStack> isMaterial) {
		if (this.trip == null) {
			this.setState(State.PLAN);
			return;
		}
		int[] taken = {0};
		ChestTrip.Result result = this.trip.tick(level, villager, container -> this.missing.forEach((item, count) ->
			taken[0] += Chests.take(container, villager.getInventory(), stack -> stack.is(item) && isMaterial.test(stack), count)
		));
		if (result == ChestTrip.Result.RUNNING) {
			return;
		}
		this.trip = null;
		if (result == ChestTrip.Result.FAILED) {
			this.waitReason = WorkerStatus.component("chest_unreachable");
			this.setState(State.WAIT);
		} else if (taken[0] == 0) {
			// Nothing fit into the inventory, or someone emptied the chest in between
			this.waitReason = WorkerStatus.component("carpenter.inventory_full");
			this.setState(State.WAIT);
		} else {
			this.setState(State.PLAN);
		}
	}

	private void goToStation(ServerLevel level, Villager villager, Workstations.Station station) {
		this.stationSpot = StandSpots.find(level, station.pos(), villager).orElse(null);
		this.setState(State.WALK_TO_STATION);
	}

	private void walkToStation(Villager villager, Workstations.Station station) {
		boolean there = this.stationSpot != null ? StandSpots.isAt(villager, this.stationSpot) : station.pos().closerToCenterThan(villager.position(), STATION_REACH);
		if (there) {
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			this.craftTicks = 0;
			this.setState(State.CRAFT);
		} else if (this.stateTicks > WALK_TIMEOUT) {
			this.waitReason = WorkerStatus.component("carpenter.bench_unreachable");
			this.setState(State.WAIT);
		} else if (this.stateTicks % 20 == 1) {
			BlockPos target = this.stationSpot != null ? this.stationSpot : station.pos();
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, SPEED, this.stationSpot != null ? 0 : 1));
			villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(station.pos()));
		}
	}

	private void craft(ServerLevel level, Villager villager, Workstations.Station station, List<CraftingRecipe> recipes, Predicate<ItemStack> isMaterial) {
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(station.pos()));
		SimpleContainer inventory = villager.getInventory();
		StackedItemContents carried = new StackedItemContents();
		CarpenterRecipes.account(carried, inventory, inventory.getContainerSize(), isMaterial);
		CraftingRecipe recipe = this.planned != null && recipes.contains(this.planned) && carried.canCraft(this.planned, null)
			? this.planned
			: CarpenterRecipes.firstCraftable(carried, recipes);
		if (recipe == null || station.stock() > 0 && this.craftTicks == 0 && this.countStock(level, station) >= station.stock()) {
			this.finishCrafting(villager);
			return;
		}
		ItemStack result = CarpenterRecipes.preview(recipe);
		this.crafting = result;
		if (this.craftTicks == 0) {
			// Start the next piece
			if (!inventory.canAddItem(result) && inventory.hasAnyMatching(isProduct(isMaterial))) {
				this.finishCrafting(villager); // no room for the product, deliver first
				return;
			}
			this.craftTicks = Math.max(MIN_CRAFT_TICKS, (int) Math.round(CarpenterRecipes.ingredientCount(recipe) * Config.CRAFT_SECONDS_PER_INGREDIENT.get() * 20.0));
			this.stateTicks = 0;
			villager.setItemSlot(EquipmentSlot.MAINHAND, this.firstMaterial(inventory, isMaterial).copyWithCount(1));
			villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
		}

		if (this.stateTicks % 4 == 0) {
			this.shavings(level, station.pos(), villager.getMainHandItem());
		}
		if (this.stateTicks % 10 == 0) {
			villager.swingForAttack(InteractionHand.MAIN_HAND);
			level.playSound(null, station.pos(), SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.4F, 0.8F + level.getRandom().nextFloat() * 0.4F);
		}
		if (this.stateTicks < this.craftTicks) {
			return;
		}

		for (ItemStack made : CarpenterRecipes.craft(inventory, inventory.getContainerSize(), isMaterial, recipe)) {
			ItemStack rest = inventory.addItem(made);
			if (!rest.isEmpty()) {
				villager.spawnAtLocation(level, rest);
			}
		}
		level.playSound(null, station.pos(), SoundEvents.AXE_STRIP.value(), SoundSource.BLOCKS, 0.6F, 1.1F);
		this.craftTicks = 0;
	}

	private ItemStack firstMaterial(SimpleContainer inventory, Predicate<ItemStack> isMaterial) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty() && isMaterial.test(stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private void finishCrafting(Villager villager) {
		villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		this.craftTicks = 0;
		this.crafting = ItemStack.EMPTY;
		this.setState(State.DELIVER);
	}

	/** Shavings of the material flying off the bench. */
	private void shavings(ServerLevel level, BlockPos station, ItemStack material) {
		BlockState state = material.getItem() instanceof BlockItem blockItem
			? blockItem.getBlock().defaultBlockState()
			: level.getBlockState(station);
		level.sendParticles(
			new BlockParticleOption(ParticleTypes.BLOCK, state),
			station.getX() + 0.5, station.getY() + 1.05, station.getZ() + 0.5,
			3, 0.2, 0.05, 0.2, 0.05
		);
	}

	private void deliver(ServerLevel level, Villager villager, Workstations.Station station, Predicate<ItemStack> isMaterial) {
		Predicate<ItemStack> isProduct = isProduct(isMaterial);
		if (this.trip == null) {
			if (!villager.getInventory().hasAnyMatching(isProduct)) {
				this.setState(State.PLAN);
				return;
			}
			Optional<BlockPos> chest = Chests.closest(station.validOutputs(level).stream().filter(pos -> Chests.hasRoom(level, pos)).toList(), villager);
			if (chest.isEmpty()) {
				this.waitReason = WorkerStatus.component("chests_full");
				this.walkToBench(villager, station);
				this.setState(State.WAIT);
				return;
			}
			this.trip = ChestTrip.to(level, villager, chest.get());
		}
		ChestTrip.Result result = this.trip.tick(level, villager, container -> Chests.deposit(villager.getInventory(), container, isProduct));
		if (result != ChestTrip.Result.RUNNING) {
			// If not everything fit, the next round picks another chest with room
			this.trip = null;
			this.setState(result == ChestTrip.Result.DONE ? State.PLAN : State.WAIT);
			this.waitReason = WorkerStatus.component("chest_unreachable");
		}
	}

	/** Brings material the worker cannot use right now back to an input chest. */
	private void returnMaterial(ServerLevel level, Villager villager, Workstations.Station station, Predicate<ItemStack> isMaterial) {
		if (this.trip == null) {
			Optional<BlockPos> chest = Chests.closest(station.validInputs(level).stream().filter(pos -> Chests.hasRoom(level, pos)).toList(), villager);
			if (chest.isEmpty()) {
				this.setState(State.WAIT);
				return;
			}
			this.trip = ChestTrip.to(level, villager, chest.get());
		}
		ChestTrip.Result result = this.trip.tick(level, villager, container -> Chests.deposit(villager.getInventory(), container, isMaterial));
		if (result != ChestTrip.Result.RUNNING) {
			this.trip = null;
			this.setState(State.WAIT);
		}
	}

	/**
	 * Nothing to do: wait at the bench. Material the worker carries goes back to an input
	 * chest first, so it is not stuck in the worker's pockets.
	 */
	private void idle(Villager villager, Workstations.Station station, Predicate<ItemStack> isMaterial, Component reason) {
		this.waitReason = reason;
		this.planned = null;
		if (villager.getInventory().hasAnyMatching(stack -> !stack.isEmpty() && isMaterial.test(stack))) {
			this.setState(State.RETURN);
			return;
		}
		this.walkToBench(villager, station);
		this.setState(State.WAIT);
	}

	private void walkToBench(Villager villager, Workstations.Station station) {
		villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(station.pos(), SPEED, 2));
	}

	/**
	 * Why there is no material: names the first ingredient of the first product that is
	 * missing in the inventory and the input chests.
	 */
	private Component missingMaterial(
		ServerLevel level, Villager villager, Workstations.Station station, List<CraftingRecipe> recipes, Predicate<ItemStack> isMaterial
	) {
		if (recipes.isEmpty()) {
			return WorkerStatus.component("carpenter.no_recipe");
		}
		Map<Item, Integer> counts = new HashMap<>();
		addCounts(villager.getInventory(), isMaterial, counts);
		for (BlockPos pos : station.validInputs(level)) {
			Container container = Workstations.getContainer(level, pos);
			if (container != null) {
				addCounts(container, isMaterial, counts);
			}
		}
		Map<Ingredient, Integer> needed = new LinkedHashMap<>();
		recipes.getFirst().placementInfo().ingredients().forEach(ingredient -> needed.merge(ingredient, 1, Integer::sum));
		for (Map.Entry<Ingredient, Integer> entry : needed.entrySet()) {
			int have = counts.entrySet().stream()
				.filter(item -> entry.getKey().acceptsItem(item.getKey().builtInRegistryHolder()))
				.mapToInt(Map.Entry::getValue)
				.sum();
			if (have < entry.getValue()) {
				Optional<Holder<Item>> first = entry.getKey().items().findFirst();
				if (first.isPresent()) {
					return WorkerStatus.component("carpenter.waiting_for", new ItemStack(first.get()).getHoverName());
				}
			}
		}
		return WorkerStatus.component("saw.no_material");
	}

	/** How many of the ordered products the output chests hold. */
	private int countStock(ServerLevel level, Workstations.Station station) {
		List<Item> products = station.products();
		Predicate<ItemStack> isProduct = stack -> products.contains(stack.getItem());
		int stocked = 0;
		for (BlockPos pos : station.validOutputs(level)) {
			Container container = Workstations.getContainer(level, pos);
			if (container != null) {
				stocked += countIn(container, isProduct);
			}
		}
		return stocked;
	}

	private static void addCounts(Container container, Predicate<ItemStack> filter, Map<Item, Integer> counts) {
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot);
			if (!stack.isEmpty() && filter.test(stack)) {
				counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
			}
		}
	}

	private static int countIn(Container container, Predicate<ItemStack> filter) {
		int count = 0;
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot);
			if (!stack.isEmpty() && filter.test(stack)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	/** The recipes for the products, looked up again now and then in case the recipes were reloaded. */
	private List<CraftingRecipe> recipesFor(ServerLevel level, List<Item> products, long timestamp) {
		if (!products.equals(this.recipesFor) || timestamp - this.recipesTime > RECIPE_REFRESH_TICKS) {
			this.recipes = products.stream().flatMap(product -> CarpenterRecipes.forResult(level, product).stream()).toList();
			this.recipesFor = products;
			this.recipesTime = timestamp;
		}
		return this.recipes;
	}

	/**
	 * Everything the worker hands in: all but food and the material for the current order.
	 * Material of an earlier order is handed in as well.
	 */
	private static Predicate<ItemStack> isProduct(Predicate<ItemStack> isMaterial) {
		return stack -> Chests.isNotFood(stack) && !isMaterial.test(stack);
	}

	private void setState(State state) {
		this.state = state;
		this.stateTicks = 0;
	}
}
