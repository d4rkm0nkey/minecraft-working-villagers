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
import me.teajay.workingvillagers.station.Workstations.ChestRole;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The game tests: hiring, and one test per profession that sets up a station like a player
 * would and checks that the worker gets its wage and delivers products. The tests that need
 * work run in the work hours (environment {@code workingvillagers:work_hours}); see the
 * test instances in {@code data/workingvillagers/test_instance}.
 */
public final class WorkerTests {
	private static final BlockPos WORKER = new BlockPos(6, 1, 6);
	private static final BlockPos STATION = new BlockPos(8, 1, 8);
	private static final BlockPos PERSONAL_CHEST = new BlockPos(4, 1, 4);
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
		registry.accept("sawyer_cuts_planks", WorkerTests::sawyerCutsPlanks);
		registry.accept("stone_sawyer_cuts_bricks", WorkerTests::stoneSawyerCutsBricks);
		registry.accept("carpenter_makes_chests", WorkerTests::carpenterMakesChests);
		registry.accept("carrier_moves_items", WorkerTests::carrierMovesItems);
	}

	private static void contractHiresUnemployed(GameTestHelper helper) {
		Villager villager = helper.spawn(EntityTypes.VILLAGER, WORKER);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack contract = new ItemStack(WorkingVillagers.WORK_CONTRACT.get());
		WorkContractItem.hire(villager, player, contract, InteractionHand.MAIN_HAND);
		helper.assertTrue(((HiredVillager) villager).workingvillagers$isHired(), "The villager is not hired");
		helper.assertTrue(villager.hasCustomName(), "The hired villager has no name");
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

	/** Sets up a chopping block with an output chest and a paid lumberjack with an axe. */
	private static Villager lumberjack(GameTestHelper helper, ItemStack... personal) {
		helper.setBlock(STATION, WorkingVillagers.CHOPPING_BLOCK.get());
		TestWorld.chest(helper, OUTPUT);
		TestWorld.link(helper, STATION, OUTPUT, ChestRole.OUTPUT);
		Villager villager = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, villager, STATION);
		TestWorld.personalChest(helper, villager, PERSONAL_CHEST, personal);
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
			TestWorld.assertHolds(helper, worker, PERSONAL_CHEST, stack -> stack.is(Items.EMERALD), 1, "emeralds left");
			helper.assertTrue(TestWorld.count(helper, PERSONAL_CHEST, stack -> stack.is(Items.EMERALD)) == 1, "The wage was not taken");
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
		lumberjack(helper, new ItemStack(Items.IRON_AXE));
		helper.failIfEver(() -> helper.assertBlockPresent(Blocks.OAK_LOG, tree));
		helper.runAfterDelay(600, () -> {
			helper.assertTrue(TestWorld.count(helper, PERSONAL_CHEST, stack -> stack.is(ItemTags.AXES)) == 1, "The unpaid lumberjack took its axe");
			helper.succeed();
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
		TestWorld.personalChest(helper, villager, PERSONAL_CHEST, new ItemStack(Items.EMERALD, 2));
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
		TestWorld.personalChest(helper, worker, PERSONAL_CHEST, new ItemStack(Items.EMERALD, 2));
		helper.succeedWhen(() -> {
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.COBBLESTONE), 74, "cobblestone in the target");
			helper.assertTrue(TestWorld.count(helper, INPUT, stack -> !stack.isEmpty()) == 0, "The source is not empty");
		});
	}
}
