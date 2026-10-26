package me.teajay.workingvillagers.job;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.station.StationType;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Village chests: chests every worker nearby takes its supplies from (see
 * {@link WorkerSupplies}). A worker uses each village chest within
 * {@link Config#VILLAGE_CHEST_RADIUS} blocks of its workplace (horizontally, and up to
 * {@link #VERTICAL_REACH} up or down), and only takes what runs low: the wage for a few days
 * once it has less than a day's wage, a spare tool once it has none or only an almost used up
 * one, and saplings, seeds, ... up to the amount it keeps once it has less than a quarter of
 * that. It never takes a worn tool ({@link WorkerSupplies#isWorn}): those are given back here.
 */
public final class VillageChests {
	/** How far up or down a village chest may be from the workplace (the pit digger's pit). */
	public static final int VERTICAL_REACH = 24;

	private VillageChests() {
	}

	/** One move out of a village chest: that many items of the stack in that slot. */
	record Take(int slot, int count) {
	}

	/** The village chests in reach of a worker that works at the anchor, the closest first. */
	public static List<BlockPos> inReach(ServerLevel level, BlockPos anchor) {
		if (!Config.VILLAGE_CHESTS.get()) {
			return List.of();
		}
		int radius = Config.VILLAGE_CHEST_RADIUS.get();
		return level.getPoiManager()
			.getInSquare(poi -> poi.is(WorkingVillagers.VILLAGE_CHEST_POI), anchor, radius, PoiManager.Occupancy.ANY)
			.map(record -> record.getPos())
			.filter(chest -> inReach(anchor, chest))
			.sorted(Comparator.comparingDouble(chest -> chest.distSqr(anchor)))
			.toList();
	}

	public static boolean inReach(BlockPos anchor, BlockPos chest) {
		int radius = Config.VILLAGE_CHEST_RADIUS.get();
		double dx = chest.getX() - anchor.getX();
		double dz = chest.getZ() - anchor.getZ();
		return dx * dx + dz * dz <= (double) radius * radius && Math.abs(chest.getY() - anchor.getY()) <= VERTICAL_REACH;
	}

	/** The hired workers whose workplace has the village chest in reach. */
	public static List<Villager> served(ServerLevel level, BlockPos chest) {
		int radius = Config.VILLAGE_CHEST_RADIUS.get();
		// Workers are near their workplace while they work; at night they may be farther off
		AABB area = new AABB(chest).inflate(radius + 32, VERTICAL_REACH + 16, radius + 32);
		return level.getEntitiesOfClass(Villager.class, area, villager -> ((HiredVillager) villager).workingvillagers$isHired()
			&& !villager.isBaby() && inReach(WorkerSupplies.anchor(level, villager), chest));
	}

	/**
	 * Whether a hired smith (smithing table) or weaponsmith (grindstone that grinds tools
	 * together) has the village chest in reach and so repairs the worn tools given back there.
	 */
	public static boolean repairsWornTools(ServerLevel level, BlockPos chest) {
		return served(level, chest).stream().anyMatch(worker -> Workstations.find(level, workplace(level, worker))
			.filter(station -> station.type() == StationType.SMITHING_TABLE || station.type() == StationType.GRINDSTONE && station.merges())
			.isPresent());
	}

	/** Where the worker works: its workstation, the source of a carrier's first route. */
	public static BlockPos workplace(ServerLevel level, Villager worker) {
		return WorkerSupplies.anchor(level, worker);
	}

	/**
	 * What the worker cannot work without and finds in none of its village chests, as the
	 * kind of the supply ({@code "wage"}, {@code "axe"}, {@code "ladders"}, ...), or
	 * {@code "food"} if it eats out; empty if it lacks nothing (or knows where to get it).
	 */
	public static Optional<String> missing(ServerLevel level, Villager worker) {
		SimpleContainer inventory = worker.getInventory();
		List<Map.Entry<String, Predicate<ItemStack>>> lacking = new ArrayList<>();
		if (WorkerSupplies.lacksWage(level, worker)) {
			lacking.add(Map.entry("wage", WorkerSupplies::isWage));
		}
		for (WorkerSupplies.Tool tool : WorkerSupplies.toolsOf(worker)) {
			if (WorkerSupplies.count(inventory, tool.ready()) == 0) {
				lacking.add(Map.entry(tool.kind(), tool.ready()));
			}
		}
		for (WorkerSupplies.Stock stock : WorkerSupplies.stocksOf(worker)) {
			if (stock.required() && WorkerSupplies.count(inventory, stock.item()) == 0) {
				lacking.add(Map.entry(stock.kind(), stock.item()));
			}
		}
		if (Meals.eatsOut(worker)) {
			// It can work without, but its food money leaves the village
			lacking.add(Map.entry("food", Meals::isMeal));
		}
		if (lacking.isEmpty()) {
			return Optional.empty();
		}
		List<Container> chests = new ArrayList<>();
		for (BlockPos chest : inReach(level, workplace(level, worker))) {
			Container container = Workstations.getContainer(level, chest);
			if (container != null) {
				chests.add(container);
			}
		}
		return lacking.stream()
			.filter(need -> chests.stream().noneMatch(container -> offers(worker, container, need.getValue())))
			.map(Map.Entry::getKey)
			.findFirst();
	}

	/** Whether the worker would take something from the container now (or buy food there); with {@code only}, something of that. */
	static boolean offers(Villager villager, Container container, Predicate<ItemStack> only) {
		return plan(villager, container).stream().anyMatch(take -> only.test(container.getItem(take.slot())))
			|| Meals.plan(villager, container).stream().anyMatch(take -> only.test(container.getItem(take.slot())));
	}

	/** Whether the worker runs low on anything it could take from a village chest. */
	static boolean runsLow(Villager villager) {
		SimpleContainer inventory = villager.getInventory();
		if (wageWanted(villager) > 0) {
			return true;
		}
		for (WorkerSupplies.Tool tool : WorkerSupplies.toolsOf(villager)) {
			if (toolLow(inventory, tool)) {
				return true;
			}
		}
		for (WorkerSupplies.Stock stock : WorkerSupplies.stocksOf(villager)) {
			if (stockLow(inventory, stock)) {
				return true;
			}
		}
		return Uniforms.wearsArmor(villager) && Uniforms.SLOTS.stream().anyMatch(slot -> Uniforms.lacks(villager, slot))
			|| Meals.wantsFood(villager);
	}

	/** Moves what the worker wants out of the container into its inventory. Returns how many items it took. */
	static int take(Villager villager, Container container) {
		int taken = 0;
		for (Take take : plan(villager, container)) {
			ItemStack stack = container.getItem(take.slot());
			ItemStack moved = stack.split(take.count());
			// The wage goes into the wage box of the workstation, the rest into the pockets
			ItemStack rest = WorkerSupplies.store(villager, moved.copy());
			stack.grow(rest.getCount());
			taken += moved.getCount() - rest.getCount();
		}
		if (taken > 0) {
			container.setChanged();
		}
		return taken + Meals.buy(villager, container);
	}

	/** What the worker would take out of the container now, slot by slot. */
	static List<Take> plan(Villager villager, Container container) {
		List<Take> takes = new ArrayList<>();
		if (!((HiredVillager) villager).workingvillagers$isHired() || villager.isBaby()) {
			return takes;
		}
		SimpleContainer inventory = villager.getInventory();
		int wage = fairShare(villager, container, wageWanted(villager));
		for (int slot = 0; slot < container.getContainerSize() && wage > 0; slot++) {
			ItemStack stack = container.getItem(slot);
			if (!stack.isEmpty() && WorkerSupplies.isWage(stack)) {
				int count = Math.min(stack.getCount(), wage);
				takes.add(new Take(slot, count));
				wage -= count;
			}
		}
		for (WorkerSupplies.Tool tool : WorkerSupplies.toolsOf(villager)) {
			if (!toolLow(inventory, tool)) {
				continue;
			}
			boolean hasNone = WorkerSupplies.count(inventory, tool.ready()) == 0;
			int best = -1;
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);
				if (stack.isEmpty() || !tool.ready().test(stack) || !mayTake(stack) || (!hasNone && isAlmostUsedUp(stack))) {
					continue;
				}
				// The one with the fewest uses left: worn tools are used up, cheap ones before good ones
				if (best < 0 || usesLeft(stack) < usesLeft(container.getItem(best))) {
					best = slot;
				}
			}
			if (best >= 0) {
				takes.add(new Take(best, 1));
			}
		}
		if (Uniforms.wearsArmor(villager)) {
			for (EquipmentSlot slot : Uniforms.SLOTS) {
				if (!Uniforms.lacks(villager, slot)) {
					continue;
				}
				for (int i = 0; i < container.getContainerSize(); i++) {
					ItemStack stack = container.getItem(i);
					if (Uniforms.slotOf(stack) == slot && !WorkerSupplies.isWorn(stack) && mayTake(stack)) {
						takes.add(new Take(i, 1));
						break;
					}
				}
			}
		}
		for (WorkerSupplies.Stock stock : WorkerSupplies.stocksOf(villager)) {
			if (!stockLow(inventory, stock)) {
				continue;
			}
			int keep = stock.keep().getAsInt();
			int room = keep - WorkerSupplies.count(inventory, stock.item());
			Map<Item, Integer> perKind = new HashMap<>();
			// Kinds it already has first (the saplings of its trees, the seeds of its fields)
			for (boolean known : new boolean[] {true, false}) {
				for (int slot = 0; slot < container.getContainerSize() && room > 0; slot++) {
					ItemStack stack = container.getItem(slot);
					if (stack.isEmpty() || !stock.item().test(stack) || hasKind(inventory, stack.getItem()) != known) {
						continue;
					}
					int had = perKind.computeIfAbsent(stack.getItem(), item -> WorkerSupplies.count(inventory, other -> other.is(item)));
					int count = Math.min(Math.min(stack.getCount(), room), keep - had);
					if (count > 0) {
						takes.add(new Take(slot, count));
						perKind.put(stack.getItem(), had + count);
						room -= count;
					}
				}
			}
		}
		return takes;
	}

	/**
	 * The emeralds it takes: the wage for a few days, once it has less than a day's wage in
	 * the wage box of its workstation and its pockets. They go into the wage box.
	 */
	private static int wageWanted(Villager villager) {
		int wage = WorkerLevels.dailyWage(villager);
		if (wage <= 0 || !(villager.level() instanceof ServerLevel level)) {
			return 0;
		}
		int has = WorkerSupplies.emeralds(level, villager);
		if (has >= wage) {
			return 0;
		}
		Workstations.Station box = WorkerSupplies.wageBox(level, villager);
		int room = box == null ? WorkerSupplies.MAX_EMERALDS : Workstations.MAX_WAGE;
		return Math.max(0, Math.min(wage * Config.VILLAGE_CHEST_WAGE_DAYS.get(), room) - has);
	}

	/**
	 * The emeralds it takes of the {@code wanted} ones: the wage for a few days only while the
	 * chest holds that much for every worker it serves, otherwise its share of them, but at
	 * least what it needs for today. So no worker fills its wage box for days while the others
	 * find the chest empty.
	 */
	private static int fairShare(Villager villager, Container container, int wanted) {
		if (wanted <= 0 || !(villager.level() instanceof ServerLevel level) || !(container instanceof BlockEntity chest)) {
			return wanted;
		}
		int today = Math.max(0, Config.DAILY_WAGE.get() - WorkerSupplies.emeralds(level, villager));
		if (wanted <= today) {
			return wanted;
		}
		int stored = WorkerSupplies.count(container, WorkerSupplies::isWage);
		int workers = Math.max(1, served(level, chest.getBlockPos()).size());
		return Math.max(today, Math.min(wanted, stored / workers));
	}

	/** No tool of the kind it can work with, or only one that is almost used up. */
	private static boolean toolLow(SimpleContainer inventory, WorkerSupplies.Tool tool) {
		List<ItemStack> carried = new ArrayList<>();
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && tool.ready().test(stack)) {
				carried.add(stack);
			}
		}
		return carried.isEmpty() || carried.size() == 1 && carried.getFirst().getCount() == 1 && isAlmostUsedUp(carried.getFirst());
	}

	/** Less than a quarter of what it keeps. */
	private static boolean stockLow(SimpleContainer inventory, WorkerSupplies.Stock stock) {
		int keep = stock.keep().getAsInt();
		return keep > 0 && WorkerSupplies.count(inventory, stock.item()) < Math.max(1, keep / 4);
	}

	private static boolean hasKind(SimpleContainer inventory, Item item) {
		return inventory.hasAnyMatching(stack -> !stack.isEmpty() && stack.is(item));
	}

	/** Enchanted and renamed tools stay for the player, unless the config says otherwise. */
	private static boolean mayTake(ItemStack tool) {
		return Config.VILLAGE_CHEST_ENCHANTED.get() || !tool.isEnchanted() && !tool.has(DataComponents.CUSTOM_NAME);
	}

	/**
	 * A tool with a tenth of its durability or less left. Unless tools are used up (see
	 * {@link WorkerSupplies#isWorn}), the worker gives it back about then, so it takes a spare.
	 */
	private static boolean isAlmostUsedUp(ItemStack tool) {
		return tool.isDamageableItem() && usesLeft(tool) * 10 <= tool.getMaxDamage();
	}

	private static int usesLeft(ItemStack tool) {
		return tool.isDamageableItem() ? tool.getMaxDamage() - tool.getDamageValue() : Integer.MAX_VALUE;
	}
}
