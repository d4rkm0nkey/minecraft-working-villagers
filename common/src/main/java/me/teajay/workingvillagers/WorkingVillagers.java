package me.teajay.workingvillagers;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import me.teajay.workingvillagers.block.CarpenterBenchBlock;
import me.teajay.workingvillagers.block.ChoppingBlock;
import me.teajay.workingvillagers.block.KilnBlock;
import me.teajay.workingvillagers.block.KilnBlockEntity;
import me.teajay.workingvillagers.block.MiningBenchBlock;
import me.teajay.workingvillagers.block.PitStakeBlock;
import me.teajay.workingvillagers.block.TradingCounterBlock;
import me.teajay.workingvillagers.block.VillageChestBlock;
import me.teajay.workingvillagers.block.VillageChestBlockEntity;
import me.teajay.workingvillagers.block.WoodcutterBlock;
import me.teajay.workingvillagers.crafting.MillingRecipe;
import me.teajay.workingvillagers.crafting.WoodcuttingRecipe;
import me.teajay.workingvillagers.item.WickerBasketItem;
import me.teajay.workingvillagers.item.WorkContractItem;
import me.teajay.workingvillagers.menu.CarpenterMenu;
import me.teajay.workingvillagers.menu.CarrierMenu;
import me.teajay.workingvillagers.menu.CutterMenu;
import me.teajay.workingvillagers.menu.KilnMenu;
import me.teajay.workingvillagers.menu.StationMenu;
import me.teajay.workingvillagers.menu.VillageChestMenu;
import me.teajay.workingvillagers.network.RenameContainerPayload;
import me.teajay.workingvillagers.network.RenameRoutePayload;
import me.teajay.workingvillagers.station.StationType;
import me.teajay.workingvillagers.platform.Platform;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
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
	/** Works at the vanilla composter, the job site of the vanilla farmer. */
	public static final ResourceKey<VillagerProfession> FARMER_PROFESSION = ResourceKey.create(Registries.VILLAGER_PROFESSION, id("farmer"));
	/** Works at the vanilla loom, the job site of the vanilla shepherd. */
	public static final ResourceKey<VillagerProfession> SHEPHERD_PROFESSION = ResourceKey.create(Registries.VILLAGER_PROFESSION, id("shepherd"));
	/** Works at the vanilla barrel, the job site of the vanilla fisherman. */
	public static final ResourceKey<VillagerProfession> FISHER_PROFESSION = ResourceKey.create(Registries.VILLAGER_PROFESSION, id("fisher"));
	/** Works at the vanilla smoker, the job site of the vanilla butcher. */
	public static final ResourceKey<VillagerProfession> BUTCHER_PROFESSION = ResourceKey.create(Registries.VILLAGER_PROFESSION, id("butcher"));
	/** Works at the vanilla blast furnace (the job site of the armorer) or the smithing table (the toolsmith's). */
	public static final ResourceKey<VillagerProfession> SMITH_PROFESSION = ResourceKey.create(Registries.VILLAGER_PROFESSION, id("smith"));
	public static final ResourceKey<PoiType> PIT_DIGGER_POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, id("pit_digger"));
	public static final ResourceKey<VillagerProfession> PIT_DIGGER_PROFESSION = ResourceKey.create(Registries.VILLAGER_PROFESSION, id("pit_digger"));
	public static final ResourceKey<PoiType> KILN_OPERATOR_POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, id("kiln_operator"));
	public static final ResourceKey<VillagerProfession> KILN_OPERATOR_PROFESSION = ResourceKey.create(Registries.VILLAGER_PROFESSION, id("kiln_operator"));
	public static final ResourceKey<PoiType> MINER_POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, id("miner"));
	public static final ResourceKey<VillagerProfession> MINER_PROFESSION = ResourceKey.create(Registries.VILLAGER_PROFESSION, id("miner"));
	public static final ResourceKey<PoiType> SHOP_ASSISTANT_POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, id("shop_assistant"));
	public static final ResourceKey<VillagerProfession> SHOP_ASSISTANT_PROFESSION = ResourceKey.create(Registries.VILLAGER_PROFESSION, id("shop_assistant"));

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
	public static Supplier<MenuType<StationMenu>> COMPOSTER_STATION_MENU;
	public static Supplier<MenuType<StationMenu>> BLAST_FURNACE_STATION_MENU;
	public static Supplier<MenuType<StationMenu>> SMITHING_TABLE_STATION_MENU;
	public static Supplier<MenuType<StationMenu>> LOOM_STATION_MENU;
	public static Supplier<MenuType<StationMenu>> SMOKER_STATION_MENU;
	public static Supplier<MenuType<StationMenu>> BARREL_STATION_MENU;
	public static Supplier<MenuType<CutterMenu>> WOODCUTTER_MENU;
	public static Supplier<RecipeType<WoodcuttingRecipe>> WOODCUTTING;
	public static Supplier<RecipeSerializer<WoodcuttingRecipe>> WOODCUTTING_SERIALIZER;
	public static final CutterMenu.Kind WOODCUTTER_KIND = new CutterMenu.Kind(
		() -> WOODCUTTER_MENU.get(), () -> WOODCUTTING.get(), () -> WOODCUTTER_BLOCK.get(), (level, stack) -> stack.is(WOODCUTTABLE)
	);

	public static Supplier<MenuType<CutterMenu>> STONECUTTER_MENU;

	/** The blocks the pit digger digs: natural ground, stone, ores, plants. */
	public static final TagKey<Block> PIT_DIGGABLE = TagKey.create(Registries.BLOCK, id("pit_diggable"));
	public static Supplier<Block> PIT_STAKE;
	public static Supplier<Item> PIT_STAKE_ITEM;
	public static Supplier<MenuType<StationMenu>> PIT_STAKE_MENU;

	/** Items the kiln does not fire although a furnace smelts them. */
	public static final TagKey<Item> KILN_EXCLUDED = TagKey.create(Registries.ITEM, id("kiln_excluded"));
	public static Supplier<Block> KILN;
	public static Supplier<Item> KILN_ITEM;
	public static Supplier<BlockEntityType<KilnBlockEntity>> KILN_BLOCK_ENTITY;
	public static Supplier<MenuType<KilnMenu>> KILN_MENU;
	public static Supplier<MenuType<StationMenu>> KILN_STATION_MENU;

	/** What the miner digs through: natural ground and stone, ores, cave plants. */
	public static final TagKey<Block> MINE_DIGGABLE = TagKey.create(Registries.BLOCK, id("mine_diggable"));
	/** The ores the miner follows from its tunnel. */
	public static final TagKey<Block> MINER_ORES = TagKey.create(Registries.BLOCK, id("miner_ores"));
	/** Items that can be put into the mill by shift-clicking. */
	public static final TagKey<Item> MILLABLE = TagKey.create(Registries.ITEM, id("millable"));
	public static Supplier<Block> MINING_BENCH;
	public static Supplier<Item> MINING_BENCH_ITEM;
	public static Supplier<MenuType<StationMenu>> MINING_BENCH_MENU;
	public static Supplier<MenuType<CutterMenu>> MILL_MENU;
	public static Supplier<RecipeType<MillingRecipe>> MILLING;
	public static Supplier<RecipeSerializer<MillingRecipe>> MILLING_SERIALIZER;
	public static final CutterMenu.Kind MILL_KIND = new CutterMenu.Kind(
		() -> MILL_MENU.get(), () -> MILLING.get(), () -> MINING_BENCH.get(),
		(level, stack) -> stack.is(MILLABLE)
	);

	public static Supplier<Block> TRADING_COUNTER;
	public static Supplier<Item> TRADING_COUNTER_ITEM;
	public static Supplier<MenuType<StationMenu>> TRADING_COUNTER_MENU;
	/** Every worker nearby takes its supplies from these, see VillageChests. */
	public static final ResourceKey<PoiType> VILLAGE_CHEST_POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, id("village_chest"));
	public static Supplier<Block> VILLAGE_CHEST;
	public static Supplier<Item> VILLAGE_CHEST_ITEM;
	public static Supplier<BlockEntityType<VillageChestBlockEntity>> VILLAGE_CHEST_BLOCK_ENTITY;
	public static Supplier<MenuType<VillageChestMenu>> VILLAGE_CHEST_MENU;
	public static Supplier<MenuType<VillageChestMenu>> VILLAGE_CHEST_DOUBLE_MENU;

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
		registerFarmer(platform);
		registerSmith(platform);
		registerShepherd(platform);
		registerButcher(platform);
		registerFisher(platform);
		registerPitDigger(platform);
		registerKilnOperator(platform);
		registerShopAssistant(platform);
		registerVillageChest(platform);
		registerMiner(platform);
		platform.registerServerbound(RenameRoutePayload.TYPE, RenameRoutePayload.CODEC, RenameRoutePayload::handle);
		platform.registerServerbound(RenameContainerPayload.TYPE, RenameContainerPayload.CODEC, RenameContainerPayload::handle);
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
		registerProfession(platform, name, poi -> poi.is(jobSite), requestedItems, workSound, null);
	}

	private static void registerProfession(
		Platform platform, String name, ResourceKey<PoiType> jobSite, ImmutableSet<Item> requestedItems, @Nullable SoundEvent workSound,
		@Nullable ResourceKey<VillagerProfession> tradesLike
	) {
		registerProfession(platform, name, poi -> poi.is(jobSite), requestedItems, workSound, tradesLike);
	}

	/**
	 * Registers a profession of this mod. {@code tradesLike}: the vanilla profession whose
	 * trades it has (see {@code TradeFamilies}), null for a worker that does not trade.
	 */
	private static void registerProfession(
		Platform platform, String name, Predicate<Holder<PoiType>> jobSite, ImmutableSet<Item> requestedItems, @Nullable SoundEvent workSound,
		@Nullable ResourceKey<VillagerProfession> tradesLike
	) {
		platform.register(Registries.VILLAGER_PROFESSION, name, () -> new VillagerProfession(
			Component.translatable("entity." + MOD_ID + ".villager." + name),
			jobSite,
			poi -> false,
			requestedItems,
			ImmutableSet.of(),
			workSound,
			tradesLike == null ? Int2ObjectMaps.emptyMap() : BuiltInRegistries.VILLAGER_PROFESSION.getValueOrThrow(tradesLike).tradeSetsByLevel()
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
		registerProfession(platform, "stonecutter", PoiTypes.MASON, ImmutableSet.of(), SoundEvents.VILLAGER_WORK_MASON, VillagerProfession.MASON);
	}

	/**
	 * The farmer works at the vanilla composter. Like at the stonecutter, ordinary villagers
	 * still become (vanilla) farmers there; the overview opens with a right-click with an empty
	 * hand.
	 */
	private static void registerFarmer(Platform platform) {
		COMPOSTER_STATION_MENU = platform.registerMenu("composter_station", (containerId, inventory) -> new StationMenu(StationType.COMPOSTER, containerId, inventory));
		registerProfession(platform, "farmer", PoiTypes.FARMER, ImmutableSet.of(
			Items.WHEAT, Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.CARROT, Items.POTATO, Items.BEETROOT, Items.BONE_MEAL
		), SoundEvents.VILLAGER_WORK_FARMER, VillagerProfession.FARMER);
	}

	/**
	 * The shepherd works at the vanilla loom: it shears the sheep around it and breeds them.
	 * Ordinary villagers still become (vanilla) shepherds there; the button with the
	 * shepherd's face in the loom's screen opens the overview.
	 */
	private static void registerShepherd(Platform platform) {
		LOOM_STATION_MENU = platform.registerMenu("loom_station", (containerId, inventory) -> new StationMenu(StationType.LOOM, containerId, inventory));
		registerProfession(platform, "shepherd", PoiTypes.SHEPHERD, ImmutableSet.of(), SoundEvents.VILLAGER_WORK_SHEPHERD, VillagerProfession.SHEPHERD);
	}

	/**
	 * The fisher works at the vanilla barrel: it fishes from the shore nearby or rows out in a
	 * boat. Ordinary villagers still become (vanilla) fishermen there.
	 */
	private static void registerFisher(Platform platform) {
		BARREL_STATION_MENU = platform.registerMenu("barrel_station", (containerId, inventory) -> new StationMenu(StationType.BARREL, containerId, inventory));
		registerProfession(platform, "fisher", PoiTypes.FISHERMAN, ImmutableSet.of(), SoundEvents.VILLAGER_WORK_FISHERMAN, VillagerProfession.FISHERMAN);
	}

	/**
	 * The butcher works at the vanilla smoker: it breeds and slaughters the animals around it
	 * and smokes the meat. Ordinary villagers still become (vanilla) butchers there.
	 */
	private static void registerButcher(Platform platform) {
		SMOKER_STATION_MENU = platform.registerMenu("smoker_station", (containerId, inventory) -> new StationMenu(StationType.SMOKER, containerId, inventory));
		registerProfession(platform, "butcher", PoiTypes.BUTCHER, ImmutableSet.of(), SoundEvents.VILLAGER_WORK_BUTCHER, VillagerProfession.BUTCHER);
	}

	/**
	 * The smith smelts at the vanilla blast furnace and repairs tools at the vanilla smithing
	 * table; ordinary villagers still become armorers and toolsmiths there. The button with the
	 * smith's face in the screens of the blast furnace and the smithing table opens the overview.
	 */
	private static void registerSmith(Platform platform) {
		BLAST_FURNACE_STATION_MENU = platform.registerMenu("blast_furnace_station", (containerId, inventory) -> new StationMenu(StationType.BLAST_FURNACE, containerId, inventory));
		SMITHING_TABLE_STATION_MENU = platform.registerMenu("smithing_table_station", (containerId, inventory) -> new StationMenu(StationType.SMITHING_TABLE, containerId, inventory));
		registerProfession(platform, "smith", poi -> poi.is(PoiTypes.ARMORER) || poi.is(PoiTypes.TOOLSMITH), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_ARMORER,
			VillagerProfession.ARMORER);
	}

	/**
	 * The pit digger works at the pit stake: it digs out the pit the player marked with a
	 * ring of string and brings everything it digs to its chests.
	 */
	private static void registerPitDigger(Platform platform) {
		PIT_STAKE = platform.register(Registries.BLOCK, "pit_stake", () -> new PitStakeBlock(
			BlockBehaviour.Properties.of()
				.setId(ResourceKey.create(Registries.BLOCK, id("pit_stake")))
				.mapColor(MapColor.WOOD)
				.strength(1.5F)
				.sound(SoundType.WOOD)
				.ignitedByLava()
				.noOcclusion()
		));
		PIT_STAKE_ITEM = platform.register(Registries.ITEM, "pit_stake", () -> new BlockItem(
			PIT_STAKE.get(),
			itemProperties("pit_stake").useBlockDescriptionPrefix()
		));
		PIT_STAKE_MENU = platform.registerMenu("pit_stake", (containerId, inventory) -> new StationMenu(StationType.PIT_STAKE, containerId, inventory));
		platform.registerPoi("pit_digger", () -> PIT_STAKE.get(), 1, 1);
		registerProfession(platform, "pit_digger", PIT_DIGGER_POI, ImmutableSet.of(), SoundEvents.VILLAGER_WORK_TOOLSMITH);
	}

	/**
	 * The kiln operator works at the kiln, a furnace for building materials: it fetches what
	 * the kiln fires and fuel from its input chests and brings the results to its output
	 * chests. The button with the kiln operator's face in the kiln's screen opens the overview.
	 */
	private static void registerKilnOperator(Platform platform) {
		KILN = platform.register(Registries.BLOCK, "kiln", () -> new KilnBlock(
			BlockBehaviour.Properties.of()
				.setId(ResourceKey.create(Registries.BLOCK, id("kiln")))
				.mapColor(MapColor.COLOR_RED)
				.instrument(NoteBlockInstrument.BASEDRUM)
				.requiresCorrectToolForDrops()
				.strength(3.5F)
				.lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 13 : 0)
		));
		KILN_ITEM = platform.register(Registries.ITEM, "kiln", () -> new BlockItem(
			KILN.get(),
			itemProperties("kiln").useBlockDescriptionPrefix()
		));
		KILN_BLOCK_ENTITY = registerBlockEntityType(platform, "kiln", KilnBlockEntity::new, () -> KILN.get());
		KILN_MENU = platform.registerMenu("kiln", KilnMenu::new);
		KILN_STATION_MENU = platform.registerMenu("kiln_station", (containerId, inventory) -> new StationMenu(StationType.KILN, containerId, inventory));
		platform.registerPoi("kiln_operator", () -> KILN.get(), 1, 1);
		registerProfession(platform, "kiln_operator", KILN_OPERATOR_POI, ImmutableSet.of(), SoundEvents.VILLAGER_WORK_ARMORER);
	}

	/**
	 * The shop assistant works at the trading counter: it sells the goods from its input
	 * chests to the villagers around, through their own trades, and brings the emeralds to
	 * its output chests.
	 */
	private static void registerShopAssistant(Platform platform) {
		TRADING_COUNTER = platform.register(Registries.BLOCK, "trading_counter", () -> new TradingCounterBlock(
			BlockBehaviour.Properties.of()
				.setId(ResourceKey.create(Registries.BLOCK, id("trading_counter")))
				.mapColor(MapColor.WOOD)
				.strength(2.5F)
				.sound(SoundType.WOOD)
				.ignitedByLava()
				.noOcclusion()
		));
		TRADING_COUNTER_ITEM = platform.register(Registries.ITEM, "trading_counter", () -> new BlockItem(
			TRADING_COUNTER.get(),
			itemProperties("trading_counter").useBlockDescriptionPrefix()
		));
		TRADING_COUNTER_MENU = platform.registerMenu("trading_counter", (containerId, inventory) -> new StationMenu(StationType.TRADING_COUNTER, containerId, inventory));
		platform.registerPoi("shop_assistant", () -> TRADING_COUNTER.get(), 1, 1);
		registerProfession(platform, "shop_assistant", SHOP_ASSISTANT_POI, ImmutableSet.of(), SoundEvents.VILLAGER_WORK_LIBRARIAN);
	}

	/**
	 * The miner works at the mining bench: it digs the tunnel the player picked (3×3 with
	 * supports, side tunnels, or stairs down) and brings what it digs to its chests. For
	 * players the bench is a mill: cobblestone to gravel, gravel to sand and flint.
	 */
	private static void registerMiner(Platform platform) {
		MINING_BENCH = platform.register(Registries.BLOCK, "mining_bench", () -> new MiningBenchBlock(
			BlockBehaviour.Properties.of()
				.setId(ResourceKey.create(Registries.BLOCK, id("mining_bench")))
				.mapColor(MapColor.STONE)
				.instrument(NoteBlockInstrument.BASEDRUM)
				.requiresCorrectToolForDrops()
				.strength(3.0F)
				.noOcclusion()
		));
		MINING_BENCH_ITEM = platform.register(Registries.ITEM, "mining_bench", () -> new BlockItem(
			MINING_BENCH.get(),
			itemProperties("mining_bench").useBlockDescriptionPrefix()
		));
		MINING_BENCH_MENU = platform.registerMenu("mining_bench", (containerId, inventory) -> new StationMenu(StationType.MINING_BENCH, containerId, inventory));
		MILL_MENU = platform.registerMenu("mill", (containerId, inventory) -> new CutterMenu(MILL_KIND, containerId, inventory));
		MILLING = registerRecipeType(platform, "milling");
		MILLING_SERIALIZER = registerRecipeSerializer(platform, "milling", () -> new RecipeSerializer<>(
			MillingRecipe.MAP_CODEC, MillingRecipe.STREAM_CODEC
		));
		platform.registerPoi("miner", () -> MINING_BENCH.get(), 1, 1);
		registerProfession(platform, "miner", MINER_POI, ImmutableSet.of(), SoundEvents.VILLAGER_WORK_TOOLSMITH);
	}

	/**
	 * The village chest: a chest the workers nearby take their wage, tools and what they use
	 * up from. A point of interest without job, so the workers find it quickly.
	 */
	private static void registerVillageChest(Platform platform) {
		VILLAGE_CHEST = platform.register(Registries.BLOCK, "village_chest", () -> new VillageChestBlock(
			BlockBehaviour.Properties.of()
				.setId(ResourceKey.create(Registries.BLOCK, id("village_chest")))
				.mapColor(MapColor.WOOD)
				.instrument(NoteBlockInstrument.BASS)
				.strength(2.5F)
				.sound(SoundType.WOOD)
				.ignitedByLava()
		));
		VILLAGE_CHEST_ITEM = platform.register(Registries.ITEM, "village_chest", () -> new BlockItem(
			VILLAGE_CHEST.get(),
			itemProperties("village_chest").useBlockDescriptionPrefix()
		));
		VILLAGE_CHEST_BLOCK_ENTITY = registerBlockEntityType(platform, "village_chest", VillageChestBlockEntity::new, () -> VILLAGE_CHEST.get());
		VILLAGE_CHEST_MENU = platform.registerMenu("village_chest", VillageChestMenu::single);
		VILLAGE_CHEST_DOUBLE_MENU = platform.registerMenu("village_chest_double", VillageChestMenu::doubleChest);
		platform.registerPoi("village_chest", () -> VILLAGE_CHEST.get(), 0, 1);
	}

	@SuppressWarnings("unchecked")
	private static <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntityType(
		Platform platform, String name, BlockEntityType.BlockEntitySupplier<T> factory, Supplier<Block> block
	) {
		Supplier<BlockEntityType<?>> registered = platform.register(Registries.BLOCK_ENTITY_TYPE, name, () -> new BlockEntityType<>(factory, Set.of(block.get())));
		return () -> (BlockEntityType<T>) registered.get();
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
