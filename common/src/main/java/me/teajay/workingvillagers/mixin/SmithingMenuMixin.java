package me.teajay.workingvillagers.mixin;

import me.teajay.workingvillagers.menu.CutterMenu;
import me.teajay.workingvillagers.menu.StationMenu;
import me.teajay.workingvillagers.station.StationType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SmithingMenu;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes the vanilla smithing table the smith's second workstation: the "i" button in its
 * screen (see {@code SmithingScreenMixin}) opens the overview of the station.
 */
@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin extends ItemCombinerMenu {
	private SmithingMenuMixin(MenuType<?> menuType, int containerId, Inventory inventory, ContainerLevelAccess access, ItemCombinerMenuSlotDefinition slots) {
		super(menuType, containerId, inventory, access, slots);
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		if (buttonId == CutterMenu.STATION_BUTTON) {
			this.access.execute((level, pos) -> player.openMenu(StationMenu.provider(StationType.SMITHING_TABLE, level, pos)));
			return true;
		}
		return super.clickMenuButton(player, buttonId);
	}
}
