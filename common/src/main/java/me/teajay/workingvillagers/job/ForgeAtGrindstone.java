package me.teajay.workingvillagers.job;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.crafting.CarpenterRecipes;
import me.teajay.workingvillagers.crafting.ForgeRecipes;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jspecify.annotations.Nullable;

/**
 * The work of the weaponsmith at the grindstone, in this order:
 *
 * <ol>
 * <li><b>Grinding together</b>, like a player at a grindstone: two worn tools of the same kind
 * (given back by the workers to the village chests, or damaged ones in its input chests) make
 * one, with the durability of both plus 5%. It takes them from one chest and brings the
 * result back there. Never enchanted or renamed ones: the grindstone would take the
 * enchantments off, those are the smith's.</li>
 * <li><b>Disenchanting</b> what lies in its input chests: all enchantments but curses come off,
 * like at a grindstone, and the experience goes into bottles o' enchanting for the smith.</li>
 * <li><b>Forging</b> the ordered tools and weapons from its input chests and the village
 * chests (see {@link ForgeRecipes}, {@link Orders#sources}), as many as ordered or until as
 * many of each lie in the output chests as the stock; with several, a batch of the most
 * needed one there is material for ({@link Workstations.Station#ordersByNeed}).</li>
 * </ol>
 *
 * The station decides whether it grinds together and disenchants ({@link Workstations.Station#grindMode}).
 */
public class ForgeAtGrindstone extends Behavior<Villager> {
	private static final float SPEED = 0.5F;
	private static final int WALK_TIMEOUT = 400;
	private static final int WAIT_TICKS = 100;
	/** One piece every two seconds. */
	private static final int WORK_TICKS = 40;
	/** After fetching found nothing, how long it does not try the same again. */
	private static final long FETCH_COOLDOWN = 600L;
	/** At most this many pieces it makes or disenchants per trip. */
	private static final int BATCH = 4;
	/** How much more than both halves together a tool ground together has, like vanilla. */
	private static final int MERGE_BONUS_PERCENT = 5;
	/** Marks a tool ground together until it is back in its chest. */
	private static final String MERGED = "workingvillagers_merged";

	private enum State { PLAN, FETCH, WALK, WORK, RETURN, DELIVER, WAIT }

	private enum Task { MERGE, DISENCHANT, FORGE }

	private State state = State.PLAN;
	private Task task = Task.FORGE;
	private int stateTicks;
	private String waitReason = "weaponsmith.no_order";
	private Object[] waitArgs = new Object[0];
	private @Nullable ChestTrip trip;
	private @Nullable Consumer<Container> fetchAction;
	private @Nullable Task fetchFor;
	/** Where the tools ground together go back to. */
	private @Nullable BlockPos returnTo;
	private final Map<Task, Long> failedAt = new HashMap<>();
	/** The product it forges right now, null before it picked one. */
	private @Nullable Item current;
	private final WorkerSupplies supplies = new WorkerSupplies();

	public ForgeAtGrindstone() {
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
		if (!villager.getVillagerData().profession().is(WorkingVillagers.WEAPONSMITH_PROFESSION)) {
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
			case FETCH -> this.fetch(level, villager, timestamp);
			case WALK -> this.walk(level, villager, workplace);
			case WORK -> this.work(level, villager, workplace);
			case RETURN -> this.giveBack(level, villager, workplace);
			case DELIVER -> this.deliver(level, villager, workplace);
			case WAIT -> {
				if (this.stateTicks >= WAIT_TICKS) {
					this.setState(State.PLAN);
				}
			}
		}
		this.report(villager);
	}

	// --- What things are ---

	/** A damaged tool, weapon or piece of armor that can be ground together with another one. */
	static boolean isMergeable(ItemStack stack) {
		return !stack.isEmpty() && stack.isDamageableItem() && stack.isDamaged() && stack.getMaxStackSize() == 1
			&& !EnchantmentHelper.hasAnyEnchantments(stack) && !stack.has(DataComponents.CUSTOM_NAME) && !stack.has(DataComponents.CUSTOM_DATA);
	}

