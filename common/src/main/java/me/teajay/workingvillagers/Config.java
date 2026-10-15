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
		"Emeralds a worker takes from what it carries before work each day (give them by right-clicking",
		"the worker). 0: they work for free.");
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

	public static final IntOption SMELT_BATCH = intOption("smith", "batchSize", 16, 1, 64,
		"How many items to smelt the smith fetches from an input chest at once.");
	public static final IntOption FUEL_BATCH = intOption("smith", "fuelPerTrip", 8, 1, 64,
		"How much fuel the smith fetches at once.");

	public static final IntOption ROUTE_LENGTH = intOption("carrier", "maxRouteLength", 32, 4, 64,
		"How far (in blocks) source and target of a route may be apart.");
	public static final IntOption ROUTE_SOURCE_SPREAD = intOption("carrier", "maxSourceSpread", 40, 4, 96,
		"How far apart the sources of one carrier's routes may be.");
	public static final IntOption STACKS_PER_TRIP = intOption("carrier", "stacksPerTrip", 6, 1, 6,
		"How many stacks a carrier carries at once (its basket holds 6).");

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
