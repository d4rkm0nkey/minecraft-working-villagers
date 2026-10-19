package me.teajay.workingvillagers.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * The vanilla tabs of the screens: above the window like the creative inventory, or to its
 * left like the advancements (for windows too high for tabs above them, like a double
 * chest). The selected tab is drawn after the window, the others before it.
 */
public final class GuiTabs {
	/** How far the tabs above the window stick out. */
	public static final int TOP_HEIGHT = 28;
	public static final int TOP_WIDTH = 26;
	public static final int TOP_STEP = 27;
	/** How far the tabs left of the window stick out. */
	public static final int SIDE_WIDTH = 28;
	public static final int SIDE_HEIGHT = 28;
	private static final Identifier[] TOP_SELECTED = topSprites("selected");
	private static final Identifier[] TOP_UNSELECTED = topSprites("unselected");

	private GuiTabs() {
	}

	private static Identifier[] topSprites(String state) {
		Identifier[] sprites = new Identifier[7];
		for (int i = 0; i < sprites.length; i++) {
			sprites[i] = Identifier.withDefaultNamespace("container/creative_inventory/tab_top_" + state + "_" + (i + 1));
		}
		return sprites;
	}

	/** The left edge of the tab with that index above the window. */
	public static int topX(int left, int index) {
		return left + index * TOP_STEP;
	}

	/** The top edge of the tab with that index left of the window. */
	public static int sideY(int top, int index) {
		return top + index * SIDE_HEIGHT;
	}

	/** Draws a tab above the window; the icon goes to (x + 5, {@link #topIconY}). */
	public static void top(GuiGraphicsExtractor graphics, int left, int top, int index, boolean selected) {
		Identifier[] sprites = selected ? TOP_SELECTED : TOP_UNSELECTED;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprites[Math.min(index, sprites.length - 1)], topX(left, index), top - TOP_HEIGHT, TOP_WIDTH, 32);
	}

	public static int topIconY(int top, boolean selected) {
		return top - TOP_HEIGHT + 9 + (selected ? 0 : 1);
	}

	/** Draws a tab left of the window; the icon goes to ({@link #sideIconX}, y + 6). */
	public static void side(GuiGraphicsExtractor graphics, int left, int top, int index, boolean selected) {
		String sprite = "advancements/tab_left_" + (index == 0 ? "top" : "middle") + (selected ? "_selected" : "");
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace(sprite), left - SIDE_WIDTH, sideY(top, index), 32, SIDE_HEIGHT);
	}

	public static int sideIconX(int left, boolean selected) {
		return left - SIDE_WIDTH + 8 + (selected ? 0 : 1);
	}

	/** A red mark at the top right of a tab, like a notification: something is missing there. */
	public static void problem(GuiGraphicsExtractor graphics, int right, int y) {
		graphics.fill(right - 8, y + 3, right - 3, y + 8, 0xFF000000);
		graphics.fill(right - 7, y + 4, right - 4, y + 7, 0xFFE03030);
	}
}
