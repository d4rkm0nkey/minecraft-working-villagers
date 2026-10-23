package me.teajay.workingvillagers.job;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The work of the butcher: keep the cows, mooshrooms, pigs, chickens and rabbits around its
 * smoker at the herd size set there, for each kind on its own. Below it, it feeds two grown
 * animals the food of their kind so they get a young one; above it, it slaughters grown
 * animals, but never below two of a kind and never named or leashed ones. The meat it smokes
 * in the smoker if it has coal or charcoal; everything else (and the raw meat without coal)
 * goes to one of the output chests. Without an output chest it does not work. Sheep are the
 * shepherd's.
 *
 * <p>It reaches over a fence like the shepherd ({@link TendSheep}). It needs its wage and a
 * sword or an axe, which wears like a player's, one use per animal. Feed and coal it is given,
 * takes from a village chest or fetches from the input chests of the smoker.
 */
public class TendAnimals extends Behavior<Villager> {
	/** The kinds of animals the butcher keeps. */
	public static final Set<EntityType<?>> KINDS = Set.of(EntityTypes.COW, EntityTypes.MOOSHROOM, EntityTypes.PIG, EntityTypes.CHICKEN, EntityTypes.RABBIT);
	private static final float SPEED = 0.5F;
	private static final int PASTURE_DOWN = 4;
	private static final int PASTURE_UP = 4;
	private static final int WALK_TIMEOUT = 300;
	private static final int SEARCH_COOLDOWN = 100;
	/** How long an animal that could not be reached is left alone. */
	private static final long UNREACHABLE_COOLDOWN = 600L;
	/** After fetching from the input chests found nothing, how long it does not try again. */
	private static final long FETCH_COOLDOWN = 1200L;
	private static final double REACH = 3.5;
	private static final double SMOKER_REACH = 2.5;
	/** How long two animals are given to find each other after they were fed. */
	private static final int LOVE_TICKS = 600;
	/** Never fewer grown animals of a kind than this. */
	private static final int MIN_ADULTS = 2;
	private static final int SLOT_INPUT = 0;
	private static final int SLOT_FUEL = 1;
	private static final int SLOT_RESULT = 2;
	private static final int TEND_TICKS = 20;

	private enum State { SEARCH, WALK, WORK, SMOKE, FETCH, DELIVER, WAIT }

	private enum Task { SLAUGHTER, FEED }

	private State state = State.SEARCH;
	private int stateTicks;
	private int waitTicks = SEARCH_COOLDOWN;
	private @Nullable UUID target;
	private Task task = Task.SLAUGHTER;
	private String waitReason = "butcher.waiting";
	private @Nullable ChestTrip trip;
	private long fetchFailedAt = Long.MIN_VALUE / 2;
	/** One animal of each kind that should breed but has no feed in the inventory, from the last search. */
	private List<Animal> hungry = List.of();
	private final Map<UUID, Long> unreachable = new HashMap<>();
	private final Map<Item, Boolean> smokable = new HashMap<>();
	private final WorkerSupplies supplies = new WorkerSupplies();

