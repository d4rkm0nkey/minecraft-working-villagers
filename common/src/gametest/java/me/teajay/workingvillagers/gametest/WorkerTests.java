package me.teajay.workingvillagers.gametest;

import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.carrier.CarrierData;
import me.teajay.workingvillagers.carrier.Carriers;
import me.teajay.workingvillagers.carrier.Route;
import me.teajay.workingvillagers.item.WorkContractItem;
import me.teajay.workingvillagers.job.GuardPost;
import me.teajay.workingvillagers.job.HiredVillager;
import me.teajay.workingvillagers.job.TendAnimals;
import me.teajay.workingvillagers.job.TendSheep;
import me.teajay.workingvillagers.job.WorkerLevels;
import me.teajay.workingvillagers.job.WorkerNotice;
import me.teajay.workingvillagers.job.WorkerStatus;
import me.teajay.workingvillagers.job.WorkerSupplies;
import me.teajay.workingvillagers.job.ChopTrees;
import me.teajay.workingvillagers.network.PitPreviewPayload;
import me.teajay.workingvillagers.station.Pit;
import me.teajay.workingvillagers.station.PitLimits;
import me.teajay.workingvillagers.station.PitPreviews;
import me.teajay.workingvillagers.station.StationType;
import me.teajay.workingvillagers.station.Workstations.ChestRole;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.registries.Registries;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.entity.EquipmentSlot;
import me.teajay.workingvillagers.job.TanLeather;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.core.Holder;
import me.teajay.workingvillagers.crafting.BrewingPaths;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.phys.Vec3;

/**
 * The game tests: hiring, and one test per profession that sets up a station like a player
 * would and checks that the worker gets its wage and delivers products. The tests that need
 * work run in the work hours (environment {@code workingvillagers:work_hours}); see the
 * test instances in {@code data/workingvillagers/test_instance}.
 */
public final class WorkerTests {
	private static final BlockPos WORKER = new BlockPos(6, 1, 6);
	private static final BlockPos STATION = new BlockPos(8, 1, 8);
	private static final BlockPos INPUT = new BlockPos(10, 1, 6);
	private static final BlockPos OUTPUT = new BlockPos(12, 1, 6);
	private static final BlockPos VILLAGE_CHEST = new BlockPos(4, 1, 12);
	/** The middle of the sheep pen, on the floor. */
	private static final BlockPos PEN = new BlockPos(15, 0, 15);

	private WorkerTests() {
	}

	/**
	 * Registers every test function under its name (the path of the test instance). Each sets
	 * the config values of the tests when it starts: the mod may load its config file after
	 * this, the order of the mod initializers is not fixed.
	 */
	public static void register(BiConsumer<String, Consumer<GameTestHelper>> functions) {
		BiConsumer<String, Consumer<GameTestHelper>> registry = (name, test) -> functions.accept(name, helper -> {
			configure();
			test.accept(helper);
		});
		MinerTests.register(registry);
		LevelTests.register(registry);
		registry.accept("contract_hires_unemployed", WorkerTests::contractHiresUnemployed);
		registry.accept("contract_rejects_employed", WorkerTests::contractRejectsEmployed);
		registry.accept("lumberjack_fells_tree", WorkerTests::lumberjackFellsTree);
		registry.accept("lumberjack_fells_tree_on_mud", WorkerTests::lumberjackFellsTreeOnMud);
		registry.accept("lumberjack_fells_tree_on_grass", WorkerTests::lumberjackFellsTreeOnGrass);
		registry.accept("lumberjack_hired_late_works_for_free", WorkerTests::lumberjackHiredLateWorksForFree);
		registry.accept("lumberjack_hired_before_pays_late", WorkerTests::lumberjackHiredBeforePaysLate);
		registry.accept("lumberjack_stands_at_trunk_on_path", WorkerTests::lumberjackStandsAtTrunkOnPath);
		registry.accept("lumberjack_spares_builds", WorkerTests::lumberjackSparesBuilds);
		registry.accept("lumberjack_clears_leaves", WorkerTests::lumberjackClearsLeaves);
		registry.accept("lumberjack_replants_later", WorkerTests::lumberjackReplantsLater);
		registry.accept("no_wage_no_work", WorkerTests::noWageNoWork);
		registry.accept("stuck_worker_is_brought_back", WorkerTests::stuckWorkerIsBroughtBack);
		registry.accept("sawyer_cuts_planks", WorkerTests::sawyerCutsPlanks);
		registry.accept("stone_sawyer_cuts_bricks", WorkerTests::stoneSawyerCutsBricks);
		registry.accept("carpenter_makes_chests", WorkerTests::carpenterMakesChests);
		registry.accept("carpenter_makes_several_products", WorkerTests::carpenterMakesSeveralProducts);
		registry.accept("carpenter_skips_product_without_material", WorkerTests::carpenterSkipsProductWithoutMaterial);
		registry.accept("sawyer_cuts_several_shapes", WorkerTests::sawyerCutsSeveralShapes);
		registry.accept("kiln_operator_fires_several_products", WorkerTests::kilnOperatorFiresSeveralProducts);
		registry.accept("fletcher_makes_several_products", WorkerTests::fletcherMakesSeveralProducts);
		registry.accept("weaponsmith_forges_several_products", WorkerTests::weaponsmithForgesSeveralProducts);
		registry.accept("tanner_makes_several_products", WorkerTests::tannerMakesSeveralProducts);
		registry.accept("carpenter_fills_order", WorkerTests::carpenterFillsOrder);
		registry.accept("carpenter_takes_material_from_village_chest", WorkerTests::carpenterTakesMaterialFromVillageChest);
		registry.accept("carpenter_shows_missing_material", WorkerTests::carpenterShowsMissingMaterial);
		registry.accept("carpenter_without_material_costs_nothing", WorkerTests::carpenterWithoutMaterialCostsNothing);
		registry.accept("carpenter_makes_missing_ingredient", WorkerTests::carpenterMakesMissingIngredient);
		registry.accept("sawyer_fills_order", WorkerTests::sawyerFillsOrder);
		registry.accept("kiln_operator_keeps_finished_order", WorkerTests::kilnOperatorKeepsFinishedOrder);
		registry.accept("fletcher_makes_arrows_for_spectral_arrows", WorkerTests::fletcherMakesArrowsForSpectralArrows);
		registry.accept("carrier_moves_items", WorkerTests::carrierMovesItems);
		registry.accept("farmer_harvests_and_replants", WorkerTests::farmerHarvestsAndReplants);
		registry.accept("workers_do_not_trample_farmland", WorkerTests::workersDoNotTrampleFarmland);
		registry.accept("worker_finds_workplace", WorkerTests::workerFindsWorkplace);
		registry.accept("worker_picks_up_wage", WorkerTests::workerPicksUpWage);
		registry.accept("smith_smelts_ore", WorkerTests::smithSmeltsOre);
		registry.accept("replaced_station_starts_fresh", WorkerTests::replacedStationStartsFresh);
		registry.accept("double_chest_is_linked_once", WorkerTests::doubleChestIsLinkedOnce);
		registry.accept("kiln_operator_fires_glass", WorkerTests::kilnOperatorFiresGlass);
		registry.accept("kiln_operator_fires_smooth_stone", WorkerTests::kilnOperatorFiresSmoothStone);
		registry.accept("pit_digger_digs_pit", WorkerTests::pitDiggerDigsPit);
		registry.accept("pit_digger_digs_small_pit", WorkerTests::pitDiggerDigsSmallPit);
		registry.accept("pit_digger_finishes_half_dug_pit", WorkerTests::pitDiggerFinishesHalfDugPit);
		registry.accept("pit_digger_climbs_out_of_half_dug_pit", WorkerTests::pitDiggerClimbsOutOfHalfDugPit);
		registry.accept("pit_digger_finishes_played_pit", WorkerTests::pitDiggerFinishesPlayedPit);
		registry.accept("pit_stake_takes_ring_it_touches", WorkerTests::pitStakeTakesRingItTouches);
		registry.accept("pit_limits_grow_with_level", WorkerTests::pitLimitsGrowWithLevel);
		registry.accept("pit_digger_builds_storeys", helper -> pitDiggerBuildsStoreys(helper, 10));
		registry.accept("pit_digger_builds_two_storeys", helper -> pitDiggerBuildsStoreys(helper, 14));
		registry.accept("shop_assistant_sells_wheat", WorkerTests::shopAssistantSellsWheat);
		registry.accept("ordinary_villager_keeps_off_set_up_station", WorkerTests::ordinaryVillagerKeepsOffSetUpStation);
		registry.accept("assign_displaces_ordinary_villager", WorkerTests::assignDisplacesOrdinaryVillager);
		registry.accept("worker_takes_lost_station", WorkerTests::workerTakesLostStation);
		registry.accept("village_chest_supplies_lumberjack", WorkerTests::villageChestSuppliesLumberjack);
		registry.accept("village_chest_keeps_named_tools", WorkerTests::villageChestKeepsNamedTools);
		registry.accept("lumberjack_gives_back_worn_axe", WorkerTests::lumberjackGivesBackWornAxe);
		registry.accept("lumberjack_swaps_worn_axe", WorkerTests::lumberjackSwapsWornAxe);
		registry.accept("worn_axe_goes_to_output_chest", WorkerTests::wornAxeGoesToOutputChest);
		registry.accept("stone_axe_is_used_up", WorkerTests::stoneAxeIsUsedUp);
		registry.accept("smith_repairs_worn_axe", WorkerTests::smithRepairsWornAxe);
		registry.accept("wage_box_outlives_worker", WorkerTests::wageBoxOutlivesWorker);
		registry.accept("broken_station_drops_wage", WorkerTests::brokenStationDropsWage);
		registry.accept("right_click_fills_wage_box", WorkerTests::rightClickFillsWageBox);
		registry.accept("smith_repairs_input_chest", WorkerTests::smithRepairsInputChest);
		registry.accept("shepherd_shears_over_fence", WorkerTests::shepherdShearsOverFence);
		registry.accept("shepherd_breeds_sheep", WorkerTests::shepherdBreedsSheep);
		registry.accept("contract_hires_at_job_site", WorkerTests::contractHiresAtJobSite);
		registry.accept("contract_keeps_trades", WorkerTests::contractKeepsTrades);
		registry.accept("butcher_slaughters_surplus", WorkerTests::butcherSlaughtersSurplus);
		registry.accept("fisher_fishes_from_shore", WorkerTests::fisherFishesFromShore);
		registry.accept("fisher_rows_out", WorkerTests::fisherRowsOut);
		registry.accept("fisher_rows_out_past_pond", WorkerTests::fisherRowsOutPastPond);
		registry.accept("fletcher_makes_arrows_from_gravel", WorkerTests::fletcherMakesArrowsFromGravel);
		registry.accept("fletcher_makes_arrows_from_nuggets", WorkerTests::fletcherMakesArrowsFromNuggets);
		registry.accept("fletcher_keeps_stock", WorkerTests::fletcherKeepsStock);
		registry.accept("apothecary_makes_bottles", WorkerTests::apothecaryMakesBottles);
		registry.accept("tanner_makes_books", WorkerTests::tannerMakesBooks);
		registry.accept("worker_stores_carried_wage", WorkerTests::workerStoresCarriedWage);
		registry.accept("weaponsmith_forges_axes", WorkerTests::weaponsmithForgesAxes);
		registry.accept("librarian_enchants_book", WorkerTests::librarianEnchantsBook);
		registry.accept("librarian_enchants_at_level_30", WorkerTests::librarianEnchantsAtLevel30);
		registry.accept("weaponsmith_grinds_worn_tools_together", WorkerTests::weaponsmithGrindsWornToolsTogether);
		registry.accept("weaponsmith_disenchants", WorkerTests::weaponsmithDisenchants);
		registry.accept("tanner_makes_leather_from_hides", WorkerTests::tannerMakesLeatherFromHides);
		registry.accept("tanner_tans_rotten_flesh", WorkerTests::tannerTansRottenFlesh);
		registry.accept("tanner_fills_cauldron", WorkerTests::tannerFillsCauldron);
		registry.accept("tanner_dyes_armor", WorkerTests::tannerDyesArmor);
		registry.accept("tanner_recolors_armor", WorkerTests::tannerRecolorsArmor);
		registry.accept("guard_wears_uniform", WorkerTests::guardWearsUniform);
		registry.accept("guard_takes_armor_from_village_chest", WorkerTests::guardTakesArmorFromVillageChest);
		registry.accept("apothecary_fills_water", WorkerTests::apothecaryFillsWater);
		registry.accept("apothecary_brews_healing", WorkerTests::apothecaryBrewsHealing);
		registry.accept("apothecary_heals_worker", WorkerTests::apothecaryHealsWorker);
		registry.accept("apothecary_harvests_nether_wart", WorkerTests::apothecaryHarvestsNetherWart);
		registry.accept("guard_shoots_zombie", WorkerTests::guardShootsZombie);
		registry.accept("guard_collects_arrows", WorkerTests::guardCollectsArrows);
		registry.accept("guard_bottles_experience", WorkerTests::guardBottlesExperience);
		registry.accept("guard_bottles_reach_smith", WorkerTests::guardBottlesReachSmith);
		registry.accept("guard_hunts", WorkerTests::guardHunts);
		registry.accept("guard_fights_raid_by_day", WorkerTests::guardFightsRaidByDay);
		registry.accept("smith_repairs_bow_with_experience", WorkerTests::smithRepairsBowWithExperience);
		registry.accept("smith_waits_for_experience", WorkerTests::smithWaitsForExperience);
		registry.accept("fisher_gives_back_enchanted_rod", WorkerTests::fisherGivesBackEnchantedRod);
		registry.accept("butcher_breeds_pigs", WorkerTests::butcherBreedsPigs);
		registry.accept("butcher_fetches_feed_of_its_animals", WorkerTests::butcherFetchesFeedOfItsAnimals);
		registry.accept("farmer_swaps_worn_hoe", WorkerTests::farmerSwapsWornHoe);
		registry.accept("butcher_smokes_meat", WorkerTests::butcherSmokesMeat);
		for (StationType type : StationType.values()) {
			String name = type.name().toLowerCase(Locale.ROOT);
			registry.accept("worker_finds_" + name, helper -> workerFinds(helper, type));
		}
	}

	/**
	 * A workstation that counts as taken although nobody works there or walks there (a
	 * ticket lost by earlier versions) is freed and taken by a hired villager.
	 */
	private static void workerTakesLostStation(GameTestHelper helper) {
		BlockPos station = STATION.below();
		helper.setBlock(station, WorkingVillagers.PIT_STAKE.get());
		BlockPos absolute = helper.absolutePos(station);
		helper.getLevel().getPoiManager().take(poi -> true, (poi, pos) -> pos.equals(absolute), absolute, 1);
		Villager worker = TestWorld.hireVillager(helper, new BlockPos(2, 0, 2));
		helper.succeedWhen(() -> helper.assertTrue(worker.getVillagerData().profession().is(WorkingVillagers.PIT_DIGGER_PROFESSION),
			"The lost station was not taken: " + TestWorld.describe(worker)));
	}

