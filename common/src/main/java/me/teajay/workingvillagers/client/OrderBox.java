package me.teajay.workingvillagers.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

/**
 * The box below the result slot of the carpenter's bench and the cutters that shows the order:
 * with several products one after another, a second each, with their number in the corner.
 */
final class OrderBox {
	private OrderBox() {
	}

	/** Draws the product shown right now at the position, nothing without an order. */
	static void extract(GuiGraphicsExtractor graphics, Font font, List<ItemStack> orders, int x, int y) {
		if (orders.isEmpty()) {
			return;
		}
		ItemStack shown = orders.get((int) (Util.getMillis() / 1000L % orders.size()));
		graphics.item(shown, x, y);
		if (orders.size() > 1) {
			graphics.itemDecorations(font, shown, x, y, "×" + orders.size());
		}
	}

	/** The first lines of the tooltip: the order, one line per product if there are several. */
	static List<Component> describe(List<ItemStack> orders) {
		List<Component> lines = new ArrayList<>();
		if (orders.isEmpty()) {
			lines.add(Component.translatable("gui.workingvillagers.order.none"));
		} else if (orders.size() == 1) {
			lines.add(Component.translatable("gui.workingvillagers.order", orders.getFirst().getHoverName()));
		} else {
			lines.add(Component.translatable("gui.workingvillagers.order.several", orders.size()));
			for (ItemStack order : orders) {
				lines.add(Component.literal("• ").append(order.getHoverName()).withStyle(ChatFormatting.GRAY));
			}
		}
		return lines;
	}
}
