package me.teajay.workingvillagers.mixin.client;

import java.util.List;
import me.teajay.workingvillagers.client.StationTabs;
import me.teajay.workingvillagers.menu.CutterMenu;
import me.teajay.workingvillagers.menu.StationWorker;
import me.teajay.workingvillagers.station.StationType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SmokerMenu;
import org.spongepowered.asm.mixin.Mixin;

/**
 * The tabs of the butcher's station above the smoker screen (see {@link StationTabs}):
 * the smoker is the open one, the others open the overview there.
 */
@Mixin(SmokerScreen.class)
public abstract class SmokerScreenMixin extends AbstractFurnaceScreen<SmokerMenu> {
	private SmokerScreenMixin(SmokerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, title, Identifier.withDefaultNamespace("textures/gui/container/smoker.png"), null, null, List.of());
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		StationTabs.extractAbove(graphics, this.leftPos, this.topPos, mouseX, mouseY, StationType.SMOKER, ((StationWorker) this.menu).workingvillagers$tabFlags());
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		StationTabs.tooltip(graphics, this.font, mouseX, mouseY, this.leftPos, this.topPos, StationType.SMOKER, ((StationWorker) this.menu).workingvillagers$tabFlags());
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (StationTabs.click(this.minecraft, this.menu.containerId, CutterMenu.STATION_BUTTON, event.x(), event.y(), this.leftPos, this.topPos,
			StationType.SMOKER, ((StationWorker) this.menu).workingvillagers$tabFlags())) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}
}
