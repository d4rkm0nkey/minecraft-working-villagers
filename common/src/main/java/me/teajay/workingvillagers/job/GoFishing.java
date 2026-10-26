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
import me.teajay.workingvillagers.entity.FisherBobber;
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
import net.minecraft.world.entity.item.ItemEntity;
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
 * least two deep), it puts the boat on the water at the shore of that water (a pond closer
 * by does not keep it there), rows out, fishes there and rows back with its catch, then
 * takes the boat with it again. This can be switched off at the barrel.
 *
 * <p>Catching works like vanilla fishing: a bite after 5 to 30 seconds (a lure makes it
 * quicker), then a catch from the vanilla fishing loot: fish, junk, and on open water
 * treasure (luck of the sea helps). The bobber ({@link FisherBobber}) is only for the looks;
 * the catch flies out of the water to the fisher like to a player. The rod wears like a
 * player's, one use per catch. While it is in its boat it goes on until it is back on land,
 * also after work.
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
	/** How long the bobber stays under after a bite before the fisher strikes. */
	private static final int STRIKE_TICKS = 10;
	/** How long the fisher waits after a catch before it casts again. */
	private static final int RECAST_TICKS = 30;
	/** How long the catch flies to the fisher at most. */
	private static final int CATCH_FLIGHT_TICKS = 30;

	private enum State { SEARCH, WALK, FISH, LAUNCH, ROW, DELIVER, WAIT }

	/** Where the fisher stands on the shore, and the water next to it. */
	private record Spot(BlockPos stand, BlockPos water) {
	}

	/** Where the fisher puts its boat on the water, and the open water it rows out to from there. */
	private record Launch(Spot spot, BlockPos sea) {
	}

	private State state = State.SEARCH;
	private int stateTicks;
	private int waitTicks = SEARCH_COOLDOWN;
	private String waitReason = "fisher.no_water";
	private @Nullable ChestTrip trip;
	private final WorkerSupplies supplies = new WorkerSupplies();
	private @Nullable Spot spot;
	private long spotFoundAt = Long.MIN_VALUE / 2;
	/** Whether the spot was looked for with a boat. */
	private boolean spotByBoat;
	/** The open water to row out to from the spot, if it was looked for with a boat. */
	private @Nullable BlockPos sea;
	/** Out on the water: where it fishes. */
	private @Nullable BlockPos target;
	private @Nullable UUID boat;
	private boolean homeward;
	private boolean byBoat;
	private @Nullable Vec3 lastPosition;
	private int stuckChecks;
	private int biteAt = -1;
	/** When the fisher casts again after a catch. */
	private int castAt;
	private @Nullable UUID bobber;
	/** Where the bobber is, from the cast until the bobber is taken in. */
	private @Nullable BlockPos bobberWater;
	/** The catch on its way out of the water to the fisher. */
	private final List<UUID> flying = new ArrayList<>();
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
		this.takeInBobber(level);
		this.catchFlying(level, villager, true);
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
		this.catchFlying(level, villager, false);
		if (this.state != State.FISH && this.bobberWater != null) {
			this.takeInBobber(level);
		}
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

	/** The places on the shore next to water around the barrel, one for each block of water, the closest first. */
	private static List<Spot> findShore(ServerLevel level, BlockPos barrel, Villager villager) {
		int radius = WorkerLevels.area(villager, Config.WATER_RADIUS.get());
		List<Spot> spots = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(barrel.offset(-radius, -WATER_DOWN, -radius), barrel.offset(radius, WATER_UP, radius))) {
			if (!isSurface(level, pos) || !isPond(level, pos)) {
				continue;
			}
			Spot best = null;
			double bestDistance = Double.MAX_VALUE;
			for (Direction side : Direction.Plane.HORIZONTAL) {
				for (BlockPos stand : List.of(pos.relative(side).above(), pos.relative(side))) {
					double distance = stand.distToCenterSqr(villager.position());
					if (distance < bestDistance && StandSpots.isStandable(level, stand)) {
						best = new Spot(stand.immutable(), pos.immutable());
						bestDistance = distance;
					}
				}
			}
			if (best != null) {
				spots.add(best);
			}
		}
		spots.sort(Comparator.comparingDouble(spot -> spot.stand().distToCenterSqr(villager.position())));
		return spots;
	}

	/**
	 * The closest place on the shore from which the fisher can row out to open water, and the
	 * open water: 7 × 7 blocks of water, some way out but within {@link Config#SEA_RADIUS} of
	 * the barrel, and only water on the way. A pond closer to the barrel does not keep it from
	 * the sea.
	 */
	private static Optional<Launch> findLaunch(ServerLevel level, BlockPos barrel, List<Spot> shore, int radius) {
		Map<Integer, List<BlockPos>> openWater = new HashMap<>();
		for (Spot spot : shore) {
			BlockPos start = spot.water();
			BlockPos best = null;
			double bestScore = Double.MAX_VALUE;
			for (BlockPos pos : openWater.computeIfAbsent(start.getY(), y -> findOpenWater(level, barrel, y, radius))) {
				double distance = Math.sqrt(pos.distSqr(start));
				double score = Math.abs(distance - ROW_BEST);
				if (distance < ROW_MIN || score >= bestScore || !isWaterOnTheWay(level, start, pos)) {
					continue;
				}
				best = pos;
				bestScore = score;
			}
			if (best != null) {
				return Optional.of(new Launch(spot, best));
			}
		}
		return Optional.empty();
	}

	/** The open water within {@link Config#SEA_RADIUS} of the barrel, at the height of the surface. */
	private static List<BlockPos> findOpenWater(ServerLevel level, BlockPos barrel, int y, int radius) {
		List<BlockPos> found = new ArrayList<>();
		for (int dx = -radius; dx <= radius; dx += 2) {
			for (int dz = -radius; dz <= radius; dz += 2) {
				BlockPos pos = new BlockPos(barrel.getX() + dx, y, barrel.getZ() + dz);
				if (isOpenWater(level, pos, 3)) {
					found.add(pos);
				}
			}
		}
		return found;
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

	/**
	 * Where the fisher fishes from the shore, or with a boat where it puts the boat on the
	 * water to row out to {@link #sea} (null if there is no open water to row to).
	 */
	private @Nullable Spot spot(ServerLevel level, Villager villager, BlockPos barrel, long timestamp, boolean byBoat) {
		if (this.spot == null || timestamp - this.spotFoundAt > SCAN_TICKS || byBoat != this.spotByBoat) {
			List<Spot> shore = findShore(level, barrel, villager);
			Optional<Launch> launch = byBoat ? findLaunch(level, barrel, shore, WorkerLevels.area(villager, Config.SEA_RADIUS.get())) : Optional.empty();
			this.spot = launch.map(Launch::spot).orElse(shore.isEmpty() ? null : shore.getFirst());
			this.sea = launch.map(Launch::sea).orElse(null);
			this.spotByBoat = byBoat;
			this.spotFoundAt = timestamp;
		}
		return this.spot;
	}

	// --- The work ---

	private void search(ServerLevel level, Villager villager, Workstations.Station workplace, long timestamp) {
		SimpleContainer inventory = villager.getInventory();
		AbstractBoat boat = this.riding(level, villager);
		// Rowing out from journeyman on
		boolean boatAtHand = workplace.boat() && Unlocks.can(villager, Unlocks.Ability.BOAT) && !boatItem(inventory).isEmpty();
		Spot spot = this.spot(level, villager, workplace.pos(), timestamp, boat != null || boatAtHand);
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
		// The trip is over, the inventory is full, or the rod broke: the catch goes to the chest first
		if (hasCatch(inventory) && (this.catches >= Config.CATCHES_PER_TRIP.get() || !inventory.canAddItem(new ItemStack(Items.COD, 64))
			|| findRod(inventory).isEmpty())) {
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
		this.target = boatAtHand ? this.sea : null;
		this.byBoat = this.target != null;
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
		BlockPos water = this.bobberWater != null ? this.bobberWater : boat != null ? this.ahead(boat, 3.0) : spot.water();
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(water));
		villager.setItemSlot(EquipmentSlot.MAINHAND, rod.copyWithCount(1));
		villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
		if (this.biteAt < 0) {
			if (this.stateTicks < this.castAt) {
				return; // the last catch is still on its way
			}
			int min = Config.MIN_BITE_SECONDS.get() * 20;
			int max = Math.max(min, Config.MAX_BITE_SECONDS.get() * 20);
			int lure = Math.round(EnchantmentHelper.getFishingTimeReduction(level, rod, villager) * 20);
			this.biteAt = this.stateTicks + Math.max(20, Mth.nextInt(villager.getRandom(), min, max) - lure);
			villager.swingForAttack(InteractionHand.MAIN_HAND);
			level.playSound(null, villager, SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F);
			this.bobberWater = water;
			FisherBobber bobber = FisherBobber.cast(level, villager, floatAt(water));
			this.bobber = bobber != null ? bobber.getUUID() : null;
		}
		Vec3 floating = floatAt(water);
		if (this.stateTicks < this.biteAt) {
			if (this.stateTicks % 40 == 0) {
				level.sendParticles(ParticleTypes.FISHING, floating.x, floating.y + 0.2, floating.z, 1, 0.05, 0.0, 0.05, 0.0);
			}
			return;
		}
		if (this.stateTicks == this.biteAt) {
			// A bite: the bobber dips, then the fisher strikes
			FisherBobber bobber = this.bobber(level);
			if (bobber != null) {
				bobber.setBiting(true);
			}
			level.sendParticles(ParticleTypes.SPLASH, floating.x, floating.y + 0.2, floating.z, 12, 0.2, 0.0, 0.2, 0.0);
			level.sendParticles(ParticleTypes.BUBBLE, floating.x, floating.y, floating.z, 6, 0.1, 0.1, 0.1, 0.0);
			level.playSound(null, floating.x, floating.y, floating.z, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.25F, 1.0F);
		}
		if (this.stateTicks < this.biteAt + STRIKE_TICKS) {
			return;
		}
		this.biteAt = -1;
		this.castAt = this.stateTicks + RECAST_TICKS;
		level.playSound(null, villager, SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1.0F, 1.0F);
		villager.swingForAttack(InteractionHand.MAIN_HAND);
		this.takeInBobber(level);
		this.reelIn(level, villager, rod, water, isOpenWater(level, water, 2));
	}

	/** Where the bobber floats on the water. */
	private static Vec3 floatAt(BlockPos water) {
		return new Vec3(water.getX() + 0.5, water.getY() + 0.8, water.getZ() + 0.5);
	}

	private @Nullable FisherBobber bobber(ServerLevel level) {
		return this.bobber != null && level.getEntity(this.bobber) instanceof FisherBobber bobber ? bobber : null;
	}

	private void takeInBobber(ServerLevel level) {
		FisherBobber bobber = this.bobber(level);
		if (bobber != null) {
			bobber.discard();
		}
		this.bobber = null;
		this.bobberWater = null;
	}

	/**
	 * A catch from the vanilla fishing loot; one use of the rod. The catch flies out of the
	 * water to the fisher, like to a player, and goes into its inventory when it arrives
	 * (see {@link #catchFlying}).
	 */
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
		Vec3 from = floatAt(bobber);
		for (ItemStack stack : level.getServer().reloadableRegistries().getLootTable(table).getRandomItems(params)) {
			ItemEntity item = new ItemEntity(level, from.x, from.y, from.z, stack);
			double dx = villager.getX() - from.x;
			double dy = villager.getY() - from.y;
			double dz = villager.getZ() - from.z;
			item.setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08, dz * 0.1);
			// Nobody else picks it up on the way
			item.setPickUpDelay(CATCH_FLIGHT_TICKS * 2);
			if (level.addFreshEntity(item)) {
				this.flying.add(item.getUUID());
			} else {
				addCatch(level, villager, stack);
			}
		}
		this.catches++;
		((HiredVillager) villager).workingvillagers$addWorkDone(ChopTrees.today(level), 1);
		WorkerLevels.wear(level, villager, rod, 1, broken -> villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY));
		villager.getInventory().setChanged();
	}

	/**
	 * The catch that flew to the fisher goes into its inventory once it is there (or has
	 * flown long enough); with {@code all} at once, when the fisher stops.
	 */
	private void catchFlying(ServerLevel level, Villager villager, boolean all) {
		if (this.flying.isEmpty()) {
			return;
		}
		this.flying.removeIf(id -> {
			if (!(level.getEntity(id) instanceof ItemEntity item) || item.isRemoved()) {
				return true; // merged with another catch, or taken by a hopper
			}
			if (!all && item.getAge() < CATCH_FLIGHT_TICKS && item.distanceToSqr(villager) > 1.5 * 1.5) {
				return false;
			}
			addCatch(level, villager, item.getItem());
			item.discard();
			return true;
		});
	}

	private static void addCatch(ServerLevel level, Villager villager, ItemStack stack) {
		ItemStack rest = villager.getInventory().addItem(stack);
		if (!rest.isEmpty()) {
			villager.spawnAtLocation(level, rest);
		}
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
		this.castAt = 0;
	}
}
