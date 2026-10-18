package me.teajay.workingvillagers.block;

import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.menu.VillageChestMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 27 slots of a village chest. Everything else (opening the lid slowly, the sounds,
 * the redstone signal) is the vanilla chest's; only the menu shows more.
 */
public class VillageChestBlockEntity extends ChestBlockEntity {
	public VillageChestBlockEntity(BlockPos pos, BlockState state) {
		super(WorkingVillagers.VILLAGE_CHEST_BLOCK_ENTITY.get(), pos, state);
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("block." + WorkingVillagers.MOD_ID + ".village_chest");
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new VillageChestMenu(VillageChestMenu.SINGLE_ROWS, containerId, inventory, this, this.getBlockPos());
	}
}
