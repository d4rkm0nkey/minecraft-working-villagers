package me.teajay.workingvillagers.mixin.client;

import java.util.List;
import java.util.Optional;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.client.WorkerButton;
import me.teajay.workingvillagers.menu.CutterMenu;
import me.teajay.workingvillagers.menu.StationWorker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * The button with the smith's face at the top right of the blast furnace screen, like in the
 * stonecutter: it opens the overview of the smith's station (worker, chests, wage).
 */
@Mixin(BlastFurnaceScreen.class)
public abstract class BlastFurnaceScreenMixin extends AbstractFurnaceScreen<BlastFurnaceMenu> {
	private BlastFurnaceScreenMixin(BlastFurnaceMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, title, Identifier.withDefaultNamespace("textures/gui/container/blast_furnace.png"), null, null, List.of());
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
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
