package me.teajay.workingvillagers.gametest.simulation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;

/**
 * What the simulation built: the workers, the customers and the chests the monitor looks
 * at. Saved next to the world, so the monitor goes on after a restart.
 */
public final class SimRegistry {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	record Worker(UUID uuid, String job, int wing, BlockPos station) {
	}

	record Trader(UUID uuid, String profession, int wing, BlockPos block) {
	}

	int wings;
	int ground;
	BlockPos center = BlockPos.ZERO;
	final List<Worker> workers = new ArrayList<>();
	final List<Trader> traders = new ArrayList<>();
	final List<BlockPos> storage = new ArrayList<>();
	final List<BlockPos> depots = new ArrayList<>();
	final List<BlockPos> tills = new ArrayList<>();
	final List<BlockPos> villageChests = new ArrayList<>();
	final List<BlockPos> relays = new ArrayList<>();
	/** The village chests the workers buy their food at. */
	final List<BlockPos> pantries = new ArrayList<>();

	/** Plain data for Gson: positions as arrays. */
	private record Data(int wings, int ground, long[] center, List<String[]> workers, List<String[]> traders, List<long[]> storage, List<long[]> depots,
		List<long[]> tills, List<long[]> villageChests, List<long[]> relays, List<long[]> pantries) {
	}

	void save(Path file) throws IOException {
		Data data = new Data(this.wings, this.ground, pos(this.center),
			this.workers.stream().map(w -> new String[] {w.uuid().toString(), w.job(), Integer.toString(w.wing()), Long.toString(w.station().asLong())}).toList(),
			this.traders.stream().map(t -> new String[] {t.uuid().toString(), t.profession(), Integer.toString(t.wing()), Long.toString(t.block().asLong())}).toList(),
			this.storage.stream().map(SimRegistry::pos).toList(), this.depots.stream().map(SimRegistry::pos).toList(),
			this.tills.stream().map(SimRegistry::pos).toList(), this.villageChests.stream().map(SimRegistry::pos).toList(),
			this.relays.stream().map(SimRegistry::pos).toList(), this.pantries.stream().map(SimRegistry::pos).toList());
		Files.createDirectories(file.getParent());
		Files.writeString(file, GSON.toJson(data));
	}

	static SimRegistry load(Path file) throws IOException {
		Data data = GSON.fromJson(Files.readString(file), Data.class);
		SimRegistry registry = new SimRegistry();
		registry.wings = data.wings();
		registry.ground = data.ground();
		registry.center = pos(data.center());
		for (String[] w : data.workers()) {
			registry.workers.add(new Worker(UUID.fromString(w[0]), w[1], Integer.parseInt(w[2]), BlockPos.of(Long.parseLong(w[3]))));
		}
		for (String[] t : data.traders()) {
			registry.traders.add(new Trader(UUID.fromString(t[0]), t[1], Integer.parseInt(t[2]), BlockPos.of(Long.parseLong(t[3]))));
		}
		data.storage().forEach(p -> registry.storage.add(pos(p)));
		data.depots().forEach(p -> registry.depots.add(pos(p)));
		data.tills().forEach(p -> registry.tills.add(pos(p)));
		data.villageChests().forEach(p -> registry.villageChests.add(pos(p)));
		data.relays().forEach(p -> registry.relays.add(pos(p)));
		if (data.pantries() != null) {
			data.pantries().forEach(p -> registry.pantries.add(pos(p)));
		}
		return registry;
	}

	private static long[] pos(BlockPos pos) {
		return new long[] {pos.getX(), pos.getY(), pos.getZ()};
	}

	private static BlockPos pos(long[] pos) {
		return new BlockPos((int) pos[0], (int) pos[1], (int) pos[2]);
	}
}
