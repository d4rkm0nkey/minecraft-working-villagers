package me.teajay.workingvillagers.client;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.teajay.workingvillagers.carrier.Route;
import me.teajay.workingvillagers.menu.CarrierMenu;
import me.teajay.workingvillagers.menu.SuppliesInfo;
import me.teajay.workingvillagers.network.RenameRoutePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;

/**
 * The carrier screen: one row per route (name, source, target) with buttons to rename and
 * delete it, free slots as buttons that start picking a new route, the basket and the
 * player's inventory.
 */
public class CarrierScreen extends AbstractContainerScreen<CarrierMenu> {
	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
	private static final int ROWS_X = 7;
	private static final int STATUS_Y = 17;
	private static final int ROWS_Y = 28;
	private static final int ROW_WIDTH = 162;
	private static final int ROW_HEIGHT = 30;
	private static final int BUTTON_SIZE = 12;
	private static final int TEXT_X = 15;
	private static final int HINT_Y = ROWS_Y + Route.MAX_ROUTES * ROW_HEIGHT + 3;
	private static final int SHARE_Y = 3;
	private static final int SHARE_HEIGHT = 12;

	/** The text field while a route is renamed, null otherwise. */
	private @Nullable EditBox nameBox;
	private int renaming = -1;

	public CarrierScreen(CarrierMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, CarrierMenu.INVENTORY_Y + 82);
		this.inventoryLabelY = CarrierMenu.INVENTORY_Y - 11;
	}

	@Override
	protected void init() {
		super.init();
		// The widgets are rebuilt on resize, the text field with them
		this.nameBox = null;
		this.renaming = -1;
	}

	private int rowX() {
		return this.leftPos + ROWS_X;
	}

	private int rowY(int index) {
		return this.topPos + ROWS_Y + index * ROW_HEIGHT;
	}

	private int deleteX() {
		return this.rowX() + ROW_WIDTH - BUTTON_SIZE - 3;
	}

	private int renameX() {
		return this.deleteX() - BUTTON_SIZE - 2;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		GuiPanels.panel(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
		for (Slot slot : this.menu.slots) {
			if (slot.isActive()) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, this.leftPos + slot.x - 1, this.topPos + slot.y - 1, 18, 18);
			}
		}

		List<Route> routes = this.menu.getRoutes();
		boolean canEdit = this.menu.canEdit();
		for (int i = 0; i < Route.MAX_ROUTES; i++) {
			int x = this.rowX();
			int y = this.rowY(i);
			if (i >= routes.size()) {
				if (canEdit && i == routes.size()) {
					boolean hovered = this.isOverFreeRow(mouseX, mouseY, i);
					GuiPanels.button(graphics, this.font, x, y, ROW_WIDTH, ROW_HEIGHT - 2,
						Component.translatable("gui.workingvillagers.carrier.new_route").getString(), hovered);
					if (hovered) {
						graphics.requestCursor(CursorTypes.POINTING_HAND);
					}
				} else {
					GuiPanels.inset(graphics, x, y, ROW_WIDTH, ROW_HEIGHT - 2, 0xFFB4B4B4);
					graphics.centeredText(this.font, Component.translatable("gui.workingvillagers.carrier.free"), x + ROW_WIDTH / 2, y + 10, GuiPanels.MUTED_COLOR);
				}
				continue;
			}

			Route route = routes.get(i);
			boolean active = i == this.menu.getActiveRoute();
			GuiPanels.inset(graphics, x, y, ROW_WIDTH, ROW_HEIGHT - 2, active ? 0xFFA9C99F : 0xFFB4B4B4);
			int textWidth = (canEdit ? this.renameX() - 3 : x + ROW_WIDTH - 3) - (x + TEXT_X);
			if (i != this.renaming) {
				GuiPanels.scrollingText(graphics, this.font, route.title(i).getString(), x + TEXT_X, y + 3, textWidth, 0xFF202020);
			}
			graphics.text(this.font, String.valueOf(i + 1), x + 5, y + 10, GuiPanels.MUTED_COLOR, false);
			this.line(graphics, Component.translatable("gui.workingvillagers.carrier.from", this.menu.getContainerName(i, false)), x + TEXT_X, y + 12, ROW_WIDTH - TEXT_X - 3);
			this.line(graphics, Component.translatable("gui.workingvillagers.carrier.to", this.menu.getContainerName(i, true)), x + TEXT_X, y + 20, ROW_WIDTH - TEXT_X - 3);
			if (canEdit && i != this.renaming) {
				this.smallButton(graphics, mouseX, mouseY, this.renameX(), y + 1, "✎");
				this.smallButton(graphics, mouseX, mouseY, this.deleteX(), y + 1, "×");
			}
		}

		if (this.menu.canChangeSharing()) {
			int width = this.shareWidth();
			int bx = this.leftPos + this.imageWidth - 7 - width;
			int by = this.topPos + SHARE_Y;
			boolean hovered = GuiPanels.isOver(mouseX, mouseY, bx, by, width, SHARE_HEIGHT);
			GuiPanels.button(graphics, this.font, bx, by, width, SHARE_HEIGHT, this.shareLabel().getString(), hovered);
			if (hovered) {
				graphics.requestCursor(CursorTypes.POINTING_HAND);
			}
		}

		Optional<SuppliesInfo> supplies = this.menu.getSupplies();
		int y = this.topPos + HINT_Y;
		if (supplies.isPresent()) {
			// The carrier's emeralds, the line next to them; room for one more line only
			SuppliesInfo info = supplies.get();
			SuppliesRow.extractSlots(graphics, this.font, info, this.rowX(), y);
			int x = this.rowX() + SuppliesRow.slotsWidth(info) + 4;
			int width = ROW_WIDTH - SuppliesRow.slotsWidth(info) - 4;
			GuiPanels.scrollingText(graphics, this.font, SuppliesRow.line(info).getString(), x, y + 1, width, SuppliesRow.color(info));
			if (!canEdit) {
				GuiPanels.scrollingText(graphics, this.font, Component.translatable("gui.workingvillagers.carrier.hint_not_yours").getString(), x, y + 10, width,
					GuiPanels.MUTED_COLOR);
			}
			return;
		}
		Component hint = Component.translatable(canEdit ? "gui.workingvillagers.carrier.hint" : "gui.workingvillagers.carrier.hint_not_yours");
		for (FormattedCharSequence line : this.font.split(hint, ROW_WIDTH)) {
			graphics.text(this.font, line, this.leftPos + ROWS_X + 1, y, GuiPanels.MUTED_COLOR, false);
			y += 9;
		}
	}

	private boolean isOverSupplies(double mouseX, double mouseY) {
		return this.menu.getSupplies().isPresent()
			&& GuiPanels.isOver(mouseX, mouseY, this.rowX(), this.topPos + HINT_Y, ROW_WIDTH, SuppliesRow.SLOT);
	}

	private void smallButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, String label) {
		boolean hovered = GuiPanels.isOver(mouseX, mouseY, x, y, BUTTON_SIZE, BUTTON_SIZE);
		GuiPanels.button(graphics, this.font, x, y, BUTTON_SIZE, label, hovered);
		if (hovered) {
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}
	}

	private void line(GuiGraphicsExtractor graphics, Component text, int x, int y, int width) {
		GuiPanels.scrollingText(graphics, this.font, text.getString(), x, y, width, GuiPanels.TEXT_COLOR);
	}

	private Component shareLabel() {
		return Component.translatable(this.menu.isShared() ? "gui.workingvillagers.carrier.share.everyone" : "gui.workingvillagers.carrier.share.only_employer");
	}

	private int shareWidth() {
		return this.font.width(this.shareLabel()) + 10;
	}

	private boolean isOverShare(double mouseX, double mouseY) {
		int width = this.shareWidth();
		return this.menu.canChangeSharing()
			&& GuiPanels.isOver(mouseX, mouseY, this.leftPos + this.imageWidth - 7 - width, this.topPos + SHARE_Y, width, SHARE_HEIGHT);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
		int titleWidth = this.imageWidth - 16 - (this.menu.canChangeSharing() ? this.shareWidth() + 4 : 0);
		GuiPanels.scrollingText(graphics, this.font, this.title.getString(), this.titleLabelX, this.titleLabelY, titleWidth, GuiPanels.TEXT_COLOR);
		graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, GuiPanels.TEXT_COLOR, false);
		graphics.text(this.font, Component.translatable("gui.workingvillagers.carrier.basket"), 8, CarrierMenu.BASKET_Y + 4, GuiPanels.TEXT_COLOR, false);
		this.menu.getStatus().ifPresent(status -> GuiPanels.scrollingText(graphics, this.font,
			Component.translatable("gui.workingvillagers.status", status).getString(), 8, STATUS_Y, ROW_WIDTH, GuiPanels.STATUS_COLOR));
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		if (this.isOverShare(mouseX, mouseY)) {
			graphics.setComponentTooltipForNextFrame(this.font, List.of(
				Component.translatable("gui.workingvillagers.carrier.share.tooltip"),
				this.shareLabel().copy().withStyle(ChatFormatting.GRAY),
				Component.translatable("gui.workingvillagers.carrier.share.click").withStyle(ChatFormatting.DARK_GRAY)
			), mouseX, mouseY);
			return;
		}
		if (this.isOverSupplies(mouseX, mouseY)) {
			graphics.setComponentTooltipForNextFrame(this.font, SuppliesRow.tooltip(this.menu.getSupplies().get()), mouseX, mouseY);
			return;
		}
		List<Route> routes = this.menu.getRoutes();
		if (this.isOverFreeRow(mouseX, mouseY, routes.size())) {
			graphics.setTooltipForNextFrame(this.font.split(Component.translatable("gui.workingvillagers.carrier.new_route.tooltip"), 200), mouseX, mouseY);
			return;
		}
		for (int i = 0; i < routes.size(); i++) {
			if (this.isOverButton(mouseX, mouseY, this.deleteX(), i)) {
				graphics.setTooltipForNextFrame(this.font, Component.translatable("gui.workingvillagers.carrier.delete"), mouseX, mouseY);
			} else if (this.isOverButton(mouseX, mouseY, this.renameX(), i)) {
				graphics.setTooltipForNextFrame(this.font, Component.translatable("gui.workingvillagers.carrier.rename"), mouseX, mouseY);
			} else if (i != this.renaming && GuiPanels.isOver(mouseX, mouseY, this.rowX(), this.rowY(i), ROW_WIDTH, ROW_HEIGHT - 2)) {
				graphics.setTooltipForNextFrame(this.routeTooltip(routes.get(i), i), mouseX, mouseY);
			}
		}
	}

	private List<FormattedCharSequence> routeTooltip(Route route, int index) {
		List<Component> lines = new ArrayList<>();
		lines.add(route.title(index));
		lines.add(Component.translatable("gui.workingvillagers.carrier.tooltip.from", this.menu.getContainerName(index, false))
			.withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("gui.workingvillagers.carrier.tooltip.to", this.menu.getContainerName(index, true))
			.withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("gui.workingvillagers.distance", Math.round(Math.sqrt(route.source().distSqr(route.target()))))
			.withStyle(ChatFormatting.GRAY));
		if (index == this.menu.getActiveRoute()) {
			lines.add(Component.translatable("gui.workingvillagers.carrier.tooltip.active").withStyle(ChatFormatting.DARK_GREEN));
		}
		List<FormattedCharSequence> result = new ArrayList<>();
		for (Component line : lines) {
			result.addAll(this.font.split(line, 220));
		}
		return result;
	}

	/** The name of the container block, if the client knows it. */
	private boolean isOverButton(double mouseX, double mouseY, int x, int index) {
		return this.menu.canEdit() && index != this.renaming && GuiPanels.isOver(mouseX, mouseY, x, this.rowY(index) + 1, BUTTON_SIZE, BUTTON_SIZE);
	}

	/** Only the first free slot is a button: new routes are added at the end. */
	private boolean isOverFreeRow(double mouseX, double mouseY, int index) {
		return this.menu.canEdit() && index == this.menu.getRoutes().size() && index < Route.MAX_ROUTES
			&& GuiPanels.isOver(mouseX, mouseY, this.rowX(), this.rowY(index), ROW_WIDTH, ROW_HEIGHT - 2);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.nameBox != null && !this.nameBox.isMouseOver(event.x(), event.y())) {
			this.finishRename(true);
		}
		if (this.isOverShare(event.x(), event.y())) {
			this.click(CarrierMenu.SHARE_BUTTON);
			return true;
		}
		List<Route> routes = this.menu.getRoutes();
		if (this.isOverFreeRow(event.x(), event.y(), routes.size())) {
			this.click(CarrierMenu.NEW_ROUTE_BUTTON);
			return true;
		}
		for (int i = 0; i < routes.size(); i++) {
			if (this.isOverButton(event.x(), event.y(), this.deleteX(), i)) {
				this.click(i);
				return true;
			}
			if (this.isOverButton(event.x(), event.y(), this.renameX(), i)) {
				this.startRename(i, routes.get(i));
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void click(int button) {
		this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, button);
	}

	private void startRename(int index, Route route) {
		this.finishRename(false);
		int x = this.rowX() + TEXT_X - 1;
		int width = ROW_WIDTH - TEXT_X - 3;
		EditBox box = new EditBox(this.font, x, this.rowY(index) + 1, width, 11, Component.translatable("gui.workingvillagers.carrier.rename"));
		box.setMaxLength(Route.MAX_NAME_LENGTH);
		box.setValue(route.name());
		box.setHint(route.title(index).copy().withStyle(ChatFormatting.DARK_GRAY));
		this.nameBox = box;
		this.renaming = index;
		this.addRenderableWidget(box);
		this.setFocused(box);
		box.setFocused(true);
	}

	/** Ends renaming; sends the new name if {@code save}. */
	private void finishRename(boolean save) {
		if (this.nameBox == null) {
			return;
		}
		if (save) {
			this.minecraft.getConnection().send(new ServerboundCustomPayloadPacket(
				new RenameRoutePayload(this.menu.containerId, this.renaming, this.nameBox.getValue())
			));
		}
		this.removeWidget(this.nameBox);
		this.nameBox = null;
		this.renaming = -1;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.nameBox != null) {
			if (event.isConfirmation()) {
				this.finishRename(true);
				return true;
			}
			if (event.isEscape()) {
				this.finishRename(false);
				return true;
			}
			// Keep keys like E (inventory) from closing the screen while typing
			this.nameBox.keyPressed(event);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void containerTick() {
		super.containerTick();
		// The route may have been deleted meanwhile
		if (this.nameBox != null && this.renaming >= this.menu.getRoutes().size()) {
			this.finishRename(false);
		}
	}
}
