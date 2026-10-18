package me.teajay.workingvillagers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;

/**
 * The settings in {@code config/workingvillagers.json}, read once when the game starts. The file is
 * JSON with {@code //} comments, one object per section. Missing or out of range values fall
 * back to their default or are clamped, and the file is written again with every option and
 * its explanation, so new options show up after an update. A file that cannot be read is
 * left alone and the defaults are used.
 *
 * <p>The server's values count. The few the screens show (wage, logs per day, chests per
 * station) are sent along with the menu data, so a client with other settings shows the
 * server's.
 */
public final class Config {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String FILE_NAME = "workingvillagers.json";
	private static final List<Option> OPTIONS = new ArrayList<>();

	public static final IntOption DAILY_WAGE = intOption("workers", "dailyWage", 1, 0, 64,
		"Emeralds a worker takes from the wage box of its workstation before work each day (put them in there,",
		"or right-click the station or the worker with them). 0: they work for free.");
	public static final BoolOption RANDOM_NAMES = boolOption("workers", "randomNames", false,
		"Give hired villagers a random name (like Anton or Berta). Otherwise only a name tag names them.");
	public static final IntOption MAX_CHESTS = intOption("workers", "maxChests", 8, 1, 8,
		"How many chests can be linked to one workstation.");
	public static final IntOption CHEST_DISTANCE = intOption("workers", "chestDistance", 16, 4, 32,
		"How far (in blocks) a linked chest may be from its workstation.");

	public static final IntOption STUCK_SECONDS = intOption("workers", "stuckSeconds", 60, 0, 600,
		"After this many seconds of trying to get somewhere without getting anywhere (in a hole, boxed in),",
		"a worker is brought back to its workstation (a carrier: to the source of its first route).",
		"0: never, the menus only show that it is stuck.");

	public static final BoolOption REQUIRE_MOB_GRIEFING = boolOption("lumberjack", "requireMobGriefing", true,
		"Lumberjacks only work while the game rule mob_griefing is on.");
	public static final IntOption TREE_SEARCH_RADIUS = intOption("lumberjack", "searchRadius", 16, 4, 32,
		"How far from the chopping block (in blocks) the lumberjack looks for trees.");
	public static final IntOption DAILY_LOGS = intOption("lumberjack", "dailyLogs", 64, 1, 4096,
		"After this many logs a day the lumberjack stops for the day.");
	public static final DoubleOption CHOP_SLOWDOWN = doubleOption("lumberjack", "chopSlowdown", 8.0, 1.0, 32.0,
		"How many times as long as a player with the same axe a (novice) lumberjack needs for a tree.");
	public static final IntOption REST_SECONDS = intOption("lumberjack", "restSeconds", 30, 0, 600,
		"How long the lumberjack rests after bringing the wood to a chest.");
	public static final IntOption KEEP_SAPLINGS = intOption("lumberjack", "keepSaplings", 16, 1, 64,
		"How many saplings of each kind the lumberjack carries for replanting.");

	public static final IntOption CUT_ITEMS_PER_SECOND = intOption("sawyer", "itemsPerSecond", 4, 1, 64,
		"How many items sawyers and stone sawyers make per second (at least one second per cut).");
	public static final IntOption CUT_BATCH = intOption("sawyer", "batchSize", 16, 1, 64,
		"How many items of material they fetch from an input chest at once.");

	public static final DoubleOption CRAFT_SECONDS_PER_INGREDIENT = doubleOption("carpenter", "secondsPerIngredient", 0.5, 0.05, 10.0,
		"How long the carpenter needs per ingredient of a piece (at least one second per piece).");
	public static final IntOption CRAFT_INGREDIENTS_PER_TRIP = intOption("carpenter", "ingredientsPerTrip", 32, 1, 64,
		"How many ingredients the carpenter fetches from the input chests at once (at least one piece's worth).");

	public static final IntOption FIELD_RADIUS = intOption("farmer", "fieldRadius", 8, 2, 16,
		"How far from the composter (in blocks, sideways) the farmer works the farmland.");
	public static final DoubleOption SECONDS_PER_CROP = doubleOption("farmer", "secondsPerCrop", 1.5, 0.1, 20.0,
		"How long the farmer needs to harvest or plant one crop.");
	public static final IntOption KEEP_SEEDS = intOption("farmer", "keepSeeds", 16, 1, 64,
		"How many seeds of each kind (wheat seeds, carrots, potatoes, ...) the farmer keeps for planting.");

