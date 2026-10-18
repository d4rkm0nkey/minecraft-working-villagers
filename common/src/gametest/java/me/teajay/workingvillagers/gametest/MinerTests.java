package me.teajay.workingvillagers.gametest;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.crafting.CuttingRecipes;
import me.teajay.workingvillagers.job.HiredVillager;
import me.teajay.workingvillagers.station.Tunnel;
import me.teajay.workingvillagers.station.Workstations.ChestRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The game tests of the miner: a hill of stone in front of the mining bench, a tunnel
 * picked into it, a hired miner with a pickaxe. The miner digs fast here (the tests set its
 * slowdown to 1), so a tunnel takes seconds, not minutes.
 */
final class MinerTests {
	private static final BlockPos WORKER = new BlockPos(6, 1, 6);
	private static final BlockPos STATION = new BlockPos(8, 1, 8);
	private static final BlockPos OUTPUT = new BlockPos(12, 1, 6);
	/** The first block of the tunnel, on the floor of the platform; the tunnel goes south (+z), into the hill. */
	private static final BlockPos START = new BlockPos(8, 0, 12);

	private MinerTests() {
	}

	/** The miner digs as fast as a player, so the tests stay short. */
	static void configure() {
		Config.MINER_DIG_SLOWDOWN.set(1.0);
		Config.MINER_MIN_DIG_SECONDS.set(0.25);
	}

	static void register(BiConsumer<String, Consumer<GameTestHelper>> registry) {
		registry.accept("miner_digs_tunnel", MinerTests::minerDigsTunnel);
		registry.accept("miner_digs_ore_and_seals_lava", MinerTests::minerDigsOreAndSealsLava);
		registry.accept("miner_stops_at_planks", MinerTests::minerStopsAtPlanks);
		registry.accept("miner_digs_stairs", MinerTests::minerDigsStairs);
		registry.accept("miner_digs_side_tunnels", MinerTests::minerDigsSideTunnels);
		registry.accept("mill_grinds_cobblestone", MinerTests::millGrindsCobblestone);
	}

