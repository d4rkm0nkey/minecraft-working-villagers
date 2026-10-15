package me.teajay.workingvillagers.mixin.client;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.List;
import me.teajay.workingvillagers.client.GuiPanels;
import me.teajay.workingvillagers.menu.CutterMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * The "i" button at the top right of the blast furnace screen, like in the stonecutter: it
 * opens the overview of the smith's station (worker, chests, wage).
 */
@Mixin(BlastFurnaceScreen.class)
public abstract class BlastFurnaceScreenMixin extends AbstractFurnaceScreen<BlastFurnaceMenu> {
	@Unique
	private static final int STATION_X = 157;
	@Unique
	private static final int STATION_Y = 3;
	@Unique
	private static final int STATION_SIZE = 12;

	private BlastFurnaceScreenMixin(BlastFurnaceMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, title, Identifier.withDefaultNamespace("textures/gui/container/blast_furnace.png"), null, null, List.of());
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		boolean over = this.workingvillagers$isOverStation(mouseX, mouseY);
		GuiPanels.button(graphics, this.font, this.leftPos + STATION_X, this.topPos + STATION_Y, STATION_SIZE, "i", over);
		if (over) {
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		if (this.workingvillagers$isOverStation(mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(this.font, Component.translatable("gui.workingvillagers.station.open"), mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.workingvillagers$isOverStation(event.x(), event.y())) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, CutterMenu.STATION_BUTTON);
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Unique
	private boolean workingvillagers$isOverStation(double mouseX, double mouseY) {
		return GuiPanels.isOver(mouseX, mouseY, this.leftPos + STATION_X, this.topPos + STATION_Y, STATION_SIZE, STATION_SIZE);
	}
}
