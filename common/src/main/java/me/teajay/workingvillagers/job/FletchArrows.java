package me.teajay.workingvillagers.job;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * The work of the fletcher: make the ordered products (arrows, spectral arrows, bows,
 * crossbows) at the fletching table from what lies in the input chests, and bring them to the
 * output chests, until as many of each as the stock lie there. With several products it makes
 * a batch of the most needed one there is material for ({@link Workstations.Station#ordersByNeed}).
 *
 * <p>An arrow needs a head, a stick and a feather and makes 4, like the vanilla recipe. The
 * head is flint, or a copper or iron nugget. The fletcher makes heads itself: it knaps flint
 * from gravel (1 gravel, 1 flint, like the mill) and cuts ingots into nuggets (1 ingot, 9
 * nuggets). It uses flint first, then copper, then iron, and only knaps or cuts when it has
 * no heads left.
 *
 * <p>What the order does not need it puts back into the input chest it fetches from,
 * products of an earlier order it brings to the output chests.
 */
public class FletchArrows extends Behavior<Villager> {
	private static final float SPEED = 0.5F;
	private static final int WALK_TIMEOUT = 400;
	private static final int WAIT_TICKS = 100;
	/** One piece (4 arrows, a bow, ...) per second. */
	private static final int CRAFT_TICKS = 20;
	/** After fetching from the input chests found nothing, how long it does not try again. */
	private static final long FETCH_COOLDOWN = 600L;

	/** One part of a recipe; the head of an arrow can be one of several items. */
	private record Part(Item shown, Predicate<ItemStack> item, int amount) {
	}

	private record Recipe(Item product, int count, List<Part> parts, int batch) {
	}

	private static final Part HEAD = new Part(Items.FLINT, FletchArrows::isHeadSource, 1);
	private static final Part STICK = new Part(Items.STICK, stack -> stack.is(Items.STICK), 1);
	private static final Map<Item, Recipe> RECIPES = Map.of(
		Items.ARROW, new Recipe(Items.ARROW, 4, List.of(HEAD, STICK, new Part(Items.FEATHER, stack -> stack.is(Items.FEATHER), 1)), 16),
		Items.SPECTRAL_ARROW, new Recipe(Items.SPECTRAL_ARROW, 2, List.of(
			new Part(Items.GLOWSTONE_DUST, stack -> stack.is(Items.GLOWSTONE_DUST), 4), new Part(Items.ARROW, stack -> stack.is(Items.ARROW), 1)), 8),
		Items.BOW, new Recipe(Items.BOW, 1, List.of(new Part(Items.STICK, stack -> stack.is(Items.STICK), 3),
			new Part(Items.STRING, stack -> stack.is(Items.STRING), 3)), 4),
		Items.CROSSBOW, new Recipe(Items.CROSSBOW, 1, List.of(new Part(Items.STICK, stack -> stack.is(Items.STICK), 3),
			new Part(Items.STRING, stack -> stack.is(Items.STRING), 2), new Part(Items.IRON_INGOT, stack -> stack.is(Items.IRON_INGOT), 1),
			new Part(Items.TRIPWIRE_HOOK, stack -> stack.is(Items.TRIPWIRE_HOOK), 1)), 4)
	);
	/** What can be ordered, in the order of the overview. */
	public static final List<Item> PRODUCTS = List.of(Items.ARROW, Items.SPECTRAL_ARROW, Items.BOW, Items.CROSSBOW);
	/** Heads in the order they are used, then what they are made from. */
	private static final List<Item> HEADS = List.of(Items.FLINT, Items.COPPER_NUGGET, Items.IRON_NUGGET);
	private static final Map<Item, Integer> HEAD_SOURCES = ImmutableMap.of(Items.GRAVEL, 1, Items.COPPER_INGOT, 9, Items.IRON_INGOT, 9);

	private enum State { PLAN, FETCH, WALK, CRAFT, DELIVER, WAIT }

	private State state = State.PLAN;
	private int stateTicks;
	private String waitReason = "fletcher.no_order";
	private Object[] waitArgs = new Object[0];
	private @Nullable ChestTrip trip;
	private @Nullable BlockPos fetchFrom;
	/** When fetching material for a product found nothing, per product. */
	private final Map<Item, Long> fetchFailedAt = new HashMap<>();
	/** The product the worker makes right now, null before it picked one. */
	private @Nullable Item current;
	private final WorkerSupplies supplies = new WorkerSupplies();

	public FletchArrows() {
		super(ImmutableMap.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT), 24000);
	}

	public static boolean isHeadSource(ItemStack stack) {
		return HEADS.stream().anyMatch(stack::is) || HEAD_SOURCES.keySet().stream().anyMatch(stack::is);
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
		if (!villager.getVillagerData().profession().is(WorkingVillagers.FLETCHER_PROFESSION)) {
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
			case DELIVER -> this.deliver(level, villager, workplace);
			case WAIT -> {
				if (this.stateTicks >= WAIT_TICKS) {
					this.setState(State.PLAN);
				}
			}
		}
		this.report(villager);
	}

	// --- Counting ---

	/** How many of a part the stack is worth: an ingot makes 9 heads. */
	private static int worth(Part part, ItemStack stack) {
		if (part != HEAD) {
			return stack.getCount();
		}
		return stack.getCount() * HEAD_SOURCES.getOrDefault(stack.getItem(), 1);
	}

	private static int available(SimpleContainer inventory, Part part) {
		int available = 0;
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && part.item().test(stack)) {
				available += worth(part, stack);
			}
		}
		return available;
	}

	/** How many pieces the fletcher can make from what it carries. */
	private static int possible(SimpleContainer inventory, Recipe recipe) {
		int possible = Integer.MAX_VALUE;
		for (Part part : recipe.parts()) {
			possible = Math.min(possible, available(inventory, part) / part.amount());
		}
		return possible;
	}

	private static int inOutputs(ServerLevel level, Workstations.Station workplace, Item product) {
		int count = 0;
		for (BlockPos pos : workplace.validOutputs(level)) {
			Container container = Workstations.getContainer(level, pos);
			if (container != null) {
				count += container.countItem(product);
			}
		}
		return count;
	}

	/** What the fletcher brings to the output chests: what it made, and anything it does not make with. */
	private static boolean isProduct(ItemStack stack, @Nullable Recipe recipe) {
		if (!PRODUCTS.contains(stack.getItem())) {
			return false;
		}
		return recipe == null || recipe.parts().stream().noneMatch(part -> part.item().test(stack)) || stack.is(recipe.product());
	}

	/** What it does not need for any product of the order and puts back into an input chest. */
	private static boolean isUnneeded(ItemStack stack, @Nullable Recipe recipe, List<Recipe> recipes) {
		return !stack.isEmpty() && !WorkerSupplies.isWage(stack) && !isProduct(stack, recipe)
			&& recipes.stream().noneMatch(other -> other.parts().stream().anyMatch(part -> part.item().test(stack)));
	}

	/** The recipes of the order's products. */
	private static List<Recipe> recipes(Workstations.Station workplace) {
		return workplace.orders().stream().map(RECIPES::get).filter(Objects::nonNull).toList();
	}

	/** The recipe of the product it makes right now, null if none. */
	private @Nullable Recipe recipe(Workstations.Station workplace) {
		if (this.current != null && !workplace.orders().contains(this.current)) {
			this.current = null; // the order changed
		}
		return this.current == null ? null : RECIPES.get(this.current);
	}

	private static boolean hasProducts(SimpleContainer inventory, @Nullable Recipe recipe) {
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && isProduct(stack, recipe)) {
				return true;
			}
		}
		return false;
	}

	// --- The work ---

	private void plan(ServerLevel level, Villager villager, Workstations.Station workplace, long timestamp) {
		SimpleContainer inventory = villager.getInventory();
		List<Item> orders = workplace.orders().stream().filter(RECIPES::containsKey).toList();
		if (orders.isEmpty()) {
			this.current = null;
			if (hasProducts(inventory, null)) {
				this.setState(State.DELIVER);
			} else {
				this.idle(villager, workplace, "fletcher.no_order");
			}
			return;
		}
		int stock = workplace.stock();
		List<Item> byNeed = workplace.ordersByNeed(product -> inOutputs(level, workplace, product) + inventory.countItem(product)).stream()
			.filter(RECIPES::containsKey)
			.toList();
		if (byNeed.isEmpty()) {
			if (hasProducts(inventory, this.recipe(workplace))) {
				this.setState(State.DELIVER);
			} else {
				int have = orders.stream().mapToInt(product -> inOutputs(level, workplace, product) + inventory.countItem(product)).min().orElse(0);
				this.idle(villager, workplace, "fletcher.stock_full", have, stock);
			}
			return;
		}
		// The most needed product it carries material for, or else can fetch material for
		Item pick = byNeed.stream().filter(product -> possible(inventory, RECIPES.get(product)) > 0).findFirst()
			.or(() -> byNeed.stream().filter(product -> this.mayFetch(product, timestamp)
				&& this.inputWith(level, villager, workplace, RECIPES.get(product), inventory, 1).isPresent()).findFirst())
			.orElse(byNeed.getFirst());
		if (pick != this.current && hasProducts(inventory, this.recipe(workplace))) {
			this.setState(State.DELIVER); // what it made for the last product first
			return;
		}
		this.current = pick;
		Recipe recipe = RECIPES.get(pick);
		int have = inOutputs(level, workplace, recipe.product()) + inventory.countItem(recipe.product());
		if (!inventory.canAddItem(new ItemStack(recipe.product(), recipe.count()))) {
			this.setState(State.DELIVER);
			return;
		}
		int wanted = recipe.batch();
		if (stock > 0) {
			wanted = Math.min(wanted, Math.max(1, Mth.positiveCeilDiv(stock - have, recipe.count())));
		}
		int possible = possible(inventory, recipe);
		if (possible < wanted && this.mayFetch(pick, timestamp)) {
			Optional<BlockPos> chest = this.inputWith(level, villager, workplace, recipe, inventory, wanted);
			if (chest.isPresent()) {
				this.fetchFrom = chest.get();
				this.setState(State.FETCH);
				return;
			}
		}
		if (possible > 0) {
			this.setState(State.WALK);
		} else if (hasProducts(inventory, recipe)) {
			this.setState(State.DELIVER);
		} else {
			Part missing = recipe.parts().stream().filter(part -> available(inventory, part) < part.amount()).findFirst().orElse(recipe.parts().getFirst());
			this.idle(villager, workplace, "fletcher.no_material", new ItemStack(missing.shown()).getHoverName());
		}
	}

	private boolean mayFetch(Item product, long timestamp) {
		return timestamp - this.fetchFailedAt.getOrDefault(product, Long.MIN_VALUE / 2) > FETCH_COOLDOWN;
	}

	/** The closest input chest with a part the fletcher lacks for {@code wanted} pieces. */
	private Optional<BlockPos> inputWith(ServerLevel level, Villager villager, Workstations.Station workplace, Recipe recipe, SimpleContainer inventory,
		int wanted) {
		List<BlockPos> chests = workplace.validInputs(level).stream()
			.filter(pos -> recipe.parts().stream().anyMatch(part -> available(inventory, part) < part.amount() * wanted && Chests.contains(level, pos, part.item())))
			.toList();
		return Chests.closest(chests, villager);
	}

	private void fetch(ServerLevel level, Villager villager, Workstations.Station workplace, long timestamp) {
		Recipe recipe = this.recipe(workplace);
		List<Recipe> recipes = recipes(workplace);
		if (this.trip == null) {
			if (this.fetchFrom == null || recipe == null) {
				this.setState(State.PLAN);
				return;
			}
			this.trip = ChestTrip.to(level, villager, this.fetchFrom);
		}
		SimpleContainer inventory = villager.getInventory();
		int[] taken = {0};
		ChestTrip.Result result = this.trip.tick(level, villager, container -> {
			// Back with what the order does not need
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				ItemStack stack = inventory.getItem(slot);
				if (isUnneeded(stack, recipe, recipes)) {
					inventory.setItem(slot, HopperBlockEntity.addItem(null, container, stack, null));
				}
			}
			for (Part part : recipe.parts()) {
				int missing = part.amount() * recipe.batch() - available(inventory, part);
				if (missing <= 0) {
					continue;
				}
				if (part != HEAD) {
					taken[0] += Chests.take(container, inventory, part.item(), missing);
					continue;
				}
				// Heads first, then what heads are made from
				int before = available(inventory, part);
				for (Item head : HEADS) {
					missing -= Chests.take(container, inventory, stack -> stack.is(head), Math.max(0, missing));
				}
				for (Map.Entry<Item, Integer> source : HEAD_SOURCES.entrySet()) {
					if (missing > 0) {
						int took = Chests.take(container, inventory, stack -> stack.is(source.getKey()), Mth.positiveCeilDiv(missing, source.getValue()));
						missing -= took * source.getValue();
					}
				}
				taken[0] += available(inventory, part) - before;
			}
		});
		if (result == ChestTrip.Result.RUNNING) {
			return;
		}
		this.trip = null;
		if (result == ChestTrip.Result.FAILED || recipe == null || possible(inventory, recipe) == 0 && taken[0] == 0) {
			if (this.current != null) {
				this.fetchFailedAt.put(this.current, timestamp);
			}
		}
		this.setState(State.PLAN);
	}

	private void walk(ServerLevel level, Villager villager, Workstations.Station workplace) {
		Optional<BlockPos> spot = StandSpots.find(level, workplace.pos(), villager);
		boolean there = spot.map(pos -> StandSpots.isAt(villager, pos)).orElse(workplace.pos().closerToCenterThan(villager.position(), 2.5));
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(workplace.pos()));
		if (there) {
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			this.setState(State.CRAFT);
		} else if (this.stateTicks > WALK_TIMEOUT) {
			this.idle(villager, workplace, "fletcher.table_unreachable");
		} else if (this.stateTicks % 20 == 1) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(spot.orElse(workplace.pos()), SPEED, spot.isPresent() ? 0 : 1));
		}
	}

	/** One piece per second, as long as there is material and room. */
	private void craft(ServerLevel level, Villager villager, Workstations.Station workplace) {
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(workplace.pos()));
		if (this.stateTicks % CRAFT_TICKS != 0) {
			return;
		}
		SimpleContainer inventory = villager.getInventory();
		Recipe recipe = this.recipe(workplace);
		int stock = workplace.stock();
		if (recipe == null || possible(inventory, recipe) == 0 || !inventory.canAddItem(new ItemStack(recipe.product(), recipe.count()))
			|| stock > 0 && inOutputs(level, workplace, recipe.product()) + inventory.countItem(recipe.product()) >= stock) {
			this.setState(hasProducts(inventory, recipe) ? State.DELIVER : State.PLAN);
			return;
		}
		for (Part part : recipe.parts()) {
			consume(inventory, part);
		}
		ItemStack rest = inventory.addItem(new ItemStack(recipe.product(), recipe.count()));
		if (!rest.isEmpty()) {
			villager.spawnAtLocation(level, rest);
		}
		((HiredVillager) villager).workingvillagers$addWorkDone(ChopTrees.today(level), recipe.count());
		workplace.addMade(recipe.product(), recipe.count());
		villager.swingForAttack(InteractionHand.MAIN_HAND);
		level.playSound(null, workplace.pos(), SoundEvents.VILLAGER_WORK_FLETCHER, SoundSource.BLOCKS, 0.6F, 1.0F);
	}

	/** Uses up one piece's worth of the part; a head is knapped from gravel or cut from an ingot if needed. */
	private static void consume(SimpleContainer inventory, Part part) {
		if (part == HEAD) {
			for (Item head : HEADS) {
				if (take(inventory, stack -> stack.is(head), 1) == 1) {
					return;
				}
			}
			for (Map.Entry<Item, Integer> source : HEAD_SOURCES.entrySet()) {
				if (take(inventory, stack -> stack.is(source.getKey()), 1) == 1) {
					if (source.getValue() > 1) {
						// The rest of the ingot as nuggets
						Item nugget = source.getKey() == Items.COPPER_INGOT ? Items.COPPER_NUGGET : Items.IRON_NUGGET;
						inventory.addItem(new ItemStack(nugget, source.getValue() - 1));
					}
					return;
				}
			}
			return;
		}
		take(inventory, part.item(), part.amount());
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

	private void deliver(ServerLevel level, Villager villager, Workstations.Station workplace) {
		Recipe recipe = this.recipe(workplace);
		if (this.trip == null) {
			if (!hasProducts(villager.getInventory(), recipe)) {
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
				if (!stack.isEmpty() && isProduct(stack, recipe)) {
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
			case PLAN, WALK -> WorkerStatus.task(villager, "fletcher.plan");
			case FETCH -> WorkerStatus.task(villager, "fletcher.fetch");
			case CRAFT -> WorkerStatus.task(villager, "fletcher.craft");
			case DELIVER -> WorkerStatus.task(villager, "deliver");
			case WAIT -> WorkerStatus.task(villager, this.waitReason, this.waitArgs);
		}
	}

	private void setState(State state) {
		this.state = state;
		this.stateTicks = 0;
	}
}
