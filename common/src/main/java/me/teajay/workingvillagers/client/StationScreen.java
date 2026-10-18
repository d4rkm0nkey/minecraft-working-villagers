package me.teajay.workingvillagers.client;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The overview of a workstation: the worker and the linked chests (role, how full, unlink).
 * At the woodcutter and the carpenter's bench a second column on the left sets the order:
 * the product (or its shape) and the wood types to use; at the bench also how many
 * products to keep in stock. At the stonecutter that column shows the stone families
 * first and, once one is opened, its products. The composter and the blast furnace have
 * no order: the farmer works the fields around it, the smith smelts what it finds.
 */
public class StationScreen extends AbstractContainerScreen<StationMenu> {
	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
	private static final int COLUMN_WIDTH = 162;
	private static final int ROW_HEIGHT = 19;
	private static final int BUTTON_SIZE = 12;
	private static final int SMALL_BUTTON_HEIGHT = 11;
	private static final int BAR_WIDTH = 16;
	private static final int ROLE_WIDTH = 42;
	private static final int GRID_COLUMNS = 9;
	/** Woodcutter and carpenter's bench: where the order column starts. */
	private static final int SHAPES_Y = 77;
	private static final int STATUS_Y = 29;
	private static final int SECOND_LINE_Y = 40;
	/** The slots with the worker's emeralds, tool and seeds or saplings. */
	private static final int SUPPLIES_Y = 49;
	/** Stonecutter: the grid of families or products, as many rows as fit next to the chests. */
	private static final int STONE_CELLS = GRID_COLUMNS * 6;
	/** Chopping block: the row with the strip switch. */
	private static final int STRIP_Y = 71;

	private final boolean orderColumn;
	/** Where the wood types start, below the products. */
	private final int woodsLabelY;
	private final int woodsY;
	/** Carpenter's bench: the row with the stock. */
	private final int stockY;
	/** Left edge of the chest column. */
	private final int chestsX;
	/** Top of the chest list. */
	private final int chestsY;
	private final List<Hotspot> hotspots = new ArrayList<>();
	/** Stonecutter: the index of the family whose products are shown, -1 for the families. */
	private int openFamily = -1;
	private int familyPage;
	/** Stonecutter: whether the family of the order was opened when the families arrived. */
	private boolean familiesShown;

