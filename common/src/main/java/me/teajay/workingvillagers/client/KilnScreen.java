package me.teajay.workingvillagers.client;

import java.util.List;
import java.util.Optional;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.menu.CutterMenu;
import me.teajay.workingvillagers.menu.KilnMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeBookCategories;

/**
 * The kiln's screen: the furnace screen and its recipe book without the food tab. The button
 * with the kiln operator's face at the top right opens the overview of the station, like in
 * the blast furnace.
 */
public class KilnScreen extends AbstractFurnaceScreen<KilnMenu> {
	private static final Identifier LIT_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
	private static final Identifier BURN_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/burn_progress");
	private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
	private static final Component FILTER_NAME = Component.translatable("gui.recipebook.toggleRecipes.smeltable");
	private static final List<RecipeBookComponent.TabInfo> TABS = List.of(
		new RecipeBookComponent.TabInfo(SearchRecipeBookCategory.FURNACE),
		new RecipeBookComponent.TabInfo(Items.STONE, RecipeBookCategories.FURNACE_BLOCKS),
		new RecipeBookComponent.TabInfo(Items.LAVA_BUCKET, Items.EMERALD, RecipeBookCategories.FURNACE_MISC)
	);

	public KilnScreen(KilnMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, FILTER_NAME, TEXTURE, LIT_PROGRESS_SPRITE, BURN_PROGRESS_SPRITE, TABS);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		WorkerButton.extract(graphics, this.leftPos, this.topPos, mouseX, mouseY, WorkingVillagers.KILN_OPERATOR_PROFESSION, this.menu.workingvillagers$hasWorker());
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		if (this.isOverStation(mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(WorkerButton.tooltip(this.menu.workingvillagers$hasWorker() ? null : WorkerButton.workerLine(Optional.empty())), mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.isOverStation(event.x(), event.y())) {
			WorkerButton.click(this.minecraft, this.menu.containerId, CutterMenu.STATION_BUTTON);
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	private boolean isOverStation(double mouseX, double mouseY) {
		return WorkerButton.isOver(mouseX, mouseY, this.leftPos, this.topPos);
	}
}
