package me.teajay.workingvillagers.menu;

import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.crafting.KilnRecipes;
import me.teajay.workingvillagers.station.StationType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * The kiln's screen: the furnace screen with the furnace's recipe book. The "i" button opens
 * the overview of the kiln operator's station.
 */
public class KilnMenu extends AbstractFurnaceMenu {
	private final @Nullable Container kiln;

	/** Client side. */
	public KilnMenu(int containerId, Inventory inventory) {
		super(WorkingVillagers.KILN_MENU.get(), RecipePropertySet.FURNACE_INPUT, RecipeBookType.FURNACE, containerId, inventory);
		this.kiln = null;
	}

	public KilnMenu(int containerId, Inventory inventory, Container kiln, ContainerData data) {
		super(WorkingVillagers.KILN_MENU.get(), RecipePropertySet.FURNACE_INPUT, RecipeBookType.FURNACE, containerId, inventory, kiln, data);
		this.kiln = kiln;
	}

	/** Shift-clicking only puts in what the kiln fires; the client only knows the furnace's inputs. */
	@Override
	protected boolean canSmelt(ItemStack stack) {
		return this.level instanceof ServerLevel serverLevel ? KilnRecipes.accepts(serverLevel, stack) : super.canSmelt(stack);
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		if (buttonId == CutterMenu.STATION_BUTTON && this.kiln instanceof BlockEntity entity && entity.getLevel() != null) {
			player.openMenu(StationMenu.provider(StationType.KILN, entity.getLevel(), entity.getBlockPos()));
			return true;
		}
		return super.clickMenuButton(player, buttonId);
	}
}
