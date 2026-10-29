package me.teajay.workingvillagers.job;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.carrier.CarrierData;
import me.teajay.workingvillagers.carrier.Carriers;
import me.teajay.workingvillagers.carrier.Route;
import me.teajay.workingvillagers.station.Pit;
import me.teajay.workingvillagers.station.StationType;
import me.teajay.workingvillagers.station.Tunnel;
import me.teajay.workingvillagers.station.Voyage;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Workers that work without a player nearby: their employer switched it on in the menu of the
 * workstation (a carrier: in its own menu), and the chunks they need stay loaded, like those of
 * a flying ender pearl.
 *
 * <p>The worker carries a chunk ticket with it ({@link #TICKET}, timing out after
 * {@link #TICKET_TIMEOUT} ticks): every {@link WorkerJobs} check renews it at the chunk the
 * worker is in, with {@link #WORKER_RADIUS}, so the 3 × 3 chunks around it are ticked. Wherever
 * it goes (to bed, to a village chest, along a tunnel) the chunks go with it, and when it dies
 * or is fired the ticket times out by itself. With {@link Config#CHUNK_LOADING_WORKPLACE} the
 * workplace gets tickets of its own the same way ({@link #workplace}), so a furnace burns and
 * saplings grow while the worker sleeps.
 *
 * <p>Vanilla does not keep the tickets over a restart, these entries do: the worker, its
 * employer and the chunk it was last in. A worker that has not renewed its ticket for a while
 * (after a restart, after its employer came back online) is loaded again at that chunk by
 * {@link #tick}; once it is loaded it renews the ticket itself. A worker that does not show up
 * within {@link #RESTORE_TICKS} is gone (killed while nobody looked, moved by a command) and
 * forgotten.
 *
 * <p>Random ticks (saplings, crops) run in every ticked chunk, players or not; mobs only spawn
 * near players, so none spawn around a worker on its own.
 *
 * <p>It costs {@link Config#CHUNK_LOADING_FEE} emeralds per work day on top of the wage (see
 * {@link WorkerLevels#dailyWage(Villager)}). A worker that lacks its wage for more than a day
 * stops, and its employer is told.
 */
public final class WorkerChunks extends SavedData {
	private static final Logger LOGGER = LogUtils.getLogger();
	/** How long a ticket of a worker lasts without being renewed. */
	public static final long TICKET_TIMEOUT = 100L;
	/** The ticket around the worker: it and the chunks next to it are ticked (3 × 3). */
	private static final int WORKER_RADIUS = 3;
	/** A ticket of the workplace: only that chunk is ticked. */
	private static final int WORKPLACE_RADIUS = 2;
	/** A worker that has not renewed its ticket for this long is loaded again by {@link #tick}. */
	private static final long UNSEEN_TICKS = 60L;
	/** How long a worker that is loaded again has to show up before it is forgotten. */
	private static final long RESTORE_TICKS = 1200L;
	/** How often {@link #tick} looks for workers to load again. */
	private static final int TICK_INTERVAL = 20;
	/** The most chunks of a workplace that are kept loaded, besides the worker's own. */
	private static final int MAX_WORKPLACE_CHUNKS = 25;

	/** Loads, ticks and keeps the dimension active (like an ender pearl's), not saved by vanilla. */
	public static TicketType newTicketType() {
		return new TicketType(TICKET_TIMEOUT, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
	}

	/** See {@link WorkingVillagers#WORKER_TICKET}. */
	private static TicketType ticket() {
		return WorkingVillagers.WORKER_TICKET.get();
	}

	private static final Codec<WorkerChunks> CODEC = Entry.CODEC.listOf()
		.fieldOf("workers")
		.codec()
		.xmap(WorkerChunks::new, data -> List.copyOf(data.entries.values()));
	// No data fixes apply to this data; command storage is the closest type that has none
	public static final SavedDataType<WorkerChunks> TYPE = new SavedDataType<>(
		WorkingVillagers.id("worker_chunks"), WorkerChunks::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	private final Map<UUID, Entry> entries = new HashMap<>();
	/** When each worker last renewed its ticket (game time of its level). Not saved. */
	private final Map<UUID, Long> lastSeen = new HashMap<>();
	/** Since when each worker that did not renew its ticket is being loaded again. Not saved. */
	private final Map<UUID, Long> restoring = new HashMap<>();
	/** The chunks of each worker's workplace, as last renewed. Not saved. */
	private final Map<UUID, LongSet> workplaces = new HashMap<>();

	private WorkerChunks() {
	}

	private WorkerChunks(List<Entry> entries) {
		for (Entry entry : entries) {
			this.entries.put(entry.worker, entry);
		}
	}

	/**
	 * A worker that works without a player nearby: who employs it, where it was last and since
	 * which day it lacks its wage (-1: it does not). The name is for the list of the command,
	 * while the worker is not loaded.
	 */
	public static final class Entry {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.fieldOf("worker").forGetter(entry -> entry.worker),
			UUIDUtil.CODEC.fieldOf("employer").forGetter(entry -> entry.employer),
			Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(entry -> entry.dimension),
			Codec.LONG.fieldOf("chunk").forGetter(entry -> entry.chunk),
			Codec.STRING.optionalFieldOf("name", "").forGetter(entry -> entry.name),
			Codec.LONG.optionalFieldOf("unpaid_since", -1L).forGetter(entry -> entry.unpaidSince)
		).apply(i, Entry::new));

		private final UUID worker;
		private final UUID employer;
		private ResourceKey<Level> dimension;
		private long chunk;
		private String name;
		private long unpaidSince;

		private Entry(UUID worker, UUID employer, ResourceKey<Level> dimension, long chunk, String name, long unpaidSince) {
			this.worker = worker;
			this.employer = employer;
			this.dimension = dimension;
			this.chunk = chunk;
			this.name = name;
			this.unpaidSince = unpaidSince;
		}

		public UUID worker() {
			return this.worker;
		}

		public UUID employer() {
			return this.employer;
		}

		public ResourceKey<Level> dimension() {
			return this.dimension;
		}

		public ChunkPos chunk() {
			return ChunkPos.unpack(this.chunk);
		}

		public String name() {
			return this.name;
		}
	}

	/** Why the chunks of a worker that works without a player nearby are loaded or not. */
	public enum State {
		/** The worker works without a player nearby. */
		ACTIVE("active"),
		/** Waits for its employer to come online ({@link Config#CHUNK_LOADING_EMPLOYER_ONLINE}). */
		EMPLOYER_OFFLINE("employer_offline"),
		/** The server switched it off ({@link Config#CHUNK_LOADING}). */
		DISABLED("disabled");

		private final String key;

		State(String key) {
			this.key = key;
		}

		public Component describe() {
			return Component.translatable("gui.workingvillagers.chunks.state." + this.key);
		}
	}

	/** The data of the server; it is kept with the overworld, for the workers of every dimension. */
	private static WorkerChunks get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	/** Whether the employer switched it on for the worker (whether or not the chunks are loaded right now). */
	public static boolean isOn(ServerLevel level, Villager villager) {
		return get(level.getServer()).entries.containsKey(villager.getUUID());
	}

	/** The emeralds per work day the worker takes on top of its wage for working without a player nearby. */
	public static int fee(Villager villager) {
		if (!(villager.level() instanceof ServerLevel level) || !Config.CHUNK_LOADING.get()) {
			return 0;
		}
		return isOn(level, villager) ? Config.CHUNK_LOADING_FEE.get() : 0;
	}

	/** Whether the player may switch it on or off for the worker: its employer and operators (and the owner of a singleplayer world). */
	public static boolean canSwitch(Villager villager, Player player) {
		HiredVillager hired = (HiredVillager) villager;
		return Config.CHUNK_LOADING.get() && hired.workingvillagers$isHired()
			&& (player.getUUID().equals(hired.workingvillagers$getEmployer()) || Carriers.isAdmin(player));
	}

	/**
	 * Switches working without a player nearby on or off for the worker; returns what to tell
	 * the player, or null if the player may not.
	 */
	public static @Nullable Component toggle(ServerLevel level, Villager villager, Player player) {
		if (!canSwitch(villager, player)) {
			return null;
		}
		WorkerChunks data = get(level.getServer());
		UUID employer = ((HiredVillager) villager).workingvillagers$getEmployer();
		if (data.entries.containsKey(villager.getUUID())) {
			data.forget(villager.getUUID());
			return Component.translatable("message.workingvillagers.chunks.off", villager.getName());
		}
		int max = Config.CHUNK_LOADING_MAX_WORKERS.get();
		if (data.entries.values().stream().filter(entry -> entry.employer.equals(employer)).count() >= max) {
			return Component.translatable("message.workingvillagers.chunks.limit", max);
		}
		data.entries.put(villager.getUUID(), new Entry(villager.getUUID(), employer, level.dimension(), villager.chunkPosition().pack(),
			villager.getName().getString(), -1L));
		data.setDirty();
		keep(level, villager);
		return Component.translatable("message.workingvillagers.chunks.on", villager.getName(), Config.CHUNK_LOADING_FEE.get());
	}

	/** Whether the chunks of the worker are loaded and if not, why; empty if it is not switched on for it. */
	public static Optional<State> state(ServerLevel level, Villager villager) {
		WorkerChunks data = get(level.getServer());
		Entry entry = data.entries.get(villager.getUUID());
		return entry == null ? Optional.empty() : Optional.of(state(level.getServer(), entry));
	}

	private static State state(MinecraftServer server, Entry entry) {
		if (!Config.CHUNK_LOADING.get()) {
			return State.DISABLED;
		}
		if (Config.CHUNK_LOADING_EMPLOYER_ONLINE.get() && server.getPlayerList().getPlayer(entry.employer) == null) {
			return State.EMPLOYER_OFFLINE;
		}
		return State.ACTIVE;
	}

	/**
	 * How many chunks the worker keeps ticking right now: the ones around it and those of its
	 * workplace. 0 while its chunks are not loaded for it.
	 */
	public static int chunkCount(ServerLevel level, Villager villager) {
		WorkerChunks data = get(level.getServer());
		Entry entry = data.entries.get(villager.getUUID());
		return entry == null || state(level.getServer(), entry) != State.ACTIVE ? 0 : data.ticked(entry).size();
	}

	/** The chunks ticked for the worker: the 3 × 3 around it and those of its workplace. */
	private LongSet ticked(Entry entry) {
		LongSet chunks = new LongOpenHashSet();
		ChunkPos center = entry.chunk();
		ChunkPos.rangeClosed(center, WORKER_RADIUS - 2).forEach(pos -> chunks.add(pos.pack()));
		chunks.addAll(this.workplaces.getOrDefault(entry.worker, LongSet.of()));
		return chunks;
	}

	/**
	 * Every {@link WorkerJobs} check of a hired villager: renews the tickets of a worker that
	 * works without a player nearby and notes where it is. A villager that is no longer hired,
	 * or hired by somebody else, is forgotten.
	 */
	public static void keep(ServerLevel level, Villager villager) {
		WorkerChunks data = get(level.getServer());
		Entry entry = data.entries.get(villager.getUUID());
		if (entry == null) {
			return;
		}
		HiredVillager hired = (HiredVillager) villager;
		if (!entry.employer.equals(hired.workingvillagers$getEmployer())) {
			data.forget(entry.worker);
			return;
		}
		long chunk = villager.chunkPosition().pack();
		String name = villager.getName().getString();
		if (entry.chunk != chunk || entry.dimension != level.dimension() || !entry.name.equals(name)) {
			entry.chunk = chunk;
			entry.dimension = level.dimension();
			entry.name = name;
			data.setDirty();
		}
		data.lastSeen.put(entry.worker, level.getGameTime());
		data.restoring.remove(entry.worker);
		if (data.unpaid(level, villager, entry)) {
			return;
		}
		if (state(level.getServer(), entry) != State.ACTIVE) {
			data.workplaces.remove(entry.worker);
			return;
		}
		level.getChunkSource().addTicketWithRadius(ticket(), villager.chunkPosition(), WORKER_RADIUS);
		LongSet workplace = Config.CHUNK_LOADING_WORKPLACE.get() ? workplace(level, villager) : LongSet.of();
		for (long pos : workplace) {
			level.getChunkSource().addTicketWithRadius(ticket(), ChunkPos.unpack(pos), WORKPLACE_RADIUS);
		}
		data.workplaces.put(entry.worker, workplace);
	}

	/**
	 * Notes when the worker lacks its wage; after a whole day without it, it stops working
	 * without a player nearby and its employer is told. Returns true then.
	 */
	private boolean unpaid(ServerLevel level, Villager villager, Entry entry) {
		if (!WorkerSupplies.lacksWage(level, villager)) {
			if (entry.unpaidSince >= 0) {
				entry.unpaidSince = -1;
				this.setDirty();
			}
			return false;
		}
		long today = ChopTrees.today(level);
		if (entry.unpaidSince < 0) {
			entry.unpaidSince = today;
			this.setDirty();
			return false;
		}
		if (today - entry.unpaidSince < 1) {
			return false;
		}
		this.forget(entry.worker);
		ServerPlayer employer = level.getServer().getPlayerList().getPlayer(entry.employer);
		if (employer != null) {
			employer.sendSystemMessage(Component.translatable("message.workingvillagers.chunks.unpaid", villager.getName()));
		}
		return true;
	}

	/**
	 * The chunks of the worker's workplace: its workstation with the area it works in (the
	 * trees of a lumberjack, the fields, the pasture, the pit, the tunnel, a hauler's other
	 * jetty) and the linked chests; for a carrier the containers of its routes. At most
	 * {@link #MAX_WORKPLACE_CHUNKS}, the closest to the workstation first.
	 */
	static LongSet workplace(ServerLevel level, Villager villager) {
		List<BlockPos> places = new ArrayList<>();
		BlockPos center;
		CarrierData carrier = ((HiredVillager) villager).workingvillagers$getCarrier();
		if (Carriers.isCarrier(villager) && carrier != null) {
			for (Route route : carrier.routes()) {
				places.add(route.source());
				places.add(route.target());
			}
			center = places.isEmpty() ? villager.blockPosition() : places.getFirst();
		} else {
			Optional<GlobalPos> site = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
				.filter(jobSite -> jobSite.dimension() == level.dimension());
			if (site.isEmpty()) {
				return LongSet.of();
			}
			center = site.get().pos();
			places.add(center);
			Optional<Workstations.Station> station = Workstations.find(level, center);
			if (station.isPresent()) {
				places.addAll(station.get().validInputs(level));
				places.addAll(station.get().validOutputs(level));
				// The pit digger only digs while the whole ring is loaded, the miner while the tunnel is
				Pit pit = station.get().pit();
				if (pit != null) {
					places.addAll(pit.ring());
					places.addAll(pit.inside());
				}
				// The hauler's other jetty, where it unloads and loads; the water between it carries along itself
				Voyage voyage = station.get().voyage();
				if (voyage != null) {
					places.add(voyage.destination());
					places.add(voyage.end());
					Workstations.find(level, voyage.destination()).ifPresent(other -> {
						places.addAll(other.validInputs(level));
						places.addAll(other.validOutputs(level));
					});
				}
				Tunnel tunnel = station.get().tunnel();
				if (tunnel != null) {
					places.add(tunnel.start());
					for (Tunnel.Section section : tunnel.sections(false)) {
						places.add(section.cells().getLast());
					}
				}
			}
			int radius = level.isLoaded(center) ? areaRadius(StationType.of(level.getBlockState(center))) : 0;
			for (int dx = -radius; dx <= radius; dx += 16) {
				for (int dz = -radius; dz <= radius; dz += 16) {
					places.add(center.offset(dx, 0, dz));
				}
			}
			// The corners of the area, a radius that is not a multiple of 16 reaches into them
			places.add(center.offset(radius, 0, radius));
			places.add(center.offset(-radius, 0, radius));
			places.add(center.offset(radius, 0, -radius));
			places.add(center.offset(-radius, 0, -radius));
		}
		ChunkPos home = ChunkPos.containing(center);
		LongSet chunks = new LongOpenHashSet();
		places.stream()
			.map(ChunkPos::containing)
			.distinct()
			.sorted(Comparator.comparingInt(pos -> Math.max(Math.abs(pos.x() - home.x()), Math.abs(pos.z() - home.z()))))
			.limit(MAX_WORKPLACE_CHUNKS)
			.forEach(pos -> chunks.add(pos.pack()));
		return chunks;
	}

	/** How far from its workstation (in blocks) a worker of the station works outdoors. */
	private static int areaRadius(@Nullable StationType type) {
		if (type == null) {
			return 0;
		}
		return switch (type) {
			case CHOPPING_BLOCK -> Config.TREE_SEARCH_RADIUS.get();
			case COMPOSTER -> Config.FIELD_RADIUS.get();
			case LOOM -> Config.PASTURE_RADIUS.get();
			case SMOKER -> Config.BUTCHER_RADIUS.get();
			default -> 0;
		};
	}

	/**
	 * Every tick of a level: loads the workers of the level again that work without a player
	 * nearby but have not renewed their tickets (after a restart, when their employer came
	 * online), at the chunk they were last in.
	 */
	public static void tick(ServerLevel level) {
		if (level.getGameTime() % TICK_INTERVAL != 0) {
			return;
		}
		WorkerChunks data = get(level.getServer());
		if (data.entries.isEmpty()) {
			return;
		}
		long now = level.getGameTime();
		for (Entry entry : List.copyOf(data.entries.values())) {
			if (entry.dimension != level.dimension()) {
				continue;
			}
			if (state(level.getServer(), entry) != State.ACTIVE) {
				data.restoring.remove(entry.worker);
				continue;
			}
			Long seen = data.lastSeen.get(entry.worker);
			if (seen != null && now - seen <= UNSEEN_TICKS) {
				continue;
			}
			long since = data.restoring.computeIfAbsent(entry.worker, worker -> now);
			if (now - since > RESTORE_TICKS) {
				LOGGER.info("Worker {} ({}) did not show up at chunk {} in {}, it no longer works without a player nearby",
					entry.name, entry.worker, entry.chunk(), entry.dimension.identifier());
				data.forget(entry.worker);
				continue;
			}
			level.getChunkSource().addTicketWithRadius(ticket(), entry.chunk(), WORKER_RADIUS);
		}
	}

	/** The villager died or is no longer hired: it no longer works without a player nearby. */
	public static void forget(ServerLevel level, Villager villager) {
		WorkerChunks data = get(level.getServer());
		if (data.entries.containsKey(villager.getUUID())) {
			data.forget(villager.getUUID());
		}
	}

	private void forget(UUID worker) {
		this.entries.remove(worker);
		this.lastSeen.remove(worker);
		this.restoring.remove(worker);
		this.workplaces.remove(worker);
		this.setDirty();
	}

	/** What the command lists about one worker. */
	public record Listed(Entry entry, State state, int chunks) {
	}

	/** Every worker that works without a player nearby, by employer and name. */
	public static List<Listed> list(MinecraftServer server) {
		WorkerChunks data = get(server);
		return data.entries.values().stream()
			.sorted(Comparator.comparing((Entry entry) -> entry.employer).thenComparing(entry -> entry.name))
			.map(entry -> {
				State state = state(server, entry);
				return new Listed(entry, state, state == State.ACTIVE ? data.ticked(entry).size() : 0);
			})
			.toList();
	}

	/** Switches it off for the worker, loaded or not; false if it was not on. */
	public static boolean switchOff(MinecraftServer server, UUID worker) {
		WorkerChunks data = get(server);
		if (!data.entries.containsKey(worker)) {
			return false;
		}
		data.forget(worker);
		return true;
	}
}
