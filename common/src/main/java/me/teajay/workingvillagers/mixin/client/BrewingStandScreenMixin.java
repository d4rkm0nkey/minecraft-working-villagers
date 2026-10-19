package me.teajay.workingvillagers.mixin.client;

import me.teajay.workingvillagers.client.StationTabs;
import me.teajay.workingvillagers.menu.CutterMenu;
import me.teajay.workingvillagers.menu.StationWorker;
import me.teajay.workingvillagers.station.StationType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BrewingStandMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The tabs of the apothecary's station above the brewing stand screen (see {@link StationTabs}):
 * the brewing stand is the open one, the others open the overview there.
 */
@Mixin(BrewingStandScreen.class)
public abstract class BrewingStandScreenMixin extends AbstractContainerScreen<BrewingStandMenu> {
	private BrewingStandScreenMixin(BrewingStandMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void workingvillagers$stationTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		StationTabs.extractAbove(graphics, this.leftPos, this.topPos, mouseX, mouseY, StationType.BREWING_STAND, ((StationWorker) this.menu).workingvillagers$tabFlags());
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		StationTabs.tooltip(graphics, this.font, mouseX, mouseY, this.leftPos, this.topPos, StationType.BREWING_STAND,
			((StationWorker) this.menu).workingvillagers$tabFlags());
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (StationTabs.click(this.minecraft, this.menu.containerId, CutterMenu.STATION_BUTTON, event.x(), event.y(), this.leftPos, this.topPos,
			StationType.BREWING_STAND, ((StationWorker) this.menu).workingvillagers$tabFlags())) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}
}