	public static final IntOption PASTURE_RADIUS = intOption("shepherd", "pastureRadius", 12, 4, 24,
		"How far from the loom (in blocks, sideways) the shepherd looks after sheep.");
	public static final DoubleOption SECONDS_PER_SHEEP = doubleOption("shepherd", "secondsPerSheep", 1.5, 0.1, 20.0,
		"How long the shepherd needs to shear or feed one sheep.");
	public static final IntOption KEEP_WHEAT = intOption("shepherd", "keepWheat", 16, 2, 64,
		"How much wheat the shepherd keeps for breeding its sheep.");

	public static final IntOption BUTCHER_RADIUS = intOption("butcher", "pastureRadius", 12, 4, 24,
		"How far from the smoker (in blocks, sideways) the butcher looks after cows, pigs, chickens and rabbits.");
	public static final DoubleOption SECONDS_PER_ANIMAL = doubleOption("butcher", "secondsPerAnimal", 1.5, 0.1, 20.0,
		"How long the butcher needs to feed or slaughter one animal.");
	public static final IntOption KEEP_FEED = intOption("butcher", "keepFeed", 16, 2, 64,
		"How much feed of each kind (wheat, carrots, seeds, ...) the butcher keeps for breeding.");
	public static final IntOption KEEP_FUEL = intOption("butcher", "keepFuel", 16, 1, 64,
		"How much coal or charcoal the butcher keeps for the smoker.");

	public static final IntOption SMELT_BATCH = intOption("smith", "batchSize", 16, 1, 64,
		"How many items to smelt the smith fetches from an input chest at once.");
	public static final IntOption FUEL_BATCH = intOption("smith", "fuelPerTrip", 8, 1, 64,
		"How much fuel the smith fetches at once.");

	public static final IntOption KILN_BATCH = intOption("kilnOperator", "batchSize", 16, 1, 64,
		"How many items to fire the kiln operator fetches from an input chest at once.");
	public static final IntOption KILN_FUEL_BATCH = intOption("kilnOperator", "fuelPerTrip", 8, 1, 64,
		"How much fuel the kiln operator fetches at once.");

	public static final IntOption TRADE_RADIUS = intOption("shopAssistant", "tradeRadius", 24, 4, 48,
		"How far from the trading counter (in blocks) the shop assistant looks for villagers to sell to.");
	public static final DoubleOption SECONDS_PER_TRADE = doubleOption("shopAssistant", "secondsPerTrade", 2.0, 0.25, 20.0,
		"How long one trade with a villager takes.");

	public static final IntOption PIT_MAX_AREA = intOption("pitDigger", "maxArea", 64, 4, 256,
		"How many blocks of ground (inside the string ring) a pit may have at most.");
	public static final IntOption PIT_MAX_DEPTH = intOption("pitDigger", "maxDepth", 16, 1, 64,
		"How deep (in blocks, below the highest string of the ring) a pit may be at most.");
	public static final IntOption PIT_MAX_UNEVENNESS = intOption("pitDigger", "maxUnevenness", 4, 0, 8,
		"How far apart (in height) the strings of a ring may lie at most.");
	public static final IntOption PIT_DAILY_BLOCKS = intOption("pitDigger", "dailyBlocks", 256, 1, 4096,
		"After this many blocks a day the pit digger stops for the day.");
	public static final DoubleOption DIG_SLOWDOWN = doubleOption("pitDigger", "digSlowdown", 8.0, 1.0, 32.0,
		"How many times as long as a player with the same tool a (novice) pit digger needs for a block,",
		"but at least minSecondsPerBlock.");
	public static final DoubleOption MIN_DIG_SECONDS = doubleOption("pitDigger", "minSecondsPerBlock", 2.0, 0.25, 20.0,
		"How long the pit digger needs at least for a block, however good its tool (plants take a quarter of it).");
	public static final IntOption KEEP_LADDERS = intOption("pitDigger", "keepLadders", 64, 1, 64,
		"How many ladders the pit digger carries at most.");
	public static final IntOption KEEP_TORCHES = intOption("pitDigger", "keepTorches", 16, 0, 64,
		"How many torches the pit digger carries at most (0: it takes none).");

