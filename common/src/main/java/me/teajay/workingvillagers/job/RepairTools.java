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

	/** Whether the item can be repaired at all: damaged and with a material for it. */
	static boolean isRepairable(ItemStack stack) {
		return !stack.isEmpty() && stack.isDamageableItem() && stack.isDamaged() && stack.has(DataComponents.REPAIRABLE);
	}

	/** Whether the material repairs the tool. */
	static boolean repairs(ItemStack tool, ItemStack material) {
		Repairable repairable = tool.get(DataComponents.REPAIRABLE);
		return repairable != null && !material.isEmpty() && repairable.isValidRepairItem(material);
	}

	/** A tool (or armor) the smith carries to repair: everything with durability and a repair material. */
	private static boolean isCarriedTool(ItemStack stack) {
		return !stack.isEmpty() && stack.isDamageableItem() && stack.has(DataComponents.REPAIRABLE);
	}

	/** How many materials repair the tool fully, a quarter of its durability each. */
	private static int materialsFor(ItemStack tool) {
		int quarter = Math.max(1, tool.getMaxDamage() / 4);
		return (tool.getDamageValue() + quarter - 1) / quarter;
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
			if (damaged.stream().anyMatch(tool -> inventory.hasAnyMatching(material -> repairs(tool, material)))) {
				this.tableSpot = StandSpots.find(level, station.pos(), villager).orElse(null);
				this.setState(State.WALK_TO_TABLE);
				return;
			}
			Optional<BlockPos> materialChest = materialCarried(inventory) >= MAX_MATERIAL ? Optional.empty()
				: this.closest(villager, station.validInputs(level).stream()
					.filter(pos -> damaged.stream().anyMatch(tool -> Chests.contains(level, pos, material -> repairs(tool, material)))));
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

	private static boolean hasMaterial(ServerLevel level, Workstations.Station station, SimpleContainer inventory, ItemStack tool) {
		Predicate<ItemStack> material = stack -> repairs(tool, stack);
		return inventory.hasAnyMatching(material) || station.validInputs(level).stream().anyMatch(pos -> Chests.contains(level, pos, material));
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

	/** Material for the carried tools, as much as repairs them fully, at most {@link #MAX_MATERIAL}. */
	private void takeMaterial(SimpleContainer inventory, Container container) {
		for (ItemStack tool : stacks(inventory, RepairTools::isRepairable)) {
			int room = MAX_MATERIAL - materialCarried(inventory);
			int missing = Math.min(room, materialsFor(tool) - WorkerSupplies.count(inventory, material -> repairs(tool, material)));
			if (missing > 0) {
				Chests.take(container, inventory, material -> repairs(tool, material), missing);
			}
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
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				ItemStack material = inventory.getItem(slot);
				if (repairs(tool, material)) {
					material.shrink(1);
					inventory.setItem(slot, material.isEmpty() ? ItemStack.EMPTY : material);
					tool.setDamageValue(Math.max(0, tool.getDamageValue() - Math.max(1, tool.getMaxDamage() / 4)));
					inventory.setChanged();
					villager.swingForAttack(InteractionHand.MAIN_HAND);
					level.playSound(null, station.pos(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
					return;
				}
			}
		}
		this.setState(State.PLAN);
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
