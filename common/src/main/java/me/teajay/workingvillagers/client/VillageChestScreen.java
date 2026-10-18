package me.teajay.workingvillagers.client;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.ArrayList;
import java.util.List;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.menu.VillageChestMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;

/**
 * The village chest's screen, with two tabs left of it (a double chest has no room for tabs
 * above it):
 * <ul>
 * <li><b>Chest</b>: the chest, like vanilla.</li>
 * <li><b>Workers</b>: the workers it serves, each with its face, its name and its job or
 *     what it lacks, page by page; below them the worn tools given back and for how long
 *     the emeralds pay the wages.</li>
 * </ul>
 */
public class VillageChestScreen extends ContainerScreen {
	/** Two lines per worker: its name, then its job or what it lacks. */
	private static final int ROW_HEIGHT = 21;
	private static final int TEXT_X = 28;
	private static final int HEADER_Y = 18;
	private static final int ROWS_Y = 31;
	/** Below the workers: the lines about worn tools and the wage. */
	private static final int FOOTER_HEIGHT = 30;
	private static final int PAGE_BUTTON = 12;

	private enum Tab { CHEST, WORKERS }

	private final VillageChestMenu villageChest;
	private Tab tab = Tab.CHEST;
	private int page;

	public VillageChestScreen(ChestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.villageChest = (VillageChestMenu) menu;
	}

	/** Shows a tab (also for the screenshots of the menus). */
	public void showTab(String name) {
		for (Tab candidate : Tab.values()) {
			if (candidate.name().equalsIgnoreCase(name)) {
				this.tab = candidate;
			}
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		this.villageChest.showSlots(this.tab == Tab.CHEST);
		super.extractBackground(graphics, mouseX, mouseY, a);
		// The chest is drawn with the background, so the other tab only goes next to it
		for (Tab shown : Tab.values()) {
			if (shown != this.tab) {
				graphics.enableScissor(this.leftPos - GuiTabs.SIDE_WIDTH, this.topPos, this.leftPos, this.topPos + this.imageHeight);
				this.extractTab(graphics, shown, false);
				graphics.disableScissor();
				if (this.isOverTab(mouseX, mouseY, shown)) {
					graphics.requestCursor(CursorTypes.POINTING_HAND);
				}
			}
		}
		this.extractTab(graphics, this.tab, true);
		if (this.tab == Tab.WORKERS) {
			// The chest's slots and labels covered, the frame and the title kept
			graphics.fill(this.leftPos + 4, this.topPos + 16, this.leftPos + this.imageWidth - 4, this.topPos + this.imageHeight - 4, 0xFFC6C6C6);
			this.extractWorkers(graphics, mouseX, mouseY);
		}
	}

	// --- Tabs ---

	private void extractTab(GuiGraphicsExtractor graphics, Tab shown, boolean selected) {
		int index = shown.ordinal();
		int y = GuiTabs.sideY(this.topPos, index);
		GuiTabs.side(graphics, this.leftPos, this.topPos, index, selected);
		int iconX = GuiTabs.sideIconX(this.leftPos, selected);
		if (shown == Tab.CHEST) {
			graphics.item(new ItemStack(WorkingVillagers.VILLAGE_CHEST_ITEM.get()), iconX, y + 6);
		} else {
			List<VillageChestMenu.Worker> workers = this.villageChest.getWorkers();
			WorkerButton.face(graphics, iconX, y + 6,
				workers.isEmpty() ? WorkingVillagers.LUMBERJACK_PROFESSION : workers.getFirst().profession(), !workers.isEmpty());
			if (this.workersProblem()) {
				GuiTabs.problem(graphics, this.leftPos - GuiTabs.SIDE_WIDTH + 26, y);
			}
		}
	}

	private boolean isOverTab(double mouseX, double mouseY, Tab shown) {
		return GuiPanels.isOver(mouseX, mouseY, this.leftPos - GuiTabs.SIDE_WIDTH, GuiTabs.sideY(this.topPos, shown.ordinal()),
			GuiTabs.SIDE_WIDTH, GuiTabs.SIDE_HEIGHT);
	}

	/** Some worker lacks something no village chest has, or the emeralds run out today. */
	private boolean workersProblem() {
		return this.villageChest.isEnabled() && (this.villageChest.getWorkers().stream().anyMatch(worker -> worker.missing().isPresent())
			|| this.villageChest.getWageDays().map(days -> days < 1).orElse(false));
	}

	private List<Component> tabTooltip(Tab shown) {
		if (shown == Tab.CHEST) {
			return List.of(Component.translatable("gui.workingvillagers.tab.chest"));
		}
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("gui.workingvillagers.tab.workers"));
		lines.add(Component.translatable("gui.workingvillagers.village_chest.serves", this.villageChest.getWorkers().size()).withStyle(ChatFormatting.GRAY));
		long lacking = this.villageChest.getWorkers().stream().filter(worker -> worker.missing().isPresent()).count();
		if (lacking > 0) {
			lines.add(Component.translatable("gui.workingvillagers.village_chest.lacking", lacking).withStyle(ChatFormatting.RED));
		}
		return lines;
	}

