package me.teajay.workingvillagers.job;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
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
 * from the input chests and the village chests ({@link Orders#sources}), make the products at
 * the bench and bring them to an output chest. Ingredients may come from several chests; the
 * worker only sets off when all of them together are enough for at least one piece. With
 * several products it makes them one batch at a time, the most needed first
 * ({@link Workstations.Station#ordersByNeed}); a product without material does not hold up the
 * others. A product ordered once it makes until there are as many as ordered, one kept in
 * stock while the output chests hold fewer of it than that.
 *
 * <p>An ingredient that is missing but that the bench makes itself (the chest of a boat with
 * chest) it makes first, from what lies in the chests, up to two steps back. What is still
 * missing for the order it names in its status, with how many.
 *
 * <p>Without an order, input chest or output chest the worker does not work, nor before it
 * took its wage (see {@link WorkerSupplies}). Material and products are kept in the
 * villager's inventory, so nothing is lost when the work is interrupted.
 */
public class CraftAtBench extends Behavior<Villager> {
	private static final float SPEED = 0.5F;
	private static final int MIN_CRAFT_TICKS = 20;
	private static final int WALK_TIMEOUT = 400;
	private static final int IDLE_TICKS = 100;
	private static final double STATION_REACH = 2.0;
	private static final long RECIPE_REFRESH_TICKS = 1200L;
	/** How many steps back the worker makes missing ingredients itself (the boat of a boat with chest, its planks). */
	private static final int HELPER_DEPTH = 2;

	private enum State { PLAN, FETCH, WALK_TO_STATION, CRAFT, DELIVER, RETURN, WAIT }

	private State state = State.PLAN;
	private int stateTicks;
	private @Nullable ChestTrip trip;
	private @Nullable BlockPos stationSpot;
	/** Ticks the current piece takes, 0 if the worker is not making anything. */
	private int craftTicks;
	/** The recipe the worker fetches material for, kept as long as there is material for it. */
	private @Nullable CraftingRecipe planned;
	/**
	 * Recipes of the bench for ingredients the order lacks (see {@link #findHelper}): the
	 * planned one, if it is one of them, and how many more times the worker makes it.
	 */
	private List<CraftingRecipe> helpers = List.of();
	private int helperCrafts;
	/** What the worker still has to take out of the chest it is going to. */
	private Map<Item, Integer> missing = Map.of();
	private List<Item> recipesFor = List.of();
	private List<CraftingRecipe> recipes = List.of();
	private long recipesTime;
	/** Why the worker waits, for the status in the menu. */
	private Component waitReason = Component.empty();
	private ItemStack crafting = ItemStack.EMPTY;
	private final WorkerSupplies supplies = new WorkerSupplies();

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
			&& station.hasOrder()
			&& Orders.hasSources(level, station)
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
		this.helpers = List.of();
		this.setState(State.PLAN);
	}

	@Override
	protected void stop(ServerLevel level, Villager villager, long timestamp) {
		WorkerStatus.clear(villager);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
		villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		this.supplies.stop(villager);
		if (this.trip != null) {
			this.trip.close(villager);
			this.trip = null;
		}
	}

	@Override
	protected void tick(ServerLevel level, Villager villager, long timestamp) {
		Workstations.Station station = this.getWorkplace(level, villager);
		if (station == null || !station.hasOrder()) {
			return;
		}
		this.stateTicks++;
		if (this.state == State.PLAN && this.supplies.busy(level, villager)) {
			return; // wage first, the chest sets the status
		}
		List<CraftingRecipe> all = this.recipesFor(level, station.products(), timestamp);
		// Only what its level lets it make; for the rest it says from when it can
		List<CraftingRecipe> recipes = all.stream()
			.filter(recipe -> Unlocks.canMake(villager, CarpenterRecipes.resultOf(recipe), recipe.placementInfo().ingredients()))
			.toList();
		// The ingredients of the order and of what the worker makes for it
		List<CraftingRecipe> usable = this.helpers.isEmpty() ? recipes : Stream.concat(recipes.stream(), this.helpers.stream()).toList();
		Predicate<ItemStack> isMaterial = stack -> station.acceptsMaterial(stack)
			&& usable.stream().anyMatch(recipe -> recipe.placementInfo().ingredients().stream().anyMatch(ingredient -> ingredient.test(stack)));

		switch (this.state) {
			case PLAN -> {
				if (recipes.isEmpty() && !all.isEmpty()) {
					this.idle(villager, station, isMaterial, Unlocks.lockedStatus(all.stream()
						.mapToInt(recipe -> Unlocks.level(villager, CarpenterRecipes.resultOf(recipe), recipe.placementInfo().ingredients()))
						.min().orElse(WorkerLevels.MASTER)));
				} else {
					this.plan(level, villager, station, recipes, isMaterial);
				}
			}
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
			case WAIT -> {
				((HiredVillager) villager).workingvillagers$setTask(this.waitReason);
				((HiredVillager) villager).workingvillagers$setWorking(false);
			}
		}
	}

	private void plan(ServerLevel level, Villager villager, Workstations.Station station, List<CraftingRecipe> recipes, Predicate<ItemStack> isMaterial) {
		SimpleContainer inventory = villager.getInventory();
		if (inventory.hasAnyMatching(isProduct(station, isMaterial))) {
			this.setState(State.DELIVER);
			return;
		}
		// Everything made is handed in: the orders that are done are done
		Orders.finish(level, villager, station);
		if (!station.hasOrder()) {
			return;
		}
		if (this.planned != null && !recipes.contains(this.planned) && !this.helpers.contains(this.planned)) {
			this.planned = null; // the order changed
		}
		List<Item> byNeed = station.ordersByNeed(order -> station.inOutputs(level, order));
		if (byNeed.isEmpty()) {
			Orders.Idle idle = Orders.nothingToMake(level, station, "carpenter");
			this.idle(villager, station, isMaterial, WorkerStatus.component(idle.key(), idle.args()));
			return;
		}
		// The recipes of the products that are not in stock yet, the most needed first
		List<CraftingRecipe> open = byNeed.stream()
			.flatMap(order -> station.productsOf(order).stream())
			.flatMap(product -> recipes.stream().filter(recipe -> CarpenterRecipes.resultOf(recipe) == product))
			.toList();

		StackedItemContents carried = new StackedItemContents();
		CarpenterRecipes.account(carried, inventory, inventory.getContainerSize(), isMaterial);
		boolean helping = this.planned != null && this.helpers.contains(this.planned) && this.helperCrafts > 0;
		if (this.planned != null && (open.contains(this.planned) || helping) && carried.canCraft(this.planned, null)
			|| CarpenterRecipes.firstCraftable(carried, open) != null) {
			this.goToStation(level, villager, station);
			return;
		}

		StackedItemContents available = new StackedItemContents();
		CarpenterRecipes.account(available, inventory, inventory.getContainerSize(), isMaterial);
		List<BlockPos> sources = Orders.sources(level, station);
		for (BlockPos pos : sources) {
			Container container = Workstations.getContainer(level, pos);
			if (container != null) {
				CarpenterRecipes.account(available, container, container.getContainerSize(), isMaterial);
			}
		}
		// The most needed product there is material for; one batch of it, then plan again
		this.planned = null;
		int wanted = Integer.MAX_VALUE;
		for (Item order : byNeed) {
			List<CraftingRecipe> orderRecipes = open.stream().filter(recipe -> station.productsOf(order).contains(CarpenterRecipes.resultOf(recipe))).toList();
			CraftingRecipe recipe = CarpenterRecipes.firstCraftable(available, orderRecipes);
			if (recipe != null) {
				this.planned = recipe;
				wanted = Orders.wanted(level, station, order);
				break;
			}
		}
		Predicate<ItemStack> fetched = isMaterial;
		int limit;
		if (this.planned != null) {
			this.helpers = List.of();
			int perCraft = CarpenterRecipes.preview(this.planned).getCount();
			limit = Math.min(tripLimit(this.planned), (int) Math.min(Integer.MAX_VALUE, ((long) wanted + perCraft - 1) / perCraft));
		} else {
			// Nothing of the order can be made: maybe an ingredient the worker makes itself
			Helper helper = this.findHelper(level, villager, station, byNeed, open, sources);
			if (helper == null) {
				this.helpers = List.of();
				this.idle(villager, station, isMaterial, this.missingMaterial(level, villager, station, open, isMaterial));
				return;
			}
			this.planned = helper.recipe();
			this.helpers = List.of(helper.recipe());
			this.helperCrafts = helper.crafts();
			limit = Math.min(tripLimit(helper.recipe()), helper.crafts());
			fetched = stack -> station.acceptsMaterial(stack)
				&& helper.recipe().placementInfo().ingredients().stream().anyMatch(ingredient -> ingredient.test(stack));
			available = new StackedItemContents();
			CarpenterRecipes.account(available, inventory, inventory.getContainerSize(), fetched);
			for (BlockPos pos : sources) {
				Container container = Workstations.getContainer(level, pos);
				if (container != null) {
					CarpenterRecipes.account(available, container, container.getContainerSize(), fetched);
				}
			}
		}

		int crafts = CarpenterRecipes.maxCrafts(available, this.planned, limit);
		Map<Item, Integer> needed = CarpenterRecipes.pick(available, this.planned, crafts).orElse(Map.of());
		Map<Item, Integer> stillMissing = new LinkedHashMap<>();
		Predicate<ItemStack> material = fetched;
		needed.forEach((item, count) -> {
			int have = countIn(inventory, stack -> stack.is(item) && material.test(stack));
			if (count > have) {
				stillMissing.put(item, count - have);
			}
		});
		Optional<BlockPos> chest = Chests.closest(
			sources.stream().filter(pos -> Chests.contains(level, pos, stack -> stillMissing.containsKey(stack.getItem()) && material.test(stack))).toList(),
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

	/** How many crafts of the recipe the worker fetches material for at once. */
	private static int tripLimit(CraftingRecipe recipe) {
		return Math.max(1, Config.CRAFT_INGREDIENTS_PER_TRIP.get() / Math.max(1, CarpenterRecipes.ingredientCount(recipe)));
	}

	/** A recipe of the bench for an ingredient the order lacks, and how many times to make it. */
	private record Helper(CraftingRecipe recipe, int crafts) {
	}

	/**
	 * When nothing of the order can be made: an ingredient that is missing and that the bench
	 * makes itself, from what lies in the inventory and the chests. If its own ingredients
	 * are missing as well, one of them, up to {@link #HELPER_DEPTH} steps back. Made as often
	 * as the order needs (at most a trip's worth), the most needed product first.
	 */
	private @Nullable Helper findHelper(
		ServerLevel level, Villager villager, Workstations.Station station, List<Item> byNeed, List<CraftingRecipe> open, List<BlockPos> sources
	) {
		// Everything the worker may use, not only the ingredients of the order
		Predicate<ItemStack> usable = station::acceptsMaterial;
		StackedItemContents available = new StackedItemContents();
		Map<Item, Integer> counts = new HashMap<>();
		SimpleContainer inventory = villager.getInventory();
		CarpenterRecipes.account(available, inventory, inventory.getContainerSize(), usable);
		addCounts(inventory, usable, counts);
		for (BlockPos pos : sources) {
			Container container = Workstations.getContainer(level, pos);
			if (container != null) {
				CarpenterRecipes.account(available, container, container.getContainerSize(), usable);
				addCounts(container, usable, counts);
			}
		}
		for (Item order : byNeed) {
			int wanted = Orders.wanted(level, station, order);
			for (CraftingRecipe recipe : open) {
				if (!station.productsOf(order).contains(CarpenterRecipes.resultOf(recipe))) {
					continue;
				}
				int perCraft = CarpenterRecipes.preview(recipe).getCount();
				int crafts = (int) Math.min(tripLimit(recipe), ((long) Math.min(wanted, Integer.MAX_VALUE - perCraft) + perCraft - 1) / perCraft);
				Helper helper = this.helperFor(level, villager, station, recipe, crafts, available, counts, 0);
				if (helper != null) {
					return helper;
				}
			}
		}
		return null;
	}

	private @Nullable Helper helperFor(
		ServerLevel level, Villager villager, Workstations.Station station, CraftingRecipe recipe, int crafts, StackedItemContents available,
		Map<Item, Integer> counts, int depth
	) {
		if (depth >= HELPER_DEPTH) {
			return null;
		}
		for (Map.Entry<Ingredient, Integer> entry : grouped(recipe).entrySet()) {
			int lacking = entry.getValue() * crafts - have(entry.getKey(), counts);
			if (lacking <= 0) {
				continue;
			}
			for (CraftingRecipe candidate : CarpenterRecipes.all(level)) {
				Item result = CarpenterRecipes.resultOf(candidate);
				// A product of the order is handed in, not used (see isProduct): ordered chests are no chests for boats
				if (candidate == recipe || station.orderOf(result) != null || !entry.getKey().acceptsItem(result.builtInRegistryHolder())
					|| !station.acceptsMaterial(new ItemStack(result))
					|| !Unlocks.canMake(villager, result, candidate.placementInfo().ingredients())) {
					continue;
				}
				int perCraft = CarpenterRecipes.preview(candidate).getCount();
				int needed = (lacking + perCraft - 1) / perCraft;
				int possible = CarpenterRecipes.maxCrafts(available, candidate, needed);
				if (possible > 0) {
					return new Helper(candidate, possible);
				}
				Helper deeper = this.helperFor(level, villager, station, candidate, needed, available, counts, depth + 1);
				if (deeper != null) {
					return deeper;
				}
			}
		}
		return null;
	}

	/** The ingredients of the recipe with how many of each one craft takes. */
	private static Map<Ingredient, Integer> grouped(CraftingRecipe recipe) {
		Map<Ingredient, Integer> grouped = new LinkedHashMap<>();
		recipe.placementInfo().ingredients().forEach(ingredient -> grouped.merge(ingredient, 1, Integer::sum));
		return grouped;
	}

	/** How many of the counted items the ingredient accepts. */
	private static int have(Ingredient ingredient, Map<Item, Integer> counts) {
		return counts.entrySet().stream()
			.filter(item -> ingredient.acceptsItem(item.getKey().builtInRegistryHolder()))
			.mapToInt(Map.Entry::getValue)
			.sum();
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
		// The planned batch first (an ingredient it makes for the order, too), then what else the worker carries material for
		List<CraftingRecipe> open = this.craftTicks == 0
			? recipes.stream().filter(candidate -> {
				Item order = station.orderOf(CarpenterRecipes.resultOf(candidate));
				return order != null && (!station.limited(order) || Orders.wanted(level, station, order) > 0);
			}).toList()
			: recipes;
		boolean helping = this.planned != null && this.helpers.contains(this.planned) && (this.helperCrafts > 0 || this.craftTicks > 0);
		CraftingRecipe recipe = this.planned != null && (open.contains(this.planned) || helping) && carried.canCraft(this.planned, null)
			? this.planned
			: CarpenterRecipes.firstCraftable(carried, open);
		if (recipe == null) {
			this.finishCrafting(villager);
			return;
		}
		ItemStack result = CarpenterRecipes.preview(recipe);
		this.crafting = result;
		if (this.craftTicks == 0) {
			// Start the next piece
			if (!inventory.canAddItem(result) && inventory.hasAnyMatching(isProduct(station, isMaterial))) {
				this.finishCrafting(villager); // no room for the product, deliver first
				return;
			}
			this.craftTicks = WorkerLevels.ticks(villager, Math.max(MIN_CRAFT_TICKS, (int) Math.round(CarpenterRecipes.ingredientCount(recipe) * Config.CRAFT_SECONDS_PER_INGREDIENT.get() * 20.0)));
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
		if (recipe == this.planned && this.helpers.contains(recipe)) {
			this.helperCrafts--;
		}
		station.addMade(result.getItem(), result.getCount());
		((HiredVillager) villager).workingvillagers$addWorkDone(ChopTrees.today(level), result.getCount());
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
		Predicate<ItemStack> isProduct = isProduct(station, isMaterial);
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

	/** Brings material the worker cannot use right now back to an input chest (or a village chest). */
	private void returnMaterial(ServerLevel level, Villager villager, Workstations.Station station, Predicate<ItemStack> isMaterial) {
		if (this.trip == null) {
			Optional<BlockPos> chest = Chests.closest(Orders.sources(level, station).stream().filter(pos -> Chests.hasRoom(level, pos)).toList(), villager);
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
		this.helpers = List.of();
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
	 * Why there is no material: names the first ingredient of the most needed product that is
	 * missing in the inventory and the chests; for a product ordered once or kept in stock with
	 * how many are missing for all that are still to make.
	 */
	private Component missingMaterial(
		ServerLevel level, Villager villager, Workstations.Station station, List<CraftingRecipe> recipes, Predicate<ItemStack> isMaterial
	) {
		if (recipes.isEmpty()) {
			return WorkerStatus.component("carpenter.no_recipe");
		}
		Map<Item, Integer> counts = new HashMap<>();
		addCounts(villager.getInventory(), isMaterial, counts);
		for (BlockPos pos : Orders.sources(level, station)) {
			Container container = Workstations.getContainer(level, pos);
			if (container != null) {
				addCounts(container, isMaterial, counts);
			}
		}
		CraftingRecipe recipe = recipes.getFirst();
		Item order = station.orderOf(CarpenterRecipes.resultOf(recipe));
		int wanted = order == null ? Integer.MAX_VALUE : Orders.wanted(level, station, order);
		int perCraft = CarpenterRecipes.preview(recipe).getCount();
		int crafts = wanted == Integer.MAX_VALUE ? 1 : Math.max(1, (wanted + perCraft - 1) / perCraft);
		for (Map.Entry<Ingredient, Integer> entry : grouped(recipe).entrySet()) {
			int lacking = entry.getValue() * crafts - have(entry.getKey(), counts);
			if (lacking > 0) {
				Optional<Holder<Item>> first = entry.getKey().items().findFirst();
				if (first.isPresent()) {
					Component name = new ItemStack(first.get()).getHoverName();
					return wanted == Integer.MAX_VALUE
						? WorkerStatus.component("carpenter.waiting_for", name)
						: WorkerStatus.component("carpenter.missing", lacking, name);
				}
			}
		}
		return WorkerStatus.component("saw.no_material");
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
	 * Everything the worker hands in: all but food and the material for the current order,
	 * but the products of the order even if they are material of another one (chests for
	 * boats with chest). Material of an earlier order is handed in as well.
	 */
	private static Predicate<ItemStack> isProduct(Workstations.Station station, Predicate<ItemStack> isMaterial) {
		List<Item> products = station.products();
		return stack -> Chests.isHandedIn(stack) && (!isMaterial.test(stack) || products.contains(stack.getItem()));
	}

	private void setState(State state) {
		this.state = state;
		this.stateTicks = 0;
	}
}
