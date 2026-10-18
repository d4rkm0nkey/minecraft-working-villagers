package me.teajay.workingvillagers.job;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.mixin.BoatItemAccessor;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The work of the fisher: fish at the water around its barrel and bring the catch to one of
 * the chests linked to the barrel. Without a linked chest the fisher does not work.
 *
 * <p>From the shore it fishes at a pond or a river within {@link Config#WATER_RADIUS}. If it
 * has a boat and there is open water nearby (a sea, a big lake: 7 × 7 blocks of water at
 * least two deep), it puts the boat on the water at the shore, rows out, fishes there and
 * rows back with its catch, then takes the boat with it again. This can be switched off at
 * the barrel.
 *
 * <p>Catching works like vanilla fishing without the bobber: a bite after 5 to 30 seconds
 * (a lure makes it quicker), then a catch from the vanilla fishing loot: fish, junk, and on
 * open water treasure (luck of the sea helps). The rod wears like a player's, one use per
 * catch. While it is in its boat it goes on until it is back on land, also after work.
 */
public class GoFishing extends Behavior<Villager> {
	private static final float SPEED = 0.5F;
	/** How fast the fisher rows (blocks per tick); a player rows about as fast. */
	private static final double ROW_SPEED = 0.18;
	private static final int WATER_DOWN = 4;
	private static final int WATER_UP = 4;
	private static final int WALK_TIMEOUT = 400;
	private static final int SEARCH_COOLDOWN = 100;
	/** How long the found water is trusted before it is looked for again. */
	private static final int SCAN_TICKS = 400;
	/** How far out (from the shore) the fisher rows: at least, at best and at most. */
	private static final int ROW_MIN = 6;
	private static final int ROW_BEST = 14;
	/** How often (ticks) rowing is checked for progress, and how many failed checks give up. */
	private static final int PROGRESS_TICKS = 60;
	private static final int STUCK_CHECKS = 3;
	/** Junk, fish and treasure, like vanilla fishing at luck 0. */
	private static final int JUNK_WEIGHT = 10;
	private static final int FISH_WEIGHT = 85;
	private static final int TREASURE_WEIGHT = 5;

	private enum State { SEARCH, WALK, FISH, LAUNCH, ROW, DELIVER, WAIT }

	/** Where the fisher stands on the shore, and the water next to it. */
	private record Spot(BlockPos stand, BlockPos water) {
	}

	private State state = State.SEARCH;
	private int stateTicks;
	private int waitTicks = SEARCH_COOLDOWN;
	private String waitReason = "fisher.no_water";
	private @Nullable ChestTrip trip;
	private final WorkerSupplies supplies = new WorkerSupplies();
	private @Nullable Spot spot;
	private long spotFoundAt = Long.MIN_VALUE / 2;
	/** Out on the water: where it fishes. */
	private @Nullable BlockPos target;
	private @Nullable UUID boat;
	private boolean homeward;
	private boolean byBoat;
	private @Nullable Vec3 lastPosition;
	private int stuckChecks;
	private int biteAt = -1;
	private int catches;

	public GoFishing() {
		super(ImmutableMap.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT), 24000);
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
		return this.canWork(level, villager);
	}

	/** Once in the boat it goes on until it is back on land, also after work. */
	@Override
	protected boolean canStillUse(ServerLevel level, Villager villager, long timestamp) {
		if (this.riding(level, villager) != null) {
			return villager.getVillagerData().profession().is(WorkingVillagers.FISHER_PROFESSION);
		}
		return this.canWork(level, villager) && villager.getBrain().isActive(Activity.WORK);
	}

	private boolean canWork(ServerLevel level, Villager villager) {
		if (!villager.getVillagerData().profession().is(WorkingVillagers.FISHER_PROFESSION)) {
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
		AbstractBoat boat = this.riding(level, villager);
		if (boat != null) {
			// Only if something else takes over: never leave it out on the water
			this.land(level, villager, boat);
		}
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
		if (this.state == State.SEARCH && this.riding(level, villager) == null && !hasCatch(villager.getInventory())
			&& this.supplies.busy(level, villager, WorkerSupplies.ROD)) {
			return; // the chest sets the status
		}
		switch (this.state) {
			case SEARCH -> this.search(level, villager, workplace, timestamp);
			case WALK -> this.walk(level, villager);
			case FISH -> this.fish(level, villager);
			case LAUNCH -> this.launch(level, villager);
			case ROW -> this.row(level, villager);
			case DELIVER -> this.deliver(level, villager, workplace);
			case WAIT -> {
				if (this.stateTicks >= this.waitTicks) {
					this.setState(State.SEARCH);
				}
			}
		}
		this.report(villager);
	}

	// --- Finding water ---

	private static boolean isSurface(ServerLevel level, BlockPos pos) {
		FluidState fluid = level.getFluidState(pos);
		return fluid.is(FluidTags.WATER) && fluid.isSource() && level.getBlockState(pos.above()).isAir();
	}

	/** Water all around (radius blocks to each side) and two blocks deep: room for a bobber, or for a boat. */
	private static boolean isOpenWater(ServerLevel level, BlockPos pos, int radius) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (!isSurface(level, pos.offset(dx, 0, dz))) {
					return false;
				}
			}
		}
		return level.getFluidState(pos.below()).is(FluidTags.WATER);
	}

	/** At least a small pond, not a single puddle. */
	private static boolean isPond(ServerLevel level, BlockPos pos) {
		int water = 0;
		for (BlockPos around : BlockPos.betweenClosed(pos.offset(-2, 0, -2), pos.offset(2, 0, 2))) {
			if (isSurface(level, around)) {
				water++;
			}
		}
		return water >= 4;
	}

	/** The closest place on the shore next to water around the barrel. */
	private static Optional<Spot> findShore(ServerLevel level, BlockPos barrel, Villager villager) {
		int radius = Config.WATER_RADIUS.get();
		Spot best = null;
		double bestDistance = Double.MAX_VALUE;
		for (BlockPos pos : BlockPos.betweenClosed(barrel.offset(-radius, -WATER_DOWN, -radius), barrel.offset(radius, WATER_UP, radius))) {
			if (!isSurface(level, pos) || !isPond(level, pos)) {
				continue;
			}
			for (Direction side : Direction.Plane.HORIZONTAL) {
				for (BlockPos stand : List.of(pos.relative(side).above(), pos.relative(side))) {
					double distance = stand.distToCenterSqr(villager.position());
					if (distance < bestDistance && StandSpots.isStandable(level, stand)) {
						best = new Spot(stand.immutable(), pos.immutable());
						bestDistance = distance;
					}
				}
			}
		}
		return Optional.ofNullable(best);
	}

	/**
	 * Open water to row out to from the spot on the shore: 7 × 7 blocks of water, some way out
	 * but within {@link Config#SEA_RADIUS} of the barrel, and only water on the way.
	 */
	private static Optional<BlockPos> findOpenWater(ServerLevel level, BlockPos barrel, Spot spot) {
		int radius = Config.SEA_RADIUS.get();
		BlockPos start = spot.water();
		BlockPos best = null;
		double bestScore = Double.MAX_VALUE;
		for (int dx = -radius; dx <= radius; dx += 2) {
			for (int dz = -radius; dz <= radius; dz += 2) {
				BlockPos pos = new BlockPos(barrel.getX() + dx, start.getY(), barrel.getZ() + dz);
				double distance = Math.sqrt(pos.distSqr(start));
				double score = Math.abs(distance - ROW_BEST);
				if (distance < ROW_MIN || score >= bestScore || !isOpenWater(level, pos, 3) || !isWaterOnTheWay(level, start, pos)) {
					continue;
				}
				best = pos;
				bestScore = score;
			}
		}
		return Optional.ofNullable(best);
	}

	private static boolean isWaterOnTheWay(ServerLevel level, BlockPos from, BlockPos to) {
		double distance = Math.sqrt(from.distSqr(to));
		for (double along = 0; along <= distance; along += 0.5) {
			double t = along / distance;
			BlockPos pos = BlockPos.containing(Mth.lerp(t, from.getX(), to.getX()) + 0.5, from.getY(), Mth.lerp(t, from.getZ(), to.getZ()) + 0.5);
			if (!isSurface(level, pos)) {
				return false;
			}
		}
		return true;
	}

	private @Nullable Spot spot(ServerLevel level, Villager villager, BlockPos barrel, long timestamp) {
		if (this.spot == null || timestamp - this.spotFoundAt > SCAN_TICKS) {
			this.spot = findShore(level, barrel, villager).orElse(null);
			this.spotFoundAt = timestamp;
		}
		return this.spot;
	}

	// --- The work ---

	private void search(ServerLevel level, Villager villager, Workstations.Station workplace, long timestamp) {
		SimpleContainer inventory = villager.getInventory();
		AbstractBoat boat = this.riding(level, villager);
		Spot spot = this.spot(level, villager, workplace.pos(), timestamp);
		if (boat != null) {
			// Back on the water after a reload: home first
			if (spot == null) {
				villager.stopRiding();
				return;
			}
			this.homeward = true;
			this.setState(State.ROW);
			return;
		}
		if (hasCatch(inventory) && (this.catches >= Config.CATCHES_PER_TRIP.get() || !inventory.canAddItem(new ItemStack(Items.COD, 64)))) {
			this.setState(State.DELIVER);
			return;
		}
		if (spot == null) {
			if (hasCatch(inventory)) {
				this.setState(State.DELIVER);
			} else {
				this.rest(level, villager, workplace, "fisher.no_water");
			}
			return;
		}
		this.byBoat = false;
		this.target = null;
		if (workplace.boat() && !boatItem(inventory).isEmpty()) {
			this.target = findOpenWater(level, workplace.pos(), spot).orElse(null);
			this.byBoat = this.target != null;
		}
		this.setState(State.WALK);
	}

	private void walk(ServerLevel level, Villager villager) {
		Spot spot = this.spot;
		if (spot == null) {
			this.setState(State.SEARCH);
			return;
		}
		if (StandSpots.isAt(villager, spot.stand())) {
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			this.biteAt = -1;
			this.setState(this.byBoat ? State.LAUNCH : State.FISH);
		} else if (this.stateTicks > WALK_TIMEOUT) {
			this.spot = null;
			this.waitReason = "fisher.water_unreachable";
			this.waitTicks = SEARCH_COOLDOWN;
			this.setState(State.WAIT);
		} else if (this.stateTicks % 20 == 1) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(spot.stand(), SPEED, 0));
			villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(spot.water()));
		}
	}

	/** Waits for a bite with the rod out, then reels in the catch. */
	private void fish(ServerLevel level, Villager villager) {
		AbstractBoat boat = this.riding(level, villager);
		Spot spot = this.spot;
		if (boat == null && (spot == null || !StandSpots.isAt(villager, spot.stand()))) {
			this.setState(State.SEARCH);
			return;
		}
		ItemStack rod = findRod(villager.getInventory());
		boolean done = this.catches >= Config.CATCHES_PER_TRIP.get() || !villager.getBrain().isActive(Activity.WORK)
			|| !villager.getInventory().canAddItem(new ItemStack(Items.COD, 64));
		if (rod.isEmpty() || done) {
			villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			if (boat != null) {
				this.homeward = true;
				this.setState(State.ROW);
			} else {
				this.setState(State.SEARCH);
			}
			return;
		}
		if (boat != null) {
			boat.setDeltaMovement(0.0, boat.getDeltaMovement().y, 0.0);
		}
		BlockPos bobber = boat != null ? this.ahead(boat, 3.0) : spot.water();
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(bobber));
		villager.setItemSlot(EquipmentSlot.MAINHAND, rod.copyWithCount(1));
		villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
		if (this.biteAt < 0) {
			int min = Config.MIN_BITE_SECONDS.get() * 20;
			int max = Math.max(min, Config.MAX_BITE_SECONDS.get() * 20);
			int lure = Math.round(EnchantmentHelper.getFishingTimeReduction(level, rod, villager) * 20);
			this.biteAt = this.stateTicks + Math.max(20, Mth.nextInt(villager.getRandom(), min, max) - lure);
			villager.swingForAttack(InteractionHand.MAIN_HAND);
			level.playSound(null, villager, SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F);
		}
		if (this.stateTicks < this.biteAt) {
			if (this.stateTicks % 40 == 0) {
				level.sendParticles(ParticleTypes.FISHING, bobber.getX() + 0.5, bobber.getY() + 1.0, bobber.getZ() + 0.5, 1, 0.05, 0.0, 0.05, 0.0);
			}
			return;
		}
		this.biteAt = -1;
		level.sendParticles(ParticleTypes.SPLASH, bobber.getX() + 0.5, bobber.getY() + 1.0, bobber.getZ() + 0.5, 12, 0.2, 0.0, 0.2, 0.0);
		level.playSound(null, villager, SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1.0F, 1.0F);
		villager.swingForAttack(InteractionHand.MAIN_HAND);
		this.reelIn(level, villager, rod, bobber, isOpenWater(level, bobber, 2));
	}

	/** A catch from the vanilla fishing loot; one use of the rod. */
	private void reelIn(ServerLevel level, Villager villager, ItemStack rod, BlockPos bobber, boolean openWater) {
		int luck = EnchantmentHelper.getFishingLuckBonus(level, rod, villager);
		int junk = Math.max(0, JUNK_WEIGHT - 2 * luck);
		int fish = Math.max(0, FISH_WEIGHT - luck);
		int treasure = openWater ? TREASURE_WEIGHT + 2 * luck : 0;
		int roll = villager.getRandom().nextInt(Math.max(1, junk + fish + treasure));
		ResourceKey<LootTable> table = roll < fish ? BuiltInLootTables.FISHING_FISH
			: roll < fish + junk ? BuiltInLootTables.FISHING_JUNK
			: BuiltInLootTables.FISHING_TREASURE;
		LootParams params = new LootParams.Builder(level)
			.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(bobber))
			.withParameter(LootContextParams.TOOL, rod)
			.withParameter(LootContextParams.THIS_ENTITY, villager)
			.withLuck(luck)
			.create(LootContextParamSets.FISHING);
		for (ItemStack stack : level.getServer().reloadableRegistries().getLootTable(table).getRandomItems(params)) {
			ItemStack rest = villager.getInventory().addItem(stack);
			if (!rest.isEmpty()) {
				villager.spawnAtLocation(level, rest);
			}
		}
		this.catches++;
		((HiredVillager) villager).workingvillagers$addWorkDone(ChopTrees.today(level), 1);
		rod.hurtAndBreak(1, level, null, broken -> villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY));
		villager.getInventory().setChanged();
	}

	// --- The boat ---

	private @Nullable AbstractBoat riding(ServerLevel level, Villager villager) {
		return villager.getVehicle() instanceof AbstractBoat boat && boat.getUUID().equals(this.boat) ? boat : null;
	}

	/** The water a few blocks ahead of the boat, where the bobber is. */
	private BlockPos ahead(AbstractBoat boat, double distance) {
		float yaw = boat.getYRot() * Mth.DEG_TO_RAD;
		return BlockPos.containing(boat.getX() - Mth.sin(yaw) * distance, boat.getY() - 0.5, boat.getZ() + Mth.cos(yaw) * distance);
	}

	/** Puts the boat on the water next to the spot on the shore and gets in. */
	private void launch(ServerLevel level, Villager villager) {
		Spot spot = this.spot;
		ItemStack item = boatItem(villager.getInventory());
		if (spot == null || this.target == null || !(item.getItem() instanceof BoatItem)) {
			this.setState(State.SEARCH);
			return;
		}
		AbstractBoat boat = ((BoatItemAccessor) item.getItem()).workingvillagers$getEntityType().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
		if (boat == null) {
			this.byBoat = false;
			this.setState(State.FISH);
			return;
		}
		BlockPos water = spot.water();
		boat.snapTo(water.getX() + 0.5, water.getY() + 1.0, water.getZ() + 0.5, yaw(Vec3.atCenterOf(water), Vec3.atCenterOf(this.target)), 0.0F);
		if (!level.addFreshEntity(boat)) {
			this.byBoat = false;
			this.setState(State.FISH);
			return;
		}
		item.shrink(1);
		villager.getInventory().setChanged();
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		if (!villager.startRiding(boat)) {
			boat.discard();
			villager.getInventory().addItem(boat.getPickResult());
			this.byBoat = false;
			this.setState(State.FISH);
			return;
		}
		this.boat = boat.getUUID();
		this.homeward = false;
		this.lastPosition = boat.position();
		this.stuckChecks = 0;
		this.setState(State.ROW);
	}

	/** The yaw of a boat heading from one point to another. */
	private static float yaw(Vec3 from, Vec3 to) {
		return (float) (Mth.atan2(-(to.x - from.x), to.z - from.z) * Mth.RAD_TO_DEG);
	}

	/** Rows out to the open water, or home to the spot on the shore. */
	private void row(ServerLevel level, Villager villager) {
		AbstractBoat boat = level.getEntity(this.boat != null ? this.boat : new UUID(0, 0)) instanceof AbstractBoat found ? found : null;
		Spot spot = this.spot;
		if (boat == null || villager.getVehicle() != boat) {
			this.lostBoat(level, villager, boat);
			return;
		}
		BlockPos destination = this.homeward ? (spot != null ? spot.water() : null) : this.target;
		if (destination == null) {
			this.land(level, villager, boat);
			return;
		}
		Vec3 to = Vec3.atCenterOf(destination);
		double dx = to.x - boat.getX();
		double dz = to.z - boat.getZ();
		double distance = Math.sqrt(dx * dx + dz * dz);
		if (distance < 1.3) {
			boat.setDeltaMovement(0.0, boat.getDeltaMovement().y, 0.0);
			if (this.homeward) {
				this.land(level, villager, boat);
			} else {
				this.biteAt = -1;
				this.setState(State.FISH);
			}
			return;
		}
		boat.setYRot(yaw(boat.position(), to));
		double speed = Math.min(ROW_SPEED, distance / 4.0 + 0.04);
		boat.setDeltaMovement(dx / distance * speed, boat.getDeltaMovement().y, dz / distance * speed);
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(destination));
		if (this.stateTicks % PROGRESS_TICKS == 0) {
			Vec3 last = this.lastPosition;
			this.lastPosition = boat.position();
			if (last != null && last.distanceTo(boat.position()) < 1.0 && ++this.stuckChecks >= STUCK_CHECKS) {
				if (this.homeward) {
					// Stuck on the way home: land where it is
					this.land(level, villager, boat);
				} else {
					this.homeward = true;
					this.stuckChecks = 0;
				}
			}
		}
	}

	/** Back at the shore: gets out onto the spot, takes the boat and goes to deliver. */
	private void land(ServerLevel level, Villager villager, AbstractBoat boat) {
		villager.stopRiding();
		Spot spot = this.spot;
		if (spot != null) {
			villager.teleportTo(spot.stand().getX() + 0.5, spot.stand().getY(), spot.stand().getZ() + 0.5);
		}
		ItemStack item = boat.getPickResult();
		boat.discard();
		ItemStack rest = villager.getInventory().addItem(item);
		if (!rest.isEmpty()) {
			villager.spawnAtLocation(level, rest);
		}
		this.boat = null;
		this.homeward = false;
		this.setState(State.SEARCH);
	}

	/** Out of the boat without landing (it broke, something pushed the fisher out): takes it back if it is close. */
	private void lostBoat(ServerLevel level, Villager villager, @Nullable AbstractBoat boat) {
		if (boat != null && boat.getPassengers().isEmpty() && boat.distanceTo(villager) < 4.0) {
			ItemStack rest = villager.getInventory().addItem(boat.getPickResult());
			boat.discard();
			if (!rest.isEmpty()) {
				villager.spawnAtLocation(level, rest);
			}
		}
		villager.stopRiding();
		this.boat = null;
		this.homeward = false;
		this.setState(State.SEARCH);
	}

	// --- Items ---

	private static ItemStack findRod(SimpleContainer inventory) {
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && stack.is(Items.FISHING_ROD) && !WorkerSupplies.isWorn(stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static ItemStack boatItem(SimpleContainer inventory) {
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && stack.is(ItemTags.BOATS) && stack.getItem() instanceof BoatItem) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	/** What the fisher keeps: its rods, its boat and its wage. */
	private static boolean isKept(ItemStack stack) {
		return stack.is(Items.FISHING_ROD) || stack.is(ItemTags.BOATS) || WorkerSupplies.isWage(stack);
	}

	private static boolean hasCatch(SimpleContainer inventory) {
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && !isKept(stack)) {
				return true;
			}
		}
		return false;
	}

	private void deliver(ServerLevel level, Villager villager, Workstations.Station workplace) {
		if (this.trip == null) {
			villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			if (!hasCatch(villager.getInventory())) {
				this.catches = 0;
				this.setState(State.SEARCH);
				return;
			}
			Optional<BlockPos> chest = Chests.closest(workplace.validOutputs(level).stream().filter(pos -> Chests.hasRoom(level, pos)).toList(), villager);
			if (chest.isEmpty()) {
				this.rest(level, villager, workplace, "chests_full");
				return;
			}
			this.trip = ChestTrip.to(level, villager, chest.get());
		}
		ChestTrip.Result result = this.trip.tick(level, villager, container -> {
			SimpleContainer inventory = villager.getInventory();
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				ItemStack stack = inventory.getItem(slot);
				if (!stack.isEmpty() && !isKept(stack)) {
					inventory.setItem(slot, HopperBlockEntity.addItem(null, container, stack, null));
				}
			}
		});
		if (result == ChestTrip.Result.RUNNING) {
			return;
		}
		this.trip = null;
		if (result == ChestTrip.Result.FAILED) {
			this.rest(level, villager, workplace, "chest_unreachable");
		} else if (hasCatch(villager.getInventory())) {
			this.setState(State.DELIVER);
		} else {
			this.catches = 0;
			this.setState(State.SEARCH);
		}
	}

	/** Goes back to the barrel and waits next to it. */
	private void rest(ServerLevel level, Villager villager, Workstations.Station workplace, String reason) {
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
		this.waitTicks = SEARCH_COOLDOWN;
		this.setState(State.WAIT);
	}

	private void report(Villager villager) {
		switch (this.state) {
			case SEARCH, WALK -> WorkerStatus.task(villager, "fisher.search");
			case FISH -> WorkerStatus.task(villager, villager.getVehicle() instanceof AbstractBoat ? "fisher.fish_boat" : "fisher.fish");
			case LAUNCH, ROW -> WorkerStatus.task(villager, this.homeward ? "fisher.row_back" : "fisher.row_out");
			case DELIVER -> WorkerStatus.task(villager, "deliver");
			case WAIT -> WorkerStatus.task(villager, this.waitReason);
		}
	}

	private void setState(State state) {
		this.state = state;
		this.stateTicks = 0;
	}
}
