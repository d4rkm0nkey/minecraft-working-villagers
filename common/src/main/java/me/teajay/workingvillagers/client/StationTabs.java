package me.teajay.workingvillagers.client;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.teajay.workingvillagers.menu.StationTabFlags;
import me.teajay.workingvillagers.station.StationType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * The tabs of a workstation, above its overview ({@link StationScreen}) and above the screen
 * of the block itself (woodcutter, stonecutter, mill, carpenter's bench, kiln, blast furnace,
 * smithing table, loom): <b>Block</b> · <b>Worker</b> · <b>Wage</b> · <b>Chests</b> ·
 * <b>Order</b> or <b>Settings</b>. Above a block's screen the block tab is the open one; a
 * click on another opens the overview on that tab.
 *
 * <p>A vanilla block nobody has set up (no worker, no chests) shows only the block and the
 * worker tab, without red marks: players who do not use this mod there are not bothered.
 */
public final class StationTabs {
	public enum Tab { BLOCK, WORK, WAGE, CHESTS, ORDER }

	/** Set by a block's screen just before it opens the overview: the tab the overview opens on. */
	private static @Nullable Tab pending;

	private StationTabs() {
	}

	/** Whether the station has a tab for its order or its settings. */
	public static boolean hasOrderTab(StationType type) {
		return type.hasOrder() || type == StationType.CHOPPING_BLOCK || type == StationType.PIT_STAKE || type == StationType.MINING_BENCH
			|| type == StationType.LOOM || type == StationType.SMOKER;
	}

	/** The tabs of the station; {@code overview}: all of them, for the overview itself. */
	public static List<Tab> tabs(StationType type, int flags, boolean overview) {
		List<Tab> tabs = new ArrayList<>();
		if (type.hasOwnScreen()) {
			tabs.add(Tab.BLOCK);
		}
		tabs.add(Tab.WORK);
		if (!overview && !isActive(type, flags)) {
			return tabs;
		}
		tabs.add(Tab.WAGE);
		tabs.add(Tab.CHESTS);
		if (hasOrderTab(type)) {
			tabs.add(Tab.ORDER);
		}
		return tabs;
	}

	/** Whether the station is used: a block of this mod, or a vanilla one that was set up. */
	private static boolean isActive(StationType type, int flags) {
		return !type.isVanilla() || StationTabFlags.has(flags, StationTabFlags.SET_UP);
	}

	/** Whether something on that tab keeps the worker from working (the red mark). */
	public static boolean problem(Tab tab, int flags) {
		return StationTabFlags.has(flags, switch (tab) {
			case WORK -> StationTabFlags.WORK_PROBLEM;
			case WAGE -> StationTabFlags.WAGE_PROBLEM;
			case CHESTS -> StationTabFlags.CHESTS_PROBLEM;
			case ORDER -> StationTabFlags.ORDER_PROBLEM;
			case BLOCK -> 0;
		});
	}

	/** The overview takes the tab a block's screen asked for, once. */
	static @Nullable Tab takePending() {
		Tab tab = pending;
		pending = null;
		return tab;
	}

	/** Draws a tab above the window: its sprite, its icon and the red mark. */
	static void extract(GuiGraphicsExtractor graphics, int left, int top, int index, Tab tab, StationType type, int flags, boolean selected, boolean problems) {
		int x = GuiTabs.topX(left, index);
		GuiTabs.top(graphics, left, top, index, selected);
		int iconX = x + 5;
		int iconY = GuiTabs.topIconY(top, selected);
		if (tab == Tab.WORK) {
			WorkerFace.face(graphics, iconX, iconY, type.profession(), StationTabFlags.has(flags, StationTabFlags.WORKER));
		} else {
			graphics.item(icon(type, tab), iconX, iconY);
		}
		if (problems && problem(tab, flags)) {
			GuiTabs.problem(graphics, x + GuiTabs.TOP_WIDTH, top - GuiTabs.TOP_HEIGHT);
		}
	}

	static ItemStack icon(StationType type, Tab tab) {
		return new ItemStack(switch (tab) {
			case BLOCK -> type.block();
			case WAGE -> Items.EMERALD;
			case CHESTS -> Items.CHEST;
			case ORDER -> switch (type) {
				case CHOPPING_BLOCK -> Items.STRIPPED_OAK_LOG;
				case PIT_STAKE -> Items.STRING;
				case MINING_BENCH -> Items.IRON_PICKAXE;
				case LOOM -> Items.WOOL.white();
				case SMOKER -> Items.PORKCHOP;
				default -> Items.PAPER;
			};
			case WORK -> Items.AIR;
		});
	}