	/** Something with enchantments (not only curses) that it takes off; renamed things stay as they are. */
	static boolean isDisenchantable(ItemStack stack) {
		if (stack.isEmpty() || stack.has(DataComponents.CUSTOM_NAME)) {
			return false;
		}
		return EnchantmentHelper.getEnchantmentsForCrafting(stack).keySet().stream().anyMatch(enchantment -> !enchantment.is(EnchantmentTags.CURSE));
	}

	private static boolean isMerged(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data != null && data.copyTag().contains(MERGED);
	}

	/** The material of the order: what its recipes take. */
	private static Predicate<ItemStack> materialOf(List<CraftingRecipe> recipes) {
		return stack -> !stack.isEmpty() && recipes.stream()
			.anyMatch(recipe -> recipe.placementInfo().ingredients().stream().anyMatch(ingredient -> ingredient.test(stack)));
	}

	/** What it brings to the output chests: what it made or disenchanted, the bottles o' enchanting. */
	private static boolean isProduct(ItemStack stack, Predicate<ItemStack> isMaterial, Workstations.Station workplace) {
		return Chests.isHandedIn(stack) && !isMaterial.test(stack) && !stack.is(Items.GLASS_BOTTLE) && !isMerged(stack)
			&& !(workplace.disenchants() && isDisenchantable(stack));
	}

	private static int count(Container container, Predicate<ItemStack> filter) {
		return WorkerSupplies.count(container, filter);
	}