	public static final IntOption TUNNEL_MAX_LENGTH = intOption("miner", "maxTunnelLength", 32, 4, 64,
		"How long (in blocks) a tunnel may be at most. The miner carries every load out to its chests itself.");
	public static final IntOption TUNNEL_MAX_BRANCH = intOption("miner", "maxBranchLength", 16, 0, 32,
		"How long the side tunnels (every 4 blocks, left and right) may be at most. 0: no side tunnels.");
	public static final IntOption MINER_DAILY_BLOCKS = intOption("miner", "dailyBlocks", 256, 1, 4096,
		"After this many blocks a day the miner stops for the day.");
	public static final DoubleOption MINER_DIG_SLOWDOWN = doubleOption("miner", "digSlowdown", 8.0, 1.0, 32.0,
		"How many times as long as a player with the same pickaxe a (novice) miner needs for a block,",
		"but at least minSecondsPerBlock.");
	public static final DoubleOption MINER_MIN_DIG_SECONDS = doubleOption("miner", "minSecondsPerBlock", 1.5, 0.25, 20.0,
		"How long the miner needs at least for a block, however good its pickaxe.");
	public static final BoolOption TUNNEL_SUPPORTS = boolOption("miner", "supports", true,
		"The miner puts wooden supports (fences and planks) into its tunnels every 4 blocks, like a mineshaft.");
	public static final IntOption KEEP_FENCES = intOption("miner", "keepFences", 16, 0, 64,
		"How many fences for the supports the miner carries at most (0: it takes none).");
	public static final IntOption KEEP_PLANKS = intOption("miner", "keepPlanks", 16, 0, 64,
		"How many planks for the supports the miner carries at most (0: it takes none).");
	public static final IntOption KEEP_SEAL_BLOCKS = intOption("miner", "keepSealBlocks", 16, 1, 64,
		"How much cobblestone the miner keeps of what it digs, to seal off water and lava and to close holes in the floor.");
	public static final IntOption MINER_KEEP_TORCHES = intOption("miner", "keepTorches", 16, 0, 64,
		"How many torches the miner carries at most (0: it takes none and leaves its tunnels dark).");

	public static final IntOption ROUTE_LENGTH = intOption("carrier", "maxRouteLength", 32, 4, 64,
		"How far (in blocks) source and target of a route may be apart.");
	public static final IntOption ROUTE_SOURCE_SPREAD = intOption("carrier", "maxSourceSpread", 40, 4, 96,
		"How far apart the sources of one carrier's routes may be.");
	public static final IntOption STACKS_PER_TRIP = intOption("carrier", "stacksPerTrip", 6, 1, 6,
		"How many stacks a carrier carries at once (its basket holds 6).");

	public static final BoolOption VILLAGE_CHESTS = boolOption("villageChests", "enabled", true,
		"Workers take their wage, tools, saplings, seeds, ladders and torches from village chests nearby.",
		"Off: village chests are ordinary chests.");
	public static final IntOption VILLAGE_CHEST_RADIUS = intOption("villageChests", "radius", 48, 8, 128,
		"How far (in blocks, horizontally) a village chest may be from a worker's workstation (24 up or down).");
	public static final IntOption VILLAGE_CHEST_WAGE_DAYS = intOption("villageChests", "wageDays", 3, 1, 16,
		"For how many days a worker takes its wage from a village chest at once.");
	public static final BoolOption VILLAGE_CHEST_ENCHANTED = boolOption("villageChests", "takeEnchanted", false,
		"Workers also take enchanted and renamed tools from village chests.");

	public static final IntOption TOOL_RETURN_BELOW = intOption("tools", "returnBelow", 10, 0, 50,
		"Workers stop using a tool once this many percent of its durability are left (but at least 2 uses)",
		"and give it back instead of breaking it: into a village chest, or an output chest of their workstation.",
		"They never take such a tool. 0: tools are used up until they break.");
	public static final BoolOption TOOL_RETURN_CHEAP = boolOption("tools", "returnCheap", false,
		"Wooden and stone tools are given back, too. Off: they are used up; enchanted and renamed ones always come back.");

	private Config() {
	}

