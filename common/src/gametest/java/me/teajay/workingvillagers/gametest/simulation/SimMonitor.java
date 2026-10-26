package me.teajay.workingvillagers.gametest.simulation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import me.teajay.workingvillagers.job.ChopTrees;
import me.teajay.workingvillagers.job.HiredVillager;
import me.teajay.workingvillagers.job.MealData;
import me.teajay.workingvillagers.job.Meals;
import me.teajay.workingvillagers.job.WorkerStatus;
import me.teajay.workingvillagers.job.WorkerSupplies;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watches the simulated village: how long the server needs per tick, what every worker is
 * doing, where the emeralds are and what lies in the storage. Writes CSV files (one row per
 * in-game hour or day) and a summary into the output directory.
 */
public final class SimMonitor {
	static final Logger LOG = LoggerFactory.getLogger("WV-Simulation");
	/** How often the workers are looked at, in ticks. */
	private static final int SAMPLE_TICKS = 100;
	private static final int HOUR = 1000;

	private final SimRegistry registry;
	private final Path out;
	private final long startDay;

	// Tick times of the current in-game hour
	private long tickStarted;
	private long[] hourTicks = new long[2048];
	private int hourTickCount;
	private long hour = -1;

	// Worker samples of the current day: job -> status -> samples (only during work)
	private final Map<String, Map<String, Integer>> daySamples = new TreeMap<>();
	private final Map<String, Integer> dayWorkSamples = new TreeMap<>();
	private final Map<String, Integer> dayProblemSamples = new TreeMap<>();
	private final Map<String, Integer> dayHungrySamples = new TreeMap<>();
	/** The food and emeralds in the pantries at the end of the last day, to see what was bought. */
	private Map<String, Integer> pantryLast = Map.of();
	private final Map<UUID, Integer> lastWorkDone = new HashMap<>();
	private final Set<UUID> dead = new HashSet<>();
	private long day = -1;

	// For the summary
	final List<double[]> hourRows = new ArrayList<>();
	final List<Map<String, Object>> dayRows = new ArrayList<>();
	final Map<String, int[]> jobTotals = new TreeMap<>();
	final List<String> events = new ArrayList<>();
	private Map<String, Integer> storageAtStart;
	private Map<String, Integer> storageLast = Map.of();