	/** An item of which the inventory or container holds two to grind together, null if none. */
	private static @Nullable Item pair(Container container, Predicate<ItemStack> filter) {
		Map<Item, Integer> counts = new HashMap<>();
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot);
			if (isMergeable(stack) && filter.test(stack) && counts.merge(stack.getItem(), 1, Integer::sum) >= 2) {
				return stack.getItem();
			}
		}
		return null;
	}

	// --- Planning ---

	private void plan(ServerLevel level, Villager villager, Workstations.Station workplace, long timestamp) {
		SimpleContainer inventory = villager.getInventory();
		List<CraftingRecipe> recipes = recipesOf(level, workplace.orders());
		Predicate<ItemStack> isMaterial = materialOf(recipes);
		if (inventory.hasAnyMatching(ForgeAtGrindstone::isMerged)) {
			this.setState(State.RETURN);
			return;
		}
		if (workplace.merges() && pair(inventory, stack -> true) != null) {
			this.goToGrindstone(Task.MERGE);
			return;
		}
		boolean disenchants = workplace.disenchants() && Unlocks.can(villager, Unlocks.Ability.DISENCHANTING);
		if (disenchants && inventory.hasAnyMatching(ForgeAtGrindstone::isDisenchantable)) {
			this.goToGrindstone(Task.DISENCHANT);
			return;
		}
		this.bottle(level, villager);
		if (inventory.hasAnyMatching(stack -> isProduct(stack, isMaterial, workplace))) {
			this.setState(State.DELIVER);
			return;
		}
		// Everything made is handed in: the orders that are done are done
		Orders.finish(level, villager, workplace);
		if (workplace.merges() && this.mayTry(Task.MERGE, timestamp) && this.planMerge(level, villager, workplace)) {
			return;
		}
		if (disenchants && this.mayTry(Task.DISENCHANT, timestamp) && this.planDisenchant(level, villager, workplace)) {
			return;
		}
		this.planForge(level, villager, workplace, isMaterial, timestamp);
	}

	/** From which level the weaponsmith forges the product (its earliest recipe, see {@link Unlocks}). */
	private static int forgeLevel(ServerLevel level, Villager villager, Item product) {
		return ForgeRecipes.forResult(level, product).stream()
			.mapToInt(recipe -> Unlocks.level(villager, product, recipe.placementInfo().ingredients()))
			.min().orElse(WorkerLevels.NOVICE);
	}

	/** The recipes of the products. */
	private static List<CraftingRecipe> recipesOf(ServerLevel level, List<Item> products) {
		return products.stream().flatMap(product -> ForgeRecipes.forResult(level, product).stream()).toList();
	}

	private boolean mayTry(Task task, long timestamp) {
		return timestamp - this.failedAt.getOrDefault(task, Long.MIN_VALUE / 2) > FETCH_COOLDOWN;
	}

	/** Two worn tools of a kind in a village chest in reach, or two damaged ones in an input chest. */
	private boolean planMerge(ServerLevel level, Villager villager, Workstations.Station workplace) {
		List<Map.Entry<BlockPos, Predicate<ItemStack>>> sources = new ArrayList<>();
		VillageChests.inReach(level, workplace.pos()).forEach(pos -> sources.add(Map.entry(pos, WorkerSupplies::isWorn)));
		workplace.validInputs(level).forEach(pos -> sources.add(Map.entry(pos, stack -> true)));
		for (Map.Entry<BlockPos, Predicate<ItemStack>> source : sources) {
			Container container = Workstations.getContainer(level, source.getKey());
			Item item = container == null ? null : pair(container, source.getValue());
			if (item != null) {
				this.returnTo = source.getKey();
				this.fetchFrom(level, villager, source.getKey(), Task.MERGE,
					chest -> Chests.take(chest, villager.getInventory(), stack -> stack.is(item) && isMergeable(stack) && source.getValue().test(stack), 2));
				return true;
			}
		}
		return false;
	}

	/** Enchanted things in an input chest, with glass bottles for the experience. */
	private boolean planDisenchant(ServerLevel level, Villager villager, Workstations.Station workplace) {
		Optional<BlockPos> chest = Chests.closest(
			workplace.validInputs(level).stream().filter(pos -> Chests.contains(level, pos, ForgeAtGrindstone::isDisenchantable)).toList(), villager);
		if (chest.isEmpty()) {
			return false;
		}
		this.fetchFrom(level, villager, chest.get(), Task.DISENCHANT, container -> {
			SimpleContainer inventory = villager.getInventory();
			Chests.take(container, inventory, ForgeAtGrindstone::isDisenchantable, BATCH);
			Chests.take(container, inventory, stack -> stack.is(Items.GLASS_BOTTLE), Math.max(0, 8 - inventory.countItem(Items.GLASS_BOTTLE)));
		});
		return true;
	}

	private void planForge(ServerLevel level, Villager villager, Workstations.Station workplace, Predicate<ItemStack> isMaterial, long timestamp) {
		if (!workplace.hasOrder()) {
			this.idle(villager, workplace, "weaponsmith.no_order");
			return;
		}
		if (recipesOf(level, workplace.orders()).isEmpty()) {
			this.idle(villager, workplace, "weaponsmith.no_recipe");
			return;
		}
		// Only what its level lets it make (gold from apprentice, diamond from expert)
		int lockedUntil = workplace.orders().stream().mapToInt(product -> forgeLevel(level, villager, product)).min().orElse(WorkerLevels.NOVICE);
		if (villager.getVillagerData().level() < lockedUntil) {
			this.idle(villager, workplace, "locked", WorkerLevels.name(lockedUntil));
			return;
		}
		List<Item> byNeed = workplace.ordersByNeed(product -> this.inOutputs(level, workplace, product)).stream()
			.filter(product -> villager.getVillagerData().level() >= forgeLevel(level, villager, product))
			.toList();
		if (byNeed.isEmpty()) {
			Orders.Idle idle = Orders.nothingToMake(level, workplace, "weaponsmith");
			this.idle(villager, workplace, idle.key(), idle.args());
			return;
		}
		SimpleContainer inventory = villager.getInventory();
		StackedItemContents carried = new StackedItemContents();
		CarpenterRecipes.account(carried, inventory, inventory.getContainerSize(), isMaterial);
		for (Item product : byNeed) {
			if (CarpenterRecipes.firstCraftable(carried, ForgeRecipes.forResult(level, product)) != null) {
				this.current = product;
				this.goToGrindstone(Task.FORGE);
				return;
			}
		}
		StackedItemContents available = new StackedItemContents();
		CarpenterRecipes.account(available, inventory, inventory.getContainerSize(), isMaterial);
		for (BlockPos pos : Orders.sources(level, workplace)) {
			Container container = Workstations.getContainer(level, pos);
			if (container != null) {
				CarpenterRecipes.account(available, container, container.getContainerSize(), isMaterial);
			}
		}
		// The most needed product there is material for
		Item order = null;
		CraftingRecipe recipe = null;
		for (Item product : byNeed) {
			recipe = CarpenterRecipes.firstCraftable(available, ForgeRecipes.forResult(level, product));
			if (recipe != null) {
				order = product;
				break;
			}
		}
		List<CraftingRecipe> recipes = recipesOf(level, byNeed);
		if (recipe == null || !this.mayTry(Task.FORGE, timestamp)) {
			this.idle(villager, workplace, "weaponsmith.no_material", missing(level, villager, workplace, recipes, isMaterial));
			return;
		}
		this.current = order;
		int limit = workplace.limited(order) ? Math.min(BATCH, Orders.wanted(level, workplace, order)) : BATCH;
		int crafts = CarpenterRecipes.maxCrafts(available, recipe, Math.max(1, limit));
		Map<Item, Integer> needed = CarpenterRecipes.pick(available, recipe, crafts).orElse(Map.of());
		Map<Item, Integer> stillMissing = new LinkedHashMap<>();
		needed.forEach((item, amount) -> {
			int have = count(inventory, stack -> stack.is(item));
			if (amount > have) {
				stillMissing.put(item, amount - have);
			}
		});
		Optional<BlockPos> chest = Chests.closest(Orders.sources(level, workplace).stream()
			.filter(pos -> Chests.contains(level, pos, stack -> stillMissing.containsKey(stack.getItem()) && isMaterial.test(stack))).toList(), villager);
		if (chest.isEmpty()) {
			this.idle(villager, workplace, "weaponsmith.no_material", missing(level, villager, workplace, recipes, isMaterial));
			return;
		}
		this.fetchFrom(level, villager, chest.get(), Task.FORGE, container -> {
			// Back with what the order does not need, then the material
			SimpleContainer pockets = villager.getInventory();
			for (int slot = 0; slot < pockets.getContainerSize(); slot++) {
				ItemStack stack = pockets.getItem(slot);
				if (Chests.isHandedIn(stack) && !isMaterial.test(stack) && !stack.is(Items.GLASS_BOTTLE)) {
					pockets.setItem(slot, Chests.store(container, stack));
				}
			}
			stillMissing.forEach((item, amount) -> Chests.take(container, pockets, stack -> stack.is(item) && isMaterial.test(stack), amount));
		});
	}

	/** The first ingredient of the order that neither the inventory nor the chests have enough of. */
	private static Component missing(ServerLevel level, Villager villager, Workstations.Station workplace, List<CraftingRecipe> recipes,
		Predicate<ItemStack> isMaterial) {
		Map<Item, Integer> counts = new HashMap<>();
		List<Container> containers = new ArrayList<>();
		containers.add(villager.getInventory());
		for (BlockPos pos : Orders.sources(level, workplace)) {
			Container container = Workstations.getContainer(level, pos);
			if (container != null) {
				containers.add(container);
			}
		}
		for (Container container : containers) {
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);
				if (!stack.isEmpty() && isMaterial.test(stack)) {
					counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
				}
			}
		}
		Map<Ingredient, Integer> needed = new LinkedHashMap<>();
		recipes.getFirst().placementInfo().ingredients().forEach(ingredient -> needed.merge(ingredient, 1, Integer::sum));
		for (Map.Entry<Ingredient, Integer> entry : needed.entrySet()) {
			int have = counts.entrySet().stream().filter(item -> entry.getKey().acceptsItem(item.getKey().builtInRegistryHolder()))
				.mapToInt(Map.Entry::getValue).sum();
			Optional<Holder<Item>> first = entry.getKey().items().findFirst();
			if (have < entry.getValue() && first.isPresent()) {
				return new ItemStack(first.get()).getHoverName();
			}
		}
		return CarpenterRecipes.preview(recipes.getFirst()).getHoverName();
	}

	private int inOutputs(ServerLevel level, Workstations.Station workplace, Item product) {
		int count = 0;
		for (BlockPos pos : workplace.validOutputs(level)) {
			Container container = Workstations.getContainer(level, pos);
			if (container != null) {
				count += container.countItem(product);
			}
		}
		return count;
	}

	// --- Fetching ---

	private void fetchFrom(ServerLevel level, Villager villager, BlockPos chest, Task task, Consumer<Container> action) {
		this.trip = ChestTrip.to(level, villager, chest);
		this.fetchAction = action;
		this.fetchFor = task;
		this.setState(State.FETCH);
	}

	private void fetch(ServerLevel level, Villager villager, long timestamp) {
		if (this.trip == null || this.fetchAction == null) {
			this.setState(State.PLAN);
			return;
		}
		int before = count(villager.getInventory(), stack -> true);
		ChestTrip.Result result = this.trip.tick(level, villager, this.fetchAction);
		if (result == ChestTrip.Result.RUNNING) {
			return;
		}
		this.trip = null;
		if (result == ChestTrip.Result.FAILED || count(villager.getInventory(), stack -> true) <= before && this.fetchFor != null) {
			this.failedAt.put(this.fetchFor, timestamp);
		}
		this.setState(State.PLAN);
	}

	// --- At the grindstone ---

	private void goToGrindstone(Task task) {
		this.task = task;
		this.setState(State.WALK);
	}

	private void walk(ServerLevel level, Villager villager, Workstations.Station workplace) {
		Optional<BlockPos> spot = StandSpots.find(level, workplace.pos(), villager);
		boolean there = spot.map(pos -> StandSpots.isAt(villager, pos)).orElse(workplace.pos().closerToCenterThan(villager.position(), 2.5));
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(workplace.pos()));
		if (there) {
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			this.setState(State.WORK);
		} else if (this.stateTicks > WALK_TIMEOUT) {
			this.idle(villager, workplace, "weaponsmith.grindstone_unreachable");
		} else if (this.stateTicks % 20 == 1) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(spot.orElse(workplace.pos()), SPEED, spot.isPresent() ? 0 : 1));
		}
	}

	/** One piece every two seconds, as long as there is one. */
	private void work(ServerLevel level, Villager villager, Workstations.Station workplace) {
		BlockPos pos = workplace.pos();
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(pos));
		if (this.stateTicks % 5 == 0) {
			level.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 2, 0.2, 0.1, 0.2, 0.05);
		}
		if (this.stateTicks % WorkerLevels.ticks(villager, WORK_TICKS) != 0) {
			return;
		}
		boolean done = switch (this.task) {
			case MERGE -> this.merge(villager);
			case DISENCHANT -> this.disenchant(villager, level.getRandom());
			case FORGE -> this.forge(level, villager, workplace);
		};
		if (!done) {
			if (this.task == Task.DISENCHANT) {
				this.bottle(level, villager);
			}
			this.setState(State.PLAN);
			return;
		}
		villager.swingForAttack(InteractionHand.MAIN_HAND);
		level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.6F, 1.0F);
	}

	/** Grinds two of a kind together, like a grindstone. Returns false if there is no pair. */
	private boolean merge(Villager villager) {
		SimpleContainer inventory = villager.getInventory();
		Item item = pair(inventory, stack -> true);
		if (item == null) {
			return false;
		}
		int left = 0;
		ItemStack first = ItemStack.EMPTY;
		for (int slot = 0, taken = 0; slot < inventory.getContainerSize() && taken < 2; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.is(item) && isMergeable(stack)) {
				first = first.isEmpty() ? stack.copy() : first;
				left += stack.getMaxDamage() - stack.getDamageValue();
				inventory.setItem(slot, ItemStack.EMPTY);
				taken++;
			}
		}
		int max = first.getMaxDamage();
		ItemStack result = new ItemStack(item);
		result.setDamageValue(Math.max(0, max - left - max * MERGE_BONUS_PERCENT / 100));
		CompoundTag mark = new CompoundTag();
		mark.putBoolean(MERGED, true);
		result.set(DataComponents.CUSTOM_DATA, CustomData.of(mark));
		inventory.addItem(result);
		return true;
	}

	/** Takes the enchantments off one thing, like a grindstone, and keeps the experience. Returns false if there is nothing. */
	private boolean disenchant(Villager villager, RandomSource random) {
		SimpleContainer inventory = villager.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!isDisenchantable(stack)) {
				continue;
			}
			HiredVillager hired = (HiredVillager) villager;
			hired.workingvillagers$setExperience(hired.workingvillagers$getExperience() + experienceOf(stack, random));
			inventory.setItem(slot, disenchanted(stack));
			((HiredVillager) villager).workingvillagers$addWorkDone(ChopTrees.today((ServerLevel) villager.level()), 1);
			return true;
		}
		return false;
	}

	/** The experience a grindstone gives for the enchantments of the stack (not for curses). */
	static int experienceOf(ItemStack stack, RandomSource random) {
		int cost = 0;
		for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
			if (!entry.getKey().is(EnchantmentTags.CURSE)) {
				cost += entry.getKey().value().getMinCost(entry.getIntValue());
			}
		}
		int half = (int) Math.ceil(cost / 2.0);
		return half > 0 ? half + random.nextInt(half) : 0;
	}

	/** The stack without its enchantments but the curses, an enchanted book without any a book; like a grindstone. */
	static ItemStack disenchanted(ItemStack stack) {
		ItemStack result = stack.copy();
		ItemEnchantments left = EnchantmentHelper.updateEnchantments(result, enchantments -> enchantments.removeIf(enchantment -> !enchantment.is(EnchantmentTags.CURSE)));
		if (result.is(Items.ENCHANTED_BOOK) && left.isEmpty()) {
			result = result.transmuteCopy(Items.BOOK);
		}
		int repairCost = 0;
		for (int i = 0; i < left.size(); i++) {
			repairCost = AnvilMenu.calculateIncreasedRepairCost(repairCost);
		}
		result.set(DataComponents.REPAIR_COST, repairCost);
		return result;
	}

	/** Fills the experience into bottles o' enchanting, as many as it has glass bottles for. */
	private void bottle(ServerLevel level, Villager villager) {
		HiredVillager hired = (HiredVillager) villager;
		int perBottle = Config.EXPERIENCE_PER_BOTTLE.get();
		SimpleContainer inventory = villager.getInventory();
		int bottles = Math.min(hired.workingvillagers$getExperience() / perBottle, inventory.countItem(Items.GLASS_BOTTLE));
		if (bottles <= 0) {
			return;
		}
		inventory.removeItemType(Items.GLASS_BOTTLE, bottles);
		ItemStack rest = inventory.addItem(new ItemStack(Items.EXPERIENCE_BOTTLE, bottles));
		if (!rest.isEmpty()) {
			villager.spawnAtLocation(level, rest);
		}
		hired.workingvillagers$setExperience(hired.workingvillagers$getExperience() - bottles * perBottle);
		level.playSound(null, villager, SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
	}

	/**
	 * Makes one piece of the product it picked. Returns false if the material is not enough,
	 * the stock is full or the order has as many as ordered.
	 */
	private boolean forge(ServerLevel level, Villager villager, Workstations.Station workplace) {
		Item order = this.current;
		if (order == null || !workplace.orders().contains(order)) {
			return false;
		}
		List<CraftingRecipe> recipes = ForgeRecipes.forResult(level, order);
		Predicate<ItemStack> isMaterial = materialOf(recipesOf(level, workplace.orders()));
		SimpleContainer inventory = villager.getInventory();
		StackedItemContents carried = new StackedItemContents();
		CarpenterRecipes.account(carried, inventory, inventory.getContainerSize(), isMaterial);
		CraftingRecipe recipe = CarpenterRecipes.firstCraftable(carried, recipes);
		boolean enough = workplace.limited(order)
			&& workplace.wanted(order, workplace.stocked(order) ? this.inOutputs(level, workplace, order) + inventory.countItem(order) : 0) <= 0;
		if (recipe == null || enough || !inventory.canAddItem(CarpenterRecipes.preview(recipe))) {
			return false;
		}
		for (ItemStack made : CarpenterRecipes.craft(inventory, inventory.getContainerSize(), isMaterial, recipe)) {
			ItemStack rest = inventory.addItem(made);
			if (!rest.isEmpty()) {
				villager.spawnAtLocation(level, rest);
			}
		}
		((HiredVillager) villager).workingvillagers$addWorkDone(ChopTrees.today(level), 1);
		workplace.addMade(order, 1);
		return true;
	}

	// --- Bringing things away ---

	/** Brings the tools ground together back to where they came from (or a village chest, or an output chest). */
	private void giveBack(ServerLevel level, Villager villager, Workstations.Station workplace) {
		if (this.trip == null) {
			List<BlockPos> candidates = new ArrayList<>();
			if (this.returnTo != null) {
				candidates.add(this.returnTo);
			}
			candidates.addAll(VillageChests.inReach(level, workplace.pos()));
			candidates.addAll(workplace.validOutputs(level));
			Optional<BlockPos> chest = candidates.stream().filter(pos -> Chests.emptySlots(level, pos) > 0).findFirst();
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
				if (isMerged(stack)) {
					ItemStack clean = stack.copy();
					clean.remove(DataComponents.CUSTOM_DATA);
					ItemStack rest = Chests.store(container, clean);
					if (!rest.isEmpty()) {
						rest.set(DataComponents.CUSTOM_DATA, stack.get(DataComponents.CUSTOM_DATA));
					}
					inventory.setItem(slot, rest);
				}
			}
		});
		if (result == ChestTrip.Result.RUNNING) {
			return;
		}
		this.trip = null;
		this.returnTo = null;
		if (result == ChestTrip.Result.FAILED) {
			this.idle(villager, workplace, "chest_unreachable");
		} else {
			this.setState(State.PLAN);
		}
	}

	private void deliver(ServerLevel level, Villager villager, Workstations.Station workplace) {
		Predicate<ItemStack> isMaterial = materialOf(recipesOf(level, workplace.orders()));
		Predicate<ItemStack> isProduct = stack -> isProduct(stack, isMaterial, workplace);
		if (this.trip == null) {
			if (!villager.getInventory().hasAnyMatching(isProduct)) {
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
		ChestTrip.Result result = this.trip.tick(level, villager, container -> Chests.deposit(villager.getInventory(), container, isProduct));
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
			case PLAN, WALK -> WorkerStatus.task(villager, "weaponsmith.plan");
			case FETCH -> WorkerStatus.task(villager, "weaponsmith.fetch");
			case WORK -> WorkerStatus.task(villager, switch (this.task) {
				case MERGE -> "weaponsmith.merge";
				case DISENCHANT -> "weaponsmith.disenchant";
				case FORGE -> "weaponsmith.forge";
			});
			case RETURN -> WorkerStatus.task(villager, "weaponsmith.return");
			case DELIVER -> WorkerStatus.task(villager, "deliver");
			case WAIT -> WorkerStatus.task(villager, this.waitReason, this.waitArgs);
		}
	}

	private void setState(State state) {
		this.state = state;
		this.stateTicks = 0;
	}
}
