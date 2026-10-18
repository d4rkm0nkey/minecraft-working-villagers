package me.teajay.workingvillagers.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/**
 * Drawing helpers for the screens without a texture of their own: the gray vanilla panel,
 * sunken rows and small buttons.
 */
public final class GuiPanels {
	public static final int TEXT_COLOR = 0xFF404040;
	public static final int MUTED_COLOR = 0xFF707070;
	public static final int WARNING_COLOR = 0xFF8B2020;
	/** What a worker is doing right now. */
	public static final int STATUS_COLOR = 0xFF2B4F7A;
	private static final Identifier BUTTON = Identifier.withDefaultNamespace("widget/button");
	private static final Identifier BUTTON_HIGHLIGHTED = Identifier.withDefaultNamespace("widget/button_highlighted");
	private static final Identifier BUTTON_DISABLED = Identifier.withDefaultNamespace("widget/button_disabled");

	private GuiPanels() {
	}

	/** The raised gray panel of the vanilla container screens. */
	public static void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
		graphics.fill(x + 1, y, x + width - 1, y + height, 0xFF000000);
		graphics.fill(x, y + 1, x + width, y + height - 1, 0xFF000000);
		graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFC6C6C6);
		graphics.fill(x + 1, y + 1, x + width - 3, y + 3, 0xFFFFFFFF);
		graphics.fill(x + 1, y + 1, x + 3, y + height - 3, 0xFFFFFFFF);
		graphics.fill(x + 3, y + height - 3, x + width - 1, y + height - 1, 0xFF555555);
		graphics.fill(x + width - 3, y + 3, x + width - 1, y + height - 1, 0xFF555555);
	}

	/** A sunken area like a slot, for a row of a list. */
	public static void inset(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
		graphics.fill(x, y, x + width, y + height, 0xFF373737);
		graphics.fill(x + 1, y + 1, x + width, y + height, 0xFFFFFFFF);
		graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, color);
	}

	/** A small square button with a one-character label. */
	public static void button(GuiGraphicsExtractor graphics, Font font, int x, int y, int size, String label, boolean hovered) {
		button(graphics, font, x, y, size, size, label, hovered);
	}

	/** A small button with a text label. */
	public static void button(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height, String label, boolean hovered) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, hovered ? BUTTON_HIGHLIGHTED : BUTTON, x, y, width, height);
		graphics.centeredText(font, label, x + width / 2 + 1, y + (height - 8) / 2, hovered ? 0xFFFFFFA0 : 0xFFFFFFFF);
	}

	/** A button that cannot be clicked right now, grayed out like vanilla's. */
	public static void disabledButton(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height, String label) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BUTTON_DISABLED, x, y, width, height);
		graphics.centeredText(font, label, x + width / 2 + 1, y + (height - 8) / 2, 0xFFA0A0A0);
	}

	/**
	 * A line of text in a space of the given width. If it is too long, it scrolls slowly back
	 * and forth inside that space, like the labels of the vanilla buttons, instead of being cut
	 * off.
	 */
	public static void scrollingText(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int width, int color) {
		scrollingText(graphics, font, text, x, y, width, color, false);
	}

	/** Like {@link #scrollingText(GuiGraphicsExtractor, Font, String, int, int, int, int)}; {@code right}: a text that fits is aligned right. */
	public static void scrollingText(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int width, int color, boolean right) {
		int textWidth = font.width(text);
		if (textWidth <= width) {
			graphics.text(font, text, right ? x + width - textWidth : x, y, color, false);
			return;
		}
		if (width <= 0) {
			return;
		}
		int overflow = textWidth - width;
		double seconds = Util.getMillis() / 1000.0;
		double period = Math.max(overflow * 0.5, 3.0);
		double along = Math.sin(Math.PI / 2 * Math.cos(Math.PI * 2 * seconds / period)) / 2.0 + 0.5;
		graphics.enableScissor(x, y - 1, x + width, y + font.lineHeight);
		graphics.text(font, text, x - (int) Math.round(Mth.lerp(along, 0.0, overflow)), y, color, false);
		graphics.disableScissor();
	}

	public static boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
