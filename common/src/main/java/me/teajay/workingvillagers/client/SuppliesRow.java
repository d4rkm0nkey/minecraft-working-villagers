package me.teajay.workingvillagers.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
 * slots with its emeralds, its tools (with the durability bar) and what it uses up, like an
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
		return (1 + info.tools().size() + info.stocks().size()) * SLOT;
	}

	/** Draws the slots with their top left corner at x, y. */
	static void extractSlots(GuiGraphicsExtractor graphics, Font font, SuppliesInfo info, int x, int y) {
		boolean unpaid = !info.paid() && info.emeralds() < info.wage();
		x = slot(graphics, font, x, y, info.emeralds() > 0 ? new ItemStack(Items.EMERALD, info.emeralds()) : ItemStack.EMPTY,
			new ItemStack(Items.EMERALD), unpaid);
		for (SuppliesInfo.Slot slot : info.tools()) {
			x = slot(graphics, font, x, y, slot.stack(), placeholder(slot.kind()), slot.missing());
		}
		for (SuppliesInfo.Slot slot : info.stocks()) {
			x = slot(graphics, font, x, y, slot.stack(), placeholder(slot.kind()), slot.missing());
		}
	}

	/** What an empty slot shows, faded. */
	private static ItemStack placeholder(String kind) {
		return new ItemStack(switch (kind) {
			case "axe" -> Items.IRON_AXE;
			case "hoe" -> Items.IRON_HOE;
			case "shovel" -> Items.IRON_SHOVEL;
			case "pickaxe" -> Items.IRON_PICKAXE;
			case "saplings" -> Items.OAK_SAPLING;
			case "seeds" -> Items.WHEAT_SEEDS;
			case "ladders" -> Items.LADDER;
			case "torches" -> Items.TORCH;
			case "shears" -> Items.SHEARS;
			case "wheat" -> Items.WHEAT;
			case "fences" -> Items.OAK_FENCE;
			case "planks" -> Items.OAK_PLANKS;
			case "seal" -> Items.COBBLESTONE;
			default -> Items.BARRIER;
		});
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
		Optional<SuppliesInfo.Slot> missing = info.firstMissing();
		if (missing.isPresent()) {
			return Component.translatable("gui.workingvillagers.supplies.no_" + missing.get().kind());
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
		for (SuppliesInfo.Slot tool : info.tools()) {
			lines.add(tool.stack().isEmpty()
				? Component.translatable("gui.workingvillagers.supplies.no_" + tool.kind()).withStyle(ChatFormatting.RED)
				: Component.translatable("gui.workingvillagers.supplies." + tool.kind(),
					100 - tool.stack().getDamageValue() * 100 / Math.max(1, tool.stack().getMaxDamage())).withStyle(ChatFormatting.GRAY));
		}
		if (info.worn() > 0) {
			lines.add(Component.translatable("gui.workingvillagers.supplies.worn", info.worn()).withStyle(ChatFormatting.GOLD));
		}
		for (SuppliesInfo.Slot stock : info.stocks()) {
			lines.add(Component.translatable("gui.workingvillagers.supplies." + stock.kind(), stock.stack().getCount())
				.withStyle(stock.missing() ? ChatFormatting.RED : stock.stack().isEmpty() ? ChatFormatting.GOLD : ChatFormatting.GRAY));
		}
		lines.add(info.villageChests() > 0
			? Component.translatable("gui.workingvillagers.supplies.village_chests", info.villageChests()).withStyle(ChatFormatting.DARK_GREEN)
			: Component.translatable("gui.workingvillagers.supplies.no_village_chest").withStyle(ChatFormatting.DARK_GRAY));
		lines.add(Component.translatable("gui.workingvillagers.supplies.give").withStyle(ChatFormatting.DARK_GRAY));
		return lines;
	}
}