	// --- Workers tab ---

	/** How many workers fit on one page. */
	private int rowsPerPage() {
		return (this.imageHeight - ROWS_Y - FOOTER_HEIGHT) / ROW_HEIGHT;
	}

	private int pages() {
		return Math.max(1, Mth.positiveCeilDiv(this.villageChest.getWorkers().size(), this.rowsPerPage()));
	}

	private void extractWorkers(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int x = this.leftPos + 8;
		int textWidth = this.imageWidth - 16;
		if (!this.villageChest.isEnabled()) {
			this.paragraph(graphics, Component.translatable("gui.workingvillagers.village_chest.disabled"), x, this.topPos + HEADER_Y, textWidth, GuiPanels.WARNING_COLOR);
			return;
		}
		List<VillageChestMenu.Worker> workers = this.villageChest.getWorkers();
		graphics.text(this.font, Component.translatable("gui.workingvillagers.village_chest.serves", workers.size()), x, this.topPos + HEADER_Y,
			GuiPanels.TEXT_COLOR, false);
		if (workers.isEmpty()) {
			this.paragraph(graphics, Component.translatable("gui.workingvillagers.village_chest.none", this.villageChest.getRadius()), x,
				this.topPos + ROWS_Y, textWidth, GuiPanels.MUTED_COLOR);
			return;
		}
		this.page = Mth.clamp(this.page, 0, this.pages() - 1);
		if (this.pages() > 1) {
			this.pageButton(graphics, mouseX, mouseY, this.previousX(), "‹", this.page > 0);
			this.pageButton(graphics, mouseX, mouseY, this.nextX(), "›", this.page < this.pages() - 1);
			String label = (this.page + 1) + "/" + this.pages();
			graphics.text(this.font, label, this.previousX() - 3 - this.font.width(label), this.topPos + HEADER_Y, GuiPanels.MUTED_COLOR, false);
		}
		int rows = this.rowsPerPage();
		int y = this.topPos + ROWS_Y;
		for (int i = this.page * rows; i < Math.min(workers.size(), (this.page + 1) * rows); i++) {
			VillageChestMenu.Worker worker = workers.get(i);
			WorkerButton.face(graphics, x, y + 1, worker.profession(), true);
			int width = textWidth - TEXT_X + 8;
			// Without a name of its own the job is the name; the line below it then only says what it lacks
			Component name = worker.name().orElse(worker.job());
			boolean twoLines = worker.missing().isPresent() || worker.name().isPresent();
			GuiPanels.scrollingText(graphics, this.font, name.getString(), x + TEXT_X - 8, twoLines ? y : y + 5, width, GuiPanels.TEXT_COLOR);
			if (worker.missing().isPresent()) {
				GuiPanels.scrollingText(graphics, this.font, Component.translatable("gui.workingvillagers.supplies.no_" + worker.missing().get()).getString(),
					x + TEXT_X - 8, y + 10, width, GuiPanels.WARNING_COLOR);
			} else if (worker.name().isPresent()) {
				GuiPanels.scrollingText(graphics, this.font, worker.job().getString(), x + TEXT_X - 8, y + 10, width, GuiPanels.MUTED_COLOR);
			}
			y += ROW_HEIGHT;
		}
		int footer = this.topPos + this.imageHeight - FOOTER_HEIGHT + 4;
		int worn = this.villageChest.getWornTools();
		if (worn > 0) {
			GuiPanels.scrollingText(graphics, this.font, Component.translatable("gui.workingvillagers.village_chest.worn", worn).getString(),
				x, footer, textWidth, GuiPanels.MUTED_COLOR);
		}
		this.villageChest.getWageDays().ifPresent(days -> GuiPanels.scrollingText(graphics, this.font,
			Component.translatable("gui.workingvillagers.village_chest.wage_days", days).getString(), x, footer + 10,
			textWidth, days < 1 ? GuiPanels.WARNING_COLOR : GuiPanels.MUTED_COLOR));
	}

