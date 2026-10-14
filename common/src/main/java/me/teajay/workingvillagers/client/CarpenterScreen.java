package me.teajay.workingvillagers.client;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.ArrayList;
import java.util.List;
import me.teajay.workingvillagers.menu.CarpenterMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The carpenter's bench screen: a scrollable grid with every recipe of the bench (the ones
 * the player lacks the ingredients for are greyed out), the result slot and below it the
 * box that shows the order of the workstation; clicking it opens the overview, where the
 * order is set.
 */
public class CarpenterScreen extends AbstractContainerScreen<CarpenterMenu> {
	private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/scroller");
	private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/scroller_disabled");
	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
	private static final int COLUMNS = 7;
	private static final int ROWS = 4;
	private static final int CELL = 18;
	private static final int GRID_X = 8;
	private static final int GRID_Y = 18;
	private static final int SCROLLER_X = GRID_X + COLUMNS * CELL + 3;
	private static final int SCROLLER_HEIGHT = ROWS * CELL;
	/** Position of the order item, below the result slot. */
	private static final int ORDER_X = CarpenterMenu.RESULT_X;
	private static final int ORDER_Y = 63;
	/** The button to the overview of the workstation, top right. */
	private static final int STATION_X = 157;
	private static final int STATION_Y = 3;
	private static final int STATION_SIZE = 12;
	private float scrollOffs;
	private boolean scrolling;
	private int startRow;

