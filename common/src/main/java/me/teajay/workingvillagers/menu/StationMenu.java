package me.teajay.workingvillagers.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.crafting.RecipeType;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.crafting.CarpenterRecipes;
import me.teajay.workingvillagers.crafting.CuttingRecipes;
import me.teajay.workingvillagers.crafting.BrewingPaths;
import me.teajay.workingvillagers.crafting.ForgeRecipes;
import me.teajay.workingvillagers.crafting.KilnRecipes;
import me.teajay.workingvillagers.crafting.StoneFamilies;
import me.teajay.workingvillagers.crafting.WoodTypes;
import me.teajay.workingvillagers.job.ChopTrees;
import me.teajay.workingvillagers.job.EnchantItems;
import me.teajay.workingvillagers.job.FletchArrows;
import me.teajay.workingvillagers.job.TanLeather;
import me.teajay.workingvillagers.job.GuardPost;
import me.teajay.workingvillagers.job.HiredVillager;
import me.teajay.workingvillagers.job.TendAnimals;
import me.teajay.workingvillagers.job.TendSheep;
import me.teajay.workingvillagers.job.Unlocks;
import me.teajay.workingvillagers.job.WorkerJobs;
import me.teajay.workingvillagers.job.WorkerLevels;
import me.teajay.workingvillagers.job.WorkerStatus;
import me.teajay.workingvillagers.station.ContainerNames;
import me.teajay.workingvillagers.station.Pit;
import me.teajay.workingvillagers.station.PitLimits;
import me.teajay.workingvillagers.station.StationPicks;
import me.teajay.workingvillagers.station.StationType;
import me.teajay.workingvillagers.station.Tunnel;
import me.teajay.workingvillagers.station.Workstations.ChestRole;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The overview of a workstation: who works there and the linked chests, each with buttons
 * to switch its role and to unlink it. At the chopping block it also shows the logs felled
 * today; at the woodcutter it sets the order, the product's shape and the wood types to
 * use; at the carpenter's bench the product, the wood types and how many to keep in stock;
 * at the stonecutter the product, picked from its stone family. With an order, how many of
 * each product to make once or to keep in stock. At the composter it shows
 * the crops harvested today; the blast furnace has no order. At the pit stake it shows the
 * blocks dug today and picks the pit, its ladder and depth; at the kiln it sets the product
 * (none: everything) and how many to keep in stock.
 *
 * <p>The cutters, the bench and the furnaces open it with a button in their own menu and get
 * back with a button.
 */
public class StationMenu extends AbstractContainerMenu {
	/** Plus the chest index: unlinks the chest. */
	public static final int UNLINK_BUTTON = 0;
	/** Plus the chest index: switches the chest between input and output. */
	public static final int ROLE_BUTTON = 10;
	/**
	 * Plus the index in {@link WoodTypes#SHAPES} (woodcutter) or in the products of the
	 * bench: adds that shape or product to the order, or takes it out.
	 */
	public static final int SHAPE_BUTTON = 100;
	/** Plus the index in {@link WoodTypes#ALL}: allows or forbids that wood type. */
	public static final int WOOD_BUTTON = 200;
	public static final int ALL_WOODS_BUTTON = 300;
	public static final int NO_WOODS_BUTTON = 301;
	/** Back to the crafting menu of a cutter. */
	public static final int BACK_BUTTON = 302;
	/** Closes the menu, the player then right-clicks the hired villager to work here. */
	public static final int ASSIGN_BUTTON = 303;
	/** Takes the worker away from the station (it stays hired). */
	public static final int UNASSIGN_BUTTON = 304;
	/** Closes the menu, the player then right-clicks chests to link them as inputs / outputs. */
	public static final int ADD_INPUTS_BUTTON = 305;
	public static final int ADD_OUTPUTS_BUTTON = 306;
	/** Chopping block: turns stripping the logs on or off. */
	public static final int STRIP_BUTTON = 310;
	/** Pit stake: closes the menu, the player then right-clicks the string where the ladder goes. */
	public static final int PICK_LADDER_BUTTON = 312;
	/** Pit stake: less / more deep. */
	public static final int DEPTH_LESS_BUTTON = 313;
	public static final int DEPTH_MORE_BUTTON = 314;
	/** Mining bench: closes the menu, the player then right-clicks the side of a block where the tunnel starts. */
	public static final int PICK_TUNNEL_BUTTON = 315;
	/** Mining bench: shorter / longer tunnel. */
	public static final int LENGTH_LESS_BUTTON = 316;
	public static final int LENGTH_MORE_BUTTON = 317;
	/** Mining bench: shorter / longer side tunnels (0: none). */
	public static final int BRANCHES_LESS_BUTTON = 318;
	public static final int BRANCHES_MORE_BUTTON = 319;
	/** Mining bench: switches between a tunnel and stairs down. */
	public static final int STAIRS_BUTTON = 320;
	/** Loom: fewer / more sheep the shepherd breeds (0: none). */
	public static final int HERD_LESS_BUTTON = 321;
	public static final int HERD_MORE_BUTTON = 322;
	/** Barrel: turns rowing out in a boat on or off. */
	public static final int BOAT_BUTTON = 323;
	/** Target: switches between standing watch and hunting. */
	public static final int HUNT_BUTTON = 324;
	/** Cauldron: the previous and next color for leather armor (none, then the 16 dyes). */
	public static final int DYE_LESS_BUTTON = 325;
	public static final int DYE_MORE_BUTTON = 326;
	/** Grindstone: the previous and next of what the weaponsmith does besides its order. */
	public static final int GRIND_LESS_BUTTON = 327;
	public static final int GRIND_MORE_BUTTON = 328;
	/** Lectern: the level the librarian enchants at, lower and higher (0 is the highest the table offers). */
	public static final int ENCHANT_LESS_BUTTON = 329;
	public static final int ENCHANT_MORE_BUTTON = 330;
	/** The steps of the side tunnel length the buttons go through. */
	private static final int BRANCH_STEP = 4;
	/**
	 * Stonecutter: plus the index of the product in all families one after another, adds it
	 * to the order (or takes it out if it is in it already).
	 */
	public static final int STONE_PRODUCT_BUTTON = 1000;
	/**
	 * Kiln and the other product grids: plus the index of the product, adds it to the order
	 * (or takes it out if it is in it already; a kiln without products fires everything).
	 * The brewing stand orders only one.
	 */
	public static final int KILN_PRODUCT_BUTTON = 2000;
	/**
	 * Plus the index of the product in the order: fewer / more of it, ordered once or kept in
	 * stock; switches between ordering it once and keeping it in stock.
	 */
	public static final int AMOUNT_LESS_BUTTON = 5000;
	public static final int AMOUNT_MORE_BUTTON = 5100;
	public static final int AMOUNT_MODE_BUTTON = 5200;
	/** Carpenter's bench: at most this many products are shown (three rows). */
	public static final int MAX_PRODUCTS = 27;
	/** The stock levels the buttons step through, 0 means no limit. */
	private static final int[] STOCK_STEPS = {0, 1, 2, 4, 8, 12, 16, 24, 32, 48, 64, 96, 128, 192, 256};
	/** How many of a product ordered once the buttons step through. */
	private static final int[] BATCH_STEPS = {1, 2, 4, 8, 12, 16, 24, 32, 48, 64, 96, 128, 192, 256, 384, 512, 768, 1024};

