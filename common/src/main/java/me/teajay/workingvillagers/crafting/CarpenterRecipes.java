package me.teajay.workingvillagers.crafting;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import me.teajay.workingvillagers.WorkingVillagers;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.jspecify.annotations.Nullable;

/**
 * The recipes of the carpenter's bench: the vanilla crafting recipes (shaped and
 * shapeless) whose result is in the item tag {@code workingvillagers:joinery}. Data packs add
 * products by adding them to the tag.
 *
 * <p>Ingredients are matched like in the crafting table, so mixed planks make a chest.
 */
public final class CarpenterRecipes {
	/** The recipes of the last recipe manager; a reload makes a new one. */
	private static WeakReference<RecipeManager> cachedFor = new WeakReference<>(null);
	private static List<CraftingRecipe> cached = List.of();

	private CarpenterRecipes() {
	}

	/** All recipes of the bench, sorted by their result like the creative inventory. */
	public static List<CraftingRecipe> all(ServerLevel level) {
		RecipeManager recipes = level.recipeAccess();
		if (cachedFor.get() != recipes) {
			cached = recipes.getRecipes().stream()
				.map(RecipeHolder::value)
				.filter(recipe -> recipe.getType() == RecipeType.CRAFTING && (recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe))
				.map(recipe -> (CraftingRecipe) recipe)
				.filter(recipe -> !recipe.placementInfo().isImpossibleToPlace() && preview(recipe).is(WorkingVillagers.JOINERY))
				.sorted(Comparator.comparingInt(recipe -> BuiltInRegistries.ITEM.getId(resultOf(recipe))))
				.toList();
			cachedFor = new WeakReference<>(recipes);
		}
		return cached;
	}

	/** The recipes that make the given item. */
	public static List<CraftingRecipe> forResult(ServerLevel level, Item result) {
		return all(level).stream().filter(recipe -> resultOf(recipe) == result).toList();
	}

	/** The products of the bench, each once, in the order of {@link #all}. */
	public static List<Item> products(ServerLevel level) {
		return all(level).stream().map(CarpenterRecipes::resultOf).distinct().toList();
	}

	/** The result of the recipe, the same for every input (only shaped and shapeless recipes are used). */
	public static ItemStack preview(CraftingRecipe recipe) {
		return recipe.assemble(CraftingInput.EMPTY);
	}

	public static Item resultOf(CraftingRecipe recipe) {
		return preview(recipe).getItem();
	}

	/** How many items one craft takes, e.g. 8 for a chest. */
	public static int ingredientCount(CraftingRecipe recipe) {
		return recipe.placementInfo().ingredients().size();
	}

	/**
	 * Counts the matching items in the first {@code slots} slots of the container (for a
	 * player only the main inventory), as material for {@link #pick}.
	 */
	public static void account(StackedItemContents contents, Container container, int slots, Predicate<ItemStack> filter) {
		for (int slot = 0; slot < slots; slot++) {
			ItemStack stack = container.getItem(slot);
			if (!stack.isEmpty() && filter.test(stack)) {
				contents.accountStack(stack, Integer.MAX_VALUE);
			}
		}
	}

	/**
	 * The items to use for {@code amount} crafts of the recipe out of the counted material,
	 * with how many of each. Empty if there is not enough.
	 */
	public static Optional<Map<Item, Integer>> pick(StackedItemContents contents, CraftingRecipe recipe, int amount) {
		List<Holder<Item>> picked = new ArrayList<>();
		if (amount <= 0 || !contents.canCraft(recipe, amount, picked::add)) {
			return Optional.empty();
		}
		Map<Item, Integer> needed = new LinkedHashMap<>();
		for (Holder<Item> item : picked) {
			needed.merge(item.value(), amount, Integer::sum);
		}
		return Optional.of(needed);
	}

	/** How many times the recipe can be made from the counted material, at most {@code max}. */
	public static int maxCrafts(StackedItemContents contents, CraftingRecipe recipe, int max) {
		return contents.getBiggestCraftableStack(recipe, max, null);
	}

	/**
	 * Takes the items for one craft out of the first {@code slots} slots of the container
	 * (only items passing the filter) and returns the result plus what the ingredients
	 * leave behind (e.g. empty buckets). Returns an empty list and takes nothing if the
	 * container does not have everything.
	 */
	public static List<ItemStack> craft(Container container, int slots, Predicate<ItemStack> filter, CraftingRecipe recipe) {
		StackedItemContents contents = new StackedItemContents();
		account(contents, container, slots, filter);
		Optional<Map<Item, Integer>> needed = pick(contents, recipe, 1);
		if (needed.isEmpty()) {
			return List.of();
		}
		List<ItemStack> results = new ArrayList<>();
		results.add(preview(recipe));
		for (Map.Entry<Item, Integer> entry : needed.get().entrySet()) {
			int missing = entry.getValue();
			for (int slot = 0; slot < slots && missing > 0; slot++) {
				ItemStack stack = container.getItem(slot);
				if (stack.is(entry.getKey()) && filter.test(stack)) {
					int taken = Math.min(missing, stack.getCount());
					stack.shrink(taken);
					missing -= taken;
				}
			}
			ItemStackTemplate remainder = entry.getKey().getCraftingRemainder();
			if (remainder != null) {
				for (int i = 0; i < entry.getValue(); i++) {
					results.add(remainder.create());
				}
			}
		}
		container.setChanged();
		return results;
	}

	/**
	 * What one craft needs, for tooltips: e.g. "8× Oak Planks (or similar)". Ingredients
	 * accepting several items are named after the first one.
	 */
	public static List<Component> describeIngredients(CraftingRecipe recipe) {
		Map<Ingredient, Integer> counts = new LinkedHashMap<>();
		for (Ingredient ingredient : recipe.placementInfo().ingredients()) {
			counts.merge(ingredient, 1, Integer::sum);
		}
		List<Component> lines = new ArrayList<>();
		counts.forEach((ingredient, count) -> {
			List<Holder<Item>> items = ingredient.items().toList();
			if (items.isEmpty()) {
				return;
			}
			Component name = new ItemStack(items.getFirst()).getHoverName();
			if (items.size() > 1) {
				name = Component.translatable("gui.workingvillagers.carpenter.or_similar", name);
			}
			lines.add(Component.translatable("gui.workingvillagers.carpenter.ingredient", count, name));
		});
		return lines;
	}

	/** The first recipe for one of the products that the material is enough for, if any. */
	public static @Nullable CraftingRecipe firstCraftable(StackedItemContents contents, List<CraftingRecipe> recipes) {
		for (CraftingRecipe recipe : recipes) {
			if (contents.canCraft(recipe, null)) {
				return recipe;
			}
		}
		return null;
	}
}
