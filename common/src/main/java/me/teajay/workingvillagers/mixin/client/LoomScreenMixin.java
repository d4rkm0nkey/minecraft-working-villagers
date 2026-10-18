package me.teajay.workingvillagers.mixin.client;

import java.util.Optional;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.client.WorkerButton;
import me.teajay.workingvillagers.menu.CutterMenu;
import me.teajay.workingvillagers.menu.StationWorker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.LoomMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The button with the shepherd's face at the top right of the loom screen, like at the
 * smithing table: it opens the overview of the shepherd's station (worker, chests, wage).
 */
@Mixin(LoomScreen.class)
public abstract class LoomScreenMixin extends AbstractContainerScreen<LoomMenu> {
	private LoomScreenMixin(LoomMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void workingvillagers$stationButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		WorkerButton.extract(graphics, this.leftPos, this.topPos, mouseX, mouseY, WorkingVillagers.SHEPHERD_PROFESSION,
			((StationWorker) this.menu).workingvillagers$hasWorker());
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		if (this.workingvillagers$isOverStation(mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(WorkerButton.tooltip(((StationWorker) this.menu).workingvillagers$hasWorker()
				? null : WorkerButton.workerLine(Optional.empty())), mouseX, mouseY);
		}
	}

	/** The loom screen has its own click handling (the patterns, the scroll bar): before it. */
	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void workingvillagers$clickStation(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		if (this.workingvillagers$isOverStation(event.x(), event.y())) {
			WorkerButton.click(this.minecraft, this.menu.containerId, CutterMenu.STATION_BUTTON);
			cir.setReturnValue(true);
		}
	}

	@Unique
	private boolean workingvillagers$isOverStation(double mouseX, double mouseY) {
		return WorkerButton.isOver(mouseX, mouseY, this.leftPos, this.topPos);
	}
}
