package me.teajay.workingvillagers.client;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.teajay.workingvillagers.WorkingVillagers;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * The carrier screen, as big as a chest, with two tabs above it:
 * <ul>
 * <li><b>Routes</b>: one row per route (name, source, target) with buttons to rename and
 *     delete it, the first free row as a button that starts picking a new route, below them
 *     who may change the routes.</li>
 * <li><b>Basket</b>: what the carrier is doing, the basket, its wage and the player's
 *     inventory.</li>
 * </ul>
 */
public class CarrierScreen extends AbstractContainerScreen<CarrierMenu> {
	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
	private static final int WIDTH = 176;
	private static final int HEIGHT = 166;
	private static final int ROWS_X = 7;
	private static final int ROWS_Y = 18;
	private static final int ROW_WIDTH = 162;
	private static final int ROW_HEIGHT = 30;
	private static final int BUTTON_SIZE = 14;
	private static final int TEXT_X = 15;
	private static final int HINT_Y = ROWS_Y + Route.MAX_ROUTES * ROW_HEIGHT + 4;
	private static final int SHARE_Y = 140;
	private static final int SHARE_HEIGHT = 18;
	private static final int STATUS_Y = 17;
	private static final int SUPPLIES_Y = 54;

	private enum Tab { ROUTES, BASKET }

	private Tab tab = Tab.ROUTES;
	/** The text field while a route is renamed, null otherwise. */
	private @Nullable EditBox nameBox;
	private int renaming = -1;

