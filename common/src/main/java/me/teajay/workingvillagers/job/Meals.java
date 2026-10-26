package me.teajay.workingvillagers.job;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Workers eat. Over a work day a worker gets hungry by {@link Config#DAILY_FOOD} food points
 * and eats from its lunch bag ({@link MealData}) once it misses half of that.
 *
 * <p>Of each wage it takes, the worker spends {@link Config#UPKEEP} percent on its upkeep:
 * those emeralds are gone. The rest goes into its purse, and with it the worker buys food at
 * a village chest once it carries less than a day's food: the emeralds go into the chest, so
 * they pay wages again. There, a day's food money buys {@link Config#FOOD_BUDGET} percent of
 * a day's food. A worker that misses all of a day's food and has none eats out: a day's food
 * money buys a day's food there, and those emeralds are gone. So food in the village chests
 * saves the village the food money of its workers. A worker whose purse holds the food money
 * of {@link #PURSE_DAYS} days leaves the rest of its wage where it was. The player can also
 * give a worker food, or throw it to it.
 *
 * <p>Variety: food of a kind (field crops, meat, fish, fruit; see the {@code meals/...} item
 * tags) the worker had in one of its last {@link #REMEMBERED} meals counts a quarter less
 * per such meal; a meal eaten out is a kind of its own. It buys a mix of what the chest has,
 * but only food that counts at least as much for its emeralds as eating out, and eats what
 * counts most.
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
	/** The kind of a meal eaten out, for the variety. */
	private static final String EATEN_OUT = "out";
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
	public static int nutrition(ItemStack stack) {
		FoodProperties food = stack.get(DataComponents.FOOD);
		return food == null ? 0 : food.nutrition();
	}

	/** The food points one of the food gives the worker now: less for a kind it just had. */
	static float value(MealData data, ItemStack stack) {
		String kind = kind(stack);
		long repeats = data.recent().stream().filter(kind::equals).count();
		return nutrition(stack) * Math.max(0.0F, 1.0F - REPEAT_PENALTY * repeats);
	}

	/** Whether the worker misses all of a day's food (and has no money left to eat out). */
	public static boolean isHungry(Villager villager) {
		return enabled() && ((HiredVillager) villager).workingvillagers$isHired() && of(villager).hunger() >= dailyFood();
	}

	/** Whether the worker ate out last: it found no food it buys at its village chests, so its food money leaves the village. */
	public static boolean eatsOut(Villager villager) {
		return enabled() && ((HiredVillager) villager).workingvillagers$isHired() && of(villager).ateOut();
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

	/** The emeralds of a novice's daily wage that are food money: food costs the same for every worker. */
	public static double dailyFoodMoney() {
		return enabled() ? Config.DAILY_WAGE.get() * (100 - Config.UPKEEP.get()) / 100.0 : 0.0;
	}

	/**
	 * The emeralds the worker costs the village a day: its upkeep, and if it eats out, the food
	 * money of a novice that it spends there (the rest of its wage stays in the wage box once
	 * its purse is full). The food money of a worker fed at village chests comes back.
	 */
	public static double dailyCost(Villager villager, boolean fed) {
		int wage = WorkerLevels.dailyWage(villager);
		if (!enabled()) {
			return wage;
		}
		double upkeep = wage * Config.UPKEEP.get() / 100.0;
		return fed ? upkeep : Math.min(wage, upkeep + dailyFoodMoney());
	}

	/** Food points an emerald of the purse buys eating out: a day's food money buys a day's food. 0 when there is no food money (food is free). */
	static double perEmeraldOut() {
		double money = dailyFoodMoney();
		return money <= 0.0 ? 0.0 : dailyFood() / money;
	}

	/** Food points an emerald of the purse buys at a village chest: {@link Config#FOOD_BUDGET} percent of eating out. */
	static double perEmerald() {
		return perEmeraldOut() * Config.FOOD_BUDGET.get() / 100.0;
	}

	/** The emeralds food with these food points costs. */
	static int price(int points) {
		double per = perEmerald();
		return per <= 0.0 ? 0 : (int) Math.ceil(points / per - 1.0E-6);
	}

	// --- Eating ---

	/** Called every {@code interval} ticks for a worker: it gets hungry at work and eats from its lunch bag, or out. */
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
		if (data.hunger() >= daily && data.purse() > 0 && carried(data) == 0 && !villager.isSleeping()) {
			eatOut(level, villager, data);
		}
	}

	/** Spends an emerald of the purse on a meal out: the emerald is gone. */
	private static void eatOut(ServerLevel level, Villager villager, MealData data) {
		double per = perEmeraldOut();
		if (per <= 0.0) {
			return;
		}
		data.setPurse(data.purse() - 1);
		data.setHunger(data.hunger() - (float) per);
		remember(data, EATEN_OUT);
		data.setAteOut(true);
		level.playSound(null, villager.getX(), villager.getY(), villager.getZ(), SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 0.6F, 1.0F);
		level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, WorkerSupplies.WAGE), villager.getX(), villager.getEyeY() - 0.2, villager.getZ(), 6, 0.15, 0.1, 0.15, 0.05);
	}

	private static void remember(MealData data, String kind) {
		data.recent().add(kind);
		while (data.recent().size() > REMEMBERED) {
			data.recent().removeFirst();
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
		data.setHunger(Math.max(0.0F, data.hunger() - bestValue));
		remember(data, kind(meal));
		data.setAteOut(false);
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

	/** How many pieces of food are in the lunch bag. */
	public static int count(MealData data) {
		int count = 0;
		for (ItemStack stack : data.bag()) {
			count += isMeal(stack) ? stack.getCount() : 0;
		}
		return count;
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
	 * and carries least of. Not food it had so often lately that it counts less for its
	 * emeralds than a meal out: it rather eats out once more.
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
					|| !worthBuying(data, stack) || !bag.canAddItem(stack.copyWithCount(1))) {
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

	/** Whether the food, eaten next, counts at least as much for its price as a meal out. */
	private static boolean worthBuying(MealData data, ItemStack stack) {
		double per = perEmerald();
		return per <= 0.0 || value(data, stack) * per >= nutrition(stack) * perEmeraldOut() - 1.0E-6;
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
}
