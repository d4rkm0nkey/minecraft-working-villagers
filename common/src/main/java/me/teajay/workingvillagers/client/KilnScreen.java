package me.teajay.workingvillagers.client;

import java.util.List;
import me.teajay.workingvillagers.menu.CutterMenu;
import me.teajay.workingvillagers.menu.KilnMenu;
import me.teajay.workingvillagers.station.StationType;
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
 * The kiln's screen: the furnace screen and its recipe book without the food tab, with the
 * tabs of the station above it (see {@link StationTabs}).
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
		StationTabs.extractAbove(graphics, this.leftPos, this.topPos, mouseX, mouseY, StationType.KILN, this.menu.workingvillagers$tabFlags());
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		StationTabs.tooltip(graphics, this.font, mouseX, mouseY, this.leftPos, this.topPos, StationType.KILN, this.menu.workingvillagers$tabFlags());
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (StationTabs.click(this.minecraft, this.menu.containerId, CutterMenu.STATION_BUTTON, event.x(), event.y(), this.leftPos, this.topPos,
			StationType.KILN, this.menu.workingvillagers$tabFlags())) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}
}
