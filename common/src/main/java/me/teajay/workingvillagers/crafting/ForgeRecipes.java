package me.teajay.workingvillagers.crafting;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.List;
import me.teajay.workingvillagers.WorkingVillagers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * The recipes of the weaponsmith at the grindstone: the vanilla crafting recipes (shaped and
 * shapeless) whose result is in the item tag {@code workingvillagers:forgeable} (axes,
 * pickaxes, shovels, hoes, swords, spears, shears, the mace), except the wooden ones (the
 * carpenter's). Data packs add products by adding them to the tag. The crafting itself works
 * like the carpenter's, see {@link CarpenterRecipes}.
 */
public final class ForgeRecipes {
	/** The recipes of the last recipe manager; a reload makes a new one. */
	private static WeakReference<RecipeManager> cachedFor = new WeakReference<>(null);
	private static List<CraftingRecipe> cached = List.of();

	private ForgeRecipes() {
	}

	/** All recipes of the grindstone, sorted by their result like the creative inventory. */
	public static List<CraftingRecipe> all(ServerLevel level) {
		RecipeManager recipes = level.recipeAccess();
		if (cachedFor.get() != recipes) {
			cached = recipes.getRecipes().stream()
				.map(RecipeHolder::value)
				.filter(recipe -> recipe.getType() == RecipeType.CRAFTING && (recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe))
				.map(recipe -> (CraftingRecipe) recipe)
				.filter(recipe -> !recipe.placementInfo().isImpossibleToPlace() && CarpenterRecipes.preview(recipe).is(WorkingVillagers.FORGEABLE))
				.filter(recipe -> recipe.placementInfo().ingredients().stream().noneMatch(ingredient -> ingredient.items().anyMatch(item -> item.is(ItemTags.PLANKS))))
				.sorted(Comparator.comparingInt(recipe -> BuiltInRegistries.ITEM.getId(CarpenterRecipes.resultOf(recipe))))
				.toList();
			cachedFor = new WeakReference<>(recipes);
		}
		return cached;
	}

	/** The recipes that make the given item. */
	public static List<CraftingRecipe> forResult(ServerLevel level, Item result) {
		return all(level).stream().filter(recipe -> CarpenterRecipes.resultOf(recipe) == result).toList();
	}

	/** The products of the grindstone, each once, in the order of {@link #all}. */
	public static List<Item> products(ServerLevel level) {
		return all(level).stream().map(CarpenterRecipes::resultOf).distinct().toList();
	}
}
