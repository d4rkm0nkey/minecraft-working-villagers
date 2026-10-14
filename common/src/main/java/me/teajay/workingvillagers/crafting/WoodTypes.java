package me.teajay.workingvillagers.crafting;

import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * The wood types and product shapes of the woodcutter and the carpenter's bench. An order
 * of the woodcutter is a shape (stairs, slabs, ...) and the wood types to use: the product
 * is the item {@code <wood>_<shape>}, e.g. {@code spruce_stairs}. At the carpenter's bench
 * only some products are made of one wood type (boats, shelves, ...); the others (chests,
 * ladders, ...) can be made of any wood type.
 */
public final class WoodTypes {
	/** What the woodcutter can make, in the order of the order screen. */
	public static final List<String> SHAPES = List.of(
		"planks", "slab", "stairs", "fence", "fence_gate", "door", "trapdoor", "pressure_plate", "button", "sign", "stick"
	);
	public static final String STICK = "stick";
	/** The products of the carpenter's bench that are made of one wood type. */
	public static final List<String> CARPENTER_SHAPES = List.of("boat", "chest_boat", "hanging_sign", "shelf");
	/** Bamboo makes rafts instead of boats. */
	private static final Map<String, String> ALIASES = Map.of("boat", "raft", "chest_boat", "chest_raft");

	public static final List<WoodType> ALL = List.of(
		new WoodType("oak", "oak_logs", Items.OAK_LOG),
		new WoodType("spruce", "spruce_logs", Items.SPRUCE_LOG),
		new WoodType("birch", "birch_logs", Items.BIRCH_LOG),
		new WoodType("jungle", "jungle_logs", Items.JUNGLE_LOG),
		new WoodType("acacia", "acacia_logs", Items.ACACIA_LOG),
		new WoodType("dark_oak", "dark_oak_logs", Items.DARK_OAK_LOG),
		new WoodType("pale_oak", "pale_oak_logs", Items.PALE_OAK_LOG),
		new WoodType("mangrove", "mangrove_logs", Items.MANGROVE_LOG),
		new WoodType("cherry", "cherry_logs", Items.CHERRY_LOG),
		new WoodType("poplar", "poplar_logs", Items.POPLAR_LOG),
		new WoodType("crimson", "crimson_stems", Items.CRIMSON_STEM),
		new WoodType("warped", "warped_stems", Items.WARPED_STEM),
		new WoodType("bamboo", "bamboo_blocks", Items.BAMBOO_BLOCK)
	);

	private WoodTypes() {
	}

	public static @Nullable WoodType byName(String name) {
		return ALL.stream().filter(wood -> wood.name().equals(name)).findFirst().orElse(null);
	}

	/** The wood type of a product, log or plank, null if it has none. */
	public static @Nullable WoodType woodOf(Item item) {
		String path = BuiltInRegistries.ITEM.getKey(item).getPath();
		WoodType found = null;
		for (WoodType wood : ALL) {
			// The longest match wins, so dark_oak_planks is not oak
			if ((path.startsWith(wood.name() + "_") || item == wood.log()) && (found == null || wood.name().length() > found.name().length())) {
				found = wood;
			}
		}
		return found;
	}

	/** The shape of a woodcutter product, e.g. "stairs" for spruce_stairs, null if it is none of the shapes. */
	public static @Nullable String shapeOf(Item item) {
		return shapeOf(item, SHAPES);
	}

	/** The shape of a product, if it is one of the given shapes. */
	public static @Nullable String shapeOf(Item item, List<String> shapes) {
		if (item == Items.STICK) {
			return shapes.contains(STICK) ? STICK : null;
		}
		WoodType wood = woodOf(item);
		if (wood == null) {
			return null;
		}
		String shape = BuiltInRegistries.ITEM.getKey(item).getPath().substring(wood.name().length() + 1);
		for (Map.Entry<String, String> alias : ALIASES.entrySet()) {
			if (alias.getValue().equals(shape)) {
				shape = alias.getKey();
			}
		}
		return shapes.contains(shape) ? shape : null;
	}

	/**
	 * The wood type of a material: the one whose logs it is (stripped logs and wood count),
	 * otherwise the one in its name (planks, slabs, boats, ...). Null if it has none.
	 */
	public static @Nullable WoodType woodOfMaterial(ItemStack stack) {
		for (WoodType wood : ALL) {
			if (stack.is(wood.logs())) {
				return wood;
			}
		}
		return woodOf(stack.getItem());
	}

	/** The product of the given shape and wood, null if there is no such item. */
	public static @Nullable Item product(WoodType wood, String shape) {
		if (STICK.equals(shape)) {
			return Items.STICK;
		}
		Item product = BuiltInRegistries.ITEM.getOptional(Identifier.withDefaultNamespace(wood.name() + "_" + shape)).orElse(null);
		if (product == null && ALIASES.containsKey(shape)) {
			product = BuiltInRegistries.ITEM.getOptional(Identifier.withDefaultNamespace(wood.name() + "_" + ALIASES.get(shape))).orElse(null);
		}
		return product;
	}

	/**
	 * A wood type: its name as in the item ids, the item tag of its logs (or stems, or
	 * bamboo blocks) and the log shown for it.
	 */
	public record WoodType(String name, String logsTag, Item log) {
		public TagKey<Item> logs() {
			return TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace(this.logsTag));
		}

		public @Nullable Item planks() {
			return product(this, "planks");
		}

		/** Whether the item is material of this wood type: one of its logs or its planks. */
		public boolean isMaterial(ItemStack stack) {
			Item planks = this.planks();
			return stack.is(this.logs()) || planks != null && stack.is(planks);
		}
	}
}
