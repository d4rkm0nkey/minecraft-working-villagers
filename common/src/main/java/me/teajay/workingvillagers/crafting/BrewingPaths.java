package me.teajay.workingvillagers.crafting;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.BrewingInput;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.PotionIngredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import org.jspecify.annotations.Nullable;

/**
 * What the apothecary makes at the brewing stand, and how: glass bottles, water bottles and
 * every potion (drinkable and splash) the brewing recipes ({@code minecraft:brewing}) lead to
 * from a water bottle, with the steps on the way (water, awkward potion, healing potion,
 * ...). As this comes from the recipes, those of data packs count too.
 *
 * <p>An order is an item plus, for potions, the potion: it is written as a key, the item id
 * alone or {@code minecraft:potion#minecraft:healing}.
 */
public final class BrewingPaths {
	/** One brew: the reagent, and what the bottles become. */
	public record Step(PotionIngredient reagent, ItemStack shownReagent, ItemStack output) {
	}

	/** The results of the last recipe manager; a reload makes new ones. */
	private static WeakReference<RecipeManager> cachedFor = new WeakReference<>(null);
	/** The shortest way from a water bottle to each potion, by key. */
	private static Map<String, List<Step>> paths = Map.of();
	private static List<ItemStack> products = List.of();

	private BrewingPaths() {
	}

	public static ItemStack water() {
		return PotionContents.createItemStack(Items.POTION, Potions.WATER);
	}

	/** The key of an order or product, see the class comment. */
	public static String key(ItemStack stack) {
		String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		Optional<Holder<Potion>> potion = potionOf(stack);
		return potion.flatMap(Holder::unwrapKey).map(key -> item + "#" + key.identifier()).orElse(item);
	}

	/** The product of a key, empty if the item or the potion is unknown. */
	public static ItemStack fromKey(String key) {
		int split = key.indexOf('#');
		Identifier itemId = Identifier.tryParse(split < 0 ? key : key.substring(0, split));
		Optional<Item> item = itemId == null ? Optional.empty() : BuiltInRegistries.ITEM.getOptional(itemId);
		if (item.isEmpty()) {
			return ItemStack.EMPTY;
		}
		if (split < 0) {
			return new ItemStack(item.get());
		}
		Identifier potionId = Identifier.tryParse(key.substring(split + 1));
		Optional<Holder.Reference<Potion>> potion = potionId == null ? Optional.empty() : BuiltInRegistries.POTION.get(potionId);
		return potion.map(holder -> PotionContents.createItemStack(item.get(), holder)).orElse(ItemStack.EMPTY);
	}

	/** The product the station is ordered: the item, with the potion for potions. */
	public static ItemStack order(@Nullable Item item, @Nullable String potion) {
		if (item == null) {
			return ItemStack.EMPTY;
		}
		return potion == null ? new ItemStack(item) : fromKey(BuiltInRegistries.ITEM.getKey(item) + "#" + potion);
	}

	public static Optional<Holder<Potion>> potionOf(ItemStack stack) {
		PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
		return contents == null ? Optional.empty() : contents.potion();
	}

	/** Whether the stacks are the same product: same item, same potion. */
	public static boolean same(ItemStack a, ItemStack b) {
		return !a.isEmpty() && !b.isEmpty() && a.is(b.getItem()) && potionOf(a).equals(potionOf(b));
	}

	/** Whether the stack heals at once (a potion of healing, also a splash one). */
	public static boolean heals(ItemStack stack) {
		PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
		if (contents == null || !(stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION))) {
			return false;
		}
		for (var effect : contents.getAllEffects()) {
			if (effect.getEffect().is(MobEffects.INSTANT_HEALTH)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * What the apothecary can be ordered: glass bottles, water bottles, then the potions in the
	 * order of the potion registry, each drinkable one followed by its splash potion.
	 */
	public static List<ItemStack> products(ServerLevel level) {
		load(level);
		return products;
	}

	/**
	 * The brews that make the product from a water bottle, empty for a water bottle itself, null
	 * if no recipe leads there (or the product is no potion).
	 */
	public static @Nullable List<Step> path(ServerLevel level, ItemStack product) {
		load(level);
		return paths.get(key(product));
	}

	private static void load(ServerLevel level) {
		RecipeManager recipes = level.recipeAccess();
		if (cachedFor.get() == recipes) {
			return;
		}
		List<BrewingRecipe> brewing = new ArrayList<>();
		for (RecipeHolder<?> holder : recipes.getRecipes()) {
			if (holder.value() instanceof BrewingRecipe recipe && recipe.getType() == RecipeType.BREWING) {
				brewing.add(recipe);
			}
		}
		// Breadth first from a water bottle: the first way found to a potion is a shortest one
		Map<String, List<Step>> found = new LinkedHashMap<>();
		Map<String, ItemStack> stacks = new HashMap<>();
		Deque<ItemStack> open = new ArrayDeque<>();
		ItemStack water = water();
		found.put(key(water), List.of());
		stacks.put(key(water), water);
		open.add(water);
		while (!open.isEmpty()) {
			ItemStack from = open.poll();
			List<Step> way = found.get(key(from));
			for (BrewingRecipe recipe : brewing) {
				if (!recipe.getInput().test(from)) {
					continue;
				}
				Optional<ItemStack> reagent = recipe.getReagent().ingredient().items().findFirst().map(item -> new ItemStack(item));
				if (reagent.isEmpty()) {
					continue;
				}
				ItemStack output = recipe.assemble(new BrewingInput(from.copy(), reagent.get()));
				if (output.isEmpty() || potionOf(output).isEmpty() || found.containsKey(key(output))) {
					continue;
				}
				List<Step> longer = new ArrayList<>(way);
				longer.add(new Step(recipe.getReagent(), reagent.get(), output.copyWithCount(1)));
				found.put(key(output), List.copyOf(longer));
				stacks.put(key(output), output.copyWithCount(1));
				open.add(output.copyWithCount(1));
			}
		}
		List<ItemStack> list = new ArrayList<>();
		list.add(new ItemStack(Items.GLASS_BOTTLE));
		list.add(water);
		stacks.values().stream()
			.filter(stack -> !same(stack, water) && (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION)))
			.filter(stack -> potionOf(stack).filter(potion -> !potion.is(Potions.AWKWARD) && !potion.is(Potions.MUNDANE) && !potion.is(Potions.THICK)).isPresent())
			.sorted(Comparator.<ItemStack>comparingInt(stack -> potionOf(stack).map(potion -> BuiltInRegistries.POTION.getId(potion.value())).orElse(0))
				.thenComparing(stack -> stack.is(Items.SPLASH_POTION)))
			.forEach(list::add);
		paths = Map.copyOf(found);
		products = List.copyOf(list);
		cachedFor = new WeakReference<>(recipes);
	}
}
