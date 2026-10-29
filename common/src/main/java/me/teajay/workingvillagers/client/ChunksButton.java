package me.teajay.workingvillagers.client;

import java.util.ArrayList;
import java.util.List;
import me.teajay.workingvillagers.menu.ChunksInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The switch for working without a player nearby, in the station overview and the carrier's
 * menu: a square button with an ender pearl (its chunks travel with the worker like those of
 * a thrown pearl) and a mark at the top right: green while the chunks are loaded, yellow while
 * it is on but waits (employer offline), none while it is off.
 */
final class ChunksButton {
	private static final int ACTIVE_COLOR = 0xFF40C040;
	private static final int WAITING_COLOR = 0xFFE0C030;
	private static final int TOOLTIP_WIDTH = 200;

	private ChunksButton() {
	}

	static void extract(GuiGraphicsExtractor graphics, Font font, ChunksInfo info, int x, int y, int size, boolean hovered) {
		if (info.canSwitch()) {
			GuiPanels.button(graphics, font, x, y, size, size, "", hovered);
		} else {
			GuiPanels.disabledButton(graphics, font, x, y, size, size, "");
		}
		graphics.item(new ItemStack(Items.ENDER_PEARL), x + (size - 16) / 2, y + (size - 16) / 2);
		if (info.on()) {
			int right = x + size;
			graphics.fill(right - 7, y + 2, right - 2, y + 7, 0xFF000000);
			graphics.fill(right - 6, y + 3, right - 3, y + 6, info.chunks() > 0 ? ACTIVE_COLOR : WAITING_COLOR);
		}
	}

	static List<Component> tooltip(Font font, ChunksInfo info) {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("gui.workingvillagers.chunks.title",
			Component.translatable(info.on() ? "gui.workingvillagers.chunks.on" : "gui.workingvillagers.chunks.off")));
		if (info.on()) {
			lines.addAll(info.chunks() > 0
				? wrapped(font, Component.translatable("gui.workingvillagers.chunks.loaded", info.chunks()), ChatFormatting.DARK_GREEN)
				: wrapped(font, info.state().orElse(Component.empty()), ChatFormatting.GOLD));
		}
		lines.addAll(wrapped(font, Component.translatable("gui.workingvillagers.chunks.hint", info.fee()), ChatFormatting.GRAY));
		lines.add(Component.translatable(info.canSwitch() ? "gui.workingvillagers.station.click_toggle" : "gui.workingvillagers.chunks.only_employer")
			.withStyle(ChatFormatting.DARK_GRAY));
		return lines;
	}

	/** The text in lines of a tooltip's width, in the color. */
	private static List<Component> wrapped(Font font, Component text, ChatFormatting color) {
		List<Component> lines = new ArrayList<>();
		for (FormattedCharSequence line : font.split(text, TOOLTIP_WIDTH)) {
			StringBuilder plain = new StringBuilder();
			line.accept((index, style, codePoint) -> {
				plain.appendCodePoint(codePoint);
				return true;
			});
			lines.add(Component.literal(plain.toString()).withStyle(color));
		}
		return lines;
	}
}