	/**
	 * A hired villager without a job walks to a workstation standing on the ground, like a
	 * player places it, and takes the job there.
	 */
	private static void workerFinds(GameTestHelper helper, StationType type) {
		// On the floor, where a player puts it (y 1 of the platform is one block above the grass)
		BlockPos station = STATION.below();
		helper.setBlock(station, type.block());
		Villager worker = TestWorld.hireVillager(helper, new BlockPos(2, 0, 2));
		helper.succeedWhen(() -> helper.assertTrue(TestWorld.station(helper, station).occupant(helper.getLevel()).isPresent(),
			"Nobody took the job: " + worker.getVillagerData().profession().getRegisteredName()
				+ ", potential " + worker.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE) + ", job " + worker.getBrain().getMemory(MemoryModuleType.JOB_SITE)
				+ ", at " + worker.blockPosition() + ", station " + helper.absolutePos(station)));
	}

	private static void configure() {
		// The work areas of the tests are laid out for the full radius; the level tests set the level they need
		Config.NOVICE_AREA.set(100);
		// The tests run side by side: the village chests of one test must not reach the workers of the next
		Config.VILLAGE_CHEST_RADIUS.set(8);
		// Quick bites, short trips
		Config.MIN_BITE_SECONDS.set(1);
		Config.MAX_BITE_SECONDS.set(1);
		Config.CATCHES_PER_TRIP.set(3);
		// Rotten flesh tans in a few seconds
		Config.TANNING_SECONDS.set(5);
		MinerTests.configure();
		// The pit digger digs as fast as a player
		Config.DIG_SLOWDOWN.set(1.0);
		Config.MIN_DIG_SECONDS.set(0.25);
	}

	private static void contractHiresUnemployed(GameTestHelper helper) {
		Villager villager = helper.spawn(EntityTypes.VILLAGER, WORKER);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack contract = new ItemStack(WorkingVillagers.WORK_CONTRACT.get());
		WorkContractItem.hire(villager, player, contract, InteractionHand.MAIN_HAND);
		helper.assertTrue(((HiredVillager) villager).workingvillagers$isHired(), "The villager is not hired");
		helper.assertFalse(villager.hasCustomName(), "The hired villager got a name, random names are off by default");
		helper.assertTrue(contract.isEmpty(), "The contract was not used up");
		helper.succeed();
	}

	/** Not hired: a farmer without a job site, a baby and a cartographer (no job of this mod at a cartography table). */
	private static void contractRejectsEmployed(GameTestHelper helper) {
		Villager farmer = helper.spawn(EntityTypes.VILLAGER, WORKER);
		farmer.setVillagerData(farmer.getVillagerData().withProfession(helper.getLevel().registryAccess(), VillagerProfession.FARMER));
		Villager baby = helper.spawn(EntityTypes.VILLAGER, WORKER.east(2));
		baby.setAge(-24000);
		Villager cartographer = vanillaWorker(helper, new BlockPos(9, 0, 2), Blocks.CARTOGRAPHY_TABLE, VillagerProfession.CARTOGRAPHER);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		for (Villager villager : new Villager[] {farmer, baby, cartographer}) {
			ItemStack contract = new ItemStack(WorkingVillagers.WORK_CONTRACT.get());
			WorkContractItem.hire(villager, player, contract, InteractionHand.MAIN_HAND);
			helper.assertFalse(((HiredVillager) villager).workingvillagers$isHired(), "Hired although it must not be: " + villager.getVillagerData().profession().getRegisteredName());
			helper.assertFalse(contract.isEmpty(), "The contract was used up although nobody was hired");
		}
		helper.succeed();
	}

	/** A vanilla villager with the profession, working at the block next to it. */
	private static Villager vanillaWorker(GameTestHelper helper, BlockPos block, net.minecraft.world.level.block.Block type,
		net.minecraft.resources.ResourceKey<VillagerProfession> profession) {
		helper.setBlock(block, type);
		Villager villager = helper.spawn(EntityTypes.VILLAGER, block.offset(0, 1, 2));
		BlockPos absolute = helper.absolutePos(block);
		helper.getLevel().getPoiManager().take(poi -> true, (poi, pos) -> pos.equals(absolute), absolute, 1);
		villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, net.minecraft.core.GlobalPos.of(helper.getLevel().dimension(), absolute));
		villager.setVillagerData(villager.getVillagerData().withProfession(helper.getLevel().registryAccess(), profession));
		return villager;
	}

	/** A vanilla farmer that has never traded is hired and stays at its composter, as the farmer of this mod. */
	private static void contractHiresAtJobSite(GameTestHelper helper) {
		BlockPos composter = STATION.below();
		Villager farmer = vanillaWorker(helper, composter, Blocks.COMPOSTER, VillagerProfession.FARMER);
		ItemStack contract = new ItemStack(WorkingVillagers.WORK_CONTRACT.get());
		WorkContractItem.hire(farmer, helper.makeMockPlayer(GameType.SURVIVAL), contract, InteractionHand.MAIN_HAND);
		helper.assertTrue(((HiredVillager) farmer).workingvillagers$isHired(), "The farmer was not hired");
		helper.assertTrue(contract.isEmpty(), "The contract was not used up");
		helper.assertTrue(farmer.getVillagerData().profession().is(WorkingVillagers.FARMER_PROFESSION), "Not the farmer of this mod: "
			+ farmer.getVillagerData().profession().getRegisteredName());
		helper.assertTrue(farmer.getBrain().getMemory(MemoryModuleType.JOB_SITE).map(site -> site.pos().equals(helper.absolutePos(composter))).orElse(false),
			"The farmer left its composter");
		helper.assertTrue(TestWorld.station(helper, composter).worker() != null, "The composter does not know its worker");
		helper.assertFalse(farmer.getOffers().isEmpty(), "The farmer of this mod has no trades");
		helper.succeed();
	}

	/** A vanilla farmer somebody traded with keeps its trades and its experience when it is hired. */
	private static void contractKeepsTrades(GameTestHelper helper) {
		BlockPos composter = STATION.below();
		Villager farmer = vanillaWorker(helper, composter, Blocks.COMPOSTER, VillagerProfession.FARMER);
		net.minecraft.world.item.trading.MerchantOffers offers = new net.minecraft.world.item.trading.MerchantOffers();
		offers.add(new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.trading.ItemCost(Items.WHEAT, 20),
			new ItemStack(Items.EMERALD), 12, 2, 0.05F));
		farmer.setOffers(offers);
		farmer.setVillagerXp(5);
		WorkContractItem.hire(farmer, helper.makeMockPlayer(GameType.SURVIVAL), new ItemStack(WorkingVillagers.WORK_CONTRACT.get()), InteractionHand.MAIN_HAND);
		helper.assertTrue(((HiredVillager) farmer).workingvillagers$isHired(), "The farmer was not hired");
		helper.assertTrue(farmer.getVillagerData().profession().is(WorkingVillagers.FARMER_PROFESSION), "Not the farmer of this mod");
		helper.assertTrue(farmer.getOffers().size() == 1 && farmer.getOffers().getFirst().getCostA().is(Items.WHEAT), "The trades were lost: " + farmer.getOffers().size());
		helper.assertTrue(farmer.getVillagerXp() == 5, "The experience was lost");
		helper.succeed();
	}

	/**
	 * Water two deep from {@code from} to {@code to} (inclusive, x and z), its surface level
	 * with the floor's air, a rim of dirt around it and stone below.
	 */
	private static void pond(GameTestHelper helper, int fromX, int fromZ, int toX, int toZ) {
		for (int x = fromX - 1; x <= toX + 1; x++) {
			for (int z = fromZ - 1; z <= toZ + 1; z++) {
				boolean inside = x >= fromX && x <= toX && z >= fromZ && z <= toZ;
				helper.setBlock(new BlockPos(x, -2, z), Blocks.STONE);
				helper.setBlock(new BlockPos(x, -1, z), inside ? Blocks.WATER : Blocks.DIRT);
				helper.setBlock(new BlockPos(x, 0, z), inside ? Blocks.WATER : Blocks.DIRT);
			}
		}
	}

	/** A small pond next to the barrel: the fisher fishes from the shore and brings the catch to the chest. */
	private static void fisherFishesFromShore(GameTestHelper helper) {
		helper.setBlock(STATION.below(), Blocks.BARREL);
		pond(helper, 12, 12, 15, 15);
		TestWorld.chest(helper, OUTPUT);
		TestWorld.link(helper, STATION.below(), OUTPUT, ChestRole.OUTPUT);
		Villager worker = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, worker, STATION.below());
		TestWorld.give(helper, worker, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.FISHING_ROD));
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> !stack.is(Items.FISHING_ROD), 3, "the catch in the output chest");
			helper.assertTrue(TestWorld.count(helper, OUTPUT, stack -> stack.is(Items.FISHING_ROD)) == 0, "The fisher delivered its rod");
		});
	}

	/**
	 * Two rods about to break: the plain one is cheap and used up, the enchanted one is given
	 * back into the output chest (there is no village chest) instead of being broken.
	 */
	private static void fisherGivesBackEnchantedRod(GameTestHelper helper) {
		helper.setBlock(STATION.below(), Blocks.BARREL);
		pond(helper, 12, 12, 15, 15);
		TestWorld.chest(helper, OUTPUT);
		TestWorld.link(helper, STATION.below(), OUTPUT, ChestRole.OUTPUT);
		Villager worker = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, worker, STATION.below());
		TestWorld.give(helper, worker, new ItemStack(Items.EMERALD, 2));
		ItemStack enchanted = new ItemStack(Items.FISHING_ROD);
		enchanted.enchant(helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
			.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LURE), 1);
		enchanted.setDamageValue(enchanted.getMaxDamage() - 2);
		ItemStack plain = new ItemStack(Items.FISHING_ROD);
		plain.setDamageValue(plain.getMaxDamage() - 2);
		// Into its inventory: a worker does not accept a worn tool from a player
		worker.getInventory().addItem(enchanted);
		worker.getInventory().addItem(plain);
		helper.succeedWhen(() -> {
			helper.assertTrue(TestWorld.count(helper, OUTPUT, stack -> stack.is(Items.FISHING_ROD) && stack.isEnchanted()) == 1,
				"The enchanted rod is not in the output chest: " + TestWorld.describe(worker));
			helper.assertTrue(TestWorld.count(helper, OUTPUT, stack -> stack.is(Items.FISHING_ROD) && !stack.isEnchanted()) == 0, "The plain rod was given back");
			helper.assertTrue(TestWorld.count(helper, OUTPUT, stack -> !stack.is(Items.FISHING_ROD)) >= 2, "Not caught with the plain rod until it broke");
			helper.assertTrue(worker.getInventory().countItem(Items.FISHING_ROD) == 0, "The plain rod did not break");
		});
	}

	/** A fletching table with an input chest holding {@code material}, an output chest and the order. */
	private static Villager fletcher(GameTestHelper helper, Item order, int stock, ItemStack... material) {
		BlockPos table = STATION.below();
		helper.setBlock(table, Blocks.FLETCHING_TABLE);
		TestWorld.chest(helper, INPUT, material);
		TestWorld.chest(helper, OUTPUT);
		TestWorld.link(helper, table, INPUT, ChestRole.INPUT);
		TestWorld.link(helper, table, OUTPUT, ChestRole.OUTPUT);
		TestWorld.station(helper, table).setOrder(order);
		TestWorld.station(helper, table).setStock(stock);
		Villager worker = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, worker, table);
		TestWorld.give(helper, worker, new ItemStack(Items.EMERALD, 2));
		return worker;
	}

	/** Gravel, sticks and feathers: the fletcher knaps flint from the gravel and makes 4 arrows of each set. */
	private static void fletcherMakesArrowsFromGravel(GameTestHelper helper) {
		Villager worker = fletcher(helper, Items.ARROW, 0, new ItemStack(Items.GRAVEL, 6), new ItemStack(Items.STICK, 6), new ItemStack(Items.FEATHER, 6));
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.ARROW), 24, "24 arrows in the output chest");
			helper.assertTrue(TestWorld.count(helper, INPUT, stack -> stack.is(Items.GRAVEL)) == 0, "Gravel left in the input chest");
		});
	}

	/** An iron ingot instead of flint: 9 nuggets, 9 sets of arrows. */
	private static void fletcherMakesArrowsFromNuggets(GameTestHelper helper) {
		Villager worker = fletcher(helper, Items.ARROW, 0, new ItemStack(Items.IRON_INGOT), new ItemStack(Items.STICK, 9), new ItemStack(Items.FEATHER, 9));
		helper.succeedWhen(() -> TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.ARROW), 36, "36 arrows in the output chest"));
	}

	/** A stock of 8: the fletcher makes 8 arrows and stops, although there is material for more. */
	private static void fletcherKeepsStock(GameTestHelper helper) {
		Villager worker = fletcher(helper, Items.ARROW, 8, new ItemStack(Items.FLINT, 16), new ItemStack(Items.STICK, 16), new ItemStack(Items.FEATHER, 16));
		helper.runAfterDelay(1200, () -> {
			int arrows = TestWorld.count(helper, OUTPUT, stack -> stack.is(Items.ARROW));
			helper.assertTrue(arrows == 8, "Arrows in the output chest: " + arrows + ", " + TestWorld.describe(worker));
			helper.succeed();
		});
	}

	/**
	 * At night a guard at its target shoots a zombie dead with its arrows. A cow stands in the
	 * way: the guard's arrows fly through it.
	 */
	private static void guardShootsZombie(GameTestHelper helper) {
		BlockPos target = STATION.below();
		helper.setBlock(target, Blocks.TARGET);
		Villager guard = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, guard, target);
		TestWorld.give(helper, guard, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.BOW), new ItemStack(Items.ARROW, 32));
		GuardPost.setTimeForTests(guard, 14000);
		// A husk does not burn in the daylight of the other tests
		net.minecraft.world.entity.monster.zombie.Husk zombie = helper.spawn(EntityTypes.HUSK, new BlockPos(18, 0, 18));
		net.minecraft.world.entity.animal.cow.Cow cow = helper.spawn(EntityTypes.COW, new BlockPos(13, 0, 13));
		cow.setNoAi(true);
		helper.succeedWhen(() -> {
			helper.assertTrue(!zombie.isAlive(), "The zombie is still alive: " + TestWorld.describe(guard));
			helper.assertTrue(zombie.getLastHurtByMob() == guard, "Not the guard killed the zombie");
			helper.assertTrue(guard.getInventory().countItem(Items.ARROW) < 32, "The guard used no arrows");
			helper.assertTrue(cow.getHealth() == cow.getMaxHealth(), "The guard hit the cow");
			helper.assertTrue(((HiredVillager) guard).workingvillagers$getExperience() >= 5, "The guard did not get the zombie's experience");
		});
	}

	/** In the morning a guard picks up its arrows that missed, but not somebody else's. */
	private static void guardCollectsArrows(GameTestHelper helper) {
		BlockPos target = STATION.below();
		helper.setBlock(target, Blocks.TARGET);
		Villager guard = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, guard, target);
		TestWorld.give(helper, guard, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.BOW), new ItemStack(Items.ARROW, 10));
		GuardPost.setTimeForTests(guard, 200);
		List<net.minecraft.world.entity.projectile.arrow.Arrow> missed = new java.util.ArrayList<>();
		for (BlockPos pos : List.of(new BlockPos(14, 0, 14), new BlockPos(4, 0, 15), new BlockPos(15, 0, 3))) {
			net.minecraft.world.entity.projectile.arrow.Arrow arrow = helper.spawn(EntityTypes.ARROW, pos);
			arrow.setOwner(guard);
			arrow.pickup = net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.ALLOWED;
			missed.add(arrow);
		}
		net.minecraft.world.entity.projectile.arrow.Arrow other = helper.spawn(EntityTypes.ARROW, new BlockPos(12, 0, 12));
		other.pickup = net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.ALLOWED;
		helper.succeedWhen(() -> {
			helper.assertTrue(missed.stream().noneMatch(arrow -> arrow.isAlive()), "Arrows left: " + TestWorld.describe(guard));
			helper.assertTrue(guard.getInventory().countItem(Items.ARROW) == 13, "Arrows carried: " + guard.getInventory().countItem(Items.ARROW));
			helper.assertTrue(other.isAlive(), "The guard picked up somebody else's arrow");
		});
	}

	/**
	 * A guard set to hunt shoots a zombie far outside its radius of 8 (the zombie stands
	 * still); standing watch it would leave it alone.
	 */
	private static void guardHunts(GameTestHelper helper) {
		BlockPos target = STATION.below();
		helper.setBlock(target, Blocks.TARGET);
		TestWorld.station(helper, target).setGuardRadius(8);
		TestWorld.station(helper, target).setHunting(true);
		Villager guard = TestWorld.hireVillager(helper, WORKER);
		// A journeyman hunts
		TestWorld.setLevel(guard, WorkerLevels.JOURNEYMAN);
		TestWorld.assign(helper, guard, target);
		TestWorld.give(helper, guard, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.BOW), new ItemStack(Items.ARROW, 32));
		GuardPost.setTimeForTests(guard, 14000);
		net.minecraft.world.entity.monster.zombie.Husk zombie = helper.spawn(EntityTypes.HUSK, new BlockPos(22, 0, 22));
		zombie.setNoAi(true);
		helper.succeedWhen(() -> helper.assertTrue(!zombie.isAlive() && zombie.getLastHurtByMob() == guard, "The hunting guard did not kill the zombie: "
			+ TestWorld.describe(guard)));
	}

	/**
	 * A raid in the daytime, when guards sleep: the guard works anyway. The raid ends right
	 * away again, the other tests run side by side and their villagers would hide.
	 */
	@SuppressWarnings("removal")
	private static void guardFightsRaidByDay(GameTestHelper helper) {
		BlockPos target = STATION.below();
		helper.setBlock(target, Blocks.TARGET);
		helper.setBlock(new BlockPos(4, 0, 4), Blocks.BELL);
		Villager guard = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, guard, target);
		GuardPost.setTimeForTests(guard, 6000);
		helper.assertTrue(GuardPost.activity(helper.getLevel(), guard) == net.minecraft.world.entity.schedule.Activity.REST, "At noon a guard sleeps");
		net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(8, 0, 4)));
		net.minecraft.world.entity.raid.Raid raid = helper.getLevel().getRaids().createOrExtendRaid(player, helper.absolutePos(target));
		helper.assertTrue(raid != null, "No raid started");
		boolean works = GuardPost.activity(helper.getLevel(), guard) == net.minecraft.world.entity.schedule.Activity.WORK;
		raid.stop();
		helper.assertTrue(works, "The guard does not work in a raid");
		helper.succeed();
	}

	/** In the morning a guard fills the experience of its kills into its glass bottles and brings them to its output chest. */
	private static void guardBottlesExperience(GameTestHelper helper) {
		BlockPos target = STATION.below();
		helper.setBlock(target, Blocks.TARGET);
		TestWorld.chest(helper, OUTPUT);
		TestWorld.link(helper, target, OUTPUT, ChestRole.OUTPUT);
		Villager guard = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, guard, target);
		TestWorld.give(helper, guard, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.BOW), new ItemStack(Items.ARROW, 10), new ItemStack(Items.GLASS_BOTTLE, 3));
		((HiredVillager) guard).workingvillagers$setExperience(20);
		GuardPost.setTimeForTests(guard, 200);
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, guard, OUTPUT, stack -> stack.is(Items.EXPERIENCE_BOTTLE), 2, "2 bottles o' enchanting in the output chest");
			helper.assertTrue(((HiredVillager) guard).workingvillagers$getExperience() == 6, "Experience left: " + ((HiredVillager) guard).workingvillagers$getExperience());
			helper.assertTrue(guard.getInventory().countItem(Items.GLASS_BOTTLE) == 1, "The guard did not keep its last glass bottle");
		});
	}

	/**
	 * One chest is the guard's output chest and the smith's input chest: the guard brings its
	 * bottles o' enchanting there, the smith repairs a bow in it with them. (A village chest as
	 * the guard's output chest would fill up with the loot of the guard.)
	 */
	private static void guardBottlesReachSmith(GameTestHelper helper) {
		helper.setBlock(STATION, Blocks.SMITHING_TABLE);
		ItemStack bow = new ItemStack(Items.BOW);
		bow.setDamageValue(300);
		Villager smith = processingStation(helper, bow);
		BlockPos target = new BlockPos(14, 0, 10);
		helper.setBlock(target, Blocks.TARGET);
		TestWorld.link(helper, target, INPUT, ChestRole.OUTPUT);
		Villager guard = TestWorld.hireVillager(helper, new BlockPos(14, 1, 12));
		TestWorld.assign(helper, guard, target);
		TestWorld.give(helper, guard, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.BOW), new ItemStack(Items.ARROW, 10), new ItemStack(Items.GLASS_BOTTLE, 10));
		// Enough for 9 bottles; the bow takes 8
		((HiredVillager) guard).workingvillagers$setExperience(63);
		GuardPost.setTimeForTests(guard, 200);
		helper.succeedWhen(() -> helper.assertTrue(TestWorld.count(helper, INPUT, stack -> stack.is(Items.BOW) && !stack.isDamaged()) == 1,
			"The bow is not repaired with the guard's bottles: smith " + TestWorld.describe(smith) + ", guard " + TestWorld.describe(guard)));
	}

	/** A bow has no repair material: the smith repairs it with bottles o' enchanting, a tenth of the durability each. */
	private static void smithRepairsBowWithExperience(GameTestHelper helper) {
		helper.setBlock(STATION, Blocks.SMITHING_TABLE);
		ItemStack bow = new ItemStack(Items.BOW);
		bow.setDamageValue(300);
		Villager worker = processingStation(helper, bow, new ItemStack(Items.EXPERIENCE_BOTTLE, 10));
		helper.succeedWhen(() -> {
			helper.assertTrue(TestWorld.count(helper, INPUT, stack -> stack.is(Items.BOW) && !stack.isDamaged()) == 1,
				"The bow is not repaired in the input chest: " + TestWorld.describe(worker));
			helper.assertTrue(TestWorld.count(helper, INPUT, stack -> stack.is(Items.EXPERIENCE_BOTTLE)) == 2, "The smith did not use 8 bottles");
		});
	}

	/** An enchanted pickaxe and diamonds, but no bottles o' enchanting: the smith does not repair it. */
	private static void smithWaitsForExperience(GameTestHelper helper) {
		helper.setBlock(STATION, Blocks.SMITHING_TABLE);
		ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
		pickaxe.setDamageValue(500);
		pickaxe.enchant(helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
			.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.EFFICIENCY), 3);
		Villager worker = processingStation(helper, pickaxe, new ItemStack(Items.DIAMOND, 3));
		helper.runAfterDelay(800, () -> {
			helper.assertTrue(TestWorld.count(helper, INPUT, stack -> stack.is(Items.DIAMOND)) == 3, "The smith used diamonds without bottles: " + TestWorld.describe(worker));
			helper.assertTrue(TestWorld.count(helper, INPUT, stack -> stack.is(Items.DIAMOND_PICKAXE) && stack.getDamageValue() == 500) == 1, "The pickaxe was repaired");
			helper.succeed();
		});
	}

	private static final BlockPos ENCHANTING_TABLE = new BlockPos(12, 0, 12);

	/** A lectern with an enchanting table nearby, an input chest holding {@code material} and an output chest. */
	private static Villager librarian(GameTestHelper helper, ItemStack... material) {
		BlockPos lectern = STATION.below();
		helper.setBlock(lectern, Blocks.LECTERN);
		helper.setBlock(ENCHANTING_TABLE, Blocks.ENCHANTING_TABLE);
		TestWorld.chest(helper, INPUT, material);
		TestWorld.chest(helper, OUTPUT);
		TestWorld.link(helper, lectern, INPUT, ChestRole.INPUT);
		TestWorld.link(helper, lectern, OUTPUT, ChestRole.OUTPUT);
		Villager worker = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, worker, lectern);
		TestWorld.give(helper, worker, new ItemStack(Items.EMERALD, 2));
		return worker;
	}

	/** A bare table (level 8): a book, 3 bottles o' enchanting and a lapis lazuli make an enchanted book. */
	private static void librarianEnchantsBook(GameTestHelper helper) {
		Villager worker = librarian(helper, new ItemStack(Items.BOOK), new ItemStack(Items.EXPERIENCE_BOTTLE, 3), new ItemStack(Items.LAPIS_LAZULI));
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.ENCHANTED_BOOK), 1, "an enchanted book in the output chest");
			helper.assertTrue(TestWorld.count(helper, INPUT, stack -> stack.is(Items.EXPERIENCE_BOTTLE)) == 0
				&& worker.getInventory().countItem(Items.EXPERIENCE_BOTTLE) == 0, "The bottles were not used");
		});
	}

	/** 15 bookshelves around the table: level 30, 10 bottles and 3 lapis lazuli for an iron pickaxe. */
	private static void librarianEnchantsAtLevel30(GameTestHelper helper) {
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				// A ring two blocks off the table, open at the front to get in
				if ((Math.abs(x) == 2 || Math.abs(z) == 2) && !(x == 0 && z == -2)) {
					helper.setBlock(ENCHANTING_TABLE.offset(x, 0, z), Blocks.BOOKSHELF);
				}
			}
		}
		Villager worker = librarian(helper, new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.EXPERIENCE_BOTTLE, 12), new ItemStack(Items.LAPIS_LAZULI, 3));
		// A master enchants up to level 30
		TestWorld.setLevel(worker, WorkerLevels.MASTER);
		// A master takes 3 emeralds a day
		TestWorld.give(helper, worker, new ItemStack(Items.EMERALD, 1));
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.IRON_PICKAXE) && stack.isEnchanted(), 1, "an enchanted pickaxe in the output chest");
			int left = TestWorld.count(helper, INPUT, stack -> stack.is(Items.EXPERIENCE_BOTTLE)) + worker.getInventory().countItem(Items.EXPERIENCE_BOTTLE)
				+ TestWorld.count(helper, OUTPUT, stack -> stack.is(Items.EXPERIENCE_BOTTLE));
			helper.assertTrue(left == 2, "Bottles left: " + left + " (12 - 10 for level 30)");
		});
	}

	/** A grindstone with an input chest holding {@code material}, an output chest and the order (null for none). */
	private static Villager weaponsmith(GameTestHelper helper, @Nullable Item order, ItemStack... material) {
		BlockPos grindstone = STATION.below();
		helper.setBlock(grindstone, Blocks.GRINDSTONE);
		TestWorld.chest(helper, INPUT, material);
		TestWorld.chest(helper, OUTPUT);
		TestWorld.link(helper, grindstone, INPUT, ChestRole.INPUT);
		TestWorld.link(helper, grindstone, OUTPUT, ChestRole.OUTPUT);
		TestWorld.station(helper, grindstone).setOrder(order);
		Villager worker = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, worker, grindstone);
		TestWorld.give(helper, worker, new ItemStack(Items.EMERALD, 2));
		return worker;
	}

	/** 6 iron ingots and 4 sticks: 2 iron axes. */
	private static void weaponsmithForgesAxes(GameTestHelper helper) {
		Villager worker = weaponsmith(helper, Items.IRON_AXE, new ItemStack(Items.IRON_INGOT, 6), new ItemStack(Items.STICK, 4));
		helper.succeedWhen(() -> TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.IRON_AXE), 2, "2 iron axes in the output chest"));
	}

	/** Two worn iron pickaxes in a village chest become one, back in the village chest. */
	private static void weaponsmithGrindsWornToolsTogether(GameTestHelper helper) {
		ItemStack worn = new ItemStack(Items.IRON_PICKAXE);
		worn.setDamageValue(worn.getMaxDamage() - 20);
		TestWorld.villageChest(helper, VILLAGE_CHEST, worn.copy(), worn.copy());
		Villager worker = weaponsmith(helper, null);
		helper.succeedWhen(() -> {
			int pickaxes = TestWorld.count(helper, VILLAGE_CHEST, stack -> stack.is(Items.IRON_PICKAXE));
			helper.assertTrue(pickaxes == 1, "Pickaxes in the village chest: " + pickaxes + ", " + TestWorld.describe(worker));
			// 20 + 20 left, plus 5%
			helper.assertTrue(TestWorld.count(helper, VILLAGE_CHEST,
				stack -> stack.is(Items.IRON_PICKAXE) && stack.getMaxDamage() - stack.getDamageValue() >= 40 && !stack.has(DataComponents.CUSTOM_DATA)) == 1,
				"The pickaxe is not ground together");
		});
	}

	/** An enchanted sword and glass bottles: the sword without enchantments and bottles o' enchanting. */
	private static void weaponsmithDisenchants(GameTestHelper helper) {
		ItemStack sword = new ItemStack(Items.IRON_SWORD);
		sword.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 3);
		Villager worker = weaponsmith(helper, null, sword, new ItemStack(Items.GLASS_BOTTLE, 4));
		// A journeyman disenchants
		TestWorld.setLevel(worker, WorkerLevels.JOURNEYMAN);
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.IRON_SWORD) && !stack.isEnchanted(), 1, "the sword without enchantments");
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.EXPERIENCE_BOTTLE), 1, "a bottle o' enchanting");
		});
	}

	/** A worker from 0.3.0, before there were wage boxes, with its wage in its pockets, puts it into the box. */
	private static void workerStoresCarriedWage(GameTestHelper helper) {
		BlockPos station = STATION.below();
		helper.setBlock(station, Blocks.STONECUTTER);
		Villager worker = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, worker, station);
		worker.getInventory().addItem(new ItemStack(Items.EMERALD, 20));
		((HiredVillager) worker).workingvillagers$setWageStored(false);
		helper.succeedWhen(() -> {
			helper.assertTrue(worker.getInventory().countItem(Items.EMERALD) == 0, "Still carries " + worker.getInventory().countItem(Items.EMERALD));
			// The wage of the day may already be taken out
			int box = TestWorld.station(helper, station).wage();
			helper.assertTrue(box >= 20 - Config.DAILY_WAGE.get() && box <= 20, "Wage box: " + box);
		});
	}

	/** A cauldron (with water, 0 for an empty one) with an input chest holding {@code material}, an output chest, the order (null for none) and the color. */
	private static Villager tanner(GameTestHelper helper, int water, @Nullable Item order, @Nullable DyeColor dye, ItemStack... material) {
		BlockPos cauldron = STATION.below();
		helper.setBlock(cauldron, water == 0 ? Blocks.CAULDRON.defaultBlockState()
			: Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, water));
		TestWorld.chest(helper, INPUT, material);
		TestWorld.chest(helper, OUTPUT);
		TestWorld.link(helper, cauldron, INPUT, ChestRole.INPUT);
		TestWorld.link(helper, cauldron, OUTPUT, ChestRole.OUTPUT);
		TestWorld.station(helper, cauldron).setOrder(order);
		TestWorld.station(helper, cauldron).setDye(dye);
		Villager worker = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, worker, cauldron);
		TestWorld.give(helper, worker, new ItemStack(Items.EMERALD, 2));
		return worker;
	}

	/** Paper and leather: books. */
	private static void tannerMakesBooks(GameTestHelper helper) {
		Villager worker = tanner(helper, 0, Items.BOOK, null, new ItemStack(Items.PAPER, 6), new ItemStack(Items.LEATHER, 2));
		helper.succeedWhen(() -> TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.BOOK), 2, "2 books in the output chest"));
	}

	/** 8 rabbit hides: 2 leather. */
	private static void tannerMakesLeatherFromHides(GameTestHelper helper) {
		Villager worker = tanner(helper, 0, Items.LEATHER, null, new ItemStack(Items.RABBIT_HIDE, 8));
		helper.succeedWhen(() -> TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.LEATHER), 2, "2 leather in the output chest"));
	}

	/** 8 rotten flesh tan in the water of the cauldron (one level): 2 leather. */
	private static void tannerTansRottenFlesh(GameTestHelper helper) {
		Villager worker = tanner(helper, 3, Items.LEATHER, null, new ItemStack(Items.ROTTEN_FLESH, 8));
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.LEATHER), 2, "2 leather in the output chest");
			helper.assertBlockProperty(STATION.below(), LayeredCauldronBlock.LEVEL, 2);
		});
	}

	/** An empty cauldron, a bucket and a puddle: the tanner fetches water, then tans. */
	private static void tannerFillsCauldron(GameTestHelper helper) {
		puddle(helper);
		Villager worker = tanner(helper, 0, Items.LEATHER, null, new ItemStack(Items.ROTTEN_FLESH, 4));
		TestWorld.give(helper, worker, new ItemStack(Items.BUCKET));
		helper.succeedWhen(() -> TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.LEATHER), 1, "1 leather in the output chest"));
	}

	/** A leather chestplate in the color of the station, with its dye. */
	private static void tannerDyesArmor(GameTestHelper helper) {
		Villager worker = tanner(helper, 0, Items.LEATHER_CHESTPLATE, DyeColor.RED, new ItemStack(Items.LEATHER, 8), new ItemStack(Items.DYE.red()));
		// A apprentice makes leather armor
		TestWorld.setLevel(worker, WorkerLevels.APPRENTICE);
		helper.succeedWhen(() -> TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.LEATHER_CHESTPLATE) && TanLeather.hasColor(stack, DyeColor.RED), 1,
			"a red leather chestplate in the output chest"));
	}

	/** Blue boots in the input chest: washed out in the cauldron and dyed red, without an order. */
	private static void tannerRecolorsArmor(GameTestHelper helper) {
		ItemStack blue = DyedItemColor.applyDyes(new ItemStack(Items.LEATHER_BOOTS), List.of(DyeColor.BLUE));
		Villager worker = tanner(helper, 3, null, DyeColor.RED, blue, new ItemStack(Items.DYE.red()));
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.LEATHER_BOOTS) && TanLeather.hasColor(stack, DyeColor.RED), 1, "red boots in the output chest");
			helper.assertBlockProperty(STATION.below(), LayeredCauldronBlock.LEVEL, 2);
		});
	}

	/** Given a red leather chestplate, the guard puts it on, and it drops whole. */
	private static void guardWearsUniform(GameTestHelper helper) {
		BlockPos target = STATION.below();
		helper.setBlock(target, Blocks.TARGET);
		Villager guard = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, guard, target);
		ItemStack red = DyedItemColor.applyDyes(new ItemStack(Items.LEATHER_CHESTPLATE), List.of(DyeColor.RED));
		TestWorld.give(helper, guard, red);
		helper.succeedWhen(() -> {
			ItemStack worn = guard.getItemBySlot(EquipmentSlot.CHEST);
			helper.assertTrue(worn.is(Items.LEATHER_CHESTPLATE) && TanLeather.hasColor(worn, DyeColor.RED), "Not wearing the chestplate: " + worn);
			helper.assertTrue(guard.getInventory().countItem(Items.LEATHER_CHESTPLATE) == 0, "The chestplate is still in the inventory");
		});
	}

	/** Boots in a village chest: the guard takes and wears them. */
	private static void guardTakesArmorFromVillageChest(GameTestHelper helper) {
		BlockPos target = STATION.below();
		helper.setBlock(target, Blocks.TARGET);
		TestWorld.villageChest(helper, VILLAGE_CHEST, new ItemStack(Items.LEATHER_BOOTS));
		Villager guard = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, guard, target);
		TestWorld.give(helper, guard, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.BOW), new ItemStack(Items.ARROW, 64));
		GuardPost.setTimeForTests(guard, 14000);
		helper.succeedWhen(() -> helper.assertTrue(guard.getItemBySlot(EquipmentSlot.FEET).is(Items.LEATHER_BOOTS),
			"Not wearing boots: " + TestWorld.describe(guard)));
	}

	/** A brewing stand with an input chest holding {@code material}, an output chest and the order (empty for none). */
	private static Villager apothecary(GameTestHelper helper, ItemStack order, ItemStack... material) {
		BlockPos stand = STATION.below();
		helper.setBlock(stand, Blocks.BREWING_STAND);
		TestWorld.chest(helper, INPUT, material);
		TestWorld.chest(helper, OUTPUT);
		TestWorld.link(helper, stand, INPUT, ChestRole.INPUT);
		TestWorld.link(helper, stand, OUTPUT, ChestRole.OUTPUT);
		if (!order.isEmpty()) {
			TestWorld.station(helper, stand).setOrder(order.getItem(),
				BrewingPaths.potionOf(order).flatMap(Holder::unwrapKey).map(key -> key.identifier().toString()).orElse(null));
		}
		Villager worker = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, worker, stand);
		TestWorld.give(helper, worker, new ItemStack(Items.EMERALD, 2));
		return worker;
	}

	/** Water in the floor next to the brewing stand: two source blocks. */
	private static void puddle(GameTestHelper helper) {
		helper.setBlock(new BlockPos(8, -1, 11), Blocks.WATER);
		helper.setBlock(new BlockPos(9, -1, 11), Blocks.WATER);
	}

	/** 6 glass: 6 glass bottles. */
	private static void apothecaryMakesBottles(GameTestHelper helper) {
		Villager worker = apothecary(helper, new ItemStack(Items.GLASS_BOTTLE), new ItemStack(Items.GLASS, 6));
		helper.succeedWhen(() -> TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.GLASS_BOTTLE), 6, "6 glass bottles in the output chest"));
	}

	/** Glass bottles and water nearby: water bottles. */
	private static void apothecaryFillsWater(GameTestHelper helper) {
		puddle(helper);
		Villager worker = apothecary(helper, BrewingPaths.water(), new ItemStack(Items.GLASS_BOTTLE, 3));
		helper.succeedWhen(() -> TestWorld.assertHolds(helper, worker, OUTPUT, stack -> BrewingPaths.same(stack, BrewingPaths.water()), 3,
			"3 water bottles in the output chest"));
	}

	/** Glass bottles, water, nether wart, a glistering melon slice and blaze powder: water, awkward potion, potion of healing. */
	private static void apothecaryBrewsHealing(GameTestHelper helper) {
		puddle(helper);
		ItemStack healing = PotionContents.createItemStack(Items.POTION, Potions.HEALING);
		Villager worker = apothecary(helper, healing, new ItemStack(Items.GLASS_BOTTLE, 3), new ItemStack(Items.NETHER_WART),
			new ItemStack(Items.GLISTERING_MELON_SLICE), new ItemStack(Items.BLAZE_POWDER));
		helper.succeedWhen(() -> TestWorld.assertHolds(helper, worker, OUTPUT, stack -> BrewingPaths.same(stack, healing), 3,
			"3 potions of healing in the output chest"));
	}

	/** An injured worker nearby and a potion of healing in the output chest: the apothecary heals it. */
	private static void apothecaryHealsWorker(GameTestHelper helper) {
		Villager apothecary = apothecary(helper, ItemStack.EMPTY);
		TestWorld.chest(helper, OUTPUT, PotionContents.createItemStack(Items.POTION, Potions.HEALING));
		Villager injured = TestWorld.hireVillager(helper, new BlockPos(14, 0, 14));
		injured.setHealth(8.0F);
		helper.succeedWhen(() -> {
			helper.assertTrue(injured.getHealth() > 8.0F, "Not healed: " + injured.getHealth() + ", " + TestWorld.describe(apothecary));
			helper.assertTrue(TestWorld.count(helper, OUTPUT, BrewingPaths::heals) == 0, "The potion is still in the output chest");
		});
	}

	/** Ripe nether wart on soul sand next to the brewing stand: harvested and planted again. */
	private static void apothecaryHarvestsNetherWart(GameTestHelper helper) {
		BlockPos wart = new BlockPos(6, 0, 10);
		helper.setBlock(wart.below(), Blocks.SOUL_SAND);
		helper.setBlock(wart, Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, NetherWartBlock.MAX_AGE));
		Villager worker = apothecary(helper, ItemStack.EMPTY);
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(Blocks.NETHER_WART, wart);
			helper.assertTrue(helper.getBlockState(wart).getValue(NetherWartBlock.AGE) == 0, "The nether wart was not harvested");
			helper.assertTrue(worker.getInventory().countItem(Items.NETHER_WART) > 0, "No nether wart in the inventory: " + TestWorld.describe(worker));
		});
	}

	/** A lake and a boat: the fisher rows out, fishes, rows back, takes its boat and delivers the catch. */
	private static void fisherRowsOut(GameTestHelper helper) {
		helper.setBlock(STATION.below(), Blocks.BARREL);
		pond(helper, 3, 11, 22, 22);
		TestWorld.chest(helper, OUTPUT);
		TestWorld.link(helper, STATION.below(), OUTPUT, ChestRole.OUTPUT);
		Villager worker = TestWorld.hireVillager(helper, WORKER);
		// A journeyman rows out
		TestWorld.setLevel(worker, WorkerLevels.JOURNEYMAN);
		TestWorld.assign(helper, worker, STATION.below());
		TestWorld.give(helper, worker, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.FISHING_ROD), new ItemStack(Items.OAK_BOAT));
		boolean[] rowed = {false};
		helper.onEachTick(() -> rowed[0] |= worker.getVehicle() instanceof net.minecraft.world.entity.vehicle.boat.AbstractBoat
			&& worker.getVehicle().position().distanceTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(8, 0, 11))) > 5.0);
		helper.succeedWhen(() -> {
			helper.assertTrue(rowed[0], "The fisher did not row out");
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> !stack.is(Items.FISHING_ROD) && !stack.is(Items.OAK_BOAT), 3, "the catch in the output chest");
			helper.assertTrue(worker.getVehicle() == null, "The fisher is still in the boat");
			helper.assertTrue(worker.getInventory().countItem(Items.OAK_BOAT) == 1, "The fisher did not take its boat back");
		});
	}

	/**
	 * A small pond right next to the barrel and a lake a bit further: the fisher with a boat
	 * walks past the pond and rows out on the lake.
	 */
	private static void fisherRowsOutPastPond(GameTestHelper helper) {
		helper.setBlock(STATION.below(), Blocks.BARREL);
		pond(helper, 4, 9, 6, 10);
		pond(helper, 3, 14, 22, 22);
		TestWorld.chest(helper, OUTPUT);
		TestWorld.link(helper, STATION.below(), OUTPUT, ChestRole.OUTPUT);
		Villager worker = TestWorld.hireVillager(helper, WORKER);
		// A journeyman rows out
		TestWorld.setLevel(worker, WorkerLevels.JOURNEYMAN);
		TestWorld.assign(helper, worker, STATION.below());
		TestWorld.give(helper, worker, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.FISHING_ROD), new ItemStack(Items.OAK_BOAT));
		boolean[] rowed = {false};
		helper.onEachTick(() -> rowed[0] |= worker.getVehicle() instanceof net.minecraft.world.entity.vehicle.boat.AbstractBoat
			&& worker.getVehicle().getZ() > helper.absoluteVec(new net.minecraft.world.phys.Vec3(0, 0, 16)).z);
		helper.succeedWhen(() -> {
			helper.assertTrue(rowed[0], "The fisher did not row out on the lake: " + TestWorld.describe(worker));
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> !stack.is(Items.FISHING_ROD) && !stack.is(Items.OAK_BOAT), 3, "the catch in the output chest");
		});
	}

	/** A pen of fences without a gate (3 × 3 inside, around {@link #PEN}) with animals of one kind, a smoker and an output chest. */
	private static List<net.minecraft.world.entity.animal.Animal> animalPen(GameTestHelper helper, net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.animal.Animal> kind,
		int count, int herd) {
		BlockPos smoker = STATION.below();
		helper.setBlock(smoker, Blocks.SMOKER);
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				if (Math.abs(x) == 2 || Math.abs(z) == 2) {
					helper.setBlock(PEN.offset(x, 0, z), Blocks.OAK_FENCE);
				}
			}
		}
		List<net.minecraft.world.entity.animal.Animal> animals = new java.util.ArrayList<>();
		for (int i = 0; i < count; i++) {
			animals.add(helper.spawn(kind, PEN.offset(i % 3 - 1, 0, i / 3 - 1)));
		}
		TestWorld.chest(helper, OUTPUT);
		TestWorld.link(helper, smoker, OUTPUT, ChestRole.OUTPUT);
		TestWorld.station(helper, smoker).setHerd(herd);
		return animals;
	}

	/** Four cows, herd size 2: the butcher slaughters two over the fence and brings beef and leather to the chest. */
	private static void butcherSlaughtersSurplus(GameTestHelper helper) {
		animalPen(helper, EntityTypes.COW, 4, 2);
		Villager worker = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, worker, STATION.below());
		TestWorld.give(helper, worker, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.IRON_SWORD));
		helper.succeedWhen(() -> {
			int cows = TendAnimals.herd(helper.getLevel(), helper.absolutePos(STATION.below())).size();
			helper.assertTrue(cows == 2, "Cows left: " + cows);
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.BEEF), 2, "raw beef in the output chest");
			helper.assertTrue(TestWorld.count(helper, OUTPUT, stack -> stack.is(Items.IRON_SWORD)) == 0, "The butcher delivered its sword");
		});
	}

	/** Two pigs, fewer than the herd size: the butcher feeds them carrots and they get a piglet. */
	private static void butcherBreedsPigs(GameTestHelper helper) {
		animalPen(helper, EntityTypes.PIG, 2, Workstations.DEFAULT_HERD);
		Villager worker = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, worker, STATION.below());
		TestWorld.give(helper, worker, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.IRON_SWORD), new ItemStack(Items.CARROT, 4));
		helper.succeedWhen(() -> {
			List<net.minecraft.world.entity.animal.Animal> herd = TendAnimals.herd(helper.getLevel(), helper.absolutePos(STATION.below()));
			helper.assertTrue(herd.stream().anyMatch(net.minecraft.world.entity.animal.Animal::isBaby), "No piglet yet, " + herd.size() + " pigs");
			helper.assertTrue(herd.size() >= 3, "A pig was slaughtered");
		});
	}

	/**
	 * A butcher of chickens carrying wheat, which chickens do not eat, and seeds in its input
	 * chest: it fetches the seeds (16, as of every kind of feed) and the chickens get a chick.
	 */
	private static void butcherFetchesFeedOfItsAnimals(GameTestHelper helper) {
		animalPen(helper, EntityTypes.CHICKEN, 2, Workstations.DEFAULT_HERD);
		TestWorld.chest(helper, INPUT, new ItemStack(Items.WHEAT, 32), new ItemStack(Items.WHEAT_SEEDS, 32));
		TestWorld.link(helper, STATION.below(), INPUT, ChestRole.INPUT);
		Villager worker = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, worker, STATION.below());
		TestWorld.give(helper, worker, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.IRON_SWORD), new ItemStack(Items.WHEAT, 16));
		helper.succeedWhen(() -> {
			List<net.minecraft.world.entity.animal.Animal> herd = TendAnimals.herd(helper.getLevel(), helper.absolutePos(STATION.below()));
			helper.assertTrue(herd.stream().anyMatch(net.minecraft.world.entity.animal.Animal::isBaby), "No chick yet: " + TestWorld.describe(worker));
			helper.assertTrue(TestWorld.count(helper, INPUT, stack -> stack.is(Items.WHEAT)) == 32, "The butcher took wheat its chickens do not eat");
		});
	}

	/** Three chickens, herd size 2, and coal: the butcher slaughters one and smokes the chicken in the smoker. */
	private static void butcherSmokesMeat(GameTestHelper helper) {
		animalPen(helper, EntityTypes.CHICKEN, 3, 2);
		Villager worker = TestWorld.hireVillager(helper, WORKER);
		// A journeyman smokes meat
		TestWorld.setLevel(worker, WorkerLevels.JOURNEYMAN);
		TestWorld.assign(helper, worker, STATION.below());
		TestWorld.give(helper, worker, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.IRON_SWORD), new ItemStack(Items.COAL, 2));
		helper.succeedWhen(() -> TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.COOKED_CHICKEN), 1, "cooked chicken in the output chest"));
	}

	/**
	 * A hired villager without a job walks to a free stonecutter

	/** A station put where another kind of station was must not take over its settings. */
	private static void replacedStationStartsFresh(GameTestHelper helper) {
		helper.setBlock(STATION, WorkingVillagers.WOODCUTTER_BLOCK.get());
		TestWorld.station(helper, STATION).setOrder(Items.STICK);
		TestWorld.chest(helper, INPUT);
		TestWorld.chest(helper, OUTPUT);
		TestWorld.link(helper, STATION, INPUT, ChestRole.INPUT);
		TestWorld.link(helper, STATION, OUTPUT, ChestRole.OUTPUT);
		helper.setBlock(STATION, WorkingVillagers.CARPENTER_BENCH.get());
		Workstations.Station bench = TestWorld.station(helper, STATION);
		helper.assertTrue(bench.order() == null, "The carpenter's bench took over the order of the woodcutter");
		helper.assertTrue(bench.chestCount() == 0, "The carpenter's bench took over the chests of the woodcutter");
		helper.succeed();
	}

	/**
	 * A double chest is one chest: linking one half unlinks the other, and two linked halves
	 * (from before, or two linked chests placed side by side) count once.
	 */
	private static void doubleChestIsLinkedOnce(GameTestHelper helper) {
		helper.setBlock(STATION, WorkingVillagers.CARPENTER_BENCH.get());
		BlockPos other = TestWorld.doubleChest(helper, INPUT);
		Workstations.Station bench = TestWorld.station(helper, STATION);
		TestWorld.link(helper, STATION, INPUT, ChestRole.INPUT);
		TestWorld.link(helper, STATION, other, ChestRole.INPUT);
		helper.assertTrue(bench.validInputs(helper.getLevel()).size() == 1, "Both halves of the double chest count as input");
		bench.setRole(helper.getLevel(), helper.absolutePos(other), ChestRole.OUTPUT);
		helper.assertTrue(bench.validInputs(helper.getLevel()).isEmpty(), "The other half is still an input");
		helper.assertTrue(bench.validOutputs(helper.getLevel()).size() == 1, "The double chest is no output");
		helper.assertTrue(bench.linkedHalf(helper.getLevel(), helper.absolutePos(INPUT)) != null, "Clicking the other half does not find the link");
		helper.succeed();
	}

	/** Sets up a chopping block with an output chest and a paid lumberjack with an axe. */
	private static Villager lumberjack(GameTestHelper helper, ItemStack... supplies) {
		helper.setBlock(STATION, WorkingVillagers.CHOPPING_BLOCK.get());
		TestWorld.chest(helper, OUTPUT);
		TestWorld.link(helper, STATION, OUTPUT, ChestRole.OUTPUT);
		Villager villager = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, villager, STATION);
		TestWorld.give(helper, villager, supplies);
		return villager;
	}

	/** A grown tree: growing turns the grass below the trunk into dirt. */
	private static void lumberjackFellsTree(GameTestHelper helper) {
		lumberjackFellsTreeOn(helper, Blocks.DIRT);
	}

	/** Trees on other ground (mangroves on mud, trees placed on grass or moss) count too. */
	private static void lumberjackFellsTreeOnMud(GameTestHelper helper) {
		lumberjackFellsTreeOn(helper, Blocks.MUD);
	}

	private static void lumberjackFellsTreeOnGrass(GameTestHelper helper) {
		lumberjackFellsTreeOn(helper, Blocks.GRASS_BLOCK);
	}

	private static void lumberjackFellsTreeOn(GameTestHelper helper, Block ground) {
		BlockPos tree = new BlockPos(15, 1, 15);
		TestWorld.oakTree(helper, tree, 5, true, ground);
		Villager worker = lumberjack(helper, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.IRON_AXE), new ItemStack(Items.OAK_SAPLING));
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.OAK_LOG), 5, "oak logs in the output chest");
			helper.assertTrue(TestWorld.station(helper, STATION).wage() == 1 && WorkerSupplies.count(worker.getInventory(), WorkerSupplies::isWage) == 0,
				"The wage was not taken from the wage box: " + TestWorld.station(helper, STATION).wage());
			helper.assertTrue(TestWorld.count(helper, OUTPUT, WorkerSupplies::isWage) == 0, "The lumberjack delivered its emeralds");
			helper.assertBlockPresent(Blocks.OAK_SAPLING, tree);
		});
	}

	/**
	 * Hired late in the afternoon, the lumberjack works the rest of the day without a wage.
	 * The hiring is played at 7500 (the test runs in the morning: changing the clock would
	 * change it for the other tests running at the same time).
	 */
	private static void lumberjackHiredLateWorksForFree(GameTestHelper helper) {
		BlockPos tree = new BlockPos(15, 1, 15);
		TestWorld.oakTree(helper, tree, 5, true, Blocks.DIRT);
		Villager worker = lumberjack(helper, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.IRON_AXE));
		((HiredVillager) worker).workingvillagers$setHiredDay(-1);
		WorkerSupplies.onHired(worker, ChopTrees.today(helper.getLevel()), 7500);
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.OAK_LOG), 5, "oak logs in the output chest");
			int left = TestWorld.station(helper, STATION).wage() + WorkerSupplies.count(worker.getInventory(), WorkerSupplies::isWage);
			helper.assertTrue(left == 2, "The lumberjack took a wage late in the day: " + left + " of 2 emeralds left");
		});
	}

	/**
	 * Hired before, the lumberjack takes its wage also when it is hired again late in the
	 * afternoon (like a carrier that got its basket back): no free work.
	 */
	private static void lumberjackHiredBeforePaysLate(GameTestHelper helper) {
		BlockPos tree = new BlockPos(15, 1, 15);
		TestWorld.oakTree(helper, tree, 5, true, Blocks.DIRT);
		Villager worker = lumberjack(helper, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.IRON_AXE));
		long today = ChopTrees.today(helper.getLevel());
		WorkerSupplies.onHired(worker, today, 7500);
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.OAK_LOG), 5, "oak logs in the output chest");
			int left = TestWorld.station(helper, STATION).wage() + WorkerSupplies.count(worker.getInventory(), WorkerSupplies::isWage);
			helper.assertTrue(left == 1, "The lumberjack hired before did not take its wage: " + left + " of 2 emeralds left (paid day "
				+ ((HiredVillager) worker).workingvillagers$getPaidDay() + ", hired day " + ((HiredVillager) worker).workingvillagers$getHiredDay() + ", today " + today + ")");
		});
	}

	/**
	 * A tree on a dirt path, which is not a full block: the lumberjack still finds a spot
	 * right next to the trunk instead of chopping from a few blocks away.
	 */
	private static void lumberjackStandsAtTrunkOnPath(GameTestHelper helper) {
		BlockPos tree = new BlockPos(15, 1, 15);
		for (int x = 12; x <= 18; x++) {
			for (int z = 12; z <= 18; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.DIRT_PATH);
			}
		}
		TestWorld.oakTree(helper, tree, 5, true, Blocks.DIRT);
		Villager worker = lumberjack(helper, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.IRON_AXE));
		BlockPos top = tree.above(4);
		double[] distance = {-1};
		helper.onEachTick(() -> {
			if (distance[0] < 0 && !helper.getBlockState(top).is(Blocks.OAK_LOG)) {
				// the tree comes down from the top: where the lumberjack stood while chopping
				distance[0] = Math.sqrt(helper.absolutePos(tree).distToCenterSqr(worker.getX(), helper.absolutePos(tree).getY() + 0.5, worker.getZ()));
			}
		});
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.OAK_LOG), 5, "oak logs in the output chest");
			helper.assertTrue(distance[0] >= 0 && distance[0] < 1.6, "The lumberjack chopped from " + distance[0] + " blocks away");
		});
	}

	private static void lumberjackSparesBuilds(GameTestHelper helper) {
		// Persistent leaves: placed by a player, so this is a build, not a tree
		BlockPos build = new BlockPos(15, 1, 15);
		TestWorld.oakTree(helper, build, 4, false, Blocks.DIRT);
		lumberjack(helper, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.IRON_AXE));
		helper.failIfEver(() -> helper.assertBlockPresent(Blocks.OAK_LOG, build));
		helper.runAfterDelay(600, () -> {
			helper.assertTrue(TestWorld.count(helper, OUTPUT, stack -> !stack.isEmpty()) == 0, "The lumberjack delivered something");
			helper.succeed();
		});
	}

	/**
	 * The leaves of a felled tree come down with it, with their drops for the lumberjack,
	 * instead of decaying where nobody picks up the saplings.
	 */
	private static void lumberjackClearsLeaves(GameTestHelper helper) {
		BlockPos tree = new BlockPos(15, 1, 15);
		TestWorld.oakTree(helper, tree, 5, true, Blocks.DIRT);
		Villager worker = lumberjack(helper, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.IRON_AXE));
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.OAK_LOG), 5, "oak logs in the output chest");
			BlockPos top = tree.above(4);
			for (BlockPos pos : BlockPos.betweenClosed(top.offset(-1, -1, -1), top.offset(1, 1, 1))) {
				helper.assertBlockNotPresent(Blocks.OAK_LEAVES, pos);
			}
		});
	}

	/**
	 * Without a sapling of its kind (the leaves are birch leaves, so the tree drops none), the
	 * lumberjack remembers where the tree stood and plants there once it has one, then forgets
	 * the place.
	 */
	private static void lumberjackReplantsLater(GameTestHelper helper) {
		BlockPos tree = new BlockPos(15, 1, 15);
		TestWorld.oakTree(helper, tree, 5, true, Blocks.DIRT);
		BlockPos top = tree.above(4);
		for (BlockPos pos : BlockPos.betweenClosed(top.offset(-1, -1, -1), top.offset(1, 1, 1))) {
			if (helper.getBlockState(pos).is(Blocks.OAK_LEAVES)) {
				helper.setBlock(pos, Blocks.BIRCH_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1));
			}
		}
		Villager worker = lumberjack(helper, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.IRON_AXE));
		List<ChopTrees.Planting> plantings = ((HiredVillager) worker).workingvillagers$getPlantings();
		boolean[] given = {false};
		helper.onEachTick(() -> {
			if (!given[0] && !plantings.isEmpty()) {
				helper.assertTrue(plantings.getFirst().spots().equals(List.of(helper.absolutePos(tree))), "Remembered the wrong place: " + plantings);
				TestWorld.give(helper, worker, new ItemStack(Items.OAK_SAPLING));
				given[0] = true;
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(given[0], "The lumberjack did not remember where to plant");
			helper.assertBlockPresent(Blocks.OAK_SAPLING, tree);
			helper.assertTrue(plantings.isEmpty(), "The lumberjack did not forget the place it planted: " + plantings);
		});
	}

	/**
	 * A lumberjack that was given nothing takes its wage for a few days, the more worn of two
	 * axes and saplings from a village chest, and fells the tree.
	 */
	private static void villageChestSuppliesLumberjack(GameTestHelper helper) {
		BlockPos tree = new BlockPos(15, 1, 15);
		TestWorld.oakTree(helper, tree, 5, true, Blocks.DIRT);
		ItemStack worn = new ItemStack(Items.IRON_AXE);
		worn.setDamageValue(worn.getMaxDamage() / 2);
		TestWorld.villageChest(helper, VILLAGE_CHEST, new ItemStack(Items.EMERALD, 10), new ItemStack(Items.IRON_AXE), worn,
			new ItemStack(Items.OAK_SAPLING, 32));
		Villager worker = lumberjack(helper);
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.OAK_LOG), 5, "oak logs in the output chest");
			int wageDays = Config.VILLAGE_CHEST_WAGE_DAYS.get() * Config.DAILY_WAGE.get();
			helper.assertTrue(TestWorld.count(helper, VILLAGE_CHEST, WorkerSupplies::isWage) == 10 - wageDays,
				"The lumberjack did not take its wage for " + Config.VILLAGE_CHEST_WAGE_DAYS.get() + " days: " + TestWorld.describe(worker));
			helper.assertTrue(TestWorld.count(helper, VILLAGE_CHEST, stack -> stack.is(Items.IRON_AXE) && !stack.isDamaged()) == 1,
				"The lumberjack took the new axe instead of the worn one");
			helper.assertTrue(TestWorld.count(helper, VILLAGE_CHEST, stack -> stack.is(Items.OAK_SAPLING)) == 32 - Config.KEEP_SAPLINGS.get(),
				"The lumberjack did not take its saplings");
			TestWorld.removeVillageChest(helper, VILLAGE_CHEST);
		});
	}

	/** Renamed (and enchanted) tools in a village chest stay there for the player. */
	private static void villageChestKeepsNamedTools(GameTestHelper helper) {
		BlockPos tree = new BlockPos(15, 1, 15);
		TestWorld.oakTree(helper, tree, 4, true, Blocks.DIRT);
		ItemStack named = new ItemStack(Items.DIAMOND_AXE);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Lucky"));
		TestWorld.villageChest(helper, VILLAGE_CHEST, new ItemStack(Items.EMERALD, 10), named);
		Villager worker = lumberjack(helper);
		helper.failIfEver(() -> helper.assertBlockPresent(Blocks.OAK_LOG, tree));
		helper.runAfterDelay(600, () -> {
			helper.assertTrue(TestWorld.count(helper, VILLAGE_CHEST, stack -> stack.is(Items.DIAMOND_AXE)) == 1,
				"The lumberjack took the named axe");
			helper.assertTrue(WorkerSupplies.emeralds(helper.getLevel(), worker) > 0, "The lumberjack took no wage: " + TestWorld.describe(worker));
			helper.assertTrue(((HiredVillager) worker).workingvillagers$hasProblem(), "The lumberjack without axe does not complain: " + TestWorld.describe(worker));
			TestWorld.removeVillageChest(helper, VILLAGE_CHEST);
			helper.succeed();
		});
	}

	/** An iron axe with 20 of its 250 uses left: worn, it is given back instead of used. */
	private static ItemStack wornAxe() {
		ItemStack axe = new ItemStack(Items.IRON_AXE);
		axe.setDamageValue(axe.getMaxDamage() - 20);
		return axe;
	}

	/** Into its inventory: a worker does not accept a worn tool from a player. */
	private static void carryWornAxe(Villager worker) {
		worker.getInventory().addItem(wornAxe());
	}

	private static boolean isWornAxe(ItemStack stack) {
		return stack.is(Items.IRON_AXE) && WorkerSupplies.isWorn(stack);
	}

	/**
	 * A lumberjack with a worn axe and a spare fells the tree with the spare and puts the worn
	 * axe into the output chest: no smith repairs from the village chest, so the player sees it.
	 */
	private static void lumberjackGivesBackWornAxe(GameTestHelper helper) {
		BlockPos tree = new BlockPos(15, 1, 15);
		TestWorld.oakTree(helper, tree, 5, true, Blocks.DIRT);
		TestWorld.villageChest(helper, VILLAGE_CHEST);
		Villager worker = lumberjack(helper, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.IRON_AXE));
		carryWornAxe(worker);
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.OAK_LOG), 5, "oak logs in the output chest");
			helper.assertTrue(TestWorld.count(helper, OUTPUT, WorkerTests::isWornAxe) == 1,
				"The worn axe is not in the output chest: " + TestWorld.describe(worker));
			helper.assertTrue(WorkerSupplies.count(worker.getInventory(), stack -> stack.is(Items.IRON_AXE)) == 1, "The lumberjack lost its spare axe");
			TestWorld.removeVillageChest(helper, VILLAGE_CHEST);
		});
	}

	/**
	 * A lumberjack whose only axe is worn puts it into the output chest (no smith repairs from
	 * the village chest), takes a new one from the village chest and fells the tree.
	 */
	private static void lumberjackSwapsWornAxe(GameTestHelper helper) {
		BlockPos tree = new BlockPos(15, 1, 15);
		TestWorld.oakTree(helper, tree, 5, true, Blocks.DIRT);
		TestWorld.villageChest(helper, VILLAGE_CHEST, new ItemStack(Items.IRON_AXE));
		Villager worker = lumberjack(helper, new ItemStack(Items.EMERALD, 2));
		carryWornAxe(worker);
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.OAK_LOG), 5, "oak logs in the output chest");
			helper.assertTrue(TestWorld.count(helper, OUTPUT, WorkerTests::isWornAxe) == 1,
				"The worn axe is not in the output chest: " + TestWorld.describe(worker));
			helper.assertTrue(TestWorld.count(helper, VILLAGE_CHEST, stack -> stack.is(Items.IRON_AXE)) == 0, "The lumberjack did not take the new axe");
			TestWorld.removeVillageChest(helper, VILLAGE_CHEST);
		});
	}

	/**
	 * Without a village chest the worn axe goes into the output chest. The lumberjack does not
	 * use it up: without another axe it waits and leaves the tree standing.
	 */
	private static void wornAxeGoesToOutputChest(GameTestHelper helper) {
		BlockPos tree = new BlockPos(15, 1, 15);
		TestWorld.oakTree(helper, tree, 4, true, Blocks.DIRT);
		Villager worker = lumberjack(helper, new ItemStack(Items.EMERALD, 2));
		carryWornAxe(worker);
		helper.failIfEver(() -> helper.assertBlockPresent(Blocks.OAK_LOG, tree));
		helper.succeedWhen(() -> {
			helper.assertTrue(TestWorld.count(helper, OUTPUT, WorkerTests::isWornAxe) == 1,
				"The worn axe is not in the output chest: " + TestWorld.describe(worker));
			helper.assertTrue(((HiredVillager) worker).workingvillagers$hasProblem(), "The lumberjack without axe does not complain: " + TestWorld.describe(worker));
		});
	}

	/** A stone axe is cheap: the lumberjack uses it up instead of giving it back. */
	private static void stoneAxeIsUsedUp(GameTestHelper helper) {
		BlockPos tree = new BlockPos(15, 1, 15);
		TestWorld.oakTree(helper, tree, 4, true, Blocks.DIRT);
		ItemStack axe = new ItemStack(Items.STONE_AXE);
		axe.setDamageValue(axe.getMaxDamage() - 2);
		Villager worker = lumberjack(helper, new ItemStack(Items.EMERALD, 2), axe);
		helper.succeedWhen(() -> {
			helper.assertTrue(WorkerSupplies.count(worker.getInventory(), stack -> stack.is(Items.STONE_AXE)) == 0
				&& TestWorld.count(helper, OUTPUT, stack -> stack.is(Items.STONE_AXE)) == 0, "The stone axe was not used up: " + TestWorld.describe(worker));
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.OAK_LOG), 1, "oak logs in the output chest");
		});
	}

	/**
	 * A worn iron axe in the village chest (20 of 250 uses left): the smith at the smithing
	 * table repairs it with 4 iron ingots, a quarter each, and puts it back.
	 */
	private static void smithRepairsWornAxe(GameTestHelper helper) {
		helper.setBlock(STATION, Blocks.SMITHING_TABLE);
		TestWorld.villageChest(helper, VILLAGE_CHEST, wornAxe());
		Villager worker = processingStation(helper, new ItemStack(Items.IRON_INGOT, 6));
		helper.succeedWhen(() -> {
			helper.assertTrue(TestWorld.count(helper, VILLAGE_CHEST, stack -> stack.is(Items.IRON_AXE) && !stack.isDamaged()) == 1,
				"The axe is not repaired in the village chest: " + TestWorld.describe(worker));
			helper.assertTrue(TestWorld.count(helper, INPUT, stack -> stack.is(Items.IRON_INGOT)) == 2, "The smith did not use 4 iron ingots");
			TestWorld.removeVillageChest(helper, VILLAGE_CHEST);
		});
	}

	/**
	 * A damaged, enchanted diamond pickaxe in the input chest: the smith repairs it with
	 * diamonds and puts it back, the enchantment stays. Stone it cannot repair anything with
	 * stays in the chest.
	 */
	private static void smithRepairsInputChest(GameTestHelper helper) {
		helper.setBlock(STATION, Blocks.SMITHING_TABLE);
		ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
		pickaxe.setDamageValue(500);
		pickaxe.enchant(helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
			.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.EFFICIENCY), 3);
		Villager worker = processingStation(helper, pickaxe, new ItemStack(Items.DIAMOND, 3), new ItemStack(Items.STONE, 4), new ItemStack(Items.EXPERIENCE_BOTTLE, 3));
		// A journeyman repairs enchanted diamond
		TestWorld.setLevel(worker, WorkerLevels.JOURNEYMAN);
		helper.succeedWhen(() -> {
			helper.assertTrue(TestWorld.count(helper, INPUT, stack -> stack.is(Items.DIAMOND_PICKAXE) && !stack.isDamaged() && stack.isEnchanted()) == 1,
				"The pickaxe is not repaired in the input chest: " + TestWorld.describe(worker));
			helper.assertTrue(TestWorld.count(helper, INPUT, stack -> stack.is(Items.DIAMOND)) == 1, "The smith did not use 2 diamonds");
			helper.assertTrue(TestWorld.count(helper, INPUT, stack -> stack.is(Items.EXPERIENCE_BOTTLE)) == 1, "The smith did not use 2 bottles o' enchanting");
			helper.assertTrue(TestWorld.count(helper, INPUT, stack -> stack.is(Items.STONE)) == 4, "The smith took the stone");
		});
	}

	/**
	 * Emeralds given to the lumberjack go into the wage box of its chopping block; it takes its
	 * wage from there. When it dies, they stay there; the worker drops none.
	 */
	private static void wageBoxOutlivesWorker(GameTestHelper helper) {
		Villager worker = lumberjack(helper, new ItemStack(Items.EMERALD, 5));
		helper.assertTrue(WorkerSupplies.count(worker.getInventory(), WorkerSupplies::isWage) == 0, "The emeralds went into the worker's pockets");
		helper.assertTrue(TestWorld.station(helper, STATION).wage() == 5, "The emeralds are not in the wage box: " + TestWorld.station(helper, STATION).wage());
		helper.runAfterDelay(40, () -> {
			worker.kill(helper.getLevel());
			helper.runAfterDelay(5, () -> {
				int wage = TestWorld.station(helper, STATION).wage();
				// No tree, no work: the wage of the day is not taken either
				helper.assertTrue(wage == 5, "The wage box lost its emeralds: " + wage);
				helper.assertItemEntityNotPresent(Items.EMERALD);
				helper.succeed();
			});
		});
	}

	/** A broken workstation drops the emeralds of its wage box. */
	private static void brokenStationDropsWage(GameTestHelper helper) {
		helper.setBlock(STATION, WorkingVillagers.CHOPPING_BLOCK.get());
		TestWorld.station(helper, STATION).setWage(10);
		helper.destroyBlock(STATION);
		helper.succeedWhen(() -> {
			int dropped = helper.getEntities(EntityTypes.ITEM, STATION, 2.0).stream()
				.filter(item -> item.getItem().is(Items.EMERALD))
				.mapToInt(item -> item.getItem().getCount())
				.sum();
			helper.assertTrue(dropped == 10, "Expected 10 dropped emeralds, found " + dropped);
		});
	}

	/** Right-clicking a workstation with emeralds puts them into its wage box, up to 64. */
	private static void rightClickFillsWageBox(GameTestHelper helper) {
		helper.setBlock(STATION, Blocks.BLAST_FURNACE);
		net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
		// In survival, so the emeralds leave the hand
		player.setGameMode(GameType.SURVIVAL);
		ItemStack emeralds = new ItemStack(Items.EMERALD, 50);
		helper.assertTrue(Workstations.depositWage(player, helper.absolutePos(STATION), emeralds), "The blast furnace did not take the emeralds");
		ItemStack more = new ItemStack(Items.EMERALD, 30);
		Workstations.depositWage(player, helper.absolutePos(STATION), more);
		helper.assertTrue(TestWorld.station(helper, STATION).wage() == 64 && emeralds.isEmpty() && more.getCount() == 16,
			"The wage box holds " + TestWorld.station(helper, STATION).wage() + ", 16 should be left in the hand: " + more.getCount());
		helper.assertFalse(Workstations.depositWage(player, helper.absolutePos(STATION), new ItemStack(Items.DIAMOND)), "A diamond went into the wage box");
		helper.succeed();
	}

	private static void noWageNoWork(GameTestHelper helper) {
		BlockPos tree = new BlockPos(15, 1, 15);
		TestWorld.oakTree(helper, tree, 4, true, Blocks.DIRT);
		Villager worker = lumberjack(helper, new ItemStack(Items.IRON_AXE));
		helper.failIfEver(() -> helper.assertBlockPresent(Blocks.OAK_LOG, tree));
		helper.runAfterDelay(600, () -> {
			helper.assertTrue(((HiredVillager) worker).workingvillagers$hasProblem(), "The unpaid lumberjack does not complain: " + TestWorld.describe(worker));
			helper.succeed();
		});
	}

	/**
	 * A lumberjack boxed in where it stands cannot reach anything: after a while it is
	 * brought back to its chopping block and fells the tree.
	 */
	private static void stuckWorkerIsBroughtBack(GameTestHelper helper) {
		BlockPos tree = new BlockPos(15, 1, 15);
		TestWorld.oakTree(helper, tree, 4, true, Blocks.DIRT);
		for (Direction side : Direction.Plane.HORIZONTAL) {
			helper.setBlock(WORKER.relative(side), Blocks.STONE);
			helper.setBlock(WORKER.relative(side).above(), Blocks.STONE);
		}
		helper.setBlock(WORKER.above(2), Blocks.STONE);
		Villager worker = lumberjack(helper, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.IRON_AXE), new ItemStack(Items.OAK_SAPLING));
		helper.succeedWhen(() -> {
			helper.assertFalse(worker.blockPosition().equals(helper.absolutePos(WORKER)), "The worker is still boxed in: " + WorkerStatus.describe(worker).getString());
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.OAK_LOG), 4, "oak logs in the output chest");
		});
	}

	/** A processing station with an input and an output chest and a paid worker. */
	/** Chests and barrels with a stock of 2: two of each, from the same planks. */
	private static void carpenterMakesSeveralProducts(GameTestHelper helper) {
		helper.setBlock(STATION, WorkingVillagers.CARPENTER_BENCH.get());
		Workstations.Station bench = TestWorld.station(helper, STATION);
		bench.orderProduct(Items.CHEST);
		bench.orderProduct(Items.BARREL);
		bench.setStock(2);
		Villager worker = processingStation(helper, new ItemStack(Items.OAK_PLANKS, 32), new ItemStack(Items.OAK_SLAB, 8));
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.CHEST), 2, "chests in the output chest");
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.BARREL), 2, "barrels in the output chest");
			helper.assertTrue(TestWorld.count(helper, OUTPUT, stack -> stack.is(Items.CHEST)) == 2, "The carpenter made more chests than the stock");
		});
	}

	/** Chests and ladders, but no sticks: the ladders do not hold up the chests. */
	private static void carpenterSkipsProductWithoutMaterial(GameTestHelper helper) {
		helper.setBlock(STATION, WorkingVillagers.CARPENTER_BENCH.get());
		Workstations.Station bench = TestWorld.station(helper, STATION);
		bench.orderProduct(Items.LADDER);
		bench.orderProduct(Items.CHEST);
		bench.setStock(2);
		Villager worker = processingStation(helper, new ItemStack(Items.OAK_PLANKS, 16));
		helper.succeedWhen(() -> TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.CHEST), 2, "chests in the output chest"));
	}

	/** Planks and stairs from the same logs: the sawyer cuts both. */
	private static void sawyerCutsSeveralShapes(GameTestHelper helper) {
		helper.setBlock(STATION, WorkingVillagers.WOODCUTTER_BLOCK.get());
		Workstations.Station saw = TestWorld.station(helper, STATION);
		saw.setWood("oak", true);
		saw.toggleShape("planks");
		saw.toggleShape("stairs");
		Villager worker = processingStation(helper, new ItemStack(Items.OAK_LOG, 16));
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.OAK_PLANKS), 8, "oak planks in the output chest");
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.OAK_STAIRS), 8, "oak stairs in the output chest");
		});
	}

	/** Glass and stone with a stock of 2: the kiln operator fires both. */
	private static void kilnOperatorFiresSeveralProducts(GameTestHelper helper) {
		helper.setBlock(STATION, WorkingVillagers.KILN.get());
		Workstations.Station kiln = TestWorld.station(helper, STATION);
		kiln.toggleOrder(Items.GLASS);
		kiln.toggleOrder(Items.STONE);
		kiln.setStock(2);
		Villager worker = processingStation(helper, new ItemStack(Items.SAND, 4), new ItemStack(Items.COBBLESTONE, 4), new ItemStack(Items.COAL, 4));
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.GLASS), 2, "glass in the output chest");
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.STONE), 2, "stone in the output chest");
		});
	}

	/** Arrows and bows with a stock of 4: four of each. */
	private static void fletcherMakesSeveralProducts(GameTestHelper helper) {
		Villager worker = fletcher(helper, Items.ARROW, 4, new ItemStack(Items.FLINT, 4), new ItemStack(Items.STICK, 16), new ItemStack(Items.FEATHER, 4),
			new ItemStack(Items.STRING, 12));
		TestWorld.station(helper, STATION.below()).toggleOrder(Items.BOW);
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.ARROW), 4, "arrows in the output chest");
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.BOW), 4, "bows in the output chest");
		});
	}

	/** An iron axe and an iron pickaxe with a stock of 1, from 6 ingots: one of each. */
	private static void weaponsmithForgesSeveralProducts(GameTestHelper helper) {
		Villager worker = weaponsmith(helper, Items.IRON_AXE, new ItemStack(Items.IRON_INGOT, 6), new ItemStack(Items.STICK, 4));
		Workstations.Station grindstone = TestWorld.station(helper, STATION.below());
		grindstone.toggleOrder(Items.IRON_PICKAXE);
		grindstone.setStock(1);
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.IRON_AXE), 1, "an iron axe in the output chest");
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.IRON_PICKAXE), 1, "an iron pickaxe in the output chest");
		});
	}

	/** Books and bundles with a stock of 1: one of each. */
	private static void tannerMakesSeveralProducts(GameTestHelper helper) {
		Villager worker = tanner(helper, 0, Items.BOOK, null, new ItemStack(Items.PAPER, 6), new ItemStack(Items.LEATHER, 4), new ItemStack(Items.STRING, 2));
		// A journeyman makes bundles
		TestWorld.setLevel(worker, WorkerLevels.JOURNEYMAN);
		Workstations.Station cauldron = TestWorld.station(helper, STATION.below());
		cauldron.toggleOrder(Items.BUNDLE);
		cauldron.setStock(1);
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.BOOK), 1, "a book in the output chest");
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.BUNDLE), 1, "a bundle in the output chest");
		});
	}

	private static Villager processingStation(GameTestHelper helper, ItemStack... input) {
		TestWorld.chest(helper, INPUT, input);
		TestWorld.chest(helper, OUTPUT);
		TestWorld.link(helper, STATION, INPUT, ChestRole.INPUT);
		TestWorld.link(helper, STATION, OUTPUT, ChestRole.OUTPUT);
		Villager villager = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, villager, STATION);
		TestWorld.give(helper, villager, new ItemStack(Items.EMERALD, 2));
		return villager;
	}

	private static void sawyerCutsPlanks(GameTestHelper helper) {
		helper.setBlock(STATION, WorkingVillagers.WOODCUTTER_BLOCK.get());
		TestWorld.station(helper, STATION).setWood("oak", true);
		TestWorld.station(helper, STATION).setShape("planks");
		Villager worker = processingStation(helper, new ItemStack(Items.OAK_LOG, 4), new ItemStack(Items.BIRCH_LOG, 4));
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.OAK_PLANKS), 16, "oak planks in the output chest");
			// Birch is not an allowed wood type: it stays in the input chest
			TestWorld.assertHolds(helper, worker, INPUT, stack -> stack.is(Items.BIRCH_LOG), 4, "birch logs left in the input chest");
		});
	}

	private static void stoneSawyerCutsBricks(GameTestHelper helper) {
		helper.setBlock(STATION, Blocks.STONECUTTER);
		TestWorld.station(helper, STATION).setOrder(Items.STONE_BRICKS);
		Villager worker = processingStation(helper, new ItemStack(Items.STONE, 8));
		helper.succeedWhen(() -> TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.STONE_BRICKS), 8, "stone bricks in the output chest"));
	}

	private static void carpenterMakesChests(GameTestHelper helper) {
		helper.setBlock(STATION, WorkingVillagers.CARPENTER_BENCH.get());
		TestWorld.station(helper, STATION).orderProduct(Items.CHEST);
		TestWorld.station(helper, STATION).setStock(2);
		Villager worker = processingStation(helper, new ItemStack(Items.OAK_PLANKS, 32));
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.CHEST), 2, "chests in the output chest");
			// The stock limit: it stops at two
			helper.assertTrue(TestWorld.count(helper, OUTPUT, stack -> stack.is(Items.CHEST)) == 2, "The carpenter made more than the stock");
		});
	}

	/**
	 * Three chests ordered once: the carpenter makes three, hands them in and is done; the
	 * order is gone and the rest of the planks stays in the input chest.
	 */
	private static void carpenterFillsOrder(GameTestHelper helper) {
		helper.setBlock(STATION, WorkingVillagers.CARPENTER_BENCH.get());
		Workstations.Station bench = TestWorld.station(helper, STATION);
		bench.orderProduct(Items.CHEST);
		bench.setBatch(Items.CHEST, 3);
		Villager worker = processingStation(helper, new ItemStack(Items.OAK_PLANKS, 40));
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.CHEST), 3, "chests in the output chest");
			helper.assertTrue(TestWorld.count(helper, OUTPUT, stack -> stack.is(Items.CHEST)) == 3, "The carpenter made more than ordered");
			helper.assertTrue(!bench.hasOrder(), "The order is not done: " + TestWorld.describe(worker));
			helper.assertTrue(TestWorld.count(helper, INPUT, stack -> stack.is(Items.OAK_PLANKS)) == 16, "The carpenter took more planks than the order needs");
		});
	}

	/** Without an input chest the carpenter takes the planks for its order from a village chest. */
	private static void carpenterTakesMaterialFromVillageChest(GameTestHelper helper) {
		helper.setBlock(STATION, WorkingVillagers.CARPENTER_BENCH.get());
		Workstations.Station bench = TestWorld.station(helper, STATION);
		bench.orderProduct(Items.CHEST);
		bench.setBatch(Items.CHEST, 2);
		TestWorld.villageChest(helper, VILLAGE_CHEST, new ItemStack(Items.OAK_PLANKS, 16));
		TestWorld.chest(helper, OUTPUT);
		TestWorld.link(helper, STATION, OUTPUT, ChestRole.OUTPUT);
		Villager worker = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, worker, STATION);
		TestWorld.give(helper, worker, new ItemStack(Items.EMERALD, 2));
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.CHEST), 2, "chests in the output chest");
			helper.assertTrue(TestWorld.count(helper, VILLAGE_CHEST, stack -> stack.is(Items.OAK_PLANKS)) == 0, "Planks left in the village chest");
			TestWorld.removeVillageChest(helper, VILLAGE_CHEST);
		});
	}

	/**
	 * Four chests ordered, planks for two: the carpenter makes two, then says how many planks
	 * are missing for the other two and shows a "!" above its head.
	 */
	private static void carpenterShowsMissingMaterial(GameTestHelper helper) {
		helper.setBlock(STATION, WorkingVillagers.CARPENTER_BENCH.get());
		Workstations.Station bench = TestWorld.station(helper, STATION);
		bench.orderProduct(Items.CHEST);
		bench.setBatch(Items.CHEST, 4);
		Villager worker = processingStation(helper, new ItemStack(Items.OAK_PLANKS, 16));
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.CHEST), 2, "chests in the output chest");
			Component task = ((HiredVillager) worker).workingvillagers$getTask();
			helper.assertTrue(task != null && task.getContents() instanceof TranslatableContents contents
				&& contents.getKey().equals("status.workingvillagers.carpenter.missing") && contents.getArgs()[0].equals(16),
				"The carpenter does not say that 16 planks are missing: " + TestWorld.describe(worker));
			helper.assertTrue(((HiredVillager) worker).workingvillagers$getNotice() == WorkerNotice.MISSING, "No \"!\" above the carpenter's head");
		});
	}

	/**
	 * Chests ordered, but no planks: the carpenter waits and takes no wage. Once there are
	 * planks it makes a chest and takes the wage with it.
	 */
	private static void carpenterWithoutMaterialCostsNothing(GameTestHelper helper) {
		helper.setBlock(STATION, WorkingVillagers.CARPENTER_BENCH.get());
		Workstations.Station bench = TestWorld.station(helper, STATION);
		bench.orderProduct(Items.CHEST);
		bench.setBatch(Items.CHEST, 1);
		Villager worker = processingStation(helper);
		helper.runAfterDelay(400, () -> {
			helper.assertTrue(!WorkerSupplies.isPaid(helper.getLevel(), worker) && WorkerSupplies.emeralds(helper.getLevel(), worker) == 2,
				"The carpenter took its wage without having made anything: " + WorkerSupplies.emeralds(helper.getLevel(), worker) + " of 2 emeralds left");
			TestWorld.container(helper, INPUT).setItem(0, new ItemStack(Items.OAK_PLANKS, 8));
			helper.succeedWhen(() -> {
				TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.CHEST), 1, "a chest in the output chest");
				helper.assertTrue(WorkerSupplies.isPaid(helper.getLevel(), worker) && WorkerSupplies.emeralds(helper.getLevel(), worker) == 1,
					"The carpenter did not take its wage with its work: " + WorkerSupplies.emeralds(helper.getLevel(), worker) + " of 2 emeralds left");
			});
		});
	}

	/**
	 * An oak boat with chest ordered, but only planks: the carpenter makes the chest and the
	 * boat itself, then the boat with chest; neither is handed in on its own.
	 */
	private static void carpenterMakesMissingIngredient(GameTestHelper helper) {
		helper.setBlock(STATION, WorkingVillagers.CARPENTER_BENCH.get());
		Workstations.Station bench = TestWorld.station(helper, STATION);
		bench.setWood("oak", true);
		bench.orderProduct(Items.OAK_CHEST_BOAT);
		bench.setBatch(bench.orders().getFirst(), 1);
		Villager worker = processingStation(helper, new ItemStack(Items.OAK_PLANKS, 13));
		// An apprentice makes boats with chest
		TestWorld.setLevel(worker, WorkerLevels.APPRENTICE);
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.OAK_CHEST_BOAT), 1, "an oak boat with chest in the output chest");
			helper.assertTrue(TestWorld.count(helper, OUTPUT, stack -> stack.is(Items.CHEST) || stack.is(Items.OAK_BOAT)) == 0,
				"The carpenter handed in the chest or the boat it made for the order");
		});
	}

	/** Ten oak planks ordered: the sawyer cuts three logs (twelve planks) and leaves the rest. */
	private static void sawyerFillsOrder(GameTestHelper helper) {
		helper.setBlock(STATION, WorkingVillagers.WOODCUTTER_BLOCK.get());
		Workstations.Station saw = TestWorld.station(helper, STATION);
		saw.setWood("oak", true);
		saw.setShape("planks");
		saw.setBatch(saw.orders().getFirst(), 10);
		Villager worker = processingStation(helper, new ItemStack(Items.OAK_LOG, 8));
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.OAK_PLANKS), 10, "oak planks in the output chest");
			helper.assertTrue(!saw.hasOrder(), "The order is not done: " + TestWorld.describe(worker));
			helper.assertTrue(TestWorld.count(helper, INPUT, stack -> stack.is(Items.OAK_LOG)) == 5, "The sawyer cut more logs than the order needs");
		});
	}

	/**
	 * Two glass ordered at the kiln: the kiln operator fires two and stops. The done order stays
	 * (without one the kiln would fire everything).
	 */
	private static void kilnOperatorKeepsFinishedOrder(GameTestHelper helper) {
		helper.setBlock(STATION, WorkingVillagers.KILN.get());
		Workstations.Station kiln = TestWorld.station(helper, STATION);
		kiln.toggleOrder(Items.GLASS);
		kiln.setBatch(Items.GLASS, 2);
		Villager worker = processingStation(helper, new ItemStack(Items.SAND, 8), new ItemStack(Items.COBBLESTONE, 8), new ItemStack(Items.COAL, 4));
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.GLASS), 2, "glass in the output chest");
			Workstations.Station.Batch batch = kiln.batch(Items.GLASS);
			helper.assertTrue(batch != null && batch.handedIn(), "The done order was not kept: " + TestWorld.describe(worker));
			helper.assertTrue(TestWorld.count(helper, OUTPUT, stack -> stack.is(Items.GLASS)) == 2, "The kiln operator fired more than ordered");
			helper.assertTrue(TestWorld.count(helper, INPUT, stack -> stack.is(Items.COBBLESTONE)) == 8, "The kiln operator fired what was not ordered");
		});
	}

	/** Spectral arrows ordered, but no arrows: the fletcher makes arrows first. */
	private static void fletcherMakesArrowsForSpectralArrows(GameTestHelper helper) {
		Villager worker = fletcher(helper, Items.SPECTRAL_ARROW, 0, new ItemStack(Items.GLOWSTONE_DUST, 4), new ItemStack(Items.FLINT, 1),
			new ItemStack(Items.STICK, 1), new ItemStack(Items.FEATHER, 1));
		// A journeyman makes spectral arrows
		TestWorld.setLevel(worker, WorkerLevels.JOURNEYMAN);
		Workstations.Station table = TestWorld.station(helper, STATION.below());
		table.setBatch(Items.SPECTRAL_ARROW, 2);
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.SPECTRAL_ARROW), 2, "spectral arrows in the output chest");
			helper.assertTrue(!table.hasOrder(), "The order is not done: " + TestWorld.describe(worker));
		});
	}

	/**
	 * Ripe wheat next to the composter: the farmer brings the wheat to the chest and plants
	 * the seeds it got from the harvest. An empty spot of farmland gets wheat as well.
	 */
	private static void farmerHarvestsAndReplants(GameTestHelper helper) {
		helper.setBlock(STATION, Blocks.COMPOSTER);
		// A field in the floor, and a raised bed one block higher
		List<BlockPos> crops = List.of(new BlockPos(10, 0, 10), new BlockPos(11, 0, 10), new BlockPos(12, 1, 13));
		BlockPos empty = new BlockPos(12, 0, 10);
		for (BlockPos crop : crops) {
			helper.setBlock(crop.below(), Blocks.FARMLAND);
			helper.setBlock(crop, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE));
		}
		helper.setBlock(empty.below(), Blocks.FARMLAND);
		helper.setBlock(new BlockPos(11, -1, 11), Blocks.WATER);
		TestWorld.chest(helper, OUTPUT);
		TestWorld.link(helper, STATION, OUTPUT, ChestRole.OUTPUT);
		Villager worker = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, worker, STATION);
		TestWorld.give(helper, worker, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.IRON_HOE));
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.WHEAT), 3, "wheat in the output chest");
			helper.assertTrue(TestWorld.count(helper, OUTPUT, stack -> stack.is(Items.IRON_HOE)) == 0, "The farmer delivered its hoe");
			for (BlockPos crop : crops) {
				helper.assertBlockPresent(Blocks.WHEAT, crop);
			}
			helper.assertBlockPresent(Blocks.WHEAT, empty);
		});
	}

	/**
	 * A farmer with wheat in its pockets and only a worn hoe: it gives the hoe back (into the
	 * output chest, no smith repairs from the village chest), takes the new one from the
	 * village chest and harvests with it, never without a hoe.
	 */
	private static void farmerSwapsWornHoe(GameTestHelper helper) {
		helper.setBlock(STATION, Blocks.COMPOSTER);
		List<BlockPos> crops = List.of(new BlockPos(10, 0, 10), new BlockPos(11, 0, 10), new BlockPos(12, 0, 10), new BlockPos(10, 0, 12));
		for (BlockPos crop : crops) {
			helper.setBlock(crop.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE));
			helper.setBlock(crop, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE));
		}
		TestWorld.villageChest(helper, VILLAGE_CHEST, new ItemStack(Items.IRON_HOE));
		TestWorld.chest(helper, OUTPUT);
		TestWorld.link(helper, STATION, OUTPUT, ChestRole.OUTPUT);
		Villager worker = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, worker, STATION);
		TestWorld.give(helper, worker, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.WHEAT_SEEDS, 16));
		ItemStack worn = new ItemStack(Items.IRON_HOE);
		worn.setDamageValue(worn.getMaxDamage() - 20);
		// Into its inventory: a worker does not accept a worn tool from a player
		worker.getInventory().addItem(worn);
		worker.getInventory().addItem(new ItemStack(Items.WHEAT, 20));
		helper.succeedWhen(() -> {
			helper.assertTrue(TestWorld.count(helper, OUTPUT, stack -> stack.is(Items.IRON_HOE) && WorkerSupplies.isWorn(stack)) == 1,
				"The worn hoe is not in the output chest: " + TestWorld.describe(worker));
			int harvested = ((HiredVillager) worker).workingvillagers$getWorkDone(me.teajay.workingvillagers.job.ChopTrees.today(helper.getLevel()));
			helper.assertTrue(harvested >= crops.size(), "Harvested " + harvested + " of " + crops.size());
			int wear = worker.getInventory().getItems().stream().filter(stack -> stack.is(Items.IRON_HOE)).mapToInt(ItemStack::getDamageValue).sum();
			helper.assertTrue(wear == harvested, "Harvested " + harvested + " crops, but the hoe wore only " + wear + ": harvested without a hoe");
			TestWorld.removeVillageChest(helper, VILLAGE_CHEST);
		});
	}

	/** A pen of fences without a gate (3 × 3 inside, around {@link #PEN}) with sheep in it, a loom and an output chest. */
	private static List<Sheep> pen(GameTestHelper helper, int sheep, boolean sheared) {
		helper.setBlock(STATION.below(), Blocks.LOOM);
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				if (Math.abs(x) == 2 || Math.abs(z) == 2) {
					helper.setBlock(PEN.offset(x, 0, z), Blocks.OAK_FENCE);
				}
			}
		}
		List<Sheep> herd = new java.util.ArrayList<>();
		for (int i = 0; i < sheep; i++) {
			Sheep animal = helper.spawn(EntityTypes.SHEEP, PEN.offset(i - 1, 0, 0));
			animal.setColor(DyeColor.WHITE);
			animal.setSheared(sheared);
			herd.add(animal);
		}
		TestWorld.chest(helper, OUTPUT);
		TestWorld.link(helper, STATION.below(), OUTPUT, ChestRole.OUTPUT);
		return herd;
	}

	/** Sheep in a pen without a gate: the shepherd shears them over the fence and brings the wool to the chest. */
	private static void shepherdShearsOverFence(GameTestHelper helper) {
		List<Sheep> herd = pen(helper, 2, false);
		Villager worker = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, worker, STATION.below());
		TestWorld.give(helper, worker, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.SHEARS));
		helper.succeedWhen(() -> {
			helper.assertTrue(herd.stream().allMatch(Sheep::isSheared), "Not every sheep was sheared");
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.WOOL.white()), 2, "white wool in the output chest");
			helper.assertTrue(TestWorld.count(helper, OUTPUT, stack -> stack.is(Items.SHEARS)) == 0, "The shepherd delivered its shears");
		});
	}

	/** Two sheared sheep, fewer than the herd size: the shepherd feeds them wheat and they get a lamb. */
	private static void shepherdBreedsSheep(GameTestHelper helper) {
		pen(helper, 2, true);
		Villager worker = TestWorld.hireVillager(helper, WORKER);
		// A apprentice breeds
		TestWorld.setLevel(worker, WorkerLevels.APPRENTICE);
		TestWorld.assign(helper, worker, STATION.below());
		TestWorld.give(helper, worker, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.SHEARS), new ItemStack(Items.WHEAT, 4));
		helper.succeedWhen(() -> {
			List<Sheep> herd = TendSheep.herd(helper.getLevel(), helper.absolutePos(STATION.below()));
			helper.assertTrue(herd.stream().anyMatch(Sheep::isBaby), "No lamb yet, " + herd.size() + " sheep");
			helper.assertTrue(TestWorld.count(helper, OUTPUT, stack -> stack.is(Items.WHEAT)) == 0, "The shepherd delivered its wheat");
		});
	}

	/**
	 * A hired villager without a job walks to a free stonecutter and becomes a stone sawyer
	 * there, not a mason.
	 */
	private static void workerFindsWorkplace(GameTestHelper helper) {
		// On the floor: a job site has to be reachable, like vanilla ones
		BlockPos station = STATION.below();
		helper.setBlock(station, Blocks.STONECUTTER);
		Villager worker = TestWorld.hireVillager(helper, new BlockPos(2, 0, 2));
		helper.succeedWhen(() -> {
			helper.assertTrue(worker.getVillagerData().profession().is(WorkingVillagers.STONECUTTER_PROFESSION),
				"Not a stone sawyer: " + worker.getVillagerData().profession().getRegisteredName()
					+ ", potential " + worker.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE) + ", job " + worker.getBrain().getMemory(MemoryModuleType.JOB_SITE)
					+ ", at " + worker.blockPosition() + ", station " + helper.absolutePos(station));
			helper.assertTrue(TestWorld.station(helper, station).occupant(helper.getLevel()).isPresent(), "The station has no worker");
		});
	}

	/** Emeralds thrown next to a worker: it picks them up. */
	/** A thrown stack is only picked up as far as the worker still wants emeralds. */
	private static void workerPicksUpWage(GameTestHelper helper) {
		Villager worker = TestWorld.hireVillager(helper, WORKER);
		worker.getInventory().addItem(new ItemStack(Items.EMERALD, WorkerSupplies.MAX_EMERALDS - 4));
		Vec3 spot = helper.absoluteVec(Vec3.atBottomCenterOf(WORKER.east(2)));
		ItemEntity thrown = new ItemEntity(helper.getLevel(), spot.x, spot.y, spot.z, new ItemStack(Items.EMERALD, 10));
		helper.getLevel().addFreshEntity(thrown);
		helper.succeedWhen(() -> {
			helper.assertTrue(WorkerSupplies.count(worker.getInventory(), WorkerSupplies::isWage) == WorkerSupplies.MAX_EMERALDS, "The worker did not pick up the emeralds up to its limit");
			helper.assertTrue(thrown.isAlive() && thrown.getItem().getCount() == 6, "The rest of the stack must stay on the ground");
		});
	}

	/** A hired villager falling onto farmland leaves it; an ordinary one tramples it. */
	private static void workersDoNotTrampleFarmland(GameTestHelper helper) {
		BlockPos worked = new BlockPos(3, 0, 3);
		BlockPos trampled = new BlockPos(9, 0, 3);
		// Moist, so it does not dry out to dirt by itself meanwhile
		BlockState farmland = Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE);
		helper.setBlock(worked, farmland);
		helper.setBlock(trampled, farmland);
		Villager worker = TestWorld.hireVillager(helper, worked.above(4));
		Villager villager = helper.spawn(EntityTypes.VILLAGER, trampled.above(4));
		helper.succeedWhen(() -> {
			helper.assertTrue(worker.onGround() && villager.onGround(), "Still falling");
			helper.assertBlockPresent(Blocks.DIRT, trampled);
			helper.assertBlockPresent(Blocks.FARMLAND, worked);
		});
	}

	private static void smithSmeltsOre(GameTestHelper helper) {
		helper.setBlock(STATION, Blocks.BLAST_FURNACE);
		Villager worker = processingStation(helper, new ItemStack(Items.RAW_IRON, 4), new ItemStack(Items.COAL, 2));
		helper.succeedWhen(() -> TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.IRON_INGOT), 4, "iron ingots in the output chest"));
	}

	/** Sand becomes glass; meat (the smoker's) and raw iron (the blast furnace's) stay in the chest. */
	private static void kilnOperatorFiresGlass(GameTestHelper helper) {
		helper.setBlock(STATION, WorkingVillagers.KILN.get());
		Villager worker = processingStation(helper, new ItemStack(Items.SAND, 4), new ItemStack(Items.BEEF, 2), new ItemStack(Items.RAW_IRON, 2), new ItemStack(Items.COAL, 2));
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.GLASS), 4, "glass in the output chest");
			TestWorld.assertHolds(helper, worker, INPUT, stack -> stack.is(Items.BEEF), 2, "beef left in the input chest");
			TestWorld.assertHolds(helper, worker, INPUT, stack -> stack.is(Items.RAW_IRON), 2, "raw iron left in the input chest");
		});
	}

	/**
	 * With the order "smooth stone" cobblestone is fired twice; the stone in between stays
	 * in the kiln operator's hands. Sand is not fired for that order.
	 */
	private static void kilnOperatorFiresSmoothStone(GameTestHelper helper) {
		helper.setBlock(STATION, WorkingVillagers.KILN.get());
		TestWorld.station(helper, STATION).setOrder(Items.SMOOTH_STONE);
		Villager worker = processingStation(helper, new ItemStack(Items.COBBLESTONE, 2), new ItemStack(Items.SAND, 2), new ItemStack(Items.COAL, 2));
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.SMOOTH_STONE), 2, "smooth stone in the output chest");
			helper.assertTrue(TestWorld.count(helper, OUTPUT, stack -> stack.is(Items.STONE)) == 0, "The kiln operator delivered stone");
			TestWorld.assertHolds(helper, worker, INPUT, stack -> stack.is(Items.SAND), 2, "sand left in the input chest");
		});
	}

	/**
	 * The pit stake takes the ring of string it touches as the pit, standing in the ring or
	 * next to it; string connects to the stake. Strings on a dirt path still count, and an
	 * open ring says where it ends.
	 */
	private static void pitStakeTakesRingItTouches(GameTestHelper helper) {
		int ground = 5;
		for (int x = 4; x <= 16; x++) {
			for (int z = 4; z <= 16; z++) {
				for (int y = 1; y <= ground; y++) {
					helper.setBlock(new BlockPos(x, y, z), y == ground ? Blocks.GRASS_BLOCK : Blocks.DIRT);
				}
			}
		}
		// A ring around 3×3 columns, a dirt path under one side
		for (int i = 8; i <= 12; i++) {
			for (BlockPos string : List.of(new BlockPos(i, ground + 1, 8), new BlockPos(i, ground + 1, 12), new BlockPos(8, ground + 1, i), new BlockPos(12, ground + 1, i))) {
				helper.setBlock(string, Blocks.TRIPWIRE);
			}
			helper.setBlock(new BlockPos(i, ground, 12), Blocks.DIRT_PATH);
		}
		ServerLevel level = helper.getLevel();

		// The stake next to the ring
		BlockPos outside = new BlockPos(7, ground + 1, 10);
		helper.setBlock(outside, WorkingVillagers.PIT_STAKE.get());
		helper.assertTrue(helper.getBlockState(new BlockPos(8, ground + 1, 10)).getValue(TripWireBlock.WEST), "The string does not connect to the stake");
		Pit pit = Pit.fromStake(level, helper.absolutePos(outside), 64).left()
			.orElseThrow(() -> helper.assertionException(outside, "The ring next to the stake is no pit"));
		helper.assertTrue(pit.inside().size() == 9, "The pit has " + pit.inside().size() + " blocks instead of 9");
		Workstations.Station station = TestWorld.station(helper, outside);
		helper.assertTrue(Pit.attach(level, station).isEmpty() && station.pit() != null, "The stake did not take the ring");
		helper.setBlock(outside, Blocks.AIR);

		// The stake in the ring, instead of a string
		BlockPos inRing = new BlockPos(8, ground + 1, 10);
		helper.setBlock(inRing, WorkingVillagers.PIT_STAKE.get());
		pit = Pit.fromStake(level, helper.absolutePos(inRing), 64).left()
			.orElseThrow(() -> helper.assertionException(inRing, "The ring through the stake is no pit"));
		helper.assertTrue(pit.inside().size() == 9, "The pit has " + pit.inside().size() + " blocks instead of 9");
		helper.assertFalse(pit.ring().contains(helper.absolutePos(inRing)), "The stake is taken as a string of the ring");
		helper.assertTrue(pit.isIntact(level), "The ring through the stake is not intact");

		// An open ring tells where it ends
		helper.setBlock(new BlockPos(12, ground + 1, 10), Blocks.AIR);
		Pit.Problem open = Pit.fromStake(level, helper.absolutePos(inRing), 64).right()
			.orElseThrow(() -> helper.assertionException(inRing, "An open ring was taken as a pit"));
		Component problem = open.message();
		helper.assertTrue(problem.getContents() instanceof TranslatableContents contents && contents.getKey().equals("message.workingvillagers.pit.not_closed_at"),
			"Wrong problem for an open ring: " + problem.getString());
		helper.assertTrue(open.spots().equals(List.of(helper.absolutePos(new BlockPos(12, ground + 1, 9)))) || open.spots().equals(List.of(helper.absolutePos(new BlockPos(12, ground + 1, 11)))),
			"The open end is not marked: " + open.spots());

		// Too big for the pit digger: every string is marked
		helper.setBlock(new BlockPos(12, ground + 1, 10), Blocks.TRIPWIRE);
		Pit.Problem tooBig = Pit.fromStake(level, helper.absolutePos(inRing), 8).right()
			.orElseThrow(() -> helper.assertionException(inRing, "A ring around 9 blocks was taken as a pit of at most 8"));
		helper.assertTrue(tooBig.message().getContents() instanceof TranslatableContents contents && contents.getKey().equals("message.workingvillagers.pit.too_big"),
			"Wrong problem for a ring that is too big: " + tooBig.message().getString());
		helper.assertTrue(tooBig.spots().size() == 15, "Not every string of the ring is marked: " + tooBig.spots().size());

		// The preview for a player nearby: the pit, dug 4 deep (the default) below the strings
		PitPreviewPayload preview = PitPreviews.preview(level, helper.absolutePos(new BlockPos(4, ground + 1, 4)));
		helper.assertTrue(preview.stakes().size() == 1 && preview.stakes().getFirst().isPit(), "No pit in the preview: " + preview);
		PitPreviewPayload.Stake stake = preview.stakes().getFirst();
		helper.assertTrue(stake.inside().size() == 9 && stake.spots().isEmpty(), "Wrong pit in the preview: " + stake);
		helper.assertTrue(stake.bottom() == helper.absolutePos(new BlockPos(0, ground + 1 - Pit.DEFAULT_DEPTH, 0)).getY(), "Wrong floor in the preview: " + stake.bottom());
		helper.succeed();
	}

	/**
	 * A master pit digger digs a 3×3 pit {@code depth} deep into a mound of dirt: from the
	 * novice depth (8) on it lays a floor of planks every 4 blocks (10 deep: one, 14 deep: two),
	 * leaving the ladder column open, on a pillar of logs in the corner of the grid that goes
	 * from the bottom up to the first floor, through the floor below it.
	 */
	private static void pitDiggerBuildsStoreys(GameTestHelper helper, int depth) {
		int ground = depth + 3;
		for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(4, 1, 4), new BlockPos(16, ground, 16))) {
			helper.setBlock(pos, pos.getY() == ground ? Blocks.GRASS_BLOCK : Blocks.DIRT);
		}
		for (int i = 8; i <= 12; i++) {
			for (BlockPos string : List.of(new BlockPos(i, ground + 1, 8), new BlockPos(i, ground + 1, 12), new BlockPos(8, ground + 1, i), new BlockPos(12, ground + 1, i))) {
				helper.setBlock(string, Blocks.TRIPWIRE);
			}
		}
		BlockPos stake = new BlockPos(7, ground + 1, 10);
		helper.setBlock(stake, WorkingVillagers.PIT_STAKE.get());
		Workstations.Station station = TestWorld.station(helper, stake);
		helper.assertTrue(Pit.attach(helper.getLevel(), station).isEmpty() && station.pit() != null, "The stake did not take the ring");
		station.setDepth(depth);
		BlockPos output = new BlockPos(6, ground + 1, 13);
		TestWorld.chest(helper, output);
		TestWorld.link(helper, stake, output, ChestRole.OUTPUT);
		Villager worker = TestWorld.hireVillager(helper, new BlockPos(5, ground + 1, 6));
		TestWorld.assign(helper, worker, stake);
		worker.setVillagerData(worker.getVillagerData().withLevel(5));
		// A master's wage is 3 emeralds a day
		TestWorld.give(helper, worker, new ItemStack(Items.EMERALD, 6), new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.LADDER, 16),
			new ItemStack(Items.OAK_PLANKS, 32), new ItemStack(Items.OAK_LOG, 8));
		Pit.Storeys storeys = station.pit().storeys(helper.getLevel(), depth);
		int floors = depth == 10 ? 1 : 2;
		helper.assertTrue(storeys.floors().size() == floors && storeys.pillars().size() == 1 && storeys.planks() == 8 + (floors - 1) * 7
			&& storeys.logs() == depth - 8, "Wrong storeys: " + storeys);
		// The floors at depth 8 and 12, the bottom 4 blocks above the ground of the platform
		int floor = ground + 1 - 8;
		int lowerFloor = ground + 1 - 12;
		// The test may be rotated: compare columns in the world
		BlockPos ladder = station.pit().ladder();
		BlockPos pillar = storeys.pillars().getFirst();
		helper.succeedWhen(() -> {
			for (int x = 9; x <= 11; x++) {
				for (int z = 9; z <= 11; z++) {
					for (int y = ground + 1 - depth; y <= ground; y++) {
						BlockPos pos = new BlockPos(x, y, z);
						BlockPos at = helper.absolutePos(pos);
						BlockState state = helper.getBlockState(pos);
						boolean pillarColumn = at.getX() == pillar.getX() && at.getZ() == pillar.getZ();
						boolean ok = at.getX() == ladder.getX() && at.getZ() == ladder.getZ() ? state.is(Blocks.LADDER)
							: y == floor ? state.is(BlockTags.PLANKS)
							: pillarColumn && y < floor ? state.is(BlockTags.LOGS)
							: y == lowerFloor && floors == 2 ? state.is(BlockTags.PLANKS)
							: state.isAir() || state.getBlock() instanceof BaseTorchBlock;
						if (!ok) {
							throw helper.assertionException(pos, "Unexpected " + state + "; worker: " + TestWorld.describe(worker));
						}
					}
				}
			}
		});
	}

	/** From 8×8 and 8 deep for a novice to 16×16 and 16 deep for a master, the side growing by 2 a level. */
	private static void pitLimitsGrowWithLevel(GameTestHelper helper) {
		int[][] expected = {{64, 8}, {100, 10}, {144, 12}, {196, 14}, {256, 16}};
		for (int level = 1; level <= 5; level++) {
			PitLimits limits = PitLimits.forLevel(level);
			helper.assertTrue(limits.area() == expected[level - 1][0] && limits.depth() == expected[level - 1][1], "Wrong limits for level " + level + ": " + limits);
		}
		helper.succeed();
	}

	/**
	 * A 4×4 pit marked with string on a mound of dirt, 3 deep: the pit digger digs it out,
	 * puts ladders down to the bottom and a torch on the floor, leaves the ring and
	 * everything outside it alone and brings the dirt to the chest.
	 */
	private static void pitDiggerDigsPit(GameTestHelper helper) {
		pitDiggerDigsPit(helper, 4, false, false);
	}

	/**
	 * A 3×3 pit: the pit digger reaches every block from the ladder column it starts on, and
	 * still has to dig the ladder column with the rest of each layer.
	 */
	private static void pitDiggerDigsSmallPit(GameTestHelper helper) {
		pitDiggerDigsPit(helper, 3, false, false);
	}

	/**
	 * A 3×3 pit dug out to the bottom except for the column of the ladder (as earlier
	 * versions left it): the pit digger digs the ladder column, puts the ladders in and gets
	 * out again.
	 */
	private static void pitDiggerFinishesHalfDugPit(GameTestHelper helper) {
		pitDiggerDigsPit(helper, 3, true, false);
	}

	/** The same, with the pit digger down in the hole at the start. */
	private static void pitDiggerClimbsOutOfHalfDugPit(GameTestHelper helper) {
		pitDiggerDigsPit(helper, 3, true, true);
	}

	/**
	 * A pit {@code size}×{@code size} inside, starting at x 9, z 9, 3 deep. {@code halfDug}:
	 * everything but the ladder column (x 9) is dug out already.
	 */
	private static void pitDiggerDigsPit(GameTestHelper helper, int size, boolean halfDug, boolean startInHole) {
		int ground = 5;
		int far = 9 + size;
		for (int x = 4; x <= 16; x++) {
			for (int z = 4; z <= 16; z++) {
				for (int y = 1; y <= ground; y++) {
					helper.setBlock(new BlockPos(x, y, z), y == ground ? Blocks.GRASS_BLOCK : Blocks.DIRT);
				}
			}
		}
		// The ring around the inside, one string still missing
		BlockPos gap = new BlockPos(far, ground + 1, far);
		for (int i = 8; i <= far; i++) {
			for (BlockPos string : List.of(new BlockPos(i, ground + 1, 8), new BlockPos(i, ground + 1, far), new BlockPos(8, ground + 1, i), new BlockPos(far, ground + 1, i))) {
				if (!string.equals(gap)) {
					helper.setBlock(string, Blocks.TRIPWIRE);
				}
			}
		}
		if (halfDug) {
			for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(10, ground - 2, 9), new BlockPos(far - 1, ground, far - 1))) {
				helper.setBlock(pos, Blocks.AIR);
			}
		}
		// Next to the ring
		BlockPos stake = new BlockPos(7, ground + 1, 10);
		helper.setBlock(stake, WorkingVillagers.PIT_STAKE.get());
		helper.assertTrue(Pit.fromStake(helper.getLevel(), helper.absolutePos(stake), 64).left().isEmpty(), "An open ring was taken as a pit");
		helper.setBlock(gap, Blocks.TRIPWIRE);
		Pit pit = Pit.fromStake(helper.getLevel(), helper.absolutePos(stake), 64)
			.left().orElseThrow(() -> helper.assertionException(gap, "The closed ring is no pit"));
		helper.assertTrue(pit.inside().size() == size * size, "The pit has " + pit.inside().size() + " blocks instead of " + size * size);
		Workstations.Station station = TestWorld.station(helper, stake);
		station.setPit(pit);
		station.setDepth(3);
		BlockPos output = new BlockPos(6, ground + 1, 13);
		TestWorld.chest(helper, output);
		TestWorld.link(helper, stake, output, ChestRole.OUTPUT);
		Villager worker = TestWorld.hireVillager(helper, new BlockPos(5, ground + 1, 6));
		TestWorld.assign(helper, worker, stake);
		TestWorld.give(helper, worker, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.LADDER, 8),
			new ItemStack(Items.TORCH, 4));
		// It starts on the ladder column (the block under it has to be dug like every other), or
		// down in the hole
		Vec3 start = helper.absoluteVec(Vec3.atBottomCenterOf(startInHole ? new BlockPos(10, ground - 2, 10) : new BlockPos(9, ground + 1, 10)));
		worker.teleportTo(start.x, start.y, start.z);
		// It never gets stuck on the way (a worker that does is brought back after a minute)
		helper.failIfEver(() -> helper.assertFalse(((HiredVillager) worker).workingvillagers$getStuckWatch().isStuck(), "The pit digger got stuck: " + TestWorld.describe(worker)));
		// The ladder goes into the column inside closest to the stake, against the ring
		BlockPos ladder = new BlockPos(9, 0, 10);
		helper.succeedWhen(() -> {
			for (int x = 9; x < far; x++) {
				for (int z = 9; z < far; z++) {
					for (int y = ground - 2; y <= ground; y++) {
						BlockPos pos = new BlockPos(x, y, z);
						Block expected = x == ladder.getX() && z == ladder.getZ() ? Blocks.LADDER : Blocks.AIR;
						// A torch on the floor
						boolean torch = y == ground - 2 && helper.getBlockState(pos).is(Blocks.TORCH);
						if (!helper.getBlockState(pos).is(expected) && !torch) {
							throw helper.assertionException(pos, "Expected " + expected + ", found " + helper.getBlockState(pos) + "; worker: " + TestWorld.describe(worker)
								+ " at " + helper.relativePos(worker.blockPosition()));
						}
					}
					// The floor stays (grass may grow on it in the light)
					BlockPos floor = new BlockPos(x, ground - 3, z);
					helper.assertTrue(helper.getBlockState(floor).is(Blocks.DIRT) || helper.getBlockState(floor).is(Blocks.GRASS_BLOCK),
						"The floor of the pit was dug at " + floor);
				}
			}
			for (int i = 8; i <= far; i++) {
				helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(i, ground, 8));
				helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(8, ground, i));
			}
			TestWorld.assertHolds(helper, worker, output, stack -> stack.is(Items.DIRT), halfDug ? size * 3 - 1 : size * size * 3 - 1, "dirt in the output chest");
			helper.assertFalse(Pit.fromStake(helper.getLevel(), helper.absolutePos(stake), 64).left().isEmpty(), "The ring was damaged");
			helper.assertTrue(worker.getY() > helper.absolutePos(new BlockPos(0, ground, 0)).getY(), "The pit digger is still in the pit: " + TestWorld.describe(worker));
			int torches = 0;
			for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(9, ground - 2, 9), new BlockPos(far - 1, ground - 2, far - 1))) {
				torches += helper.getBlockState(pos).is(Blocks.TORCH) ? 1 : 0;
			}
			helper.assertTrue(halfDug ? torches <= 1 : torches == 1, "Expected one torch on the floor, found " + torches);
		});
	}

	/**
	 * A pit as it was left in a played world by an earlier version: a 3×3 pit with a ring
	 * without corners, the stake right next to the exit, dug out 4 deep except for two
	 * columns (one of them the ladder column), no ladders, and leaves of a tree above those
	 * columns. The pit digger finishes it.
	 */
	private static void pitDiggerFinishesPlayedPit(GameTestHelper helper) {
		for (int x = 4; x <= 18; x++) {
			for (int z = 5; z <= 18; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
				for (int y = 2; y <= 4; y++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.DIRT);
				}
				helper.setBlock(new BlockPos(x, 5, z), Blocks.GRASS_BLOCK);
			}
		}
		List<BlockPos> dug = new java.util.ArrayList<>(List.of(new BlockPos(11, 0, 12)));
		for (int x = 9; x <= 10; x++) {
			for (int z = 10; z <= 12; z++) {
				dug.add(new BlockPos(x, 0, z));
			}
		}
		for (BlockPos column : dug) {
			for (int y = 2; y <= 5; y++) {
				helper.setBlock(column.atY(y), Blocks.AIR);
			}
		}
		List<BlockPos> ring = new java.util.ArrayList<>();
		for (int i = 9; i <= 11; i++) {
			ring.add(new BlockPos(i, 6, 9));
			ring.add(new BlockPos(i, 6, 13));
		}
		for (int i = 10; i <= 12; i++) {
			ring.add(new BlockPos(8, 6, i));
			ring.add(new BlockPos(12, 6, i));
		}
		ring.forEach(string -> helper.setBlock(string, Blocks.TRIPWIRE));
		// The crown of a tree hangs over the pit: it stays, the ground below is dug
		BlockState leaves = Blocks.BIRCH_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
		helper.setBlock(new BlockPos(11, 9, 10), leaves);
		helper.setBlock(new BlockPos(11, 10, 11), leaves);
		List<BlockPos> inside = new java.util.ArrayList<>();
		for (int x = 9; x <= 11; x++) {
			for (int z = 10; z <= 12; z++) {
				inside.add(helper.absolutePos(new BlockPos(x, 6, z)));
			}
		}
		BlockPos stake = new BlockPos(13, 6, 11);
		helper.setBlock(stake, WorkingVillagers.PIT_STAKE.get().defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.WEST));
		Pit pit = new Pit(ring.stream().map(helper::absolutePos).toList(), inside, helper.absolutePos(new BlockPos(12, 6, 11)),
			helper.absolutePos(new BlockPos(11, 5, 11)), helper.absolutePos(new BlockPos(0, 6, 0)).getY());
		Workstations.Station station = TestWorld.station(helper, stake);
		station.setPit(pit);
		BlockPos output = new BlockPos(15, 6, 15);
		TestWorld.chest(helper, output);
		TestWorld.link(helper, stake, output, ChestRole.OUTPUT);
		Villager worker = TestWorld.hireVillager(helper, new BlockPos(15, 6, 11));
		TestWorld.assign(helper, worker, stake);
		TestWorld.give(helper, worker, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.LADDER, 8));
		helper.failIfEver(() -> helper.assertFalse(((HiredVillager) worker).workingvillagers$getStuckWatch().isStuck(), "The pit digger got stuck: " + TestWorld.describe(worker)));
		helper.succeedWhen(() -> {
			for (int y = 2; y <= 5; y++) {
				BlockPos pos = new BlockPos(11, y, 11);
				if (!helper.getBlockState(pos).is(Blocks.LADDER) || !helper.getBlockState(new BlockPos(11, y, 10)).isAir()) {
					throw helper.assertionException(pos, "Not dug yet; worker: " + TestWorld.describe(worker) + " at " + helper.relativePos(worker.blockPosition()));
				}
			}
			helper.assertTrue(worker.getY() > helper.absolutePos(new BlockPos(0, 5, 0)).getY(), "The pit digger is still in the pit: " + TestWorld.describe(worker));
		});
	}

	/**
	 * Two composters on the floor, one set up with a chest: an ordinary unemployed villager
	 * becomes a farmer at the other one.
	 */
	private static void ordinaryVillagerKeepsOffSetUpStation(GameTestHelper helper) {
		BlockPos setUp = new BlockPos(8, 0, 8);
		BlockPos plain = new BlockPos(14, 0, 14);
		helper.setBlock(setUp, Blocks.COMPOSTER);
		helper.setBlock(plain, Blocks.COMPOSTER);
		TestWorld.chest(helper, OUTPUT);
		TestWorld.link(helper, setUp, OUTPUT, ChestRole.OUTPUT);
		Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(8, 0, 4));
		helper.failIfEver(() -> helper.assertFalse(TestWorld.station(helper, setUp).occupant(helper.getLevel()).isPresent(), "The set-up composter was taken"));
		helper.succeedWhen(() -> helper.assertTrue(villager.getVillagerData().profession().is(VillagerProfession.FARMER)
			&& TestWorld.station(helper, plain).occupant(helper.getLevel()).isPresent(), "The villager did not become a farmer at the other composter"));
	}

	/**
	 * An ordinary farmer that has never traded works at the composter: assigning a hired
	 * villager makes it unemployed and gives the composter to the hired one.
	 */
	private static void assignDisplacesOrdinaryVillager(GameTestHelper helper) {
		BlockPos composter = STATION.below();
		helper.setBlock(composter, Blocks.COMPOSTER);
		Villager farmer = helper.spawn(EntityTypes.VILLAGER, new BlockPos(6, 0, 8));
		BlockPos absolute = helper.absolutePos(composter);
		helper.getLevel().getPoiManager().take(poi -> true, (poi, pos) -> pos.equals(absolute), absolute, 1);
		farmer.getBrain().setMemory(MemoryModuleType.JOB_SITE, net.minecraft.core.GlobalPos.of(helper.getLevel().dimension(), absolute));
		farmer.setVillagerData(farmer.getVillagerData().withProfession(helper.getLevel().registryAccess(), VillagerProfession.FARMER));
		Villager worker = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, worker, composter);
		helper.assertTrue(worker.getVillagerData().profession().is(WorkingVillagers.FARMER_PROFESSION), "The hired villager is no farmer");
		helper.assertTrue(farmer.getVillagerData().profession().is(VillagerProfession.NONE), "The ordinary farmer kept its job");
		helper.succeed();
	}

	/**
	 * A vanilla farmer nearby buys 20 wheat for an emerald, up to 3 times: the shop assistant
	 * sells it the wheat from the input chest and brings the emeralds to the output chest.
	 * The stone it cannot sell stays in the input chest.
	 */
	private static void shopAssistantSellsWheat(GameTestHelper helper) {
		helper.setBlock(STATION, WorkingVillagers.TRADING_COUNTER.get());
		Villager farmer = helper.spawn(EntityTypes.VILLAGER, new BlockPos(14, 1, 12));
		farmer.setVillagerData(farmer.getVillagerData().withProfession(helper.getLevel().registryAccess(), VillagerProfession.FARMER));
		// Has traded before, so it keeps its job without a composter
		farmer.setVillagerXp(1);
		MerchantOffers offers = new MerchantOffers();
		offers.add(new MerchantOffer(new ItemCost(Items.WHEAT, 20), new ItemStack(Items.EMERALD), 3, 2, 0.05F));
		farmer.setOffers(offers);
		Villager worker = processingStation(helper, new ItemStack(Items.WHEAT, 64), new ItemStack(Items.STONE, 10));
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.EMERALD), 3, "emeralds in the output chest");
			helper.assertTrue(offers.getFirst().isOutOfStock(), "The farmer's trade was not used up");
			TestWorld.assertHolds(helper, worker, INPUT, stack -> stack.is(Items.WHEAT), 4, "unsold wheat back in the input chest");
			TestWorld.assertHolds(helper, worker, INPUT, stack -> stack.is(Items.STONE), 10, "stone left in the input chest");
			helper.assertTrue(farmer.getVillagerXp() > 1, "The farmer earned no experience");
		});
	}

	private static void carrierMovesItems(GameTestHelper helper) {
		TestWorld.chest(helper, INPUT, new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.COBBLESTONE, 10));
		TestWorld.chest(helper, OUTPUT);
		ItemStack basket = new ItemStack(WorkingVillagers.WICKER_BASKET.get());
		CarrierData.writeRoutes(basket, List.of(new Route(helper.absolutePos(INPUT), helper.absolutePos(OUTPUT))));
		Villager worker = helper.spawn(EntityTypes.VILLAGER, WORKER);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, basket);
		Carriers.appoint(worker, player, basket, InteractionHand.MAIN_HAND);
		helper.assertTrue(Carriers.isCarrier(worker), "The basket did not make the villager a carrier");
		TestWorld.give(helper, worker, new ItemStack(Items.EMERALD, 2));
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.COBBLESTONE), 74, "cobblestone in the target");
			helper.assertTrue(TestWorld.count(helper, INPUT, stack -> !stack.isEmpty()) == 0, "The source is not empty");
		});
	}
}
