package me.teajay.workingvillagers.job;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The work of the guard: at night it stands at its target and shoots the monsters around it
 * with bow and arrows, like a skeleton. Every shot costs an arrow; its arrows hit nothing but
 * monsters. In the morning it picks up the arrows that missed and what the monsters dropped,
 * fills the experience of its kills into bottles o' enchanting and brings the drops and the
 * bottles to its output chests. Arrows, bows and glass bottles it takes from its input
 * chests, a village chest, or the player gives them.
 *
 * <p>Set to <b>hunt</b> at the target, it roams around within {@link Config#HUNT_RADIUS} at
 * night instead, looking for monsters; with few arrows or wounded it goes back to its post.
 *
 * <p>Guards keep their own schedule (see {@link #activity}): work from dusk to the morning,
 * sleep in the morning, free in the afternoon, and work whenever there is a raid. They do not
 * panic, do not hide from raids, shoot raiders first and ring the closest bell when they see
 * one, so the raiders glow.
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

	/** A hunting guard turns back with this many arrows or less. */
	private static final int HUNT_MIN_ARROWS = 8;
	/** How often a guard rings the bell while raiders are around. */
	private static final long ALARM_TICKS = 600L;
	/** How far from its post a guard looks for a bell to ring. */
	private static final int BELL_RANGE = 48;
	/** Game tests: a time of day per guard instead of the clock (tests run side by side at different times). */
	private static final Map<UUID, Long> TEST_TIME = new HashMap<>();
	/** Where the monsters a guard killed fell, to pick up the loot in the morning. Not saved. */
	private static final Map<UUID, List<Vec3>> KILLS = new HashMap<>();

	private enum State { POST, WATCH, HUNT, COLLECT, FETCH, DELIVER, WAIT }

	private State state = State.POST;
	private int stateTicks;
	private int cooldown;
	private String waitReason = "guard.no_arrows";
	private @Nullable LivingEntity target;
	private @Nullable ChestTrip trip;
	private @Nullable BlockPos roamTo;
	/** A hunting guard that turned back stays at its post for the rest of the night. */
	private long retreatedNight = -1;
	private long lastAlarm = Long.MIN_VALUE / 2;
	private final WorkerSupplies supplies = new WorkerSupplies();

	public GuardPost() {
		super(ImmutableMap.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT), 24000);
	}

	public static boolean isGuard(Villager villager) {
		return villager.getVillagerData().profession().is(WorkingVillagers.GUARD_PROFESSION);
	}

	/** The guard's schedule: work from dusk to the morning round and during raids, sleep, then free time. */
	public static Activity activity(ServerLevel level, Villager villager) {
		if (isRaid(level, villager)) {
			return Activity.WORK;
		}
		long time = timeOfDay(level, villager);
		if (time >= DUSK || time < COLLECT_UNTIL) {
			return Activity.WORK;
		}
		return time < SLEEP_UNTIL ? Activity.REST : Activity.IDLE;
	}

	/** The number of the night (from dusk to dusk), for the monsters of the last night. */
	public static long night(ServerLevel level) {
		return Math.floorDiv(level.getOverworldClockTime() + 24000L - DUSK, 24000L);
	}

	private static boolean isMorning(ServerLevel level, Villager villager) {
		return timeOfDay(level, villager) < COLLECT_UNTIL;
	}

	private static long timeOfDay(ServerLevel level, Villager villager) {
		Long test = TEST_TIME.get(villager.getUUID());
		return test != null ? test : Math.floorMod(level.getOverworldClockTime(), 24000L);
	}

	/** Game tests: the guard lives at this time of day from now on, whatever the clock says. */
	public static void setTimeForTests(Villager villager, long timeOfDay) {
		TEST_TIME.put(villager.getUUID(), timeOfDay);
	}

	/** Whether a raid is going on where the guard is. */
	public static boolean isRaid(ServerLevel level, Villager villager) {
		Raid raid = level.getRaidAt(villager.blockPosition());
		return raid != null && !raid.isOver();
	}

	/**
	 * A monster the guard's arrow killed (see {@code LivingEntityMixin}): counted for the
	 * night, its place noted for the loot, and its experience credited to the guard (in
	 * vanilla only players get it).
	 */
	public static void onKill(ServerLevel level, Villager guard, LivingEntity monster, int experience) {
		HiredVillager hired = (HiredVillager) guard;
		hired.workingvillagers$addWorkDone(night(level), 1);
		KILLS.computeIfAbsent(guard.getUUID(), id -> new ArrayList<>()).add(monster.position());
		if (Config.GUARD_EXPERIENCE.get() && experience > 0) {
			hired.workingvillagers$setExperience(hired.workingvillagers$getExperience() + experience);
		}
	}

	private List<Vec3> kills(Villager villager) {
		return KILLS.computeIfAbsent(villager.getUUID(), id -> new ArrayList<>());
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
			case HUNT -> this.hunt(level, villager, workplace);
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
		boolean raid = isRaid(level, villager);
		if (isMorning(level, villager) && !raid) {
			this.setState(State.COLLECT);
			return;
		}
		int arrows = inventory.countItem(Items.ARROW);
		if (arrows < LOW_ARROWS && !raid && this.inputWithArrows(level, villager, workplace).isPresent()) {
			this.setState(State.FETCH);
			return;
		}
		if (arrows == 0) {
			this.waitReason = "guard.no_arrows";
			this.setState(State.WAIT);
			return;
		}
		if (workplace.hunting() && !raid && this.retreatedNight != night(level) && arrows > HUNT_MIN_ARROWS) {
			this.roamTo = null;
			this.setState(State.HUNT);
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

	/** The monster to shoot at: raiders first, then the closest. */
	private static Optional<LivingEntity> findTarget(ServerLevel level, Villager villager, BlockPos center, int radius) {
		AABB area = new AABB(center).inflate(radius);
		return level.getEntitiesOfClass(LivingEntity.class, area, entity -> isTarget(villager, center, radius, entity)).stream()
			.min(Comparator.<LivingEntity>comparingInt(entity -> entity instanceof Raider ? 0 : 1).thenComparingDouble(entity -> entity.distanceToSqr(villager)));
	}

	/** Rings the closest bell when it sees a raider, at most every half minute: the raiders glow, the villagers hide. */
	private void alarm(ServerLevel level, Villager villager, BlockPos post) {
		if (level.getGameTime() - this.lastAlarm < ALARM_TICKS) {
			return;
		}
		this.lastAlarm = level.getGameTime();
		level.getPoiManager().findClosest(poi -> poi.is(PoiTypes.MEETING), post, BELL_RANGE, PoiManager.Occupancy.ANY)
			.filter(bell -> level.getBlockState(bell).getBlock() instanceof BellBlock)
			.ifPresent(bell -> ((BellBlock) level.getBlockState(bell).getBlock()).attemptToRing(villager, level, bell,
				level.getBlockState(bell).getValue(BellBlock.FACING)));
	}

	/** A monster in the guard's radius that it can see and reach with an arrow. */
	private static boolean isTarget(Villager villager, BlockPos post, int radius, LivingEntity entity) {
		return entity instanceof Enemy && !(entity instanceof NeutralMob) && entity.isAlive() && !entity.isInvisible()
			&& entity.position().closerThan(Vec3.atCenterOf(post), radius)
			&& entity.distanceTo(villager) <= Config.GUARD_RANGE.get()
			&& villager.hasLineOfSight(entity);
	}

	private void watch(ServerLevel level, Villager villager, Workstations.Station workplace) {
		if (isMorning(level, villager)) {
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
		// In a raid the whole village is the post
		int radius = isRaid(level, villager) ? Math.max(workplace.guardRadius(), Config.GUARD_RANGE.get()) : workplace.guardRadius();
		if (this.fight(level, villager, workplace.pos(), radius, bow)) {
			return;
		}
		villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(workplace.pos().above(2)));
		// Back to the spot if it was pushed away, or off hunting again
		if (this.stateTicks % 100 == 0) {
			this.setState(State.POST);
		}
	}

	/** Aims at the best target around {@code center} and shoots when the bow is ready. False if there is none. */
	private boolean fight(ServerLevel level, Villager villager, BlockPos center, int radius, ItemStack bow) {
		LivingEntity current = this.target;
		if (current == null || !isTarget(villager, center, radius, current)) {
			current = this.stateTicks % 10 == 0 ? findTarget(level, villager, center, radius).orElse(null) : null;
		}
		this.target = current;
		if (current == null) {
			return false;
		}
		if (current instanceof Raider && isRaid(level, villager)) {
			this.alarm(level, villager, center);
		}
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		villager.setItemSlot(EquipmentSlot.MAINHAND, bow.copy());
		villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(current, true));
		villager.getLookControl().setLookAt(current, 30.0F, 30.0F);
		if (this.cooldown == 0) {
			this.shoot(level, villager, current, bow);
			this.cooldown = Math.max(5, (int) Math.round(Config.SECONDS_PER_SHOT.get() * 20));
		}
		return true;
	}

	// --- Hunting ---

	/** Roams around the post looking for monsters; turns back with few arrows or wounded. */
	private void hunt(ServerLevel level, Villager villager, Workstations.Station workplace) {
		SimpleContainer inventory = villager.getInventory();
		ItemStack bow = findBow(inventory);
		if (isMorning(level, villager) || isRaid(level, villager) || bow.isEmpty() || !workplace.hunting()) {
			villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			this.setState(State.POST);
			return;
		}
		if (inventory.countItem(Items.ARROW) <= HUNT_MIN_ARROWS || villager.getHealth() < villager.getMaxHealth() / 2) {
			this.retreatedNight = night(level);
			villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			this.setState(State.POST);
			return;
		}
		int huntRadius = Config.HUNT_RADIUS.get();
		if (this.fight(level, villager, villager.blockPosition(), Config.GUARD_RANGE.get(), bow)
			&& this.target != null && this.target.position().closerThan(Vec3.atCenterOf(workplace.pos()), huntRadius)) {
			return;
		}
		this.target = null;
		villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		if (this.roamTo == null || this.roamTo.closerToCenterThan(villager.position(), 2.0) || this.stateTicks % 400 == 0) {
			int dx = villager.getRandom().nextIntBetweenInclusive(-huntRadius, huntRadius);
			int dz = villager.getRandom().nextIntBetweenInclusive(-huntRadius, huntRadius);
			this.roamTo = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, workplace.pos().offset(dx, 0, dz));
		}
		if (this.stateTicks % 20 == 1 && this.roamTo != null) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.roamTo, SPEED, 2));
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
		int reach = workplace.hunting() ? Config.HUNT_RADIUS.get() : workplace.guardRadius();
		AABB area = new AABB(workplace.pos()).inflate(reach + 8);
		Optional<Vec3> arrow = level.getEntitiesOfClass(AbstractArrow.class, area,
				found -> found.getOwner() == villager && found.pickup == AbstractArrow.Pickup.ALLOWED && found.getDeltaMovement().lengthSqr() < 1.0E-4)
			.stream().map(found -> found.position()).min(Comparator.comparingDouble(pos -> pos.distanceToSqr(villager.position())));
		if (arrow.isPresent()) {
			return arrow.get();
		}
		List<Vec3> kills = this.kills(villager);
		while (!kills.isEmpty()) {
			Vec3 kill = kills.getFirst();
			if (!level.getEntitiesOfClass(ItemEntity.class, new AABB(kill, kill).inflate(3.0), ItemEntity::isAlive).isEmpty()) {
				return kill;
			}
			kills.removeFirst();
		}
		return null;
	}

	private void collect(ServerLevel level, Villager villager, Workstations.Station workplace) {
		Vec3 next = this.nextPickup(level, villager, workplace);
		if (next == null || !isMorning(level, villager) && this.stateTicks > WALK_TIMEOUT) {
			this.kills(villager).clear();
			this.bottle(level, villager);
			this.setState(this.hasLoot(villager.getInventory()) && !workplace.validOutputs(level).isEmpty() ? State.DELIVER : State.POST);
			if (next == null && this.state == State.POST && isMorning(level, villager)) {
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
			this.kills(villager).removeIf(kill -> kill.closerThan(next, 1.0));
			this.setState(State.COLLECT);
		}
	}

	/** Fills the experience of its kills into bottles o' enchanting, as many as it has glass bottles for. */
	private void bottle(ServerLevel level, Villager villager) {
		HiredVillager hired = (HiredVillager) villager;
		int perBottle = Config.EXPERIENCE_PER_BOTTLE.get();
		SimpleContainer inventory = villager.getInventory();
		int bottles = Math.min(hired.workingvillagers$getExperience() / perBottle, inventory.countItem(Items.GLASS_BOTTLE));
		if (Config.BOTTLES_PER_NIGHT.get() > 0) {
			bottles = Math.min(bottles, Config.BOTTLES_PER_NIGHT.get());
		}
		if (!Config.GUARD_EXPERIENCE.get() || bottles <= 0) {
			return;
		}
		int filled = 0;
		for (int slot = 0; slot < inventory.getContainerSize() && filled < bottles; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.is(Items.GLASS_BOTTLE)) {
				int now = Math.min(bottles - filled, stack.getCount());
				stack.shrink(now);
				inventory.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
				filled += now;
			}
		}
		ItemStack rest = inventory.addItem(new ItemStack(Items.EXPERIENCE_BOTTLE, filled));
		if (!rest.isEmpty()) {
			villager.spawnAtLocation(level, rest);
		}
		hired.workingvillagers$setExperience(hired.workingvillagers$getExperience() - filled * perBottle);
		level.playSound(null, villager, SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
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
			if (Config.GUARD_EXPERIENCE.get()) {
				Chests.take(container, inventory, stack -> stack.is(Items.GLASS_BOTTLE), 16 - inventory.countItem(Items.GLASS_BOTTLE));
			}
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
		return !stack.isEmpty() && !stack.is(Items.BOW) && !stack.is(Items.ARROW) && !stack.is(Items.GLASS_BOTTLE) && !WorkerSupplies.isWage(stack);
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
			case HUNT -> WorkerStatus.task(villager, this.target != null ? "guard.shoot" : "guard.hunt");
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