	static Component name(StationType type, Tab tab) {
		return switch (tab) {
			case BLOCK -> type.displayName();
			case WORK -> Component.translatable("gui.workingvillagers.tab.work");
			case WAGE -> Component.translatable("gui.workingvillagers.tab.wage");
			case CHESTS -> Component.translatable("gui.workingvillagers.tab.chests");
			case ORDER -> Component.translatable(type.hasOrder() ? "gui.workingvillagers.tab.order" : "gui.workingvillagers.tab.settings");
		};
	}

	// --- Above a block's screen ---

	/** Draws the tabs above a block's screen whose window starts at {@code left}, {@code top}; the block tab is open. */
	public static void extractAbove(GuiGraphicsExtractor graphics, int left, int top, int mouseX, int mouseY, StationType type, int flags) {
		List<Tab> tabs = tabs(type, flags, false);
		boolean problems = isActive(type, flags);
		// The other tabs stand behind the window: only the part above it
		graphics.enableScissor(left, top - GuiTabs.TOP_HEIGHT, left + tabs.size() * GuiTabs.TOP_STEP, top);
		for (int i = 1; i < tabs.size(); i++) {
			extract(graphics, left, top, i, tabs.get(i), type, flags, false, problems);
		}
		graphics.disableScissor();
		extract(graphics, left, top, 0, tabs.getFirst(), type, flags, true, problems);
		if (overTab(mouseX, mouseY, left, top, type, flags).filter(tab -> tab != Tab.BLOCK).isPresent()) {
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}
	}

	/** The tab above a block's screen under the mouse. */
	public static Optional<Tab> overTab(double mouseX, double mouseY, int left, int top, StationType type, int flags) {
		List<Tab> tabs = tabs(type, flags, false);
		for (int i = 0; i < tabs.size(); i++) {
			if (GuiPanels.isOver(mouseX, mouseY, GuiTabs.topX(left, i), top - GuiTabs.TOP_HEIGHT, GuiTabs.TOP_WIDTH, GuiTabs.TOP_HEIGHT)) {
				return Optional.of(tabs.get(i));
			}
		}
		return Optional.empty();
	}

	/** The tooltip of the tab under the mouse; false if the mouse is over none. */
	public static boolean tooltip(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, int left, int top, StationType type, int flags) {
		Optional<Tab> tab = overTab(mouseX, mouseY, left, top, type, flags);
		if (tab.isEmpty()) {
			return false;
		}
		List<Component> lines = new ArrayList<>();
		lines.add(name(type, tab.get()));
		if (tab.get() == Tab.WORK && !StationTabFlags.has(flags, StationTabFlags.WORKER)) {
			lines.add(WorkerFace.workerLine(Optional.empty()).copy().withStyle(ChatFormatting.GRAY));
		}
		if (isActive(type, flags) && problem(tab.get(), flags)) {
			lines.add(Component.translatable("gui.workingvillagers.tab.problem").withStyle(ChatFormatting.RED));
		}
		graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
		return true;
	}

	/**
	 * A click on the tabs above a block's screen: another tab than the block's opens the
	 * overview there ({@code button}: the menu's button that opens it). False if the click
	 * was not on a tab.
	 */
	public static boolean click(Minecraft minecraft, int containerId, int button, double mouseX, double mouseY, int left, int top, StationType type, int flags) {
		Optional<Tab> tab = overTab(mouseX, mouseY, left, top, type, flags);
		if (tab.isEmpty()) {
			return false;
		}
		if (tab.get() != Tab.BLOCK) {
			open(minecraft, containerId, button, tab.get());
		}
		return true;
	}

	/** Opens the overview of the station on the given tab. */
	public static void open(Minecraft minecraft, int containerId, int button, Tab tab) {
		pending = tab;
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		minecraft.gameMode.handleInventoryButtonClick(containerId, button);
	}

	/** Whether a click is on the tabs, which is no click outside the screen. */
	public static boolean isOver(double mouseX, double mouseY, int left, int top, StationType type, int flags) {
		return overTab(mouseX, mouseY, left, top, type, flags).isPresent();
	}
}