	public StationScreen(StationMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, hasOrderColumn(menu) ? 344 : 176, chestsY(menu) + Workstations.MAX_CHESTS * ROW_HEIGHT + 25);
		this.orderColumn = hasOrderColumn(menu);
		this.chestsX = this.orderColumn ? 175 : 7;
		this.chestsY = chestsY(menu);
		int productRows = menu.type() == StationType.CARPENTER_BENCH ? 3 : 2;
		this.woodsLabelY = SHAPES_Y + productRows * 18 + 5;
		this.woodsY = this.woodsLabelY + 11;
		this.stockY = this.woodsY + 2 * 18 + 5;
	}

	private static boolean hasOrderColumn(StationMenu menu) {
		return menu.type().hasOrder();
	}

	private boolean isStonecutter() {
		return this.menu.type() == StationType.STONECUTTER;
	}

	private boolean isBench() {
		return this.menu.type() == StationType.CARPENTER_BENCH;
	}

	private static int chestsY(StationMenu menu) {
		return hasOrderColumn(menu) ? 29 : 95;
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
		super.extractBackground(graphics, mouseX, mouseY, a);
		this.hotspots.clear();
		int left = this.leftPos;
		int top = this.topPos;
		GuiPanels.panel(graphics, left, top, this.imageWidth, this.imageHeight);

		if (this.menu.type().isProcessing()) {
			String back = Component.translatable("gui.workingvillagers.station.back").getString();
			int width = this.font.width(back) + 10;
			this.button(graphics, mouseX, mouseY, left + this.imageWidth - 7 - width, top + 3, width, BUTTON_SIZE, back, StationMenu.BACK_BUTTON,
				List.of(Component.translatable(this.backTooltip())));
		}
		if (this.orderColumn) {
			this.extractOrder(graphics, mouseX, mouseY);
		}
		if (this.menu.type() == StationType.CHOPPING_BLOCK) {
			this.extractStrip(graphics, mouseX, mouseY);
		}
		this.extractChests(graphics, mouseX, mouseY);
		this.extractWorkerButton(graphics, mouseX, mouseY);
		this.extractSupplies(graphics);
		for (Hotspot hotspot : this.hotspots) {
			if (hotspot.contains(mouseX, mouseY)) {
				graphics.requestCursor(CursorTypes.POINTING_HAND);
			}
		}
	}

	private void extractOrder(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (this.isStonecutter()) {
			this.extractStoneOrder(graphics, mouseX, mouseY);
			return;
		}
		int left = this.leftPos + 8;
		String shape = this.menu.getShape();
		if (this.isBench()) {
			this.extractProducts(graphics, left, shape);
			this.extractStock(graphics, mouseX, mouseY);
		}
		for (int i = 0; !this.isBench() && i < WoodTypes.SHAPES.size(); i++) {
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

		Set<String> woods = this.menu.getWoods();
		Set<String> available = this.menu.getAvailableWoods();
		String all = Component.translatable("gui.workingvillagers.station.woods.all").getString();
		String none = Component.translatable("gui.workingvillagers.station.woods.none").getString();
		int noneWidth = this.font.width(none) + 8;
		int allWidth = this.font.width(all) + 8;
		int right = this.leftPos + 7 + COLUMN_WIDTH;
		int buttonY = this.topPos + this.woodsLabelY - 2;
		this.button(graphics, mouseX, mouseY, right - noneWidth, buttonY, noneWidth, SMALL_BUTTON_HEIGHT, none, StationMenu.NO_WOODS_BUTTON, List.of());
		this.button(graphics, mouseX, mouseY, right - noneWidth - 2 - allWidth, buttonY, allWidth, SMALL_BUTTON_HEIGHT, all, StationMenu.ALL_WOODS_BUTTON, List.of());

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
		int right = this.leftPos + 7 + COLUMN_WIDTH;
		int buttonY = top - 12;

		if (this.openFamily >= 0) {
			String back = Component.translatable("gui.workingvillagers.station.stone.back").getString();
			int width = this.font.width(back) + 8;
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
			this.localButton(graphics, mouseX, mouseY, right - BUTTON_SIZE, buttonY, BUTTON_SIZE, ">",
				() -> this.familyPage = Math.min(this.familyPage + 1, pages - 1));
			this.localButton(graphics, mouseX, mouseY, right - 2 * BUTTON_SIZE - 2, buttonY, BUTTON_SIZE, "<",
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
	private void extractProducts(GuiGraphicsExtractor graphics, int left, String shape) {
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

	/** Carpenter's bench: how many products to keep in the output chests, with buttons to change it. */
	private void extractStock(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int right = this.leftPos + 7 + COLUMN_WIDTH;
		int y = this.topPos + this.stockY - 2;
		List<Component> tooltip = List.of(
			Component.translatable("gui.workingvillagers.station.stock.tooltip"),
			Component.translatable("gui.workingvillagers.station.stock.tooltip2").withStyle(ChatFormatting.GRAY)
		);
		this.button(graphics, mouseX, mouseY, right - BUTTON_SIZE, y, BUTTON_SIZE, BUTTON_SIZE, "+", StationMenu.STOCK_MORE_BUTTON, tooltip);
		this.button(graphics, mouseX, mouseY, right - 2 * BUTTON_SIZE - 2, y, BUTTON_SIZE, BUTTON_SIZE, "-", StationMenu.STOCK_LESS_BUTTON, tooltip);
		this.hotspots.add(new Hotspot(this.leftPos + 7, y, COLUMN_WIDTH - 2 * BUTTON_SIZE - 4, BUTTON_SIZE, -1, tooltip));
	}

	/** Chopping block: the switch whether the lumberjack strips the logs. */
	private void extractStrip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int width = this.stripButtonWidth();
		int y = this.topPos + STRIP_Y - 2;
		List<Component> tooltip = List.of(
			Component.translatable("gui.workingvillagers.chopping_block.strip.tooltip"),
			Component.translatable("gui.workingvillagers.chopping_block.strip.tooltip2").withStyle(ChatFormatting.GRAY),
			Component.translatable("gui.workingvillagers.station.click_toggle").withStyle(ChatFormatting.DARK_GRAY)
		);
		this.button(graphics, mouseX, mouseY, this.leftPos + 7 + COLUMN_WIDTH - width, y, width, BUTTON_SIZE, this.stripLabel().getString(),
			StationMenu.STRIP_BUTTON, tooltip);
		this.hotspots.add(new Hotspot(this.leftPos + 7, y, COLUMN_WIDTH - width - 4, BUTTON_SIZE, -1, tooltip));
	}

	private Component stripLabel() {
		return Component.translatable(this.menu.getStrip() ? "gui.workingvillagers.chopping_block.strip.on" : "gui.workingvillagers.chopping_block.strip.off");
	}

	private int stripButtonWidth() {
		return Math.max(this.font.width(Component.translatable("gui.workingvillagers.chopping_block.strip.on")),
			this.font.width(Component.translatable("gui.workingvillagers.chopping_block.strip.off"))) + 10;
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

	private void extractChests(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int left = this.leftPos + this.chestsX;
		int top = this.topPos + this.chestsY;
		boolean processing = this.menu.type().isProcessing();
		List<StationMenu.Chest> chests = this.menu.getChests();
		if (chests.isEmpty()) {
			int y = top + 4;
			for (FormattedCharSequence line : this.font.split(this.noChestsWarning(), COLUMN_WIDTH - 4)) {
				graphics.text(this.font, line, left + 2, y, GuiPanels.WARNING_COLOR, false);
				y += 10;
			}
		}
		for (int i = 0; i < chests.size(); i++) {
			StationMenu.Chest chest = chests.get(i);
			int y = top + i * ROW_HEIGHT;
			GuiPanels.inset(graphics, left, y, COLUMN_WIDTH, ROW_HEIGHT - 1, 0xFFB4B4B4);
			BlockState state = this.minecraft.level.getBlockState(chest.pos());
			if (!state.isAir()) {
				graphics.item(new ItemStack(state.getBlock()), left + 2, y + 1);
			}
			int buttonX = left + COLUMN_WIDTH - BUTTON_SIZE - 3;
			int barX = buttonX - 4 - BAR_WIDTH;
			int roleX = barX - 4 - ROLE_WIDTH;
			int textWidth = (processing ? roleX : barX) - (left + 20) - 3;
			GuiPanels.scrollingText(graphics, this.font, chest.name().getString(), left + 20, y + 5, textWidth,
				chest.named() ? GuiPanels.TEXT_COLOR : GuiPanels.MUTED_COLOR);

			List<Component> rowTooltip = this.chestTooltip(chest, state);
			this.hotspots.add(new Hotspot(left, y, roleX - left, ROW_HEIGHT - 1, -1, rowTooltip));
			if (processing) {
				String role = Component.translatable(chest.input() ? "gui.workingvillagers.station.input" : "gui.workingvillagers.station.output").getString();
				this.button(graphics, mouseX, mouseY, roleX, y + 3, ROLE_WIDTH, BUTTON_SIZE, role, StationMenu.ROLE_BUTTON + i, List.of(
					Component.translatable(chest.input() ? "gui.workingvillagers.station.input.tooltip" : "gui.workingvillagers.station.output.tooltip"),
					Component.translatable("gui.workingvillagers.station.click_toggle").withStyle(ChatFormatting.DARK_GRAY)
				));
			}

			// How full the chest is
			int barY = y + 7;
			float fill = chest.size() == 0 ? 0.0F : (float) chest.used() / chest.size();
			graphics.fill(barX - 1, barY - 1, barX + BAR_WIDTH + 1, barY + 4, 0xFF373737);
			graphics.fill(barX, barY, barX + BAR_WIDTH, barY + 3, 0xFF8B8B8B);
			graphics.fill(barX, barY, barX + Math.round(BAR_WIDTH * fill), barY + 3, fill >= 1.0F ? 0xFFC03030 : 0xFF50A040);
			this.hotspots.add(new Hotspot(barX - 1, y, BAR_WIDTH + 2, ROW_HEIGHT - 1, -1, rowTooltip));

			this.button(graphics, mouseX, mouseY, buttonX, y + 3, BUTTON_SIZE, BUTTON_SIZE, "×", StationMenu.UNLINK_BUTTON + i,
				List.of(Component.translatable("gui.workingvillagers.station.unlink")));
		}

		// Buttons to link more chests by clicking them in the world
		int y = top + Workstations.MAX_CHESTS * ROW_HEIGHT + 4;
		List<Component> tooltip = List.of(Component.translatable("gui.workingvillagers.station.add_chests.tooltip"));
		if (processing) {
			int width = (COLUMN_WIDTH - 2) / 2;
			this.button(graphics, mouseX, mouseY, left, y, width, 14, Component.translatable("gui.workingvillagers.station.add_inputs").getString(),
				StationMenu.ADD_INPUTS_BUTTON, tooltip);
			this.button(graphics, mouseX, mouseY, left + width + 2, y, width, 14, Component.translatable("gui.workingvillagers.station.add_outputs").getString(),
				StationMenu.ADD_OUTPUTS_BUTTON, tooltip);
		} else {
			this.button(graphics, mouseX, mouseY, left, y, COLUMN_WIDTH, 14, Component.translatable("gui.workingvillagers.station.add_chests").getString(),
				StationMenu.ADD_OUTPUTS_BUTTON, tooltip);
		}
	}

	/** The button next to the worker: assign one if the station is free, take it away otherwise. */
	private void extractWorkerButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		boolean free = this.menu.getWorker().isEmpty();
		if (!free && !this.menu.hasHiredWorker()) {
			return;
		}
		String label = Component.translatable(free ? "gui.workingvillagers.station.assign" : "gui.workingvillagers.station.unassign").getString();
		int width = this.workerButtonWidth();
		this.button(graphics, mouseX, mouseY, this.leftPos + 7 + COLUMN_WIDTH - width, this.topPos + 16, width, 12, label,
			free ? StationMenu.ASSIGN_BUTTON : StationMenu.UNASSIGN_BUTTON,
			List.of(Component.translatable(free ? "gui.workingvillagers.station.assign.tooltip" : "gui.workingvillagers.station.unassign.tooltip")));
	}

	/** What the worker carries for its work: emeralds, tool, seeds or saplings. */
	private void extractSupplies(GuiGraphicsExtractor graphics) {
		this.menu.getSupplies().ifPresent(info -> {
			int y = this.topPos + SUPPLIES_Y;
			SuppliesRow.extractSlots(graphics, this.font, info, this.leftPos + 7, y);
			this.hotspots.add(new Hotspot(this.leftPos + 7, y, COLUMN_WIDTH, SuppliesRow.SLOT, -1, SuppliesRow.tooltip(info)));
		});
	}

	private int workerButtonWidth() {
		boolean free = this.menu.getWorker().isEmpty();
		if (!free && !this.menu.hasHiredWorker()) {
			return 0;
		}
		return this.font.width(Component.translatable(free ? "gui.workingvillagers.station.assign" : "gui.workingvillagers.station.unassign")) + 10;
	}

	private String backTooltip() {
		return switch (this.menu.type()) {
			case CARPENTER_BENCH -> "gui.workingvillagers.station.back.tooltip.bench";
			case BLAST_FURNACE -> "gui.workingvillagers.station.back.tooltip.furnace";
			default -> "gui.workingvillagers.station.back.tooltip";
		};
	}

	private Component noChestsWarning() {
		return Component.translatable(switch (this.menu.type()) {
			case CHOPPING_BLOCK -> "gui.workingvillagers.chopping_block.no_chests";
			case COMPOSTER -> "gui.workingvillagers.composter.no_chests";
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

	/** Stonecutter: a small button that changes what the screen shows, without the server. */
	private void localButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int width, String label, Runnable action) {
		GuiPanels.button(graphics, this.font, x, y, width, SMALL_BUTTON_HEIGHT, label, GuiPanels.isOver(mouseX, mouseY, x, y, width, SMALL_BUTTON_HEIGHT));
		this.hotspots.add(new Hotspot(x, y, width, SMALL_BUTTON_HEIGHT, -1, List.of(), action));
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
		Component worker = this.menu.getWorker()
			.map(name -> (Component) Component.translatable("gui.workingvillagers.worker", name))
			.orElse(Component.translatable("gui.workingvillagers.worker.none"));
		GuiPanels.scrollingText(graphics, this.font, worker.getString(), 8, 18, COLUMN_WIDTH - this.workerButtonWidth() - 4, GuiPanels.TEXT_COLOR);
		this.menu.getStatus().ifPresent(status -> GuiPanels.scrollingText(graphics, this.font,
			Component.translatable("gui.workingvillagers.status", status).getString(), 8, STATUS_Y, COLUMN_WIDTH, GuiPanels.STATUS_COLOR));
		this.secondLine().ifPresent(line -> GuiPanels.scrollingText(graphics, this.font, line.getString(), 8, SECOND_LINE_Y, COLUMN_WIDTH,
			line.getStyle().getColor() != null ? 0xFF000000 | line.getStyle().getColor().getValue() : GuiPanels.MUTED_COLOR));

		this.menu.getSupplies().ifPresent(info -> {
			int x = 7 + SuppliesRow.slotsWidth(info) + 4;
			GuiPanels.scrollingText(graphics, this.font, SuppliesRow.line(info).getString(), x, SUPPLIES_Y + 5, COLUMN_WIDTH - x + 7, SuppliesRow.color(info));
		});

		if (this.isStonecutter()) {
			GuiPanels.scrollingText(graphics, this.font, this.stoneLabel().getString(), 8, SHAPES_Y - 10, COLUMN_WIDTH - 40, GuiPanels.TEXT_COLOR);
		} else if (this.orderColumn) {
			graphics.text(this.font, Component.translatable("gui.workingvillagers.station.shape"), 8, SHAPES_Y - 10, GuiPanels.TEXT_COLOR, false);
			graphics.text(this.font, Component.translatable(this.isBench() ? "gui.workingvillagers.station.woods.bench" : "gui.workingvillagers.station.woods"),
				8, this.woodsLabelY, GuiPanels.TEXT_COLOR, false);
		}
		if (this.menu.type() == StationType.CHOPPING_BLOCK) {
			GuiPanels.scrollingText(graphics, this.font, Component.translatable("gui.workingvillagers.chopping_block.strip").getString(), 8, STRIP_Y,
				COLUMN_WIDTH - this.stripButtonWidth() - 4, GuiPanels.TEXT_COLOR);
		}
		if (this.isBench()) {
			int stock = this.menu.getStock();
			Component text = stock == 0
				? Component.translatable("gui.workingvillagers.station.stock.none")
				: Component.translatable("gui.workingvillagers.station.stock", stock);
			GuiPanels.scrollingText(graphics, this.font, text.getString(), 8, this.stockY, COLUMN_WIDTH - 2 * BUTTON_SIZE - 6, GuiPanels.TEXT_COLOR);
		}
		Component chests = Component.translatable("gui.workingvillagers.station.chests", this.menu.getChests().size(), this.menu.getMaxChests());
		graphics.text(this.font, chests, this.chestsX + 1, this.chestsY - 11, GuiPanels.TEXT_COLOR, false);
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

	/** Below the worker: the logs or crops of today, or the order. */
	private Optional<Component> secondLine() {
		if (this.menu.type() == StationType.COMPOSTER) {
			int crops = this.menu.getLogsToday();
			if (this.menu.getWorker().isEmpty()) {
				return Optional.of(Component.translatable("gui.workingvillagers.chopping_block.hire"));
			}
			return crops < 0 ? Optional.empty() : Optional.of(Component.translatable("gui.workingvillagers.composter.crops", crops));
		}
		if (this.menu.type() == StationType.BLAST_FURNACE) {
			return Optional.of(Component.translatable(this.menu.getWorker().isEmpty()
				? "gui.workingvillagers.chopping_block.hire"
				: "gui.workingvillagers.blast_furnace.smelts"));
		}
		if (this.menu.type() == StationType.CHOPPING_BLOCK) {
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
		if (!this.orderColumn || this.menu.getShape() == null) {
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
}
