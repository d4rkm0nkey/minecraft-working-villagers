package me.teajay.workingvillagers.station;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.crafting.WoodTypes;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
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
				pos.immutable(), Optional.empty(), List.of(), List.of(), Optional.empty(), Optional.empty(), Optional.empty(), List.of(), 0, false,
				Optional.empty(), Pit.DEFAULT_DEPTH, Optional.empty(), 0, DEFAULT_HERD, true
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
	 * or became another kind, the emeralds of its wage box drop where it stood.
	 */
	public static void onBlockChanged(ServerLevel level, BlockPos pos, BlockState before, BlockState after) {
		StationType old = StationType.of(before);
		if (old == null || old == StationType.of(after)) {
			return;
		}
		Workstations workstations = level.getDataStorage().computeIfAbsent(TYPE);
		Station station = workstations.stations.get(pos);
		if (station == null || station.wage == 0) {
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
		int daily = Config.DAILY_WAGE.get();
		player.sendOverlayMessage(daily > 0
			? Component.translatable("message.workingvillagers.wage.deposited", station.wage(), station.wage() / daily)
			: Component.translatable("message.workingvillagers.wage.deposited_free", station.wage()));
		return true;
	}

	public static @Nullable Container getContainer(Level level, BlockPos pos) {
		return level.isLoaded(pos) ? HopperBlockEntity.getContainerAt(level, pos) : null;
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
	public static final int MAX_HERD = 32;
	public static final int MIN_GUARD_RADIUS = 8;
	public static final int MAX_GUARD_RADIUS = 32;

	public static final class Station {
		private static final Codec<Station> CODEC = RecordCodecBuilder.create(i -> i.group(
			BlockPos.CODEC.fieldOf("pos").forGetter(station -> station.pos),
			Codec.STRING.optionalFieldOf("type").forGetter(station -> Optional.ofNullable(station.type).map(Enum::name)),
			BlockPos.CODEC.listOf().optionalFieldOf("inputs", List.of()).forGetter(station -> station.inputs),
			BlockPos.CODEC.listOf().optionalFieldOf("outputs", List.of()).forGetter(station -> station.outputs),
			BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("order").forGetter(station -> Optional.ofNullable(station.order)),
			UUIDUtil.CODEC.optionalFieldOf("worker").forGetter(station -> Optional.ofNullable(station.worker)),
			Codec.STRING.optionalFieldOf("worker_name").forGetter(station -> Optional.ofNullable(station.workerName)),
			Codec.STRING.listOf().optionalFieldOf("woods", List.of()).forGetter(station -> List.copyOf(station.woods)),
			Codec.INT.optionalFieldOf("stock", 0).forGetter(station -> station.stock),
			Codec.BOOL.optionalFieldOf("strip", false).forGetter(station -> station.strip),
			Pit.CODEC.optionalFieldOf("pit").forGetter(station -> Optional.ofNullable(station.pit)),
			Codec.INT.optionalFieldOf("depth", Pit.DEFAULT_DEPTH).forGetter(station -> station.depth),
			Tunnel.CODEC.optionalFieldOf("tunnel").forGetter(station -> Optional.ofNullable(station.tunnel)),
			Codec.intRange(0, MAX_WAGE).optionalFieldOf("wage", 0).forGetter(station -> station.wage),
			Codec.intRange(0, MAX_HERD).optionalFieldOf("herd", DEFAULT_HERD).forGetter(station -> station.herd),
			Codec.BOOL.optionalFieldOf("boat", true).forGetter(station -> station.boat)
		).apply(i, Station::new));

		private final BlockPos pos;
		private final List<BlockPos> inputs;
		private final List<BlockPos> outputs;
		private @Nullable Item order;
		private @Nullable UUID worker;
		private @Nullable String workerName;
		/**
		 * Woodcutter and carpenter's bench: the wood types the order may be made of. At the
		 * bench they also limit the material of products made of any wood (chests, ...).
		 */
		private final Set<String> woods;
		/** Carpenter's bench: how many products to keep in the output chests, 0 to make them as long as there is material. */
		private int stock;
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
		private @Nullable Workstations owner;
		/** The kind of station the settings belong to, unknown for entries saved before it was stored. */
		private @Nullable StationType type;

		private Station(
			BlockPos pos, Optional<String> type, List<BlockPos> inputs, List<BlockPos> outputs, Optional<Item> order, Optional<UUID> worker, Optional<String> workerName,
			List<String> woods, int stock, boolean strip, Optional<Pit> pit, int depth, Optional<Tunnel> tunnel, int wage, int herd, boolean boat
		) {
			this.wage = wage;
			this.herd = herd;
			this.boat = boat;
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
			this.order = order.orElse(null);
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

		/** What the worker should make, only used by processing stations. */
		public @Nullable Item order() {
			return this.order;
		}

		public void setOrder(@Nullable Item order) {
			this.order = order;
			this.setDirty();
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
		 * Carpenter's bench: orders a product picked in the overview. A product made of one
		 * wood type becomes its shape (keeping the wood types), any other product keeps the
		 * wood types too, all of them if none were selected.
		 */
		public void orderProduct(Item product) {
			String shape = WoodTypes.shapeOf(product, this.type.shapes());
			if (shape != null) {
				this.setShape(shape);
				return;
			}
			if (this.woods.isEmpty()) {
				WoodTypes.ALL.forEach(wood -> this.woods.add(wood.name()));
			}
			this.setOrder(product);
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

		/** Pit stake: how deep to dig, below the highest string of the ring. */
		public int depth() {
			return Math.clamp(this.depth, 1, Config.PIT_MAX_DEPTH.get());
		}

		public void setDepth(int depth) {
			int clamped = Math.clamp(depth, 1, Config.PIT_MAX_DEPTH.get());
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

		/** Carpenter's bench: how many products to keep in the output chests, 0 for no limit. */
		public int stock() {
			return this.stock;
		}

		public void setStock(int stock) {
			if (this.stock != Math.max(0, stock)) {
				this.stock = Math.max(0, stock);
				this.setDirty();
			}
		}

		/**
		 * Woodcutter and carpenter's bench: the shape of the order (stairs, boat, ...), null
		 * if there is no order or it is something else.
		 */
		public @Nullable String shape() {
			return this.type.hasWoods() && this.order != null ? WoodTypes.shapeOf(this.order, this.type.shapes()) : null;
		}

		/** Woodcutter and carpenter's bench: orders the given shape, keeping the wood types. */
		public void setShape(String shape) {
			WoodTypes.WoodType wood = this.woods.stream()
				.map(WoodTypes::byName)
				.filter(Objects::nonNull)
				.filter(candidate -> WoodTypes.product(candidate, shape) != null)
				.findFirst()
				.orElse(WoodTypes.ALL.getFirst());
			Item product = WoodTypes.product(wood, shape);
			if (product != null) {
				if (this.woods.isEmpty()) {
					this.woods.add(wood.name());
				}
				this.setOrder(product);
			}
		}

		/**
		 * What the worker makes: the order, and for a shape the order's shape in every
		 * allowed wood type.
		 */
		public List<Item> products() {
			if (this.order == null) {
				return List.of();
			}
			String shape = this.shape();
			if (shape == null) {
				return List.of(this.order);
			}
			if (WoodTypes.STICK.equals(shape)) {
				return List.of(this.order);
			}
			return this.woods.stream()
				.map(WoodTypes::byName)
				.filter(Objects::nonNull)
				.map(wood -> WoodTypes.product(wood, shape))
				.filter(Objects::nonNull)
				.distinct()
				.toList();
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
			if (this.shape() == null) {
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

		public int chestCount() {
			return this.inputs.size() + this.outputs.size();
		}

		public boolean canReach(BlockPos chest) {
			return chest.closerThan(this.pos, Config.CHEST_DISTANCE.get());
		}

		/** The input chests that still exist. */
		public List<BlockPos> validInputs(Level level) {
			return this.inputs.stream().filter(pos -> getContainer(level, pos) != null).toList();
		}

		/** The output chests that still exist. */
		public List<BlockPos> validOutputs(Level level) {
			return this.outputs.stream().filter(pos -> getContainer(level, pos) != null).toList();
		}

		private boolean isEmpty() {
			return this.inputs.isEmpty() && this.outputs.isEmpty() && this.order == null && this.worker == null && this.woods.isEmpty() && this.stock == 0
				&& !this.strip && this.pit == null && this.depth == Pit.DEFAULT_DEPTH && this.tunnel == null && this.wage == 0
				&& this.herd == DEFAULT_HERD && this.boat;
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
