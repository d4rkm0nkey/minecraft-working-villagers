package me.teajay.workingvillagers.crafting;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.teajay.workingvillagers.WorkingVillagers;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import org.jspecify.annotations.Nullable;

/**
 * What the kiln fires: everything a furnace can smelt ({@code minecraft:smelting}), except
 * food (recipes of the book category "food", that is the smoker's) and everything the blast
 * furnace can smelt (ores, raw metal, iron and gold tools). Items in the tag
 * {@code workingvillagers:kiln_excluded} are left out as well. As this comes from the recipes,
 * those of data packs and other mods count too.
 *
 * <p>Firing glass, stone, bricks, terracotta, charcoal, dyes, ...
 */
public final class KilnRecipes {
	/** How many times a product may be fired again on the way to the order (cobblestone, stone, smooth stone). */
	private static final int MAX_STEPS = 4;
	private static final Comparator<Item> BY_ID = Comparator.comparingInt(BuiltInRegistries.ITEM::getId);

	/** The results of the last recipe manager; a reload makes a new one. */
	private static WeakReference<RecipeManager> cachedFor = new WeakReference<>(null);
	/** What the kiln makes of each item it accepts. */
	private static Map<Item, Item> results = Map.of();
	private static List<Item> products = List.of();

	private KilnRecipes() {
	}

	private static Map<Item, Item> results(ServerLevel level) {
		RecipeManager recipes = level.recipeAccess();
		if (cachedFor.get() != recipes) {
			Set<Item> excluded = new HashSet<>();
			Map<Item, Item> found = new LinkedHashMap<>();
			for (RecipeHolder<?> holder : recipes.getRecipes()) {
				if (!(holder.value() instanceof AbstractCookingRecipe recipe)) {
					continue;
				}
				boolean food = recipe.getType() == RecipeType.SMELTING && recipe.category() == CookingBookCategory.FOOD;
				if (recipe.getType() == RecipeType.BLASTING || food) {
					recipe.input().items().map(Holder::value).forEach(excluded::add);
				} else if (recipe.getType() == RecipeType.SMELTING) {
					recipe.input().items().forEach(input -> {
						ItemStack result = recipe.assemble(new SingleRecipeInput(new ItemStack(input)));
						if (!result.isEmpty() && !input.is(WorkingVillagers.KILN_EXCLUDED)) {
							found.putIfAbsent(input.value(), result.getItem());
						}
					});
				}
			}
			found.keySet().removeAll(excluded);
			results = Map.copyOf(found);
			products = found.values().stream().distinct().sorted(BY_ID).toList();
			cachedFor = new WeakReference<>(recipes);
		}
		return results;
	}

	/** Whether the kiln fires the item. */
	public static boolean accepts(ServerLevel level, ItemStack stack) {
		return !stack.isEmpty() && results(level).containsKey(stack.getItem());
	}

	/** What the kiln makes of the item, null if it does not fire it. */
	public static @Nullable Item resultOf(ServerLevel level, ItemStack stack) {
		return stack.isEmpty() ? null : results(level).get(stack.getItem());
	}

	/** Everything the kiln makes, each once, sorted like the creative inventory. */
	public static List<Item> products(ServerLevel level) {
		results(level);
		return products;
	}

	/**
	 * Whether firing the item, maybe several times, makes the product: cobblestone leads to
	 * smooth stone (by way of stone), sand to glass.
	 */
	public static boolean leadsTo(ServerLevel level, ItemStack stack, Item product) {
		Item item = stack.getItem();
		for (int step = 0; step < MAX_STEPS; step++) {
			item = results(level).get(item);
			if (item == null) {
				return false;
			}
			if (item == product) {
				return true;
			}
		}
		return false;
	}
}
