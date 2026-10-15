package me.teajay.workingvillagers.mixin;

import me.teajay.workingvillagers.menu.CutterMenu;
import me.teajay.workingvillagers.menu.StationMenu;
import me.teajay.workingvillagers.station.StationType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes the vanilla blast furnace the smith's workstation: the "i" button in its screen
 * (see {@code BlastFurnaceScreenMixin}) opens the overview of the station.
 */
@Mixin(BlastFurnaceMenu.class)
public abstract class BlastFurnaceMenuMixin extends AbstractFurnaceMenu {
	private BlastFurnaceMenuMixin(MenuType<?> menuType, ResourceKey<RecipePropertySet> allowedInputs, RecipeBookType recipeBookType, int containerId, Inventory inventory) {
		super(menuType, allowedInputs, recipeBookType, containerId, inventory);
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		if (buttonId == CutterMenu.STATION_BUTTON
			&& ((AbstractFurnaceMenuAccessor) this).workingvillagers$getContainer() instanceof BlockEntity furnace
			&& furnace.getLevel() != null) {
			player.openMenu(StationMenu.provider(StationType.BLAST_FURNACE, furnace.getLevel(), furnace.getBlockPos()));
			return true;
		}
		return super.clickMenuButton(player, buttonId);
	}
}