	public CarpenterScreen(CarpenterMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, CarpenterMenu.INVENTORY_Y + 82);
		this.titleLabelY--;
		this.inventoryLabelY = CarpenterMenu.INVENTORY_Y - 11;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int left = this.leftPos;
		int top = this.topPos;
		GuiPanels.panel(graphics, left, top, this.imageWidth, this.imageHeight);
		for (Slot slot : this.menu.slots) {
			if (slot.isActive()) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, left + slot.x - 1, top + slot.y - 1, 18, 18);
			}
		}

		this.extractRecipes(graphics, mouseX, mouseY);

		int scrollerY = top + GRID_Y + (int) ((SCROLLER_HEIGHT - 15) * this.scrollOffs);
		graphics.fill(left + SCROLLER_X - 1, top + GRID_Y - 1, left + SCROLLER_X + 13, top + GRID_Y + SCROLLER_HEIGHT + 1, 0xFF8B8B8B);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.isScrollBarActive() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE,
			left + SCROLLER_X, scrollerY, 12, 15);
		if (GuiPanels.isOver(mouseX, mouseY, left + SCROLLER_X, top + GRID_Y, 12, SCROLLER_HEIGHT)) {
			graphics.requestCursor(this.isScrollBarActive()
				? (this.scrolling ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND)
				: CursorTypes.NOT_ALLOWED);
		}

		int orderX = left + ORDER_X;
		int orderY = top + ORDER_Y;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, orderX - 1, orderY - 1, 18, 18);
		ItemStack order = this.menu.getOrder();
		if (!order.isEmpty()) {
			graphics.item(order, orderX, orderY);
		}
		if (this.isOverOrder(mouseX, mouseY)) {
			graphics.fill(orderX, orderY, orderX + 16, orderY + 16, 0x80FFFFFF);
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}

		boolean overStation = this.isOverStation(mouseX, mouseY);
		GuiPanels.button(graphics, this.font, left + STATION_X, top + STATION_Y, STATION_SIZE, "i", overStation);
		if (overStation) {
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}
	}

	private void extractRecipes(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		List<ItemStack> options = this.menu.getOptions();
		for (int cell = 0; cell < COLUMNS * ROWS; cell++) {
			int index = this.startRow * COLUMNS + cell;
			int x = this.leftPos + GRID_X + cell % COLUMNS * CELL;
			int y = this.topPos + GRID_Y + cell / COLUMNS * CELL;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, y, CELL, CELL);
			if (index >= options.size()) {
				continue;
			}
			ItemStack option = options.get(index);
			boolean hovered = GuiPanels.isOver(mouseX, mouseY, x, y, CELL, CELL);
			if (index == this.menu.getSelectedRecipeIndex()) {
				graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF7FB069);
			}
			graphics.item(option, x + 1, y + 1);
			graphics.itemDecorations(this.font, option, x + 1, y + 1);
			if (!CarpenterMenu.isCraftable(option)) {
				graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xA08B8B8B);
			}
			if (hovered) {
				graphics.fill(x + 1, y + 1, x + 17, y + 17, 0x80FFFFFF);
				graphics.requestCursor(CursorTypes.POINTING_HAND);
			}
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
		graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, GuiPanels.TEXT_COLOR, false);
		graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, GuiPanels.TEXT_COLOR, false);
		Component worker = this.menu.getWorker()
			.map(name -> (Component) Component.translatable("gui.workingvillagers.worker", name))
			.orElse(Component.translatable("gui.workingvillagers.worker.none"));
		int room = STATION_X - 4 - (this.titleLabelX + this.font.width(this.title) + 6);
		String text = this.font.plainSubstrByWidth(worker.getString(), Math.max(0, room));
		graphics.text(this.font, text, STATION_X - 4 - this.font.width(text), this.titleLabelY, GuiPanels.MUTED_COLOR, false);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		if (this.isOverOrder(mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(this.orderTooltip(), mouseX, mouseY);
		}
		if (this.isOverStation(mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(this.font, Component.translatable("gui.workingvillagers.station.open"), mouseX, mouseY);
		}
		int index = this.optionAt(mouseX, mouseY);
		if (index >= 0) {
			graphics.setTooltipForNextFrame(this.font, this.menu.getOptions().get(index), mouseX, mouseY);
		}
	}

	private List<FormattedCharSequence> orderTooltip() {
		List<Component> lines = new ArrayList<>();
		ItemStack order = this.menu.getOrder();
		lines.add(order.isEmpty()
			? Component.translatable("gui.workingvillagers.order.none")
			: Component.translatable("gui.workingvillagers.order", order.getHoverName()));
		lines.add(Component.translatable("gui.workingvillagers.order.change").withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("gui.workingvillagers.order.chests").withStyle(ChatFormatting.DARK_GRAY));
		List<FormattedCharSequence> result = new ArrayList<>();
		for (Component line : lines) {
			result.addAll(this.font.split(line, 200));
		}
		return result;
	}

	/** The index of the recipe under the mouse, -1 if none. */
	private int optionAt(double mouseX, double mouseY) {
		if (!GuiPanels.isOver(mouseX, mouseY, this.leftPos + GRID_X, this.topPos + GRID_Y, COLUMNS * CELL, ROWS * CELL)) {
			return -1;
		}
		int column = (int) (mouseX - this.leftPos - GRID_X) / CELL;
		int row = (int) (mouseY - this.topPos - GRID_Y) / CELL;
		int index = (this.startRow + row) * COLUMNS + column;
		return index < this.menu.getOptions().size() ? index : -1;
	}

	private boolean isOverStation(double mouseX, double mouseY) {
		return GuiPanels.isOver(mouseX, mouseY, this.leftPos + STATION_X, this.topPos + STATION_Y, STATION_SIZE, STATION_SIZE);
	}

	private boolean isOverOrder(double mouseX, double mouseY) {
		return GuiPanels.isOver(mouseX, mouseY, this.leftPos + ORDER_X - 1, this.topPos + ORDER_Y - 1, 18, 18);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		// The order box leads to the overview too, where the order is set
		if (this.isOverStation(event.x(), event.y()) || this.isOverOrder(event.x(), event.y())) {
			this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, CarpenterMenu.STATION_BUTTON);
			return true;
		}
		int index = this.optionAt(event.x(), event.y());
		if (index >= 0) {
			if (this.menu.clickMenuButton(this.minecraft.player, index)) {
				this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
				this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, index);
			}
			return true;
		}
		if (this.isScrollBarActive() && GuiPanels.isOver(event.x(), event.y(), this.leftPos + SCROLLER_X, this.topPos + GRID_Y, 12, SCROLLER_HEIGHT)) {
			this.scrolling = true;
			this.scrollTo(event.y());
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (this.scrolling && this.isScrollBarActive()) {
			this.scrollTo(event.y());
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	private void scrollTo(double mouseY) {
		float top = this.topPos + GRID_Y;
		this.scrollOffs = Mth.clamp(((float) mouseY - top - 7.5F) / (SCROLLER_HEIGHT - 15.0F), 0.0F, 1.0F);
		this.startRow = (int) (this.scrollOffs * this.getOffscreenRows() + 0.5);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		this.scrolling = false;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (super.mouseScrolled(x, y, scrollX, scrollY)) {
			return true;
		}
		if (this.isScrollBarActive()) {
			int offscreenRows = this.getOffscreenRows();
			this.scrollOffs = Mth.clamp(this.scrollOffs - (float) scrollY / offscreenRows, 0.0F, 1.0F);
			this.startRow = (int) (this.scrollOffs * offscreenRows + 0.5);
		}
		return true;
	}

	private boolean isScrollBarActive() {
		return this.getOffscreenRows() > 0;
	}

	private int getOffscreenRows() {
		return (this.menu.getOptions().size() + COLUMNS - 1) / COLUMNS - ROWS;
	}
}
