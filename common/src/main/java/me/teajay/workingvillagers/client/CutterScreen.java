package me.teajay.workingvillagers.client;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.ArrayList;
import java.util.List;
import me.teajay.workingvillagers.menu.CutterMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.item.ItemStack;

/**
 * The woodcutter and stonecutter screen: the vanilla stonecutter screen, drawing the recipe
 * buttons from the options the server put into the menu, plus a box below the result slot
 * that shows the order of the workstation. The order is set in the overview, which a click
 * on the box opens on its order tab. The tabs of the station stand above the screen (see
 * {@link StationTabs}).
 */
public class CutterScreen extends AbstractContainerScreen<CutterMenu> {
	private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/scroller");
	private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/scroller_disabled");
	private static final Identifier RECIPE_SELECTED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe_selected");
	private static final Identifier RECIPE_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe_highlighted");
	private static final Identifier RECIPE_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe");
	private static final Identifier BG_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/stonecutter.png");
	private static final int RECIPES_COLUMNS = 4;
	private static final int RECIPES_ROWS = 3;
	private static final int RECIPES_PER_PAGE = RECIPES_COLUMNS * RECIPES_ROWS;
	private static final int RECIPES_X = 52;
	private static final int RECIPES_Y = 14;
	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
	/** Position of the order item, below the result slot. */
	private static final int ORDER_X = 143;
	private static final int ORDER_Y = 59;
	private float scrollOffs;
	private boolean scrolling;
	private int startIndex;

	public CutterScreen(CutterMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		menu.registerUpdateListener(this::containerChanged);
		this.titleLabelY--;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int xo = this.leftPos;
		int yo = this.topPos;
		graphics.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
		int sy = (int) (41.0F * this.scrollOffs);
		Identifier sprite = this.isScrollBarActive() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
		int scrollerXStart = xo + 119;
		int scrollerYStart = yo + 15;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, scrollerXStart, scrollerYStart + sy, 12, 15);
		if (mouseX >= scrollerXStart && mouseY >= scrollerYStart && mouseX < scrollerXStart + 12 && mouseY < scrollerYStart + 54) {
			graphics.requestCursor(this.isScrollBarActive()
				? (this.scrolling ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND)
				: CursorTypes.NOT_ALLOWED);
		}

