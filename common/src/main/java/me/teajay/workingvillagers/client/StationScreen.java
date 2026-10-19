package me.teajay.workingvillagers.client;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import me.teajay.workingvillagers.client.StationTabs.Tab;
import me.teajay.workingvillagers.crafting.StoneFamilies;
import me.teajay.workingvillagers.crafting.WoodTypes;
import me.teajay.workingvillagers.menu.StationMenu;
import me.teajay.workingvillagers.station.StationType;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The overview of a workstation, the size of a vanilla chest screen with tabs above it like
 * the creative inventory:
 *
 * <ul>
 * <li><b>Block</b> (stations with a screen of their own): back to that screen.</li>
 * <li><b>Worker</b>: who works here (assign, take away), what it is doing, what it did
 *     today and what it carries for its work.</li>
 * <li><b>Wage</b>: the wage box of the station (see {@link Workstations.Station#wage}) and the
 *     player's inventory, to put emeralds in and take them out.</li>
 * <li><b>Chests</b>: the linked chests (role, how full, unlink) and the buttons to link more.</li>
 * <li><b>Order</b> (woodcutter, stonecutter, carpenter's bench, kiln): the product, the wood
 *     types, the stock. <b>Settings</b> instead at the chopping block (strip logs), the pit
 *     stake (pit, ladder, depth), the mining bench (tunnel) and the loom (herd size).</li>
 * </ul>
 *
 * <p>The same tabs stand above the screens of the blocks (see {@link StationTabs}), so the
 * window stays where it is when switching.
 *
 * A tab with something missing (no wage or tool, no chest, no order) shows a red mark.
 */
public class StationScreen extends AbstractContainerScreen<StationMenu> {
	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
	private static final int WIDTH = 176;
	private static final int HEIGHT = 166;
	private static final int COLUMN_X = 7;
	private static final int COLUMN_WIDTH = 162;
	/** Where the content of a tab starts, below the title. */
	private static final int CONTENT_Y = 18;
	/** Buttons for the main actions, as high as the vanilla buttons. */
	private static final int BIG = 20;
	/** Buttons in rows: −, +, ×, the role of a chest. */
	private static final int SMALL = 16;
	private static final int ROW_HEIGHT = 20;
	private static final int VISIBLE_CHESTS = 6;
	private static final int BAR_WIDTH = 16;
	private static final int ROLE_WIDTH = 50;
	private static final int GRID_COLUMNS = 9;
	/** Order tab: where the grid of products starts, below its label. */
	private static final int SHAPES_Y = 30;
	/** Stonecutter: the grid of families or products. */
	private static final int STONE_CELLS = GRID_COLUMNS * 6;
	/** Kiln: rows of products on one page. */
	private static final int KILN_ROWS = 5;
	private static final int KILN_CELLS = GRID_COLUMNS * KILN_ROWS;
	/** Settings tab: the rows (one below the other, a big button high each). */
	private static final int SETTING_STEP = 26;
	private static final int LINE = 10;

	private final List<Tab> tabs = new ArrayList<>();
	private Tab tab = Tab.WORK;
	/** Where the wood types start, below the products. */
	private final int woodsLabelY;
	private final int woodsY;
	/** Carpenter's bench and kiln: the row with the stock. */
	private final int stockY;
	private final List<Hotspot> hotspots = new ArrayList<>();
	/** Chests tab: the first chest shown. */
	private int chestScroll;
	/** Stonecutter: the index of the family whose products are shown, -1 for the families. */
	private int openFamily = -1;
	private int familyPage;
	/** Stonecutter: whether the family of the order was opened when the families arrived. */
	private boolean familiesShown;
	/** Kiln: the page of products shown. */
	private int kilnPage;
	/** Kiln: whether the page of the order was opened when the products arrived. */
	private boolean kilnPageShown;

	public StationScreen(StationMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, WIDTH, HEIGHT);
		StationType type = menu.type();
		this.tabs.addAll(StationTabs.tabs(type, 0, true));
		// Opened from a tab above the block's screen
		Tab asked = StationTabs.takePending();
		if (asked != null && asked != Tab.BLOCK && this.tabs.contains(asked)) {
			this.tab = asked;
		}
		int productRows = type == StationType.CARPENTER_BENCH ? 3 : 2;
		this.woodsLabelY = SHAPES_Y + productRows * 18 + 5;
		this.woodsY = this.woodsLabelY + 11;
		this.stockY = type == StationType.KILN || type == StationType.FLETCHING_TABLE || type == StationType.BREWING_STAND
			|| type == StationType.CAULDRON ? SHAPES_Y + KILN_ROWS * 18 + 8 : this.woodsY + 2 * 18 + 6;
	}

	/** The open tab, e.g. "wage" (for the tests of the menus). */
	public String shownTab() {
		return this.tab.name().toLowerCase(java.util.Locale.ROOT);
	}

	/** Shows a tab (also for the screenshots of the menus). */
	public void showTab(String name) {
		for (Tab candidate : this.tabs) {
			if (candidate.name().equalsIgnoreCase(name)) {
				this.tab = candidate;
			}
		}
	}

	private StationType type() {
		return this.menu.type();
	}

	private boolean isStonecutter() {
		return this.type() == StationType.STONECUTTER;
	}

	private boolean isBench() {
		return this.type() == StationType.CARPENTER_BENCH;
	}

	private boolean isKiln() {
		return this.type() == StationType.KILN;
	}

	/** Kiln, fletching table and brewing stand: the order is picked from a grid of products, with a stock. */
	private boolean hasProductGrid() {
		return this.isKiln() || this.type() == StationType.FLETCHING_TABLE || this.type() == StationType.BREWING_STAND || this.type() == StationType.CAULDRON;
	}

	/** The start of the translation keys of the product grid: the kiln's, the fletcher's or the apothecary's. */
	private String gridKeys() {
		return switch (this.type()) {
			case KILN -> "gui.workingvillagers.kiln";
			case BREWING_STAND -> "gui.workingvillagers.apothecary";
			case CAULDRON -> "gui.workingvillagers.tanner";
			default -> "gui.workingvillagers.fletcher";
		};
	}

	/**
	 * A clickable area of the screen with its button id (sent to the server, -1 for none)
	 * or an action on the screen itself, and its tooltip.
	 */
	private record Hotspot(int x, int y, int width, int height, int button, List<Component> tooltip, @Nullable Runnable action) {
		Hotspot(int x, int y, int width, int height, int button, List<Component> tooltip) {
			this(x, y, width, height, button, tooltip, null);
		}

		boolean contains(double mouseX, double mouseY) {
			return GuiPanels.isOver(mouseX, mouseY, this.x, this.y, this.width, this.height);
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		this.menu.showSlots(this.tab == Tab.WAGE);
		super.extractBackground(graphics, mouseX, mouseY, a);
		this.hotspots.clear();
		// The other tabs behind the window, the open one in front of it
		for (int i = 0; i < this.tabs.size(); i++) {
			if (this.tabs.get(i) != this.tab) {
				this.extractTab(graphics, i, false);
			}
		}
		GuiPanels.panel(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
		this.extractTab(graphics, this.tabs.indexOf(this.tab), true);
		switch (this.tab) {
			case WORK -> this.extractWork(graphics, mouseX, mouseY);
			case WAGE -> this.extractWage(graphics);
			case CHESTS -> this.extractChests(graphics, mouseX, mouseY);
			case ORDER -> this.extractOrderTab(graphics, mouseX, mouseY);
			case BLOCK -> { }
		}
		for (Hotspot hotspot : this.hotspots) {
			if (hotspot.contains(mouseX, mouseY) && (hotspot.button() >= 0 || hotspot.action() != null)) {
				graphics.requestCursor(CursorTypes.POINTING_HAND);
			}
		}
	}

	// --- Tabs ---

	private void extractTab(GuiGraphicsExtractor graphics, int index, boolean selected) {
		Tab shown = this.tabs.get(index);
		int x = GuiTabs.topX(this.leftPos, index);
		int y = this.topPos - GuiTabs.TOP_HEIGHT;
		StationTabs.extract(graphics, this.leftPos, this.topPos, index, shown, this.type(), this.menu.getTabFlags(), selected, true);
		if (!selected) {
			this.hotspots.add(new Hotspot(x, y, GuiTabs.TOP_WIDTH, GuiTabs.TOP_HEIGHT, shown == Tab.BLOCK ? StationMenu.BACK_BUTTON : -1, this.tabTooltip(shown),
				shown == Tab.BLOCK ? null : () -> this.tab = shown));
		} else {
			this.hotspots.add(new Hotspot(x, y, GuiTabs.TOP_WIDTH, GuiTabs.TOP_HEIGHT, -1, this.tabTooltip(shown)));
		}
	}

	private List<Component> tabTooltip(Tab shown) {
		return switch (shown) {
			case BLOCK -> List.of(Component.translatable(this.backTooltip()));
			case WORK -> List.of(Component.translatable("gui.workingvillagers.tab.work"),
				WorkerFace.workerLine(this.menu.getWorker()).copy().withStyle(ChatFormatting.GRAY));
			case WAGE -> List.of(Component.translatable("gui.workingvillagers.tab.wage"),
				Component.translatable("gui.workingvillagers.wage.box", this.menu.getWageBox(), Workstations.MAX_WAGE).withStyle(ChatFormatting.GRAY),
				Component.translatable("gui.workingvillagers.wage.kept").withStyle(ChatFormatting.DARK_GRAY));
			case CHESTS -> List.of(Component.translatable("gui.workingvillagers.tab.chests"),
				Component.translatable("gui.workingvillagers.station.chests", this.menu.getChests().size(), this.menu.getMaxChests()).withStyle(ChatFormatting.GRAY));
			case ORDER -> List.of(StationTabs.name(this.type(), Tab.ORDER));
		};
	}

	// --- Worker tab ---

	/** The button next to the worker: assign one if the station is free, take it away otherwise. */
	private void extractWork(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		boolean free = this.menu.getWorker().isEmpty();
		if (free || this.menu.hasHiredWorker()) {
			int width = this.workerButtonWidth();
			this.button(graphics, mouseX, mouseY, this.leftPos + COLUMN_X + COLUMN_WIDTH - width, this.topPos + CONTENT_Y, width, BIG,
				Component.translatable(free ? "gui.workingvillagers.station.assign" : "gui.workingvillagers.station.unassign").getString(),
				free ? StationMenu.ASSIGN_BUTTON : StationMenu.UNASSIGN_BUTTON,
				List.of(Component.translatable(free ? "gui.workingvillagers.station.assign.tooltip" : "gui.workingvillagers.station.unassign.tooltip")));
		}
		this.menu.getSupplies().ifPresent(info -> {
			int y = this.topPos + this.suppliesY();
			SuppliesRow.extractSlots(graphics, this.font, info, this.leftPos + COLUMN_X, y);
			this.hotspots.add(new Hotspot(this.leftPos + COLUMN_X, y, COLUMN_WIDTH, SuppliesRow.SLOT, -1, SuppliesRow.tooltip(info)));
		});
	}

	private int workerButtonWidth() {
		boolean free = this.menu.getWorker().isEmpty();
		if (!free && !this.menu.hasHiredWorker()) {
			return 0;
		}
		return Math.max(60, this.font.width(Component.translatable(free ? "gui.workingvillagers.station.assign" : "gui.workingvillagers.station.unassign")) + 16);
	}

	/** The status, wrapped to at most three lines. */
	private List<FormattedCharSequence> statusLines() {
		return this.menu.getStatus()
			.map(status -> this.limit(this.font.split(status, COLUMN_WIDTH), 3))
			.orElse(List.of());
	}

	/** The line about today or the order, wrapped to at most two lines. */
	private List<FormattedCharSequence> secondLines() {
		return this.secondLine().map(line -> this.limit(this.font.split(line, COLUMN_WIDTH), 2)).orElse(List.of());
	}

	private List<FormattedCharSequence> limit(List<FormattedCharSequence> lines, int max) {
		return lines.size() <= max ? lines : lines.subList(0, max);
	}

	/** Where the slots of the supplies are: below the status and the second line. */
	private int suppliesY() {
		return CONTENT_Y + BIG + 6 + (this.statusLines().size() + this.secondLines().size()) * LINE + 4;
	}

	private void extractWorkLabels(GuiGraphicsExtractor graphics) {
		Component worker = this.menu.getWorker().orElse(Component.translatable("gui.workingvillagers.worker.none"));
		GuiPanels.scrollingText(graphics, this.font, worker.getString(), 8, CONTENT_Y + 6, COLUMN_WIDTH - this.workerButtonWidth() - 4,
			this.menu.getWorker().isPresent() ? GuiPanels.TEXT_COLOR : GuiPanels.MUTED_COLOR);
		int y = CONTENT_Y + BIG + 6;
		for (FormattedCharSequence line : this.statusLines()) {
			graphics.text(this.font, line, 8, y, GuiPanels.STATUS_COLOR, false);
			y += LINE;
		}
		Optional<Component> second = this.secondLine();
		int secondColor = second.map(line -> line.getStyle().getColor() != null ? 0xFF000000 | line.getStyle().getColor().getValue() : GuiPanels.MUTED_COLOR)
			.orElse(GuiPanels.MUTED_COLOR);
		for (FormattedCharSequence line : this.secondLines()) {
			graphics.text(this.font, line, 8, y, secondColor, false);
			y += LINE;
		}
		this.menu.getSupplies().ifPresent(info -> {
			int x = COLUMN_X + SuppliesRow.slotsWidth(info) + 4;
			int top = this.suppliesY();
			List<FormattedCharSequence> lines = this.limit(this.font.split(SuppliesRow.line(info), COLUMN_X + COLUMN_WIDTH - x), 2);
			int lineY = top + (SuppliesRow.SLOT - lines.size() * LINE) / 2 + 1;
			for (FormattedCharSequence line : lines) {
				graphics.text(this.font, line, x, lineY, SuppliesRow.color(info), false);
				lineY += LINE;
			}
			// How to give the worker its things, at the bottom
			List<FormattedCharSequence> hint = this.limit(this.font.split(Component.translatable("gui.workingvillagers.supplies.give"), COLUMN_WIDTH), 2);
			int hintY = HEIGHT - 7 - hint.size() * LINE;
			for (FormattedCharSequence line : hint) {
				graphics.text(this.font, line, 8, hintY, GuiPanels.MUTED_COLOR, false);
				hintY += LINE;
			}
		});
	}

	// --- Wage tab ---

	/** The slot of the wage box and the player's inventory, like a container screen. */
	private void extractWage(GuiGraphicsExtractor graphics) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, this.leftPos + StationMenu.WAGE_X - 1, this.topPos + StationMenu.WAGE_Y - 1, 18, 18);
		for (int row = 0; row < 4; row++) {
			int y = this.topPos + StationMenu.INVENTORY_Y - 1 + (row == 3 ? 58 : row * 18);
			for (int column = 0; column < 9; column++) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, this.leftPos + 7 + column * 18, y, 18, 18);
			}
		}
	}

	private void extractWageLabels(GuiGraphicsExtractor graphics) {
		graphics.text(this.font, Component.translatable("gui.workingvillagers.wage.box", this.menu.getWageBox(), Workstations.MAX_WAGE), 8, CONTENT_Y + 2,
			GuiPanels.TEXT_COLOR, false);
		int daily = this.menu.getDailyWage();
		int box = this.menu.getWageBox();
		int x = StationMenu.WAGE_X + 22;
		int width = COLUMN_X + COLUMN_WIDTH - x;
		Component days = daily <= 0 ? Component.translatable("gui.workingvillagers.wage.free")
			: box < daily ? Component.translatable(this.menu.getWorker().isPresent() ? "gui.workingvillagers.wage.empty" : "gui.workingvillagers.wage.empty_free")
			: Component.translatable("gui.workingvillagers.wage.days", box / daily);
		GuiPanels.scrollingText(graphics, this.font, days.getString(), x, StationMenu.WAGE_Y, width, box < daily && daily > 0 ? GuiPanels.WARNING_COLOR : GuiPanels.TEXT_COLOR);
		if (daily > 0) {
			GuiPanels.scrollingText(graphics, this.font, Component.translatable("gui.workingvillagers.wage.per_day", daily).getString(), x, StationMenu.WAGE_Y + LINE,
				width, GuiPanels.MUTED_COLOR);
		}
		int y = StationMenu.WAGE_Y + 22;
		for (FormattedCharSequence line : this.limit(this.font.split(Component.translatable("gui.workingvillagers.wage.hint"), COLUMN_WIDTH), 2)) {
			graphics.text(this.font, line, 8, y, GuiPanels.MUTED_COLOR, false);
			y += LINE - 1;
		}
		graphics.text(this.font, this.playerInventoryTitle, 8, StationMenu.INVENTORY_Y - 10, GuiPanels.TEXT_COLOR, false);
	}

	// --- Chests tab ---

	private void extractChests(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int left = this.leftPos + COLUMN_X;
		int top = this.topPos + CONTENT_Y;
		boolean processing = this.type().isProcessing();
		List<StationMenu.Chest> chests = this.menu.getChests();
		this.chestScroll = Mth.clamp(this.chestScroll, 0, Math.max(0, chests.size() - VISIBLE_CHESTS));
		boolean scrolls = chests.size() > VISIBLE_CHESTS;
		int rowWidth = scrolls ? COLUMN_WIDTH - 8 : COLUMN_WIDTH;
		for (int row = 0; row < Math.min(VISIBLE_CHESTS, chests.size()); row++) {
			int i = row + this.chestScroll;
			StationMenu.Chest chest = chests.get(i);
			int y = top + row * ROW_HEIGHT;
			GuiPanels.inset(graphics, left, y, rowWidth, ROW_HEIGHT - 1, 0xFFB4B4B4);
			BlockState state = this.minecraft.level.getBlockState(chest.pos());
			if (!state.isAir()) {
				graphics.item(new ItemStack(state.getBlock()), left + 2, y + 1);
			}
			int buttonX = left + rowWidth - SMALL - 2;
			int barX = buttonX - 4 - BAR_WIDTH;
			int roleX = barX - 4 - ROLE_WIDTH;
			int textWidth = (processing ? roleX : barX) - (left + 20) - 3;
			GuiPanels.scrollingText(graphics, this.font, chest.name().getString(), left + 20, y + 6, textWidth,
				chest.named() ? GuiPanels.TEXT_COLOR : GuiPanels.MUTED_COLOR);

			List<Component> rowTooltip = this.chestTooltip(chest, state);
			this.hotspots.add(new Hotspot(left, y, (processing ? roleX : barX) - left, ROW_HEIGHT - 1, -1, rowTooltip));
			if (processing) {
				String role = Component.translatable(chest.input() ? "gui.workingvillagers.station.input" : "gui.workingvillagers.station.output").getString();
				this.button(graphics, mouseX, mouseY, roleX, y + 1, ROLE_WIDTH, SMALL + 1, role, StationMenu.ROLE_BUTTON + i, List.of(
					Component.translatable(chest.input() ? "gui.workingvillagers.station.input.tooltip" : "gui.workingvillagers.station.output.tooltip"),
					Component.translatable("gui.workingvillagers.station.click_toggle").withStyle(ChatFormatting.DARK_GRAY)
				));
			}

			// How full the chest is
			int barY = y + 8;
			float fill = chest.size() == 0 ? 0.0F : (float) chest.used() / chest.size();
			graphics.fill(barX - 1, barY - 1, barX + BAR_WIDTH + 1, barY + 4, 0xFF373737);
			graphics.fill(barX, barY, barX + BAR_WIDTH, barY + 3, 0xFF8B8B8B);
			graphics.fill(barX, barY, barX + Math.round(BAR_WIDTH * fill), barY + 3, fill >= 1.0F ? 0xFFC03030 : 0xFF50A040);
			this.hotspots.add(new Hotspot(barX - 1, y, BAR_WIDTH + 2, ROW_HEIGHT - 1, -1, rowTooltip));

			this.button(graphics, mouseX, mouseY, buttonX, y + 1, SMALL, SMALL + 1, "×", StationMenu.UNLINK_BUTTON + i,
				List.of(Component.translatable("gui.workingvillagers.station.unlink")));
		}
		if (scrolls) {
			int trackX = left + COLUMN_WIDTH - 6;
			int trackHeight = VISIBLE_CHESTS * ROW_HEIGHT - 1;
			GuiPanels.inset(graphics, trackX, top, 6, trackHeight, 0xFF8B8B8B);
			int thumbHeight = trackHeight * VISIBLE_CHESTS / chests.size();
			int thumbY = top + (trackHeight - thumbHeight) * this.chestScroll / (chests.size() - VISIBLE_CHESTS);
			graphics.fill(trackX + 1, thumbY + 1, trackX + 5, thumbY + thumbHeight - 1, 0xFFE0E0E0);
			this.hotspots.add(new Hotspot(trackX, top, 6, trackHeight, -1, List.of(), () -> this.chestScroll = this.chestScroll == 0 ? chests.size() : 0));
		}

		// Buttons to link more chests by clicking them in the world
		int y = this.topPos + HEIGHT - 7 - BIG;
		List<Component> tooltip = List.of(Component.translatable("gui.workingvillagers.station.add_chests.tooltip"));
		boolean full = chests.size() >= this.menu.getMaxChests();
		if (processing) {
			int width = (COLUMN_WIDTH - 2) / 2;
			this.button(graphics, mouseX, mouseY, left, y, width, BIG, Component.translatable("gui.workingvillagers.station.add_inputs").getString(),
				StationMenu.ADD_INPUTS_BUTTON, tooltip);
			this.button(graphics, mouseX, mouseY, left + width + 2, y, width, BIG, Component.translatable("gui.workingvillagers.station.add_outputs").getString(),
				StationMenu.ADD_OUTPUTS_BUTTON, tooltip);
		} else {
			this.button(graphics, mouseX, mouseY, left, y, COLUMN_WIDTH, BIG, Component.translatable("gui.workingvillagers.station.add_chests").getString(),
				StationMenu.ADD_OUTPUTS_BUTTON, tooltip);
		}
		if (full) {
			// Picking still unlinks chests; the server refuses more than the maximum
			graphics.fill(left, y, left + COLUMN_WIDTH, y + BIG, 0x40000000);
		}
	}

	private void extractChestLabels(GuiGraphicsExtractor graphics) {
		Component count = Component.translatable("gui.workingvillagers.station.chests", this.menu.getChests().size(), this.menu.getMaxChests());
		graphics.text(this.font, count, WIDTH - 8 - this.font.width(count), this.titleLabelY, GuiPanels.MUTED_COLOR, false);
		if (this.menu.getChests().isEmpty()) {
			int y = CONTENT_Y + 4;
			for (FormattedCharSequence line : this.font.split(this.noChestsWarning(), COLUMN_WIDTH - 4)) {
				graphics.text(this.font, line, 9, y, GuiPanels.WARNING_COLOR, false);
				y += LINE;
			}
		}
	}

	private Component noChestsWarning() {
		return Component.translatable(switch (this.type()) {
			case CHOPPING_BLOCK -> "gui.workingvillagers.chopping_block.no_chests";
			case COMPOSTER -> "gui.workingvillagers.composter.no_chests";
			case PIT_STAKE -> "gui.workingvillagers.pit_stake.no_chests";
			case MINING_BENCH -> "gui.workingvillagers.mining_bench.no_chests";
			case LOOM -> "gui.workingvillagers.loom.no_chests";
			case SMOKER -> "gui.workingvillagers.smoker.no_chests";
			case BARREL -> "gui.workingvillagers.barrel.no_chests";
			case TARGET -> "gui.workingvillagers.target.no_chests";
			default -> "gui.workingvillagers.station.no_chests";
		});
	}

	private List<Component> chestTooltip(StationMenu.Chest chest, BlockState state) {
		List<Component> lines = new ArrayList<>();
		lines.add(chest.name());
		if (chest.named() && !state.isAir()) {
			lines.add(state.getBlock().getName().copy().withStyle(ChatFormatting.GRAY));
		}
		lines.add(Component.translatable("gui.workingvillagers.chopping_block.used", chest.used(), chest.size()).withStyle(ChatFormatting.GRAY));
		this.menu.getStationPos().ifPresent(station -> lines.add(
			Component.translatable("gui.workingvillagers.distance", Math.round(Math.sqrt(station.distSqr(chest.pos())))).withStyle(ChatFormatting.GRAY)
		));
		if (!chest.named()) {
			lines.add(Component.translatable("gui.workingvillagers.container.unnamed").withStyle(ChatFormatting.DARK_GRAY));
		}
		return lines;
	}

	// --- Order and settings tab ---

	private void extractOrderTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		switch (this.type()) {
			case STONECUTTER -> this.extractStoneOrder(graphics, mouseX, mouseY);
			case KILN, FLETCHING_TABLE, BREWING_STAND -> {
				this.extractKilnOrder(graphics, mouseX, mouseY);
				this.extractStock(graphics, mouseX, mouseY);
			}
			case CAULDRON -> {
				this.extractKilnOrder(graphics, mouseX, mouseY);
				this.extractStock(graphics, mouseX, mouseY);
				this.extractDye(graphics, mouseX, mouseY);
			}
			case CARPENTER_BENCH -> {
				this.extractProducts(graphics, this.leftPos + 8, this.menu.getShape());
				this.extractStock(graphics, mouseX, mouseY);
				this.extractWoods(graphics, mouseX, mouseY);
			}
			case WOODCUTTER -> {
				this.extractShapes(graphics);
				this.extractWoods(graphics, mouseX, mouseY);
			}
			case CHOPPING_BLOCK -> this.extractStrip(graphics, mouseX, mouseY);
			case BARREL -> this.extractBoat(graphics, mouseX, mouseY);
			case TARGET -> {
				this.plusMinus(graphics, mouseX, mouseY, this.settingY(0), BIG, StationMenu.HERD_LESS_BUTTON, StationMenu.HERD_MORE_BUTTON, List.of(
					Component.translatable("gui.workingvillagers.target.radius.tooltip"),
					Component.translatable("gui.workingvillagers.target.radius.tooltip2", Workstations.MIN_GUARD_RADIUS, Workstations.MAX_GUARD_RADIUS)
						.withStyle(ChatFormatting.GRAY)
				));
				int width = this.toggleWidth("gui.workingvillagers.target.mode.watch", "gui.workingvillagers.target.mode.hunt");
				this.button(graphics, mouseX, mouseY, this.leftPos + COLUMN_X + COLUMN_WIDTH - width, this.settingY(1), width, BIG,
					Component.translatable(this.menu.getHunting() ? "gui.workingvillagers.target.mode.hunt" : "gui.workingvillagers.target.mode.watch").getString(),
					StationMenu.HUNT_BUTTON, List.of(
						Component.translatable("gui.workingvillagers.target.mode.tooltip"),
						Component.translatable("gui.workingvillagers.station.click_toggle").withStyle(ChatFormatting.DARK_GRAY)
					));
			}
			case PIT_STAKE -> this.extractPit(graphics, mouseX, mouseY);
			case MINING_BENCH -> this.extractTunnel(graphics, mouseX, mouseY);
			case LOOM, SMOKER -> this.plusMinus(graphics, mouseX, mouseY, this.settingY(0), BIG, StationMenu.HERD_LESS_BUTTON, StationMenu.HERD_MORE_BUTTON, List.of(
				Component.translatable("gui.workingvillagers." + this.herdKeys() + ".herd.tooltip"),
				Component.translatable("gui.workingvillagers." + this.herdKeys() + ".herd.tooltip2", Workstations.MAX_HERD).withStyle(ChatFormatting.GRAY)
			));
			default -> { }
		}
	}

	/** Woodcutter: the shapes (planks, slabs, ...). */
	private void extractShapes(GuiGraphicsExtractor graphics) {
		int left = this.leftPos + 8;
		String shape = this.menu.getShape();
		for (int i = 0; i < WoodTypes.SHAPES.size(); i++) {
			String option = WoodTypes.SHAPES.get(i);
			int x = left + i % GRID_COLUMNS * 18;
			int y = this.topPos + SHAPES_Y + i / GRID_COLUMNS * 18;
			Item icon = WoodTypes.product(WoodTypes.ALL.getFirst(), option);
			this.cell(graphics, x, y, icon == null ? ItemStack.EMPTY : new ItemStack(icon), option.equals(shape), false);
			this.hotspots.add(new Hotspot(x, y, 18, 18, StationMenu.SHAPE_BUTTON + i, List.of(
				shapeName(option),
				Component.translatable(option.equals(shape) ? "gui.workingvillagers.station.shape.selected" : "gui.workingvillagers.station.shape.select")
					.withStyle(ChatFormatting.GRAY)
			)));
		}
	}

	/** Woodcutter and carpenter's bench: the wood types the order may be made of. */
	private void extractWoods(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int left = this.leftPos + 8;
		String shape = this.menu.getShape();
		Set<String> woods = this.menu.getWoods();
		Set<String> available = this.menu.getAvailableWoods();
		String all = Component.translatable("gui.workingvillagers.station.woods.all").getString();
		String none = Component.translatable("gui.workingvillagers.station.woods.none").getString();
		int noneWidth = this.font.width(none) + 10;
		int allWidth = this.font.width(all) + 10;
		int right = this.leftPos + COLUMN_X + COLUMN_WIDTH;
		int buttonY = this.topPos + this.woodsLabelY - 4;
		this.button(graphics, mouseX, mouseY, right - noneWidth, buttonY, noneWidth, 14, none, StationMenu.NO_WOODS_BUTTON, List.of());
		this.button(graphics, mouseX, mouseY, right - noneWidth - 2 - allWidth, buttonY, allWidth, 14, all, StationMenu.ALL_WOODS_BUTTON, List.of());

		for (int i = 0; i < WoodTypes.ALL.size(); i++) {
			WoodTypes.WoodType wood = WoodTypes.ALL.get(i);
			int x = left + i % GRID_COLUMNS * 18;
			int y = this.topPos + this.woodsY + i / GRID_COLUMNS * 18;
			boolean allowed = woods.contains(wood.name());
			boolean possible = available.contains(wood.name());
			this.cell(graphics, x, y, new ItemStack(wood.log()), allowed, !allowed);
			if (allowed && !possible) {
				graphics.fill(x + 1, y + 1, x + 17, y + 17, 0x60C03030);
			}
			List<Component> tooltip = new ArrayList<>();
			tooltip.add(new ItemStack(wood.log()).getHoverName());
			tooltip.add(Component.translatable(allowed ? "gui.workingvillagers.station.wood.allowed" : "gui.workingvillagers.station.wood.forbidden")
				.withStyle(allowed ? ChatFormatting.DARK_GREEN : ChatFormatting.GRAY));
			if (shape != null && !possible) {
				tooltip.add(Component.translatable("gui.workingvillagers.station.wood.impossible", shapeName(shape)).withStyle(ChatFormatting.RED));
			}
			tooltip.add(Component.translatable("gui.workingvillagers.station.click_toggle").withStyle(ChatFormatting.DARK_GRAY));
			this.hotspots.add(new Hotspot(x, y, 18, 18, StationMenu.WOOD_BUTTON + i, tooltip));
		}
	}

	/**
	 * Stonecutter: the families (a click opens one, the family of the order is marked) or the
	 * products of the open family (a click orders one, another click clears the order).
	 */
	private void extractStoneOrder(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		List<StoneFamilies.Family> families = this.menu.getFamilies();
		ItemStack order = this.menu.getOrder();
		if (!this.familiesShown && !families.isEmpty()) {
			this.familiesShown = true;
			this.openFamily = order.isEmpty() ? -1 : this.familyOf(families, order);
			this.familyPage = Math.max(0, this.openFamily) / STONE_CELLS;
		}
		if (this.openFamily >= families.size()) {
			this.openFamily = -1;
		}
		int left = this.leftPos + 8;
		int top = this.topPos + SHAPES_Y;
		int right = this.leftPos + COLUMN_X + COLUMN_WIDTH;
		int buttonY = top - 14;

		if (this.openFamily >= 0) {
			String back = Component.translatable("gui.workingvillagers.station.stone.back").getString();
			int width = this.font.width(back) + 10;
			this.localButton(graphics, mouseX, mouseY, right - width, buttonY, width, back, () -> this.openFamily = -1);
			int first = families.stream().limit(this.openFamily).mapToInt(family -> family.products().size()).sum();
			List<Item> products = families.get(this.openFamily).products();
			for (int i = 0; i < Math.min(products.size(), STONE_CELLS); i++) {
				ItemStack product = new ItemStack(products.get(i));
				boolean selected = order.is(product.getItem());
				int x = left + i % GRID_COLUMNS * 18;
				int y = top + i / GRID_COLUMNS * 18;
				this.cell(graphics, x, y, product, selected, false);
				List<Component> tooltip = new ArrayList<>();
				tooltip.add(product.getHoverName());
				tooltip.add(Component.translatable(selected ? "gui.workingvillagers.station.shape.selected" : "gui.workingvillagers.station.shape.select")
					.withStyle(ChatFormatting.GRAY));
				if (selected) {
					tooltip.add(Component.translatable("gui.workingvillagers.station.stone.clear").withStyle(ChatFormatting.DARK_GRAY));
				}
				this.hotspots.add(new Hotspot(x, y, 18, 18, StationMenu.STONE_PRODUCT_BUTTON + first + i, tooltip));
			}
			return;
		}

		int pages = this.familyPages(families);
		this.familyPage = Mth.clamp(this.familyPage, 0, pages - 1);
		if (pages > 1) {
			this.localButton(graphics, mouseX, mouseY, right - SMALL, buttonY, SMALL, ">",
				() -> this.familyPage = Math.min(this.familyPage + 1, pages - 1));
			this.localButton(graphics, mouseX, mouseY, right - 2 * SMALL - 2, buttonY, SMALL, "<",
				() -> this.familyPage = Math.max(this.familyPage - 1, 0));
		}
		int start = this.familyPage * STONE_CELLS;
		for (int i = start; i < Math.min(families.size(), start + STONE_CELLS); i++) {
			StoneFamilies.Family family = families.get(i);
			ItemStack icon = new ItemStack(family.icon());
			int x = left + (i - start) % GRID_COLUMNS * 18;
			int y = top + (i - start) / GRID_COLUMNS * 18;
			this.cell(graphics, x, y, icon, !order.isEmpty() && family.contains(order.getItem()), false);
			int index = i;
			this.hotspots.add(new Hotspot(x, y, 18, 18, -1, List.of(
				icon.getHoverName(),
				Component.translatable("gui.workingvillagers.station.stone.family", family.products().size()).withStyle(ChatFormatting.GRAY)
			), () -> this.openFamily = index));
		}
	}

	private int familyOf(List<StoneFamilies.Family> families, ItemStack item) {
		for (int i = 0; i < families.size(); i++) {
			if (families.get(i).contains(item.getItem())) {
				return i;
			}
		}
		return -1;
	}

	private int familyPages(List<StoneFamilies.Family> families) {
		return Math.max(1, (families.size() + STONE_CELLS - 1) / STONE_CELLS);
	}

	/** Carpenter's bench: the products; one cell per shape for those made of one wood type. */
	private void extractProducts(GuiGraphicsExtractor graphics, int left, @Nullable String shape) {
		List<ItemStack> products = this.menu.getProducts();
		ItemStack order = this.menu.getOrder();
		for (int i = 0; i < products.size(); i++) {
			ItemStack product = products.get(i);
			String productShape = WoodTypes.shapeOf(product.getItem(), WoodTypes.CARPENTER_SHAPES);
			boolean selected = productShape != null ? productShape.equals(shape) : order.is(product.getItem());
			int x = left + i % GRID_COLUMNS * 18;
			int y = this.topPos + SHAPES_Y + i / GRID_COLUMNS * 18;
			this.cell(graphics, x, y, product, selected, false);
			this.hotspots.add(new Hotspot(x, y, 18, 18, StationMenu.SHAPE_BUTTON + i, List.of(
				productShape != null ? shapeName(productShape) : product.getHoverName(),
				Component.translatable(selected ? "gui.workingvillagers.station.shape.selected" : "gui.workingvillagers.station.shape.select")
					.withStyle(ChatFormatting.GRAY)
			)));
		}
	}

	/**
	 * Kiln and fletching table: everything it makes (a click orders one, another click clears the order, at the kiln then
	 * it fires everything), in pages.
	 */
	private void extractKilnOrder(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		List<ItemStack> products = this.menu.getProducts();
		ItemStack order = this.menu.getOrder();
		int pages = Math.max(1, (products.size() + KILN_CELLS - 1) / KILN_CELLS);
		if (!this.kilnPageShown && !products.isEmpty()) {
			this.kilnPageShown = true;
			for (int i = 0; i < products.size(); i++) {
				if (ItemStack.isSameItemSameComponents(order, products.get(i))) {
					this.kilnPage = i / KILN_CELLS;
				}
			}
		}
		this.kilnPage = Mth.clamp(this.kilnPage, 0, pages - 1);
		int left = this.leftPos + 8;
		int top = this.topPos + SHAPES_Y;
		int right = this.leftPos + COLUMN_X + COLUMN_WIDTH;
		if (pages > 1) {
			this.localButton(graphics, mouseX, mouseY, right - SMALL, top - 14, SMALL, ">",
				() -> this.kilnPage = Math.min(this.kilnPage + 1, pages - 1));
			this.localButton(graphics, mouseX, mouseY, right - 2 * SMALL - 2, top - 14, SMALL, "<",
				() -> this.kilnPage = Math.max(this.kilnPage - 1, 0));
		}
		int start = this.kilnPage * KILN_CELLS;
		for (int i = start; i < Math.min(products.size(), start + KILN_CELLS); i++) {
			ItemStack product = products.get(i);
			boolean selected = ItemStack.isSameItemSameComponents(order, product);
			int x = left + (i - start) % GRID_COLUMNS * 18;
			int y = top + (i - start) / GRID_COLUMNS * 18;
			this.cell(graphics, x, y, product, selected, false);
			List<Component> tooltip = new ArrayList<>();
			tooltip.add(product.getHoverName());
			tooltip.add(Component.translatable(selected ? "gui.workingvillagers.station.shape.selected" : this.gridKeys() + ".select")
				.withStyle(ChatFormatting.GRAY));
			if (selected) {
				tooltip.add(Component.translatable(this.gridKeys() + ".clear").withStyle(ChatFormatting.DARK_GRAY));
			}
			this.hotspots.add(new Hotspot(x, y, 18, 18, StationMenu.KILN_PRODUCT_BUTTON + i, tooltip));
		}
	}

	/** Carpenter's bench and kiln: how many products to keep in the output chests, with buttons to change it. */
	private void extractStock(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int y = this.topPos + this.stockY - 4;
		List<Component> tooltip = List.of(
			Component.translatable(this.hasProductGrid() ? this.gridKeys() + ".stock.tooltip" : "gui.workingvillagers.station.stock.tooltip"),
			Component.translatable("gui.workingvillagers.station.stock.tooltip2").withStyle(ChatFormatting.GRAY)
		);
		this.plusMinus(graphics, mouseX, mouseY, y, SMALL, StationMenu.STOCK_LESS_BUTTON, StationMenu.STOCK_MORE_BUTTON, tooltip);
		this.hotspots.add(new Hotspot(this.leftPos + COLUMN_X, y, COLUMN_WIDTH - 2 * SMALL - 4, SMALL, -1, tooltip));
	}

	/** Cauldron: the color of the leather armor, with buttons to the previous and next one. */
	private void extractDye(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int y = this.topPos + this.dyeY() - 4;
		List<Component> tooltip = List.of(
			Component.translatable("gui.workingvillagers.tanner.dye.tooltip"),
			Component.translatable("gui.workingvillagers.tanner.dye.tooltip2").withStyle(ChatFormatting.GRAY)
		);
		int right = this.leftPos + COLUMN_X + COLUMN_WIDTH;
		this.button(graphics, mouseX, mouseY, right - SMALL, y, SMALL, SMALL, ">", StationMenu.DYE_MORE_BUTTON, tooltip);
		this.button(graphics, mouseX, mouseY, right - 2 * SMALL - 2, y, SMALL, SMALL, "<", StationMenu.DYE_LESS_BUTTON, tooltip);
		this.hotspots.add(new Hotspot(this.leftPos + COLUMN_X, y, COLUMN_WIDTH - 2 * SMALL - 4, SMALL, -1, tooltip));
	}

	/** Cauldron: the row of the color, below the stock. */
	private int dyeY() {
		return this.stockY + SMALL + 8;
	}

	/** Chopping block: the switch whether the lumberjack strips the logs. */
	private void extractStrip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int width = this.toggleWidth("gui.workingvillagers.chopping_block.strip.on", "gui.workingvillagers.chopping_block.strip.off");
		int y = this.topPos + CONTENT_Y;
		List<Component> tooltip = List.of(
			Component.translatable("gui.workingvillagers.chopping_block.strip.tooltip"),
			Component.translatable("gui.workingvillagers.station.click_toggle").withStyle(ChatFormatting.DARK_GRAY)
		);
		this.button(graphics, mouseX, mouseY, this.leftPos + COLUMN_X + COLUMN_WIDTH - width, y, width, BIG, this.stripLabel().getString(),
			StationMenu.STRIP_BUTTON, tooltip);
		this.hotspots.add(new Hotspot(this.leftPos + COLUMN_X, y, COLUMN_WIDTH - width - 4, BIG, -1, tooltip));
	}

	/** Barrel: the switch whether the fisher rows out in a boat. */
	private void extractBoat(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int width = this.toggleWidth("gui.workingvillagers.barrel.boat.on", "gui.workingvillagers.barrel.boat.off");
		int y = this.topPos + CONTENT_Y;
		List<Component> tooltip = List.of(
			Component.translatable("gui.workingvillagers.barrel.boat.tooltip"),
			Component.translatable("gui.workingvillagers.station.click_toggle").withStyle(ChatFormatting.DARK_GRAY)
		);
		this.button(graphics, mouseX, mouseY, this.leftPos + COLUMN_X + COLUMN_WIDTH - width, y, width, BIG,
			Component.translatable(this.menu.getBoat() ? "gui.workingvillagers.barrel.boat.on" : "gui.workingvillagers.barrel.boat.off").getString(),
			StationMenu.BOAT_BUTTON, tooltip);
		this.hotspots.add(new Hotspot(this.leftPos + COLUMN_X, y, COLUMN_WIDTH - width - 4, BIG, -1, tooltip));
	}

	private Component stripLabel() {
		return Component.translatable(this.menu.getStrip() ? "gui.workingvillagers.chopping_block.strip.on" : "gui.workingvillagers.chopping_block.strip.off");
	}

	/** Pit stake: the pit (pick it, the ladder) and how deep to dig. */
	private void extractPit(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int left = this.leftPos + COLUMN_X;
		this.button(graphics, mouseX, mouseY, left, this.settingY(0), COLUMN_WIDTH, BIG,
			Component.translatable("gui.workingvillagers.pit.pick").getString(), StationMenu.PICK_PIT_BUTTON, List.of(
				Component.translatable("gui.workingvillagers.pit.pick.tooltip"),
				Component.translatable("gui.workingvillagers.pit.pick.tooltip2").withStyle(ChatFormatting.GRAY)
			));
		List<Component> depthTooltip = List.of(
			Component.translatable("gui.workingvillagers.pit.depth.tooltip"),
			Component.translatable("gui.workingvillagers.pit.depth.tooltip2", this.menu.getMaxDepth()).withStyle(ChatFormatting.GRAY)
		);
		this.plusMinus(graphics, mouseX, mouseY, this.settingY(1), BIG, StationMenu.DEPTH_LESS_BUTTON, StationMenu.DEPTH_MORE_BUTTON, depthTooltip);
		if (this.menu.getPit().isPresent()) {
			this.button(graphics, mouseX, mouseY, left, this.settingY(2), COLUMN_WIDTH, BIG,
				Component.translatable("gui.workingvillagers.pit.ladder").getString(), StationMenu.PICK_LADDER_BUTTON,
				List.of(Component.translatable("gui.workingvillagers.pit.ladder.tooltip")));
		}
	}

	/** Mining bench: picking the tunnel, its length and kind, and the side tunnels. */
	private void extractTunnel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int right = this.leftPos + COLUMN_X + COLUMN_WIDTH;
		this.button(graphics, mouseX, mouseY, this.leftPos + COLUMN_X, this.settingY(0), COLUMN_WIDTH, BIG,
			Component.translatable("gui.workingvillagers.tunnel.pick").getString(), StationMenu.PICK_TUNNEL_BUTTON, List.of(
				Component.translatable("gui.workingvillagers.tunnel.pick.tooltip"),
				Component.translatable("gui.workingvillagers.tunnel.pick.tooltip2").withStyle(ChatFormatting.GRAY)
			));
		Optional<StationMenu.TunnelInfo> tunnel = this.menu.getTunnel();
		if (tunnel.isEmpty()) {
			return;
		}
		this.plusMinus(graphics, mouseX, mouseY, this.settingY(1), BIG, StationMenu.LENGTH_LESS_BUTTON, StationMenu.LENGTH_MORE_BUTTON, List.of(
			Component.translatable("gui.workingvillagers.tunnel.length.tooltip"),
			Component.translatable("gui.workingvillagers.tunnel.length.tooltip2", this.menu.getMaxTunnelLength()).withStyle(ChatFormatting.GRAY)
		));
		int kindWidth = this.toggleWidth("gui.workingvillagers.tunnel.kind.stairs", "gui.workingvillagers.tunnel.kind.tunnel");
		this.button(graphics, mouseX, mouseY, right - kindWidth, this.settingY(2), kindWidth, BIG, this.stairsLabel(tunnel.get().stairs()).getString(),
			StationMenu.STAIRS_BUTTON, List.of(
				Component.translatable("gui.workingvillagers.tunnel.kind.tooltip"),
				Component.translatable("gui.workingvillagers.tunnel.kind.tooltip2").withStyle(ChatFormatting.GRAY),
				Component.translatable("gui.workingvillagers.station.click_toggle").withStyle(ChatFormatting.DARK_GRAY)
			));
		if (!tunnel.get().stairs() && this.menu.getMaxBranches() > 0) {
			this.plusMinus(graphics, mouseX, mouseY, this.settingY(3), BIG, StationMenu.BRANCHES_LESS_BUTTON, StationMenu.BRANCHES_MORE_BUTTON, List.of(
				Component.translatable("gui.workingvillagers.tunnel.branches.tooltip"),
				Component.translatable("gui.workingvillagers.tunnel.branches.tooltip2", this.menu.getMaxBranches()).withStyle(ChatFormatting.GRAY)
			));
		}
	}

	private Component stairsLabel(boolean stairs) {
		return Component.translatable(stairs ? "gui.workingvillagers.tunnel.kind.stairs" : "gui.workingvillagers.tunnel.kind.tunnel");
	}

	/** The top of a row of the settings tab (absolute). */
	private int settingY(int row) {
		return this.topPos + this.settingRowY(row);
	}

	/**
	 * The top of a row of the settings tab (relative): at the pit stake and the mining bench
	 * below the line that tells which pit or tunnel is picked.
	 */
	private int settingRowY(int row) {
		int summary = this.type() == StationType.PIT_STAKE || this.type() == StationType.MINING_BENCH ? LINE + 4 : 0;
		return CONTENT_Y + summary + row * SETTING_STEP;
	}

	/** A − and a + button at the right edge. */
	private void plusMinus(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int y, int size, int less, int more, List<Component> tooltip) {
		int right = this.leftPos + COLUMN_X + COLUMN_WIDTH;
		this.button(graphics, mouseX, mouseY, right - size, y, size, size, "+", more, tooltip);
		this.button(graphics, mouseX, mouseY, right - 2 * size - 2, y, size, size, "-", less, tooltip);
	}

	private int toggleWidth(String first, String second) {
		return Math.max(this.font.width(Component.translatable(first)), this.font.width(Component.translatable(second))) + 16;
	}

	/** The labels of the order or settings tab. */
	private void extractOrderLabels(GuiGraphicsExtractor graphics) {
		int textY = (BIG - 8) / 2 + 1;
		switch (this.type()) {
			case STONECUTTER -> GuiPanels.scrollingText(graphics, this.font, this.stoneLabel().getString(), 8, SHAPES_Y - 11, COLUMN_WIDTH - 40, GuiPanels.TEXT_COLOR);
			case KILN, FLETCHING_TABLE, BREWING_STAND, CAULDRON -> graphics.text(this.font, Component.translatable("gui.workingvillagers.station.shape"), 8, SHAPES_Y - 11, GuiPanels.TEXT_COLOR, false);
			case WOODCUTTER, CARPENTER_BENCH -> {
				graphics.text(this.font, Component.translatable("gui.workingvillagers.station.shape"), 8, SHAPES_Y - 11, GuiPanels.TEXT_COLOR, false);
				graphics.text(this.font, Component.translatable(this.isBench() ? "gui.workingvillagers.station.woods.bench" : "gui.workingvillagers.station.woods"),
					8, this.woodsLabelY, GuiPanels.TEXT_COLOR, false);
			}
			case TARGET -> {
				GuiPanels.scrollingText(graphics, this.font, Component.translatable("gui.workingvillagers.target.radius", this.menu.getHerd()).getString(),
					8, this.settingRowY(0) + textY, COLUMN_WIDTH - 2 * BIG - 6, GuiPanels.TEXT_COLOR);
				int width = this.toggleWidth("gui.workingvillagers.target.mode.watch", "gui.workingvillagers.target.mode.hunt");
				GuiPanels.scrollingText(graphics, this.font, Component.translatable("gui.workingvillagers.target.mode").getString(),
					8, this.settingRowY(1) + textY, COLUMN_WIDTH - width - 4, GuiPanels.TEXT_COLOR);
				this.hint(graphics, Component.translatable(this.menu.getHunting() ? "gui.workingvillagers.target.hint.hunt" : "gui.workingvillagers.target.hint"),
					this.settingRowY(2));
			}
			case BARREL -> {
				int width = this.toggleWidth("gui.workingvillagers.barrel.boat.on", "gui.workingvillagers.barrel.boat.off");
				GuiPanels.scrollingText(graphics, this.font, Component.translatable("gui.workingvillagers.barrel.boat").getString(), 8,
					CONTENT_Y + textY, COLUMN_WIDTH - width - 4, GuiPanels.TEXT_COLOR);
				this.hint(graphics, Component.translatable("gui.workingvillagers.barrel.boat.hint"), CONTENT_Y + BIG + 8);
			}
			case LOOM, SMOKER -> {
				String keys = "gui.workingvillagers." + this.herdKeys();
				int herd = this.menu.getHerd();
				GuiPanels.scrollingText(graphics, this.font, (herd == 0
					? Component.translatable(keys + ".herd.none")
					: Component.translatable(keys + ".herd", herd)).getString(),
					8, this.settingRowY(0) + textY, COLUMN_WIDTH - 2 * BIG - 6, GuiPanels.TEXT_COLOR);
				graphics.text(this.font, Component.translatable(keys + ".sheep", this.menu.getSheep()), 8, this.settingRowY(1) - 2,
					GuiPanels.MUTED_COLOR, false);
				this.hint(graphics, Component.translatable(keys + ".herd.hint"), this.settingRowY(1) + LINE);
			}
			case CHOPPING_BLOCK -> {
				int width = this.toggleWidth("gui.workingvillagers.chopping_block.strip.on", "gui.workingvillagers.chopping_block.strip.off");
				GuiPanels.scrollingText(graphics, this.font, Component.translatable("gui.workingvillagers.chopping_block.strip").getString(), 8,
					CONTENT_Y + textY, COLUMN_WIDTH - width - 4, GuiPanels.TEXT_COLOR);
				this.hint(graphics, Component.translatable("gui.workingvillagers.chopping_block.strip.tooltip2"), CONTENT_Y + BIG + 8);
			}
			case PIT_STAKE -> {
				Component pit = this.menu.getPit()
					.map(size -> (Component) Component.translatable("gui.workingvillagers.pit.size", size[0], size[1], size[2]))
					.orElse(Component.translatable("gui.workingvillagers.pit.none"));
				GuiPanels.scrollingText(graphics, this.font, pit.getString(), 8, CONTENT_Y + 1, COLUMN_WIDTH,
					this.menu.getPit().isPresent() ? GuiPanels.TEXT_COLOR : GuiPanels.WARNING_COLOR);
				GuiPanels.scrollingText(graphics, this.font, Component.translatable("gui.workingvillagers.pit.depth", this.menu.getDepth()).getString(), 8,
					this.settingRowY(1) + textY, COLUMN_WIDTH - 2 * BIG - 6, GuiPanels.TEXT_COLOR);
				this.hint(graphics, Component.translatable("gui.workingvillagers.pit.pick.tooltip2"),
					this.settingRowY(this.menu.getPit().isPresent() ? 3 : 2) + 2);
			}
			case MINING_BENCH -> {
				Optional<StationMenu.TunnelInfo> tunnel = this.menu.getTunnel();
				Component summary = tunnel
					.map(info -> (Component) Component.translatable("gui.workingvillagers.tunnel.direction",
						Component.translatable("gui.workingvillagers.direction." + info.facing())))
					.orElse(Component.translatable("gui.workingvillagers.tunnel.none"));
				GuiPanels.scrollingText(graphics, this.font, summary.getString(), 8, CONTENT_Y + 1, COLUMN_WIDTH,
					tunnel.isPresent() ? GuiPanels.TEXT_COLOR : GuiPanels.WARNING_COLOR);
				if (tunnel.isEmpty()) {
					this.hint(graphics, Component.translatable("gui.workingvillagers.tunnel.pick.tooltip2"), this.settingRowY(1) + 2);
					return;
				}
				StationMenu.TunnelInfo info = tunnel.get();
				GuiPanels.scrollingText(graphics, this.font,
					Component.translatable(info.stairs() ? "gui.workingvillagers.tunnel.steps" : "gui.workingvillagers.tunnel.length", info.length()).getString(),
					8, this.settingRowY(1) + textY, COLUMN_WIDTH - 2 * BIG - 6, GuiPanels.TEXT_COLOR);
				GuiPanels.scrollingText(graphics, this.font, Component.translatable("gui.workingvillagers.tunnel.kind").getString(), 8,
					this.settingRowY(2) + textY, COLUMN_WIDTH - this.toggleWidth("gui.workingvillagers.tunnel.kind.stairs", "gui.workingvillagers.tunnel.kind.tunnel") - 4,
					GuiPanels.TEXT_COLOR);
				if (!info.stairs() && this.menu.getMaxBranches() > 0) {
					GuiPanels.scrollingText(graphics, this.font, (info.branches() == 0
						? Component.translatable("gui.workingvillagers.tunnel.branches.none")
						: Component.translatable("gui.workingvillagers.tunnel.branches", info.branches())).getString(),
						8, this.settingRowY(3) + textY, COLUMN_WIDTH - 2 * BIG - 6, GuiPanels.TEXT_COLOR);
				}
			}
			default -> { }
		}
		if (this.isBench() || this.hasProductGrid()) {
			int stock = this.menu.getStock();
			Component text = this.isKiln() && this.menu.getOrder().isEmpty()
				? Component.translatable("gui.workingvillagers.kiln.stock.no_order")
				: stock == 0
				? Component.translatable("gui.workingvillagers.station.stock.none")
				: Component.translatable("gui.workingvillagers.station.stock", stock);
			GuiPanels.scrollingText(graphics, this.font, text.getString(), 8, this.stockY, COLUMN_WIDTH - 2 * SMALL - 6, GuiPanels.TEXT_COLOR);
		}
		if (this.type() == StationType.CAULDRON) {
			DyeColor dye = this.menu.getDye();
			ItemStack armor = new ItemStack(Items.LEATHER_CHESTPLATE);
			if (dye != null) {
				armor = DyedItemColor.applyDyes(armor, List.of(dye));
			}
			graphics.item(armor, 8, this.dyeY() - 4);
			Component text = dye == null
				? Component.translatable("gui.workingvillagers.tanner.dye.none")
				: Component.translatable("gui.workingvillagers.tanner.dye", Component.translatable("color.minecraft." + dye.getName()));
			GuiPanels.scrollingText(graphics, this.font, text.getString(), 28, this.dyeY(), COLUMN_WIDTH - 2 * SMALL - 26, GuiPanels.TEXT_COLOR);
		}
	}

	/** The start of the translation keys of the herd setting: the loom's or the smoker's. */
	private String herdKeys() {
		return this.type() == StationType.SMOKER ? "smoker" : "loom";
	}

	/** An explanation in the free space of the settings tab. */
	private void hint(GuiGraphicsExtractor graphics, Component text, int y) {
		for (FormattedCharSequence line : this.limit(this.font.split(text, COLUMN_WIDTH), (HEIGHT - 7 - y) / LINE)) {
			graphics.text(this.font, line, 8, y, GuiPanels.MUTED_COLOR, false);
			y += LINE;
		}
	}

	/** A slot-like cell with an item; selected cells get a green frame, faded ones a veil. */
	private void cell(GuiGraphicsExtractor graphics, int x, int y, ItemStack icon, boolean selected, boolean faded) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, y, 18, 18);
		if (selected) {
			graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF7FB069);
		}
		graphics.item(icon, x + 1, y + 1);
		if (faded) {
			graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xA08B8B8B);
		}
	}

	private String backTooltip() {
		return switch (this.type()) {
			case CARPENTER_BENCH -> "gui.workingvillagers.station.back.tooltip.bench";
			case BLAST_FURNACE -> "gui.workingvillagers.station.back.tooltip.furnace";
			case SMITHING_TABLE -> "gui.workingvillagers.station.back.tooltip.smithing_table";
			case LOOM -> "gui.workingvillagers.station.back.tooltip.loom";
			case SMOKER -> "gui.workingvillagers.station.back.tooltip.smoker";
			case BARREL -> "gui.workingvillagers.station.back.tooltip.barrel";
			case KILN -> "gui.workingvillagers.station.back.tooltip.kiln";
			case BREWING_STAND -> "gui.workingvillagers.station.back.tooltip.brewing_stand";
			case MINING_BENCH -> "gui.workingvillagers.station.back.tooltip.mill";
			default -> "gui.workingvillagers.station.back.tooltip";
		};
	}

	/** A button that changes what the screen shows, without the server. */
	private void localButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int width, String label, Runnable action) {
		GuiPanels.button(graphics, this.font, x, y, width, 14, label, GuiPanels.isOver(mouseX, mouseY, x, y, width, 14));
		this.hotspots.add(new Hotspot(x, y, width, 14, -1, List.of(), action));
	}

	/** Draws a button and remembers it for clicks and its tooltip. */
	private void button(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int width, int height, String label, int id, List<Component> tooltip) {
		boolean hovered = GuiPanels.isOver(mouseX, mouseY, x, y, width, height);
		GuiPanels.button(graphics, this.font, x, y, width, height, label, hovered);
		this.hotspots.add(new Hotspot(x, y, width, height, id, tooltip));
	}

	private static Component shapeName(String shape) {
		return Component.translatable("gui.workingvillagers.shape." + shape);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
		graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, GuiPanels.TEXT_COLOR, false);
		switch (this.tab) {
			case WORK -> this.extractWorkLabels(graphics);
			case WAGE -> this.extractWageLabels(graphics);
			case CHESTS -> this.extractChestLabels(graphics);
			case ORDER -> this.extractOrderLabels(graphics);
			case BLOCK -> { }
		}
	}

	/** Stonecutter: above the grid, the open family or which page of the families. */
	private Component stoneLabel() {
		List<StoneFamilies.Family> families = this.menu.getFamilies();
		if (this.openFamily >= 0 && this.openFamily < families.size()) {
			return new ItemStack(families.get(this.openFamily).icon()).getHoverName();
		}
		int pages = this.familyPages(families);
		return pages > 1
			? Component.translatable("gui.workingvillagers.station.stone.families.page", this.familyPage + 1, pages)
			: Component.translatable("gui.workingvillagers.station.stone.families");
	}

	/** Below the status: the logs or crops of today, or the order. */
	private Optional<Component> secondLine() {
		if (this.type() == StationType.COMPOSTER) {
			int crops = this.menu.getLogsToday();
			if (this.menu.getWorker().isEmpty()) {
				return Optional.of(Component.translatable("gui.workingvillagers.chopping_block.hire"));
			}
			return crops < 0 ? Optional.empty() : Optional.of(Component.translatable("gui.workingvillagers.composter.crops", crops));
		}
		if (this.type() == StationType.PIT_STAKE || this.type() == StationType.MINING_BENCH) {
			int blocks = this.menu.getLogsToday();
			if (this.menu.getWorker().isEmpty()) {
				return Optional.of(Component.translatable("gui.workingvillagers.chopping_block.hire"));
			}
			return blocks < 0 ? Optional.empty() : Optional.of(Component.translatable("gui.workingvillagers.pit.today", blocks, this.menu.getDailyLogs()));
		}
		if (this.type() == StationType.CAULDRON && this.menu.getTanning() > 0) {
			int minutes = Math.max(1, (this.menu.getTannedIn() + 1199) / 1200);
			return Optional.of(this.menu.getTannedIn() > 0
				? Component.translatable("gui.workingvillagers.tanner.tanning", this.menu.getTanning(), minutes)
				: Component.translatable("gui.workingvillagers.tanner.tanned", this.menu.getTanning()));
		}
		if ((this.type() == StationType.FLETCHING_TABLE || this.type() == StationType.BREWING_STAND || this.type() == StationType.CAULDRON)
			&& !this.menu.getOrder().isEmpty()
			&& this.menu.getLogsToday() >= 0) {
			return Optional.of(Component.translatable(this.gridKeys() + ".today", this.menu.getOrder().getHoverName(), this.menu.getLogsToday()));
		}
		if (this.type() == StationType.TARGET) {
			int kills = this.menu.getLogsToday();
			if (this.menu.getWorker().isEmpty()) {
				return Optional.of(Component.translatable("gui.workingvillagers.chopping_block.hire"));
			}
			if (kills < 0) {
				return Optional.empty();
			}
			int experience = this.menu.getExperience();
			return Optional.of(experience > 0
				? Component.translatable("gui.workingvillagers.target.kills_experience", kills, experience)
				: Component.translatable("gui.workingvillagers.target.kills", kills));
		}
		if (this.type() == StationType.BARREL) {
			int catches = this.menu.getLogsToday();
			if (this.menu.getWorker().isEmpty()) {
				return Optional.of(Component.translatable("gui.workingvillagers.chopping_block.hire"));
			}
			return catches < 0 ? Optional.empty() : Optional.of(Component.translatable("gui.workingvillagers.barrel.today", catches));
		}
		if (this.type() == StationType.LOOM || this.type() == StationType.SMOKER) {
			int done = this.menu.getLogsToday();
			if (this.menu.getWorker().isEmpty()) {
				return Optional.of(Component.translatable("gui.workingvillagers.chopping_block.hire"));
			}
			return done < 0 ? Optional.empty()
				: Optional.of(Component.translatable(this.type() == StationType.LOOM ? "gui.workingvillagers.loom.wool" : "gui.workingvillagers.smoker.today", done));
		}
		if (this.type() == StationType.TRADING_COUNTER) {
			int earned = this.menu.getLogsToday();
			if (this.menu.getWorker().isEmpty()) {
				return Optional.of(Component.translatable("gui.workingvillagers.chopping_block.hire"));
			}
			return earned < 0 ? Optional.empty() : Optional.of(Component.translatable("gui.workingvillagers.trading_counter.today", earned));
		}
		if (this.isKiln()) {
			ItemStack order = this.menu.getOrder();
			return Optional.of(order.isEmpty()
				? Component.translatable("gui.workingvillagers.kiln.everything")
				: Component.translatable("gui.workingvillagers.order", order.getHoverName()));
		}
		if (this.type() == StationType.BLAST_FURNACE) {
			return Optional.of(Component.translatable(this.menu.getWorker().isEmpty()
				? "gui.workingvillagers.chopping_block.hire"
				: "gui.workingvillagers.blast_furnace.smelts"));
		}
		if (this.type() == StationType.SMITHING_TABLE) {
			return Optional.of(Component.translatable(this.menu.getWorker().isEmpty()
				? "gui.workingvillagers.chopping_block.hire"
				: "gui.workingvillagers.smithing_table.repairs"));
		}
		if (this.type() == StationType.CHOPPING_BLOCK) {
			int logs = this.menu.getLogsToday();
			if (this.menu.getWorker().isEmpty()) {
				return Optional.of(Component.translatable("gui.workingvillagers.chopping_block.hire"));
			}
			if (logs < 0) {
				return Optional.empty();
			}
			int daily = this.menu.getDailyLogs();
			Component today = Component.translatable("gui.workingvillagers.chopping_block.logs", logs, daily);
			return Optional.of(logs >= daily
				? today.copy().append(" ").append(Component.translatable("gui.workingvillagers.chopping_block.done"))
				: today);
		}
		ItemStack order = this.menu.getOrder();
		if (order.isEmpty()) {
			return Optional.of(Component.translatable("gui.workingvillagers.station.no_order").withStyle(ChatFormatting.DARK_RED));
		}
		if (this.isBench() && this.menu.getShape() == null && this.menu.getWoods().isEmpty()) {
			return Optional.of(Component.translatable("gui.workingvillagers.station.no_woods").withStyle(ChatFormatting.DARK_RED));
		}
		if (!this.type().hasWoods() || this.menu.getShape() == null) {
			return Optional.of(Component.translatable("gui.workingvillagers.order", order.getHoverName()));
		}
		if (WoodTypes.STICK.equals(this.menu.getShape()) || order.is(Items.STICK)) {
			return Optional.of(this.menu.getWoods().isEmpty()
				? Component.translatable("gui.workingvillagers.station.no_woods").withStyle(ChatFormatting.DARK_RED)
				: Component.translatable("gui.workingvillagers.station.order_woods", shapeName(WoodTypes.STICK), this.menu.getWoods().size()));
		}
		int woods = this.menu.getWoods().size();
		return Optional.of(woods == 0
			? Component.translatable("gui.workingvillagers.station.no_woods").withStyle(ChatFormatting.DARK_RED)
			: Component.translatable("gui.workingvillagers.station.order_woods", shapeName(this.menu.getShape()), woods));
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		for (Hotspot hotspot : this.hotspots) {
			if (hotspot.contains(mouseX, mouseY) && !hotspot.tooltip().isEmpty()) {
				graphics.setComponentTooltipForNextFrame(this.font, hotspot.tooltip(), mouseX, mouseY);
				return;
			}
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		for (Hotspot hotspot : this.hotspots) {
			if (hotspot.action() != null && hotspot.contains(event.x(), event.y())) {
				this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				hotspot.action().run();
				return true;
			}
			if (hotspot.button() >= 0 && hotspot.contains(event.x(), event.y())) {
				this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, hotspot.button());
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.tab == Tab.CHESTS && this.menu.getChests().size() > VISIBLE_CHESTS) {
			this.chestScroll = Mth.clamp(this.chestScroll - (int) Math.signum(scrollY), 0, this.menu.getChests().size() - VISIBLE_CHESTS);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
		// Clicks on the tabs are no clicks outside the screen
		return super.hasClickedOutside(mouseX, mouseY, left, top)
			&& !GuiPanels.isOver(mouseX, mouseY, left, top - GuiTabs.TOP_HEIGHT, this.tabs.size() * GuiTabs.TOP_STEP, GuiTabs.TOP_HEIGHT);
	}
}