	/** Reads the file in the config directory (creating it on the first start). */
	public static void load(Path configDir) {
		Path file = configDir.resolve(FILE_NAME);
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				JsonReader json = new JsonReader(reader);
				json.setStrictness(Strictness.LENIENT);
				JsonElement root = JsonParser.parseReader(json);
				if (!root.isJsonObject()) {
					throw new IOException("not a JSON object");
				}
				for (Option option : OPTIONS) {
					JsonObject section = root.getAsJsonObject().getAsJsonObject(option.section);
					JsonElement value = section == null ? null : section.get(option.key);
					if (value != null && !option.read(value)) {
						LOGGER.warn("{}: invalid value {} for {}.{}, using {}", FILE_NAME, value, option.section, option.key, option.text());
					}
				}
			} catch (IOException | RuntimeException e) {
				LOGGER.error("Could not read {}, using the defaults and leaving the file alone: {}", file, e.getMessage());
				return;
			}
		}
		try {
			Files.createDirectories(configDir);
			Files.writeString(file, write());
		} catch (IOException e) {
			LOGGER.error("Could not write {}", file, e);
		}
	}

	private static String write() {
		Map<String, List<Option>> sections = new LinkedHashMap<>();
		OPTIONS.forEach(option -> sections.computeIfAbsent(option.section, key -> new ArrayList<>()).add(option));
		StringBuilder out = new StringBuilder();
		out.append("// Working Villagers settings. Changes take effect after a restart of the game or server.\n");
		out.append("// On a server, the server's settings count.\n");
		out.append("{\n");
		int s = 0;
		for (Map.Entry<String, List<Option>> section : sections.entrySet()) {
			out.append("  \"").append(section.getKey()).append("\": {\n");
			List<Option> options = section.getValue();
			for (int i = 0; i < options.size(); i++) {
				Option option = options.get(i);
				for (String line : option.comment) {
					out.append("    // ").append(line).append('\n');
				}
				out.append("    // ").append(option.range()).append('\n');
				out.append("    \"").append(option.key).append("\": ").append(option.text());
				out.append(i < options.size() - 1 ? ",\n\n" : "\n");
			}
			out.append(++s < sections.size() ? "  },\n" : "  }\n");
		}
		return out.append("}\n").toString();
	}

	private static IntOption intOption(String section, String key, int def, int min, int max, String... comment) {
		IntOption option = new IntOption(section, key, def, min, max, comment);
		OPTIONS.add(option);
		return option;
	}

	private static DoubleOption doubleOption(String section, String key, double def, double min, double max, String... comment) {
		DoubleOption option = new DoubleOption(section, key, def, min, max, comment);
		OPTIONS.add(option);
		return option;
	}

	private static BoolOption boolOption(String section, String key, boolean def, String... comment) {
		BoolOption option = new BoolOption(section, key, def, comment);
		OPTIONS.add(option);
		return option;
	}

	private abstract static class Option {
		final String section;
		final String key;
		final String[] comment;

		Option(String section, String key, String[] comment) {
			this.section = section;
			this.key = key;
			this.comment = comment;
		}

		/** Takes the value from the file; false if it is not usable (the value stays as it was). */
		abstract boolean read(JsonElement value);

		abstract String text();

		abstract String range();
	}

	public static final class IntOption extends Option {
		private final int def;
		private final int min;
		private final int max;
		private int value;

		IntOption(String section, String key, int def, int min, int max, String[] comment) {
			super(section, key, comment);
			this.def = def;
			this.min = min;
			this.max = max;
			this.value = def;
		}

		public int get() {
			return this.value;
		}

		/** Changes the value for this run (the game tests), clamped to the range. The file stays as it is. */
		public void set(int value) {
			this.value = Math.clamp(value, this.min, this.max);
		}

		@Override
		boolean read(JsonElement value) {
			if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
				return false;
			}
			double number = value.getAsDouble();
			this.value = (int) Math.clamp(Math.round(number), this.min, this.max);
			return number == this.value;
		}

		@Override
		String text() {
			return Integer.toString(this.value);
		}

		@Override
		String range() {
			return "Default: " + this.def + ", " + this.min + " to " + this.max;
		}
	}

	public static final class DoubleOption extends Option {
		private final double def;
		private final double min;
		private final double max;
		private double value;

		DoubleOption(String section, String key, double def, double min, double max, String[] comment) {
			super(section, key, comment);
			this.def = def;
			this.min = min;
			this.max = max;
			this.value = def;
		}

		public double get() {
			return this.value;
		}

		/** Changes the value for this run (the game tests), clamped to the range. The file stays as it is. */
		public void set(double value) {
			this.value = Math.clamp(value, this.min, this.max);
		}

		@Override
		boolean read(JsonElement value) {
			if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
				return false;
			}
			double number = value.getAsDouble();
			this.value = Math.clamp(number, this.min, this.max);
			return number == this.value;
		}

		@Override
		String text() {
			return format(this.value);
		}

		@Override
		String range() {
			return "Default: " + format(this.def) + ", " + format(this.min) + " to " + format(this.max);
		}

		private static String format(double number) {
			return String.format(Locale.ROOT, "%s", number);
		}
	}

	public static final class BoolOption extends Option {
		private final boolean def;
		private boolean value;

		BoolOption(String section, String key, boolean def, String[] comment) {
			super(section, key, comment);
			this.def = def;
			this.value = def;
		}

		public boolean get() {
			return this.value;
		}

		@Override
		boolean read(JsonElement value) {
			if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
				return false;
			}
			this.value = value.getAsBoolean();
			return true;
		}

		@Override
		String text() {
			return Boolean.toString(this.value);
		}

		@Override
		String range() {
			return "Default: " + this.def;
		}
	}
}
