package me.teajay.workingvillagers.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.job.MealData;
import me.teajay.workingvillagers.job.Meals;
import me.teajay.workingvillagers.job.VillageChests;
import me.teajay.workingvillagers.job.WorkerLevels;
import me.teajay.workingvillagers.job.WorkerSupplies;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What the menus show about what a worker carries for its work (see {@link WorkerSupplies}):
 * its emeralds, whether today's wage was taken and the tools of its job and what it uses up
 * (saplings, seeds, ladders, ...).
 *
 * @param emeralds the emeralds the worker has for its wage: in the wage box of its workstation and its pockets
 * @param wage the emeralds a worker takes per work day ({@link Config#DAILY_WAGE} of the server)
 * @param tools the tools the worker needs, each with the one it carries (empty if none)
 * @param stocks what the worker uses up, each as one stack with the total count (empty if
 *     it carries none)
 * @param villageChests how many village chests the worker has in reach (see {@link VillageChests})
 * @param worn how many worn tools the worker carries to give back (see {@link WorkerSupplies#isWorn})
 * @param lunch what the worker has for its meals; empty if workers do not eat ({@link Config#MEALS})
 */
public record SuppliesInfo(int emeralds, int wage, boolean paid, List<Slot> tools, List<Slot> stocks, int villageChests, int worn,
	Optional<Lunch> lunch) {
	private static final String TAG = "Supplies";
	private static final String TAG_EMERALDS = "Emeralds";
	private static final String TAG_WAGE = "Wage";
	private static final String TAG_PAID = "Paid";
	private static final String TAG_TOOLS = "Tools";
	private static final String TAG_STOCKS = "Stocks";
	private static final String TAG_KIND = "Kind";
	private static final String TAG_ITEM = "Item";
	private static final String TAG_COUNT = "Count";
	private static final String TAG_DAMAGE = "Damage";
	private static final String TAG_REQUIRED = "Required";
	private static final String TAG_VILLAGE_CHESTS = "VillageChests";
	private static final String TAG_WORN = "Worn";
	private static final String TAG_LUNCH = "Lunch";
	private static final String TAG_PURSE = "Purse";
	private static final String TAG_FOOD = "Food";
	private static final String TAG_DAILY_FOOD = "DailyFood";
	private static final String TAG_UPKEEP = "Upkeep";
	private static final String TAG_EATS_OUT = "EatsOut";
	private static final String TAG_HUNGRY = "Hungry";

	/**
	 * What the worker has for its meals (see {@link Meals}): the emeralds in its purse, the
	 * food points in its lunch bag (with {@code meal}, the food it has most of, standing for
	 * them), the food points it eats a day, the percent of its wage that is upkeep and whether
	 * it eats out or is hungry.
	 */
	public record Lunch(int purse, int food, ItemStack meal, int dailyFood, int upkeep, boolean eatsOut, boolean hungry) {
	}

	/**
	 * A tool or something the worker uses up: its kind ({@code "axe"}, {@code "ladders"},
	 * ...) and what the worker carries of it. {@code required}: the worker cannot work
	 * without it.
	 */
	public record Slot(String kind, ItemStack stack, boolean required) {
		public boolean missing() {
			return this.required && this.stack.isEmpty();
		}
	}

	/** Whether the worker cannot work because of its supplies. */
	public boolean hasProblem() {
		return !this.paid && this.emeralds < this.wage || this.firstMissing().isPresent();
	}

	/** The first tool or required stock the worker has none of. */
	public Optional<Slot> firstMissing() {
		return this.tools.stream().filter(Slot::missing).findFirst().or(() -> this.stocks.stream().filter(Slot::missing).findFirst());
	}

	static void write(ServerLevel level, Villager worker, CompoundTag info) {
		CompoundTag tag = new CompoundTag();
		SimpleContainer inventory = worker.getInventory();
		tag.putInt(TAG_EMERALDS, WorkerSupplies.emeralds(level, worker));
		tag.putInt(TAG_WAGE, WorkerLevels.dailyWage(worker));
		tag.putBoolean(TAG_PAID, WorkerSupplies.isPaid(level, worker));
		ListTag tools = new ListTag();
		for (WorkerSupplies.Tool tool : WorkerSupplies.toolsOf(worker)) {
			CompoundTag entry = new CompoundTag();
			entry.putString(TAG_KIND, tool.kind());
			for (ItemStack stack : inventory) {
				if (!stack.isEmpty() && tool.ready().test(stack)) {
					entry.putString(TAG_ITEM, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
					entry.putInt(TAG_DAMAGE, stack.getDamageValue());
					break;
				}
			}
			tools.add(entry);
		}
		tag.put(TAG_TOOLS, tools);
		ListTag stocks = new ListTag();
		for (WorkerSupplies.Stock stock : WorkerSupplies.stocksOf(worker)) {
			CompoundTag entry = new CompoundTag();
			entry.putString(TAG_KIND, stock.kind());
			entry.putBoolean(TAG_REQUIRED, stock.required());
			// The kind it has the most of stands for all of them
			Item most = Items.AIR;
			int mostCount = 0;
			for (ItemStack stack : inventory) {
				if (!stack.isEmpty() && stock.item().test(stack) && stack.getCount() > mostCount) {
					most = stack.getItem();
					mostCount = stack.getCount();
				}
			}
			if (most != Items.AIR) {
				entry.putString(TAG_ITEM, BuiltInRegistries.ITEM.getKey(most).toString());
				entry.putInt(TAG_COUNT, WorkerSupplies.count(inventory, stock.item()));
			}
			stocks.add(entry);
		}
		tag.put(TAG_STOCKS, stocks);
		tag.putInt(TAG_VILLAGE_CHESTS, VillageChests.inReach(level, VillageChests.workplace(level, worker)).size());
		tag.putInt(TAG_WORN, WorkerSupplies.count(inventory, stack -> WorkerSupplies.isToGiveBack(worker, stack)));
		if (Meals.enabled()) {
			MealData meals = Meals.of(worker);
			CompoundTag lunch = new CompoundTag();
			lunch.putInt(TAG_PURSE, meals.purse());
			int food = 0;
			Item most = Items.AIR;
			int mostCount = 0;
			for (ItemStack stack : meals.bag()) {
				if (Meals.isMeal(stack)) {
					food += Meals.nutrition(stack) * stack.getCount();
					if (stack.getCount() > mostCount) {
						most = stack.getItem();
						mostCount = stack.getCount();
					}
				}
			}
			lunch.putInt(TAG_FOOD, food);
			if (most != Items.AIR) {
				lunch.putString(TAG_ITEM, BuiltInRegistries.ITEM.getKey(most).toString());
				lunch.putInt(TAG_COUNT, Meals.count(meals));
			}
			lunch.putInt(TAG_DAILY_FOOD, Meals.dailyFood());
			lunch.putInt(TAG_UPKEEP, Config.UPKEEP.get());
			lunch.putBoolean(TAG_EATS_OUT, Meals.eatsOut(worker));
			lunch.putBoolean(TAG_HUNGRY, Meals.isHungry(worker));
			tag.put(TAG_LUNCH, lunch);
		}
		info.put(TAG, tag);
	}

	/** Empty if the menu shows no hired worker. */
	static Optional<SuppliesInfo> read(CompoundTag info) {
		return info.getCompound(TAG).map(tag -> {
			List<Slot> tools = new ArrayList<>();
			for (CompoundTag entry : tag.getListOrEmpty(TAG_TOOLS).compoundStream().toList()) {
				ItemStack tool = stack(entry.getStringOr(TAG_ITEM, ""), 1);
				if (!tool.isEmpty() && tool.isDamageableItem()) {
					tool.setDamageValue(entry.getIntOr(TAG_DAMAGE, 0));
				}
				tools.add(new Slot(entry.getStringOr(TAG_KIND, ""), tool, true));
			}
			List<Slot> stocks = new ArrayList<>();
			for (CompoundTag entry : tag.getListOrEmpty(TAG_STOCKS).compoundStream().toList()) {
				stocks.add(new Slot(entry.getStringOr(TAG_KIND, ""), stack(entry.getStringOr(TAG_ITEM, ""), entry.getIntOr(TAG_COUNT, 0)),
					entry.getBooleanOr(TAG_REQUIRED, false)));
			}
			return new SuppliesInfo(
				tag.getIntOr(TAG_EMERALDS, 0),
				tag.getIntOr(TAG_WAGE, 1),
				tag.getBooleanOr(TAG_PAID, false),
				List.copyOf(tools),
				List.copyOf(stocks),
				tag.getIntOr(TAG_VILLAGE_CHESTS, 0),
				tag.getIntOr(TAG_WORN, 0),
				tag.getCompound(TAG_LUNCH).map(lunch -> new Lunch(
					lunch.getIntOr(TAG_PURSE, 0),
					lunch.getIntOr(TAG_FOOD, 0),
					stack(lunch.getStringOr(TAG_ITEM, ""), lunch.getIntOr(TAG_COUNT, 0)),
					lunch.getIntOr(TAG_DAILY_FOOD, 10),
					lunch.getIntOr(TAG_UPKEEP, 0),
					lunch.getBooleanOr(TAG_EATS_OUT, false),
					lunch.getBooleanOr(TAG_HUNGRY, false)
				))
			);
		});
	}

	private static ItemStack stack(String id, int count) {
		Identifier key = Identifier.tryParse(id);
		if (id.isEmpty() || key == null || count <= 0) {
			return ItemStack.EMPTY;
		}
		return BuiltInRegistries.ITEM.getOptional(key).map(item -> new ItemStack(item, count)).orElse(ItemStack.EMPTY);
	}
}
