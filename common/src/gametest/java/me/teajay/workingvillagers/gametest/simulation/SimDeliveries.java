package me.teajay.workingvillagers.gametest.simulation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Counts what each worker puts into chests, by job, kind of chest and item. The test mod's
 * mixins report every item a villager stores like a hopper while it thinks.
 */
public final class SimDeliveries {
	private static SimRegistry registry;
	private static final Map<UUID, SimRegistry.Worker> WORKERS = new HashMap<>();
	private static Villager current;
	/** job -> "kind item" -> count, for the current day. */
	private static final Map<String, Map<String, Integer>> TODAY = new TreeMap<>();

	private SimDeliveries() {
	}

	static void watch(SimRegistry watched) {
		registry = watched;
		WORKERS.clear();
		watched.workers.forEach(worker -> WORKERS.put(worker.uuid(), worker));
		TODAY.clear();
	}

	public static void enter(Villager villager) {
		current = villager;
	}

	public static void exit() {
		current = null;
	}

	public static void stored(Container container, Item item, int count) {
		if (current == null || registry == null || !(container instanceof BlockEntity chest) || !(current.level() instanceof ServerLevel level)) {
			return;
		}
		SimRegistry.Worker worker = WORKERS.get(current.getUUID());
		if (worker == null) {
			return;
		}
		String job = worker.job().equals("carrier") ? "Carrier" : worker.job();
		String kind = kind(level, worker, chest.getBlockPos());
		TODAY.computeIfAbsent(job, j -> new TreeMap<>())
			.merge(kind + " " + BuiltInRegistries.ITEM.getKey(item).getPath(), count, Integer::sum);
	}

	/** Which of the worker's chests it is: its output, its input (things it brings back), a village chest, or another. */
	private static String kind(ServerLevel level, SimRegistry.Worker worker, BlockPos chest) {
		if (registry.villageChests.contains(chest)) {
			return "village_chest";
		}
		if (worker.job().equals("carrier")) {
			return "carried";
		}
		Workstations.Station station = Workstations.find(level, worker.station()).orElse(null);
		if (station == null) {
			return "other";
		}
		if (contains(level, station.validOutputs(level), chest)) {
			return "output";
		}
		if (contains(level, station.validInputs(level), chest)) {
			return "input";
		}
		return "other";
	}

	private static boolean contains(ServerLevel level, List<BlockPos> chests, BlockPos chest) {
		if (chests.contains(chest)) {
			return true;
		}
		BlockPos other = Workstations.otherHalf(level, chest);
		return other != null && chests.contains(other);
	}

	/** What was delivered since the last call, and starts counting anew. */
	static Map<String, Map<String, Integer>> takeDay() {
		Map<String, Map<String, Integer>> day = new TreeMap<>();
		TODAY.forEach((job, items) -> day.put(job, new TreeMap<>(items)));
		TODAY.clear();
		return day;
	}
}
