package me.teajay.workingvillagers.mixin.client;

import me.teajay.workingvillagers.client.StationTabs;
import me.teajay.workingvillagers.menu.CutterMenu;
import me.teajay.workingvillagers.menu.StationWorker;
import me.teajay.workingvillagers.station.StationType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.LoomMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The tabs of the shepherd's station above the loom screen (see {@link StationTabs}): the
 * loom is the open one, the others open the overview there.
 */
@Mixin(LoomScreen.class)
public abstract class LoomScreenMixin extends AbstractContainerScreen<LoomMenu> {
	private LoomScreenMixin(LoomMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void workingvillagers$stationButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		StationTabs.extractAbove(graphics, this.leftPos, this.topPos, mouseX, mouseY, StationType.LOOM, ((StationWorker) this.menu).workingvillagers$tabFlags());
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		StationTabs.tooltip(graphics, this.font, mouseX, mouseY, this.leftPos, this.topPos, StationType.LOOM, ((StationWorker) this.menu).workingvillagers$tabFlags());
	}

	/** The loom screen has its own click handling (the patterns, the scroll bar): before it. */
	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void workingvillagers$clickStation(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		if (StationTabs.click(this.minecraft, this.menu.containerId, CutterMenu.STATION_BUTTON, event.x(), event.y(), this.leftPos, this.topPos,
			StationType.LOOM, ((StationWorker) this.menu).workingvillagers$tabFlags())) {
			cir.setReturnValue(true);
		}
	}
}