		if (this.menu.hasOrder()) {
			int orderX = xo + ORDER_X;
			int orderY = yo + ORDER_Y;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, orderX - 1, orderY - 1, 18, 18);
			OrderBox.extract(graphics, this.font, this.menu.getOrders(), orderX, orderY);
			if (this.isOverOrder(mouseX, mouseY)) {
				graphics.fill(orderX, orderY, orderX + 16, orderY + 16, 0x80FFFFFF);
				graphics.requestCursor(CursorTypes.POINTING_HAND);
			}
		}

		StationTabs.extractAbove(graphics, xo, yo, mouseX, mouseY, this.menu.stationType(), this.menu.workingvillagers$tabFlags());

		if (this.menu.hasInputItem()) {
			List<ItemStack> options = this.menu.getVisibleOptions();
			int x = this.leftPos + RECIPES_X;
			int y = this.topPos + RECIPES_Y;
			int endIndex = Math.min(this.startIndex + RECIPES_PER_PAGE, options.size());
			this.extractButtons(graphics, mouseX, mouseY, x, y, endIndex);
			this.extractRecipes(graphics, options, x, y, endIndex);
		}
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		if (this.isOverOrder(mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(this.orderTooltip(), mouseX, mouseY);
		}
		StationTabs.tooltip(graphics, this.font, mouseX, mouseY, this.leftPos, this.topPos, this.menu.stationType(), this.menu.workingvillagers$tabFlags());
		if (!this.menu.hasInputItem()) {
			return;
		}
		List<ItemStack> options = this.menu.getVisibleOptions();
		int edgeLeft = this.leftPos + RECIPES_X;
		int edgeTop = this.topPos + RECIPES_Y;
		int endIndex = Math.min(this.startIndex + RECIPES_PER_PAGE, options.size());
		for (int index = this.startIndex; index < endIndex; index++) {
			int posIndex = index - this.startIndex;
			int itemLeft = edgeLeft + posIndex % RECIPES_COLUMNS * 16;
			int itemTop = edgeTop + posIndex / RECIPES_COLUMNS * 18 + 2;
			if (mouseX >= itemLeft && mouseX < itemLeft + 16 && mouseY >= itemTop && mouseY < itemTop + 18) {
				graphics.setTooltipForNextFrame(this.font, options.get(index), mouseX, mouseY);
			}
		}
	}

	private void extractButtons(GuiGraphicsExtractor graphics, int xm, int ym, int x, int y, int endIndex) {
		for (int index = this.startIndex; index < endIndex; index++) {
			int posIndex = index - this.startIndex;
			int posX = x + posIndex % RECIPES_COLUMNS * 16;
			int posY = y + posIndex / RECIPES_COLUMNS * 18 + 2;
			Identifier sprite;
			if (index == this.menu.getSelectedRecipeIndex()) {
				sprite = RECIPE_SELECTED_SPRITE;
			} else if (xm >= posX && ym >= posY && xm < posX + 16 && ym < posY + 18) {
				sprite = RECIPE_HIGHLIGHTED_SPRITE;
			} else {
				sprite = RECIPE_SPRITE;
			}
			int textureY = posY - 1;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, posX, textureY, 16, 18);
			if (xm >= posX && ym >= textureY && xm < posX + 16 && ym < textureY + 18) {
				graphics.requestCursor(CursorTypes.POINTING_HAND);
			}
		}
	}

	private void extractRecipes(GuiGraphicsExtractor graphics, List<ItemStack> options, int x, int y, int endIndex) {
		for (int index = this.startIndex; index < endIndex; index++) {
			int posIndex = index - this.startIndex;
			int posX = x + posIndex % RECIPES_COLUMNS * 16;
			int posY = y + posIndex / RECIPES_COLUMNS * 18 + 2;
			ItemStack option = options.get(index);
			graphics.item(option, posX, posY);
			graphics.itemDecorations(this.font, option, posX, posY);
		}
	}

	private boolean isOverOrder(double mouseX, double mouseY) {
		if (!this.menu.hasOrder()) {
			return false;
		}
		int x = this.leftPos + ORDER_X;
		int y = this.topPos + ORDER_Y;
		return mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17;
	}

	private List<FormattedCharSequence> orderTooltip() {
		List<Component> lines = new ArrayList<>(OrderBox.describe(this.menu.getOrders()));
		lines.add(Component.translatable("gui.workingvillagers.order.change").withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("gui.workingvillagers.order.chests").withStyle(ChatFormatting.DARK_GRAY));
		List<FormattedCharSequence> result = new ArrayList<>();
		for (Component line : lines) {
			result.addAll(this.font.split(line, 200));
		}
		return result;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (StationTabs.click(this.minecraft, this.menu.containerId, CutterMenu.STATION_BUTTON, event.x(), event.y(), this.leftPos, this.topPos,
			this.menu.stationType(), this.menu.workingvillagers$tabFlags())) {
			return true;
		}
		// The order box leads to the order tab of the overview, where the order is set
		if (this.isOverOrder(event.x(), event.y())) {
			StationTabs.open(this.minecraft, this.menu.containerId, CutterMenu.STATION_BUTTON, StationTabs.Tab.ORDER);
			return true;
		}
		if (this.menu.hasInputItem()) {
			int xo = this.leftPos + RECIPES_X;
			int yo = this.topPos + RECIPES_Y;
			int endIndex = this.startIndex + RECIPES_PER_PAGE;
			for (int index = this.startIndex; index < endIndex; index++) {
				int posIndex = index - this.startIndex;
				double xx = event.x() - (xo + posIndex % RECIPES_COLUMNS * 16);
				double yy = event.y() - (yo + posIndex / RECIPES_COLUMNS * 18);
				if (xx >= 0.0 && yy >= 0.0 && xx < 16.0 && yy < 18.0 && this.menu.clickMenuButton(this.minecraft.player, index)) {
					Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
					this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, index);
					return true;
				}
			}

			xo = this.leftPos + 119;
			yo = this.topPos + 9;
			if (event.x() >= xo && event.x() < xo + 12 && event.y() >= yo && event.y() < yo + 54) {
				this.scrolling = true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (this.scrolling && this.isScrollBarActive()) {
			int yscr = this.topPos + RECIPES_Y;
			int yscr2 = yscr + 54;
			this.scrollOffs = ((float) event.y() - yscr - 7.5F) / (yscr2 - yscr - 15.0F);
			this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
			this.startIndex = (int) (this.scrollOffs * this.getOffscreenRows() + 0.5) * RECIPES_COLUMNS;
			return true;
		}
		return super.mouseDragged(event, dx, dy);
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
			float scrolledDelta = (float) scrollY / offscreenRows;
			this.scrollOffs = Mth.clamp(this.scrollOffs - scrolledDelta, 0.0F, 1.0F);
			this.startIndex = (int) (this.scrollOffs * offscreenRows + 0.5) * RECIPES_COLUMNS;
		}
		return true;
	}

	private boolean isScrollBarActive() {
		return this.menu.hasInputItem() && this.menu.getNumberOfVisibleRecipes() > RECIPES_PER_PAGE;
	}

	private int getOffscreenRows() {
		return (this.menu.getNumberOfVisibleRecipes() + RECIPES_COLUMNS - 1) / RECIPES_COLUMNS - RECIPES_ROWS;
	}

	private void containerChanged() {
		this.scrollOffs = 0.0F;
		this.startIndex = 0;
	}
}
