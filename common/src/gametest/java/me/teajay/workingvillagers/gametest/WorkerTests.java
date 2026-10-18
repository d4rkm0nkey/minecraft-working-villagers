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
import me.teajay.workingvillagers.job.HiredVillager;
import me.teajay.workingvillagers.job.WorkerStatus;
import me.teajay.workingvillagers.job.WorkerSupplies;
import me.teajay.workingvillagers.station.Pit;
import me.teajay.workingvillagers.station.StationType;
import me.teajay.workingvillagers.station.Workstations;
import me.teajay.workingvillagers.station.Workstations.ChestRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.entity.animal.sheep.Sheep;
import me.teajay.workingvillagers.job.TendSheep;

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
		registry.accept("contract_hires_unemployed", WorkerTests::contractHiresUnemployed);
		registry.accept("contract_rejects_employed", WorkerTests::contractRejectsEmployed);
		registry.accept("lumberjack_fells_tree", WorkerTests::lumberjackFellsTree);
		registry.accept("lumberjack_fells_tree_on_mud", WorkerTests::lumberjackFellsTreeOnMud);
		registry.accept("lumberjack_fells_tree_on_grass", WorkerTests::lumberjackFellsTreeOnGrass);
		registry.accept("lumberjack_spares_builds", WorkerTests::lumberjackSparesBuilds);
		registry.accept("no_wage_no_work", WorkerTests::noWageNoWork);
		registry.accept("stuck_worker_is_brought_back", WorkerTests::stuckWorkerIsBroughtBack);
		registry.accept("sawyer_cuts_planks", WorkerTests::sawyerCutsPlanks);
		registry.accept("stone_sawyer_cuts_bricks", WorkerTests::stoneSawyerCutsBricks);
		registry.accept("carpenter_makes_chests", WorkerTests::carpenterMakesChests);
		registry.accept("carrier_moves_items", WorkerTests::carrierMovesItems);
		registry.accept("farmer_harvests_and_replants", WorkerTests::farmerHarvestsAndReplants);
		registry.accept("workers_do_not_trample_farmland", WorkerTests::workersDoNotTrampleFarmland);
		registry.accept("worker_finds_workplace", WorkerTests::workerFindsWorkplace);
		registry.accept("worker_picks_up_wage", WorkerTests::workerPicksUpWage);
		registry.accept("smith_smelts_ore", WorkerTests::smithSmeltsOre);
		registry.accept("replaced_station_starts_fresh", WorkerTests::replacedStationStartsFresh);
		registry.accept("kiln_operator_fires_glass", WorkerTests::kilnOperatorFiresGlass);
		registry.accept("kiln_operator_fires_smooth_stone", WorkerTests::kilnOperatorFiresSmoothStone);
		registry.accept("pit_digger_digs_pit", WorkerTests::pitDiggerDigsPit);
		registry.accept("pit_digger_digs_small_pit", WorkerTests::pitDiggerDigsSmallPit);
		registry.accept("pit_digger_finishes_half_dug_pit", WorkerTests::pitDiggerFinishesHalfDugPit);
		registry.accept("pit_digger_climbs_out_of_half_dug_pit", WorkerTests::pitDiggerClimbsOutOfHalfDugPit);
		registry.accept("pit_digger_finishes_played_pit", WorkerTests::pitDiggerFinishesPlayedPit);
		registry.accept("shop_assistant_sells_wheat", WorkerTests::shopAssistantSellsWheat);
		registry.accept("ordinary_villager_keeps_off_set_up_station", WorkerTests::ordinaryVillagerKeepsOffSetUpStation);
		registry.accept("assign_displaces_ordinary_villager", WorkerTests::assignDisplacesOrdinaryVillager);
		registry.accept("old_station_notes_are_removed", WorkerTests::oldStationNotesAreRemoved);
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
		// The tests run side by side: the village chests of one test must not reach the workers of the next
		Config.VILLAGE_CHEST_RADIUS.set(8);
		MinerTests.configure();
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

	private static void contractRejectsEmployed(GameTestHelper helper) {
		Villager farmer = helper.spawn(EntityTypes.VILLAGER, WORKER);
		farmer.setVillagerData(farmer.getVillagerData().withProfession(helper.getLevel().registryAccess(), VillagerProfession.FARMER));
		Villager baby = helper.spawn(EntityTypes.VILLAGER, WORKER.east(2));
		baby.setAge(-24000);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		for (Villager villager : new Villager[] {farmer, baby}) {
			ItemStack contract = new ItemStack(WorkingVillagers.WORK_CONTRACT.get());
			WorkContractItem.hire(villager, player, contract, InteractionHand.MAIN_HAND);
			helper.assertFalse(((HiredVillager) villager).workingvillagers$isHired(), "A farmer or a baby must not be hired");
			helper.assertFalse(contract.isEmpty(), "The contract was used up although nobody was hired");
		}
		helper.succeed();
	}

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
	 * axe into the village chest, where it does not take it again.
	 */
	private static void lumberjackGivesBackWornAxe(GameTestHelper helper) {
		BlockPos tree = new BlockPos(15, 1, 15);
		TestWorld.oakTree(helper, tree, 5, true, Blocks.DIRT);
		TestWorld.villageChest(helper, VILLAGE_CHEST);
		Villager worker = lumberjack(helper, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.IRON_AXE));
		carryWornAxe(worker);
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.OAK_LOG), 5, "oak logs in the output chest");
			helper.assertTrue(TestWorld.count(helper, VILLAGE_CHEST, WorkerTests::isWornAxe) == 1,
				"The worn axe is not in the village chest: " + TestWorld.describe(worker));
			helper.assertTrue(WorkerSupplies.count(worker.getInventory(), stack -> stack.is(Items.IRON_AXE)) == 1, "The lumberjack lost its spare axe");
			TestWorld.removeVillageChest(helper, VILLAGE_CHEST);
		});
	}

	/**
	 * A lumberjack whose only axe is worn takes a new one from the village chest and leaves
	 * the worn one there, in one walk, and fells the tree.
	 */
	private static void lumberjackSwapsWornAxe(GameTestHelper helper) {
		BlockPos tree = new BlockPos(15, 1, 15);
		TestWorld.oakTree(helper, tree, 5, true, Blocks.DIRT);
		TestWorld.villageChest(helper, VILLAGE_CHEST, new ItemStack(Items.IRON_AXE));
		Villager worker = lumberjack(helper, new ItemStack(Items.EMERALD, 2));
		carryWornAxe(worker);
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.OAK_LOG), 5, "oak logs in the output chest");
			helper.assertTrue(TestWorld.count(helper, VILLAGE_CHEST, WorkerTests::isWornAxe) == 1,
				"The worn axe is not in the village chest: " + TestWorld.describe(worker));
			helper.assertTrue(TestWorld.count(helper, VILLAGE_CHEST, stack -> stack.is(Items.IRON_AXE)) == 1, "The lumberjack did not take the new axe");
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
		Villager worker = processingStation(helper, pickaxe, new ItemStack(Items.DIAMOND, 3), new ItemStack(Items.STONE, 4));
		helper.succeedWhen(() -> {
			helper.assertTrue(TestWorld.count(helper, INPUT, stack -> stack.is(Items.DIAMOND_PICKAXE) && !stack.isDamaged() && stack.isEnchanted()) == 1,
				"The pickaxe is not repaired in the input chest: " + TestWorld.describe(worker));
			helper.assertTrue(TestWorld.count(helper, INPUT, stack -> stack.is(Items.DIAMOND)) == 1, "The smith did not use 2 diamonds");
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
				helper.assertTrue(wage == 4, "The wage box lost its emeralds or the wage was not paid: " + wage);
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
		BlockPos stake = new BlockPos(6, ground + 1, 10);
		helper.setBlock(stake, WorkingVillagers.PIT_STAKE.get());
		helper.assertTrue(Pit.find(helper.getLevel(), helper.absolutePos(new BlockPos(8, ground + 1, 8)), helper.absolutePos(stake)).left().isEmpty(),
			"An open ring was taken as a pit");
		helper.setBlock(gap, Blocks.TRIPWIRE);
		Pit pit = Pit.find(helper.getLevel(), helper.absolutePos(new BlockPos(8, ground + 1, 8)), helper.absolutePos(stake))
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
			helper.assertFalse(Pit.find(helper.getLevel(), helper.absolutePos(gap), helper.absolutePos(stake)).left().isEmpty(), "The ring was damaged");
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

	/**
	 * Earlier versions put name notes (text displays) on the stations, also under the old mod
	 * name: they remove themselves. Other text displays stay.
	 */
	private static void oldStationNotesAreRemoved(GameTestHelper helper) {
		Display.TextDisplay note = textDisplay(helper, new BlockPos(4, 2, 4), "workingvillagers_note");
		Display.TextDisplay oldNote = textDisplay(helper, new BlockPos(6, 2, 4), "letmework_note");
		Display.TextDisplay sign = textDisplay(helper, new BlockPos(8, 2, 4), "some_other_tag");
		helper.succeedWhen(() -> {
			helper.assertTrue(note.isRemoved() && oldNote.isRemoved(), "An old station note is still there");
			helper.assertFalse(sign.isRemoved(), "Another text display was removed");
		});
	}

	private static Display.TextDisplay textDisplay(GameTestHelper helper, BlockPos pos, String tag) {
		Display.TextDisplay display = helper.spawn(EntityTypes.TEXT_DISPLAY, pos);
		display.addTag(tag);
		return display;
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
