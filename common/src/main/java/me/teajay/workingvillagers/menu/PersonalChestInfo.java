package me.teajay.workingvillagers.menu;

import java.util.Optional;
import java.util.function.Predicate;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.job.FarmFields;
import me.teajay.workingvillagers.job.HiredVillager;
import me.teajay.workingvillagers.job.PersonalChest;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;

/**
 * What the menus show about a worker's personal chest: where it is, how many days of wage
 * are in it, whether today's wage was taken and, for a lumberjack or a farmer, its tool and
 * what it plants.
 *
 * @param emeralds emeralds in the chest, -1 if the chest is not loaded or gone
 * @param wage the emeralds a worker takes per work day ({@link Config#DAILY_WAGE} of the server)
 * @param tool the tool the worker needs ({@code "axe"} or {@code "hoe"}), empty if none
 * @param toolLeft how much of the tool is left in percent, -1 without one
 * @param stock what the worker plants (saplings, seeds), carried plus in the chest
 */
public record PersonalChestInfo(Optional<BlockPos> pos, int emeralds, int wage, boolean paid, String tool, int toolLeft, int stock) {
	private static final String TAG = "PersonalChest";
	private static final String TAG_POS = "Pos";
	private static final String TAG_EMERALDS = "Emeralds";
	private static final String TAG_WAGE = "Wage";
	private static final String TAG_PAID = "Paid";
	private static final String TAG_TOOL = "Tool";
	private static final String TAG_TOOL_LEFT = "ToolLeft";
	private static final String TAG_STOCK = "Stock";

	public boolean needsTools() {
		return !this.tool.isEmpty();
	}

	static void write(ServerLevel level, Villager worker, CompoundTag info) {
		CompoundTag tag = new CompoundTag();
		BlockPos pos = ((HiredVillager) worker).workingvillagers$getPersonalChest();
		tag.storeNullable(TAG_POS, BlockPos.CODEC, pos);
		Optional<Container> chest = PersonalChest.containerOf(level, worker);
		tag.putInt(TAG_EMERALDS, chest.map(container -> PersonalChest.countIn(container, stack -> stack.is(PersonalChest.WAGE))).orElse(-1));
		tag.putInt(TAG_WAGE, Config.DAILY_WAGE.get());
		tag.putBoolean(TAG_PAID, PersonalChest.isPaid(level, worker));
		Predicate<ItemStack> tool = null;
		Predicate<ItemStack> stock = null;
		if (worker.getVillagerData().profession().is(WorkingVillagers.LUMBERJACK_PROFESSION)) {
			tag.putString(TAG_TOOL, "axe");
			tool = stack -> stack.is(ItemTags.AXES);
			stock = stack -> stack.is(ItemTags.SAPLINGS);
		} else if (worker.getVillagerData().profession().is(WorkingVillagers.FARMER_PROFESSION)) {
			tag.putString(TAG_TOOL, "hoe");
			tool = stack -> stack.is(ItemTags.HOES);
			stock = FarmFields::isSeed;
		}
		if (tool != null) {
			int left = -1;
			int count = 0;
			for (ItemStack stack : worker.getInventory()) {
				if (tool.test(stack) && left < 0) {
					left = stack.isDamageableItem() ? 100 - stack.getDamageValue() * 100 / stack.getMaxDamage() : 100;
				} else if (stock.test(stack)) {
					count += stack.getCount();
				}
			}
			Predicate<ItemStack> stocked = stock;
			count += chest.map(container -> PersonalChest.countIn(container, stocked)).orElse(0);
			tag.putInt(TAG_TOOL_LEFT, left);
			tag.putInt(TAG_STOCK, count);
		}
		info.put(TAG, tag);
	}

	/** Empty if the menu shows no hired worker. */
	static Optional<PersonalChestInfo> read(CompoundTag info) {
		return info.getCompound(TAG).map(tag -> new PersonalChestInfo(
			tag.read(TAG_POS, BlockPos.CODEC),
			tag.getIntOr(TAG_EMERALDS, -1),
			tag.getIntOr(TAG_WAGE, 1),
			tag.getBooleanOr(TAG_PAID, false),
			tag.getStringOr(TAG_TOOL, ""),
			tag.getIntOr(TAG_TOOL_LEFT, -1),
			tag.getIntOr(TAG_STOCK, 0)
		));
	}
}