	SimMonitor(SimRegistry registry, Path out, ServerLevel level) {
		this.registry = registry;
		this.out = out;
		this.startDay = ChopTrees.today(level);
		SimDeliveries.watch(registry);
		try {
			Files.createDirectories(out);
			header("ticks.csv", "hour,day,time_of_day,ticks,mean_ms,p50_ms,p95_ms,p99_ms,max_ms,over_50ms,entities,villagers,workers_alive,monsters,animals,items,golems,chunks");
			header("workers.csv", "day,job,status,samples");
			header("economy.csv", "day,workers_alive,paid_today,unpaid_today,wage_boxes,village_chests,tills,storage,depots_relays,carried,purses,total_emeralds,shop_earned,deaths_total");
			header("meals.csv", "day,purses,hungry_at_end,mean_hunger,bag_food_points,hungry_work_share,pantry_food,pantry_emeralds");
			header("storage.csv", "day,item,count");
			header("production.csv", "day,job,worker,done");
			header("deliveries.csv", "day,job,chest,item,count");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void header(String file, String header) throws IOException {
		Path path = this.out.resolve(file);
		if (!Files.exists(path)) {
			Files.writeString(path, header + "\n");
		}
	}

	private void append(String file, String line) {
		try {
			Files.writeString(this.out.resolve(file), line + "\n", StandardOpenOption.APPEND, StandardOpenOption.CREATE);
		} catch (IOException e) {
			LOG.warn("Could not write {}", file, e);
		}
	}

	public void tickStart() {
		this.tickStarted = System.nanoTime();
	}

	public void tickEnd(MinecraftServer server) {
		long took = System.nanoTime() - this.tickStarted;
		ServerLevel level = server.overworld();
		long time = level.getOverworldClockTime();
		long nowHour = time / HOUR;
		if (this.hour == -1) {
			this.hour = nowHour;
		}
		if (nowHour != this.hour) {
			this.finishHour(level, time);
			this.hour = nowHour;
		}
		if (this.hourTickCount == this.hourTicks.length) {
			this.hourTicks = Arrays.copyOf(this.hourTicks, this.hourTicks.length * 2);
		}
		this.hourTicks[this.hourTickCount++] = took;
		if (level.getGameTime() % SAMPLE_TICKS == 0) {
			this.sample(level);
		}
		if (level.getGameTime() % HOUR == 0) {
			this.sortPantries(level);
		}
	}

	/**
	 * The player's chore: the carriers bring everything of the fisher and the chicken butcher
	 * to the pantry (they have no filters), so what is no food goes on to a depot.
	 */
	private void sortPantries(ServerLevel level) {
		String feed = System.getProperty("wvsim.feed", "");
		net.minecraft.world.item.Item fed = feed.isEmpty() ? null
			: BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(feed));
		for (BlockPos pos : this.registry.pantries) {
			Container pantry = Workstations.getContainer(level, pos);
			// A player's auto farm: tops the pantry up to 64 of its food
			if (fed != null && pantry != null) {
				int missing = 64 - WorkerSupplies.count(pantry, stack -> stack.is(fed));
				if (missing > 0) {
					net.minecraft.world.level.block.entity.HopperBlockEntity.addItem(null, pantry, new ItemStack(fed, missing), null);
				}
			}
			BlockPos depot = this.registry.depots.stream().min(java.util.Comparator.comparingDouble(other -> other.distSqr(pos))).orElse(null);
			Container target = depot == null ? null : Workstations.getContainer(level, depot);
			if (pantry == null || target == null) {
				continue;
			}
			for (int slot = 0; slot < pantry.getContainerSize(); slot++) {
				ItemStack stack = pantry.getItem(slot);
				if (!stack.isEmpty() && !Meals.isMeal(stack) && !stack.is(Items.EMERALD)) {
					pantry.setItem(slot, net.minecraft.world.level.block.entity.HopperBlockEntity.addItem(null, target, stack, null));
				}
			}
		}
	}

	/** The customers that had a job at the last sample. */
	private final Set<UUID> employed = new HashSet<>();

	/** Notes when a customer gets or loses its job, with what might be the reason. */
	private void sampleCustomers(ServerLevel level) {
		for (SimRegistry.Trader trader : this.registry.traders) {
			if (!(level.getEntity(trader.uuid()) instanceof Villager villager) || !villager.isAlive()) {
				continue;
			}
			boolean job = !villager.getVillagerData().profession().is(net.minecraft.world.entity.npc.villager.VillagerProfession.NONE);
			if (job) {
				this.employed.add(trader.uuid());
			} else if (this.employed.remove(trader.uuid())) {
				BlockPos block = trader.block();
				String taken = level.getPoiManager().getType(block).map(type -> type.getRegisteredName()).orElse("no poi")
					+ (level.getPoiManager().exists(block, type -> true) ? "" : " (gone)");
				List<Villager> holders = level.getEntitiesOfClass(Villager.class, new net.minecraft.world.phys.AABB(block).inflate(48), other -> other.getBrain()
					.getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.JOB_SITE).map(site -> site.pos().equals(block)).orElse(false));
				this.event(level, "Customer lost its job: " + trader.profession() + " xp " + villager.getVillagerXp() + " at " + villager.blockPosition().toShortString()
					+ ", " + (int) Math.sqrt(villager.blockPosition().distSqr(block)) + " blocks from its block (" + taken + "), job site now held by "
					+ (holders.isEmpty() ? "nobody" : holders.stream().map(h -> (((HiredVillager) h).workingvillagers$isHired() ? "hired " : "")
						+ h.getVillagerData().profession().getRegisteredName()).toList()) + ", sleeping " + villager.isSleeping()
					+ ", activity " + villager.getBrain().getActiveNonCoreActivity().map(Object::toString).orElse("-"));
			}
		}
	}

