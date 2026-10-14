package me.teajay.workingvillagers.client;

import java.util.ArrayList;
import java.util.List;
import me.teajay.workingvillagers.menu.PersonalChestInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * The line about a worker's personal chest in the station and carrier screens: where it is
 * and how many emeralds are left, a button to pick it, and the details in the tooltip.
 */
final class PersonalChestRow {
	static final int BUTTON_HEIGHT = 11;

	private PersonalChestRow() {
	}

	static Component line(PersonalChestInfo info) {
		if (info.pos().isEmpty()) {
			return Component.translatable("gui.workingvillagers.personal_chest.none");
		}
		Component where = Component.literal(GuiPanels.coordinates(info.pos().get()));
		return info.emeralds() < 0
			? Component.translatable("gui.workingvillagers.personal_chest", where)
			: Component.translatable("gui.workingvillagers.personal_chest.with_wage", info.emeralds());
	}

	/** Red when the worker cannot work because of the chest. */
	static int color(PersonalChestInfo info) {
		boolean problem = info.pos().isEmpty() ? info.wage() > 0 || info.needsTools() : !info.paid() && info.emeralds() < info.wage();
		return problem ? GuiPanels.WARNING_COLOR : GuiPanels.TEXT_COLOR;
	}

	static Component buttonLabel(PersonalChestInfo info) {
		return Component.translatable(info.pos().isEmpty() ? "gui.workingvillagers.personal_chest.pick" : "gui.workingvillagers.personal_chest.change");
	}

	static List<Component> buttonTooltip() {
		return List.of(
			Component.translatable("gui.workingvillagers.personal_chest.pick.tooltip"),
			Component.translatable("gui.workingvillagers.personal_chest.pick.tooltip2").withStyle(ChatFormatting.GRAY)
		);
	}

	static List<Component> tooltip(PersonalChestInfo info) {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("gui.workingvillagers.personal_chest.title"));
		boolean free = info.wage() == 0;
		lines.add((free
			? Component.translatable("gui.workingvillagers.personal_chest.explain.free")
			: Component.translatable("gui.workingvillagers.personal_chest.explain", info.wage())).withStyle(ChatFormatting.GRAY));
		if (info.pos().isEmpty()) {
			if (!free || info.needsTools()) {
				lines.add(Component.translatable("gui.workingvillagers.personal_chest.none.tooltip").withStyle(ChatFormatting.RED));
			}
			return lines;
		}
		lines.add(Component.literal(GuiPanels.coordinates(info.pos().get())).withStyle(ChatFormatting.GRAY));
		if (!free) {
			lines.add(info.paid()
				? Component.translatable("gui.workingvillagers.personal_chest.paid").withStyle(ChatFormatting.DARK_GREEN)
				: Component.translatable("gui.workingvillagers.personal_chest.unpaid").withStyle(ChatFormatting.GOLD));
			lines.add(info.emeralds() < 0
				? Component.translatable("gui.workingvillagers.personal_chest.not_loaded").withStyle(ChatFormatting.GRAY)
				: Component.translatable("gui.workingvillagers.personal_chest.days", info.emeralds() / info.wage())
					.withStyle(info.emeralds() < info.wage() ? ChatFormatting.RED : ChatFormatting.GRAY));
		}
		if (info.needsTools()) {
			lines.add(info.axe() < 0
				? Component.translatable("gui.workingvillagers.personal_chest.no_axe").withStyle(ChatFormatting.RED)
				: Component.translatable("gui.workingvillagers.personal_chest.axe", info.axe()).withStyle(ChatFormatting.GRAY));
			lines.add(Component.translatable("gui.workingvillagers.personal_chest.saplings", info.saplings())
				.withStyle(info.saplings() == 0 ? ChatFormatting.GOLD : ChatFormatting.GRAY));
		}
		return lines;
	}
}
