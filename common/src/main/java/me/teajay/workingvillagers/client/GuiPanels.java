package me.teajay.workingvillagers.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

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

	public static boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	public static String coordinates(BlockPos pos) {
		return pos.getX() + " " + pos.getY() + " " + pos.getZ();
	}
}