	public TendAnimals() {
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
		if (!villager.getVillagerData().profession().is(WorkingVillagers.BUTCHER_PROFESSION)) {
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
		this.setState(State.SEARCH);
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
		Workstations.Station workplace = getWorkplace(level, villager);
		if (workplace == null) {
			return;
		}
		this.stateTicks++;
		this.unreachable.values().removeIf(until -> until < timestamp);
		// Wage and weapon before work; with meat in its pockets only when the weapon is worn or gone
		if (this.state == State.SEARCH && (!this.hasDeliverables(level, villager.getInventory(), workplace) || findWeapon(villager.getInventory()).isEmpty())
			&& this.supplies.busy(level, villager, WorkerSupplies.WEAPON)) {
			return; // the chest sets the status
		}

		switch (this.state) {
			case SEARCH -> this.search(level, villager, workplace, timestamp);
			case WALK -> this.walk(level, villager, timestamp);
			case WORK -> this.work(level, villager);
			case SMOKE -> this.smoke(level, villager, workplace);
			case FETCH -> this.fetch(level, villager, workplace, timestamp);
			case DELIVER -> this.deliver(level, villager, workplace);
			case WAIT -> {
				if (this.stateTicks >= this.waitTicks) {
					this.setState(State.SEARCH);
				}
			}
		}
		this.report(villager);
	}

	/** The animals the butcher keeps around the smoker, young ones included. */
	public static List<Animal> herd(ServerLevel level, BlockPos smoker) {
		int radius = Config.BUTCHER_RADIUS.get();
		AABB area = new AABB(smoker).inflate(radius, 0, radius).expandTowards(0, PASTURE_UP, 0).expandTowards(0, -PASTURE_DOWN, 0);
		return level.getEntitiesOfClass(Animal.class, area, animal -> animal.isAlive() && KINDS.contains(animal.getType()));
	}

	/** What the butcher feeds its animals with: what cows, pigs, chickens or rabbits breed with. */
	public static boolean isFeed(ItemStack stack) {
		return stack.is(ItemTags.COW_FOOD) || stack.is(ItemTags.PIG_FOOD) || stack.is(ItemTags.CHICKEN_FOOD) || stack.is(ItemTags.RABBIT_FOOD);
	}

	private void search(ServerLevel level, Villager villager, Workstations.Station workplace, long timestamp) {
		SimpleContainer inventory = villager.getInventory();
		Container smoker = Workstations.getContainer(level, workplace.pos());
		if (this.hasDeliverables(level, inventory, workplace) && !inventory.canAddItem(new ItemStack(Items.BEEF, 64))) {
			this.setState(State.DELIVER);
			return;
		}
		if (smoker != null && this.needsTending(level, inventory, smoker)) {
			this.setState(State.SMOKE);
			return;
		}
		List<Animal> herd = herd(level, workplace.pos());
		Map<EntityType<?>, List<Animal>> kinds = herd.stream().collect(Collectors.groupingBy(Animal::getType));
		Comparator<Animal> closest = Comparator.comparingDouble(animal -> animal.distanceToSqr(villager));
		Optional<Animal> slaughter = Optional.empty();
		Optional<Animal> feed = Optional.empty();
		List<Animal> hungry = new ArrayList<>();
		for (List<Animal> kind : kinds.values()) {
			int size = kind.size();
			long adults = kind.stream().filter(animal -> !animal.isBaby()).count();
			if (size > workplace.herd() && adults > MIN_ADULTS) {
				Optional<Animal> candidate = kind.stream().filter(this::canSlaughter).min(closest);
				if (candidate.isPresent() && (slaughter.isEmpty() || closest.compare(candidate.get(), slaughter.get()) < 0)) {
					slaughter = candidate;
				}
			}
			// Two animals in love make a young one: one in love alone gets a partner, a new pair
			// only while the young ones to come still fit
			long inLove = kind.stream().filter(Animal::isInLove).count();
			long ready = kind.stream().filter(TendAnimals::canBreed).count();
			boolean breed = inLove % 2 == 1 || size + (inLove + 1) / 2 < workplace.herd() && ready >= 2;
			if (!breed) {
				continue;
			}
			Animal any = kind.getFirst();
			if (feedFor(inventory, any).isEmpty()) {
				hungry.add(any);
				continue;
			}
			Optional<Animal> candidate = kind.stream()
				.filter(animal -> canBreed(animal) && !this.unreachable.containsKey(animal.getUUID()))
				.min(closest);
			if (candidate.isPresent() && (feed.isEmpty() || closest.compare(candidate.get(), feed.get()) < 0)) {
				feed = candidate;
			}
		}
		if (slaughter.isPresent()) {
			this.goTo(slaughter.get(), Task.SLAUGHTER);
			return;
		}
		if (feed.isPresent()) {
			this.goTo(feed.get(), Task.FEED);
			return;
		}
		this.hungry = hungry;
		if (timestamp - this.fetchFailedAt > FETCH_COOLDOWN && this.fetchChest(level, villager, workplace).isPresent()) {
			this.setState(State.FETCH);
			return;
		}
		if (this.hasDeliverables(level, inventory, workplace)) {
			this.setState(State.DELIVER);
		} else {
			this.rest(level, villager, workplace, SEARCH_COOLDOWN,
				herd.isEmpty() ? "butcher.no_animals" : !hungry.isEmpty() ? "butcher.no_feed" : "butcher.waiting");
		}
	}

	/** A grown animal that is not in love and has not just had a young one. */
	private static boolean canBreed(Animal animal) {
		return animal.canFallInLove() && !animal.isBaby();
	}

	/** Grown, not named, not on a lead, not in love, and not one the butcher cannot get at. */
	private boolean canSlaughter(Animal animal) {
		return !animal.isBaby() && !animal.hasCustomName() && !animal.isLeashed() && !animal.isInLove() && !this.unreachable.containsKey(animal.getUUID());
	}

	private static ItemStack feedFor(SimpleContainer inventory, Animal animal) {
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && isFeed(stack) && animal.isFood(stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private void goTo(Animal animal, Task task) {
		this.target = animal.getUUID();
		this.task = task;
		this.setState(State.WALK);
	}

	private @Nullable Animal target(ServerLevel level) {
		return this.target != null && level.getEntity(this.target) instanceof Animal animal && animal.isAlive() ? animal : null;
	}

	private boolean stillToDo(Animal animal) {
		return this.task == Task.SLAUGHTER ? this.canSlaughter(animal) : canBreed(animal);
	}

	private void walk(ServerLevel level, Villager villager, long timestamp) {
		Animal animal = this.target(level);
		if (animal == null || !this.stillToDo(animal)) {
			this.target = null;
			this.setState(State.SEARCH);
			return;
		}
		if (isWithinReach(villager, animal)) {
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			ItemStack tool = this.task == Task.SLAUGHTER ? findWeapon(villager.getInventory()) : feedFor(villager.getInventory(), animal);
			if (tool.isEmpty()) {
				this.target = null;
				this.setState(State.SEARCH);
				return;
			}
			villager.setItemSlot(EquipmentSlot.MAINHAND, tool.copyWithCount(1));
			villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
			this.setState(State.WORK);
		} else if (this.stateTicks > WALK_TIMEOUT) {
			this.unreachable.put(animal.getUUID(), timestamp + UNREACHABLE_COOLDOWN);
			this.target = null;
			this.setState(State.SEARCH);
		} else if (this.stateTicks % 20 == 1) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(animal, false), SPEED, 1));
			villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(animal, true));
		}
	}

	/** Close enough sideways and about level with it; over a fence, too. */
	private static boolean isWithinReach(Villager villager, Animal animal) {
		double dx = villager.getX() - animal.getX();
		double dz = villager.getZ() - animal.getZ();
		double dy = villager.getY() - animal.getY();
		return dx * dx + dz * dz < REACH * REACH && dy > -1.5 && dy < 1.5;
	}

	private void work(ServerLevel level, Villager villager) {
		Animal animal = this.target(level);
		if (animal == null || !this.stillToDo(animal) || !isWithinReach(villager, animal)) {
			this.target = null;
			villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			this.setState(State.SEARCH);
			return;
		}
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(animal, true));
		if (this.stateTicks % 10 == 0) {
			villager.swingForAttack(InteractionHand.MAIN_HAND);
		}
		if (this.stateTicks < Math.max(1, Math.round(Config.SECONDS_PER_ANIMAL.get() * 20))) {
			return;
		}
		this.target = null;
		villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		this.setState(State.SEARCH);
		if (this.task == Task.SLAUGHTER) {
			slaughter(level, villager, animal);
		} else {
			feed(level, villager, animal);
		}
	}

	/** Kills the animal and puts what it drops into the inventory; one use of the weapon. */
	private static void slaughter(ServerLevel level, Villager villager, Animal animal) {
		ItemStack weapon = findWeapon(villager.getInventory());
		if (weapon.isEmpty()) {
			return;
		}
		AABB around = animal.getBoundingBox().inflate(2.0);
		Set<ItemEntity> before = new HashSet<>(level.getEntitiesOfClass(ItemEntity.class, around));
		animal.hurtServer(level, level.damageSources().mobAttack(villager), Float.MAX_VALUE);
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, around)) {
			if (before.contains(item) || !item.isAlive()) {
				continue;
			}
			ItemStack rest = villager.getInventory().addItem(item.getItem().copy());
			if (rest.isEmpty()) {
				item.discard();
			} else {
				item.setItem(rest);
			}
		}
		((HiredVillager) villager).workingvillagers$addWorkDone(ChopTrees.today(level), 1);
		weapon.hurtAndBreak(1, level, null, broken -> villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY));
		villager.getInventory().setChanged();
	}

	/** Feeds the animal, so it falls in love, like after a player fed it. */
	private static void feed(ServerLevel level, Villager villager, Animal animal) {
		ItemStack food = feedFor(villager.getInventory(), animal);
		if (food.isEmpty()) {
			return;
		}
		food.shrink(1);
		villager.getInventory().setChanged();
		animal.setInLove(null);
		animal.setInLoveTime(LOVE_TICKS);
		level.playSound(null, animal, SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 1.0F, 1.0F);
	}

	/** The sword or axe the butcher works with; worn ones it gives back instead (see {@link WorkerSupplies#isWorn}). */
	private static ItemStack findWeapon(SimpleContainer inventory) {
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && WorkerSupplies.WEAPON.item().test(stack) && !WorkerSupplies.isWorn(stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	// --- The smoker ---

	private boolean isSmokable(ServerLevel level, ItemStack stack) {
		return !stack.isEmpty() && this.smokable.computeIfAbsent(stack.getItem(),
			item -> level.recipeAccess().getRecipeFor(RecipeType.SMOKING, new SingleRecipeInput(stack), level).isPresent());
	}

	private static boolean isFuel(ItemStack stack) {
		return stack.is(ItemTags.COALS);
	}

	private static boolean fits(ItemStack stack, ItemStack slot) {
		return slot.isEmpty() || ItemStack.isSameItemSameComponents(stack, slot) && slot.getCount() < slot.getMaxStackSize();
	}

	/** Whether the smoker burns or has fuel to burn, or the butcher carries some. */
	private static boolean hasFuel(ServerLevel level, SimpleContainer inventory, BlockPos pos, @Nullable Container smoker) {
		if (smoker != null && isFuel(smoker.getItem(SLOT_FUEL))) {
			return true;
		}
		if (level.getBlockState(pos).getValueOrElse(BlockStateProperties.LIT, false)) {
			return true;
		}
		for (ItemStack stack : inventory) {
			if (isFuel(stack)) {
				return true;
			}
		}
		return false;
	}

	/** Something smoked to take out, or meat and coal to put in. */
	private boolean needsTending(ServerLevel level, SimpleContainer inventory, Container smoker) {
		if (smoker.getContainerSize() <= SLOT_RESULT) {
			return false;
		}
		if (!smoker.getItem(SLOT_RESULT).isEmpty()) {
			return true;
		}
		ItemStack input = smoker.getItem(SLOT_INPUT);
		ItemStack fuel = smoker.getItem(SLOT_FUEL);
		boolean meat = false;
		boolean coal = false;
		for (ItemStack stack : inventory) {
			meat |= this.isSmokable(level, stack) && fits(stack, input);
			coal |= isFuel(stack) && fits(stack, fuel);
		}
		// Meat to put in while there is fire for it; coal for meat that waits in the smoker
		return meat && (coal || isFuel(fuel)) || coal && fuel.isEmpty() && !input.isEmpty();
	}

	private void smoke(ServerLevel level, Villager villager, Workstations.Station workplace) {
		Container smoker = Workstations.getContainer(level, workplace.pos());
		if (smoker == null || smoker.getContainerSize() <= SLOT_RESULT) {
			this.setState(State.SEARCH);
			return;
		}
		Optional<BlockPos> spot = StandSpots.find(level, workplace.pos(), villager);
		boolean there = spot.map(pos -> StandSpots.isAt(villager, pos)).orElse(workplace.pos().closerToCenterThan(villager.position(), SMOKER_REACH));
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(workplace.pos()));
		if (!there) {
			if (this.stateTicks > WALK_TIMEOUT) {
				this.rest(level, villager, workplace, SEARCH_COOLDOWN, "butcher.smoker_unreachable");
			} else if (this.stateTicks % 20 == 1) {
				villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(spot.orElse(workplace.pos()), SPEED, spot.isPresent() ? 0 : 1));
			}
			return;
		}
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		if (this.stateTicks % TEND_TICKS != 0) {
			return;
		}
		SimpleContainer inventory = villager.getInventory();
		ItemStack result = smoker.getItem(SLOT_RESULT);
		if (!result.isEmpty()) {
			smoker.setItem(SLOT_RESULT, inventory.addItem(result.copy()));
		}
		putIn(inventory, smoker, SLOT_FUEL, TendAnimals::isFuel);
		putIn(inventory, smoker, SLOT_INPUT, stack -> this.isSmokable(level, stack));
		smoker.setChanged();
		villager.swingForAttack(InteractionHand.MAIN_HAND);
		level.playSound(null, workplace.pos(), SoundEvents.SMOKER_SMOKE, SoundSource.BLOCKS, 0.6F, 1.0F);
		this.setState(State.SEARCH);
	}

	private static void putIn(SimpleContainer inventory, Container smoker, int target, Predicate<ItemStack> filter) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			ItemStack inSmoker = smoker.getItem(target);
			if (stack.isEmpty() || !filter.test(stack) || !fits(stack, inSmoker)) {
				continue;
			}
			int room = (inSmoker.isEmpty() ? stack.getMaxStackSize() : inSmoker.getMaxStackSize()) - inSmoker.getCount();
			int moved = Math.min(stack.getCount(), room);
			smoker.setItem(target, stack.copyWithCount(inSmoker.getCount() + moved));
			stack.shrink(moved);
			inventory.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
		}
	}

	// --- Input chests: feed and coal ---

	/**
	 * The closest input chest with feed for a kind of animal that lacks it, or coal (if there
	 * is meat to smoke but no fire).
	 */
	private Optional<BlockPos> fetchChest(ServerLevel level, Villager villager, Workstations.Station workplace) {
		SimpleContainer inventory = villager.getInventory();
		boolean wantFeed = !this.hungry.isEmpty();
		boolean wantFuel = !hasFuel(level, inventory, workplace.pos(), Workstations.getContainer(level, workplace.pos()))
			&& WorkerSupplies.count(inventory, stack -> this.isSmokable(level, stack)) > 0;
		if (!wantFeed && !wantFuel) {
			return Optional.empty();
		}
		List<BlockPos> chests = workplace.validInputs(level).stream()
			.filter(pos -> wantFeed && Chests.contains(level, pos, stack -> this.isWantedFeed(inventory, stack))
				|| wantFuel && Chests.contains(level, pos, TendAnimals::isFuel))
			.toList();
		return Chests.closest(chests, villager);
	}

	/**
	 * Feed that one of the hungry kinds eats and of which the butcher carries less than it
	 * keeps: it keeps {@link Config#KEEP_FEED} of each kind of feed, like what it is given.
	 */
	private boolean isWantedFeed(SimpleContainer inventory, ItemStack stack) {
		return isFeed(stack) && this.hungry.stream().anyMatch(animal -> animal.isFood(stack))
			&& WorkerSupplies.count(inventory, other -> other.is(stack.getItem())) < Config.KEEP_FEED.get();
	}

	/** Takes the feed of the hungry kinds out of the container, up to what it keeps of each. Returns how many. */
	private int takeFeed(Container container, SimpleContainer inventory) {
		int taken = 0;
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot);
			if (stack.isEmpty() || !this.isWantedFeed(inventory, stack)) {
				continue;
			}
			int max = Config.KEEP_FEED.get() - WorkerSupplies.count(inventory, other -> other.is(stack.getItem()));
			taken += Chests.take(container, inventory, other -> other.is(stack.getItem()), max);
		}
		return taken;
	}

	private void fetch(ServerLevel level, Villager villager, Workstations.Station workplace, long timestamp) {
		if (this.trip == null) {
			Optional<BlockPos> chest = this.fetchChest(level, villager, workplace);
			if (chest.isEmpty()) {
				this.fetchFailedAt = timestamp;
				this.setState(State.SEARCH);
				return;
			}
			this.trip = ChestTrip.to(level, villager, chest.get());
		}
		SimpleContainer inventory = villager.getInventory();
		int[] taken = {0};
		ChestTrip.Result result = this.trip.tick(level, villager, container -> {
			taken[0] += this.takeFeed(container, inventory);
			taken[0] += Chests.take(container, inventory, TendAnimals::isFuel, Config.KEEP_FUEL.get() - WorkerSupplies.count(inventory, TendAnimals::isFuel));
		});
		if (result == ChestTrip.Result.RUNNING) {
			return;
		}
		this.trip = null;
		if (result == ChestTrip.Result.FAILED || taken[0] == 0) {
			this.fetchFailedAt = timestamp;
		}
		this.setState(State.SEARCH);
	}

	// --- Delivering ---

	/**
	 * What the butcher keeps: its weapon, feed, coal and wage, and raw meat while it can smoke
	 * it (it has fire for it and the smoker has room).
	 */
	private boolean isKept(ServerLevel level, ItemStack stack, boolean smoking, ItemStack input) {
		if (WorkerSupplies.WEAPON.item().test(stack) || isFeed(stack) || isFuel(stack) || WorkerSupplies.isWage(stack)) {
			return true;
		}
		return smoking && this.isSmokable(level, stack) && (input.isEmpty() || ItemStack.isSameItemSameComponents(stack, input));
	}

	private boolean hasDeliverables(ServerLevel level, SimpleContainer inventory, Workstations.Station workplace) {
		Container smoker = Workstations.getContainer(level, workplace.pos());
		boolean smoking = smoker != null && hasFuel(level, inventory, workplace.pos(), smoker);
		ItemStack input = smoker != null && smoker.getContainerSize() > SLOT_INPUT ? smoker.getItem(SLOT_INPUT) : ItemStack.EMPTY;
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && !this.isKept(level, stack, smoking, input)) {
				return true;
			}
		}
		return false;
	}

	private void deliver(ServerLevel level, Villager villager, Workstations.Station workplace) {
		if (this.trip == null) {
			villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			if (!this.hasDeliverables(level, villager.getInventory(), workplace)) {
				this.setState(State.SEARCH);
				return;
			}
			Optional<BlockPos> chest = Chests.closest(workplace.validOutputs(level).stream().filter(pos -> Chests.hasRoom(level, pos)).toList(), villager);
			if (chest.isEmpty()) {
				this.rest(level, villager, workplace, SEARCH_COOLDOWN, "chests_full");
				return;
			}
			this.trip = ChestTrip.to(level, villager, chest.get());
		}
		Container smoker = Workstations.getContainer(level, workplace.pos());
		boolean smoking = smoker != null && hasFuel(level, villager.getInventory(), workplace.pos(), smoker);
		ItemStack input = smoker != null && smoker.getContainerSize() > SLOT_INPUT ? smoker.getItem(SLOT_INPUT).copy() : ItemStack.EMPTY;
		ChestTrip.Result result = this.trip.tick(level, villager, container -> {
			SimpleContainer inventory = villager.getInventory();
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				ItemStack stack = inventory.getItem(slot);
				if (!stack.isEmpty() && !this.isKept(level, stack, smoking, input)) {
					inventory.setItem(slot, HopperBlockEntity.addItem(null, container, stack, null));
				}
			}
		});
		if (result == ChestTrip.Result.RUNNING) {
			return;
		}
		this.trip = null;
		if (result == ChestTrip.Result.FAILED) {
			this.rest(level, villager, workplace, SEARCH_COOLDOWN, "chest_unreachable");
		} else {
			this.setState(this.hasDeliverables(level, villager.getInventory(), workplace) ? State.DELIVER : State.SEARCH);
		}
	}

	/** Goes back to the smoker and waits next to it. */
	private void rest(ServerLevel level, Villager villager, Workstations.Station workplace, int ticks, String reason) {
		villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		Optional<BlockPos> spot = StandSpots.find(level, workplace.pos(), villager);
		if (spot.isPresent()) {
			if (!StandSpots.isAt(villager, spot.get())) {
				villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(spot.get(), SPEED, 0));
			}
		} else {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(workplace.pos(), SPEED, 2));
		}
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(workplace.pos()));
		this.waitReason = reason;
		this.waitTicks = ticks;
		this.setState(State.WAIT);
	}

	private void report(Villager villager) {
		switch (this.state) {
			case SEARCH -> WorkerStatus.task(villager, "butcher.search");
			case WALK, WORK -> WorkerStatus.task(villager, this.task == Task.SLAUGHTER ? "butcher.slaughter" : "butcher.feed");
			case SMOKE -> WorkerStatus.task(villager, "butcher.smoke");
			case FETCH -> WorkerStatus.task(villager, "butcher.fetch");
			case DELIVER -> WorkerStatus.task(villager, "deliver");
			case WAIT -> WorkerStatus.task(villager, this.waitReason);
		}
	}

	private void setState(State state) {
		this.state = state;
		this.stateTicks = 0;
	}
}
