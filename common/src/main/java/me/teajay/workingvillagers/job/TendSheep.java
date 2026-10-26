package me.teajay.workingvillagers.job;

import com.google.common.collect.ImmutableMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The work of the shepherd: shear the sheep around its loom, breed them with wheat while
 * there are fewer than the herd size set at the loom, and bring the wool to one of the chests
 * linked to the loom. Without a linked chest the shepherd does not work.
 *
 * <p>It shears and feeds from up to {@link #REACH} blocks away, also over a fence, so a pen
 * needs no gate (villagers do not open fence gates). Sheep it cannot get at are left alone
 * for a while. Shearing wears the shears like a player's; breeding takes one wheat per sheep,
 * and two sheep in love find each other like after a player fed them. It needs its wage and
 * shears; the wheat only for breeding.
 */
public class TendSheep extends Behavior<Villager> {
	private static final float SPEED = 0.5F;
	/** How far below and above the loom the sheep may be. */
	private static final int PASTURE_DOWN = 4;
	private static final int PASTURE_UP = 4;
	private static final int WALK_TIMEOUT = 300;
	private static final int SEARCH_COOLDOWN = 100;
	/** How long a sheep that could not be reached is left alone. */
	private static final long UNREACHABLE_COOLDOWN = 600L;
	/** How close (sideways) the shepherd has to be to a sheep, enough to reach over a fence. */
	private static final double REACH = 3.5;
	/** How long two sheep are given to find each other after they were fed. */
	private static final int LOVE_TICKS = 600;

	private enum State { SEARCH, WALK, WORK, DELIVER, WAIT }

	private enum Task { SHEAR, FEED }

	private State state = State.SEARCH;
	private int stateTicks;
	private int waitTicks = SEARCH_COOLDOWN;
	private @Nullable UUID target;
	private Task task = Task.SHEAR;
	private String waitReason = "shepherd.growing";
	private @Nullable ChestTrip trip;
	private final Map<UUID, Long> unreachable = new HashMap<>();
	private final WorkerSupplies supplies = new WorkerSupplies();

	public TendSheep() {
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
		if (!villager.getVillagerData().profession().is(WorkingVillagers.SHEPHERD_PROFESSION)) {
			return false;
		}
		Workstations.Station workplace = getWorkplace(level, villager);
		return workplace != null && !workplace.validOutputs(level).isEmpty();
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
		this.setState(State.SEARCH);
	}

	@Override
	protected void stop(ServerLevel level, Villager villager, long timestamp) {
		WorkerStatus.clear(villager);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
		villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		this.supplies.stop(villager);
		if (this.trip != null) {
			this.trip.close(villager);
			this.trip = null;
		}
	}

	@Override
	protected void tick(ServerLevel level, Villager villager, long timestamp) {
		Workstations.Station workplace = getWorkplace(level, villager);
		if (workplace == null) {
			return;
		}
		this.stateTicks++;
		this.unreachable.values().removeIf(until -> until < timestamp);
		if (this.state == State.SEARCH && !hasWool(villager.getInventory())
			&& this.supplies.busy(level, villager, WorkerSupplies.SHEARS)) {
			return; // the chest sets the status
		}

		switch (this.state) {
			case SEARCH -> this.search(level, villager, workplace);
			case WALK -> this.walk(level, villager, timestamp);
			case WORK -> this.work(level, villager);
			case DELIVER -> this.deliver(level, villager, workplace);
			case WAIT -> {
				if (this.stateTicks >= this.waitTicks) {
					this.setState(State.SEARCH);
				}
			}
		}
		this.report(villager);
	}

	/** The sheep (lambs included) within reach of the loom. */
	public static List<Sheep> herd(ServerLevel level, BlockPos loom) {
		int radius = WorkerLevels.area(WorkerLevels.levelAt(level, loom), Config.PASTURE_RADIUS.get());
		AABB area = new AABB(loom).inflate(radius, 0, radius).expandTowards(0, PASTURE_UP, 0).expandTowards(0, -PASTURE_DOWN, 0);
		return level.getEntities(EntityTypes.SHEEP, area, Sheep::isAlive);
	}

	private void search(ServerLevel level, Villager villager, Workstations.Station workplace) {
		SimpleContainer inventory = villager.getInventory();
		if (hasWool(inventory) && !inventory.canAddItem(new ItemStack(Items.WOOL.white(), 64))) {
			this.setState(State.DELIVER);
			return;
		}
		List<Sheep> herd = herd(level, workplace.pos());
		Comparator<Sheep> closest = Comparator.comparingDouble(sheep -> sheep.distanceToSqr(villager));
		Optional<Sheep> shear = herd.stream()
			.filter(sheep -> sheep.readyForShearing() && !this.unreachable.containsKey(sheep.getUUID()))
			.min(closest);
		if (shear.isPresent()) {
			this.goTo(shear.get(), Task.SHEAR);
			return;
		}
		// Two sheep in love make a lamb: a sheep in love alone gets a partner, a new pair only
		// while the lambs to come still fit into the herd
		long inLove = herd.stream().filter(Sheep::isInLove).count();
		long ready = herd.stream().filter(TendSheep::canBreed).count();
		boolean breed = Unlocks.can(villager, Unlocks.Ability.BREEDING)
			&& (inLove % 2 == 1 || herd.size() + (inLove + 1) / 2 < workplace.herd() && ready >= 2);
		boolean noWheat = breed && wheat(inventory).isEmpty();
		if (breed && !noWheat) {
			Optional<Sheep> feed = herd.stream()
				.filter(sheep -> canBreed(sheep) && !this.unreachable.containsKey(sheep.getUUID()))
				.min(closest);
			if (feed.isPresent()) {
				this.goTo(feed.get(), Task.FEED);
				return;
			}
		}
		if (hasWool(inventory)) {
			this.setState(State.DELIVER);
		} else {
			this.rest(level, villager, workplace, SEARCH_COOLDOWN,
				herd.isEmpty() ? "shepherd.no_sheep" : noWheat ? "shepherd.no_wheat" : "shepherd.growing");
		}
	}

	private void goTo(Sheep sheep, Task task) {
		this.target = sheep.getUUID();
		this.task = task;
		this.setState(State.WALK);
	}

	private @Nullable Sheep target(ServerLevel level) {
		return this.target != null && level.getEntity(this.target) instanceof Sheep sheep && sheep.isAlive() ? sheep : null;
	}

	private void walk(ServerLevel level, Villager villager, long timestamp) {
		Sheep sheep = this.target(level);
		if (sheep == null || !this.stillToDo(sheep)) {
			this.target = null;
			this.setState(State.SEARCH);
			return;
		}
		if (isWithinReach(villager, sheep)) {
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			ItemStack tool = this.task == Task.SHEAR ? findShears(villager.getInventory()) : wheat(villager.getInventory());
			if (tool.isEmpty()) {
				this.target = null;
				this.setState(State.SEARCH);
				return;
			}
			villager.setItemSlot(EquipmentSlot.MAINHAND, tool.copyWithCount(1));
			villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
			this.setState(State.WORK);
		} else if (this.stateTicks > WALK_TIMEOUT) {
			this.unreachable.put(sheep.getUUID(), timestamp + UNREACHABLE_COOLDOWN);
			this.target = null;
			this.setState(State.SEARCH);
		} else if (this.stateTicks % 20 == 1) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(sheep, false), SPEED, 1));
			villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(sheep, true));
		}
	}

	/** A grown sheep that is not in love and has not just had a lamb. */
	private static boolean canBreed(Sheep sheep) {
		return sheep.canFallInLove() && !sheep.isBaby();
	}

	private boolean stillToDo(Sheep sheep) {
		return this.task == Task.SHEAR ? sheep.readyForShearing() : canBreed(sheep);
	}

	/** Close enough sideways and about level with it; over a fence, too. */
	private static boolean isWithinReach(Villager villager, Sheep sheep) {
		double dx = villager.getX() - sheep.getX();
		double dz = villager.getZ() - sheep.getZ();
		double dy = villager.getY() - sheep.getY();
		return dx * dx + dz * dz < REACH * REACH && dy > -1.5 && dy < 1.5;
	}

	/** Shears the sheep, or feeds it a wheat. */
	private void work(ServerLevel level, Villager villager) {
		Sheep sheep = this.target(level);
		if (sheep == null || !this.stillToDo(sheep) || !isWithinReach(villager, sheep)) {
			this.target = null;
			villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			this.setState(State.SEARCH);
			return;
		}
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(sheep, true));
		if (this.stateTicks % 10 == 0) {
			villager.swingForAttack(InteractionHand.MAIN_HAND);
		}
		if (this.stateTicks < WorkerLevels.ticks(villager, (int) Math.round(Config.SECONDS_PER_SHEEP.get() * 20))) {
			return;
		}
		this.target = null;
		villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		this.setState(State.SEARCH);
		if (this.task == Task.SHEAR) {
			shear(level, villager, sheep);
		} else {
			feed(level, villager, sheep);
		}
	}

	/** Shears like a player and puts the wool into the inventory; one use of the shears. */
	private static void shear(ServerLevel level, Villager villager, Sheep sheep) {
		ItemStack shears = findShears(villager.getInventory());
		if (shears.isEmpty()) {
			return;
		}
		AABB around = sheep.getBoundingBox().inflate(2.0);
		Set<ItemEntity> before = new HashSet<>(level.getEntitiesOfClass(ItemEntity.class, around));
		sheep.shear(level, SoundSource.NEUTRAL, shears);
		int wool = 0;
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, around)) {
			if (before.contains(item) || !item.isAlive()) {
				continue;
			}
			ItemStack stack = item.getItem().copy();
			wool += stack.getCount();
			ItemStack rest = villager.getInventory().addItem(stack);
			if (rest.isEmpty()) {
				item.discard();
			} else {
				item.setItem(rest);
			}
		}
		((HiredVillager) villager).workingvillagers$addWorkDone(ChopTrees.today(level), wool);
		WorkerLevels.wear(level, villager, shears, 1, broken -> villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY));
		villager.getInventory().setChanged();
	}

	/** Feeds the sheep a wheat, so it falls in love, like after a player fed it. */
	private static void feed(ServerLevel level, Villager villager, Sheep sheep) {
		ItemStack wheat = wheat(villager.getInventory());
		if (wheat.isEmpty()) {
			return;
		}
		wheat.shrink(1);
		villager.getInventory().setChanged();
		sheep.setInLove(null);
		sheep.setInLoveTime(LOVE_TICKS);
		level.playSound(null, sheep, SoundEvents.SHEEP_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.0F);
	}

	/** The shears the shepherd works with; worn ones it gives back instead (see {@link WorkerSupplies#isWorn}). */
	private static ItemStack findShears(SimpleContainer inventory) {
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && stack.is(Items.SHEARS) && !WorkerSupplies.isWorn(stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static ItemStack wheat(SimpleContainer inventory) {
		for (ItemStack stack : inventory) {
			if (stack.is(Items.WHEAT)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private void deliver(ServerLevel level, Villager villager, Workstations.Station workplace) {
		if (this.trip == null) {
			villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			if (!hasWool(villager.getInventory())) {
				this.setState(State.SEARCH);
				return;
			}
			Optional<BlockPos> chest = Chests.closest(workplace.validOutputs(level).stream().filter(pos -> Chests.hasRoom(level, pos)).toList(), villager);
			if (chest.isEmpty()) {
				this.rest(level, villager, workplace, SEARCH_COOLDOWN, "chests_full");
				return;
			}
			this.trip = ChestTrip.to(level, villager, chest.get());
		}
		ChestTrip.Result result = this.trip.tick(level, villager, container -> handIn(villager.getInventory(), container));
		if (result == ChestTrip.Result.RUNNING) {
			return;
		}
		this.trip = null;
		if (result == ChestTrip.Result.FAILED) {
			this.rest(level, villager, workplace, SEARCH_COOLDOWN, "chest_unreachable");
		} else {
			// Did not all fit: the next round picks another chest with room
			this.setState(hasWool(villager.getInventory()) ? State.DELIVER : State.SEARCH);
		}
	}

	/** Goes back to the loom and waits next to it. */
	private void rest(ServerLevel level, Villager villager, Workstations.Station workplace, int ticks, String reason) {
		villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		Optional<BlockPos> spot = StandSpots.find(level, workplace.pos(), villager);
		if (spot.isPresent()) {
			if (!StandSpots.isAt(villager, spot.get())) {
				villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(spot.get(), SPEED, 0));
			}
		} else {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(workplace.pos(), SPEED, 2));
		}
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(workplace.pos()));
		this.waitReason = reason;
		this.waitTicks = ticks;
		this.setState(State.WAIT);
	}

	private void report(Villager villager) {
		switch (this.state) {
			case SEARCH -> WorkerStatus.task(villager, "shepherd.search");
			case WALK, WORK -> WorkerStatus.task(villager, this.task == Task.SHEAR ? "shepherd.shear" : "shepherd.feed");
			case DELIVER -> WorkerStatus.task(villager, "deliver");
			case WAIT -> WorkerStatus.task(villager, this.waitReason);
		}
	}

	/** What the shepherd hands in: everything but its shears, its wheat and its wage. */
	private static boolean isKept(ItemStack stack) {
		return stack.is(Items.SHEARS) || stack.is(Items.WHEAT) || WorkerSupplies.isWage(stack);
	}

	private static boolean hasWool(SimpleContainer inventory) {
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && !isKept(stack)) {
				return true;
			}
		}
		return false;
	}

	private static void handIn(SimpleContainer inventory, Container container) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty() && !isKept(stack)) {
				inventory.setItem(slot, HopperBlockEntity.addItem(null, container, stack, null));
			}
		}
	}

	private void setState(State state) {
		this.state = state;
		this.stateTicks = 0;
	}
}
