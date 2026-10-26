package me.teajay.workingvillagers.job;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Workers eat. Over a work day a worker gets hungry by {@link Config#DAILY_FOOD} food points
 * and eats from its lunch bag ({@link MealData}) once it misses half of that; a hungry worker
 * (missing all of it) works {@link Config#HUNGRY_SLOWDOWN} percent slower.
 *
 * <p>Of each wage it takes, the worker spends {@link Config#UPKEEP} percent on its upkeep:
 * those emeralds are gone. The rest goes into its purse, and with it the worker buys food at
 * a village chest once it carries less than a day's food: the emeralds go into the chest, so
 * they pay wages again. A day's food money buys a day's food. A worker whose purse holds the
 * food money of {@link #PURSE_DAYS} days (it found no food to buy) leaves the rest of its
 * wage where it was. The player can also give a worker food, or throw it to it.
 *
 * <p>Variety: food of a kind (field crops, meat, fish, fruit; see the {@code meals/...} item
 * tags) the worker had in one of its last {@link #REMEMBERED} meals counts a quarter less
 * per such meal. It buys a mix of what the chest has, and eats what counts most.
 */
public final class Meals {
	/** What workers eat. */
	public static final TagKey<Item> MEALS = TagKey.create(Registries.ITEM, WorkingVillagers.id("meals"));
	/** The kinds of food, for the variety. Food in none of them is a kind of its own. */
	private static final List<TagKey<Item>> KINDS = List.of(
		TagKey.create(Registries.ITEM, WorkingVillagers.id("meals/field")),
		TagKey.create(Registries.ITEM, WorkingVillagers.id("meals/meat")),
		TagKey.create(Registries.ITEM, WorkingVillagers.id("meals/fish")),
		TagKey.create(Registries.ITEM, WorkingVillagers.id("meals/fruit"))
	);
	/** How many of its last meals a worker remembers for the variety. */
	static final int REMEMBERED = 2;
	/** What a remembered meal of the same kind takes off the food points of food. */
	private static final float REPEAT_PENALTY = 0.25F;
	/** The work hours of vanilla villagers (2000 to 9000), over which a worker gets hungry by its daily food. */
	private static final float WORK_TICKS = 7000.0F;
	/** At most the food money of this many days stays in the purse. */
	static final int PURSE_DAYS = 3;
	/** A worker buys food for this many days at once. */
	private static final int STOCK_DAYS = 2;

	private Meals() {
	}

	public static boolean enabled() {
		return Config.MEALS.get();
	}

	public static MealData of(Villager villager) {
		return ((HiredVillager) villager).workingvillagers$getMeals();
	}

	public static int dailyFood() {
		return Config.DAILY_FOOD.get();
	}

	/** Food a worker eats: in the {@code meals} tag and food. */
	public static boolean isMeal(ItemStack stack) {
		return !stack.isEmpty() && stack.is(MEALS) && stack.has(DataComponents.FOOD);
	}

	/** Food that goes into the lunch bag of the worker, not into its pockets (where the farmer keeps carrots to plant). */
	static boolean isBagged(Villager villager, ItemStack stack) {
		return enabled() && isMeal(stack) && WorkerSupplies.toolsOf(villager).stream().noneMatch(tool -> tool.item().test(stack))
			&& WorkerSupplies.stocksOf(villager).stream().noneMatch(stock -> stock.item().test(stack));
	}

	/** The kind of the food, for the variety: its {@code meals/...} tag or else the item. */
	static String kind(ItemStack stack) {
		for (TagKey<Item> kind : KINDS) {
			if (stack.is(kind)) {
				return kind.location().getPath();
			}
		}
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	/** The food points of one of the food, as on the hunger bar. */
	static int nutrition(ItemStack stack) {
		FoodProperties food = stack.get(DataComponents.FOOD);
		return food == null ? 0 : food.nutrition();
	}

	/** The food points one of the food gives the worker now: less for a kind it just had. */
	static float value(MealData data, ItemStack stack) {
		String kind = kind(stack);
		long repeats = data.recent().stream().filter(kind::equals).count();
		return nutrition(stack) * Math.max(0.0F, 1.0F - REPEAT_PENALTY * repeats);
	}

	/** Whether the worker misses all of a day's food. */
	public static boolean isHungry(Villager villager) {
		return enabled() && ((HiredVillager) villager).workingvillagers$isHired() && of(villager).hunger() >= dailyFood();
	}

	/** How much slower (percent) the worker works now: only while it is hungry. */
	static int slowdown(Villager villager) {
		return isHungry(villager) ? Config.HUNGRY_SLOWDOWN.get() : 0;
	}

	/** The work behavior, ticked less often while the worker is hungry. */
	public static BehaviorControl<Villager> slowedWhenHungry(BehaviorControl<? super Villager> work) {
		return new HungryWork(work);
	}

	// --- Wage and purse ---

	/**
	 * The worker took its wage: spends the upkeep and puts the rest into its purse. Returns the
	 * emeralds the purse has no room for, which go back where the wage came from.
	 */
	static int paid(Villager villager, int wage) {
		if (!enabled() || wage <= 0) {
			return 0;
		}
		MealData data = of(villager);
		int owed = data.upkeepOwed() + wage * Config.UPKEEP.get();
		int spent = owed / 100;
		data.setUpkeepOwed(owed % 100);
		int food = wage - spent;
		int kept = Math.min(food, Math.max(0, purseLimit(wage) - data.purse()));
		data.setPurse(data.purse() + kept);
		return food - kept;
	}

	/** The food money of {@link #PURSE_DAYS} days, at least one emerald. */
	private static int purseLimit(int wage) {
		return Math.max(1, (int) Math.ceil(PURSE_DAYS * wage * (100 - Config.UPKEEP.get()) / 100.0));
	}

	/** Food points an emerald of the purse buys: a day's food money buys a day's food. 0 when there is no food money (food is free). */
	static double perEmerald() {
		double money = Config.DAILY_WAGE.get() * (100 - Config.UPKEEP.get()) / 100.0;
		return money <= 0.0 ? 0.0 : dailyFood() / money;
	}

	/** The emeralds food with these food points costs. */
	static int price(int points) {
		double per = perEmerald();
		return per <= 0.0 ? 0 : (int) Math.ceil(points / per - 1.0E-6);
	}

	// --- Eating ---

	/** Called every {@code interval} ticks for a worker: it gets hungry at work and eats from its lunch bag. */
	static void tick(ServerLevel level, Villager villager, int interval) {
		if (!enabled()) {
			return;
		}
		MealData data = of(villager);
		int daily = dailyFood();
		if (villager.getBrain().isActive(Activity.WORK)) {
			data.setHunger(Math.min(daily, data.hunger() + daily * interval / WORK_TICKS));
		}
		if (data.hunger() >= daily / 2.0F && !villager.isSleeping()) {
			eat(level, villager, data);
		}
	}

	/** Eats one of the food in the lunch bag that counts most now. */
	private static void eat(ServerLevel level, Villager villager, MealData data) {
		SimpleContainer bag = data.bag();
		int best = -1;
		float bestValue = 0.0F;
		for (int slot = 0; slot < bag.getContainerSize(); slot++) {
			ItemStack stack = bag.getItem(slot);
			float value = isMeal(stack) ? value(data, stack) : 0.0F;
			if (value > bestValue) {
				best = slot;
				bestValue = value;
			}
		}
		if (best < 0) {
			return;
		}
		ItemStack meal = bag.removeItem(best, 1);
		data.setHunger(data.hunger() - bestValue);
		data.recent().add(kind(meal));
		while (data.recent().size() > REMEMBERED) {
			data.recent().removeFirst();
		}
		level.playSound(null, villager.getX(), villager.getY(), villager.getZ(), SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 0.6F, 1.0F);
		level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, meal.getItem()), villager.getX(), villager.getEyeY() - 0.2, villager.getZ(), 6, 0.15, 0.1, 0.15, 0.05);
	}

	// --- Buying ---

	/** The food points of the food in the lunch bag. */
	static int carried(MealData data) {
		int points = 0;
		for (ItemStack stack : data.bag()) {
			points += isMeal(stack) ? nutrition(stack) * stack.getCount() : 0;
		}
		return points;
	}

	/** Whether the worker buys food: less than a day's food in its lunch bag, room there and money in its purse (or food is free). */
	public static boolean wantsFood(Villager villager) {
		if (!enabled() || !((HiredVillager) villager).workingvillagers$isHired() || villager.isBaby()) {
			return false;
		}
		MealData data = of(villager);
		return carried(data) < dailyFood() && (perEmerald() <= 0.0 || data.purse() > 0) && hasRoom(data.bag());
	}

	private static boolean hasRoom(SimpleContainer bag) {
		for (ItemStack stack : bag) {
			if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The food the worker would buy of the container now, slot by slot: food for
	 * {@link #STOCK_DAYS} days as far as its purse pays, one at a time of the kind it has had
	 * and carries least of.
	 */
	static List<VillageChests.Take> plan(Villager villager, Container container) {
		if (!wantsFood(villager)) {
			return List.of();
		}
		MealData data = of(villager);
		int wanted = STOCK_DAYS * dailyFood() - carried(data);
		double per = perEmerald();
		int budget = per <= 0.0 ? wanted : (int) Math.floor(Math.min(data.purse(), Math.ceil(wanted / per)) * per + 1.0E-6);
		SimpleContainer bag = new SimpleContainer(MealData.BAG_SLOTS);
		Map<String, Integer> kinds = new HashMap<>();
		data.recent().forEach(kind -> kinds.merge(kind, 1, Integer::sum));
		for (ItemStack stack : data.bag()) {
			if (!stack.isEmpty()) {
				bag.addItem(stack.copy());
				kinds.merge(kind(stack), stack.getCount(), Integer::sum);
			}
		}
		int[] taken = new int[container.getContainerSize()];
		while (true) {
			int pick = -1;
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);
				if (!isBagged(villager, stack) || taken[slot] >= stack.getCount() || nutrition(stack) > budget
					|| !bag.canAddItem(stack.copyWithCount(1))) {
					continue;
				}
				if (pick < 0 || kinds.getOrDefault(kind(stack), 0) < kinds.getOrDefault(kind(container.getItem(pick)), 0)) {
					pick = slot;
				}
			}
			if (pick < 0) {
				break;
			}
			ItemStack picked = container.getItem(pick);
			taken[pick]++;
			budget -= nutrition(picked);
			bag.addItem(picked.copyWithCount(1));
			kinds.merge(kind(picked), 1, Integer::sum);
		}
		List<VillageChests.Take> takes = new ArrayList<>();
		for (int slot = 0; slot < taken.length; slot++) {
			if (taken[slot] > 0) {
				takes.add(new VillageChests.Take(slot, taken[slot]));
			}
		}
		return takes;
	}

	/**
	 * Buys food out of the container (see {@link #plan}) into the lunch bag and pays for it
	 * from the purse into the container. Returns how many it bought.
	 */
	static int buy(Villager villager, Container container) {
		MealData data = of(villager);
		int bought = 0;
		int points = 0;
		for (VillageChests.Take take : plan(villager, container)) {
			ItemStack moved = container.getItem(take.slot()).split(take.count());
			points += nutrition(moved) * moved.getCount();
			bought += moved.getCount();
			ItemStack rest = data.bag().addItem(moved);
			container.getItem(take.slot()).grow(rest.getCount());
		}
		int price = Math.min(data.purse(), price(points));
		if (price > 0) {
			ItemStack rest = Chests.store(container, new ItemStack(WorkerSupplies.WAGE, price));
			data.setPurse(data.purse() - price + rest.getCount());
		}
		if (bought > 0) {
			container.setChanged();
		}
		return bought;
	}

	/** How many of the stack fit into the worker's lunch bag. */
	static int room(Villager villager, ItemStack stack) {
		int room = 0;
		for (ItemStack other : of(villager).bag()) {
			if (other.isEmpty()) {
				room += stack.getMaxStackSize();
			} else if (ItemStack.isSameItemSameComponents(other, stack)) {
				room += other.getMaxStackSize() - other.getCount();
			}
		}
		return room;
	}

	/** Drops the food and the purse of a worker that died. */
	public static void drop(ServerLevel level, Villager villager) {
		MealData data = of(villager);
		for (ItemStack stack : data.bag().removeAllItems()) {
			villager.spawnAtLocation(level, stack);
		}
		if (data.purse() > 0) {
			villager.spawnAtLocation(level, new ItemStack(WorkerSupplies.WAGE, data.purse()));
			data.setPurse(0);
		}
	}

	/** A work behavior that a hungry worker does slower: it skips some of its ticks. */
	private static final class HungryWork implements BehaviorControl<Villager> {
		private final BehaviorControl<? super Villager> work;
		/** Percent of a tick earned towards the next one, while hungry. */
		private int credit;

		HungryWork(BehaviorControl<? super Villager> work) {
			this.work = work;
		}

		@Override
		public Behavior.Status getStatus() {
			return this.work.getStatus();
		}

		@Override
		public Set<MemoryModuleType<?>> getRequiredMemories() {
			return this.work.getRequiredMemories();
		}

		@Override
		public boolean tryStart(ServerLevel level, Villager villager, long timestamp) {
			return this.work.tryStart(level, villager, timestamp);
		}

		@Override
		public void tickOrStop(ServerLevel level, Villager villager, long timestamp) {
			int slowdown = slowdown(villager);
			if (slowdown > 0) {
				this.credit += 100 - slowdown;
				if (this.credit < 100) {
					return;
				}
				this.credit -= 100;
			}
			this.work.tickOrStop(level, villager, timestamp);
		}

		@Override
		public void doStop(ServerLevel level, Villager villager, long timestamp) {
			this.work.doStop(level, villager, timestamp);
		}

		@Override
		public String debugString() {
			return this.work.debugString();
		}
	}
}
