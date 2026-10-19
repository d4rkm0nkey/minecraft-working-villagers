package me.teajay.workingvillagers.block;

import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.crafting.KilnRecipes;
import me.teajay.workingvillagers.menu.KilnMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The kiln: a furnace that only fires what {@link KilnRecipes} allows, at the speed of a
 * furnace. Anything else in its input slot (raw meat, raw iron) stays there unfired, while
 * the fuel burns down as usual.
 */
public class KilnBlockEntity extends AbstractFurnaceBlockEntity {
	private static final Component DEFAULT_NAME = Component.translatable("container.workingvillagers.kiln");

	public KilnBlockEntity(BlockPos pos, BlockState state) {
		super(WorkingVillagers.KILN_BLOCK_ENTITY.get(), pos, state, RecipeType.SMELTING);
	}

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, KilnBlockEntity kiln) {
		ItemStack input = kiln.items.get(SLOT_INPUT);
		if (input.isEmpty() || KilnRecipes.accepts(level, input)) {
			AbstractFurnaceBlockEntity.serverTick(level, pos, state, kiln);
			return;
		}
		// Ticks like a furnace with nothing to smelt, the input is put back right away
		kiln.items.set(SLOT_INPUT, ItemStack.EMPTY);
		try {
			AbstractFurnaceBlockEntity.serverTick(level, pos, state, kiln);
		} finally {
			kiln.items.set(SLOT_INPUT, input);
		}
	}

	/** Hoppers only put in what the kiln fires. */
	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot == SLOT_INPUT && this.level instanceof ServerLevel serverLevel) {
			return KilnRecipes.accepts(serverLevel, stack);
		}
		return super.canPlaceItem(slot, stack);
	}

	@Override
	protected Component getDefaultName() {
		return DEFAULT_NAME;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new KilnMenu(containerId, inventory, this, this.dataAccess);
	}
}
