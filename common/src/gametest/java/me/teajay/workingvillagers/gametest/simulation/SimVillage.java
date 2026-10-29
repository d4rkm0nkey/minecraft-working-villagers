package me.teajay.workingvillagers.gametest.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.carrier.CarrierData;
import me.teajay.workingvillagers.carrier.Carriers;
import me.teajay.workingvillagers.carrier.Route;
import me.teajay.workingvillagers.item.WorkContractItem;
import me.teajay.workingvillagers.job.CarryItems;
import me.teajay.workingvillagers.job.HiredVillager;
import me.teajay.workingvillagers.job.VillageChests;
import me.teajay.workingvillagers.job.WorkerJobs;
import me.teajay.workingvillagers.job.WorkerLevels;
import me.teajay.workingvillagers.job.WorkerSupplies;
import me.teajay.workingvillagers.station.ContainerNames;
import me.teajay.workingvillagers.station.Pit;
import me.teajay.workingvillagers.station.Tunnel;
import me.teajay.workingvillagers.station.Workstations;
import me.teajay.workingvillagers.station.Workstations.ChestRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Builds a big village the way a player who has played the mod for a long time might end
 * up with: a main street with the central storage and the markets, and on both sides wings
 * with every worker of the mod, chained by carriers. Every product ends up in the storage
 * halls along the main street, where the shop assistants sell what the villagers of the
 * markets buy; their emeralds go to the village chests and pay the wages.
 *
 * <p>The village is built with the same calls the game tests use (hire with a contract,
 * assign, link chests, give wage and tools), only the blocks are set directly. Each wing is
 * laid out in its own frame: {@code u} goes from the main street outwards, {@code v} along
 * it. Positions that break a limit of the mod (chest distance, route length, village chest
 * reach) are listed in {@link #warnings()}.
 */
public final class SimVillage {
	/** Length of a wing along the main street, including the gap to the next one. */
	public static final int WING_LENGTH = 104;
	/** How far a wing reaches out from the middle of the main street. */
	public static final int WING_DEPTH = 72;
	private static final int CLEAR_HEIGHT = 18;
	/** A carrier route is kept a bit below the limit, so a relay chest is never just out of reach. */
	private static final int ROUTE_MARGIN = 2;

	final ServerLevel level;
	final ServerPlayer employer;
	final int ground;
	final int originX;
	final int originZ;
	final int wingCount;
	/** Whether a wall goes around the village, as most players build one to keep monsters out. */
	final boolean walled;
	final RandomSource random;
	final SimRegistry registry = new SimRegistry();
	private final List<String> warnings = new ArrayList<>();
	/** What stands on each column of the village (x, z), to find overlaps and free spots for relay chests. */
	private final Map<Long, String> used = new HashMap<>();

	private SimVillage(ServerLevel level, ServerPlayer employer, BlockPos origin, int wingCount, boolean walled) {
		this.walled = walled;
		this.level = level;
		this.employer = employer;
		this.originX = origin.getX();
		this.originZ = origin.getZ();
		this.wingCount = wingCount;
		this.random = RandomSource.create(4711);
		int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.getX(), origin.getZ());
		this.ground = Math.max(surface, level.getMinY() + 16);
	}

	/**
	 * Builds the village with {@code wingCount} wings, the first two on both sides of the
	 * main street at {@code origin}, the next ones further along it.
	 */
	public static SimVillage build(ServerLevel level, ServerPlayer employer, BlockPos origin, int wingCount, boolean walled) {
		SimVillage village = new SimVillage(level, employer, origin, wingCount, walled);
		village.buildAll();
		return village;
	}

	public List<String> warnings() {
		return this.warnings;
	}

	public SimRegistry registry() {
		return this.registry;
	}

	public int rows() {
		return (this.wingCount + 1) / 2;
	}

	/** The middle of the village, on the main street. */
	public BlockPos center() {
		return new BlockPos(this.originX, this.ground, this.originZ + this.rows() * WING_LENGTH / 2);
	}

	private void buildAll() {
		this.prepareGround();
		List<Wing> wings = new ArrayList<>();
		for (int i = 0; i < this.wingCount; i++) {
			wings.add(new Wing(this, i, i % 2 == 0 ? 1 : -1, this.originZ + (i / 2) * WING_LENGTH));
		}
		for (Wing wing : wings) {
			wing.build();
		}
		for (Wing wing : wings) {
			wing.connect();
		}
		this.checkVillageChests();
		this.light();
		if (this.walled) {
			this.wall();
		}
		this.registry.ground = this.ground;
		this.registry.center = this.center();
		this.registry.wings = this.wingCount;
	}

	/** Clears the land and lays a floor of grass on dirt and stone, deep enough for the pits. */
	private void prepareGround() {
		int minX = this.originX - WING_DEPTH - 2;
		int maxX = this.originX + WING_DEPTH + 2;
		int minZ = this.originZ - 4;
		int maxZ = this.originZ + this.rows() * WING_LENGTH;
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
		BlockState dirt = Blocks.DIRT.defaultBlockState();
		BlockState stone = Blocks.STONE.defaultBlockState();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				for (int y = this.ground - 14; y < this.ground + CLEAR_HEIGHT; y++) {
					BlockState state = y >= this.ground ? air : y == this.ground - 1 ? grass : y >= this.ground - 4 ? dirt : stone;
					pos.set(x, y, z);
					if (this.level.getBlockState(pos) != state) {
						this.level.setBlock(pos, state, Block.UPDATE_CLIENTS);
					}
				}
			}
		}
		// The main street, paved
		for (int z = minZ; z <= maxZ; z++) {
			for (int x = this.originX - 3; x <= this.originX + 3; x++) {
				this.level.setBlock(new BlockPos(x, this.ground - 1, z), Blocks.DIRT_PATH.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
	}

	/** A wall of two cobblestone walls high around the whole village, without a gate: no zombie walks in. */
	private void wall() {
		int minX = this.originX - WING_DEPTH - 1;
		int maxX = this.originX + WING_DEPTH + 1;
		int minZ = this.originZ - 3;
		int maxZ = this.originZ + this.rows() * WING_LENGTH - 1;
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				if (x == minX || x == maxX || z == minZ || z == maxZ) {
					this.set(new BlockPos(x, this.ground, z), Blocks.COBBLESTONE_WALL);
					this.set(new BlockPos(x, this.ground + 1, z), Blocks.COBBLESTONE_WALL);
				}
			}
		}
	}

	/**
	 * Glowstone in the floor every 8 blocks, where nothing else lies: like a player who lights
	 * the village so nothing spawns in it. The fields, pits, pond and the hill stay as they are.
	 */
	private void light() {
		int minX = this.originX - WING_DEPTH;
		int maxX = this.originX + WING_DEPTH;
		int minZ = this.originZ - 2;
		int maxZ = this.originZ + this.rows() * WING_LENGTH - 2;
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				if (Math.floorMod(x - this.originX, 8) != 4 || Math.floorMod(z - this.originZ, 8) != 4) {
					continue;
				}
				String what = this.used.get(key(x, z));
				BlockPos floor = new BlockPos(x, this.ground - 1, z);
				BlockState state = this.level.getBlockState(floor);
				if ((what == null || what.startsWith("path")) && (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT_PATH))
					&& this.level.getBlockState(floor.above()).isAir()) {
					this.level.setBlock(floor, Blocks.GLOWSTONE.defaultBlockState(), Block.UPDATE_ALL);
				}
			}
		}
	}

	// --- Columns ---

	static long key(int x, int z) {
		return ((long) x << 32) ^ (z & 0xffffffffL);
	}

	/** Marks the column as used by {@code what}; a second object on the same column is a mistake of the layout. */
	void reserve(BlockPos pos, String what) {
		String before = this.used.putIfAbsent(key(pos.getX(), pos.getZ()), what);
		if (before != null && !(before.startsWith("path") && what.startsWith("path"))) {
			if (before.startsWith("path") || what.startsWith("path")) {
				this.warn("Blocked path at " + pos.toShortString() + ": " + before + " / " + what);
			} else {
				this.warn("Overlap at " + pos.toShortString() + ": " + before + " / " + what);
			}
			if (before.startsWith("path")) {
				this.used.put(key(pos.getX(), pos.getZ()), what);
			}
		}
	}

	boolean isFree(int x, int z) {
		return !this.used.containsKey(key(x, z));
	}

	void warn(String warning) {
		this.warnings.add(warning);
	}

	// --- Blocks ---

	void set(BlockPos pos, BlockState state) {
		this.level.setBlock(pos, state, Block.UPDATE_ALL);
	}

	void set(BlockPos pos, Block block) {
		this.set(pos, block.defaultBlockState());
	}

	void command(String command) {
		this.level.getServer().getCommands().performPrefixedCommand(
			this.level.getServer().createCommandSourceStack().withSuppressedOutput().withLevel(this.level), command);
	}

	/** A chest that opens towards {@code front}, named like a player names it, with the items in it. */
	BlockPos chest(BlockPos pos, Direction front, String name, ItemStack... items) {
		this.reserve(pos, "chest " + name);
		this.reserve(pos.relative(front), "path to " + name);
		this.set(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, front));
		this.fill(pos, items);
		ContainerNames.rename(this.level, pos, name);
		return pos;
	}

	/** A double chest that opens towards {@code front}; the other half lies {@code along} from {@code pos}. */
	BlockPos doubleChest(BlockPos pos, Direction front, Direction along, String name) {
		BlockPos other = pos.relative(along);
		this.reserve(pos, "chest " + name);
		this.reserve(other, "chest " + name);
		this.reserve(pos.relative(front), "path to " + name);
		this.reserve(other.relative(front), "path to " + name);
		// The left half seen from the front: the other half is to its right
		boolean otherIsRight = front.getCounterClockWise() == along;
		BlockState state = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, front);
		this.set(pos, state.setValue(ChestBlock.TYPE, otherIsRight ? ChestType.RIGHT : ChestType.LEFT));
		this.set(other, state.setValue(ChestBlock.TYPE, otherIsRight ? ChestType.LEFT : ChestType.RIGHT));
		ContainerNames.rename(this.level, pos, name);
		return pos;
	}

	BlockPos villageChest(BlockPos pos, Direction front, String name, ItemStack... items) {
		this.reserve(pos, "village chest " + name);
		this.reserve(pos.relative(front), "path to " + name);
		this.set(pos, WorkingVillagers.VILLAGE_CHEST.get().defaultBlockState().setValue(ChestBlock.FACING, front));
		this.fill(pos, items);
		ContainerNames.rename(this.level, pos, name);
		this.registry.villageChests.add(pos);
		return pos;
	}

	void fill(BlockPos pos, ItemStack... items) {
		Container container = Workstations.getContainer(this.level, pos);
		if (container == null) {
			this.warn("No container at " + pos.toShortString());
			return;
		}
		for (int i = 0; i < items.length && i < container.getContainerSize(); i++) {
			container.setItem(i, items[i]);
		}
		container.setChanged();
	}

	Workstations.Station station(BlockPos pos) {
		return Workstations.find(this.level, pos).orElseThrow(() -> new IllegalStateException("No workstation at " + pos.toShortString()));
	}

	void link(BlockPos station, BlockPos chest, ChestRole role) {
		Workstations.Station found = this.station(station);
		if (!found.canReach(chest)) {
			this.warn("Chest " + ContainerNames.name(this.level, chest).getString() + " at " + chest.toShortString() + " is out of reach of the station at "
				+ station.toShortString() + " (" + (int) Math.sqrt(station.distSqr(chest)) + " blocks)");
		}
		found.setRole(this.level, chest, role);
	}

	// --- Villagers ---

	Villager spawnVillager(BlockPos pos) {
		Villager villager = EntityTypes.VILLAGER.spawn(this.level, pos, EntitySpawnReason.COMMAND);
		if (villager == null) {
			throw new IllegalStateException("Could not spawn a villager at " + pos.toShortString());
		}
		villager.setPersistenceRequired();
		return villager;
	}

	/**
	 * A villager hired with a work contract by the employer, assigned to the station, with the
	 * station's wage box filled for {@code wageDays} days and the items given to it.
	 */
	Villager worker(String job, int wing, BlockPos station, int wageDays, ItemStack... supplies) {
		Villager villager = this.spawnVillager(station.above().relative(Direction.Plane.HORIZONTAL.getRandomDirection(this.random)));
		ItemStack contract = new ItemStack(WorkingVillagers.WORK_CONTRACT.get());
		this.employer.setItemInHand(InteractionHand.MAIN_HAND, contract);
		WorkContractItem.hire(villager, this.employer, contract, InteractionHand.MAIN_HAND);
		this.employer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		if (!((HiredVillager) villager).workingvillagers$isHired()) {
			this.warn("Could not hire the " + job + " at " + station.toShortString());
		}
		Component problem = WorkerJobs.assign(this.level, villager, station);
		if (problem != null) {
			this.warn("Could not assign the " + job + " at " + station.toShortString() + ": " + problem.getString());
		}
		this.station(station).addWage(wageDays * Config.DAILY_WAGE.get());
		this.give(villager, job, supplies);
		villager.setCustomName(Component.literal(job));
		this.registry.workers.add(new SimRegistry.Worker(villager.getUUID(), job, wing, station));
		return villager;
	}

	void give(Villager villager, String job, ItemStack... items) {
		for (ItemStack item : items) {
			if (WorkerSupplies.accepts(villager, item) <= 0) {
				this.warn("The " + job + " does not take " + item);
				continue;
			}
			this.employer.setItemInHand(InteractionHand.MAIN_HAND, item.copy());
			WorkerSupplies.give(villager, this.employer, InteractionHand.MAIN_HAND);
			this.employer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		}
	}

	/**
	 * A job block with an unemployed villager next to it, who takes the job like in any
	 * village: one of the customers of the shop assistants.
	 */
	Villager trader(int wing, BlockPos block, Block type, ResourceKey<VillagerProfession> profession, BlockPos stand) {
		this.reserve(block, "job block " + profession.identifier().getPath());
		this.set(block, type);
		Villager villager = this.spawnVillager(stand);
		this.registry.traders.add(new SimRegistry.Trader(villager.getUUID(), profession.identifier().getPath(), wing, block));
		return villager;
	}

	/**
	 * A carrier with the routes, carrying emeralds for {@code wageDays} days. It has the level
	 * its routes need (a novice only works one short route, see {@link CarryItems#canCarry}):
	 * the village is one a player has played for a long time, and a carrier that cannot work
	 * its routes never learns.
	 */
	Villager carrier(String name, int wing, List<Route> routes, int wageDays) {
		ItemStack basket = new ItemStack(WorkingVillagers.WICKER_BASKET.get());
		CarrierData.writeRoutes(basket, routes);
		Villager villager = this.spawnVillager(routes.getFirst().source().relative(Direction.Plane.HORIZONTAL.getRandomDirection(this.random)).above());
		this.employer.setItemInHand(InteractionHand.MAIN_HAND, basket);
		Carriers.appoint(villager, this.employer, basket, InteractionHand.MAIN_HAND);
		this.employer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		if (!Carriers.isCarrier(villager)) {
			this.warn("Could not make " + name + " a carrier");
		}
		int level = WorkerLevels.NOVICE;
		for (int i = 0; i < routes.size(); i++) {
			int needed = CarryItems.neededLevel(i, routes.get(i));
			if (needed < 0) {
				this.warn(name + ": route " + routes.get(i).name() + " is longer than any carrier goes");
			}
			level = Math.max(level, needed);
		}
		if (level > WorkerLevels.NOVICE) {
			villager.setVillagerData(villager.getVillagerData().withLevel(level));
			villager.setVillagerXp(VillagerData.getMinXpPerLevel(level));
		}
		this.give(villager, name, new ItemStack(Items.EMERALD, wageDays * Config.DAILY_WAGE.get()));
		villager.setCustomName(Component.literal(name));
		this.registry.workers.add(new SimRegistry.Worker(villager.getUUID(), "carrier", wing, routes.getFirst().source()));
		return villager;
	}

	void animals(EntityType<? extends net.minecraft.world.entity.Mob> type, BlockPos center, int count, int spread) {
		for (int i = 0; i < count; i++) {
			BlockPos pos = center.offset(this.random.nextInt(2 * spread + 1) - spread, 0, this.random.nextInt(2 * spread + 1) - spread);
			net.minecraft.world.entity.Mob animal = type.spawn(this.level, pos, EntitySpawnReason.COMMAND);
			if (animal != null) {
				animal.setPersistenceRequired();
				if (animal instanceof Sheep sheep) {
					sheep.setColor(DyeColor.WHITE);
				}
			}
		}
	}

	// --- Carriers ---

	/** Items to carry from one chest to another; far ones are carried in steps over relay chests. */
	record Flow(BlockPos source, BlockPos target, String name) {
	}

	/**
	 * Turns the flows of a wing into carriers: a flow longer than a route may be is split by
	 * relay chests on the way, and the routes are packed three to a carrier whose sources lie
	 * close enough together.
	 */
	void carriers(Wing wing, List<Flow> flows) {
		int maxLength = Config.ROUTE_LENGTH.get() - ROUTE_MARGIN;
		List<Route> routes = new ArrayList<>();
		for (Flow flow : flows) {
			BlockPos from = flow.source();
			int step = 0;
			while (Math.sqrt(from.distSqr(flow.target())) > maxLength) {
				step++;
				BlockPos relay = this.relaySpot(wing, from, flow.target(), maxLength);
				if (relay == null) {
					this.warn("No spot for a relay chest of " + flow.name() + " from " + from.toShortString());
					break;
				}
				this.chest(relay, wing.toSpine, flow.name() + " " + step);
				this.registry.relays.add(relay);
				routes.add(new Route(from, relay).withName(flow.name() + " " + step));
				from = relay;
			}
			routes.add(new Route(from, flow.target()).withName(flow.name()));
		}
		// Pack: routes from close sources share a carrier
		int spread = Config.ROUTE_SOURCE_SPREAD.get() - ROUTE_MARGIN;
		List<List<Route>> carriers = new ArrayList<>();
		for (Route route : routes) {
			List<Route> fitting = null;
			for (List<Route> carrier : carriers) {
				if (carrier.size() < Route.MAX_ROUTES && carrier.stream().allMatch(other -> Math.sqrt(other.source().distSqr(route.source())) <= spread)) {
					fitting = carrier;
					break;
				}
			}
			if (fitting == null) {
				fitting = new ArrayList<>();
				carriers.add(fitting);
			}
			fitting.add(route);
		}
		for (List<Route> carrier : carriers) {
			this.carrier("Carrier " + (wing.index + 1) + "-" + (carriers.indexOf(carrier) + 1), wing.index, carrier, 3);
		}
	}

	/**
	 * A free spot for a relay chest about {@code maxLength - 2} blocks from {@code from}
	 * towards {@code to}, with its front free; searched in rings around that point.
	 */
	private BlockPos relaySpot(Wing wing, BlockPos from, BlockPos to, int maxLength) {
		double length = Math.sqrt(from.distSqr(to));
		double reach = Math.min(maxLength - 3, length - 1);
		int ax = (int) Math.round(from.getX() + (to.getX() - from.getX()) * reach / length);
		int az = (int) Math.round(from.getZ() + (to.getZ() - from.getZ()) * reach / length);
		for (int radius = 0; radius <= 6; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
						continue;
					}
					int x = ax + dx;
					int z = az + dz;
					BlockPos spot = new BlockPos(x, this.ground, z);
					BlockPos front = spot.relative(wing.toSpine);
					if (this.isFree(x, z) && this.isFree(front.getX(), front.getZ()) && spot.distSqr(from) <= (double) maxLength * maxLength
						&& this.level.getBlockState(spot).isAir() && this.level.getBlockState(spot.below()).isSolid()) {
						return spot;
					}
				}
			}
		}
		return null;
	}

	/** Every workstation must lie within reach of a village chest of its wing. */
	void checkVillageChests() {
		for (SimRegistry.Worker worker : this.registry.workers) {
			if (worker.job().equals("carrier")) {
				continue;
			}
			boolean reached = this.registry.villageChests.stream().anyMatch(chest -> VillageChests.inReach(worker.station(), chest)
				&& Math.sqrt(worker.station().distSqr(chest)) <= Config.VILLAGE_CHEST_RADIUS.get());
			if (!reached) {
				this.warn("The " + worker.job() + " at " + worker.station().toShortString() + " has no village chest in reach");
			}
		}
	}

	// --- A wing ---

	/** One wing of the village, on one side of the main street. */
	static final class Wing {
		final SimVillage village;
		final int index;
		/** 1: the wing lies towards +x, -1: towards -x. */
		final int side;
		final int z0;
		final Direction toSpine;
		final Direction away;
		final List<Flow> flows = new ArrayList<>();
		final Map<String, BlockPos> chests = new LinkedHashMap<>();
		final List<BlockPos> storageA = new ArrayList<>();
		final List<BlockPos> storageB = new ArrayList<>();

		Wing(SimVillage village, int index, int side, int z0) {
			this.village = village;
			this.index = index;
			this.side = side;
			this.z0 = z0;
			this.toSpine = side > 0 ? Direction.WEST : Direction.EAST;
			this.away = this.toSpine.getOpposite();
		}

		/** The block {@code u} out from the middle of the main street, {@code v} along it, {@code dy} above the floor. */
		BlockPos at(int u, int dy, int v) {
			return new BlockPos(this.village.originX + this.side * u, this.village.ground + dy, this.z0 + v);
		}

		BlockPos at(int u, int v) {
			return this.at(u, 0, v);
		}

		BlockPos chest(String name, int u, int v, ItemStack... items) {
			BlockPos pos = this.village.chest(this.at(u, v), this.toSpine, name, items);
			this.chests.put(name, pos);
			return pos;
		}

		/** Marks a rectangle (inclusive) as taken by {@code what}. */
		void area(int u0, int v0, int u1, int v1, String what) {
			for (int u = Math.min(u0, u1); u <= Math.max(u0, u1); u++) {
				for (int v = Math.min(v0, v1); v <= Math.max(v0, v1); v++) {
					this.village.reserve(this.at(u, v), what);
				}
			}
		}

		BlockPos stationBlock(String what, int u, int v, BlockState state) {
			BlockPos pos = this.at(u, v);
			this.village.reserve(pos, what);
			this.village.set(pos, state);
			return pos;
		}

		BlockPos stationBlock(String what, int u, int v, Block block) {
			return this.stationBlock(what, u, v, block.defaultBlockState());
		}

		void flow(String from, BlockPos target, String name) {
			BlockPos source = this.chests.get(from);
			if (source == null) {
				throw new IllegalStateException("No chest " + from);
			}
			this.flows.add(new Flow(source, target, name));
		}

		void build() {
			this.market();
			this.hub();
			this.pens();
			this.mine();
			this.pits();
			this.forest();
			this.fields();
			this.pond();
			this.guards();
		}

		/** The carriers, once every chest of the wing stands. */
		void connect() {
			this.village.carriers(this, this.flows);
		}

		// The storage halls, the markets with their customers and the dormitory along the main street

		void market() {
			SimVillage v = this.village;
			int w = this.index;
			// Storage: double chests along the street, opening onto it
			for (int i = 0; i < 6; i++) {
				this.storageA.add(v.doubleChest(this.at(4, 14 + 3 * i), this.toSpine, Direction.SOUTH, "Storage " + (w + 1) + "A" + (i + 1)));
				this.storageB.add(v.doubleChest(this.at(4, 66 + 3 * i), this.toSpine, Direction.SOUTH, "Storage " + (w + 1) + "B" + (i + 1)));
			}
			v.registry.storage.addAll(this.storageA);
			v.registry.storage.addAll(this.storageB);
			this.shop("A", 22, this.storageA);
			this.shop("B", 74, this.storageB);
			// The dormitory: beds in blocks of 8 with gaps to walk through
			BlockState bed = Blocks.BED.red().defaultBlockState().setValue(BedBlock.FACING, this.away);
			for (int block = 0; block < 10; block++) {
				for (int i = 0; i < 8; i++) {
					int vv = 2 + block * 10 + i;
					v.reserve(this.at(16, vv), "bed");
					v.reserve(this.at(17, vv), "bed");
					v.level.setBlock(this.at(16, vv), bed.setValue(BedBlock.PART, BedPart.FOOT), Block.UPDATE_CLIENTS);
					v.level.setBlock(this.at(17, vv), bed.setValue(BedBlock.PART, BedPart.HEAD), Block.UPDATE_ALL);
				}
			}
			BlockPos bell = this.at(6, 50);
			v.reserve(bell, "bell");
			v.set(bell, Blocks.BELL);
			// The depots where the products of the wing gather on the way to the storage
			this.chests.put("Depot N", v.doubleChest(this.at(21, 36), this.toSpine, Direction.SOUTH, "Depot " + (w + 1) + "N"));
			this.chests.put("Depot S", v.doubleChest(this.at(21, 60), this.toSpine, Direction.SOUTH, "Depot " + (w + 1) + "S"));
			v.registry.depots.add(this.chests.get("Depot N"));
			v.registry.depots.add(this.chests.get("Depot S"));
			for (BlockPos storage : this.storageA) {
				this.flow("Depot N", storage, "To storage");
			}
			for (BlockPos storage : this.storageB) {
				this.flow("Depot S", storage, "To storage");
			}
		}

		/** A trading counter selling from the storage chests next to it, and its customers. */
		void shop(String name, int v0, List<BlockPos> storage) {
			SimVillage v = this.village;
			BlockPos counter = this.stationBlock("trading counter", 8, v0, WorkingVillagers.TRADING_COUNTER.get());
			BlockPos till = this.chest("Till " + name, 8, v0 + 3);
			for (BlockPos chest : storage) {
				v.link(counter, chest, ChestRole.INPUT);
			}
			v.link(counter, till, ChestRole.OUTPUT);
			v.worker("Shop assistant " + name, this.index, counter, 3);
			this.tills.add(till);
			v.registry.tills.add(till);
			// The customers: one of every profession that buys something
			List<Object[]> customers = List.of(
				new Object[] {Blocks.COMPOSTER, VillagerProfession.FARMER},
				new Object[] {Blocks.COMPOSTER, VillagerProfession.FARMER},
				new Object[] {Blocks.FLETCHING_TABLE, VillagerProfession.FLETCHER},
				new Object[] {Blocks.LECTERN, VillagerProfession.LIBRARIAN},
				new Object[] {Blocks.STONECUTTER, VillagerProfession.MASON},
				new Object[] {Blocks.LOOM, VillagerProfession.SHEPHERD},
				new Object[] {Blocks.SMOKER, VillagerProfession.BUTCHER},
				new Object[] {Blocks.BARREL, VillagerProfession.FISHERMAN},
				new Object[] {Blocks.CAULDRON, VillagerProfession.LEATHERWORKER},
				new Object[] {Blocks.BREWING_STAND, VillagerProfession.CLERIC},
				new Object[] {Blocks.BLAST_FURNACE, VillagerProfession.ARMORER},
				new Object[] {Blocks.SMITHING_TABLE, VillagerProfession.TOOLSMITH},
				new Object[] {Blocks.GRINDSTONE, VillagerProfession.WEAPONSMITH});
			for (int i = 0; i < customers.size(); i++) {
				int vv = v0 - 13 + 2 * i;
				@SuppressWarnings("unchecked")
				ResourceKey<VillagerProfession> profession = (ResourceKey<VillagerProfession>) customers.get(i)[1];
				v.trader(this.index, this.at(13, vv), (Block) customers.get(i)[0], profession, this.at(11, vv));
			}
		}

		final List<BlockPos> tills = new ArrayList<>();

		// The armory and workshops around the village chests in the middle of the wing

		void hub() {
			SimVillage v = this.village;
			int w = this.index;
			// The village chests: supplies the player keeps stocked, the wages, and what the workshops make
			v.villageChest(this.at(38, 47), this.toSpine, "Supplies " + (w + 1),
				new ItemStack(Items.IRON_AXE), new ItemStack(Items.IRON_AXE), new ItemStack(Items.IRON_HOE), new ItemStack(Items.IRON_SHOVEL),
				new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.IRON_PICKAXE),
				new ItemStack(Items.IRON_SWORD), new ItemStack(Items.SHEARS), new ItemStack(Items.FISHING_ROD), new ItemStack(Items.BOW), new ItemStack(Items.BOW),
				new ItemStack(Items.LADDER, 64), new ItemStack(Items.LADDER, 64), new ItemStack(Items.TORCH, 64), new ItemStack(Items.TORCH, 64),
				new ItemStack(Items.OAK_FENCE, 64), new ItemStack(Items.OAK_PLANKS, 64), new ItemStack(Items.OAK_LOG, 32), new ItemStack(Items.COBBLESTONE, 64),
				new ItemStack(Items.WHEAT, 64), new ItemStack(Items.WHEAT_SEEDS, 64), new ItemStack(Items.ARROW, 64), new ItemStack(Items.GLASS_BOTTLE, 32),
				new ItemStack(Items.COAL, 32), new ItemStack(Items.BUCKET));
			BlockPos wages = v.villageChest(this.at(38, 50), this.toSpine, "Wages " + (w + 1),
				new ItemStack(Items.EMERALD, 64), new ItemStack(Items.EMERALD, 64));
			BlockPos made = v.villageChest(this.at(38, 53), this.toSpine, "Made " + (w + 1));
			this.chests.put("Wages", wages);
			for (BlockPos till : this.tills) {
				this.flows.add(new Flow(till, wages, "Wages"));
			}

			// Shared input chests
			BlockPos ingots = this.chest("Ingots", 38, 44);
			BlockPos xp = this.chest("Loot and bottles", 34, 50);
			BlockPos sticks = this.chest("Sticks", 42, 50);
			BlockPos fletch = this.chest("Fletcher", 44, 52, new ItemStack(Items.FEATHER, 64), new ItemStack(Items.FEATHER, 64));
			BlockPos glass = this.chest("Glass", 40, 58);

			// Smith at the smithing table: repairs the worn tools in the village chests
			BlockPos smithing = this.stationBlock("smithing table", 34, 46, Blocks.SMITHING_TABLE);
			v.link(smithing, ingots, ChestRole.INPUT);
			v.link(smithing, xp, ChestRole.INPUT);
			v.worker("Repair smith", w, smithing, 3);

			// Weaponsmith: forges the tools the workers use, into the village chest
			BlockPos grindstone = this.stationBlock("grindstone", 42, 46, Blocks.GRINDSTONE);
			Workstations.Station forge = v.station(grindstone);
			for (Item tool : List.of(Items.IRON_AXE, Items.IRON_PICKAXE, Items.IRON_SHOVEL, Items.IRON_HOE, Items.IRON_SWORD, Items.SHEARS)) {
				if (!forge.toggleOrder(tool)) {
					v.warn("The weaponsmith cannot forge " + tool);
				}
			}
			forge.setStock(2);
			v.link(grindstone, ingots, ChestRole.INPUT);
			v.link(grindstone, sticks, ChestRole.INPUT);
			v.link(grindstone, made, ChestRole.OUTPUT);
			v.worker("Weaponsmith", w, grindstone, 3);

			// Fletcher: arrows for the guards from the gravel of the pit
			BlockPos table = this.stationBlock("fletching table", 42, 54, Blocks.FLETCHING_TABLE);
			Workstations.Station fletcher = v.station(table);
			fletcher.setOrder(Items.ARROW);
			fletcher.toggleOrder(Items.BOW);
			fletcher.setStock(64);
			v.link(table, sticks, ChestRole.INPUT);
			v.link(table, fletch, ChestRole.INPUT);
			v.link(table, made, ChestRole.OUTPUT);
			v.worker("Fletcher", w, table, 3);

			// Tanner: leather of the guards' rotten flesh, uniforms for the guards
			BlockPos cauldron = this.stationBlock("cauldron", 34, 54, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
			BlockPos water = this.at(31, -1, 55);
			v.reserve(water.above(), "water for the tanner");
			v.set(water, Blocks.WATER);
			Workstations.Station tanner = v.station(cauldron);
			tanner.setOrder(Items.LEATHER);
			tanner.toggleOrder(Items.LEATHER_CHESTPLATE);
			tanner.toggleOrder(Items.LEATHER_HELMET);
			tanner.setStock(4);
			tanner.setDye(this.side > 0 ? DyeColor.BLUE : DyeColor.RED);
			v.link(cauldron, xp, ChestRole.INPUT);
			v.link(cauldron, made, ChestRole.OUTPUT);
			v.worker("Tanner", w, cauldron, 3, new ItemStack(Items.BUCKET));

			// Apothecary: glass bottles for the guards, of the glass from the kiln; nether wart beside it
			BlockPos stand = this.stationBlock("brewing stand", 38, 56, Blocks.BREWING_STAND);
			Workstations.Station apothecary = v.station(stand);
			apothecary.setOrder(Items.GLASS_BOTTLE, null);
			apothecary.setStock(32);
			v.link(stand, glass, ChestRole.INPUT);
			v.link(stand, made, ChestRole.OUTPUT);
			for (int du = 0; du <= 1; du++) {
				for (int dv = 0; dv <= 1; dv++) {
					BlockPos wart = this.at(35 + du, 58 + dv);
					v.reserve(wart, "nether wart");
					v.set(wart.below(), Blocks.SOUL_SAND);
					v.set(wart, Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, v.random.nextInt(4)));
				}
			}
			v.worker("Apothecary", w, stand, 3);

			// Librarian: enchants books with the guards' bottles at a table with bookshelves
			BlockPos enchanting = this.stationBlock("enchanting table", 48, 42, Blocks.ENCHANTING_TABLE);
			for (int du = -2; du <= 2; du++) {
				for (int dv = -2; dv <= 2; dv++) {
					if ((Math.abs(du) == 2 || Math.abs(dv) == 2) && !(du == 0 && dv == 2)) {
						this.stationBlock("bookshelf", 48 + du, 42 + dv, Blocks.BOOKSHELF);
					}
				}
			}
			BlockPos lectern = this.stationBlock("lectern", 45, 46, Blocks.LECTERN);
			BlockPos books = this.chest("Books", 47, 48, new ItemStack(Items.BOOK, 64), new ItemStack(Items.LAPIS_LAZULI, 64));
			BlockPos enchanted = this.chest("Enchanted", 44, 49);
			v.link(lectern, books, ChestRole.INPUT);
			v.link(lectern, xp, ChestRole.INPUT);
			v.link(lectern, enchanted, ChestRole.OUTPUT);
			v.worker("Librarian", w, lectern, 3);
			this.flow("Enchanted", this.chests.get("Depot N"), "Enchanted books");
			if (enchanting.distSqr(lectern) > 36) {
				v.warn("The enchanting table is too far from the lectern");
			}

			// Sawyer for sticks, from logs the carriers bring from the forest
			BlockPos stickSaw = this.stationBlock("woodcutter sticks", 48, 52, WorkingVillagers.WOODCUTTER_BLOCK.get());
			v.reserve(this.at(48, 51), "path to the woodcutter");
			BlockPos stickLogs = this.chest("Logs for sticks", 50, 52);
			Workstations.Station stickCutter = v.station(stickSaw);
			stickCutter.setWood("oak", true);
			stickCutter.setShape("stick");
			stickCutter.setStock(64);
			v.link(stickSaw, stickLogs, ChestRole.INPUT);
			v.link(stickSaw, sticks, ChestRole.OUTPUT);
			v.worker("Stick sawyer", w, stickSaw, 3);
			this.chests.put("Ingots", ingots);
			this.chests.put("Loot", xp);
			this.chests.put("Fletcher", fletch);
			this.chests.put("Glass", glass);
			this.chests.put("Logs for sticks", stickLogs);
		}

		// Sheep, cows and chickens in pens on the side of the depot

		void pens() {
			SimVillage v = this.village;
			int w = this.index;
			BlockPos feed = this.chest("Feed", 32, 22);
			String[] names = {"Sheep", "Cows", "Chickens"};
			for (int i = 0; i < 3; i++) {
				int v0 = 2 + 11 * i;
				for (int u = 20; u <= 28; u++) {
					for (int vv = v0; vv <= v0 + 8; vv++) {
						v.reserve(this.at(u, vv), "pen");
						if (u == 20 || u == 28 || vv == v0 || vv == v0 + 8) {
							v.set(this.at(u, vv), Blocks.OAK_FENCE);
						}
					}
				}
				// Nothing next to the fence: villagers jump onto a chest there and over the fence into the pen
				for (int u = 19; u <= 29; u++) {
					for (int vv = v0 - 1; vv <= v0 + 9; vv++) {
						if ((u == 19 || u == 29 || vv == v0 - 1 || vv == v0 + 9) && v.isFree(this.at(u, vv).getX(), this.at(u, vv).getZ())) {
							v.reserve(this.at(u, vv), "path around the pen");
						}
					}
				}
				BlockPos middle = this.at(24, v0 + 4);
				BlockPos output = this.chest(names[i], 32, v0 + 2);
				if (i == 0) {
					BlockPos loom = this.stationBlock("loom", 30, v0 + 4, Blocks.LOOM);
					v.link(loom, output, ChestRole.OUTPUT);
					v.worker("Shepherd", w, loom, 3, new ItemStack(Items.SHEARS), new ItemStack(Items.WHEAT, 16));
					v.animals(EntityTypes.SHEEP, middle, 10, 3);
					this.flow(names[i], this.chests.get("Depot N"), "Wool");
				} else {
					BlockPos smoker = this.stationBlock("smoker", 30, v0 + 4, Blocks.SMOKER);
					v.link(smoker, output, ChestRole.OUTPUT);
					v.link(smoker, feed, ChestRole.INPUT);
					v.worker("Butcher " + names[i].toLowerCase(), w, smoker, 3, new ItemStack(Items.IRON_SWORD),
						new ItemStack(i == 1 ? Items.WHEAT : Items.WHEAT_SEEDS, 16), new ItemStack(Items.COAL, 16));
					v.animals(i == 1 ? EntityTypes.COW : EntityTypes.CHICKEN, middle, 10, 3);
					this.flow(names[i], this.chests.get("Depot N"), i == 1 ? "Beef and leather" : "Chicken");
				}
			}
			this.chests.put("Feed", feed);
		}

		// The hill with the miner and the furnaces for its stone and ore

		void mine() {
			SimVillage v = this.village;
			int w = this.index;
			// A hill of stone with ores
			for (int u = 52; u <= 70; u++) {
				for (int vv = 4; vv <= 28; vv++) {
					v.reserve(this.at(u, vv), "hill");
					for (int dy = 0; dy < 8; dy++) {
						float roll = v.random.nextFloat();
						Block block = roll < 0.06F ? Blocks.COAL_ORE : roll < 0.10F ? Blocks.IRON_ORE : roll < 0.107F ? Blocks.LAPIS_ORE : Blocks.STONE;
						v.level.setBlock(this.at(u, dy, vv), block.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
			BlockPos smelt = this.chest("Mine", 46, 16);
			BlockPos bench = this.stationBlock("mining bench", 49, 12, WorkingVillagers.MINING_BENCH.get());
			v.link(bench, smelt, ChestRole.OUTPUT);
			v.station(bench).setTunnel(Tunnel.of(this.at(52, 16), this.away).withLength(18).withBranches(8));
			v.worker("Miner", w, bench, 3, new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.OAK_FENCE, 16), new ItemStack(Items.OAK_PLANKS, 16),
				new ItemStack(Items.TORCH, 16), new ItemStack(Items.COBBLESTONE, 16));

			// The kiln for stone, the kiln for glass and the blast furnace share the mine's chest
			BlockPos stoneOut = this.chest("Stone", 40, 12);
			BlockPos ingotOut = this.chest("Iron", 40, 16);
			BlockPos glassOut = this.chest("Glass out", 40, 20);
			BlockPos kilnStone = this.stationBlock("kiln stone", 44, 12, WorkingVillagers.KILN.get());
			v.station(kilnStone).setOrder(Items.STONE);
			v.link(kilnStone, smelt, ChestRole.INPUT);
			v.link(kilnStone, stoneOut, ChestRole.OUTPUT);
			v.worker("Kiln operator stone", w, kilnStone, 3);
			BlockPos kilnGlass = this.stationBlock("kiln glass", 44, 20, WorkingVillagers.KILN.get());
			v.station(kilnGlass).setOrder(Items.GLASS);
			v.link(kilnGlass, smelt, ChestRole.INPUT);
			v.link(kilnGlass, glassOut, ChestRole.OUTPUT);
			v.worker("Kiln operator glass", w, kilnGlass, 3);
			BlockPos blast = this.stationBlock("blast furnace", 42, 16, Blocks.BLAST_FURNACE);
			v.link(blast, smelt, ChestRole.INPUT);
			v.link(blast, ingotOut, ChestRole.OUTPUT);
			v.worker("Smelter", w, blast, 3);
			// The stone sawyer turns the stone into bricks
			BlockPos cutter = this.stationBlock("stonecutter", 38, 9, Blocks.STONECUTTER);
			v.reserve(this.at(38, 8), "path to the stonecutter");
			BlockPos bricks = this.chest("Bricks", 36, 12);
			v.station(cutter).setOrder(Items.STONE_BRICKS);
			v.link(cutter, stoneOut, ChestRole.INPUT);
			v.link(cutter, bricks, ChestRole.OUTPUT);
			v.worker("Stone sawyer", w, cutter, 3);
			this.flow("Bricks", this.chests.get("Depot N"), "Bricks");
			this.flow("Iron", this.chests.get("Ingots"), "Iron");
			this.flow("Glass out", this.chests.get("Glass"), "Glass");
		}

		// A sand pit for the glass and a gravel pit for the arrows

		void pits() {
			this.pit("Sand", 32, Blocks.SAND, this.chests.get("Mine"));
			this.pit("Gravel", 66, Blocks.GRAVEL, this.chests.get("Fletcher"));
		}

		/** A 8×8 pit of {@code block} (the most a novice digs), the ring of string on the ground, the stake beside it. */
		void pit(String name, int v0, Block block, BlockPos deliverTo) {
			SimVillage v = this.village;
			for (int u = 52; u <= 61; u++) {
				for (int vv = v0; vv <= v0 + 9; vv++) {
					boolean inside = u > 52 && u < 61 && vv > v0 && vv < v0 + 9;
					v.reserve(this.at(u, vv), "pit");
					for (int dy = -9; dy <= -1; dy++) {
						v.level.setBlock(this.at(u, dy, vv), (inside ? block : dy == -1 ? Blocks.GRASS_BLOCK : Blocks.DIRT).defaultBlockState(), Block.UPDATE_CLIENTS);
					}
					v.level.setBlock(this.at(u, -10, vv), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
					if (!inside) {
						v.set(this.at(u, vv), Blocks.TRIPWIRE);
					}
				}
			}
			BlockPos stake = this.stationBlock("pit stake " + name, 51, v0 + 4, WorkingVillagers.PIT_STAKE.get());
			Workstations.Station station = v.station(stake);
			Pit.attach(v.level, station).ifPresent(problem -> v.warn("The " + name + " pit: " + problem.message().getString()));
			if (station.pit() == null) {
				v.warn("The stake of the " + name + " pit took no pit");
			}
			station.setDepth(8);
			BlockPos output = this.chest(name, 50, v0 + 2);
			v.link(stake, output, ChestRole.OUTPUT);
			v.worker("Pit digger " + name.toLowerCase(), this.index, stake, 3, new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.IRON_PICKAXE),
				new ItemStack(Items.LADDER, 16), new ItemStack(Items.TORCH, 8));
			this.flow(name, deliverTo, name);
		}

		// The forest with two lumberjacks, the sawmill and the carpenter

		void forest() {
			SimVillage v = this.village;
			int w = this.index;
			this.area(52, 78, 70, 100, "forest");
			for (int u = 53; u <= 69; u += 4) {
				for (int vv = 79; vv <= 99; vv += 4) {
					BlockPos trunk = this.at(u, vv);
					v.set(trunk.below(), Blocks.GRASS_BLOCK);
					v.command("place feature minecraft:oak " + trunk.getX() + " " + trunk.getY() + " " + trunk.getZ());
				}
			}
			BlockPos logs = this.chest("Logs", 50, 86);
			for (int[] spot : new int[][] {{55, 81}, {63, 93}}) {
				BlockPos block = this.at(spot[0], spot[1]);
				v.used.put(key(block.getX(), block.getZ()), "chopping block");
				v.set(block, WorkingVillagers.CHOPPING_BLOCK.get());
				v.link(block, logs, ChestRole.OUTPUT);
				v.worker("Lumberjack", w, block, 3, new ItemStack(Items.IRON_AXE), new ItemStack(Items.OAK_SAPLING, 16));
			}
			BlockPos saw = this.stationBlock("woodcutter planks", 48, 84, WorkingVillagers.WOODCUTTER_BLOCK.get());
			v.reserve(this.at(48, 83), "path to the woodcutter");
			BlockPos planks = this.chest("Planks", 46, 86);
			Workstations.Station cutter = v.station(saw);
			cutter.setWood("oak", true);
			cutter.setShape("planks");
			v.link(saw, logs, ChestRole.INPUT);
			v.link(saw, planks, ChestRole.OUTPUT);
			v.worker("Sawyer", w, saw, 3);
			BlockPos bench = this.stationBlock("carpenter's bench", 44, 84, WorkingVillagers.CARPENTER_BENCH.get());
			v.reserve(this.at(44, 83), "path to the bench");
			BlockPos goods = this.chest("Goods", 42, 86);
			Workstations.Station carpenter = v.station(bench);
			carpenter.orderProduct(Items.CHEST);
			carpenter.orderProduct(Items.BARREL);
			carpenter.orderProduct(Items.OAK_BOAT);
			v.link(bench, planks, ChestRole.INPUT);
			v.link(bench, goods, ChestRole.OUTPUT);
			v.worker("Carpenter", w, bench, 3);
			this.flow("Goods", this.chests.get("Depot S"), "Goods");
			this.flow("Logs", this.chests.get("Logs for sticks"), "Logs for sticks");
		}

		// Two fields: one feeds the animals, the other is sold

		void fields() {
			this.field("Crops feed", 73, this.chests.get("Feed"));
			this.field("Crops", 91, this.chests.get("Depot S"));
		}

		void field(String name, int v0, BlockPos deliverTo) {
			SimVillage v = this.village;
			BlockPos composter = this.at(29, v0);
			for (int u = 22; u <= 36; u++) {
				for (int vv = v0 - 7; vv <= v0 + 7; vv++) {
					BlockPos pos = this.at(u, vv);
					v.reserve(pos, "field");
					if (pos.equals(composter)) {
						continue;
					}
					boolean water = (u == 25 || u == 33) && (vv == v0 - 4 || vv == v0 + 4);
					if (water) {
						v.level.setBlock(pos.below(), Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
					} else {
						v.level.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE), Block.UPDATE_CLIENTS);
						v.level.setBlock(pos, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, v.random.nextInt(CropBlock.MAX_AGE + 1)), Block.UPDATE_CLIENTS);
					}
				}
			}
			v.set(composter, Blocks.COMPOSTER);
			BlockPos output = this.chest(name, 38, v0);
			v.link(composter, output, ChestRole.OUTPUT);
			v.worker("Farmer", this.index, composter, 3, new ItemStack(Items.IRON_HOE), new ItemStack(Items.WHEAT_SEEDS, 16));
			this.flow(name, deliverTo, name);
		}

		void pond() {
			SimVillage v = this.village;
			for (int u = 40; u <= 48; u++) {
				for (int vv = 92; vv <= 100; vv++) {
					v.reserve(this.at(u, vv), "pond");
					v.level.setBlock(this.at(u, -1, vv), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
					v.level.setBlock(this.at(u, -2, vv), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
			BlockPos barrel = this.stationBlock("barrel", 39, 89, Blocks.BARREL);
			BlockPos output = this.chest("Fish", 41, 89);
			v.link(barrel, output, ChestRole.OUTPUT);
			v.worker("Fisher", this.index, barrel, 3, new ItemStack(Items.FISHING_ROD));
			this.flow("Fish", this.chests.get("Depot S"), "Fish");
		}

		/** Four guards on targets around the middle of the wing; their loot and bottles go to the hub. */
		void guards() {
			SimVillage v = this.village;
			int[][] spots = {{30, 40}, {30, 64}, {60, 46}, {60, 62}};
			for (int i = 0; i < spots.length; i++) {
				BlockPos target = this.stationBlock("target", spots[i][0], spots[i][1], Blocks.TARGET);
				Workstations.Station station = v.station(target);
				station.setGuardRadius(32);
				BlockPos loot = this.chest("Guard " + (i + 1), spots[i][0] + 2, spots[i][1]);
				v.link(target, loot, ChestRole.OUTPUT);
				v.worker("Guard", this.index, target, 3, new ItemStack(Items.BOW), new ItemStack(Items.ARROW, 64), new ItemStack(Items.GLASS_BOTTLE, 16));
				this.flow("Guard " + (i + 1), this.chests.get("Loot"), "Loot");
			}
		}
	}
}