	private static final String TAG_STATION = "Station";
	private static final String TAG_WORKER = "Worker";
	private static final String TAG_LOGS = "Logs";
	/** The server's settings the screen shows: logs per day, chests per station. */
	private static final String TAG_DAILY_LOGS = "DailyLogs";
	private static final String TAG_MAX_CHESTS = "MaxChests";
	private static final String TAG_STATUS = "Status";
	/** Whether the worker is a hired villager (not a mason), so it can be taken away. */
	private static final String TAG_HIRED_WORKER = "HiredWorker";
	private static final String TAG_CHESTS = "Chests";
	private static final String TAG_DAILY_WAGE = "DailyWage";
	/** The worker's level and villager experience, and what its next level unlocks. */
	private static final String TAG_LEVEL = "Level";
	private static final String TAG_XP = "Xp";
	private static final String TAG_NEXT_UNLOCKS = "NextUnlocks";
	/** The products of the order grid a worker makes only from a level on: key (item id or potion key) to level. */
	private static final String TAG_LOCKS = "Locks";
	private static final String TAG_NAME = "Name";
	private static final String TAG_NAMED = "Named";
	private static final String TAG_POS = "Pos";
	private static final String TAG_USED = "Used";
	private static final String TAG_SIZE = "Size";
	private static final String TAG_INPUT = "Input";
	private static final String TAG_ORDERS = "Orders";
	private static final String TAG_WOODS = "Woods";
	private static final String TAG_AVAILABLE = "Available";
	private static final String TAG_PRODUCTS = "Products";
	/** Per product of the order: whether it is ordered once, how many (or the stock), how many are left. */
	private static final String TAG_AMOUNTS = "Amounts";
	private static final String TAG_STRIP = "Strip";
	private static final String TAG_FAMILIES = "Families";
	private static final String TAG_PIT = "Pit";
	private static final String TAG_PIT_PROBLEM = "PitProblem";
	/** How often (in ticks) an open pit stake menu looks at the ring again. */
	private static final int RING_INTERVAL = 10;
	private static final String TAG_TUNNEL = "Tunnel";
	private static final String TAG_BRANCHES = "Branches";
	private static final String TAG_STAIRS = "Stairs";
	private static final String TAG_MAX_LENGTH = "MaxLength";
	private static final String TAG_MAX_BRANCHES = "MaxBranches";
	private static final String TAG_FACING = "Facing";
	private static final String TAG_WIDTH = "Width";
	private static final String TAG_LENGTH = "Length";
	private static final String TAG_AREA = "Area";
	private static final String TAG_DEPTH = "Depth";
	private static final String TAG_MAX_DEPTH = "MaxDepth";
	private static final String TAG_MAX_AREA = "MaxArea";
	private static final String TAG_STOREYS = "Storeys";
	private static final String TAG_PLANKS = "Planks";
	private static final String TAG_PILLAR_LOGS = "PillarLogs";
	private static final String TAG_HERD = "Herd";
	private static final String TAG_TAB_FLAGS = "TabFlags";
	private static final String TAG_BOAT = "Boat";
	private static final String TAG_HUNT = "Hunt";
	private static final String TAG_EXPERIENCE = "Experience";
	private static final String TAG_DYE = "Dye";
	private static final String TAG_GRIND = "Grind";
	private static final String TAG_ENCHANT_LEVEL = "EnchantLevel";
	private static final String TAG_BOOKSHELVES = "Bookshelves";
	/** Lectern: the level it enchants at now, and what one piece costs. */
	private static final String TAG_LEVEL_NOW = "LevelNow";
	private static final String TAG_BOTTLES = "Bottles";
	private static final String TAG_LAPIS = "Lapis";
	private static final String TAG_TANNING = "Tanning";
	private static final String TAG_TANNED_IN = "TannedIn";
	private static final String TAG_SHEEP = "Sheep";

	/** Wage tab: the slot of the wage box, and the top left of the player's inventory. */
	public static final int WAGE_X = 8;
	public static final int WAGE_Y = 32;
	public static final int INVENTORY_Y = 84;
	private static final int WAGE_SLOT = 1;
	private static final int INVENTORY_START = 2;
	private static final int INVENTORY_END = INVENTORY_START + 36;

	private final StationType type;
	private final ContainerLevelAccess access;
	private final InfoSlot info = new InfoSlot();
	/** Client: whether the wage tab is open, the only one that shows the wage box and the inventory. */
	private boolean slotsShown;
	/** Server only: the chests as last sent, the buttons refer to them by index. */
	private List<BlockPos> shownChests = List.of();
	/** Server only, carpenter's bench: the products as last sent. */
	private List<Item> shownProducts = List.of();
	/** Brewing stand: the products of the grid, potions with their potion. */
	private List<ItemStack> shownStacks = List.of();
	/** Server only, stonecutter: the families, computed once; the buttons refer to their products. */
	private @Nullable List<StoneFamilies.Family> families;
	/** Client, stonecutter: the info the families were read from, to read them only once per update. */
	private @Nullable ItemStack familiesFrom;
	private List<StoneFamilies.Family> clientFamilies = List.of();
	/** Server side: what the products of the order grid need, computed once (see {@link #locks}). */
	private @Nullable CompoundTag locks;
	/** Server side, pit stake: why the ring at the stake is no pit, and when it was last looked at. */
	private Optional<Component> pitProblem = Optional.empty();
	private long ringCheckedAt = Long.MIN_VALUE / 2;

	/** Client side. */
	public StationMenu(StationType type, int containerId, Inventory inventory) {
		this(type, containerId, inventory, ContainerLevelAccess.NULL);
	}

