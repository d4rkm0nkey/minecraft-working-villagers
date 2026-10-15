package me.teajay.workingvillagers.job;

import com.mojang.logging.LogUtils;
import java.util.Optional;
import me.teajay.workingvillagers.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Notices workers that are stuck (in a hole, behind a fence, boxed in) and brings them back
 * to their workstation (a carrier: to the source of its first route).
 *
 * <p>A worker is stuck while it wants to walk somewhere during work hours, vanilla
 * pathfinding cannot reach its walk target (the {@code CANT_REACH_WALK_TARGET_SINCE}
 * memory) and it does not get further than a few blocks. Standing still at a station or a
 * chest is not stuck: the worker does not want to walk then. After
 * {@link #STATUS_TICKS} the menus show it as stuck, after {@link Config#STUCK_SECONDS} it is
 * moved to its workstation.
 */
public final class StuckWorkers {
	private static final Logger LOGGER = LogUtils.getLogger();
	/** How far the worker has to get from where it got stuck to count as moving again. */
	private static final double PROGRESS_DISTANCE = 3.0;
	/** A worker that has not wanted to walk for this long does stationary work. */
	private static final int WALK_GRACE_TICKS = 200;
	/** How long a worker has to be stuck before the menus say so. */
	private static final int STATUS_TICKS = 400;

	private StuckWorkers() {
	}

	/** What the stuck check knows about one worker. Not saved: a restart starts over. */
	public static final class Watch {
		private @Nullable Vec3 anchor;
		private int stuckTicks;
		private int lastWalkTick = Integer.MIN_VALUE / 2;

		private void reset(Vec3 position) {
			this.anchor = position;
			this.stuckTicks = 0;
		}

		public boolean isStuck() {
			return this.stuckTicks >= STATUS_TICKS;
		}
	}

	/** Called every tick: notes whether the worker wants to walk somewhere. */
	static void noteWalking(Villager villager) {
		if (villager.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)) {
			((HiredVillager) villager).workingvillagers$getStuckWatch().lastWalkTick = villager.tickCount;
		}
	}

	/** Called every {@code interval} ticks for every worker. */
	static void check(ServerLevel level, Villager villager, int interval) {
		Watch watch = ((HiredVillager) villager).workingvillagers$getStuckWatch();
		Brain<Villager> brain = villager.getBrain();
		boolean trying = brain.isActive(Activity.WORK)
			&& !villager.isSleeping() && !villager.isPassenger() && !villager.isLeashed()
			&& villager.tickCount - watch.lastWalkTick <= WALK_GRACE_TICKS
			&& brain.hasMemoryValue(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
		if (!trying || watch.anchor == null || !watch.anchor.closerThan(villager.position(), PROGRESS_DISTANCE)) {
			watch.reset(villager.position());
			return;
		}
		watch.stuckTicks += interval;
		int limit = Config.STUCK_SECONDS.get() * 20;
		if (limit > 0 && watch.stuckTicks >= limit) {
			if (bringBack(level, villager)) {
				watch.reset(villager.position());
			} else {
				// Nowhere better to go: stays stuck, the menus say so
				watch.stuckTicks = limit - interval;
			}
		}
	}

	/**
	 * Moves the worker next to its workstation. Not if it can walk there by itself (then
	 * only the places its work leads it to are out of reach), or if there is no room there.
	 */
	private static boolean bringBack(ServerLevel level, Villager villager) {
		BlockPos workplace = WorkerSupplies.anchor(level, villager);
		Optional<BlockPos> spot = StandSpots.find(level, workplace, villager);
		if (spot.isEmpty() || !level.isLoaded(spot.get()) || StandSpots.isAt(villager, spot.get())) {
			return false;
		}
		Path path = villager.getNavigation().createPath(spot.get(), 0);
		if (path != null && path.canReach()) {
			return false;
		}
		Vec3 target = Vec3.atBottomCenterOf(spot.get());
		Vec3 from = villager.position();
		villager.getNavigation().stop();
		Brain<Villager> brain = villager.getBrain();
		brain.eraseMemory(MemoryModuleType.WALK_TARGET);
		brain.eraseMemory(MemoryModuleType.PATH);
		brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
		villager.teleportTo(target.x, target.y, target.z);
		level.sendParticles(ParticleTypes.POOF, from.x, from.y + 1.0, from.z, 12, 0.3, 0.5, 0.3, 0.02);
		level.sendParticles(ParticleTypes.POOF, target.x, target.y + 1.0, target.z, 12, 0.3, 0.5, 0.3, 0.02);
		LOGGER.info("Worker {} was stuck at {}, moved it to its workplace at {}", villager.getName().getString(), BlockPos.containing(from), spot.get());
		return true;
	}
}