	private void sample(ServerLevel level) {
		this.sampleCustomers(level);
		for (SimRegistry.Worker worker : this.registry.workers) {
			Entity entity = level.getEntity(worker.uuid());
			if (!(entity instanceof Villager villager) || !villager.isAlive()) {
				if (this.dead.add(worker.uuid())) {
					this.event(level, "The " + worker.job() + " of wing " + (worker.wing() + 1) + " is gone (dead, converted or unloaded) "
						+ (entity == null ? "" : entity.getType().toShortString()));
				}
				continue;
			}
			if (!villager.getBrain().isActive(Activity.WORK)) {
				continue;
			}
			HiredVillager hired = (HiredVillager) villager;
			String status = key(WorkerStatus.describe(villager));
			if (!hired.workingvillagers$isHired()) {
				status = "NOT HIRED";
			}
			this.daySamples.computeIfAbsent(worker.job(), j -> new TreeMap<>()).merge(status, 1, Integer::sum);
			this.dayWorkSamples.merge(worker.job(), 1, Integer::sum);
			if (hired.workingvillagers$hasProblem() || hired.workingvillagers$getStuckWatch().isStuck()) {
				this.dayProblemSamples.merge(worker.job(), 1, Integer::sum);
			}
			if (Meals.isHungry(villager)) {
				this.dayHungrySamples.merge(worker.job(), 1, Integer::sum);
			}
		}
	}

	/** The translation key of a status (without the common prefix), or its text. */
	static String key(Component component) {
		if (component.getContents() instanceof TranslatableContents contents) {
			String key = contents.getKey();
			if (key.endsWith(".hungry") && contents.getArgs().length > 0 && contents.getArgs()[0] instanceof Component task) {
				return "hungry/" + key(task);
			}
			int at = key.indexOf("workingvillagers.");
			return at >= 0 ? key.substring(at + "workingvillagers.".length()) : key;
		}
		return component.getString();
	}

	private void finishHour(ServerLevel level, long time) {
		long[] ticks = Arrays.copyOf(this.hourTicks, this.hourTickCount);
		this.hourTickCount = 0;
		Arrays.sort(ticks);
		int n = ticks.length;
		double mean = n == 0 ? 0 : Arrays.stream(ticks).average().orElse(0) / 1e6;
		double p50 = n == 0 ? 0 : ticks[n / 2] / 1e6;
		double p95 = n == 0 ? 0 : ticks[Math.min(n - 1, (int) (n * 0.95))] / 1e6;
		double p99 = n == 0 ? 0 : ticks[Math.min(n - 1, (int) (n * 0.99))] / 1e6;
		double max = n == 0 ? 0 : ticks[n - 1] / 1e6;
		long over = Arrays.stream(ticks).filter(t -> t > 50_000_000L).count();
		int entities = 0, villagers = 0, monsters = 0, animals = 0, items = 0, golems = 0;
		for (Entity entity : level.getAllEntities()) {
			entities++;
			if (entity instanceof Villager) {
				villagers++;
			} else if (entity instanceof Enemy) {
				monsters++;
			} else if (entity instanceof Animal) {
				animals++;
			} else if (entity instanceof ItemEntity) {
				items++;
			} else if (entity instanceof IronGolem) {
				golems++;
			}
		}
		int alive = this.registry.workers.size() - this.dead.size();
		long day = this.hour * HOUR / 24000L - this.startDay;
		long timeOfDay = this.hour * HOUR % 24000L;
		int chunks = level.getChunkSource().getLoadedChunksCount();
		this.append("ticks.csv", String.format(Locale.ROOT, "%d,%d,%d,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%d,%d,%d,%d,%d,%d,%d,%d,%d",
			this.hour, day, timeOfDay, n, mean, p50, p95, p99, max, over, entities, villagers, alive, monsters, animals, items, golems, chunks));
		this.hourRows.add(new double[] {day, timeOfDay, n, mean, p50, p95, p99, max, over, entities, villagers, alive, monsters, animals, items, golems});
		if (timeOfDay % 6000 == 0) {
			long employed = this.registry.traders.stream().filter(trader -> level.getEntity(trader.uuid()) instanceof Villager villager
				&& !villager.getVillagerData().profession().is(net.minecraft.world.entity.npc.villager.VillagerProfession.NONE)).count();
			LOG.info("Day {} {}:00  tick mean {} ms, p95 {} ms, max {} ms; {} entities, {} villagers, {} monsters; {} of {} customers have a job", day,
				(timeOfDay / 1000 + 6) % 24, String.format(Locale.ROOT, "%.2f", mean), String.format(Locale.ROOT, "%.2f", p95), String.format(Locale.ROOT, "%.1f", max),
				entities, villagers, monsters, employed, this.registry.traders.size());
		}
		// The last hour of a day (time of day 23000): the day is over, write its numbers
		if (timeOfDay == 23000) {
			this.finishDay(level, day);
		}
	}

