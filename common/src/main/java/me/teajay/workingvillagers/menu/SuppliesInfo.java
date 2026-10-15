package me.teajay.workingvillagers.menu;

import java.util.Optional;
import java.util.function.Predicate;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.job.WorkerSupplies;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What the menus show about what a worker carries for its work (see {@link WorkerSupplies}):
 * its emeralds, whether today's wage was taken and, for a lumberjack or a farmer, its tool
 * and what it plants.
 *
 * @param emeralds the emeralds the worker carries
 * @param wage the emeralds a worker takes per work day ({@link Config#DAILY_WAGE} of the server)
 * @param tool the tool the worker carries, empty if none (also if it needs none)
 * @param toolKind the tool the worker needs ({@code "axe"} or {@code "hoe"}), empty if none
 * @param stock what the worker plants (saplings, seeds) as one stack with the total count,
 *     empty if it carries none or plants nothing
 * @param stockKind what the worker plants ({@code "saplings"} or {@code "seeds"}), empty if nothing
 */
public record SuppliesInfo(int emeralds, int wage, boolean paid, ItemStack tool, String toolKind, ItemStack stock, String stockKind) {
	private static final String TAG = "Supplies";
	private static final String TAG_EMERALDS = "Emeralds";
	private static final String TAG_WAGE = "Wage";
	private static final String TAG_PAID = "Paid";
	private static final String TAG_TOOL = "Tool";
	private static final String TAG_TOOL_DAMAGE = "ToolDamage";
	private static final String TAG_TOOL_KIND = "ToolKind";
	private static final String TAG_STOCK = "Stock";
	private static final String TAG_STOCK_COUNT = "StockCount";
	private static final String TAG_STOCK_KIND = "StockKind";

	public boolean needsTool() {
		return !this.toolKind.isEmpty();
	}

	/** Whether the worker cannot work because of its supplies. */
	public boolean hasProblem() {
		return !this.paid && this.emeralds < this.wage || this.needsTool() && this.tool.isEmpty();
	}

	static void write(ServerLevel level, Villager worker, CompoundTag info) {
		CompoundTag tag = new CompoundTag();
		SimpleContainer inventory = worker.getInventory();
		tag.putInt(TAG_EMERALDS, WorkerSupplies.count(inventory, WorkerSupplies::isWage));
		tag.putInt(TAG_WAGE, Config.DAILY_WAGE.get());
		tag.putBoolean(TAG_PAID, WorkerSupplies.isPaid(level, worker));
		WorkerSupplies.Tool tool = WorkerSupplies.toolOf(worker);
		if (tool != null) {
			tag.putString(TAG_TOOL_KIND, tool == WorkerSupplies.AXE ? "axe" : "hoe");
			for (ItemStack stack : inventory) {
				if (!stack.isEmpty() && tool.item().test(stack)) {
					tag.putString(TAG_TOOL, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
					tag.putInt(TAG_TOOL_DAMAGE, stack.getDamageValue());
					break;
				}
			}
		}
		Predicate<ItemStack> stock = WorkerSupplies.stockOf(worker);
		if (stock != null) {
			tag.putString(TAG_STOCK_KIND, tool == WorkerSupplies.AXE ? "saplings" : "seeds");
			Item most = Items.AIR;
			int mostCount = 0;
			for (ItemStack stack : inventory) {
				if (!stack.isEmpty() && stock.test(stack) && stack.getCount() > mostCount) {
					most = stack.getItem();
					mostCount = stack.getCount();
				}
			}
			if (most != Items.AIR) {
				tag.putString(TAG_STOCK, BuiltInRegistries.ITEM.getKey(most).toString());
				tag.putInt(TAG_STOCK_COUNT, WorkerSupplies.count(inventory, stock));
			}
		}
		info.put(TAG, tag);
	}

	/** Empty if the menu shows no hired worker. */
	static Optional<SuppliesInfo> read(CompoundTag info) {
		return info.getCompound(TAG).map(tag -> {
			ItemStack tool = stack(tag.getStringOr(TAG_TOOL, ""), 1);
			if (!tool.isEmpty() && tool.isDamageableItem()) {
				tool.setDamageValue(tag.getIntOr(TAG_TOOL_DAMAGE, 0));
			}
			return new SuppliesInfo(
				tag.getIntOr(TAG_EMERALDS, 0),
				tag.getIntOr(TAG_WAGE, 1),
				tag.getBooleanOr(TAG_PAID, false),
				tool,
				tag.getStringOr(TAG_TOOL_KIND, ""),
				stack(tag.getStringOr(TAG_STOCK, ""), tag.getIntOr(TAG_STOCK_COUNT, 0)),
				tag.getStringOr(TAG_STOCK_KIND, "")
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
