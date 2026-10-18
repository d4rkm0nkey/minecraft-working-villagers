package me.teajay.workingvillagers.client;

import java.util.List;
import me.teajay.workingvillagers.menu.VillageChestMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

/**
 * The village chest's screen: a chest, and to its right a panel with the workers it serves,
 * what one of them lacks, the worn tools given back and for how long the emeralds pay the
 * wages.
 */
public class VillageChestScreen extends ContainerScreen {
	private static final int PANEL_WIDTH = 132;
	private static final int PANEL_GAP = 2;
	/** Two lines per worker: its name, then its job or what it lacks. */
	private static final int ROW_HEIGHT = 21;
	/** Above the workers: the heading; below: the lines about worn tools and the wage. */
	private static final int HEADER_HEIGHT = 20;
	private static final int FOOTER_HEIGHT = 28;
	private static final int OK_COLOR = 0xFF2E6B2E;

	private final VillageChestMenu villageChest;

	public VillageChestScreen(ChestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.villageChest = (VillageChestMenu) menu;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int x = this.panelX();
		int y = this.topPos;
		GuiPanels.panel(graphics, x, y, PANEL_WIDTH, this.imageHeight);
		int textWidth = PANEL_WIDTH - 14;
		x += 7;
		y += 6;
		if (!this.villageChest.isEnabled()) {
			for (var line : this.font.split(Component.translatable("gui.workingvillagers.village_chest.disabled"), textWidth)) {
				graphics.text(this.font, line, x, y, GuiPanels.WARNING_COLOR, false);
				y += 10;
			}
			return;
		}
		List<VillageChestMenu.Worker> workers = this.villageChest.getWorkers();
		graphics.text(this.font, Component.translatable("gui.workingvillagers.village_chest.serves", workers.size()), x, y, GuiPanels.TEXT_COLOR, false);
		y += 14;
		if (workers.isEmpty()) {
			for (var line : this.font.split(Component.translatable("gui.workingvillagers.village_chest.none", this.villageChest.getRadius()), textWidth)) {
				graphics.text(this.font, line, x, y, GuiPanels.MUTED_COLOR, false);
				y += 10;
			}
			return;
		}
		// More workers than fit (six next to a chest, more next to a double chest) are summed up in one line
		int maxRows = (this.imageHeight - HEADER_HEIGHT - FOOTER_HEIGHT) / ROW_HEIGHT;
		int shown = workers.size() > maxRows ? maxRows - 1 : workers.size();
		for (int i = 0; i < shown; i++) {
			VillageChestMenu.Worker worker = workers.get(i);
			boolean ok = worker.missing().isEmpty();
			GuiPanels.scrollingText(graphics, this.font, (ok ? "✔ " : "✘ ") + worker.name().getString(), x, y, textWidth,
				ok ? OK_COLOR : GuiPanels.WARNING_COLOR);
			Component detail = ok ? worker.job() : Component.translatable("gui.workingvillagers.supplies.no_" + worker.missing().get());
			GuiPanels.scrollingText(graphics, this.font, detail.getString(), x + 10, y + 9, textWidth - 10,
				ok ? GuiPanels.MUTED_COLOR : GuiPanels.WARNING_COLOR);
			y += ROW_HEIGHT;
		}
		if (shown < workers.size()) {
			graphics.text(this.font, Component.translatable("gui.workingvillagers.village_chest.more", workers.size() - shown), x, y,
				GuiPanels.MUTED_COLOR, false);
		}
		int worn = this.villageChest.getWornTools();
		if (worn > 0) {
			GuiPanels.scrollingText(graphics, this.font, Component.translatable("gui.workingvillagers.village_chest.worn", worn).getString(),
				this.panelX() + 7, this.topPos + this.imageHeight - 24, textWidth, GuiPanels.MUTED_COLOR);
		}
		this.villageChest.getWageDays().ifPresent(days -> GuiPanels.scrollingText(graphics, this.font,
			Component.translatable("gui.workingvillagers.village_chest.wage_days", days).getString(), this.panelX() + 7, this.topPos + this.imageHeight - 14,
			textWidth, days < 1 ? GuiPanels.WARNING_COLOR : GuiPanels.MUTED_COLOR));
	}

	/** Clicks on the panel are no clicks outside the screen (which would drop the carried stack). */
	@Override
	protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
		return super.hasClickedOutside(mx, my, xo, yo)
			&& !GuiPanels.isOver(mx, my, this.panelX(), this.topPos, PANEL_WIDTH, this.imageHeight);
	}

	private int panelX() {
		return this.leftPos + this.imageWidth + PANEL_GAP;
	}
}
