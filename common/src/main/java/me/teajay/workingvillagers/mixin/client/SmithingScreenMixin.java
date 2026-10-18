package me.teajay.workingvillagers.mixin.client;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import me.teajay.workingvillagers.client.GuiPanels;
import me.teajay.workingvillagers.menu.CutterMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SmithingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The "i" button at the top right of the smithing table screen, like at the blast furnace:
 * it opens the overview of the smith's station (worker, chests, wage).
 */
@Mixin(SmithingScreen.class)
public abstract class SmithingScreenMixin extends ItemCombinerScreen<SmithingMenu> {
	@Unique
	private static final int STATION_X = 157;
	@Unique
	private static final int STATION_Y = 3;
	@Unique
	private static final int STATION_SIZE = 12;

	private SmithingScreenMixin(SmithingMenu menu, Inventory inventory, Component title, Identifier menuResource) {
		super(menu, inventory, title, menuResource);
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void workingvillagers$stationButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
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
