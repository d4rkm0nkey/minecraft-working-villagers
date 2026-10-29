package me.teajay.workingvillagers.gametest;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.job.HiredVillager;
import me.teajay.workingvillagers.job.WorkerChunks;
import me.teajay.workingvillagers.job.WorkerJobs;
import me.teajay.workingvillagers.job.WorkerLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.TicketStorage;

/** Workers that work without a player nearby (see {@link WorkerChunks}). */
final class ChunkTests {
	private static final BlockPos WORKER = new BlockPos(6, 1, 6);
	private static final BlockPos STATION = new BlockPos(8, 1, 8);

	private ChunkTests() {
	}

	static void register(BiConsumer<String, Consumer<GameTestHelper>> registry) {
		registry.accept("chunks_go_with_the_worker", ChunkTests::chunksGoWithTheWorker);
		registry.accept("chunks_cost_a_fee", ChunkTests::chunksCostAFee);
		registry.accept("chunks_only_by_the_employer", ChunkTests::chunksOnlyByTheEmployer);
		registry.accept("fired_worker_loads_no_chunks", ChunkTests::firedWorkerLoadsNoChunks);
	}

	/** A lumberjack at its chopping block, hired by the player. */
	private static Villager lumberjack(GameTestHelper helper, Player employer) {
		helper.setBlock(STATION, WorkingVillagers.CHOPPING_BLOCK.get());
		Villager villager = TestWorld.hireVillager(helper, WORKER, employer);
		TestWorld.assign(helper, villager, STATION);
		TestWorld.give(helper, villager, new ItemStack(Items.EMERALD, 8));
		return villager;
	}

	private static void switchOn(GameTestHelper helper, Villager worker, Player employer) {
		Component message = WorkerChunks.toggle(helper.getLevel(), worker, employer);
		helper.assertTrue(message != null && WorkerChunks.isOn(helper.getLevel(), worker), "Not switched on: " + message);
	}

	/** Whether a ticket of a worker is at the chunk. */
	private static boolean hasTicket(ServerLevel level, ChunkPos chunk) {
		return level.getDataStorage().computeIfAbsent(TicketStorage.TYPE).getTickets(chunk.pack()).stream()
			.anyMatch(ticket -> ticket.getType() == WorkingVillagers.WORKER_TICKET.get());
	}

	/**
	 * Switched on, the worker carries a ticket at its chunk; its workplace gets tickets too, up
	 * to the edge of the lumberjack's trees.
	 */
	private static void chunksGoWithTheWorker(GameTestHelper helper) {
		Player employer = helper.makeMockPlayer(GameType.SURVIVAL);
		Villager worker = lumberjack(helper, employer);
		switchOn(helper, worker, employer);
		helper.succeedWhen(() -> {
			ServerLevel level = helper.getLevel();
			helper.assertTrue(hasTicket(level, worker.chunkPosition()), "No ticket at the worker's chunk " + worker.chunkPosition());
			BlockPos edge = helper.absolutePos(STATION).east(Config.TREE_SEARCH_RADIUS.get());
			helper.assertTrue(hasTicket(level, ChunkPos.containing(edge)), "No ticket at the edge of the trees " + ChunkPos.containing(edge));
			int chunks = WorkerChunks.chunkCount(level, worker);
			helper.assertTrue(chunks >= 9, "Only " + chunks + " chunks, not even the 3 × 3 around the worker");
		});
	}

	/** Working without a player costs the fee on top of the wage, and nothing once it is off again. */
	private static void chunksCostAFee(GameTestHelper helper) {
		Player employer = helper.makeMockPlayer(GameType.SURVIVAL);
		Villager worker = lumberjack(helper, employer);
		int wage = WorkerLevels.dailyWage(worker);
		switchOn(helper, worker, employer);
		helper.assertTrue(WorkerLevels.dailyWage(worker) == wage + Config.CHUNK_LOADING_FEE.get(),
			"Wage " + WorkerLevels.dailyWage(worker) + " instead of " + wage + " + " + Config.CHUNK_LOADING_FEE.get());
		WorkerChunks.toggle(helper.getLevel(), worker, employer);
		helper.assertTrue(!WorkerChunks.isOn(helper.getLevel(), worker), "Still on");
		helper.assertTrue(WorkerLevels.dailyWage(worker) == wage, "Still pays the fee: " + WorkerLevels.dailyWage(worker));
		helper.succeed();
	}

	/** Another player cannot switch it. */
	private static void chunksOnlyByTheEmployer(GameTestHelper helper) {
		Player employer = helper.makeMockPlayer(GameType.SURVIVAL);
		Villager worker = lumberjack(helper, employer);
		Player other = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.assertTrue(WorkerChunks.toggle(helper.getLevel(), worker, other) == null, "Another player switched it");
		helper.assertTrue(!WorkerChunks.isOn(helper.getLevel(), worker), "Switched on by another player");
		helper.succeed();
	}

	/** A worker that is no longer hired no longer works without a player. */
	private static void firedWorkerLoadsNoChunks(GameTestHelper helper) {
		Player employer = helper.makeMockPlayer(GameType.SURVIVAL);
		Villager worker = lumberjack(helper, employer);
		switchOn(helper, worker, employer);
		WorkerJobs.unassign(helper.getLevel(), worker);
		((HiredVillager) worker).workingvillagers$setEmployer(null);
		helper.succeedWhen(() -> helper.assertTrue(!WorkerChunks.isOn(helper.getLevel(), worker), "Still on after being fired"));
	}
}
