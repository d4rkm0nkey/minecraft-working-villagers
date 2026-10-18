package me.teajay.workingvillagers.mixin.client;

import me.teajay.workingvillagers.client.StationTabs;
import me.teajay.workingvillagers.menu.CutterMenu;
import me.teajay.workingvillagers.menu.StationWorker;
import me.teajay.workingvillagers.station.StationType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SmithingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The tabs of the smith's station above the smithing table screen (see {@link StationTabs}):
 * the smithing table is the open one, the others open the overview there.
 */
@Mixin(SmithingScreen.class)
public abstract class SmithingScreenMixin extends ItemCombinerScreen<SmithingMenu> {
	private SmithingScreenMixin(SmithingMenu menu, Inventory inventory, Component title, Identifier menuResource) {
		super(menu, inventory, title, menuResource);
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void workingvillagers$stationButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		StationTabs.extractAbove(graphics, this.leftPos, this.topPos, mouseX, mouseY, StationType.SMITHING_TABLE, ((StationWorker) this.menu).workingvillagers$tabFlags());
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		StationTabs.tooltip(graphics, this.font, mouseX, mouseY, this.leftPos, this.topPos, StationType.SMITHING_TABLE, ((StationWorker) this.menu).workingvillagers$tabFlags());
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (StationTabs.click(this.minecraft, this.menu.containerId, CutterMenu.STATION_BUTTON, event.x(), event.y(), this.leftPos, this.topPos,
			StationType.SMITHING_TABLE, ((StationWorker) this.menu).workingvillagers$tabFlags())) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}
}
