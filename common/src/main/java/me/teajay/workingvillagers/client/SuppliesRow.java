package me.teajay.workingvillagers.client;

import java.util.ArrayList;
import java.util.List;
import me.teajay.workingvillagers.menu.SuppliesInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The row about what a worker carries for its work in the station and carrier screens:
 * slots with its emeralds, its tool (with the durability bar) and what it plants, like an
 * inventory, and a short line next to them. Details in the tooltip.
 */
final class SuppliesRow {
	static final int SLOT = 18;
	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
	private static final int MISSING_COLOR = 0x60FF3030;

	private SuppliesRow() {
	}

	/** How wide the slots are. */
	static int slotsWidth(SuppliesInfo info) {
		int slots = 1 + (info.needsTool() ? 1 : 0) + (info.stockKind().isEmpty() ? 0 : 1);
		return slots * SLOT;
	}

	/** Draws the slots with their top left corner at x, y. */
	static void extractSlots(GuiGraphicsExtractor graphics, Font font, SuppliesInfo info, int x, int y) {
		boolean unpaid = !info.paid() && info.emeralds() < info.wage();
		x = slot(graphics, font, x, y, info.emeralds() > 0 ? new ItemStack(Items.EMERALD, info.emeralds()) : ItemStack.EMPTY,
			new ItemStack(Items.EMERALD), unpaid);
		if (info.needsTool()) {
			x = slot(graphics, font, x, y, info.tool(), new ItemStack("axe".equals(info.toolKind()) ? Items.IRON_AXE : Items.IRON_HOE), info.tool().isEmpty());
		}
		if (!info.stockKind().isEmpty()) {
			slot(graphics, font, x, y, info.stock(), new ItemStack("saplings".equals(info.stockKind()) ? Items.OAK_SAPLING : Items.WHEAT_SEEDS), false);
		}
	}

	/** A slot with the stack, or a faded placeholder if it is empty (red if that is a problem). */
	private static int slot(GuiGraphicsExtractor graphics, Font font, int x, int y, ItemStack stack, ItemStack placeholder, boolean missing) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, y, SLOT, SLOT);
		if (stack.isEmpty()) {
			graphics.fakeItem(placeholder, x + 1, y + 1);
			graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xA08B8B8B);
		} else {
			graphics.item(stack, x + 1, y + 1);
			graphics.itemDecorations(font, stack, x + 1, y + 1, stack.getCount() > 1 ? String.valueOf(stack.getCount()) : null);
		}
		if (missing) {
			graphics.fill(x + 1, y + 1, x + 17, y + 17, MISSING_COLOR);
		}
		return x + SLOT;
	}

	/** The short line next to the slots: what is missing, or for how long the wage lasts. */
	static Component line(SuppliesInfo info) {
		if (!info.paid() && info.emeralds() < info.wage()) {
			return Component.translatable("gui.workingvillagers.supplies.no_wage");
		}
		if (info.needsTool() && info.tool().isEmpty()) {
			return Component.translatable("gui.workingvillagers.supplies.no_" + info.toolKind());
		}
		if (info.wage() == 0) {
			return Component.translatable("gui.workingvillagers.supplies.free");
		}
		return Component.translatable("gui.workingvillagers.supplies.days", info.emeralds() / info.wage());
	}

	static int color(SuppliesInfo info) {
		return info.hasProblem() ? GuiPanels.WARNING_COLOR : GuiPanels.MUTED_COLOR;
	}

	static List<Component> tooltip(SuppliesInfo info) {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("gui.workingvillagers.supplies.title"));
		lines.add((info.wage() == 0
			? Component.translatable("gui.workingvillagers.supplies.explain.free")
			: Component.translatable("gui.workingvillagers.supplies.explain", info.wage())).withStyle(ChatFormatting.GRAY));
		if (info.wage() > 0) {
			lines.add(info.paid()
				? Component.translatable("gui.workingvillagers.supplies.paid").withStyle(ChatFormatting.DARK_GREEN)
				: Component.translatable("gui.workingvillagers.supplies.unpaid").withStyle(ChatFormatting.GOLD));
		}
		if (info.needsTool()) {
			lines.add(info.tool().isEmpty()
				? Component.translatable("gui.workingvillagers.supplies.no_" + info.toolKind()).withStyle(ChatFormatting.RED)
				: Component.translatable("gui.workingvillagers.supplies." + info.toolKind(),
					100 - info.tool().getDamageValue() * 100 / Math.max(1, info.tool().getMaxDamage())).withStyle(ChatFormatting.GRAY));
		}
		if (!info.stockKind().isEmpty()) {
			lines.add(Component.translatable("gui.workingvillagers.supplies." + info.stockKind(), info.stock().getCount())
				.withStyle(info.stock().isEmpty() ? ChatFormatting.GOLD : ChatFormatting.GRAY));
		}
		lines.add(Component.translatable("gui.workingvillagers.supplies.give").withStyle(ChatFormatting.DARK_GRAY));
		return lines;
	}
}