	/** Stone from x 3 to 13, z 12 to 22, from the floor (y 0, on the grass) up to y {@code top}. */
	private static void hill(GameTestHelper helper, int top) {
		for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(3, 0, 12), new BlockPos(13, top, 22))) {
			helper.setBlock(pos, Blocks.STONE);
		}
	}

	private static Villager miner(GameTestHelper helper, Tunnel tunnel, ItemStack... supplies) {
		helper.setBlock(STATION, WorkingVillagers.MINING_BENCH.get());
		TestWorld.chest(helper, OUTPUT);
		TestWorld.link(helper, STATION, OUTPUT, ChestRole.OUTPUT);
		TestWorld.station(helper, STATION).setTunnel(tunnel);
		Villager villager = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, villager, STATION);
		TestWorld.give(helper, villager, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.DIAMOND_PICKAXE));
		TestWorld.give(helper, villager, supplies);
		return villager;
	}

	private static Tunnel tunnel(GameTestHelper helper, BlockPos start, int length) {
		return Tunnel.of(helper.absolutePos(start), Direction.SOUTH).withLength(length);
	}

	/**
	 * A tunnel of 8 blocks: dug out 3×3, with the supports at the 4th and 8th block, a torch,
	 * and the stone in the output chest.
	 */
	private static void minerDigsTunnel(GameTestHelper helper) {
		hill(helper, 6);
		Tunnel tunnel = tunnel(helper, START, 8);
		Villager worker = miner(helper, tunnel, new ItemStack(Items.OAK_FENCE, 8), new ItemStack(Items.OAK_PLANKS, 8), new ItemStack(Items.TORCH, 4));
		helper.succeedWhen(() -> {
			assertDug(helper, worker, tunnel);
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.COBBLESTONE), 16, "cobblestone in the output chest");
		});
	}

	/** Side tunnels of 4 blocks at the 2nd and 6th block, left and right. */
	private static void minerDigsSideTunnels(GameTestHelper helper) {
		hill(helper, 6);
		Tunnel tunnel = tunnel(helper, START, 6).withBranches(4);
		Villager worker = miner(helper, tunnel, new ItemStack(Items.OAK_FENCE, 8), new ItemStack(Items.OAK_PLANKS, 8));
		helper.succeedWhen(() -> {
			assertDug(helper, worker, tunnel);
			helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, 1, 13));
			helper.assertBlockPresent(Blocks.AIR, new BlockPos(13, 0, 17));
		});
	}

	/** Every cell of the tunnel is empty or holds its support or torch. */
	private static void assertDug(GameTestHelper helper, Villager worker, Tunnel tunnel) {
		for (Tunnel.Section section : tunnel.sections(true)) {
			for (BlockPos cell : section.cells()) {
				Tunnel.Fixture fixture = section.fixtures().get(cell);
				BlockState state = helper.getLevel().getBlockState(cell);
				boolean ok = fixture == null ? state.isAir()
					: fixture == Tunnel.Fixture.FENCE ? state.is(BlockTags.WOODEN_FENCES)
					: fixture == Tunnel.Fixture.PLANKS ? state.is(BlockTags.PLANKS)
					: state.getBlock() instanceof BaseTorchBlock;
				helper.assertTrue(ok, "Section " + section.index() + " is not done at " + helper.relativePos(cell) + " (" + state + "): " + TestWorld.describe(worker));
			}
		}
	}

	/**
	 * Iron ore in the wall and one behind it are dug, raw iron goes to the chest; lava in the
	 * other wall is sealed with cobblestone before the miner digs next to it.
	 */
	private static void minerDigsOreAndSealsLava(GameTestHelper helper) {
		hill(helper, 5);
		BlockPos ore = new BlockPos(10, 1, 13);
		BlockPos behind = new BlockPos(11, 1, 13);
		BlockPos lava = new BlockPos(6, 0, 14);
		helper.setBlock(ore, Blocks.IRON_ORE);
		helper.setBlock(behind, Blocks.IRON_ORE);
		helper.setBlock(lava, Blocks.LAVA);
		Villager worker = miner(helper, tunnel(helper, START, 3), new ItemStack(Items.COBBLESTONE, 4));
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(Blocks.AIR, ore);
			helper.assertBlockPresent(Blocks.AIR, behind);
			helper.assertTrue(helper.getBlockState(lava).is(Blocks.COBBLESTONE), "The lava was not sealed: " + TestWorld.describe(worker));
			helper.assertBlockPresent(Blocks.AIR, new BlockPos(7, 0, 14));
			TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.RAW_IRON), 2, "raw iron in the output chest");
			helper.assertTrue(worker.isAlive() && !worker.isOnFire(), "The miner got burnt");
		});
	}

	/** Planks in the way (a build) block the tunnel; they stay and the miner complains. */
	private static void minerStopsAtPlanks(GameTestHelper helper) {
		hill(helper, 5);
		BlockPos planks = new BlockPos(8, 1, 13);
		helper.setBlock(planks, Blocks.OAK_PLANKS);
		Villager worker = miner(helper, tunnel(helper, START, 4));
		helper.failIfEver(() -> helper.assertBlockPresent(Blocks.OAK_PLANKS, planks));
		helper.runAfterDelay(600, () -> {
			helper.assertTrue(((HiredVillager) worker).workingvillagers$hasProblem(), "The blocked miner does not complain: " + TestWorld.describe(worker));
			helper.assertBlockPresent(Blocks.STONE, new BlockPos(8, 0, 14));
			helper.succeed();
		});
	}

	/** Stairs from a ledge down into the hill: 1 wide, 3 high, one lower each block. */
	private static void minerDigsStairs(GameTestHelper helper) {
		hill(helper, 8);
		// A ledge in front of the hill to stand on
		for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(5, 0, 9), new BlockPos(11, 4, 11))) {
			helper.setBlock(pos, Blocks.STONE);
		}
		BlockPos start = new BlockPos(8, 5, 12);
		Tunnel stairs = tunnel(helper, start, 4).withStairs(true);
		BlockPos bench = new BlockPos(6, 5, 10);
		helper.setBlock(bench, WorkingVillagers.MINING_BENCH.get());
		BlockPos output = new BlockPos(10, 5, 10);
		TestWorld.chest(helper, output);
		TestWorld.link(helper, bench, output, ChestRole.OUTPUT);
		TestWorld.station(helper, bench).setTunnel(stairs);
		Villager worker = TestWorld.hireVillager(helper, new BlockPos(8, 5, 9));
		TestWorld.assign(helper, worker, bench);
		TestWorld.give(helper, worker, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.DIAMOND_PICKAXE));
		helper.succeedWhen(() -> {
			for (int k = 0; k < 4; k++) {
				BlockPos base = start.south(k).below(k);
				for (int y = 0; y < 3; y++) {
					helper.assertBlockPresent(Blocks.AIR, base.above(y));
				}
				helper.assertTrue(!helper.getBlockState(base.below()).isAir(), "Step " + k + " has no floor");
			}
		});
	}

	/** The mill turns cobblestone into gravel and gravel into sand and flint. */
	private static void millGrindsCobblestone(GameTestHelper helper) {
		List<Block> fromCobblestone = CuttingRecipes.forInput(helper.getLevel(), WorkingVillagers.MILLING.get(), new ItemStack(Items.COBBLESTONE)).stream()
			.map(recipe -> Block.byItem(CuttingRecipes.resultOf(recipe.value())))
			.toList();
		helper.assertTrue(fromCobblestone.contains(Blocks.GRAVEL), "No mill recipe from cobblestone to gravel: " + fromCobblestone);
		List<Item> fromGravel = CuttingRecipes.forInput(helper.getLevel(), WorkingVillagers.MILLING.get(), new ItemStack(Items.GRAVEL)).stream()
			.map(recipe -> CuttingRecipes.resultOf(recipe.value()))
			.toList();
		helper.assertTrue(fromGravel.contains(Items.SAND) && fromGravel.contains(Items.FLINT), "No mill recipes from gravel to sand and flint: " + fromGravel);
		helper.succeed();
	}
}
