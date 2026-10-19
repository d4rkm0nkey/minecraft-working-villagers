package me.teajay.workingvillagers.job;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.station.StationType;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Repairable;
import org.jspecify.annotations.Nullable;

/**
 * The work of the smith at the smithing table: repair tools. It fetches up to {@link #BATCH}
 * tools from one chest, repairs them at the smithing table with material from its input
 * chests and puts them back into the chest they came from.
 *
 * <p>It repairs the worn tools the workers gave back into the village chests in reach of the
 * smithing table ({@link WorkerSupplies#isWorn}), and every damaged item in its input chests,
 * where the player can put tools and armor. An item can be repaired if it has the vanilla
 * {@code repairable} component and its material lies in an input chest: iron ingots for iron
 * tools, diamonds for diamond tools, planks for wooden ones, ... One material repairs a
 * quarter of the durability, as at an anvil, but it costs no experience and does not get
 * more expensive; enchantments and names stay.
 *
 * <p>With bottles o' enchanting (the guards fill them, see {@link GuardPost}) it also repairs
 * what has no repair material (bows, crossbows, fishing rods, shears): one bottle repairs
 * {@link Config#EXPERIENCE_REPAIR_PERCENT} % of the durability. An enchanted item costs a
 * bottle per step on top of the material ({@link Config#ENCHANTED_NEEDS_EXPERIENCE}), as an
 * anvil costs experience. It takes the bottles from its input chests or the village chests in
 * reach.
 *
 * <p>Without an input chest the smith does not work at the smithing table. If the chest the
 * tools came from is full by the time they are done, they go into another village chest, an
 * output chest or an input chest.
 */
public class RepairTools extends Behavior<Villager> {
	private static final float SPEED = 0.5F;
	private static final int WALK_TIMEOUT = 400;
	private static final int IDLE_TICKS = 100;
	/** Fallback if there is no free spot next to the smithing table. */
	private static final double TABLE_REACH = 2.0;
	/** At most this many tools it takes along at once. */
	static final int BATCH = 4;
	/** How long one material takes at the smithing table. */
	private static final int REPAIR_TICKS = 40;
	/** At most this much material it carries. */
	private static final int MAX_MATERIAL = 16;
	/** How long a chest it could not get at is left alone. */
	private static final long UNREACHABLE_TICKS = 1200L;

	private enum State { PLAN, FETCH, WALK_TO_TABLE, REPAIR, RETURN, WAIT }

	private State state = State.PLAN;
	private int stateTicks;
	private @Nullable ChestTrip trip;
	private @Nullable BlockPos tableSpot;
	/** The chest the carried tools came from and go back to. Not saved: after a restart they go to an input chest. */
	private @Nullable BlockPos origin;
	/** Whether the current trip fetches tools (or else material). */
	private boolean fetchingTools;
	/** Chests it could not get at, until when it leaves them alone. */
	private final Map<BlockPos, Long> unreachable = new HashMap<>();
	private String waitReason = "smith.no_repairs";
	private Object[] waitArgs = {};
	private final WorkerSupplies supplies = new WorkerSupplies();