	public StationMenu(StationType type, int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(menuType(type).get(), containerId);
		this.type = type;
		this.access = access;
		this.addSlot(this.info);
		boolean client = access == ContainerLevelAccess.NULL;
		Container box = access.evaluate((level, pos) -> level instanceof ServerLevel serverLevel
				? Workstations.find(serverLevel, pos).map(station -> (Container) new WageBox(station)).orElse(null)
				: null)
			.orElseGet(() -> new SimpleContainer(1));
		this.addSlot(new TabSlot(box, 0, WAGE_X, WAGE_Y, client) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(Items.EMERALD);
			}

			@Override
			public int getMaxStackSize() {
				return Workstations.MAX_WAGE;
			}
		});
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				this.addSlot(new TabSlot(inventory, column + row * 9 + 9, 8 + column * 18, INVENTORY_Y + row * 18, client));
			}
		}
		for (int column = 0; column < 9; column++) {
			this.addSlot(new TabSlot(inventory, column, 8 + column * 18, INVENTORY_Y + 58, client));
		}
		this.updateInfo();
	}

	/** A slot of the wage tab: on the client only there while that tab is open. */
	private class TabSlot extends Slot {
		private final boolean client;

		TabSlot(Container container, int index, int x, int y, boolean client) {
			super(container, index, x, y);
			this.client = client;
		}

		@Override
		public boolean isActive() {
			return !this.client || StationMenu.this.slotsShown;
		}
	}

	/** Client: the emeralds a worker takes per work day (0: workers work for free). */
	public int getDailyWage() {
		return this.info.info().getIntOr(TAG_DAILY_WAGE, 1);
	}

	/** Client: the worker's level, 0 without a hired worker. */
	public int getLevel() {
		return this.info.info().getIntOr(TAG_LEVEL, 0);
	}

	/** Client: the worker's villager experience. */
	public int getXp() {
		return this.info.info().getIntOr(TAG_XP, 0);
	}

	/** Client: what the worker's next level unlocks, if anything. */
	public Optional<Component> getNextUnlocks() {
		return this.info.info().read(TAG_NEXT_UNLOCKS, ComponentSerialization.CODEC);
	}

	/** Client: the emeralds in the wage box. */
	public int getWageBox() {
		return this.slots.get(WAGE_SLOT).getItem().getCount();
	}

	/** Client: shows or hides the wage box and the inventory (the wage tab is open or not). */
	public void showSlots(boolean shown) {
		this.slotsShown = shown;
	}

	private static Supplier<MenuType<StationMenu>> menuType(StationType type) {
		return switch (type) {
			case CHOPPING_BLOCK -> WorkingVillagers.CHOPPING_BLOCK_MENU;
			case WOODCUTTER -> WorkingVillagers.WOODCUTTER_STATION_MENU;
			case STONECUTTER -> WorkingVillagers.STONECUTTER_STATION_MENU;
			case CARPENTER_BENCH -> WorkingVillagers.CARPENTER_STATION_MENU;
			case COMPOSTER -> WorkingVillagers.COMPOSTER_STATION_MENU;
			case BLAST_FURNACE -> WorkingVillagers.BLAST_FURNACE_STATION_MENU;
			case SMITHING_TABLE -> WorkingVillagers.SMITHING_TABLE_STATION_MENU;
			case LOOM -> WorkingVillagers.LOOM_STATION_MENU;
			case SMOKER -> WorkingVillagers.SMOKER_STATION_MENU;
			case BARREL -> WorkingVillagers.BARREL_STATION_MENU;
			case FLETCHING_TABLE -> WorkingVillagers.FLETCHING_TABLE_STATION_MENU;
			case BREWING_STAND -> WorkingVillagers.BREWING_STAND_STATION_MENU;
			case CAULDRON -> WorkingVillagers.CAULDRON_STATION_MENU;
			case GRINDSTONE -> WorkingVillagers.GRINDSTONE_STATION_MENU;
			case LECTERN -> WorkingVillagers.LECTERN_STATION_MENU;
			case TARGET -> WorkingVillagers.TARGET_STATION_MENU;
			case PIT_STAKE -> WorkingVillagers.PIT_STAKE_MENU;
			case KILN -> WorkingVillagers.KILN_STATION_MENU;
			case TRADING_COUNTER -> WorkingVillagers.TRADING_COUNTER_MENU;
			case MINING_BENCH -> WorkingVillagers.MINING_BENCH_MENU;
		};
	}

	/** Opens the overview of the workstation at the position. */
	public static MenuProvider provider(StationType type, Level level, BlockPos pos) {
		return new SimpleMenuProvider(
			(containerId, inventory, player) -> new StationMenu(type, containerId, inventory, ContainerLevelAccess.create(level, pos)),
			type.displayName()
		);
	}

	public StationType type() {
		return this.type;
	}

	private void updateInfo() {
		this.access.execute((level, pos) -> {
			if (!(level instanceof ServerLevel serverLevel)) {
				return;
			}
			Optional<Workstations.Station> found = Workstations.find(serverLevel, pos);
			if (found.isEmpty()) {
				return;
			}
			Workstations.Station station = found.get();
			if (this.type == StationType.PIT_STAKE && serverLevel.getGameTime() - this.ringCheckedAt >= RING_INTERVAL) {
				// The ring at the stake is the pit; while the menu is open, changes to it show up
				this.ringCheckedAt = serverLevel.getGameTime();
				this.pitProblem = Pit.attach(serverLevel, station).map(Pit.Problem::message);
			}
			CompoundTag tag = new CompoundTag();
			tag.store(TAG_STATION, BlockPos.CODEC, pos);
			tag.putInt(TAG_DAILY_LOGS, switch (this.type) {
				case PIT_STAKE -> Config.PIT_DAILY_BLOCKS.get();
				case MINING_BENCH -> Config.MINER_DAILY_BLOCKS.get();
				default -> Config.DAILY_LOGS.get();
			});
			tag.putInt(TAG_MAX_CHESTS, Config.MAX_CHESTS.get());
			tag.putInt(TAG_DAILY_WAGE, WorkerLevels.dailyWage(WorkerLevels.NOVICE));
			station.occupant(serverLevel).ifPresent(name -> {
				tag.store(TAG_WORKER, ComponentSerialization.CODEC, name);
				if (station.worker() != null && serverLevel.getEntity(station.worker()) instanceof Villager villager) {
					tag.putBoolean(TAG_HIRED_WORKER, true);
					tag.putInt(TAG_DAILY_WAGE, WorkerLevels.dailyWage(villager));
					tag.putInt(TAG_LEVEL, villager.getVillagerData().level());
					tag.putInt(TAG_XP, villager.getVillagerXp());
					Unlocks.next(villager).ifPresent(next -> tag.store(TAG_NEXT_UNLOCKS, ComponentSerialization.CODEC, next));
					tag.store(TAG_STATUS, ComponentSerialization.CODEC, WorkerStatus.describe(villager));
					SuppliesInfo.write(serverLevel, villager, tag);
					if (this.type == StationType.CHOPPING_BLOCK || this.type == StationType.COMPOSTER || this.type == StationType.PIT_STAKE
						|| this.type == StationType.TRADING_COUNTER || this.type == StationType.MINING_BENCH || this.type == StationType.LOOM
						|| this.type == StationType.SMOKER || this.type == StationType.BARREL || this.type == StationType.FLETCHING_TABLE
						|| this.type == StationType.BREWING_STAND || this.type == StationType.CAULDRON
						|| this.type == StationType.GRINDSTONE || this.type == StationType.LECTERN
						|| this.type == StationType.TARGET) {
						tag.putInt(TAG_LOGS, ((HiredVillager) villager).workingvillagers$getWorkDone(
							this.type == StationType.TARGET ? GuardPost.night(serverLevel) : ChopTrees.today(serverLevel)));
					}
				}
			});

			List<BlockPos> chests = new ArrayList<>(station.validInputs(serverLevel));
			chests.addAll(station.validOutputs(serverLevel));
			// A fixed order, so switching the role does not move the row
			chests.sort(null);
			this.shownChests = chests;
			ListTag chestTags = new ListTag();
			for (BlockPos chest : chests) {
				Container container = Workstations.getContainer(serverLevel, chest);
				CompoundTag entry = new CompoundTag();
				entry.store(TAG_POS, BlockPos.CODEC, chest);
				entry.store(TAG_NAME, ComponentSerialization.CODEC, ContainerNames.name(serverLevel, chest));
				entry.putBoolean(TAG_NAMED, ContainerNames.customName(serverLevel, chest) != null);
				entry.putInt(TAG_USED, container == null ? 0 : usedSlots(container));
				entry.putInt(TAG_SIZE, container == null ? 0 : container.getContainerSize());
				entry.putBoolean(TAG_INPUT, station.role(chest) == ChestRole.INPUT);
				chestTags.add(entry);
			}
			tag.put(TAG_CHESTS, chestTags);

			if (this.type == StationType.CHOPPING_BLOCK) {
				tag.putBoolean(TAG_STRIP, station.strip());
			}
			if (this.type == StationType.BARREL) {
				tag.putBoolean(TAG_BOAT, station.boat());
			}
			if (this.type == StationType.TARGET) {
				tag.putInt(TAG_HERD, station.guardRadius());
				tag.putBoolean(TAG_HUNT, station.hunting());
				if (station.worker() != null && serverLevel.getEntity(station.worker()) instanceof HiredVillager guard) {
					tag.putInt(TAG_EXPERIENCE, guard.workingvillagers$getExperience());
				}
			}
			tag.putInt(TAG_TAB_FLAGS, StationTabFlags.compute(serverLevel, pos, this.type));
			if (this.type == StationType.LOOM || this.type == StationType.SMOKER) {
				tag.putInt(TAG_HERD, station.herd());
				tag.putInt(TAG_SHEEP, this.type == StationType.LOOM ? TendSheep.herd(serverLevel, pos).size() : TendAnimals.herd(serverLevel, pos).size());
			}
			if (this.type == StationType.PIT_STAKE) {
				// What the pit digger at the stake may dig (a novice's without one)
				PitLimits limits = PitLimits.at(serverLevel, station);
				tag.putInt(TAG_DEPTH, Math.min(station.depth(), limits.depth()));
				tag.putInt(TAG_MAX_DEPTH, limits.depth());
				tag.putInt(TAG_MAX_AREA, limits.area());
				Pit pit = station.pit();
				this.pitProblem.ifPresent(problem -> tag.store(TAG_PIT_PROBLEM, ComponentSerialization.CODEC, problem));
				if (pit != null) {
					CompoundTag pitTag = new CompoundTag();
					pitTag.putInt(TAG_WIDTH, pit.width());
					pitTag.putInt(TAG_LENGTH, pit.length());
					pitTag.putInt(TAG_AREA, pit.inside().size());
					// What the storeys below the novice depth take
					Pit.Storeys storeys = pit.storeys(serverLevel, Math.min(station.depth(), limits.depth()));
					pitTag.putInt(TAG_STOREYS, storeys.floors().size());
					pitTag.putInt(TAG_PLANKS, storeys.planks());
					pitTag.putInt(TAG_PILLAR_LOGS, storeys.logs());
					tag.put(TAG_PIT, pitTag);
				}
			}
			if (this.type == StationType.MINING_BENCH) {
				tag.putInt(TAG_MAX_LENGTH, Config.TUNNEL_MAX_LENGTH.get());
				tag.putInt(TAG_MAX_BRANCHES, Config.TUNNEL_MAX_BRANCH.get());
				Tunnel tunnel = station.tunnel();
				if (tunnel != null) {
					CompoundTag tunnelTag = new CompoundTag();
					tunnelTag.putInt(TAG_LENGTH, tunnel.clampedLength());
					tunnelTag.putInt(TAG_BRANCHES, tunnel.clampedBranches());
					tunnelTag.putBoolean(TAG_STAIRS, tunnel.stairs());
					tunnelTag.putString(TAG_FACING, tunnel.direction().getSerializedName());
					tag.put(TAG_TUNNEL, tunnelTag);
				}
			}
			ListTag orders = new ListTag();
			if (this.type == StationType.BREWING_STAND) {
				if (station.order() != null) {
					orders.add(StringTag.valueOf(BrewingPaths.key(BrewingPaths.order(station.order(), station.potion()))));
				}
			} else {
				station.orders().forEach(order -> orders.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(order).toString())));
			}
			tag.put(TAG_ORDERS, orders);
			ListTag amounts = new ListTag();
			for (Item order : station.orders()) {
				Workstations.Station.Batch batch = station.batch(order);
				amounts.add(new IntArrayTag(batch != null ? new int[] {1, batch.amount(), batch.left()} : new int[] {0, station.stock(order), 0}));
			}
			tag.put(TAG_AMOUNTS, amounts);
			if (this.type.hasWoods()) {
				ListTag woods = new ListTag();
				station.woods().forEach(wood -> woods.add(StringTag.valueOf(wood)));
				tag.put(TAG_WOODS, woods);
				ListTag available = new ListTag();
				for (WoodTypes.WoodType wood : WoodTypes.ALL) {
					if (this.canMake(serverLevel, wood, station.shapes())) {
						available.add(StringTag.valueOf(wood.name()));
					}
				}
				tag.put(TAG_AVAILABLE, available);
			}
			if (this.type == StationType.STONECUTTER) {
				if (this.families == null) {
					this.families = StoneFamilies.of(serverLevel);
				}
				// Item ids, as the families are sent again with every change of the info
				ListTag families = new ListTag();
				for (StoneFamilies.Family family : this.families) {
					int[] ids = new int[family.products().size() + 1];
					ids[0] = BuiltInRegistries.ITEM.getId(family.icon());
					for (int i = 0; i < family.products().size(); i++) {
						ids[i + 1] = BuiltInRegistries.ITEM.getId(family.products().get(i));
					}
					families.add(new IntArrayTag(ids));
				}
				tag.put(TAG_FAMILIES, families);
			}
			if (this.type == StationType.CARPENTER_BENCH) {
				this.shownProducts = benchProducts(serverLevel);
				ListTag products = new ListTag();
				this.shownProducts.forEach(product -> products.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(product).toString())));
				tag.put(TAG_PRODUCTS, products);
			}
			if (this.type == StationType.CAULDRON) {
				if (station.dye() != null) {
					tag.putString(TAG_DYE, station.dye().getSerializedName());
				}
				tag.putInt(TAG_TANNING, station.tanning());
				tag.putInt(TAG_TANNED_IN, (int) Math.max(0L, station.tannedAt() - serverLevel.getGameTime()));
			}
			if (this.type == StationType.GRINDSTONE) {
				tag.putInt(TAG_GRIND, station.grindMode());
			}
			if (this.type == StationType.LECTERN) {
				tag.putInt(TAG_ENCHANT_LEVEL, station.enchantLevel());
				BlockPos table = EnchantItems.table(serverLevel, pos);
				int shelves = table == null ? -1 : EnchantItems.bookshelves(serverLevel, table);
				int highest = EnchantItems.highestLevel(Math.max(0, shelves));
				if (station.worker() != null && serverLevel.getEntity(station.worker()) instanceof Villager librarian) {
					highest = Math.min(highest, Unlocks.enchantingLevel(librarian));
				}
				int now = station.enchantLevel() == 0 ? highest : Math.min(station.enchantLevel(), highest);
				tag.putInt(TAG_BOOKSHELVES, shelves);
				tag.putInt(TAG_LEVEL_NOW, now);
				tag.putInt(TAG_BOTTLES, EnchantItems.bottlesFor(now));
				tag.putInt(TAG_LAPIS, EnchantItems.lapisFor(now));
			}
			if (this.type == StationType.KILN || this.type == StationType.FLETCHING_TABLE || this.type == StationType.CAULDRON
				|| this.type == StationType.GRINDSTONE) {
				this.shownProducts = switch (this.type) {
					case KILN -> KilnRecipes.products(serverLevel);
					case CAULDRON -> TanLeather.PRODUCTS;
					case GRINDSTONE -> ForgeRecipes.products(serverLevel);
					default -> FletchArrows.PRODUCTS;
				};
				ListTag products = new ListTag();
				this.shownProducts.forEach(product -> products.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(product).toString())));
				tag.put(TAG_PRODUCTS, products);
			}
			if (this.type == StationType.BREWING_STAND) {
				this.shownStacks = BrewingPaths.products(serverLevel);
				ListTag products = new ListTag();
				this.shownStacks.forEach(product -> products.add(StringTag.valueOf(BrewingPaths.key(product))));
				tag.put(TAG_PRODUCTS, products);
			}
			if (this.locks == null) {
				// Recipes and tags do not change while the menu is open
				this.locks = this.locks(serverLevel);
			}
			tag.put(TAG_LOCKS, this.locks.copy());
			this.info.setInfo(tag);
		});
	}

	/** The products shown in the order grid that a worker makes only from a level on (see {@link Unlocks}). */
	private CompoundTag locks(ServerLevel level) {
		CompoundTag locks = new CompoundTag();
		if (!Config.UNLOCKS.get()) {
			return locks;
		}
		ResourceKey<VillagerProfession> profession = this.type.profession();
		List<Item> items = new ArrayList<>(switch (this.type) {
			case WOODCUTTER -> WoodTypes.SHAPES.stream().map(shape -> WoodTypes.product(WoodTypes.ALL.getFirst(), shape)).filter(Objects::nonNull).toList();
			case STONECUTTER -> this.families == null ? List.<Item>of() : this.families.stream().flatMap(family -> family.products().stream()).toList();
			case CARPENTER_BENCH, KILN, FLETCHING_TABLE, CAULDRON, GRINDSTONE -> this.shownProducts;
			default -> List.<Item>of();
		});
		for (Item item : items) {
			int needed = switch (this.type) {
				case WOODCUTTER -> Unlocks.level(profession, item);
				case STONECUTTER -> CuttingRecipes.forResult(level, RecipeType.STONECUTTING, item).stream()
					.mapToInt(recipe -> Unlocks.level(profession, item, List.of(recipe.input()))).min().orElse(WorkerLevels.NOVICE);
				case CARPENTER_BENCH -> CarpenterRecipes.forResult(level, item).stream()
					.mapToInt(recipe -> Unlocks.level(profession, item, recipe.placementInfo().ingredients())).min().orElse(WorkerLevels.NOVICE);
				case GRINDSTONE -> ForgeRecipes.forResult(level, item).stream()
					.mapToInt(recipe -> Unlocks.level(profession, item, recipe.placementInfo().ingredients())).min().orElse(WorkerLevels.NOVICE);
				default -> Unlocks.level(profession, item);
			};
			if (needed > WorkerLevels.NOVICE) {
				locks.putInt(BuiltInRegistries.ITEM.getKey(item).toString(), needed);
			}
		}
		if (this.type == StationType.BREWING_STAND) {
			for (ItemStack product : this.shownStacks) {
				List<BrewingPaths.Step> steps = BrewingPaths.path(level, product);
				int needed = steps == null ? WorkerLevels.NOVICE : steps.stream()
					.mapToInt(step -> Unlocks.level(profession, step.shownReagent().getItem())).max().orElse(WorkerLevels.NOVICE);
				if (needed > WorkerLevels.NOVICE) {
					locks.putInt(BrewingPaths.key(product), needed);
				}
			}
		}
		return locks;
	}

	/** Client: from which level a worker makes the product of the order grid (a novice's if it is free). */
	public int getLockLevel(ItemStack product) {
		CompoundTag locks = this.info.info().getCompoundOrEmpty(TAG_LOCKS);
		String key = this.type == StationType.BREWING_STAND ? BrewingPaths.key(product) : BuiltInRegistries.ITEM.getKey(product.getItem()).toString();
		return locks.getIntOr(key, WorkerLevels.NOVICE);
	}

	/** Whether the station has a recipe for one of the shapes in that wood type (any wood type will do without shapes). */
	private boolean canMake(ServerLevel level, WoodTypes.WoodType wood, List<String> shapes) {
		if (shapes.isEmpty()) {
			return true;
		}
		for (String shape : shapes) {
			if (WoodTypes.STICK.equals(shape)) {
				return true;
			}
			Item product = WoodTypes.product(wood, shape);
			if (product != null && (this.type == StationType.CARPENTER_BENCH
				? !CarpenterRecipes.forResult(level, product).isEmpty()
				: !CuttingRecipes.forResult(level, WorkingVillagers.WOODCUTTING.get(), product).isEmpty())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The products to pick from at the carpenter's bench: products made of one wood type
	 * once per shape (shown in the first wood type that has it), every other product as it is.
	 */
	private static List<Item> benchProducts(ServerLevel level) {
		List<Item> products = new ArrayList<>();
		List<String> shapes = new ArrayList<>();
		for (Item product : CarpenterRecipes.products(level)) {
			String shape = WoodTypes.shapeOf(product, WoodTypes.CARPENTER_SHAPES);
			if (shape == null) {
				products.add(product);
			} else if (!shapes.contains(shape)) {
				shapes.add(shape);
				Item icon = WoodTypes.ALL.stream()
					.map(wood -> WoodTypes.product(wood, shape))
					.filter(item -> item != null && !CarpenterRecipes.forResult(level, item).isEmpty())
					.findFirst()
					.orElse(product);
				products.add(icon);
			}
		}
		return products.stream().limit(MAX_PRODUCTS).toList();
	}

	private static int usedSlots(Container container) {
		int used = 0;
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			if (!container.getItem(slot).isEmpty()) {
				used++;
			}
		}
		return used;
	}

	@Override
	public void broadcastChanges() {
		this.updateInfo();
		super.broadcastChanges();
	}

	public Optional<BlockPos> getStationPos() {
		return this.info.info().read(TAG_STATION, BlockPos.CODEC);
	}

	/** The name of the worker, empty if the station is free. */
	public Optional<Component> getWorker() {
		return this.info.info().read(TAG_WORKER, ComponentSerialization.CODEC);
	}

	/** Whether the worker can be taken away here (a hired villager that is nearby). */
	public boolean hasHiredWorker() {
		return this.info.info().getBooleanOr(TAG_HIRED_WORKER, false);
	}

	/** What the worker is doing right now, empty if unknown. */
	public Optional<Component> getStatus() {
		return this.info.info().read(TAG_STATUS, ComponentSerialization.CODEC);
	}

	/** The wage and tool of the worker, empty if the station has no hired worker. */
	public Optional<SuppliesInfo> getSupplies() {
		return SuppliesInfo.read(this.info.info());
	}

	/**
	 * Logs felled today at a chopping block, crops harvested today at a composter, blocks dug
	 * today at a pit stake, emeralds earned today at a trading counter, -1 if unknown (no
	 * worker or not loaded).
	 */
	public int getLogsToday() {
		return this.info.info().getIntOr(TAG_LOGS, -1);
	}

	public int getDailyLogs() {
		return this.info.info().getIntOr(TAG_DAILY_LOGS, 64);
	}

	public int getMaxChests() {
		return this.info.info().getIntOr(TAG_MAX_CHESTS, Workstations.MAX_CHESTS);
	}

	public List<Chest> getChests() {
		List<Chest> chests = new ArrayList<>();
		for (CompoundTag entry : this.info.info().getListOrEmpty(TAG_CHESTS).compoundStream().toList()) {
			entry.read(TAG_POS, BlockPos.CODEC).ifPresent(pos -> chests.add(
				new Chest(pos, entry.read(TAG_NAME, ComponentSerialization.CODEC).orElse(Component.empty()), entry.getBooleanOr(TAG_NAMED, false),
					entry.getIntOr(TAG_USED, 0), entry.getIntOr(TAG_SIZE, 0), entry.getBooleanOr(TAG_INPUT, false))
			));
		}
		return chests;
	}

	/** The products the worker makes, in the order they were picked; empty if there is no order. */
	public List<ItemStack> getOrders() {
		return this.info.info().getListOrEmpty(TAG_ORDERS).stream()
			.flatMap(tag -> tag.asString().stream())
			.map(BrewingPaths::fromKey)
			.filter(stack -> !stack.isEmpty())
			.toList();
	}

	/** The first product of the order, empty if there is none. */
	public ItemStack getOrder() {
		List<ItemStack> orders = this.getOrders();
		return orders.isEmpty() ? ItemStack.EMPTY : orders.getFirst();
	}

	/** Whether the product is in the order. */
	public boolean isOrdered(ItemStack product) {
		return this.getOrders().stream().anyMatch(order -> ItemStack.isSameItemSameComponents(order, product));
	}

	/** Woodcutter and carpenter's bench: the shapes in the order. */
	public List<String> getShapes() {
		return this.getOrders().stream().map(order -> WoodTypes.shapeOf(order.getItem(), this.type.shapes())).filter(Objects::nonNull).distinct().toList();
	}

	/**
	 * Carpenter's bench: the products to pick from, one per shape for those made of one wood
	 * type. Kiln: everything it makes.
	 */
	public List<ItemStack> getProducts() {
		return this.info.info().getListOrEmpty(TAG_PRODUCTS).stream()
			.flatMap(tag -> tag.asString().stream())
			.map(BrewingPaths::fromKey)
			.filter(stack -> !stack.isEmpty())
			.toList();
	}

	/** Stonecutter: the families of products to pick the order from. */
	public List<StoneFamilies.Family> getFamilies() {
		if (this.info.getItem() != this.familiesFrom) {
			this.familiesFrom = this.info.getItem();
			List<StoneFamilies.Family> families = new ArrayList<>();
			for (Tag entry : this.info.info().getListOrEmpty(TAG_FAMILIES)) {
				if (entry instanceof IntArrayTag ids && ids.size() > 1) {
					int[] array = ids.getAsIntArray();
					List<Item> products = new ArrayList<>();
					for (int i = 1; i < array.length; i++) {
						products.add(BuiltInRegistries.ITEM.byId(array[i]));
					}
					families.add(new StoneFamilies.Family(BuiltInRegistries.ITEM.byId(array[0]), List.copyOf(products)));
				}
			}
			this.clientFamilies = List.copyOf(families);
		}
		return this.clientFamilies;
	}

	/** Pit stake: the size of the picked pit (width, length, blocks of ground), empty if none. */
	public Optional<int[]> getPit() {
		return this.info.info().getCompound(TAG_PIT).map(pit -> new int[] {pit.getIntOr(TAG_WIDTH, 0), pit.getIntOr(TAG_LENGTH, 0), pit.getIntOr(TAG_AREA, 0)});
	}

	/** Pit stake: the floors of the pit and the planks and logs they take, empty without floors. */
	public Optional<int[]> getStoreys() {
		return this.info.info().getCompound(TAG_PIT).filter(pit -> pit.getIntOr(TAG_STOREYS, 0) > 0)
			.map(pit -> new int[] {pit.getIntOr(TAG_STOREYS, 0), pit.getIntOr(TAG_PLANKS, 0), pit.getIntOr(TAG_PILLAR_LOGS, 0)});
	}

	/** Pit stake: why the ring at the stake is no pit, empty if it is one. */
	public Optional<Component> getPitProblem() {
		return this.info.info().read(TAG_PIT_PROBLEM, ComponentSerialization.CODEC);
	}

	/** Mining bench: the picked tunnel, empty if none. */
	public Optional<TunnelInfo> getTunnel() {
		return this.info.info().getCompound(TAG_TUNNEL).map(tunnel -> new TunnelInfo(
			tunnel.getIntOr(TAG_LENGTH, Tunnel.DEFAULT_LENGTH), tunnel.getIntOr(TAG_BRANCHES, 0), tunnel.getBooleanOr(TAG_STAIRS, false),
			tunnel.getStringOr(TAG_FACING, "north")
		));
	}

	public int getMaxTunnelLength() {
		return this.info.info().getIntOr(TAG_MAX_LENGTH, 32);
	}

	public int getMaxBranches() {
		return this.info.info().getIntOr(TAG_MAX_BRANCHES, 16);
	}

	/** Mining bench: what the screen shows about the tunnel ({@code facing}: the direction it goes, e.g. "north"). */
	public record TunnelInfo(int length, int branches, boolean stairs, String facing) {
	}

	/** Pit stake: how deep to dig. */
	public int getDepth() {
		return this.info.info().getIntOr(TAG_DEPTH, Pit.DEFAULT_DEPTH);
	}

	public int getMaxDepth() {
		return this.info.info().getIntOr(TAG_MAX_DEPTH, 8);
	}

	/** Pit stake: how many blocks of ground the pit may have at most. */
	public int getMaxArea() {
		return this.info.info().getIntOr(TAG_MAX_AREA, 64);
	}

	/**
	 * How many the worker makes of the product of the order at the index: ordered once (with
	 * how many are left) or kept in stock (0: no limit).
	 */
	public Amount getAmount(int index) {
		ListTag amounts = this.info.info().getListOrEmpty(TAG_AMOUNTS);
		if (index < 0 || index >= amounts.size() || !(amounts.get(index) instanceof IntArrayTag tag) || tag.size() < 3) {
			return new Amount(false, 0, 0);
		}
		int[] values = tag.getAsIntArray();
		return new Amount(values[0] == 1, values[1], values[2]);
	}

	/** See {@link #getAmount}: {@code amount} is the stock if the product is not ordered once. */
	public record Amount(boolean batch, int amount, int left) {
	}

	/** What the tabs show, see {@link StationTabFlags}. */
	public int getTabFlags() {
		return this.info.info().getIntOr(TAG_TAB_FLAGS, 0);
	}

	/** Loom and smoker: up to how many sheep (of each kind of animal) the worker breeds, 0 for none. */
	public int getHerd() {
		return this.info.info().getIntOr(TAG_HERD, Workstations.DEFAULT_HERD);
	}

	/** Loom: how many sheep there are around it; smoker: how many animals the butcher keeps. */
	public int getSheep() {
		return this.info.info().getIntOr(TAG_SHEEP, 0);
	}

	/** Target: whether the guard hunts instead of standing watch. */
	/** Cauldron: the color the tanner dyes leather armor, null for none. */
	public @Nullable DyeColor getDye() {
		return this.info.info().getString(TAG_DYE).map(name -> DyeColor.byName(name, null)).orElse(null);
	}

	/** Lectern: the level the librarian enchants at, 0 for the highest the table offers. */
	public int getEnchantLevel() {
		return this.info.info().getIntOr(TAG_ENCHANT_LEVEL, 0);
	}

	/** Lectern: the level it enchants at now (the set one, at most what the table offers), and what a piece costs. */
	public int getLevelNow() {
		return this.info.info().getIntOr(TAG_LEVEL_NOW, 0);
	}

	public int getBottles() {
		return this.info.info().getIntOr(TAG_BOTTLES, 0);
	}

	public int getLapis() {
		return this.info.info().getIntOr(TAG_LAPIS, 0);
	}

	/** Lectern: the bookshelves around the enchanting table, -1 if there is no table. */
	public int getBookshelves() {
		return this.info.info().getIntOr(TAG_BOOKSHELVES, -1);
	}

	/** Grindstone: what the weaponsmith does besides its order, see {@link Workstations.Station#grindMode}. */
	public int getGrindMode() {
		return this.info.info().getIntOr(TAG_GRIND, Workstations.GRIND_BOTH);
	}

	/** Cauldron: how much rotten flesh tans in it, and in how many ticks it is done. */
	public int getTanning() {
		return this.info.info().getIntOr(TAG_TANNING, 0);
	}

	public int getTannedIn() {
		return this.info.info().getIntOr(TAG_TANNED_IN, 0);
	}

	public boolean getHunting() {
		return this.info.info().getBooleanOr(TAG_HUNT, false);
	}

	/** Target: the experience the guard has not bottled yet, -1 if unknown. */
	public int getExperience() {
		return this.info.info().getIntOr(TAG_EXPERIENCE, -1);
	}

	/** Barrel: whether the fisher rows out in a boat. */
	public boolean getBoat() {
		return this.info.info().getBooleanOr(TAG_BOAT, true);
	}

	/** Chopping block: whether the lumberjack strips the logs. */
	public boolean getStrip() {
		return this.info.info().getBooleanOr(TAG_STRIP, false);
	}

	/** Woodcutter only: the allowed wood types. */
	public Set<String> getWoods() {
		return Set.copyOf(this.info.info().getListOrEmpty(TAG_WOODS).stream().flatMap(tag -> tag.asString().stream()).toList());
	}

	/** Woodcutter only: the wood types the current shape can be made of. */
	public Set<String> getAvailableWoods() {
		return Set.copyOf(this.info.info().getListOrEmpty(TAG_AVAILABLE).stream().flatMap(tag -> tag.asString().stream()).toList());
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		if (buttonId == BACK_BUTTON) {
			if (!this.type.hasOwnScreen()) {
				return false;
			}
			this.access.execute((level, pos) -> player.openMenu(level.getBlockState(pos).getMenuProvider(level, pos)));
			return true;
		}
		if (buttonId >= ASSIGN_BUTTON && buttonId <= ADD_OUTPUTS_BUTTON || buttonId == PICK_LADDER_BUTTON
			|| buttonId == PICK_TUNNEL_BUTTON) {
			if (!(player instanceof ServerPlayer serverPlayer)) {
				return false;
			}
			boolean[] done = {false};
			this.access.execute((level, pos) -> {
				if (level instanceof ServerLevel serverLevel) {
					done[0] = this.pick(serverLevel, pos, serverPlayer, buttonId);
				}
			});
			return done[0];
		}
		boolean[] changed = {false};
		this.access.execute((level, pos) -> {
			if (level instanceof ServerLevel serverLevel) {
				Workstations.find(serverLevel, pos).ifPresent(station -> changed[0] = this.click(serverLevel, player, station, buttonId));
			}
		});
		if (changed[0]) {
			this.updateInfo();
		}
		return changed[0];
	}

	private boolean pick(ServerLevel level, BlockPos pos, ServerPlayer player, int buttonId) {
		Optional<Workstations.Station> found = Workstations.find(level, pos);
		if (found.isEmpty()) {
			return false;
		}
		Workstations.Station station = found.get();
		switch (buttonId) {
			case ASSIGN_BUTTON -> {
				if (station.occupant(level).isPresent()) {
					return false;
				}
				StationPicks.pickWorker(player, pos);
			}
			case UNASSIGN_BUTTON -> {
				if (station.worker() == null || !(level.getEntity(station.worker()) instanceof Villager worker)) {
					return false;
				}
				WorkerJobs.unassign(level, worker);
				player.sendOverlayMessage(Component.translatable("message.workingvillagers.assign.removed", worker.getName()));
				this.updateInfo();
			}
			case ADD_INPUTS_BUTTON -> {
				if (!this.type.isProcessing()) {
					return false;
				}
				StationPicks.pickChests(player, pos, ChestRole.INPUT);
			}
			case PICK_TUNNEL_BUTTON -> {
				if (this.type != StationType.MINING_BENCH) {
					return false;
				}
				StationPicks.pickTunnel(player, pos);
			}
			case PICK_LADDER_BUTTON -> {
				if (this.type != StationType.PIT_STAKE || station.pit() == null) {
					return false;
				}
				StationPicks.pickLadder(player, pos);
			}
			default -> StationPicks.pickChests(player, pos, ChestRole.OUTPUT);
		}
		return true;
	}

	private boolean click(ServerLevel level, Player player, Workstations.Station station, int buttonId) {
		if (buttonId >= UNLINK_BUTTON && buttonId < UNLINK_BUTTON + this.shownChests.size()) {
			station.setRole(level, this.shownChests.get(buttonId - UNLINK_BUTTON), null);
			return true;
		}
		if (this.type.isProcessing() && buttonId >= ROLE_BUTTON && buttonId < ROLE_BUTTON + this.shownChests.size()) {
			BlockPos chest = this.shownChests.get(buttonId - ROLE_BUTTON);
			station.setRole(level, chest, station.role(chest) == ChestRole.INPUT ? ChestRole.OUTPUT : ChestRole.INPUT);
			return true;
		}
		if (this.type.hasOrder() && buttonId >= AMOUNT_LESS_BUTTON && buttonId < AMOUNT_MODE_BUTTON + Workstations.MAX_ORDERS) {
			return changeAmount(station, buttonId);
		}
		if (this.type == StationType.CHOPPING_BLOCK && buttonId == STRIP_BUTTON) {
			station.setStrip(!station.strip());
			return true;
		}
		if (this.type == StationType.BARREL && buttonId == BOAT_BUTTON) {
			station.setBoat(!station.boat());
			return true;
		}
		if (this.type == StationType.TARGET && buttonId == HUNT_BUTTON) {
			station.setHunting(!station.hunting());
			return true;
		}
		if (this.type == StationType.TARGET && (buttonId == HERD_LESS_BUTTON || buttonId == HERD_MORE_BUTTON)) {
			station.setGuardRadius(station.guardRadius() + (buttonId == HERD_MORE_BUTTON ? 2 : -2));
			return true;
		}
		if ((this.type == StationType.LOOM || this.type == StationType.SMOKER) && (buttonId == HERD_LESS_BUTTON || buttonId == HERD_MORE_BUTTON)) {
			station.setHerd(station.herd() + (buttonId == HERD_MORE_BUTTON ? 1 : -1));
			return true;
		}
		if (this.type == StationType.MINING_BENCH && station.tunnel() != null) {
			Tunnel tunnel = station.tunnel();
			Tunnel changed = switch (buttonId) {
				case LENGTH_LESS_BUTTON -> tunnel.withLength(tunnel.clampedLength() - 1);
				case LENGTH_MORE_BUTTON -> tunnel.withLength(tunnel.clampedLength() + 1);
				case BRANCHES_LESS_BUTTON -> tunnel.withBranches((tunnel.clampedBranches() - 1) / BRANCH_STEP * BRANCH_STEP);
				case BRANCHES_MORE_BUTTON -> tunnel.withBranches((tunnel.clampedBranches() / BRANCH_STEP + 1) * BRANCH_STEP);
				case STAIRS_BUTTON -> tunnel.withStairs(!tunnel.stairs());
				default -> null;
			};
			if (changed == null) {
				return false;
			}
			station.setTunnel(changed);
			return true;
		}
		if (this.type == StationType.PIT_STAKE && (buttonId == DEPTH_LESS_BUTTON || buttonId == DEPTH_MORE_BUTTON)) {
			// Up to the depth of the pit digger at the stake; a deeper one set before counts from there
			int max = PitLimits.at(level, station).depth();
			station.setDepth(Math.clamp(Math.min(station.depth(), max) + (buttonId == DEPTH_MORE_BUTTON ? 1 : -1), 1, max));
			return true;
		}
		if (this.type == StationType.BREWING_STAND) {
			if (buttonId >= KILN_PRODUCT_BUTTON && buttonId < KILN_PRODUCT_BUTTON + this.shownStacks.size()) {
				ItemStack product = this.shownStacks.get(buttonId - KILN_PRODUCT_BUTTON);
				if (BrewingPaths.same(product, BrewingPaths.order(station.order(), station.potion()))) {
					station.setOrder(null, null);
				} else {
					station.setOrder(product.getItem(), BrewingPaths.potionOf(product).flatMap(Holder::unwrapKey).map(key -> key.identifier().toString()).orElse(null));
				}
				return true;
			}
			return false;
		}
		if (this.type == StationType.CAULDRON && (buttonId == DYE_LESS_BUTTON || buttonId == DYE_MORE_BUTTON)) {
			// None, then the dyes in their order, round
			int colors = DyeColor.values().length + 1;
			int now = station.dye() == null ? 0 : station.dye().ordinal() + 1;
			int next = Math.floorMod(now + (buttonId == DYE_MORE_BUTTON ? 1 : -1), colors);
			station.setDye(next == 0 ? null : DyeColor.values()[next - 1]);
			return true;
		}
		if (this.type == StationType.LECTERN && (buttonId == ENCHANT_LESS_BUTTON || buttonId == ENCHANT_MORE_BUTTON)) {
			// Round: highest (0), then 1 to 30
			int next = Math.floorMod(station.enchantLevel() + (buttonId == ENCHANT_MORE_BUTTON ? 1 : -1), Workstations.MAX_ENCHANT_LEVEL + 1);
			station.setEnchantLevel(next);
			return true;
		}
		if (this.type == StationType.GRINDSTONE && (buttonId == GRIND_LESS_BUTTON || buttonId == GRIND_MORE_BUTTON)) {
			station.setGrindMode(station.grindMode() + (buttonId == GRIND_MORE_BUTTON ? 1 : -1));
			return true;
		}
		if (this.type == StationType.KILN || this.type == StationType.FLETCHING_TABLE || this.type == StationType.CAULDRON
			|| this.type == StationType.GRINDSTONE) {
			if (buttonId >= KILN_PRODUCT_BUTTON && buttonId < KILN_PRODUCT_BUTTON + this.shownProducts.size()) {
				Item product = this.shownProducts.get(buttonId - KILN_PRODUCT_BUTTON);
				return toggled(player, station.toggleOrder(product));
			}
			return false;
		}
		if (this.type == StationType.STONECUTTER && this.families != null && buttonId >= STONE_PRODUCT_BUTTON) {
			Item product = this.families.stream()
				.flatMap(family -> family.products().stream())
				.skip(buttonId - STONE_PRODUCT_BUTTON)
				.findFirst()
				.orElse(null);
			if (product == null) {
				return false;
			}
			return toggled(player, station.toggleOrder(product));
		}
		if (!this.type.hasWoods()) {
			return false;
		}
		if (this.type == StationType.CARPENTER_BENCH) {
			if (buttonId >= SHAPE_BUTTON && buttonId < SHAPE_BUTTON + this.shownProducts.size()) {
				return toggled(player, station.orderProduct(this.shownProducts.get(buttonId - SHAPE_BUTTON)));
			}
		} else if (buttonId >= SHAPE_BUTTON && buttonId < SHAPE_BUTTON + WoodTypes.SHAPES.size()) {
			return toggled(player, station.toggleShape(WoodTypes.SHAPES.get(buttonId - SHAPE_BUTTON)));
		}
		if (buttonId >= WOOD_BUTTON && buttonId < WOOD_BUTTON + WoodTypes.ALL.size()) {
			String wood = WoodTypes.ALL.get(buttonId - WOOD_BUTTON).name();
			station.setWood(wood, !station.woods().contains(wood));
			return true;
		}
		if (buttonId == ALL_WOODS_BUTTON || buttonId == NO_WOODS_BUTTON) {
			WoodTypes.ALL.forEach(wood -> station.setWood(wood.name(), buttonId == ALL_WOODS_BUTTON));
			return true;
		}
		return false;
	}

	/** Tells the player if a product could not be added because the order is full. */
	private static boolean toggled(Player player, boolean done) {
		if (!done && player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendOverlayMessage(Component.translatable("message.workingvillagers.order.full", Workstations.MAX_ORDERS));
		}
		return done;
	}

	/**
	 * The buttons of the product of the order at the index: fewer or more of it, or ordering it
	 * once instead of keeping it in stock and back. Ordered once it starts with a stack (one of
	 * what does not stack), or with its stock if it had one.
	 */
	private static boolean changeAmount(Workstations.Station station, int buttonId) {
		int index = (buttonId - AMOUNT_LESS_BUTTON) % 100;
		int kind = (buttonId - AMOUNT_LESS_BUTTON) / 100;
		if (index >= station.orders().size()) {
			return false;
		}
		Item order = station.orders().get(index);
		Workstations.Station.Batch batch = station.batch(order);
		switch (kind) {
			case 0, 1 -> {
				boolean more = kind == 1;
				if (batch != null) {
					station.setBatch(order, next(BATCH_STEPS, batch.amount(), more, 1));
				} else {
					station.setStock(order, next(STOCK_STEPS, station.stock(order), more, 0));
				}
			}
			case 2 -> {
				if (batch != null) {
					station.setStock(order, 0);
				} else {
					int stock = station.stock(order);
					station.setBatch(order, stock > 0 ? stock : order.getDefaultMaxStackSize());
				}
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	/** The next step up or down from the value, at least the lowest step. */
	private static int next(int[] steps, int value, boolean more, int lowest) {
		if (more) {
			for (int step : steps) {
				if (step > value) {
					return step;
				}
			}
			return steps[steps.length - 1];
		}
		int lower = lowest;
		for (int step : steps) {
			if (step < value) {
				lower = step;
			}
		}
		return lower;
	}

	/** Shift-click: emeralds between the wage box and the inventory. */
	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		if (slotIndex < WAGE_SLOT || slotIndex >= INVENTORY_END) {
			return ItemStack.EMPTY;
		}
		Slot slot = this.slots.get(slotIndex);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack clicked = stack.copy();
		if (slotIndex == WAGE_SLOT) {
			if (!this.moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (!stack.is(Items.EMERALD) || !this.moveItemStackTo(stack, WAGE_SLOT, WAGE_SLOT + 1, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return clicked;
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(this.access, player, this.type.block());
	}

	/**
	 * A linked chest: its name (the block's name if the player gave it none) and how many of
	 * its slots are used.
	 */
	public record Chest(BlockPos pos, Component name, boolean named, int used, int size, boolean input) {
	}
}
