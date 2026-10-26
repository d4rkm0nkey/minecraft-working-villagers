package me.teajay.workingvillagers.station;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.crafting.WoodTypes;
import me.teajay.workingvillagers.job.WorkerLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * The settings of all workstations of a dimension: linked chests, the order and the
 * worker of the mod that works there, the pit of a pit stake, and the emeralds in the
 * station's wage box (see {@link Station#wage}).
 *
 * <p>They are kept here instead of in block entities because the vanilla stonecutter is a
 * workstation as well. Entries of stations that are gone are dropped when looked up.
 */
public class Workstations extends SavedData {
	/** The most chests a station can have, the screens have room for that many. See {@link Config#MAX_CHESTS}. */
	public static final int MAX_CHESTS = 8;
	/** The most products one order can have. */
	public static final int MAX_ORDERS = 8;
	/** The most of one product a player can order or keep in stock. */
	public static final int MAX_BATCH = 1024;

	private static final Codec<Workstations> CODEC = Station.CODEC.listOf()
		.fieldOf("stations")
		.codec()
		.xmap(Workstations::new, Workstations::pack);
	// No data fixes apply to this data; command storage is the closest type that has none
	public static final SavedDataType<Workstations> TYPE = new SavedDataType<>(
		WorkingVillagers.id("workstations"), Workstations::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	private final Map<BlockPos, Station> stations = new HashMap<>();

	private Workstations() {
	}

	private Workstations(List<Station> stations) {
		for (Station station : stations) {
			station.owner = this;
			this.stations.put(station.pos, station);
		}
	}

	private List<Station> pack() {
		return this.stations.values().stream().filter(station -> !station.isEmpty()).toList();
	}

	/**
	 * The settings of the workstation at the given position, if there is one.
	 */
	public static Optional<Station> find(ServerLevel level, BlockPos pos) {
		Workstations workstations = level.getDataStorage().computeIfAbsent(TYPE);
		StationType type = level.isLoaded(pos) ? StationType.of(level.getBlockState(pos)) : null;
		if (type == null) {
			if (level.isLoaded(pos) && workstations.stations.remove(pos) != null) {
				workstations.setDirty();
			}
			return Optional.empty();
		}
		Station station = workstations.stations.get(pos);
		// The block was replaced by another kind of station before the old entry was dropped
		if (station != null && station.type != null && station.type != type) {
			station = null;
			workstations.setDirty();
		}
		if (station == null) {
			station = new Station(
				pos.immutable(), Optional.empty(), List.of(), List.of(), List.of(), Optional.empty(), Optional.empty(), List.of(), 0, false,
				Optional.empty(), Pit.DEFAULT_DEPTH, Optional.empty(), 0, Station.Settings.DEFAULT
			);
			workstations.stations.put(station.pos, station);
		}
		station.owner = workstations;
		if (station.type != type) {
			station.type = type;
			station.setDirty();
		}
		return Optional.of(station);
	}

	/**
	 * Like {@link #find}, but only if the station has been set up (chests, order or worker).
	 */
	public static Optional<Station> findSetUp(ServerLevel level, BlockPos pos) {
		Workstations workstations = level.getDataStorage().computeIfAbsent(TYPE);
		Station station = workstations.stations.get(pos);
		return station == null || station.isEmpty() ? Optional.empty() : find(level, pos).filter(found -> !found.isEmpty());
	}

	/**
	 * Called whenever a block changes (see {@code ServerLevelMixin}): if a workstation is gone
	 * or became another kind, the emeralds of its wage box drop where it stood, and at a
	 * cauldron what was tanning in it (rotten flesh, or the leather if it is done).
	 */
	public static void onBlockChanged(ServerLevel level, BlockPos pos, BlockState before, BlockState after) {
		StationType old = StationType.of(before);
		if (old == null || old == StationType.of(after)) {
			return;
		}
		Workstations workstations = level.getDataStorage().computeIfAbsent(TYPE);
		Station station = workstations.stations.get(pos);
		if (station == null) {
			return;
		}
		if (station.tanning > 0) {
			Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), station.tanningContents(level.getGameTime()));
			station.setTanning(0, 0L);
		}
		if (station.wage == 0) {
			return;
		}
		int wage = station.wage;
		station.wage = 0;
		workstations.setDirty();
		Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.EMERALD, wage));
	}

	/**
	 * A right-click on a workstation with emeralds in the hand puts them into its wage box.
	 * Returns false if the block is no workstation or the hand holds no emeralds, so the
	 * click does what it does otherwise.
	 */
	public static boolean depositWage(ServerPlayer player, BlockPos pos, ItemStack stack) {
		ServerLevel level = player.level();
		if (!stack.is(Items.EMERALD) || StationType.of(level.getBlockState(pos)) == null) {
			return false;
		}
		Optional<Station> found = find(level, pos);
		if (found.isEmpty()) {
			return false;
		}
		Station station = found.get();
		int added = station.addWage(stack.getCount());
		if (added == 0) {
			player.sendOverlayMessage(Component.translatable("message.workingvillagers.wage.full", MAX_WAGE));
			return true;
		}
		if (!player.hasInfiniteMaterials()) {
			stack.shrink(added);
		}
		level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4F, 1.4F);
		int daily = station.worker() != null && level.getEntity(station.worker()) instanceof Villager worker
			? WorkerLevels.dailyWage(worker) : WorkerLevels.dailyWage(WorkerLevels.NOVICE);
		player.sendOverlayMessage(daily > 0
			? Component.translatable("message.workingvillagers.wage.deposited", station.wage(), station.wage() / daily)
			: Component.translatable("message.workingvillagers.wage.deposited_free", station.wage()));
		return true;
	}

	public static @Nullable Container getContainer(Level level, BlockPos pos) {
		return level.isLoaded(pos) ? HopperBlockEntity.getContainerAt(level, pos) : null;
	}

	/** The other half of a double chest, null for any other container. */
	public static @Nullable BlockPos otherHalf(Level level, BlockPos pos) {
		if (!level.isLoaded(pos)) {
			return null;
		}
		BlockState state = level.getBlockState(pos);
		return state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
			? ChestBlock.getConnectedBlockPos(pos, state)
			: null;
	}

	public enum ChestRole {
		/** The worker takes its material from here. */
		INPUT,
		/** The worker delivers its products here. */
		OUTPUT
	}

	/** The most emeralds the wage box of a station holds: one stack. */
	public static final int MAX_WAGE = 64;
	/** Loom: how many sheep the shepherd keeps around it unless set otherwise, and at most. */
	public static final int DEFAULT_HERD = 8;
	/** Cauldron: the most rotten flesh that tans at once. */
	public static final int MAX_TANNING = 64;
	/** Grindstone: the modes of {@link Station#grindMode}. */
	public static final int GRIND_BOTH = 0;
	public static final int GRIND_MERGE = 1;
	public static final int GRIND_DISENCHANT = 2;
	public static final int GRIND_NONE = 3;
	public static final int GRIND_MODES = 4;
	/** Lectern: the highest level of an enchanting table (15 bookshelves). */
	public static final int MAX_ENCHANT_LEVEL = 30;
	public static final int MAX_HERD = 32;
	public static final int MIN_GUARD_RADIUS = 8;
	public static final int MAX_GUARD_RADIUS = 32;

	public static final class Station {
		/** The products of the order; stations saved before 0.5.0 have one product, not a list. */
		private static final Codec<List<Item>> ORDERS_CODEC = Codec.withAlternative(
			BuiltInRegistries.ITEM.byNameCodec().listOf(),
			BuiltInRegistries.ITEM.byNameCodec().xmap(List::of, List::getFirst)
		);

		private static final Codec<Station> CODEC = RecordCodecBuilder.create(i -> i.group(
			BlockPos.CODEC.fieldOf("pos").forGetter(station -> station.pos),
			Codec.STRING.optionalFieldOf("type").forGetter(station -> Optional.ofNullable(station.type).map(Enum::name)),
			BlockPos.CODEC.listOf().optionalFieldOf("inputs", List.of()).forGetter(station -> station.inputs),
			BlockPos.CODEC.listOf().optionalFieldOf("outputs", List.of()).forGetter(station -> station.outputs),
			ORDERS_CODEC.optionalFieldOf("order", List.of()).forGetter(station -> List.copyOf(station.orders)),
			UUIDUtil.CODEC.optionalFieldOf("worker").forGetter(station -> Optional.ofNullable(station.worker)),
			Codec.STRING.optionalFieldOf("worker_name").forGetter(station -> Optional.ofNullable(station.workerName)),
			Codec.STRING.listOf().optionalFieldOf("woods", List.of()).forGetter(station -> List.copyOf(station.woods)),
			Codec.INT.optionalFieldOf("stock", 0).forGetter(station -> station.stock),
			Codec.BOOL.optionalFieldOf("strip", false).forGetter(station -> station.strip),
			Pit.CODEC.optionalFieldOf("pit").forGetter(station -> Optional.ofNullable(station.pit)),
			Codec.INT.optionalFieldOf("depth", Pit.DEFAULT_DEPTH).forGetter(station -> station.depth),
			Tunnel.CODEC.optionalFieldOf("tunnel").forGetter(station -> Optional.ofNullable(station.tunnel)),
			Codec.intRange(0, MAX_WAGE).optionalFieldOf("wage", 0).forGetter(station -> station.wage),
			Settings.MAP_CODEC.forGetter(Station::settings)
		).apply(i, Station::new));

		/**
		 * The settings of a few kinds of station, in the same map as the other fields (the
		 * codec of a record takes at most 16 fields).
		 */
		private record Settings(
			int herd, boolean boat, Optional<DyeColor> dye, int tanning, long tannedAt, int grind, int enchantLevel, Map<Item, Integer> made,
			Map<Item, Integer> stocks, Map<Item, Batch> batches
		) {
			static final Settings DEFAULT = new Settings(DEFAULT_HERD, true, Optional.empty(), 0, 0L, GRIND_BOTH, 0, Map.of(), Map.of(), Map.of());
			static final MapCodec<Settings> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.intRange(0, MAX_HERD).optionalFieldOf("herd", DEFAULT_HERD).forGetter(Settings::herd),
				Codec.BOOL.optionalFieldOf("boat", true).forGetter(Settings::boat),
				DyeColor.CODEC.optionalFieldOf("dye").forGetter(Settings::dye),
				Codec.intRange(0, MAX_TANNING).optionalFieldOf("tanning", 0).forGetter(Settings::tanning),
				Codec.LONG.optionalFieldOf("tanned_at", 0L).forGetter(Settings::tannedAt),
				Codec.intRange(0, GRIND_MODES - 1).optionalFieldOf("grind", GRIND_BOTH).forGetter(Settings::grind),
				Codec.intRange(0, MAX_ENCHANT_LEVEL).optionalFieldOf("enchant_level", 0).forGetter(Settings::enchantLevel),
				Codec.unboundedMap(BuiltInRegistries.ITEM.byNameCodec(), Codec.INT).optionalFieldOf("made", Map.of()).forGetter(Settings::made),
				Codec.unboundedMap(BuiltInRegistries.ITEM.byNameCodec(), Codec.INT).optionalFieldOf("stocks", Map.of()).forGetter(Settings::stocks),
				Codec.unboundedMap(BuiltInRegistries.ITEM.byNameCodec(), Batch.CODEC).optionalFieldOf("batches", Map.of()).forGetter(Settings::batches)
			).apply(i, Settings::new));
		}

		/**
		 * A product ordered once: how many the player ordered and how many the worker still has
		 * to make. At 0 the order is done; it is taken out once the worker handed everything in
		 * (see {@link #removeFinished}), or only marked as handed in where that would leave the
		 * station without an order.
		 */
		public record Batch(int amount, int left, boolean handedIn) {
			static final Codec<Batch> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.intRange(1, MAX_BATCH).fieldOf("amount").forGetter(Batch::amount),
				Codec.intRange(0, MAX_BATCH).fieldOf("left").forGetter(Batch::left),
				Codec.BOOL.optionalFieldOf("handed_in", false).forGetter(Batch::handedIn)
			).apply(i, Batch::new));

			public boolean done() {
				return this.left <= 0;
			}

			/** How many the worker made of it so far. */
			public int made() {
				return this.amount - this.left;
			}
		}

		private final BlockPos pos;
		private final List<BlockPos> inputs;
		private final List<BlockPos> outputs;
		/**
		 * The products the worker makes, in the order they were picked; the worker makes them
		 * in equal parts (see {@link #ordersByNeed}).
		 */
		private final List<Item> orders;
		/**
		 * How many of each product of the order the worker made since it was ordered, for
		 * equal parts without a stock. Only the differences matter, see {@link #addMade}.
		 */
		private final Map<Item, Integer> made;
		private @Nullable UUID worker;
		private @Nullable String workerName;
		/**
		 * Woodcutter and carpenter's bench: the wood types the order may be made of. At the
		 * bench they also limit the material of products made of any wood (chests, ...).
		 */
		private final Set<String> woods;
		/**
		 * How many of a product of the order to keep in the output chests unless set for the
		 * product itself ({@link #stocks}), 0 to make it as long as there is material. Stations
		 * saved before there were stocks per product only have this one.
		 */
		private int stock;
		/** The stock of single products of the order, see {@link #stock(Item)}. */
		private final Map<Item, Integer> stocks;
		/** The products of the order that are ordered once instead of kept in stock, see {@link Batch}. */
		private final Map<Item, Batch> batches;
		/** Chopping block: whether the lumberjack strips the logs before handing them in. */
		private boolean strip;
		/** Pit stake: the pit to dig, null until one is picked. */
		private @Nullable Pit pit;
		/** Pit stake: how deep to dig, below the highest string of the ring. */
		private int depth;
		/** Mining bench: the tunnel to dig, null until one is picked. */
		private @Nullable Tunnel tunnel;
		/** The emeralds in the wage box, see {@link #wage}. */
		private int wage;
		/** Loom: up to how many sheep the shepherd breeds, 0 for none. */
		private int herd;
		/** Barrel: whether the fisher rows out in a boat. */
		private boolean boat;
		/** Cauldron: the color the tanner dyes leather armor, null for none. */
		private @Nullable DyeColor dye;
		/** Cauldron: how much rotten flesh the tanner has put in to tan, and when it is done (game time). */
		private int tanning;
		private long tannedAt;
		/** Grindstone: what the weaponsmith does besides its order, see {@link #grindMode}. */
		private int grind;
		/** Lectern: the level the librarian enchants at, 0 for the highest the bookshelves allow. */
		private int enchantLevel;
		private @Nullable Workstations owner;
		/** The kind of station the settings belong to, unknown for entries saved before it was stored. */
		private @Nullable StationType type;

		private Station(
			BlockPos pos, Optional<String> type, List<BlockPos> inputs, List<BlockPos> outputs, List<Item> orders, Optional<UUID> worker, Optional<String> workerName,
			List<String> woods, int stock, boolean strip, Optional<Pit> pit, int depth, Optional<Tunnel> tunnel, int wage, Settings settings
		) {
			this.wage = wage;
			this.herd = settings.herd();
			this.boat = settings.boat();
			this.dye = settings.dye().orElse(null);
			this.tanning = settings.tanning();
			this.tannedAt = settings.tannedAt();
			this.grind = settings.grind();
			this.enchantLevel = settings.enchantLevel();
			this.made = new HashMap<>(settings.made());
			this.stocks = new HashMap<>(settings.stocks());
			this.batches = new HashMap<>(settings.batches());
			this.pit = pit.orElse(null);
			this.tunnel = tunnel.orElse(null);
			this.depth = depth;
			this.woods = new LinkedHashSet<>(woods);
			this.stock = stock;
			this.strip = strip;
			this.pos = pos;
			this.type = type.flatMap(Station::typeByName).orElse(null);
			this.inputs = new ArrayList<>(inputs);
			this.outputs = new ArrayList<>(outputs);
			this.orders = new ArrayList<>(orders.stream().distinct().limit(MAX_ORDERS).toList());
			this.worker = worker.orElse(null);
			this.workerName = workerName.orElse(null);
		}

		private static Optional<StationType> typeByName(String name) {
			try {
				return Optional.of(StationType.valueOf(name));
			} catch (IllegalArgumentException e) {
				return Optional.empty();
			}
		}

		public BlockPos pos() {
			return this.pos;
		}

		public StationType type() {
			return this.type;
		}

		/** The products the worker should make, only used by processing stations. */
		public List<Item> orders() {
			return Collections.unmodifiableList(this.orders);
		}

		public boolean hasOrder() {
			return !this.orders.isEmpty();
		}

		/**
		 * The first product of the order, null if there is none. The brewing stand has only
		 * one product.
		 */
		public @Nullable Item order() {
			return this.orders.isEmpty() ? null : this.orders.getFirst();
		}

		/** Orders only this product, or nothing. */
		public void setOrder(@Nullable Item order) {
			this.orders.clear();
			this.made.clear();
			this.stocks.clear();
			this.batches.clear();
			if (order != null) {
				this.orders.add(order);
			}
			this.setDirty();
		}

		/**
		 * Adds the product to the order, or takes it out if it is in it already. Returns false
		 * if the order is full.
		 */
		public boolean toggleOrder(Item product) {
			if (this.orders.remove(product)) {
				this.made.remove(product);
				this.stocks.remove(product);
				this.batches.remove(product);
				this.setDirty();
				return true;
			}
			if (this.orders.size() >= MAX_ORDERS) {
				return false;
			}
			// Starts where the others are, instead of catching up with all they made
			this.made.put(product, this.orders.stream().mapToInt(this::made).min().orElse(0));
			this.orders.add(product);
			this.setDirty();
			return true;
		}

		/** How many of the product of the order (all its wood types) the worker made, see {@link #made}. */
		public int made(Item order) {
			return this.made.getOrDefault(order, 0);
		}

		/** Counts made products for the product of the order they belong to, also for its {@link Batch}. */
		public void addMade(Item product, int count) {
			Item order = this.orderOf(product);
			if (order == null || count <= 0) {
				return;
			}
			Batch batch = this.batches.get(order);
			if (batch != null) {
				this.batches.put(order, new Batch(batch.amount(), Math.max(0, batch.left() - count), batch.handedIn()));
			}
			this.made.merge(order, count, Integer::sum);
			this.made.keySet().retainAll(this.orders);
			// Only the differences matter, so the numbers stay small
			int least = this.orders.stream().mapToInt(this::made).min().orElse(0);
			if (least > 0) {
				this.made.replaceAll((item, made) -> made - least);
			}
			this.setDirty();
		}

		/** The product of the order the item belongs to: itself, or its shape in another wood type. */
		public @Nullable Item orderOf(Item product) {
			for (Item order : this.orders) {
				if (this.productsOf(order).contains(product)) {
					return order;
				}
			}
			return null;
		}

		/**
		 * The products of the order the worker should make, the most needed first: products
		 * ordered once ({@link Batch}) before those kept in stock, and those before the ones
		 * without a limit. Ordered ones and those without a limit in equal parts (the ones it made
		 * the fewest of first), those in stock the ones of which the output chests hold the fewest
		 * ({@code stocked}) first. Done orders and full stocks are left out. Equal ones keep the
		 * order they were picked in.
		 */
		public List<Item> ordersByNeed(ToIntFunction<Item> stocked) {
			Map<Item, Integer> counts = new HashMap<>();
			for (Item order : this.orders) {
				if (!this.batches.containsKey(order) && this.stock(order) > 0) {
					counts.put(order, stocked.applyAsInt(order));
				}
			}
			return this.orders.stream()
				.filter(order -> this.wanted(order, counts.getOrDefault(order, 0)) > 0)
				.sorted(Comparator.<Item>comparingInt(order -> this.batches.containsKey(order) ? 0 : counts.containsKey(order) ? 1 : 2)
					.thenComparingInt(order -> counts.containsKey(order) ? counts.get(order) : this.made(order)))
				.toList();
		}

		/**
		 * How many more of the product of the order the worker should make: what is left of a
		 * {@link Batch}, what is missing to the stock ({@code stocked}: how many there are), as
		 * many as there is material for without a stock.
		 */
		public int wanted(Item order, int stocked) {
			Batch batch = this.batches.get(order);
			if (batch != null) {
				return batch.left();
			}
			int stock = this.stock(order);
			return stock > 0 ? Math.max(0, stock - stocked) : Integer.MAX_VALUE;
		}

		/** Whether the worker makes the product of the order only up to a limit: a stock or a {@link Batch}. */
		public boolean limited(Item order) {
			return this.batches.containsKey(order) || this.stock(order) > 0;
		}

		/**
		 * The products of the order of which the worker has to count how many lie in the output
		 * chests: those kept in stock.
		 */
		public boolean stocked(Item order) {
			return !this.batches.containsKey(order) && this.stock(order) > 0;
		}

		/** How many of the product of the order to keep in the output chests, 0 for no limit. */
		public int stock(Item order) {
			return this.stocks.getOrDefault(order, this.stock);
		}

		/** Keeps that many of the product of the order in stock (0: no limit) instead of ordering it once. */
		public void setStock(Item order, int stock) {
			if (!this.orders.contains(order)) {
				return;
			}
			this.batches.remove(order);
			this.stocks.put(order, Math.clamp(stock, 0, MAX_BATCH));
			this.setDirty();
		}

		/** The product of the order ordered once, null if it is kept in stock. */
		public @Nullable Batch batch(Item order) {
			return this.batches.get(order);
		}

		/**
		 * Orders that many of the product of the order once. If it was ordered once already,
		 * what was made of it counts: ordering more adds to what is left, ordering fewer takes
		 * from it.
		 */
		public void setBatch(Item order, int amount) {
			if (!this.orders.contains(order)) {
				return;
			}
			int clamped = Math.clamp(amount, 1, MAX_BATCH);
			Batch batch = this.batches.get(order);
			int made = batch == null ? 0 : batch.made();
			this.batches.put(order, new Batch(clamped, Math.max(0, clamped - made), false));
			this.setDirty();
		}

		/** The products of the order whose {@link Batch} is done but not yet handed in. */
		public List<Item> finished() {
			return this.orders.stream().filter(order -> {
				Batch batch = this.batches.get(order);
				return batch != null && batch.done() && !batch.handedIn();
			}).toList();
		}

		/**
		 * Takes the products whose {@link Batch} is done out of the order, once the worker handed
		 * them in ({@code carried}: whether it still carries some of a product of the order).
		 * With {@code keepOrder} (the kiln, which fires everything without an order) they are
		 * only marked as handed in if that would leave no order. Returns them.
		 */
		public List<Item> removeFinished(boolean keepOrder, Predicate<Item> carried) {
			List<Item> finished = this.finished().stream().filter(order -> !carried.test(order)).toList();
			if (finished.isEmpty()) {
				return finished;
			}
			if (keepOrder && finished.size() == this.orders.size()) {
				finished.forEach(order -> this.batches.computeIfPresent(order, (item, batch) -> new Batch(batch.amount(), 0, true)));
			} else {
				for (Item order : finished) {
					this.orders.remove(order);
					this.made.remove(order);
					this.stocks.remove(order);
					this.batches.remove(order);
				}
			}
			this.setDirty();
			return finished;
		}

		/** How many of the product of the order (all its wood types) lie in the output chests. */
		public int inOutputs(Level level, Item order) {
			List<Item> products = this.productsOf(order);
			int count = 0;
			for (BlockPos pos : this.validOutputs(level)) {
				Container container = getContainer(level, pos);
				if (container != null) {
					for (int slot = 0; slot < container.getContainerSize(); slot++) {
						ItemStack stack = container.getItem(slot);
						if (!stack.isEmpty() && products.contains(stack.getItem())) {
							count += stack.getCount();
						}
					}
				}
			}
			return count;
		}

		/** Chopping block: whether the lumberjack strips the logs before handing them in. */
		public boolean strip() {
			return this.strip;
		}

		public void setStrip(boolean strip) {
			if (this.strip != strip) {
				this.strip = strip;
				this.setDirty();
			}
		}

		/**
		 * Carpenter's bench: adds a product picked in the overview to the order, or takes it
		 * out. A product made of one wood type stands for its shape (keeping the wood types),
		 * any other product keeps the wood types too, all of them if none were selected.
		 * Returns false if the order is full.
		 */
		public boolean orderProduct(Item product) {
			String shape = WoodTypes.shapeOf(product, this.type.shapes());
			if (shape != null) {
				return this.toggleShape(shape);
			}
			if (this.woods.isEmpty()) {
				WoodTypes.ALL.forEach(wood -> this.woods.add(wood.name()));
			}
			return this.toggleOrder(product);
		}

		/** Pit stake: the pit to dig, null until one is picked. */
		public @Nullable Pit pit() {
			return this.pit;
		}

		public void setPit(@Nullable Pit pit) {
			this.pit = pit;
			this.setDirty();
		}

		/** Mining bench: the tunnel to dig, null until one is picked. */
		public @Nullable Tunnel tunnel() {
			return this.tunnel;
		}

		public void setTunnel(@Nullable Tunnel tunnel) {
			if (!Objects.equals(this.tunnel, tunnel)) {
				this.tunnel = tunnel;
				this.setDirty();
			}
		}

		/**
		 * Loom: up to how many sheep (lambs included) the shepherd breeds around the loom, 0
		 * if it does not breed them.
		 */
		public int herd() {
			return this.herd;
		}

		public void setHerd(int herd) {
			int clamped = Math.clamp(herd, 0, MAX_HERD);
			if (this.herd != clamped) {
				this.herd = clamped;
				this.setDirty();
			}
		}

		/**
		 * Target: how far around it the guard shoots, 8 to 32 blocks. Kept in the herd field
		 * (half the radius; the target has no herd), so the default herd of 8 is a radius of 16.
		 */
		public int guardRadius() {
			return Math.clamp(this.herd * 2, MIN_GUARD_RADIUS, MAX_GUARD_RADIUS);
		}

		public void setGuardRadius(int radius) {
			this.setHerd(Math.clamp(radius, MIN_GUARD_RADIUS, MAX_GUARD_RADIUS) / 2);
		}

		/**
		 * Target: whether the guard hunts (roams around at night) instead of standing watch.
		 * Kept in the strip field of the chopping block, which the target has no use for.
		 */
		public boolean hunting() {
			return this.strip;
		}

		public void setHunting(boolean hunting) {
			this.setStrip(hunting);
		}

		/**
		 * Brewing stand: the potion of the order ({@code minecraft:healing}), null for glass
		 * bottles. Kept in the woods field, which the brewing stand has no use for.
		 */
		public @Nullable String potion() {
			return this.woods.isEmpty() ? null : this.woods.iterator().next();
		}

		/** Brewing stand: orders the item, for potions with the potion (see {@link #potion}). */
		public void setOrder(@Nullable Item order, @Nullable String potion) {
			this.woods.clear();
			if (order != null && potion != null) {
				this.woods.add(potion);
			}
			this.setOrder(order);
		}

		private Settings settings() {
			return new Settings(this.herd, this.boat, Optional.ofNullable(this.dye), this.tanning, this.tannedAt, this.grind, this.enchantLevel, Map.copyOf(this.made),
				Map.copyOf(this.stocks), Map.copyOf(this.batches));
		}

		/** Lectern: the level the librarian enchants at, 0 for the highest the bookshelves allow. */
		public int enchantLevel() {
			return this.enchantLevel;
		}

		public void setEnchantLevel(int level) {
			int clamped = Math.clamp(level, 0, MAX_ENCHANT_LEVEL);
			if (this.enchantLevel != clamped) {
				this.enchantLevel = clamped;
				this.setDirty();
			}
		}

		/**
		 * Grindstone: what the weaponsmith does besides its order: grind worn tools together and
		 * disenchant ({@link #GRIND_BOTH}), only one of them, or neither.
		 */
		public int grindMode() {
			return this.grind;
		}

		public void setGrindMode(int mode) {
			int wrapped = Math.floorMod(mode, GRIND_MODES);
			if (this.grind != wrapped) {
				this.grind = wrapped;
				this.setDirty();
			}
		}

		/** Grindstone: whether the weaponsmith grinds two worn tools into one. */
		public boolean merges() {
			return this.grind == GRIND_BOTH || this.grind == GRIND_MERGE;
		}

		/** Grindstone: whether the weaponsmith disenchants what lies in its input chests. */
		public boolean disenchants() {
			return this.grind == GRIND_BOTH || this.grind == GRIND_DISENCHANT;
		}

		/** Cauldron: the color the tanner dyes leather armor, null for none. */
		public @Nullable DyeColor dye() {
			return this.dye;
		}

		public void setDye(@Nullable DyeColor dye) {
			if (this.dye != dye) {
				this.dye = dye;
				this.setDirty();
			}
		}

		/** Cauldron: how much rotten flesh is tanning in it, 0 for none. */
		public int tanning() {
			return this.tanning;
		}

		/** Cauldron: when the rotten flesh in it is tanned (game time). */
		public long tannedAt() {
			return this.tannedAt;
		}

		/** Cauldron: what is in it, rotten flesh or (once done) the leather made of it. */
		public ItemStack tanningContents(long now) {
			if (now < this.tannedAt) {
				return new ItemStack(Items.ROTTEN_FLESH, this.tanning);
			}
			return new ItemStack(Items.LEATHER, this.tanning / Math.max(1, Config.FLESH_PER_LEATHER.get()));
		}

		/** Cauldron: puts rotten flesh in to tan until the given time; 0 takes it out. */
		public void setTanning(int flesh, long until) {
			this.tanning = Math.clamp(flesh, 0, MAX_TANNING);
			this.tannedAt = this.tanning == 0 ? 0L : until;
			this.setDirty();
		}

		/** Barrel: whether the fisher rows out in a boat when there is open water nearby. */
		public boolean boat() {
			return this.boat;
		}

		public void setBoat(boolean boat) {
			if (this.boat != boat) {
				this.boat = boat;
				this.setDirty();
			}
		}

		/**
		 * Pit stake: how deep to dig, below the highest string of the ring. The pit digger digs
		 * at most as deep as its level allows ({@link PitLimits}).
		 */
		public int depth() {
			return Math.clamp(this.depth, 1, PitLimits.maxDepth());
		}

		public void setDepth(int depth) {
			int clamped = Math.clamp(depth, 1, PitLimits.maxDepth());
			if (this.depth != clamped) {
				this.depth = clamped;
				this.setDirty();
			}
		}

		/** Woodcutter and carpenter's bench: the wood types the order may be made of. */
		public Set<String> woods() {
			return Collections.unmodifiableSet(this.woods);
		}

		public void setWood(String wood, boolean allowed) {
			if (allowed ? this.woods.add(wood) : this.woods.remove(wood)) {
				this.setDirty();
			}
		}

		/**
		 * Keeps that many of every product of the order in stock (0: no limit), also of those
		 * added later; the stocks set per product and the orders of single products are dropped.
		 */
		public void setStock(int stock) {
			this.stock = Math.clamp(stock, 0, MAX_BATCH);
			this.stocks.clear();
			this.batches.clear();
			this.setDirty();
		}

		/**
		 * Woodcutter and carpenter's bench: the shape of a product of the order (stairs,
		 * boat, ...), null if it is something else.
		 */
		public @Nullable String shapeOf(Item order) {
			return this.type.hasWoods() ? WoodTypes.shapeOf(order, this.type.shapes()) : null;
		}

		/** Woodcutter and carpenter's bench: the shapes in the order. */
		public List<String> shapes() {
			return this.orders.stream().map(this::shapeOf).filter(Objects::nonNull).distinct().toList();
		}

		/** Woodcutter and carpenter's bench: orders only the given shape, keeping the wood types. */
		public void setShape(String shape) {
			Item product = this.shapeProduct(shape);
			if (product != null) {
				this.setOrder(product);
			}
		}

		/**
		 * Woodcutter and carpenter's bench: adds the shape to the order, keeping the wood
		 * types, or takes it out. Returns false if the order is full.
		 */
		public boolean toggleShape(String shape) {
			for (Item order : this.orders) {
				if (shape.equals(this.shapeOf(order))) {
					return this.toggleOrder(order);
				}
			}
			Item product = this.shapeProduct(shape);
			return product == null || this.toggleOrder(product);
		}

		/** The shape in the first allowed wood type that has it; allows that one if none are. */
		private @Nullable Item shapeProduct(String shape) {
			WoodTypes.WoodType wood = this.woods.stream()
				.map(WoodTypes::byName)
				.filter(Objects::nonNull)
				.filter(candidate -> WoodTypes.product(candidate, shape) != null)
				.findFirst()
				.orElse(WoodTypes.ALL.getFirst());
			Item product = WoodTypes.product(wood, shape);
			if (product != null && this.woods.isEmpty()) {
				this.woods.add(wood.name());
			}
			return product;
		}

		/**
		 * What the worker makes for a product of the order: the product, and for a shape the
		 * shape in every allowed wood type.
		 */
		public List<Item> productsOf(Item order) {
			String shape = this.shapeOf(order);
			if (shape == null || WoodTypes.STICK.equals(shape)) {
				return List.of(order);
			}
			return this.woods.stream()
				.map(WoodTypes::byName)
				.filter(Objects::nonNull)
				.map(wood -> WoodTypes.product(wood, shape))
				.filter(Objects::nonNull)
				.distinct()
				.toList();
		}

		/** What the worker makes for the whole order. */
		public List<Item> products() {
			return this.orders.stream().flatMap(order -> this.productsOf(order).stream()).distinct().toList();
		}

		/**
		 * Whether the worker may use the item as material: at a woodcutter only the allowed
		 * wood types; at a carpenter's bench anything made of an allowed wood type or of no
		 * wood at all (iron, string, ...).
		 */
		public boolean acceptsMaterial(ItemStack stack) {
			if (this.type == StationType.CARPENTER_BENCH) {
				WoodTypes.WoodType wood = WoodTypes.woodOfMaterial(stack);
				return wood == null || this.woods.contains(wood.name());
			}
			if (this.shapes().isEmpty()) {
				return true;
			}
			return this.woods.stream().map(WoodTypes::byName).filter(Objects::nonNull).anyMatch(wood -> wood.isMaterial(stack));
		}

		/** The hired villager working here, as of the last {@link #occupant} check. */
		public @Nullable UUID worker() {
			return this.worker;
		}

		public boolean hasWorker(UUID worker, String name) {
			return worker.equals(this.worker) && name.equals(this.workerName);
		}

		public void clearWorker() {
			if (this.worker != null) {
				this.worker = null;
				this.workerName = null;
				this.setDirty();
			}
		}

		/** Called when a hired villager takes this station as its job site. */
		public void setWorker(UUID worker, String name) {
			this.worker = worker;
			this.workerName = name;
			this.setDirty();
		}

		/**
		 * Who works here: the name of the hired worker, "occupied" if a villager not hired by
		 * a player works here (a mason at a stonecutter), empty if the station is free.
		 */
		public Optional<Component> occupant(ServerLevel level) {
			boolean occupied = level.getPoiManager()
				.getInSquare(poi -> true, this.pos, 0, PoiManager.Occupancy.IS_OCCUPIED)
				.anyMatch(poi -> poi.getPos().equals(this.pos));
			if (this.worker != null && (!occupied || this.workerLeft(level))) {
				this.worker = null;
				this.workerName = null;
				this.setDirty();
			}
			if (!occupied) {
				return Optional.empty();
			}
			if (this.workerName == null) {
				return Optional.of(Component.translatable("message.workingvillagers.status.occupied"));
			}
			return Optional.of(this.workerName.isEmpty()
				? Component.translatable("message.workingvillagers.status.unnamed_worker")
				: Component.literal(this.workerName));
		}

		/** Whether the stored worker is loaded and works somewhere else now. */
		private boolean workerLeft(ServerLevel level) {
			if (!(level.getEntity(this.worker) instanceof Villager villager)) {
				return false;
			}
			Optional<GlobalPos> jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
			return jobSite.isEmpty() || !jobSite.get().pos().equals(this.pos) || villager.isRemoved();
		}

		public @Nullable ChestRole role(BlockPos chest) {
			if (this.inputs.contains(chest)) {
				return ChestRole.INPUT;
			}
			return this.outputs.contains(chest) ? ChestRole.OUTPUT : null;
		}

		/**
		 * The linked position of the container: the given one, or for a double chest the
		 * other half if that one is linked. Null if the container is not linked.
		 */
		public @Nullable BlockPos linkedHalf(Level level, BlockPos chest) {
			if (this.role(chest) != null) {
				return chest;
			}
			BlockPos other = otherHalf(level, chest);
			return other != null && this.role(other) != null ? other : null;
		}

		/**
		 * Links the chest in the given role, or unlinks it if the role is null.
		 */
		public void setRole(BlockPos chest, @Nullable ChestRole role) {
			this.inputs.remove(chest);
			this.outputs.remove(chest);
			if (role == ChestRole.INPUT) {
				this.inputs.add(chest.immutable());
			} else if (role == ChestRole.OUTPUT) {
				this.outputs.add(chest.immutable());
			}
			this.setDirty();
		}

		/**
		 * Like {@link #setRole(BlockPos, ChestRole)}, but a double chest as a whole: the other
		 * half is unlinked, so the worker does not use the same chest twice.
		 */
		public void setRole(Level level, BlockPos chest, @Nullable ChestRole role) {
			BlockPos other = otherHalf(level, chest);
			if (other != null) {
				this.inputs.remove(other);
				this.outputs.remove(other);
			}
			this.setRole(chest, role);
		}

		public int chestCount() {
			return this.inputs.size() + this.outputs.size();
		}

		public boolean canReach(BlockPos chest) {
			return chest.closerThan(this.pos, Config.CHEST_DISTANCE.get());
		}

		/** The input chests that still exist, a double chest once. */
		public List<BlockPos> validInputs(Level level) {
			return valid(level, this.inputs, List.of());
		}

		/**
		 * The output chests that still exist, a double chest once. A double chest linked as
		 * input by its other half (two linked chests placed side by side) is no output.
		 */
		public List<BlockPos> validOutputs(Level level) {
			return valid(level, this.outputs, this.inputs);
		}

		private static List<BlockPos> valid(Level level, Collection<BlockPos> chests, Collection<BlockPos> taken) {
			List<BlockPos> valid = new ArrayList<>(chests.size());
			for (BlockPos pos : chests) {
				BlockPos other = otherHalf(level, pos);
				if (getContainer(level, pos) != null && (other == null || !valid.contains(other) && !taken.contains(other))) {
					valid.add(pos);
				}
			}
			return valid;
		}

		private boolean isEmpty() {
			return this.inputs.isEmpty() && this.outputs.isEmpty() && this.orders.isEmpty() && this.worker == null && this.woods.isEmpty() && this.stock == 0
				&& this.stocks.isEmpty() && this.batches.isEmpty()
				&& !this.strip && this.pit == null && this.depth == Pit.DEFAULT_DEPTH && this.tunnel == null && this.wage == 0
				&& this.herd == DEFAULT_HERD && this.boat && this.dye == null && this.tanning == 0
				&& this.grind == GRIND_BOTH && this.enchantLevel == 0;
		}

		/**
		 * The emeralds in the station's wage box: the worker takes its wage from here, and
		 * emeralds given to it go in here. They belong to the station, not to the worker, so
		 * nothing is lost when the worker dies, becomes a zombie or changes its job. When the
		 * station is broken they drop like the contents of a chest.
		 */
		public int wage() {
			return this.wage;
		}

		public void setWage(int wage) {
			int clamped = Math.clamp(wage, 0, MAX_WAGE);
			if (this.wage != clamped) {
				this.wage = clamped;
				this.setDirty();
			}
		}

		/** Puts up to {@code count} emeralds into the wage box. Returns how many fit. */
		public int addWage(int count) {
			int added = Math.clamp(count, 0, MAX_WAGE - this.wage);
			this.setWage(this.wage + added);
			return added;
		}

		private void setDirty() {
			if (this.owner != null) {
				this.owner.setDirty();
			}
		}
	}
}
