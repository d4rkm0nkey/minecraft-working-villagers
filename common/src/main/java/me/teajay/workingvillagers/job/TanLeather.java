package me.teajay.workingvillagers.job;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The work of the tanner at the cauldron: it makes the ordered leather goods (books, leather
 * armor, bundles, saddles, ...) from its input chests and the village chests
 * ({@link Orders#sources}), as many as ordered or a stock of each in its output chests; with
 * several, a batch of the most needed one there is material for
 * ({@link Workstations.Station#ordersByNeed}). Leather armor it dyes in the color set at the cauldron.
 *
 * <p>Leather it takes from the input chests, makes of rabbit hide (4 hides, 1 leather, like the
 * vanilla recipe), or tans of rotten flesh: up to 16 go into the water of the cauldron (one
 * level) and are leather after a while ({@link Config#TANNING_SECONDS}), meanwhile it does
 * other work. It fills the cauldron with a bucket at water nearby.
 *
 * <p>Leather armor of another color in its input chests it washes out in the cauldron (one
 * level, like a player) and dyes again.
 */
public class TanLeather extends Behavior<Villager> {
	private static final float SPEED = 0.5F;
	private static final int WALK_TIMEOUT = 400;
	private static final int WAIT_TICKS = 100;
	/** One piece per second. */
	private static final int WORK_TICKS = 20;
	/** After fetching from the input chests found nothing, how long it does not try again. */
	private static final long FETCH_COOLDOWN = 600L;
	private static final int HIDES_PER_LEATHER = 4;
	/** At most this much rotten flesh goes into the cauldron at once. */
	private static final int TAN_BATCH = 16;
	/** At most this many pieces of armor of another color it takes along to dye again. */
	private static final int RECOLOR_BATCH = 3;

	/** What a product takes besides leather (and the dye of dyed ones). */
	private record Part(Item item, int amount) {
	}

	private record Recipe(Item product, int leather, List<Part> parts, boolean dyed, int batch) {
	}

	private static final Map<Item, Recipe> RECIPES = Map.of(
		Items.BOOK, new Recipe(Items.BOOK, 1, List.of(new Part(Items.PAPER, 3)), false, 4),
		Items.LEATHER_HELMET, new Recipe(Items.LEATHER_HELMET, 5, List.of(), true, 1),
		Items.LEATHER_CHESTPLATE, new Recipe(Items.LEATHER_CHESTPLATE, 8, List.of(), true, 1),
		Items.LEATHER_LEGGINGS, new Recipe(Items.LEATHER_LEGGINGS, 7, List.of(), true, 1),
		Items.LEATHER_BOOTS, new Recipe(Items.LEATHER_BOOTS, 4, List.of(), true, 1),
		Items.LEATHER_HORSE_ARMOR, new Recipe(Items.LEATHER_HORSE_ARMOR, 7, List.of(), true, 1),
		Items.BUNDLE, new Recipe(Items.BUNDLE, 1, List.of(new Part(Items.STRING, 1)), false, 4),
		Items.SADDLE, new Recipe(Items.SADDLE, 3, List.of(new Part(Items.IRON_INGOT, 1)), false, 1)
	);
	/** What can be ordered, in the order of the overview: leather itself, then what it is made into. */
	public static final List<Item> PRODUCTS = List.of(Items.LEATHER, Items.BOOK, Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS,
		Items.LEATHER_BOOTS, Items.LEATHER_HORSE_ARMOR, Items.BUNDLE, Items.SADDLE);
	/** What it dyes and washes out. */
	private static final Set<Item> DYEABLE = Set.of(Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS,
		Items.LEATHER_HORSE_ARMOR, Items.WOLF_ARMOR);

	private enum State { PLAN, FETCH, WALK, WORK, WATER, DELIVER, WAIT }

	private State state = State.PLAN;
	private int stateTicks;
	private String waitReason = "tanner.no_order";
	private Object[] waitArgs = new Object[0];
	private @Nullable ChestTrip trip;
	private @Nullable BlockPos fetchFrom;
	private long fetchFailedAt = Long.MIN_VALUE / 2;
	private @Nullable BlockPos waterSpot;
	/** The product it makes right now, null before it picked one. */
	private @Nullable Item current;
	private final WorkerSupplies supplies = new WorkerSupplies();

	public TanLeather() {
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
		if (!villager.getVillagerData().profession().is(WorkingVillagers.TANNER_PROFESSION)) {
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
		if (this.current != null && !workplace.orders().contains(this.current)) {
			this.current = null; // the order changed
		}
		if (workplace.tanning() > 0 && level.getGameTime() < workplace.tannedAt() && villager.tickCount % 40 == 0) {
			BlockPos pos = workplace.pos();
			level.sendParticles(ParticleTypes.BUBBLE_POP, pos.getX() + 0.5, pos.getY() + 0.95, pos.getZ() + 0.5, 3, 0.2, 0.0, 0.2, 0.0);
		}
		if (this.state == State.PLAN && this.supplies.busy(level, villager)) {
			return; // the chest sets the status
		}
		switch (this.state) {
			case PLAN -> this.plan(level, villager, workplace, timestamp);
			case FETCH -> this.fetch(level, villager, workplace, timestamp);
			case WALK -> this.walk(level, villager, workplace);
			case WORK -> this.work(level, villager, workplace);
			case WATER -> this.water(level, villager, workplace);
			case DELIVER -> this.deliver(level, villager, workplace);
			case WAIT -> {
				if (this.stateTicks >= WAIT_TICKS) {
					this.setState(State.PLAN);
				}
			}
		}
		this.report(villager, level, workplace);
	}

	// --- What it has and needs ---

	/** The recipe of the product it makes right now, null for none or leather. */
	private @Nullable Recipe recipe() {
		return this.current == null ? null : RECIPES.get(this.current);
	}

	/** Whether it makes leather itself right now. */
	private boolean leatherOrder() {
		return this.current == Items.LEATHER;
	}

	private static int count(SimpleContainer inventory, Predicate<ItemStack> filter) {
		return WorkerSupplies.count(inventory, filter);
	}

	private static int count(SimpleContainer inventory, Item item) {
		return count(inventory, stack -> stack.is(item));
	}

	/** Whether the stack is dyed in the color, as the tanner dyes it. */
	public static boolean hasColor(ItemStack stack, DyeColor dye) {
		DyedItemColor color = stack.get(DataComponents.DYED_COLOR);
		return color != null && color.rgb() == rgb(dye);
	}

	/** The color a dye gives undyed leather. */
	public static int rgb(DyeColor dye) {
		DyedItemColor color = DyedItemColor.applyDyes(new ItemStack(Items.LEATHER_CHESTPLATE), List.of(dye)).get(DataComponents.DYED_COLOR);
		return color == null ? DyedItemColor.LEATHER_COLOR : color.rgb();
	}

	/** Leather armor that has to be dyed (again) in the color of the station. */
	private static boolean toRecolor(ItemStack stack, @Nullable DyeColor dye) {
		return dye != null && !stack.isEmpty() && DYEABLE.contains(stack.getItem()) && !hasColor(stack, dye);
	}

	private static boolean isTanningOn() {
		return Config.TAN_ROTTEN_FLESH.get();
	}

	/**
	 * What it brings to the output chests: what it made (leather only when it is the order),
	 * armor in the right color, and leather goods it does not make with.
	 */
	private static boolean isProduct(ItemStack stack, Workstations.Station workplace) {
		if (stack.isEmpty()) {
			return false;
		}
		if (stack.is(Items.LEATHER)) {
			return !workplace.hasOrder() || workplace.orders().contains(Items.LEATHER);
		}
		if (DYEABLE.contains(stack.getItem())) {
			return !toRecolor(stack, workplace.dye());
		}
		return PRODUCTS.contains(stack.getItem());
	}

	private static boolean hasProducts(SimpleContainer inventory, Workstations.Station workplace) {
		return inventory.hasAnyMatching(stack -> isProduct(stack, workplace));
	}

	/** What it keeps for its work; the rest goes back into an input chest. */
	private static boolean isNeeded(ItemStack stack, Workstations.Station workplace) {
		// Leather and parts of every product of the order, so switching between them does not carry things back
		if (WorkerSupplies.isWage(stack) || stack.is(Items.BUCKET) || stack.is(Items.WATER_BUCKET) || isProduct(stack, workplace)
			|| toRecolor(stack, workplace.dye())) {
			return true;
		}
		if (stack.is(Items.LEATHER) || stack.is(Items.RABBIT_HIDE) || stack.is(Items.ROTTEN_FLESH) && isTanningOn()) {
			return workplace.hasOrder();
		}
		if (workplace.dye() != null && stack.is(Items.DYE.pick(workplace.dye()))) {
			return true;
		}
		return workplace.orders().stream().map(RECIPES::get).filter(Objects::nonNull)
			.anyMatch(recipe -> recipe.parts().stream().anyMatch(part -> stack.is(part.item())));
	}

	/** The water in the cauldron, 0 to 3; -1 if it holds lava or powder snow. */
	private static int water(BlockState state) {
		if (state.is(Blocks.WATER_CAULDRON)) {
			return state.getValue(LayeredCauldronBlock.LEVEL);
		}
		return state.is(Blocks.CAULDRON) ? 0 : -1;
	}

	private static int water(ServerLevel level, Workstations.Station workplace) {
		return water(level.getBlockState(workplace.pos()));
	}

	/** How much leather one piece of the product takes, 0 for none. */
	private int leatherFor() {
		Recipe recipe = this.recipe();
		return this.leatherOrder() ? 1 : recipe == null ? 0 : recipe.leather();
	}

	private static boolean hasParts(SimpleContainer inventory, Recipe recipe, @Nullable DyeColor dye) {
		return recipe.parts().stream().allMatch(part -> count(inventory, part.item()) >= part.amount())
			&& (!recipe.dyed() || dye == null || count(inventory, Items.DYE.pick(dye)) > 0);
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

	/** How many more of the product the order wants, counting what the tanner carries; see {@link Workstations.Station#wanted}. */
	private static int wanted(ServerLevel level, Workstations.Station workplace, SimpleContainer inventory, Item product) {
		return workplace.wanted(product, workplace.stocked(product) ? inOutputs(level, workplace, product) + count(inventory, product) : 0);
	}

	// --- Planning ---

	private void plan(ServerLevel level, Villager villager, Workstations.Station workplace, long timestamp) {
		SimpleContainer inventory = villager.getInventory();
		DyeColor dye = workplace.dye();
		if (workplace.tanning() > 0 && level.getGameTime() >= workplace.tannedAt() || this.hasCauldronWork(level, inventory, workplace)) {
			this.setState(State.WALK);
			return;
		}
		if (hasProducts(inventory, workplace)) {
			this.setState(State.DELIVER);
			return;
		}
		// Everything made is handed in: the orders that are done are done
		Orders.finish(level, villager, workplace);
		// Armor of another color: wash out and dye again
		if (dye != null && inventory.hasAnyMatching(stack -> toRecolor(stack, dye))) {
			if (!this.fetchFromInputs(level, villager, workplace, stack -> stack.is(Items.DYE.pick(dye)), timestamp)) {
				this.idle(villager, workplace, "tanner.no_material", new ItemStack(Items.DYE.pick(dye)).getHoverName());
			}
			return;
		}
		if (dye != null && this.fetchFromInputs(level, villager, workplace, stack -> toRecolor(stack, dye), timestamp)) {
			return;
		}
		if (!workplace.hasOrder()) {
			this.idle(villager, workplace, "tanner.no_order");
			return;
		}
		// Only what its level lets it make (leather armor from apprentice, saddles from journeyman)
		int lockedUntil = workplace.orders().stream().mapToInt(product -> Unlocks.level(WorkingVillagers.TANNER_PROFESSION, product)).min().orElse(WorkerLevels.NOVICE);
		if (villager.getVillagerData().level() < lockedUntil) {
			this.idle(villager, workplace, "locked", WorkerLevels.name(lockedUntil));
			return;
		}
		List<Item> byNeed = workplace.ordersByNeed(product -> inOutputs(level, workplace, product)).stream()
			.filter(product -> Unlocks.can(villager, product))
			.toList();
		if (byNeed.isEmpty()) {
			Orders.Idle idle = Orders.nothingToMake(level, workplace, "tanner");
			this.idle(villager, workplace, idle.key(), idle.args());
			return;
		}
		// The most needed product it carries enough for, or else finds material for
		List<BlockPos> sources = Orders.sources(level, workplace);
		this.current = byNeed.stream().filter(product -> carriesEnough(inventory, product, dye)).findFirst()
			.or(() -> byNeed.stream().filter(product -> sources.stream()
				.anyMatch(pos -> Chests.contains(level, pos, lacking(inventory, workplace, product)))).findFirst())
			.orElse(byNeed.getFirst());
		// The cauldron needs water to tan
		boolean flesh = isTanningOn() && workplace.tanning() == 0 && count(inventory, Items.ROTTEN_FLESH) >= Config.FLESH_PER_LEATHER.get();
		if (flesh && water(level, workplace) == 0 && count(inventory, Items.WATER_BUCKET) == 0 && count(inventory, Items.BUCKET) > 0) {
			this.waterSpot = null;
			this.setState(State.WATER);
			return;
		}
		Recipe recipe = this.recipe();
		if (this.fetchFromInputs(level, villager, workplace, lacking(inventory, workplace, this.current), timestamp)) {
			return;
		}
		if (workplace.tanning() > 0) {
			this.idle(villager, workplace, "tanner.tanning");
		} else if (flesh && water(level, workplace) <= 0) {
			this.idle(villager, workplace, "tanner.no_water");
		} else if (recipe != null && count(inventory, Items.LEATHER) >= recipe.leather()) {
			Part missing = recipe.parts().stream().filter(part -> count(inventory, part.item()) < part.amount()).findFirst().orElse(null);
			Item item = missing != null ? missing.item() : dye != null ? Items.DYE.pick(dye) : Items.LEATHER;
			this.idle(villager, workplace, "tanner.no_material", new ItemStack(item).getHoverName());
		} else {
			this.idle(villager, workplace, "tanner.no_material", new ItemStack(Items.LEATHER).getHoverName());
		}
	}

	/** What it lacks for the product and takes from the input chests. */
	private static Predicate<ItemStack> lacking(SimpleContainer inventory, Workstations.Station workplace, Item product) {
		Recipe recipe = RECIPES.get(product);
		DyeColor dye = workplace.dye();
		return stack -> stack.is(Items.LEATHER) && product != Items.LEATHER || stack.is(Items.RABBIT_HIDE)
			|| stack.is(Items.ROTTEN_FLESH) && isTanningOn() && workplace.tanning() == 0
			|| recipe != null && recipe.parts().stream().anyMatch(part -> stack.is(part.item()) && count(inventory, part.item()) < part.amount())
			|| recipe != null && recipe.dyed() && dye != null && stack.is(Items.DYE.pick(dye));
	}

	/** Whether it carries enough to make one piece of the product (leather: of hides). */
	private static boolean carriesEnough(SimpleContainer inventory, Item product, @Nullable DyeColor dye) {
		Recipe recipe = RECIPES.get(product);
		if (recipe == null) {
			return count(inventory, Items.RABBIT_HIDE) >= HIDES_PER_LEATHER;
		}
		int leather = count(inventory, Items.LEATHER) + count(inventory, Items.RABBIT_HIDE) / HIDES_PER_LEATHER;
		return leather >= recipe.leather() && hasParts(inventory, recipe, dye);
	}

	/** Whether there is something to do at the cauldron with what it carries (see {@link #workOnce}). */
	private boolean hasCauldronWork(ServerLevel level, SimpleContainer inventory, Workstations.Station workplace) {
		return this.nextWork(level, inventory, workplace) != null;
	}

	private boolean fetchFromInputs(ServerLevel level, Villager villager, Workstations.Station workplace, Predicate<ItemStack> lacking, long timestamp) {
		if (timestamp - this.fetchFailedAt <= FETCH_COOLDOWN) {
			return false;
		}
		Optional<BlockPos> chest = Chests.closest(Orders.sources(level, workplace).stream().filter(pos -> Chests.contains(level, pos, lacking)).toList(), villager);
		if (chest.isEmpty()) {
			return false;
		}
		this.fetchFrom = chest.get();
		this.setState(State.FETCH);
		return true;
	}

	private void fetch(ServerLevel level, Villager villager, Workstations.Station workplace, long timestamp) {
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
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				ItemStack stack = inventory.getItem(slot);
				if (!stack.isEmpty() && !isNeeded(stack, workplace)) {
					inventory.setItem(slot, HopperBlockEntity.addItem(null, container, stack, null));
				}
			}
			taken[0] += this.take(level, container, inventory, workplace);
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

	/** Takes what a batch of the order needs, and armor to dye again with its dye. */
	private int take(ServerLevel level, Container container, SimpleContainer inventory, Workstations.Station workplace) {
		int taken = 0;
		DyeColor dye = workplace.dye();
		if (dye != null) {
			int recolor = RECOLOR_BATCH - count(inventory, stack -> toRecolor(stack, dye));
			taken += Chests.take(container, inventory, stack -> toRecolor(stack, dye), Math.max(0, recolor));
			int dyes = count(inventory, stack -> toRecolor(stack, dye)) + (this.recipe() != null && this.recipe().dyed() ? 1 : 0);
			taken += Chests.take(container, inventory, stack -> stack.is(Items.DYE.pick(dye)), Math.max(0, dyes - count(inventory, Items.DYE.pick(dye))));
		}
		Recipe recipe = this.recipe();
		if (this.current == null) {
			return taken;
		}
		int batch = recipe == null ? TAN_BATCH / Config.FLESH_PER_LEATHER.get() : recipe.batch();
		if (workplace.limited(this.current)) {
			// No more than the order still needs
			batch = Math.max(1, Math.min(batch, wanted(level, workplace, inventory, this.current)));
		}
		if (recipe != null) {
			for (Part part : recipe.parts()) {
				taken += Chests.take(container, inventory, stack -> stack.is(part.item()), Math.max(0, part.amount() * batch - count(inventory, part.item())));
			}
		}
		int leather = this.leatherFor() * batch - (this.leatherOrder() ? 0 : count(inventory, Items.LEATHER));
		if (!this.leatherOrder() && leather > 0) {
			int took = Chests.take(container, inventory, stack -> stack.is(Items.LEATHER), leather);
			leather -= took;
			taken += took;
		}
		if (leather > 0) {
			int hides = leather * HIDES_PER_LEATHER - count(inventory, Items.RABBIT_HIDE);
			taken += Chests.take(container, inventory, stack -> stack.is(Items.RABBIT_HIDE), Math.max(0, hides));
			leather -= count(inventory, Items.RABBIT_HIDE) / HIDES_PER_LEATHER;
		}
		if (leather > 0 && isTanningOn() && workplace.tanning() == 0) {
			int flesh = Math.min(TAN_BATCH, leather * Config.FLESH_PER_LEATHER.get()) - count(inventory, Items.ROTTEN_FLESH);
			taken += Chests.take(container, inventory, stack -> stack.is(Items.ROTTEN_FLESH), Math.max(0, flesh));
		}
		return taken;
	}

	// --- At the cauldron ---

	private void walk(ServerLevel level, Villager villager, Workstations.Station workplace) {
		Optional<BlockPos> spot = StandSpots.find(level, workplace.pos(), villager);
		boolean there = spot.map(pos -> StandSpots.isAt(villager, pos)).orElse(workplace.pos().closerToCenterThan(villager.position(), 2.5));
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(workplace.pos()));
		if (there) {
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			this.setState(State.WORK);
		} else if (this.stateTicks > WALK_TIMEOUT) {
			this.idle(villager, workplace, "tanner.cauldron_unreachable");
		} else if (this.stateTicks % 20 == 1) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(spot.orElse(workplace.pos()), SPEED, spot.isPresent() ? 0 : 1));
		}
	}

	/** What the tanner does next at the cauldron. */
	private enum Work { COLLECT, POUR, TAN, RECOLOR, HIDES, CRAFT }

	private @Nullable Work nextWork(ServerLevel level, SimpleContainer inventory, Workstations.Station workplace) {
		long now = level.getGameTime();
		int water = water(level, workplace);
		DyeColor dye = workplace.dye();
		if (workplace.tanning() > 0 && now >= workplace.tannedAt()) {
			return Work.COLLECT;
		}
		if (count(inventory, Items.WATER_BUCKET) > 0 && water >= 0 && water < LayeredCauldronBlock.MAX_FILL_LEVEL) {
			return Work.POUR;
		}
		if (isTanningOn() && workplace.tanning() == 0 && water > 0 && workplace.hasOrder()
			&& count(inventory, Items.ROTTEN_FLESH) >= Config.FLESH_PER_LEATHER.get()) {
			return Work.TAN;
		}
		if (dye != null && count(inventory, Items.DYE.pick(dye)) > 0) {
			for (ItemStack stack : inventory) {
				// Dyed ones have to be washed out first
				if (toRecolor(stack, dye) && (!stack.has(DataComponents.DYED_COLOR) || water > 0)) {
					return Work.RECOLOR;
				}
			}
		}
		Recipe recipe = this.recipe();
		Item order = this.current;
		if (order == null || workplace.limited(order) && wanted(level, workplace, inventory, order) <= 0) {
			return null;
		}
		int leather = count(inventory, Items.LEATHER);
		if (leather < this.leatherFor() && count(inventory, Items.RABBIT_HIDE) >= HIDES_PER_LEATHER) {
			return Work.HIDES;
		}
		if (recipe != null && leather >= recipe.leather() && hasParts(inventory, recipe, dye) && inventory.canAddItem(new ItemStack(recipe.product()))) {
			return Work.CRAFT;
		}
		return null;
	}

	/** One piece of work per second at the cauldron, as long as there is some. */
	private void work(ServerLevel level, Villager villager, Workstations.Station workplace) {
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(workplace.pos()));
		if (this.stateTicks % WorkerLevels.ticks(villager, WORK_TICKS) != 0) {
			return;
		}
		Work work = this.nextWork(level, villager.getInventory(), workplace);
		if (work == null) {
			this.setState(State.PLAN);
			return;
		}
		this.workOnce(level, villager, workplace, work);
		villager.swingForAttack(InteractionHand.MAIN_HAND);
	}

	private void workOnce(ServerLevel level, Villager villager, Workstations.Station workplace, Work work) {
		SimpleContainer inventory = villager.getInventory();
		BlockPos pos = workplace.pos();
		BlockState cauldron = level.getBlockState(pos);
		DyeColor dye = workplace.dye();
		switch (work) {
			case COLLECT -> {
				ItemStack contents = workplace.tanningContents(level.getGameTime());
				workplace.setTanning(0, 0L);
				if (contents.is(Items.LEATHER) && workplace.orders().contains(Items.LEATHER)) {
					((HiredVillager) villager).workingvillagers$addWorkDone(ChopTrees.today(level), contents.getCount());
					workplace.addMade(Items.LEATHER, contents.getCount());
				}
				addOrDrop(level, villager, contents);
				level.playSound(null, pos, SoundEvents.VILLAGER_WORK_LEATHERWORKER, SoundSource.BLOCKS, 0.6F, 1.0F);
			}
			case POUR -> {
				take(inventory, Items.WATER_BUCKET, 1);
				inventory.addItem(new ItemStack(Items.BUCKET));
				level.setBlock(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL), Block.UPDATE_ALL);
				level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
			}
			case TAN -> {
				int per = Config.FLESH_PER_LEATHER.get();
				int flesh = Math.min(TAN_BATCH, count(inventory, Items.ROTTEN_FLESH)) / per * per;
				take(inventory, Items.ROTTEN_FLESH, flesh);
				workplace.setTanning(flesh, level.getGameTime() + Config.TANNING_SECONDS.get() * 20L);
				LayeredCauldronBlock.lowerFillLevel(cauldron, level, pos);
				level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.5F, 1.0F);
			}
			case RECOLOR -> {
				for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
					ItemStack stack = inventory.getItem(slot);
					if (!toRecolor(stack, dye)) {
						continue;
					}
					if (stack.has(DataComponents.DYED_COLOR)) {
						// Washed out, like a player does it
						stack.remove(DataComponents.DYED_COLOR);
						LayeredCauldronBlock.lowerFillLevel(cauldron, level, pos);
						level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.5F, 1.2F);
					}
					take(inventory, Items.DYE.pick(dye), 1);
					inventory.setItem(slot, DyedItemColor.applyDyes(stack, List.of(dye)));
					((HiredVillager) villager).workingvillagers$addWorkDone(ChopTrees.today(level), 1);
					break;
				}
			}
			case HIDES -> {
				take(inventory, Items.RABBIT_HIDE, HIDES_PER_LEATHER);
				addOrDrop(level, villager, new ItemStack(Items.LEATHER));
				if (this.leatherOrder()) {
					((HiredVillager) villager).workingvillagers$addWorkDone(ChopTrees.today(level), 1);
					workplace.addMade(Items.LEATHER, 1);
				}
				level.playSound(null, pos, SoundEvents.VILLAGER_WORK_LEATHERWORKER, SoundSource.BLOCKS, 0.6F, 1.0F);
			}
			case CRAFT -> {
				Recipe recipe = this.recipe();
				if (recipe == null) {
					return;
				}
				take(inventory, Items.LEATHER, recipe.leather());
				recipe.parts().forEach(part -> take(inventory, part.item(), part.amount()));
				ItemStack product = new ItemStack(recipe.product());
				if (recipe.dyed() && dye != null) {
					take(inventory, Items.DYE.pick(dye), 1);
					product = DyedItemColor.applyDyes(product, List.of(dye));
				}
				addOrDrop(level, villager, product);
				((HiredVillager) villager).workingvillagers$addWorkDone(ChopTrees.today(level), 1);
				workplace.addMade(recipe.product(), 1);
				level.playSound(null, pos, SoundEvents.VILLAGER_WORK_LEATHERWORKER, SoundSource.BLOCKS, 0.6F, 1.0F);
			}
		}
	}

	private static void addOrDrop(ServerLevel level, Villager villager, ItemStack stack) {
		ItemStack rest = villager.getInventory().addItem(stack);
		if (!rest.isEmpty()) {
			villager.spawnAtLocation(level, rest);
		}
	}

	private static int take(SimpleContainer inventory, Item item, int amount) {
		int taken = 0;
		for (int slot = 0; slot < inventory.getContainerSize() && taken < amount; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty() && stack.is(item)) {
				int now = Math.min(amount - taken, stack.getCount());
				stack.shrink(now);
				taken += now;
				inventory.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
			}
		}
		return taken;
	}

	// --- Water ---

	/** Fills its bucket at water near the cauldron; pouring it in is work at the cauldron. */
	private void water(ServerLevel level, Villager villager, Workstations.Station workplace) {
		if (this.waterSpot == null) {
			Optional<BlockPos> spot = WaterSpots.findShore(level, workplace.pos(), Config.TANNER_WATER_RADIUS.get(), villager);
			if (spot.isEmpty()) {
				this.idle(villager, workplace, "tanner.no_water");
				return;
			}
			this.waterSpot = spot.get();
		}
		BlockPos water = WaterSpots.closestWater(level, this.waterSpot);
		if (water == null) {
			this.waterSpot = null;
			return;
		}
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(water));
		if (StandSpots.isAt(villager, this.waterSpot) || water.closerToCenterThan(villager.position(), 2.0)) {
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			SimpleContainer inventory = villager.getInventory();
			if (take(inventory, Items.BUCKET, 1) == 1) {
				// The water stays: a bucket from a pond, like an endless source
				addOrDrop(level, villager, new ItemStack(Items.WATER_BUCKET));
				level.playSound(null, villager, SoundEvents.BUCKET_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
				villager.swingForAttack(InteractionHand.MAIN_HAND);
			}
			this.setState(State.WALK);
		} else if (this.stateTicks > WALK_TIMEOUT) {
			this.idle(villager, workplace, "tanner.no_water");
		} else if (this.stateTicks % 20 == 1) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.waterSpot, SPEED, 0));
		}
	}

	// --- Delivering ---

	private void deliver(ServerLevel level, Villager villager, Workstations.Station workplace) {
		if (this.trip == null) {
			if (!hasProducts(villager.getInventory(), workplace)) {
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
				if (isProduct(stack, workplace)) {
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

	private void report(Villager villager, ServerLevel level, Workstations.Station workplace) {
		switch (this.state) {
			case PLAN, WALK -> WorkerStatus.task(villager, "tanner.plan");
			case FETCH -> WorkerStatus.task(villager, "tanner.fetch");
			case WORK -> WorkerStatus.task(villager, "tanner.work");
			case WATER -> WorkerStatus.task(villager, "tanner.fill");
			case DELIVER -> WorkerStatus.task(villager, "deliver");
			case WAIT -> {
				if (this.waitReason.equals("tanner.tanning")) {
					long minutes = Math.max(1L, (workplace.tannedAt() - level.getGameTime() + 1199L) / 1200L);
					WorkerStatus.task(villager, "tanner.tanning", minutes);
				} else {
					WorkerStatus.task(villager, this.waitReason, this.waitArgs);
				}
			}
		}
	}

	private void setState(State state) {
		this.state = state;
		this.stateTicks = 0;
	}
}