	private void finishDay(ServerLevel level, long simDay) {
		// Called on the first tick of the next hour: the day of the hour that just ended
		long today = this.hour * HOUR / 24000L;
		// Worker states
		for (Map.Entry<String, Map<String, Integer>> job : this.daySamples.entrySet()) {
			for (Map.Entry<String, Integer> status : job.getValue().entrySet()) {
				this.append("workers.csv", simDay + "," + csv(job.getKey()) + "," + csv(status.getKey()) + "," + status.getValue());
			}
		}
		for (Map.Entry<String, Integer> job : this.dayWorkSamples.entrySet()) {
			int[] totals = this.jobTotals.computeIfAbsent(job.getKey(), j -> new int[2]);
			totals[0] += job.getValue();
			totals[1] += this.dayProblemSamples.getOrDefault(job.getKey(), 0);
		}
		Map<String, Map<String, Integer>> statuses = new TreeMap<>();
		this.daySamples.forEach((job, map) -> statuses.put(job, new TreeMap<>(map)));
		int workSamples = this.dayWorkSamples.values().stream().mapToInt(Integer::intValue).sum();
		int hungrySamples = this.dayHungrySamples.values().stream().mapToInt(Integer::intValue).sum();
		Map<String, Integer> hungryByJob = new TreeMap<>(this.dayHungrySamples);
		this.dayHungrySamples.clear();
		this.daySamples.clear();
		this.dayWorkSamples.clear();
		this.dayProblemSamples.clear();

		// Production: what each worker did today (logs, crops, emeralds earned, ...)
		int paid = 0, unpaid = 0, shopEarned = 0, alive = 0, carried = 0, purses = 0, hungryAtEnd = 0, bagPoints = 0;
		float hunger = 0.0F;
		Map<String, Integer> production = new TreeMap<>();
		for (SimRegistry.Worker worker : this.registry.workers) {
			if (!(level.getEntity(worker.uuid()) instanceof Villager villager) || !villager.isAlive()) {
				continue;
			}
			alive++;
			HiredVillager hired = (HiredVillager) villager;
			int done = hired.workingvillagers$getWorkDone(today);
			this.append("production.csv", simDay + "," + csv(worker.job()) + "," + worker.uuid().toString().substring(0, 8) + "," + done);
			production.merge(worker.job(), done, Integer::sum);
			if (worker.job().startsWith("Shop assistant")) {
				shopEarned += done;
			}
			if (hired.workingvillagers$getPaidDay() >= today) {
				paid++;
			} else {
				unpaid++;
			}
			carried += WorkerSupplies.count(villager.getInventory(), WorkerSupplies::isWage);
			MealData meals = Meals.of(villager);
			purses += meals.purse();
			hunger += meals.hunger();
			hungryAtEnd += Meals.isHungry(villager) ? 1 : 0;
			for (ItemStack stack : meals.bag()) {
				if (!stack.isEmpty() && stack.get(net.minecraft.core.component.DataComponents.FOOD) instanceof net.minecraft.world.food.FoodProperties food) {
					bagPoints += food.nutrition() * stack.getCount();
				}
			}
		}
		int wageBoxes = 0;
		Set<BlockPos> stations = new HashSet<>();
		for (SimRegistry.Worker worker : this.registry.workers) {
			if (!worker.job().equals("carrier") && stations.add(worker.station())) {
				wageBoxes += Workstations.find(level, worker.station()).map(Workstations.Station::wage).orElse(0);
			}
		}
		int villageChests = emeralds(level, this.registry.villageChests);
		int tills = emeralds(level, this.registry.tills);
		int storage = emeralds(level, this.registry.storage);
		List<BlockPos> transit = new ArrayList<>(this.registry.depots);
		transit.addAll(this.registry.relays);
		int depots = emeralds(level, transit);
		int total = wageBoxes + villageChests + tills + storage + depots + carried + purses;
		this.append("economy.csv", String.format(Locale.ROOT, "%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d", simDay, alive, paid, unpaid, wageBoxes, villageChests, tills,
			storage, depots, carried, purses, total, shopEarned, this.dead.size()));
		Map<String, Integer> pantry = contents(level, this.registry.pantries);
		int pantryEmeralds = pantry.getOrDefault("emerald", 0);
		int pantryFood = pantry.entrySet().stream().filter(entry -> !entry.getKey().equals("emerald")).mapToInt(Map.Entry::getValue).sum();
		float meanHunger = alive == 0 ? 0.0F : hunger / alive;
		double hungryShare = workSamples == 0 ? 0.0 : (double) hungrySamples / workSamples;
		this.append("meals.csv", String.format(Locale.ROOT, "%d,%d,%d,%.2f,%d,%.3f,%d,%d", simDay, purses, hungryAtEnd, meanHunger, bagPoints, hungryShare, pantryFood, pantryEmeralds));

		// Storage contents
		Map<String, Integer> contents = contents(level, this.registry.storage);
		contents.forEach((item, count) -> this.append("storage.csv", simDay + "," + item + "," + count));
		if (this.storageAtStart == null) {
			this.storageAtStart = contents;
		}
		int slotsUsed = 0, slots = 0;
		for (BlockPos pos : this.registry.storage) {
			Container container = Workstations.getContainer(level, pos);
			if (container != null) {
				slots += container.getContainerSize();
				for (int i = 0; i < container.getContainerSize(); i++) {
					slotsUsed += container.getItem(i).isEmpty() ? 0 : 1;
				}
			}
		}
		// The customers of the shops: still there, still with a job, and how far they rose
		int tradersAlive = 0, tradersWithJob = 0;
		Map<String, Integer> traderLevels = new TreeMap<>();
		for (SimRegistry.Trader trader : this.registry.traders) {
			if (!(level.getEntity(trader.uuid()) instanceof Villager villager) || !villager.isAlive()) {
				continue;
			}
			tradersAlive++;
			if (!villager.getVillagerData().profession().is(net.minecraft.world.entity.npc.villager.VillagerProfession.NONE)) {
				tradersWithJob++;
			}
			traderLevels.merge(trader.profession() + " L" + villager.getVillagerData().level(), 1, Integer::sum);
		}
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("day", simDay);
		row.put("tradersAlive", tradersAlive);
		row.put("tradersWithJob", tradersWithJob);
		row.put("traderLevels", traderLevels);
		row.put("villagersDied", this.villagerDeaths);
		row.put("alive", alive);
		row.put("paid", paid);
		row.put("unpaid", unpaid);
		row.put("wageBoxes", wageBoxes);
		row.put("villageChests", villageChests);
		row.put("tills", tills);
		row.put("storageEmeralds", storage);
		row.put("transit", depots);
		row.put("carried", carried);
		row.put("purses", purses);
		row.put("total", total);
		Map<String, Object> mealsRow = new LinkedHashMap<>();
		mealsRow.put("hungryAtEnd", hungryAtEnd);
		mealsRow.put("meanHunger", meanHunger);
		mealsRow.put("bagFoodPoints", bagPoints);
		mealsRow.put("hungryWorkShare", hungryShare);
		mealsRow.put("hungrySamplesByJob", hungryByJob);
		mealsRow.put("pantry", pantry);
		Map<String, Integer> pantryChange = new TreeMap<>();
		pantry.forEach((item, count) -> pantryChange.put(item, count - this.pantryLast.getOrDefault(item, 0)));
		this.pantryLast.forEach((item, count) -> pantryChange.putIfAbsent(item, -count));
		mealsRow.put("pantryChange", pantryChange);
		this.pantryLast = pantry;
		row.put("meals", mealsRow);
		row.put("shopEarned", shopEarned);
		row.put("deaths", this.dead.size());
		row.put("storageSlotsUsed", slotsUsed);
		row.put("storageSlots", slots);
		row.put("statuses", statuses);
		row.put("production", production);
		row.put("storage", contents);
		Map<String, Integer> added = new TreeMap<>();
		contents.forEach((item, count) -> {
			int diff = count - this.storageLast.getOrDefault(item, 0);
			if (diff != 0) {
				added.put(item, diff);
			}
		});
		row.put("storageAdded", added);
		Map<String, Map<String, Integer>> deliveries = SimDeliveries.takeDay();
		deliveries.forEach((job, items) -> items.forEach((key, count) -> {
			String[] parts = key.split(" ", 2);
			this.append("deliveries.csv", simDay + "," + csv(job) + "," + parts[0] + "," + parts[1] + "," + count);
		}));
		row.put("deliveries", deliveries);
		Map<String, Integer> workersByJob = new TreeMap<>();
		for (SimRegistry.Worker worker : this.registry.workers) {
			if (level.getEntity(worker.uuid()) instanceof Villager villager && villager.isAlive()) {
				workersByJob.merge(worker.job().equals("carrier") ? "Carrier" : worker.job(), 1, Integer::sum);
			}
		}
		row.put("workersByJob", workersByJob);
		// Every worker at the end of the day: what it is doing, where, and what it carries
		List<Map<String, Object>> snapshot = new ArrayList<>();
		for (SimRegistry.Worker worker : this.registry.workers) {
			if (!(level.getEntity(worker.uuid()) instanceof Villager villager) || !villager.isAlive()) {
				continue;
			}
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("job", worker.job());
			entry.put("wing", worker.wing() + 1);
			entry.put("status", key(WorkerStatus.describe(villager)));
			entry.put("problem", ((HiredVillager) villager).workingvillagers$hasProblem());
			entry.put("pos", villager.blockPosition().toShortString());
			List<String> carries = new ArrayList<>();
			for (ItemStack stack : villager.getInventory()) {
				if (!stack.isEmpty()) {
					carries.add(stack.getCount() + " " + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()
						+ (stack.isDamageableItem() ? " (" + (stack.getMaxDamage() - stack.getDamageValue()) + "/" + stack.getMaxDamage() + ")" : ""));
				}
			}
			entry.put("carries", carries);
			MealData meals = Meals.of(villager);
			entry.put("purse", meals.purse());
			entry.put("hunger", meals.hunger());
			List<String> bag = new ArrayList<>();
			for (ItemStack stack : meals.bag()) {
				if (!stack.isEmpty()) {
					bag.add(stack.getCount() + " " + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
				}
			}
			entry.put("bag", bag);
			snapshot.add(entry);
		}
		row.put("workers", snapshot);
		this.storageLast = contents;
		this.dayRows.add(row);
		LOG.info("Day {} done: {} of {} workers paid, emeralds {} (shop earned {}, in purses {}), storage {} / {} slots; hungry {} at the end, {} % of work hungry, pantry {} food {} emeralds",
			simDay, paid, alive, total, shopEarned, purses, slotsUsed, slots, hungryAtEnd, String.format(Locale.ROOT, "%.1f", 100 * hungryShare), pantryFood, pantryEmeralds);
	}

	static int emeralds(ServerLevel level, List<BlockPos> chests) {
		int count = 0;
		for (BlockPos pos : chests) {
			Container container = Workstations.getContainer(level, pos);
			if (container != null) {
				count += WorkerSupplies.count(container, stack -> stack.is(Items.EMERALD));
			}
		}
		return count;
	}

	static Map<String, Integer> contents(ServerLevel level, List<BlockPos> chests) {
		Map<String, Integer> counts = new TreeMap<>();
		for (BlockPos pos : chests) {
			Container container = Workstations.getContainer(level, pos);
			if (container == null) {
				continue;
			}
			for (int i = 0; i < container.getContainerSize(); i++) {
				ItemStack stack = container.getItem(i);
				if (!stack.isEmpty()) {
					counts.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath(), stack.getCount(), Integer::sum);
				}
			}
		}
		return counts;
	}

