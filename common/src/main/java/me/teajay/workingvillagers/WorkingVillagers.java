package me.teajay.workingvillagers;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.util.Objects;
import java.util.function.Supplier;
import me.teajay.workingvillagers.block.CarpenterBenchBlock;
import me.teajay.workingvillagers.block.ChoppingBlock;
import me.teajay.workingvillagers.block.WoodcutterBlock;
import me.teajay.workingvillagers.crafting.WoodcuttingRecipe;
import me.teajay.workingvillagers.item.WickerBasketItem;
import me.teajay.workingvillagers.item.WorkContractItem;
import me.teajay.workingvillagers.menu.CarpenterMenu;
import me.teajay.workingvillagers.menu.CarrierMenu;
import me.teajay.workingvillagers.menu.CutterMenu;
import me.teajay.workingvillagers.menu.StationMenu;
import me.teajay.workingvillagers.network.RenameRoutePayload;
import me.teajay.workingvillagers.station.StationType;
import me.teajay.workingvillagers.platform.Platform;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.jspecify.annotations.Nullable;

public final class WorkingVillagers {
	public static final String MOD_ID = "workingvillagers";

	public static final ResourceKey<PoiType> LUMBERJACK_POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, id("lumberjack"));
	public static final ResourceKey<VillagerProfession> LUMBERJACK_PROFESSION = ResourceKey.create(Registries.VILLAGER_PROFESSION, id("lumberjack"));
	public static final ResourceKey<PoiType> WOODCUTTER_POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, id("woodcutter"));
	public static final ResourceKey<VillagerProfession> WOODCUTTER_PROFESSION = ResourceKey.create(Registries.VILLAGER_PROFESSION, id("woodcutter"));
	/** Has no job site, the basket it carries is its workstation. */
	public static final ResourceKey<VillagerProfession> CARRIER_PROFESSION = ResourceKey.create(Registries.VILLAGER_PROFESSION, id("carrier"));
	public static final ResourceKey<PoiType> CARPENTER_POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, id("carpenter"));
	public static final ResourceKey<VillagerProfession> CARPENTER_PROFESSION = ResourceKey.create(Registries.VILLAGER_PROFESSION, id("carpenter"));
	/** Works at the vanilla stonecutter, the job site of the mason. */
	public static final ResourceKey<VillagerProfession> STONECUTTER_PROFESSION = ResourceKey.create(Registries.VILLAGER_PROFESSION, id("stonecutter"));

	public static Supplier<Block> CHOPPING_BLOCK;
	public static Supplier<Item> CHOPPING_BLOCK_ITEM;
	public static Supplier<Item> WORK_CONTRACT;
	public static Supplier<Item> WICKER_BASKET;

	/** What the carpenter's bench makes: crafting recipes with a result in this tag. */
	public static final TagKey<Item> JOINERY = TagKey.create(Registries.ITEM, id("joinery"));
	public static Supplier<Block> CARPENTER_BENCH;
	public static Supplier<Item> CARPENTER_BENCH_ITEM;
	public static Supplier<MenuType<CarpenterMenu>> CARPENTER_BENCH_MENU;
	public static Supplier<MenuType<StationMenu>> CARPENTER_STATION_MENU;

	/** Items that can be put into the woodcutter by shift-clicking. */
	public static final TagKey<Item> WOODCUTTABLE = TagKey.create(Registries.ITEM, id("woodcuttable"));
	public static Supplier<Block> WOODCUTTER_BLOCK;
	public static Supplier<Item> WOODCUTTER_ITEM;
	public static Supplier<MenuType<StationMenu>> CHOPPING_BLOCK_MENU;
	public static Supplier<MenuType<StationMenu>> WOODCUTTER_STATION_MENU;
	public static Supplier<MenuType<StationMenu>> STONECUTTER_STATION_MENU;
	public static Supplier<MenuType<CarrierMenu>> CARRIER_MENU;
	public static Supplier<MenuType<CutterMenu>> WOODCUTTER_MENU;
	public static Supplier<RecipeType<WoodcuttingRecipe>> WOODCUTTING;
	public static Supplier<RecipeSerializer<WoodcuttingRecipe>> WOODCUTTING_SERIALIZER;
	public static final CutterMenu.Kind WOODCUTTER_KIND = new CutterMenu.Kind(
		() -> WOODCUTTER_MENU.get(), () -> WOODCUTTING.get(), () -> WOODCUTTER_BLOCK.get(), (level, stack) -> stack.is(WOODCUTTABLE)
	);

	public static Supplier<MenuType<CutterMenu>> STONECUTTER_MENU;
	public static final CutterMenu.Kind STONECUTTER_KIND = new CutterMenu.Kind(
		() -> STONECUTTER_MENU.get(), () -> RecipeType.STONECUTTING, () -> Blocks.STONECUTTER,
		(level, stack) -> level.recipeAccess().stonecutterRecipes().acceptsInput(stack)
	);

	private WorkingVillagers() {
	}

	private static @Nullable Platform platform;

	/** The loader specific parts, set by the loader entrypoint. */
	public static Platform platform() {
		return Objects.requireNonNull(platform, "WorkingVillagers.init has not run");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static void init(Platform platform) {
		WorkingVillagers.platform = platform;
		Config.load(platform.configDir());
		registerLumberjack(platform);
		registerWoodcutter(platform);
		registerStonecutter(platform);
		registerCarpenter(platform);
		registerCarrier(platform);
		platform.registerServerbound(RenameRoutePayload.TYPE, RenameRoutePayload.CODEC, RenameRoutePayload::handle);
	}

	private static void registerCarrier(Platform platform) {
		WICKER_BASKET = platform.register(Registries.ITEM, "wicker_basket", () -> new WickerBasketItem(
			itemProperties("wicker_basket").stacksTo(1)
		));
		CARRIER_MENU = platform.registerMenu("carrier", CarrierMenu::new);
		platform.register(Registries.VILLAGER_PROFESSION, "carrier", () -> new VillagerProfession(
			Component.translatable("entity." + MOD_ID + ".villager.carrier"),
			poi -> false,
			poi -> false,
			ImmutableSet.of(),
			ImmutableSet.of(),
			null,
			Int2ObjectMaps.emptyMap()
		));
	}

	private static void registerLumberjack(Platform platform) {
		CHOPPING_BLOCK = platform.register(Registries.BLOCK, "chopping_block", () -> new ChoppingBlock(
			BlockBehaviour.Properties.of()
				.setId(ResourceKey.create(Registries.BLOCK, id("chopping_block")))
				.mapColor(MapColor.WOOD)
				.strength(2.5F)
				.sound(SoundType.WOOD)
				.noOcclusion()
		));
		CHOPPING_BLOCK_ITEM = platform.register(Registries.ITEM, "chopping_block", () -> new BlockItem(
			CHOPPING_BLOCK.get(),
			itemProperties("chopping_block").useBlockDescriptionPrefix()
		));
		WORK_CONTRACT = platform.register(Registries.ITEM, "work_contract", () -> new WorkContractItem(
			itemProperties("work_contract").stacksTo(16)
		));
		CHOPPING_BLOCK_MENU = platform.registerMenu("chopping_block", (containerId, inventory) -> new StationMenu(StationType.CHOPPING_BLOCK, containerId, inventory));
		platform.registerPoi("lumberjack", () -> CHOPPING_BLOCK.get(), 1, 1);
		registerProfession(platform, "lumberjack", LUMBERJACK_POI, ImmutableSet.of(
			Items.OAK_SAPLING, Items.SPRUCE_SAPLING, Items.BIRCH_SAPLING, Items.JUNGLE_SAPLING, Items.ACACIA_SAPLING,
			Items.DARK_OAK_SAPLING, Items.CHERRY_SAPLING, Items.PALE_OAK_SAPLING, Items.MANGROVE_PROPAGULE, Items.POPLAR_SAPLING, Items.STICK, Items.APPLE
		), null);
	}

	/**
	 * Registers a profession that only hired villagers get (see WorkerJobs): ordinary
	 * unemployed villagers never look for its job site.
	 */
	private static void registerProfession(
		Platform platform, String name, ResourceKey<PoiType> jobSite, ImmutableSet<Item> requestedItems, @Nullable SoundEvent workSound
	) {
		platform.register(Registries.VILLAGER_PROFESSION, name, () -> new VillagerProfession(
			Component.translatable("entity." + MOD_ID + ".villager." + name),
			poi -> poi.is(jobSite),
			poi -> false,
			requestedItems,
			ImmutableSet.of(),
			workSound,
			Int2ObjectMaps.emptyMap()
		));
	}

	private static void registerWoodcutter(Platform platform) {
		WOODCUTTER_BLOCK = platform.register(Registries.BLOCK, "woodcutter", () -> new WoodcutterBlock(
			BlockBehaviour.Properties.of()
				.setId(ResourceKey.create(Registries.BLOCK, id("woodcutter")))
				.mapColor(MapColor.WOOD)
				.strength(2.5F)
				.sound(SoundType.WOOD)
				.noOcclusion()
		));
		WOODCUTTER_ITEM = platform.register(Registries.ITEM, "woodcutter", () -> new BlockItem(
			WOODCUTTER_BLOCK.get(),
			itemProperties("woodcutter").useBlockDescriptionPrefix()
		));
		WOODCUTTER_STATION_MENU = platform.registerMenu("woodcutter_station", (containerId, inventory) -> new StationMenu(StationType.WOODCUTTER, containerId, inventory));
		WOODCUTTER_MENU = platform.registerMenu("woodcutter", (containerId, inventory) -> new CutterMenu(WOODCUTTER_KIND, containerId, inventory));
		WOODCUTTING = registerRecipeType(platform, "woodcutting");
		WOODCUTTING_SERIALIZER = registerRecipeSerializer(platform, "woodcutting", () -> new RecipeSerializer<>(
			WoodcuttingRecipe.MAP_CODEC, WoodcuttingRecipe.STREAM_CODEC
		));
		platform.registerPoi("woodcutter", () -> WOODCUTTER_BLOCK.get(), 1, 1);
		registerProfession(platform, "woodcutter", WOODCUTTER_POI, ImmutableSet.of(), SoundEvents.VILLAGER_WORK_FLETCHER);
	}

	private static void registerCarpenter(Platform platform) {
		CARPENTER_BENCH = platform.register(Registries.BLOCK, "carpenter_bench", () -> new CarpenterBenchBlock(
			BlockBehaviour.Properties.of()
				.setId(ResourceKey.create(Registries.BLOCK, id("carpenter_bench")))
				.mapColor(MapColor.WOOD)
				.strength(2.5F)
				.sound(SoundType.WOOD)
				.ignitedByLava()
				.noOcclusion()
		));
		CARPENTER_BENCH_ITEM = platform.register(Registries.ITEM, "carpenter_bench", () -> new BlockItem(
			CARPENTER_BENCH.get(),
			itemProperties("carpenter_bench").useBlockDescriptionPrefix()
		));
		CARPENTER_STATION_MENU = platform.registerMenu("carpenter_station", (containerId, inventory) -> new StationMenu(StationType.CARPENTER_BENCH, containerId, inventory));
		CARPENTER_BENCH_MENU = platform.registerMenu("carpenter_bench", CarpenterMenu::new);
		platform.registerPoi("carpenter", () -> CARPENTER_BENCH.get(), 1, 1);
		registerProfession(platform, "carpenter", CARPENTER_POI, ImmutableSet.of(), SoundEvents.VILLAGER_WORK_FLETCHER);
	}

	/**
	 * The stonecutter worker uses the vanilla stonecutter, which is the job site of the mason.
	 * Vanilla looks up the profession of a job site by taking the first matching one, which is
	 * always the mason, so ordinary villagers still become masons there.
	 */
	private static void registerStonecutter(Platform platform) {
		STONECUTTER_STATION_MENU = platform.registerMenu("stonecutter_station", (containerId, inventory) -> new StationMenu(StationType.STONECUTTER, containerId, inventory));
		STONECUTTER_MENU = platform.registerMenu("stonecutter", (containerId, inventory) -> new CutterMenu(STONECUTTER_KIND, containerId, inventory));
		registerProfession(platform, "stonecutter", PoiTypes.MASON, ImmutableSet.of(), SoundEvents.VILLAGER_WORK_MASON);
	}

	@SuppressWarnings("unchecked")
	private static <T extends Recipe<?>> Supplier<RecipeType<T>> registerRecipeType(Platform platform, String name) {
		String id = id(name).toString();
		Supplier<RecipeType<?>> registered = platform.register(Registries.RECIPE_TYPE, name, () -> new RecipeType<T>() {
			@Override
			public String toString() {
				return id;
			}
		});
		return () -> (RecipeType<T>) registered.get();
	}

	@SuppressWarnings("unchecked")
	private static <T extends Recipe<?>> Supplier<RecipeSerializer<T>> registerRecipeSerializer(
		Platform platform, String name, Supplier<RecipeSerializer<T>> factory
	) {
		Supplier<RecipeSerializer<?>> registered = platform.register(Registries.RECIPE_SERIALIZER, name, factory);
		return () -> (RecipeSerializer<T>) registered.get();
	}

	private static Item.Properties itemProperties(String name) {
		return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(name)));
	}
}