	public RepairTools() {
		super(ImmutableMap.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT), 24000);
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
		return this.canWork(level, villager);
	}

	@Override
	protected boolean canStillUse(ServerLevel level, Villager villager, long timestamp) {
		return this.canWork(level, villager) && villager.getBrain().isActive(Activity.WORK);
	}

	private boolean canWork(ServerLevel level, Villager villager) {
		if (!villager.getVillagerData().profession().is(WorkingVillagers.SMITH_PROFESSION)) {
			return false;
		}
		Workstations.Station station = getWorkplace(level, villager);
		return station != null && station.type() == StationType.SMITHING_TABLE && !station.validInputs(level).isEmpty();
	}

	private static Workstations.@Nullable Station getWorkplace(ServerLevel level, Villager villager) {
		Optional<GlobalPos> jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
		if (jobSite.isEmpty() || jobSite.get().dimension() != level.dimension()) {
			return null;
		}
		return Workstations.find(level, jobSite.get().pos()).orElse(null);
	}

	@Override
	protected void start(ServerLevel level, Villager villager, long timestamp) {
		this.setState(State.PLAN);
	}

	@Override
	protected void stop(ServerLevel level, Villager villager, long timestamp) {
		WorkerStatus.clear(villager);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
		this.supplies.stop(villager);
		if (this.trip != null) {
			this.trip.close(villager);
			this.trip = null;
		}
	}

	@Override
	protected void tick(ServerLevel level, Villager villager, long timestamp) {
		Workstations.Station station = getWorkplace(level, villager);
		if (station == null) {
			return;
		}
		this.stateTicks++;
		this.unreachable.values().removeIf(until -> until < timestamp);
		if (this.state == State.PLAN && this.supplies.busy(level, villager)) {
			return; // wage first, the chest sets the status
		}
		switch (this.state) {
			case PLAN -> this.plan(level, villager, station);
			case FETCH -> this.fetch(level, villager, station);
			case WALK_TO_TABLE -> this.walkToTable(villager, station);
			case REPAIR -> this.repair(level, villager, station);
			case RETURN -> this.giveBack(level, villager, station);
			case WAIT -> {
				if (this.stateTicks >= IDLE_TICKS) {
					this.setState(State.PLAN);
				}
			}
		}
		this.report(villager);
	}

	private void report(Villager villager) {
		switch (this.state) {
			case PLAN, FETCH -> WorkerStatus.task(villager, this.fetchingTools ? "smith.fetch_tools" : "smith.fetch_material");
			case WALK_TO_TABLE, REPAIR -> WorkerStatus.task(villager, "smith.repair");
			case RETURN -> WorkerStatus.task(villager, "smith.return_tools");
			case WAIT -> WorkerStatus.task(villager, this.waitReason, this.waitArgs);
		}
	}

	/** Whether the item can be repaired at all: damaged, with a material for it or with bottles o' enchanting. */
	static boolean isRepairable(ItemStack stack) {
		return !stack.isEmpty() && stack.isDamageableItem() && stack.isDamaged();
	}

	/** Items without a repair material (bow, fishing rod, shears) are repaired with bottles o' enchanting alone. */
	static boolean usesExperience(ItemStack tool) {
		return !tool.has(DataComponents.REPAIRABLE);
	}

	/** Whether a repair step costs a bottle o' enchanting. */
	static boolean needsBottle(ItemStack tool) {
		return usesExperience(tool) || tool.isEnchanted() && Config.ENCHANTED_NEEDS_EXPERIENCE.get();
	}

	static boolean isBottle(ItemStack stack) {
		return stack.is(Items.EXPERIENCE_BOTTLE);
	}

	/** How much one step repairs: a quarter with material, a share with a bottle alone. */
	private static int stepSize(ItemStack tool) {
		return usesExperience(tool)
			? Math.max(1, tool.getMaxDamage() * Config.EXPERIENCE_REPAIR_PERCENT.get() / 100)
			: Math.max(1, tool.getMaxDamage() / 4);
	}

	/** Whether the material repairs the tool. */
	static boolean repairs(ItemStack tool, ItemStack material) {
		Repairable repairable = tool.get(DataComponents.REPAIRABLE);
		return repairable != null && !material.isEmpty() && repairable.isValidRepairItem(material);
	}

	/** A tool (or armor) the smith carries to repair: everything with durability. */
	private static boolean isCarriedTool(ItemStack stack) {
		return !stack.isEmpty() && stack.isDamageableItem();
	}

	/** How many steps repair the tool fully: materials, or bottles for an item without material. */
	private static int materialsFor(ItemStack tool) {
		int step = stepSize(tool);
		return (tool.getDamageValue() + step - 1) / step;
	}

	/** How many bottles the carried tools still need. */
	private static int bottlesNeeded(SimpleContainer inventory) {
		int needed = 0;
		for (ItemStack tool : stacks(inventory, RepairTools::isRepairable)) {
			if (needsBottle(tool)) {
				needed += materialsFor(tool);
			}
		}
		return needed - WorkerSupplies.count(inventory, RepairTools::isBottle);
	}

	private static int materialCarried(SimpleContainer inventory) {
		return WorkerSupplies.count(inventory, stack -> !stack.isDamageableItem() && !WorkerSupplies.isWage(stack));
	}

	private void plan(ServerLevel level, Villager villager, Workstations.Station station) {
		SimpleContainer inventory = villager.getInventory();
		List<ItemStack> carried = stacks(inventory, RepairTools::isCarriedTool);
		if (!carried.isEmpty()) {
			if (this.origin == null) {
				this.origin = this.closest(villager, station.validInputs(level).stream()).orElse(null);
			}
			List<ItemStack> damaged = carried.stream().filter(RepairTools::isRepairable).toList();
			if (damaged.stream().anyMatch(tool -> canRepairNow(inventory, tool))) {
				this.tableSpot = StandSpots.find(level, station.pos(), villager).orElse(null);
				this.setState(State.WALK_TO_TABLE);
				return;
			}
			boolean wantBottles = bottlesNeeded(inventory) > 0;
			Optional<BlockPos> materialChest = materialCarried(inventory) >= MAX_MATERIAL ? Optional.empty()
				: this.closest(villager, Stream.concat(station.validInputs(level).stream(), wantBottles ? this.bottleChests(level, station) : Stream.empty())
					.distinct()
					.filter(pos -> !this.unreachable.containsKey(pos))
					.filter(pos -> damaged.stream().anyMatch(tool -> !usesExperience(tool) && Chests.contains(level, pos, material -> repairs(tool, material)))
						|| wantBottles && Chests.contains(level, pos, RepairTools::isBottle)));
			if (materialChest.isPresent()) {
				this.startTrip(level, villager, materialChest.get(), false);
				return;
			}
			// Repaired, or no material left for the rest: back with them
			this.setState(State.RETURN);
			return;
		}
		ItemStack lacking = ItemStack.EMPTY;
		for (Source source : this.sources(level, station)) {
			Container container = Workstations.getContainer(level, source.pos());
			if (container == null) {
				continue;
			}
			for (ItemStack stack : container) {
				if (!source.wants(stack)) {
					continue;
				}
				if (hasMaterial(level, station, inventory, stack)) {
					this.origin = source.pos();
					this.startTrip(level, villager, source.pos(), true);
					return;
				}
				if (lacking.isEmpty()) {
					lacking = stack;
				}
			}
		}
		if (lacking.isEmpty()) {
			this.idle(villager, station, "smith.no_repairs");
		} else if (usesExperience(lacking) || needsBottle(lacking) && hasMaterialOnly(level, station, inventory, lacking)) {
			this.idle(villager, station, "smith.no_experience");
		} else {
			this.idle(villager, station, "smith.no_material", materialName(lacking));
		}
	}

	/** A chest the smith takes tools from, and which of them. */
	private record Source(BlockPos pos, boolean villageChest) {
		boolean wants(ItemStack stack) {
			return isRepairable(stack) && (!this.villageChest || WorkerSupplies.isWorn(stack));
		}
	}

	/** The village chests in reach of the smithing table first (the closest first), then the input chests. */
	private List<Source> sources(ServerLevel level, Workstations.Station station) {
		List<Source> sources = new ArrayList<>();
		for (BlockPos pos : VillageChests.inReach(level, station.pos())) {
			if (!this.unreachable.containsKey(pos)) {
				sources.add(new Source(pos, true));
			}
		}
		for (BlockPos pos : station.validInputs(level)) {
			if (!this.unreachable.containsKey(pos)) {
				sources.add(new Source(pos, false));
			}
		}
		return sources;
	}

	/** Material for the item, and a bottle if a step needs one, in the inventory or a chest the smith takes it from. */
	private boolean hasMaterial(ServerLevel level, Workstations.Station station, SimpleContainer inventory, ItemStack tool) {
		boolean bottle = !needsBottle(tool) || inventory.hasAnyMatching(RepairTools::isBottle)
			|| this.bottleChests(level, station).anyMatch(pos -> Chests.contains(level, pos, RepairTools::isBottle));
		return bottle && (usesExperience(tool) || hasMaterialOnly(level, station, inventory, tool));
	}

	private static boolean hasMaterialOnly(ServerLevel level, Workstations.Station station, SimpleContainer inventory, ItemStack tool) {
		Predicate<ItemStack> material = stack -> repairs(tool, stack);
		return inventory.hasAnyMatching(material) || station.validInputs(level).stream().anyMatch(pos -> Chests.contains(level, pos, material));
	}

	/** Where the smith takes bottles o' enchanting from: its input chests and the village chests in reach. */
	private Stream<BlockPos> bottleChests(ServerLevel level, Workstations.Station station) {
		return Stream.concat(station.validInputs(level).stream(), VillageChests.inReach(level, station.pos()).stream())
			.filter(pos -> !this.unreachable.containsKey(pos));
	}

	/** Whether the carried tool can get a repair step from what the smith carries. */
	private static boolean canRepairNow(SimpleContainer inventory, ItemStack tool) {
		boolean bottle = !needsBottle(tool) || inventory.hasAnyMatching(RepairTools::isBottle);
		return bottle && (usesExperience(tool) || inventory.hasAnyMatching(material -> repairs(tool, material)));
	}

	/** The name of the first material that repairs the item, e.g. "Iron Ingot". */
	private static Component materialName(ItemStack tool) {
		Repairable repairable = tool.get(DataComponents.REPAIRABLE);
		return repairable == null ? Component.empty() : repairable.items().stream()
			.findFirst()
			.map(item -> new ItemStack(item).getHoverName())
			.orElse(Component.empty());
	}

	private void startTrip(ServerLevel level, Villager villager, BlockPos chest, boolean tools) {
		this.trip = ChestTrip.to(level, villager, chest);
		this.fetchingTools = tools;
		this.setState(State.FETCH);
	}

	private void fetch(ServerLevel level, Villager villager, Workstations.Station station) {
		if (this.trip == null) {
			this.setState(State.PLAN);
			return;
		}
		SimpleContainer inventory = villager.getInventory();
		boolean villageChest = VillageChests.inReach(level, station.pos()).contains(this.trip.chest());
		Source source = new Source(this.trip.chest(), villageChest);
		ChestTrip.Result result = this.trip.tick(level, villager, container -> {
			if (this.fetchingTools) {
				this.takeTools(level, station, inventory, container, source);
			} else {
				this.takeMaterial(inventory, container);
			}
		});
		if (result != ChestTrip.Result.RUNNING) {
			if (result == ChestTrip.Result.FAILED) {
				this.unreachable.put(this.trip.chest(), level.getGameTime() + UNREACHABLE_TICKS);
				if (this.fetchingTools) {
					this.origin = null;
				}
			}
			this.trip = null;
			this.setState(State.PLAN);
		}
	}

	private void takeTools(ServerLevel level, Workstations.Station station, SimpleContainer inventory, Container container, Source source) {
		int taken = 0;
		for (int slot = 0; slot < container.getContainerSize() && taken < BATCH; slot++) {
			ItemStack stack = container.getItem(slot);
			if (!source.wants(stack) || !hasMaterial(level, station, inventory, stack) || !inventory.canAddItem(stack)) {
				continue;
			}
			inventory.addItem(container.removeItemNoUpdate(slot));
			taken++;
		}
		if (taken > 0) {
			container.setChanged();
		}
	}

	/** Material and bottles for the carried tools, as much as repairs them fully, at most {@link #MAX_MATERIAL}. */
	private void takeMaterial(SimpleContainer inventory, Container container) {
		for (ItemStack tool : stacks(inventory, RepairTools::isRepairable)) {
			if (usesExperience(tool)) {
				continue;
			}
			int room = MAX_MATERIAL - materialCarried(inventory);
			int missing = Math.min(room, materialsFor(tool) - WorkerSupplies.count(inventory, material -> repairs(tool, material)));
			if (missing > 0) {
				Chests.take(container, inventory, material -> repairs(tool, material), missing);
			}
		}
		int bottles = Math.min(MAX_MATERIAL - materialCarried(inventory), bottlesNeeded(inventory));
		if (bottles > 0) {
			Chests.take(container, inventory, RepairTools::isBottle, bottles);
		}
		container.setChanged();
	}

	private void walkToTable(Villager villager, Workstations.Station station) {
		boolean there = this.tableSpot != null ? StandSpots.isAt(villager, this.tableSpot) : station.pos().closerToCenterThan(villager.position(), TABLE_REACH);
		if (there) {
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			this.setState(State.REPAIR);
		} else if (this.stateTicks > WALK_TIMEOUT) {
			this.waitReason = "smith.table_unreachable";
			this.waitArgs = new Object[0];
			this.setState(State.WAIT);
		} else if (this.stateTicks % 20 == 1) {
			BlockPos target = this.tableSpot != null ? this.tableSpot : station.pos();
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, SPEED, this.tableSpot != null ? 0 : 1));
			villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(station.pos()));
		}
	}

	/** One material per {@link #REPAIR_TICKS}, a quarter of the durability each. */
	private void repair(ServerLevel level, Villager villager, Workstations.Station station) {
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(station.pos()));
		if (this.stateTicks % REPAIR_TICKS != 0) {
			return;
		}
		SimpleContainer inventory = villager.getInventory();
		for (ItemStack tool : stacks(inventory, RepairTools::isRepairable)) {
			if (!canRepairNow(inventory, tool)) {
				continue;
			}
			if (!usesExperience(tool)) {
				useUp(inventory, material -> repairs(tool, material));
			}
			if (needsBottle(tool)) {
				useUp(inventory, RepairTools::isBottle);
			}
			tool.setDamageValue(Math.max(0, tool.getDamageValue() - stepSize(tool)));
			inventory.setChanged();
			villager.swingForAttack(InteractionHand.MAIN_HAND);
			level.playSound(null, station.pos(), needsBottle(tool) ? SoundEvents.ENCHANTMENT_TABLE_USE : SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
			return;
		}
		this.setState(State.PLAN);
	}

	private static void useUp(SimpleContainer inventory, Predicate<ItemStack> filter) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty() && filter.test(stack)) {
				stack.shrink(1);
				inventory.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
				return;
			}
		}
	}

	/** Puts the tools back into the chest they came from, or else into another one with room. */
	private void giveBack(ServerLevel level, Villager villager, Workstations.Station station) {
		SimpleContainer inventory = villager.getInventory();
		if (this.trip == null) {
			if (!inventory.hasAnyMatching(RepairTools::isCarriedTool)) {
				this.origin = null;
				this.setState(State.PLAN);
				return;
			}
			Optional<BlockPos> chest = this.origin != null && Chests.hasRoom(level, this.origin) ? Optional.of(this.origin)
				: this.closest(villager, Stream.of(VillageChests.inReach(level, station.pos()), station.validOutputs(level), station.validInputs(level))
					.flatMap(List::stream)
					.filter(pos -> !this.unreachable.containsKey(pos) && Chests.hasRoom(level, pos)));
			if (chest.isEmpty()) {
				this.idle(villager, station, "chests_full");
				return;
			}
			this.trip = ChestTrip.to(level, villager, chest.get());
		}
		ChestTrip.Result result = this.trip.tick(level, villager, container -> Chests.deposit(inventory, container, RepairTools::isCarriedTool));
		if (result != ChestTrip.Result.RUNNING) {
			if (result == ChestTrip.Result.FAILED) {
				this.unreachable.put(this.trip.chest(), level.getGameTime() + UNREACHABLE_TICKS);
			}
			this.trip = null;
			// Whatever is left goes to another chest
			this.origin = null;
			this.setState(inventory.hasAnyMatching(RepairTools::isCarriedTool) ? State.RETURN : State.PLAN);
		}
	}

	private Optional<BlockPos> closest(Villager villager, Stream<BlockPos> chests) {
		return chests.min(Comparator.comparingDouble(pos -> pos.distToCenterSqr(villager.position())));
	}

	private static List<ItemStack> stacks(SimpleContainer inventory, Predicate<ItemStack> filter) {
		List<ItemStack> stacks = new ArrayList<>();
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && filter.test(stack)) {
				stacks.add(stack);
			}
		}
		return stacks;
	}

	/** Nothing to do right now: wait at the smithing table. */
	private void idle(Villager villager, Workstations.Station station, String reason, Object... args) {
		this.waitReason = reason;
		this.waitArgs = args;
		villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(station.pos(), SPEED, 2));
		this.setState(State.WAIT);
	}

	private void setState(State state) {
		this.state = state;
		this.stateTicks = 0;
	}
}
