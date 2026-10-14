package me.teajay.workingvillagers.menu;

import java.util.Optional;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
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
 * are in it, whether today's wage was taken and, for a lumberjack, its axe and saplings.
 *
 * @param emeralds emeralds in the chest, -1 if the chest is not loaded or gone
 * @param wage the emeralds a worker takes per work day ({@link Config#DAILY_WAGE} of the server)
 * @param axe how much of the lumberjack's axe is left in percent, -1 without an axe, -2 if
 *     the worker needs none
 * @param saplings saplings the lumberjack carries plus those in the chest, -1 if the worker
 *     needs none
 */
public record PersonalChestInfo(Optional<BlockPos> pos, int emeralds, int wage, boolean paid, int axe, int saplings) {
	private static final String TAG = "PersonalChest";
	private static final String TAG_POS = "Pos";
	private static final String TAG_EMERALDS = "Emeralds";
	private static final String TAG_WAGE = "Wage";
	private static final String TAG_PAID = "Paid";
	private static final String TAG_AXE = "Axe";
	private static final String TAG_SAPLINGS = "Saplings";

	public boolean needsTools() {
		return this.axe != -2;
	}

	static void write(ServerLevel level, Villager worker, CompoundTag info) {
		CompoundTag tag = new CompoundTag();
		BlockPos pos = ((HiredVillager) worker).workingvillagers$getPersonalChest();
		tag.storeNullable(TAG_POS, BlockPos.CODEC, pos);
		Optional<Container> chest = PersonalChest.containerOf(level, worker);
		tag.putInt(TAG_EMERALDS, chest.map(container -> PersonalChest.countIn(container, stack -> stack.is(PersonalChest.WAGE))).orElse(-1));
		tag.putInt(TAG_WAGE, Config.DAILY_WAGE.get());
		tag.putBoolean(TAG_PAID, PersonalChest.isPaid(level, worker));
		if (worker.getVillagerData().profession().is(WorkingVillagers.LUMBERJACK_PROFESSION)) {
			int axe = -1;
			int saplings = 0;
			for (ItemStack stack : worker.getInventory()) {
				if (stack.is(ItemTags.AXES) && axe < 0) {
					axe = stack.isDamageableItem() ? 100 - stack.getDamageValue() * 100 / stack.getMaxDamage() : 100;
				} else if (stack.is(ItemTags.SAPLINGS)) {
					saplings += stack.getCount();
				}
			}
			saplings += chest.map(container -> PersonalChest.countIn(container, stack -> stack.is(ItemTags.SAPLINGS))).orElse(0);
			tag.putInt(TAG_AXE, axe);
			tag.putInt(TAG_SAPLINGS, saplings);
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
			tag.getIntOr(TAG_AXE, -2),
			tag.getIntOr(TAG_SAPLINGS, -1)
		));
	}
}