	public CarrierScreen(CarrierMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, WIDTH, HEIGHT);
		this.inventoryLabelY = CarrierMenu.INVENTORY_Y - 10;
	}

	@Override
	protected void init() {
		super.init();
		// Centered with the tabs, which stick out above the window
		this.topPos = (this.height - HEIGHT - GuiTabs.TOP_HEIGHT) / 2 + GuiTabs.TOP_HEIGHT;
		// The widgets are rebuilt on resize, the text field with them
		this.nameBox = null;
		this.renaming = -1;
	}

	/** Shows a tab (also for the screenshots of the menus). */
	public void showTab(String name) {
		for (Tab candidate : Tab.values()) {
			if (candidate.name().equalsIgnoreCase(name)) {
				this.select(candidate);
			}
		}
	}

	private void select(Tab tab) {
		this.finishRename(true);
		this.tab = tab;
	}

	private int rowX() {
		return this.leftPos + ROWS_X;
	}

	private int rowY(int index) {
		return this.topPos + ROWS_Y + index * ROW_HEIGHT;
	}

	private int deleteX() {
		return this.rowX() + ROW_WIDTH - BUTTON_SIZE - 2;
	}

	private int renameX() {
		return this.deleteX() - BUTTON_SIZE - 1;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		this.menu.showSlots(this.tab == Tab.BASKET);
		super.extractBackground(graphics, mouseX, mouseY, a);
		// The other tab behind the window, the open one in front of it
		for (Tab shown : Tab.values()) {
			if (shown != this.tab) {
				this.extractTab(graphics, shown, false, mouseX, mouseY);
			}
		}
		GuiPanels.panel(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
		this.extractTab(graphics, this.tab, true, mouseX, mouseY);
		if (this.tab == Tab.ROUTES) {
			this.extractRoutes(graphics, mouseX, mouseY);
		} else {
			this.extractBasket(graphics);
		}
	}

	// --- Tabs ---

	private void extractTab(GuiGraphicsExtractor graphics, Tab shown, boolean selected, int mouseX, int mouseY) {
		int index = shown.ordinal();
		int x = GuiTabs.topX(this.leftPos, index);
		GuiTabs.top(graphics, this.leftPos, this.topPos, index, selected);
		graphics.item(shown == Tab.ROUTES ? new ItemStack(Items.MAP) : new ItemStack(WorkingVillagers.WICKER_BASKET.get()),
			x + 5, GuiTabs.topIconY(this.topPos, selected));
		if (this.tabProblem(shown)) {
			GuiTabs.problem(graphics, x + GuiTabs.TOP_WIDTH, this.topPos - GuiTabs.TOP_HEIGHT);
		}
		if (!selected && this.isOverTab(mouseX, mouseY, shown)) {
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}
	}

	private boolean isOverTab(double mouseX, double mouseY, Tab shown) {
		return GuiPanels.isOver(mouseX, mouseY, GuiTabs.topX(this.leftPos, shown.ordinal()), this.topPos - GuiTabs.TOP_HEIGHT,
			GuiTabs.TOP_WIDTH, GuiTabs.TOP_HEIGHT);
	}

	/** Routes: none yet. Basket: the carrier lacks its wage. */
	private boolean tabProblem(Tab shown) {
		return shown == Tab.ROUTES
			? this.menu.getRoutes().isEmpty()
			: this.menu.getSupplies().map(SuppliesInfo::hasProblem).orElse(false);
	}

	private List<Component> tabTooltip(Tab shown) {
		if (shown == Tab.ROUTES) {
			return List.of(Component.translatable("gui.workingvillagers.tab.routes"),
				Component.translatable("gui.workingvillagers.carrier.routes", this.menu.getRoutes().size(), Route.MAX_ROUTES).withStyle(ChatFormatting.GRAY));
		}
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("gui.workingvillagers.tab.basket"));
		this.menu.getSupplies().ifPresent(info -> lines.add(SuppliesRow.line(info).copy().withStyle(info.hasProblem() ? ChatFormatting.RED : ChatFormatting.GRAY)));
		return lines;
	}

	// --- Routes tab ---

	private void extractRoutes(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
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
			// Next to the buttons the title and the source, the target below them
			int besideButtons = (canEdit ? this.renameX() - 3 : x + ROW_WIDTH - 3) - (x + TEXT_X);
			if (i != this.renaming) {
				GuiPanels.scrollingText(graphics, this.font, route.title(i).getString(), x + TEXT_X, y + 3, besideButtons, 0xFF202020);
			}
			graphics.text(this.font, String.valueOf(i + 1), x + 5, y + 10, GuiPanels.MUTED_COLOR, false);
			this.line(graphics, Component.translatable("gui.workingvillagers.carrier.from", this.menu.getContainerName(i, false)), x + TEXT_X, y + 12, besideButtons);
			this.line(graphics, Component.translatable("gui.workingvillagers.carrier.to", this.menu.getContainerName(i, true)), x + TEXT_X, y + 20, ROW_WIDTH - TEXT_X - 3);
			if (canEdit && i != this.renaming) {
				this.smallButton(graphics, mouseX, mouseY, this.renameX(), y + 1, "✎");
				this.smallButton(graphics, mouseX, mouseY, this.deleteX(), y + 1, "×");
			}
		}

		if (this.menu.canChangeSharing()) {
			boolean hovered = this.isOverShare(mouseX, mouseY);
			GuiPanels.button(graphics, this.font, this.rowX(), this.topPos + SHARE_Y, ROW_WIDTH, SHARE_HEIGHT, this.shareLabel().getString(), hovered);
			if (hovered) {
				graphics.requestCursor(CursorTypes.POINTING_HAND);
			}
		}
	}

	private void extractRoutesLabels(GuiGraphicsExtractor graphics) {
		boolean canEdit = this.menu.canEdit();
		if (canEdit && this.menu.getRoutes().size() >= Route.MAX_ROUTES) {
			return;
		}
		Component hint = Component.translatable(canEdit ? "gui.workingvillagers.carrier.hint" : "gui.workingvillagers.carrier.hint_not_yours");
		int y = HINT_Y;
		for (FormattedCharSequence line : this.font.split(hint, ROW_WIDTH - 2)) {
			graphics.text(this.font, line, ROWS_X + 1, y, GuiPanels.MUTED_COLOR, false);
			y += 9;
		}
		if (!this.menu.canChangeSharing() && this.menu.getSupplies().isPresent()) {
			GuiPanels.scrollingText(graphics, this.font, this.shareLabel().getString(), ROWS_X + 1, SHARE_Y + 5, ROW_WIDTH - 2, GuiPanels.MUTED_COLOR);
		}
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
		return Component.translatable("gui.workingvillagers.carrier.share.label", Component.translatable(this.menu.isShared()
			? "gui.workingvillagers.carrier.share.everyone"
			: "gui.workingvillagers.carrier.share.only_employer"));
	}

	private boolean isOverShare(double mouseX, double mouseY) {
		return this.tab == Tab.ROUTES && this.menu.canChangeSharing()
			&& GuiPanels.isOver(mouseX, mouseY, this.rowX(), this.topPos + SHARE_Y, ROW_WIDTH, SHARE_HEIGHT);
	}

	// --- Basket tab ---

	/** The slots of the basket and the inventory, like a container screen, and the carrier's wage. */
	private void extractBasket(GuiGraphicsExtractor graphics) {
		for (Slot slot : this.menu.slots) {
			if (slot.isActive()) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, this.leftPos + slot.x - 1, this.topPos + slot.y - 1, 18, 18);
			}
		}
		this.menu.getSupplies().ifPresent(info -> SuppliesRow.extractSlots(graphics, this.font, info, this.rowX(), this.topPos + SUPPLIES_Y));
	}

	private void extractBasketLabels(GuiGraphicsExtractor graphics) {
		this.menu.getStatus().ifPresent(status -> GuiPanels.scrollingText(graphics, this.font,
			Component.translatable("gui.workingvillagers.status", status).getString(), 8, STATUS_Y, ROW_WIDTH, GuiPanels.STATUS_COLOR));
		graphics.text(this.font, Component.translatable("gui.workingvillagers.carrier.basket"), 8, CarrierMenu.BASKET_Y + 5, GuiPanels.TEXT_COLOR, false);
		Optional<SuppliesInfo> supplies = this.menu.getSupplies();
		if (supplies.isPresent()) {
			// The carrier carries its emeralds itself: it has no workstation with a wage box
			SuppliesInfo info = supplies.get();
			int x = ROWS_X + SuppliesRow.slotsWidth(info) + 4;
			int width = ROWS_X + ROW_WIDTH - x;
			GuiPanels.scrollingText(graphics, this.font, SuppliesRow.line(info).getString(), x, SUPPLIES_Y + 1, width, SuppliesRow.color(info));
			GuiPanels.scrollingText(graphics, this.font, Component.translatable("gui.workingvillagers.carrier.give").getString(), x, SUPPLIES_Y + 10, width,
				GuiPanels.MUTED_COLOR);
		}
		graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, GuiPanels.TEXT_COLOR, false);
	}

	private boolean isOverSupplies(double mouseX, double mouseY) {
		return this.tab == Tab.BASKET && this.menu.getSupplies().isPresent()
			&& GuiPanels.isOver(mouseX, mouseY, this.rowX(), this.topPos + SUPPLIES_Y, ROW_WIDTH, SuppliesRow.SLOT);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
		GuiPanels.scrollingText(graphics, this.font, this.title.getString(), this.titleLabelX, this.titleLabelY, this.imageWidth - 16, GuiPanels.TEXT_COLOR);
		if (this.tab == Tab.ROUTES) {
			this.extractRoutesLabels(graphics);
		} else {
			this.extractBasketLabels(graphics);
		}
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		for (Tab shown : Tab.values()) {
			if (this.isOverTab(mouseX, mouseY, shown)) {
				graphics.setComponentTooltipForNextFrame(this.font, this.tabTooltip(shown), mouseX, mouseY);
				return;
			}
		}
		if (this.isOverShare(mouseX, mouseY)) {
			graphics.setComponentTooltipForNextFrame(this.font, List.of(
				Component.translatable("gui.workingvillagers.carrier.share.tooltip"),
				Component.translatable("gui.workingvillagers.carrier.share.click").withStyle(ChatFormatting.DARK_GRAY)
			), mouseX, mouseY);
			return;
		}
		if (this.isOverSupplies(mouseX, mouseY)) {
			graphics.setComponentTooltipForNextFrame(this.font, SuppliesRow.tooltip(this.menu.getSupplies().get()), mouseX, mouseY);
			return;
		}
		if (this.tab != Tab.ROUTES) {
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

	private boolean isOverButton(double mouseX, double mouseY, int x, int index) {
		return this.tab == Tab.ROUTES && this.menu.canEdit() && index != this.renaming
			&& GuiPanels.isOver(mouseX, mouseY, x, this.rowY(index) + 1, BUTTON_SIZE, BUTTON_SIZE);
	}

	/** Only the first free slot is a button: new routes are added at the end. */
	private boolean isOverFreeRow(double mouseX, double mouseY, int index) {
		return this.tab == Tab.ROUTES && this.menu.canEdit() && index == this.menu.getRoutes().size() && index < Route.MAX_ROUTES
			&& GuiPanels.isOver(mouseX, mouseY, this.rowX(), this.rowY(index), ROW_WIDTH, ROW_HEIGHT - 2);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.nameBox != null && !this.nameBox.isMouseOver(event.x(), event.y())) {
			this.finishRename(true);
		}
		for (Tab shown : Tab.values()) {
			if (shown != this.tab && this.isOverTab(event.x(), event.y(), shown)) {
				this.playClick();
				this.select(shown);
				return true;
			}
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

	@Override
	protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
		// Clicks on the tabs are no clicks outside the screen
		return super.hasClickedOutside(mouseX, mouseY, left, top)
			&& !GuiPanels.isOver(mouseX, mouseY, left, top - GuiTabs.TOP_HEIGHT, Tab.values().length * GuiTabs.TOP_STEP, GuiTabs.TOP_HEIGHT);
	}

	private void playClick() {
		this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	private void click(int button) {
		this.playClick();
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
