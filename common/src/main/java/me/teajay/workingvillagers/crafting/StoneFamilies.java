package me.teajay.workingvillagers.crafting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;

/**
 * Groups the products of the stonecutter into families for picking the order: everything
 * connected by stonecutting recipes is one family (deepslate with its bricks, tiles and
 * their slabs, stairs and walls). Unlike wood, stone has no fixed set of shapes, so this
 * is derived from the recipes, including those of data packs.
 */
public final class StoneFamilies {
	private static final Comparator<Item> BY_ID = Comparator.comparingInt(BuiltInRegistries.ITEM::getId);

	private StoneFamilies() {
	}

	/**
	 * A family: the icon is the material it starts from (one that no recipe makes), the
	 * products are sorted like the creative inventory.
	 */
	public record Family(Item icon, List<Item> products) {
		public boolean contains(Item item) {
			return this.products.contains(item);
		}
	}

	public static List<Family> of(ServerLevel level) {
		Map<Item, Item> parents = new HashMap<>();
		Set<Item> results = new HashSet<>();
		for (SingleItemRecipe recipe : CuttingRecipes.recipes(level, RecipeType.STONECUTTING)) {
			Item result = CuttingRecipes.resultOf(recipe);
			results.add(result);
			root(parents, result);
			recipe.input().items().forEach(material -> parents.put(root(parents, material.value()), root(parents, result)));
		}
		Map<Item, List<Item>> groups = new HashMap<>();
		for (Item item : parents.keySet()) {
			groups.computeIfAbsent(root(parents, item), key -> new ArrayList<>()).add(item);
		}
		List<Family> families = new ArrayList<>();
		for (List<Item> group : groups.values()) {
			group.sort(BY_ID);
			List<Item> products = group.stream().filter(results::contains).toList();
			Item icon = group.stream().filter(item -> !results.contains(item)).findFirst().orElse(products.getFirst());
			families.add(new Family(icon, products));
		}
		families.sort(Comparator.comparing(Family::icon, BY_ID));
		return families;
	}

	/** Union-find: the representative of the item's family. */
	private static Item root(Map<Item, Item> parents, Item item) {
		Item parent = parents.computeIfAbsent(item, key -> key);
		if (parent == item) {
			return item;
		}
		Item root = root(parents, parent);
		parents.put(item, root);
		return root;
	}
}
