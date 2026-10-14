package me.teajay.workingvillagers.crafting;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;

/**
 * Looks up the recipes of a cutter (woodcutting or stonecutting) on the server.
 */
public final class CuttingRecipes {
	private CuttingRecipes() {
	}

	@SuppressWarnings("unchecked")
	private static Stream<RecipeHolder<SingleItemRecipe>> all(ServerLevel level, RecipeType<?> type) {
		return level.recipeAccess().getRecipes().stream()
			.filter(holder -> holder.value().getType() == type && holder.value() instanceof SingleItemRecipe)
			.map(holder -> (RecipeHolder<SingleItemRecipe>) (RecipeHolder<?>) holder);
	}

	/**
	 * The recipes for an input, in the order of the creative inventory (planks, stairs, slabs, ...).
	 */
	public static List<RecipeHolder<SingleItemRecipe>> forInput(ServerLevel level, RecipeType<?> type, ItemStack input) {
		SingleRecipeInput recipeInput = new SingleRecipeInput(input);
		return all(level, type)
			.filter(holder -> holder.value().matches(recipeInput, level))
			.sorted(Comparator.comparingInt(holder -> BuiltInRegistries.ITEM.getId(resultOf(holder.value()))))
			.toList();
	}

	/** Every recipe of the cutter. */
	public static List<SingleItemRecipe> recipes(ServerLevel level, RecipeType<?> type) {
		return all(level, type).map(RecipeHolder::value).toList();
	}

	/**
	 * The recipes that make the given item.
	 */
	public static List<SingleItemRecipe> forResult(ServerLevel level, RecipeType<?> type, Item result) {
		return all(level, type).map(RecipeHolder::value).filter(recipe -> resultOf(recipe) == result).toList();
	}

	/** The result of the recipe, the same for every input. */
	public static ItemStack preview(SingleItemRecipe recipe) {
		return recipe.assemble(new SingleRecipeInput(ItemStack.EMPTY));
	}

	public static Item resultOf(SingleItemRecipe recipe) {
		return preview(recipe).getItem();
	}
}
