package me.teajay.workingvillagers.gametest;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.carrier.CarrierData;
import me.teajay.workingvillagers.carrier.Carriers;
import me.teajay.workingvillagers.carrier.Route;
import me.teajay.workingvillagers.item.WorkContractItem;
import me.teajay.workingvillagers.job.HiredVillager;
import me.teajay.workingvillagers.job.WorkerStatus;
import me.teajay.workingvillagers.job.WorkerSupplies;
import me.teajay.workingvillagers.station.Workstations;
import me.teajay.workingvillagers.station.Workstations.ChestRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
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
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
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

	private WorkerTests() {
	}

	/** Registers every test function under its name (the path of the test instance). */
	public static void register(BiConsumer<String, Consumer<GameTestHelper>> registry) {
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
		registry.accept("old_station_notes_are_removed", WorkerTests::oldStationNotesAreRemoved);
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
			helper.assertTrue(WorkerSupplies.count(worker.getInventory(), WorkerSupplies::isWage) == 1, "The wage was not taken");
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
