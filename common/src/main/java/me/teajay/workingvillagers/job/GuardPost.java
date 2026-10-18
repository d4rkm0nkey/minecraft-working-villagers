package me.teajay.workingvillagers.job;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The work of the guard: at night it stands at its target and shoots the monsters around it
 * with bow and arrows, like a skeleton. Every shot costs an arrow; its arrows hit nothing but
 * monsters. In the morning it picks up the arrows that missed and what the monsters dropped,
 * and brings the drops to its output chests. Arrows and bows it takes from its input chests,
 * a village chest, or the player gives them.
 *
 * <p>Guards keep their own schedule (see {@link #activity}): work from dusk to the morning,
 * sleep in the morning, free in the afternoon. They do not panic.
 */
public class GuardPost extends Behavior<Villager> {
	private static final float SPEED = 0.6F;
	private static final int WALK_TIMEOUT = 400;
	private static final int WAIT_TICKS = 60;
	/** When the morning round ends (clock ticks of the day); the work ends at 1000. */
	private static final int COLLECT_UNTIL = 1000;
	private static final int DUSK = 12000;
	private static final int SLEEP_UNTIL = 9000;
	/** Below this many arrows the guard fetches more from its input chests. */
	private static final int LOW_ARROWS = 16;
	private static final double PICKUP_REACH = 1.8;

	private enum State { POST, WATCH, COLLECT, FETCH, DELIVER, WAIT }

	private State state = State.POST;
	private int stateTicks;
	private int cooldown;
	private String waitReason = "guard.no_arrows";
	private @Nullable LivingEntity target;
	private @Nullable ChestTrip trip;
	private final List<Vec3> kills = new ArrayList<>();
	private final WorkerSupplies supplies = new WorkerSupplies();

	public GuardPost() {
		super(ImmutableMap.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT), 24000);
	}

	public static boolean isGuard(Villager villager) {
		return villager.getVillagerData().profession().is(WorkingVillagers.GUARD_PROFESSION);
	}

	/** The guard's schedule: work from dusk to the morning round, sleep, then free time. */
	public static Activity activity(ServerLevel level) {
		long time = Math.floorMod(level.getOverworldClockTime(), 24000L);
		if (time >= DUSK || time < COLLECT_UNTIL) {
			return Activity.WORK;
		}
		return time < SLEEP_UNTIL ? Activity.REST : Activity.IDLE;
	}

	/** The number of the night (from dusk to dusk), for the monsters of the last night. */
	public static long night(ServerLevel level) {
		return Math.floorDiv(level.getOverworldClockTime() + 24000L - DUSK, 24000L);
	}

	private static boolean isMorning(ServerLevel level) {
		return Math.floorMod(level.getOverworldClockTime(), 24000L) < COLLECT_UNTIL;
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
		return isGuard(villager) && getWorkplace(level, villager) != null;
	}

	@Override
	protected boolean canStillUse(ServerLevel level, Villager villager, long timestamp) {
		return this.checkExtraStartConditions(level, villager) && villager.getBrain().isActive(Activity.WORK);
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
		this.setState(State.POST);
	}

	@Override
	protected void stop(ServerLevel level, Villager villager, long timestamp) {
		WorkerStatus.clear(villager);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
		villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		this.target = null;
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
		if (this.cooldown > 0) {
			this.cooldown--;
		}
		if (this.state == State.POST && this.supplies.busy(level, villager, WorkerSupplies.BOW)) {
			return; // the chest sets the status
		}
		switch (this.state) {
			case POST -> this.goToPost(level, villager, workplace);
			case WATCH -> this.watch(level, villager, workplace);
			case COLLECT -> this.collect(level, villager, workplace);
			case FETCH -> this.fetch(level, villager, workplace);
			case DELIVER -> this.deliver(level, villager, workplace);
			case WAIT -> {
				if (this.stateTicks >= WAIT_TICKS) {
					this.setState(State.POST);
				}
			}
		}
		this.report(villager);
	}

	// --- At the post ---

	/** On top of the target if there is room (a tower), otherwise next to it. */
	private static Optional<BlockPos> postSpot(ServerLevel level, BlockPos target, Villager villager) {
		if (StandSpots.isStandable(level, target.above())) {
			return Optional.of(target.above());
		}
		return StandSpots.find(level, target, villager);
	}

	private void goToPost(ServerLevel level, Villager villager, Workstations.Station workplace) {
		SimpleContainer inventory = villager.getInventory();
		if (isMorning(level)) {
			this.setState(State.COLLECT);
			return;
		}
		int arrows = inventory.countItem(Items.ARROW);
		if (arrows < LOW_ARROWS && this.inputWithArrows(level, villager, workplace).isPresent()) {
			this.setState(State.FETCH);
			return;
		}
		if (arrows == 0) {
			this.waitReason = "guard.no_arrows";
			this.setState(State.WAIT);
			return;
		}
		Optional<BlockPos> spot = postSpot(level, workplace.pos(), villager);
		boolean there = spot.map(pos -> StandSpots.isAt(villager, pos)).orElse(workplace.pos().closerToCenterThan(villager.position(), 2.5));
		if (there) {
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			this.setState(State.WATCH);
		} else if (this.stateTicks > WALK_TIMEOUT) {
			this.waitReason = "guard.post_unreachable";
			this.setState(State.WAIT);
		} else if (this.stateTicks % 20 == 1) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(spot.orElse(workplace.pos()), SPEED, spot.isPresent() ? 0 : 1));
		}
	}

	/** A monster in the guard's radius that it can see and reach with an arrow. */
	private static boolean isTarget(Villager villager, BlockPos post, int radius, LivingEntity entity) {
		return entity instanceof Enemy && !(entity instanceof NeutralMob) && entity.isAlive() && !entity.isInvisible()
			&& entity.position().closerThan(Vec3.atCenterOf(post), radius)
			&& entity.distanceTo(villager) <= Config.GUARD_RANGE.get()
			&& villager.hasLineOfSight(entity);
	}

	private void watch(ServerLevel level, Villager villager, Workstations.Station workplace) {
		if (isMorning(level)) {
			villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			this.setState(State.COLLECT);
			return;
		}
		SimpleContainer inventory = villager.getInventory();
		ItemStack bow = findBow(inventory);
		if (bow.isEmpty() || inventory.countItem(Items.ARROW) == 0) {
			villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			this.setState(State.POST);
			return;
		}
		int radius = workplace.guardRadius();
		LivingEntity current = this.target;
		if (current != null && !current.isAlive()) {
			if (current.getLastHurtByMob() == villager) {
				this.kills.add(current.position());
				((HiredVillager) villager).workingvillagers$addWorkDone(night(level), 1);
			}
			current = null;
		}
		if (current == null || !isTarget(villager, workplace.pos(), radius, current)) {
			current = null;
			if (this.stateTicks % 10 == 0) {
				AABB area = new AABB(workplace.pos()).inflate(radius);
				current = level.getEntitiesOfClass(LivingEntity.class, area, entity -> isTarget(villager, workplace.pos(), radius, entity)).stream()
					.min(Comparator.comparingDouble(entity -> entity.distanceToSqr(villager)))
					.orElse(null);
			}
		}
		this.target = current;
		if (current == null) {
			villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(workplace.pos().above(2)));
			// Back to the spot if it was pushed away
			if (this.stateTicks % 100 == 0) {
				this.setState(State.POST);
			}
			return;
		}
		villager.setItemSlot(EquipmentSlot.MAINHAND, bow.copy());
		villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(current, true));
		villager.getLookControl().setLookAt(current, 30.0F, 30.0F);
		if (this.cooldown == 0) {
			this.shoot(level, villager, current, bow);
			this.cooldown = Math.max(5, (int) Math.round(Config.SECONDS_PER_SHOT.get() * 20));
		}
	}

	/** Shoots like a skeleton: an arrow from the inventory, one use of the bow. */
	private void shoot(ServerLevel level, Villager villager, LivingEntity target, ItemStack bow) {
		SimpleContainer inventory = villager.getInventory();
		ItemStack arrowItem = ItemStack.EMPTY;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.is(Items.ARROW)) {
				arrowItem = stack.split(1);
				inventory.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
				break;
			}
		}
		if (arrowItem.isEmpty()) {
			return;
		}
		AbstractArrow arrow = ProjectileUtil.getMobArrow(villager, arrowItem, 1.0F, bow);
		double dx = target.getX() - villager.getX();
		double dy = target.getY(0.3333333333333333) - arrow.getY();
		double dz = target.getZ() - villager.getZ();
		double distance = Math.sqrt(dx * dx + dz * dz);
		arrow.shoot(dx, dy + distance * 0.2, dz, 1.6F, 4.0F);
		arrow.pickup = AbstractArrow.Pickup.ALLOWED;
		level.addFreshEntity(arrow);
		villager.swingForAttack(InteractionHand.MAIN_HAND);
		level.playSound(null, villager, SoundEvents.SKELETON_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.0F / (villager.getRandom().nextFloat() * 0.4F + 0.8F));
		ItemStack own = findBow(inventory);
		if (!own.isEmpty()) {
			own.hurtAndBreak(1, level, null, broken -> villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY));
		}
		inventory.setChanged();
	}

	private static ItemStack findBow(SimpleContainer inventory) {
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && stack.is(Items.BOW) && !WorkerSupplies.isWorn(stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	// --- The morning round ---

	/** Its own arrows lying around, and what the monsters it killed dropped. */
	private @Nullable Vec3 nextPickup(ServerLevel level, Villager villager, Workstations.Station workplace) {
		AABB area = new AABB(workplace.pos()).inflate(workplace.guardRadius() + 8);
		Optional<Vec3> arrow = level.getEntitiesOfClass(AbstractArrow.class, area,
				found -> found.getOwner() == villager && found.pickup == AbstractArrow.Pickup.ALLOWED && found.getDeltaMovement().lengthSqr() < 1.0E-4)
			.stream().map(found -> found.position()).min(Comparator.comparingDouble(pos -> pos.distanceToSqr(villager.position())));
		if (arrow.isPresent()) {
			return arrow.get();
		}
		while (!this.kills.isEmpty()) {
			Vec3 kill = this.kills.getFirst();
			if (!level.getEntitiesOfClass(ItemEntity.class, new AABB(kill, kill).inflate(3.0), ItemEntity::isAlive).isEmpty()) {
				return kill;
			}
			this.kills.removeFirst();
		}
		return null;
	}

	private void collect(ServerLevel level, Villager villager, Workstations.Station workplace) {
		Vec3 next = this.nextPickup(level, villager, workplace);
		if (next == null || !isMorning(level) && this.stateTicks > WALK_TIMEOUT) {
			this.kills.clear();
			this.setState(this.hasLoot(villager.getInventory()) && !workplace.validOutputs(level).isEmpty() ? State.DELIVER : State.POST);
			if (next == null && this.state == State.POST && isMorning(level)) {
				this.waitReason = "guard.morning";
				this.setState(State.WAIT);
			}
			return;
		}
		if (next.closerThan(villager.position(), PICKUP_REACH)) {
			this.pickUp(level, villager, next);
		} else if (this.stateTicks % 20 == 1) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(BlockPos.containing(next), SPEED, 1));
		}
		if (this.stateTicks > WALK_TIMEOUT) {
			// Cannot get there: leave it
			this.kills.removeIf(kill -> kill.closerThan(next, 1.0));
			this.setState(State.COLLECT);
		}
	}

	private void pickUp(ServerLevel level, Villager villager, Vec3 at) {
		AABB around = new AABB(at, at).inflate(PICKUP_REACH + 1.0);
		SimpleContainer inventory = villager.getInventory();
		for (AbstractArrow arrow : level.getEntitiesOfClass(AbstractArrow.class, around, found -> found.getOwner() == villager)) {
			ItemStack rest = inventory.addItem(arrow.getPickupItemStackOrigin().copy());
			if (rest.isEmpty()) {
				arrow.discard();
			}
		}
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, around, ItemEntity::isAlive)) {
			ItemStack rest = inventory.addItem(item.getItem().copy());
			if (rest.isEmpty()) {
				item.discard();
			} else {
				item.setItem(rest);
			}
		}
		villager.swingForAttack(InteractionHand.MAIN_HAND);
		this.setState(State.COLLECT);
	}

	// --- Chests ---

	private Optional<BlockPos> inputWithArrows(ServerLevel level, Villager villager, Workstations.Station workplace) {
		return Chests.closest(workplace.validInputs(level).stream()
			.filter(pos -> Chests.contains(level, pos, stack -> stack.is(Items.ARROW))
				|| findBow(villager.getInventory()).isEmpty() && Chests.contains(level, pos, stack -> stack.is(Items.BOW) && !WorkerSupplies.isWorn(stack)))
			.toList(), villager);
	}

	private void fetch(ServerLevel level, Villager villager, Workstations.Station workplace) {
		if (this.trip == null) {
			Optional<BlockPos> chest = this.inputWithArrows(level, villager, workplace);
			if (chest.isEmpty()) {
				this.setState(State.POST);
				return;
			}
			this.trip = ChestTrip.to(level, villager, chest.get());
		}
		SimpleContainer inventory = villager.getInventory();
		ChestTrip.Result result = this.trip.tick(level, villager, container -> {
			Chests.take(container, inventory, stack -> stack.is(Items.ARROW), Config.KEEP_ARROWS.get() - inventory.countItem(Items.ARROW));
			if (findBow(inventory).isEmpty()) {
				Chests.take(container, inventory, stack -> stack.is(Items.BOW) && !WorkerSupplies.isWorn(stack), 1);
			}
		});
		if (result == ChestTrip.Result.RUNNING) {
			return;
		}
		this.trip = null;
		this.setState(result == ChestTrip.Result.FAILED ? State.WAIT : State.POST);
		if (result == ChestTrip.Result.FAILED) {
			this.waitReason = "chest_unreachable";
		}
	}

	/** What it brings to the output chests: the drops, and more arrows than it carries. */
	private boolean isLoot(ItemStack stack) {
		return !stack.isEmpty() && !stack.is(Items.BOW) && !stack.is(Items.ARROW) && !WorkerSupplies.isWage(stack);
	}

	private boolean hasLoot(SimpleContainer inventory) {
		for (ItemStack stack : inventory) {
			if (this.isLoot(stack)) {
				return true;
			}
		}
		return inventory.countItem(Items.ARROW) > Config.KEEP_ARROWS.get();
	}

	private void deliver(ServerLevel level, Villager villager, Workstations.Station workplace) {
		if (this.trip == null) {
			Optional<BlockPos> chest = Chests.closest(workplace.validOutputs(level).stream().filter(pos -> Chests.hasRoom(level, pos)).toList(), villager);
			if (chest.isEmpty()) {
				this.setState(State.POST);
				return;
			}
			this.trip = ChestTrip.to(level, villager, chest.get());
		}
		ChestTrip.Result result = this.trip.tick(level, villager, container -> {
			SimpleContainer inventory = villager.getInventory();
			int extra = inventory.countItem(Items.ARROW) - Config.KEEP_ARROWS.get();
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				ItemStack stack = inventory.getItem(slot);
				if (this.isLoot(stack)) {
					inventory.setItem(slot, HopperBlockEntity.addItem(null, container, stack, null));
				} else if (stack.is(Items.ARROW) && extra > 0) {
					int moved = Math.min(extra, stack.getCount());
					ItemStack rest = HopperBlockEntity.addItem(null, container, stack.split(moved), null);
					stack.grow(rest.getCount());
					extra -= moved - rest.getCount();
					inventory.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
				}
			}
		});
		if (result == ChestTrip.Result.RUNNING) {
			return;
		}
		this.trip = null;
		this.setState(State.POST);
	}

	private void report(Villager villager) {
		switch (this.state) {
			case POST -> WorkerStatus.task(villager, "guard.post");
			case WATCH -> WorkerStatus.task(villager, this.target != null ? "guard.shoot" : "guard.watch");
			case COLLECT -> WorkerStatus.task(villager, "guard.collect");
			case FETCH -> WorkerStatus.task(villager, "guard.fetch");
			case DELIVER -> WorkerStatus.task(villager, "deliver");
			case WAIT -> WorkerStatus.task(villager, this.waitReason);
		}
	}

	private void setState(State state) {
		this.state = state;
		this.stateTicks = 0;
	}
}