	private int villagerDeaths;

	/** A villager died: who, and of what. */
	void died(ServerLevel level, Villager villager, net.minecraft.world.damagesource.DamageSource source) {
		this.villagerDeaths++;
		String who = villager.hasCustomName() ? villager.getCustomName().getString()
			: villager.getVillagerData().profession().getRegisteredName().replace("minecraft:", "") + " (customer)";
		this.event(level, "Died: " + who + " at " + villager.blockPosition().toShortString() + ": " + source.getLocalizedDeathMessage(villager).getString());
	}

	void converted(ServerLevel level, Villager villager, Entity into) {
		String who = villager.hasCustomName() ? villager.getCustomName().getString() : villager.getVillagerData().profession().getRegisteredName();
		this.event(level, "Turned into " + into.getType().toShortString() + ": " + who + " at " + villager.blockPosition().toShortString());
	}

	void event(ServerLevel level, String text) {
		long time = level.getOverworldClockTime();
		String line = "day " + (time / 24000L - this.startDay) + " " + String.format(Locale.ROOT, "%02d:%02d", (time % 24000 / 1000 + 6) % 24, time % 1000 * 60 / 1000) + " " + text;
		this.events.add(line);
		this.append("events.log", line);
		LOG.info(line);
	}

	private static String csv(String text) {
		return text.contains(",") ? "\"" + text.replace("\"", "'") + "\"" : text;
	}

