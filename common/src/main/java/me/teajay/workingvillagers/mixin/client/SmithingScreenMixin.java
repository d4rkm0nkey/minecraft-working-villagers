package me.teajay.workingvillagers.mixin.client;

import java.util.Optional;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.client.WorkerButton;
import me.teajay.workingvillagers.menu.CutterMenu;
import me.teajay.workingvillagers.menu.StationWorker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SmithingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The button with the smith's face at the top right of the smithing table screen, like at
 * the blast furnace: it opens the overview of the smith's station (worker, chests, wage).
 */
@Mixin(SmithingScreen.class)
public abstract class SmithingScreenMixin extends ItemCombinerScreen<SmithingMenu> {
	private SmithingScreenMixin(SmithingMenu menu, Inventory inventory, Component title, Identifier menuResource) {
		super(menu, inventory, title, menuResource);
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void workingvillagers$stationButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		WorkerButton.extract(graphics, this.leftPos, this.topPos, mouseX, mouseY, WorkingVillagers.SMITH_PROFESSION,
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

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.workingvillagers$isOverStation(event.x(), event.y())) {
			WorkerButton.click(this.minecraft, this.menu.containerId, CutterMenu.STATION_BUTTON);
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Unique
	private boolean workingvillagers$isOverStation(double mouseX, double mouseY) {
		return WorkerButton.isOver(mouseX, mouseY, this.leftPos, this.topPos);
	}
}