	private void paragraph(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int color) {
		for (FormattedCharSequence line : this.font.split(text, width)) {
			graphics.text(this.font, line, x, y, color, false);
			y += 10;
		}
	}

	private int nextX() {
		return this.leftPos + this.imageWidth - 7 - PAGE_BUTTON;
	}

	private int previousX() {
		return this.nextX() - PAGE_BUTTON - 2;
	}

	private int pageButtonY() {
		return this.topPos + HEADER_Y - 2;
	}

	private void pageButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, String label, boolean enabled) {
		boolean hovered = enabled && GuiPanels.isOver(mouseX, mouseY, x, this.pageButtonY(), PAGE_BUTTON, PAGE_BUTTON);
		if (!enabled) {
			GuiPanels.disabledButton(graphics, this.font, x, this.pageButtonY(), PAGE_BUTTON, PAGE_BUTTON, label);
			return;
		}
		GuiPanels.button(graphics, this.font, x, this.pageButtonY(), PAGE_BUTTON, label, hovered);
		if (hovered) {
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}
	}

	private boolean isOverPageButton(double mouseX, double mouseY, int x) {
		return this.tab == Tab.WORKERS && this.pages() > 1 && GuiPanels.isOver(mouseX, mouseY, x, this.pageButtonY(), PAGE_BUTTON, PAGE_BUTTON);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (this.tab == Tab.CHEST) {
			super.extractLabels(graphics, mouseX, mouseY);
			return;
		}
		// No "Inventory" over the workers
		graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, GuiPanels.TEXT_COLOR, false);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		for (Tab shown : Tab.values()) {
			if (this.isOverTab(mouseX, mouseY, shown)) {
				graphics.setComponentTooltipForNextFrame(this.font, this.tabTooltip(shown), mouseX, mouseY);
				return;
			}
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		for (Tab shown : Tab.values()) {
			if (shown != this.tab && this.isOverTab(event.x(), event.y(), shown)) {
				this.playClick();
				this.tab = shown;
				return true;
			}
		}
		if (this.isOverPageButton(event.x(), event.y(), this.previousX()) && this.page > 0) {
			this.playClick();
			this.page--;
			return true;
		}
		if (this.isOverPageButton(event.x(), event.y(), this.nextX()) && this.page < this.pages() - 1) {
			this.playClick();
			this.page++;
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.tab == Tab.WORKERS && this.pages() > 1) {
			this.page = Mth.clamp(this.page - (int) Math.signum(scrollY), 0, this.pages() - 1);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private void playClick() {
		this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	/** Clicks on the tabs are no clicks outside the screen (which would drop the carried stack). */
	@Override
	protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
		return super.hasClickedOutside(mx, my, xo, yo)
			&& !GuiPanels.isOver(mx, my, this.leftPos - GuiTabs.SIDE_WIDTH, this.topPos, GuiTabs.SIDE_WIDTH, Tab.values().length * GuiTabs.SIDE_HEIGHT);
	}
}