	/** Writes {@code summary.json} with every number the report needs. */
	void writeSummary(MinecraftServer server, List<String> warnings) {
		Map<String, Object> summary = new LinkedHashMap<>();
		summary.put("wings", this.registry.wings);
		summary.put("workers", this.registry.workers.size());
		summary.put("traders", this.registry.traders.size());
		Map<String, Integer> jobs = new TreeMap<>();
		this.registry.workers.forEach(w -> jobs.merge(w.job(), 1, Integer::sum));
		summary.put("jobs", jobs);
		summary.put("warnings", warnings);
		summary.put("events", this.events);
		Map<String, Object> problems = new TreeMap<>();
		this.jobTotals.forEach((job, totals) -> problems.put(job, Map.of("samples", totals[0], "problemSamples", totals[1])));
		summary.put("problemShare", problems);
		summary.put("days", this.dayRows);
		List<Map<String, Object>> hours = new ArrayList<>();
		String[] names = {"day", "timeOfDay", "ticks", "mean", "p50", "p95", "p99", "max", "over50", "entities", "villagers", "workersAlive", "monsters", "animals", "items", "golems"};
		for (double[] row : this.hourRows) {
			Map<String, Object> map = new LinkedHashMap<>();
			for (int i = 0; i < names.length; i++) {
				map.put(names[i], row[i]);
			}
			hours.add(map);
		}
		summary.put("hours", hours);
		try {
			Files.writeString(this.out.resolve("summary.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(summary));
		} catch (IOException e) {
			LOG.warn("Could not write the summary", e);
		}
	}

}
