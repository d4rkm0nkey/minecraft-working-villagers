package me.teajay.workingvillagers.job;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import me.teajay.workingvillagers.carrier.Carriers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.pathfinder.Path;

/**
 * Hired villagers without a job look for a free workstation of this mod themselves, like
 * vanilla villagers look for a job site: the closest one they can walk to within 48 blocks.
 * They walk there (vanilla {@code GoToPotentialJobSite}) and take the job when they arrive,
 * with the profession of this mod (a stone sawyer at a stonecutter, not a mason).
 *
 * <p>Part of the core activities of every villager, but it only does something for hired
 * ones. Runs before vanilla {@code AssignProfessionFromJobSite}, which would give the
 * vanilla profession.
 */
public class FindWorkplace extends Behavior<Villager> {
	private static final int SCAN_RANGE = AcquirePoi.SCAN_RANGE;
	/** How close the villager has to be to take the job, like vanilla. */
	private static final double ARRIVE_DISTANCE = 2.0;
	private static final int SEARCH_INTERVAL = 40;

	private long nextSearch;

	public FindWorkplace() {
		super(ImmutableMap.of(), 1);
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
		if (!((HiredVillager) villager).workingvillagers$isHired() || villager.isBaby() || Carriers.isCarrier(villager)) {
			return false;
		}
		Optional<GlobalPos> potential = villager.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
		if (potential.isPresent()) {
			return this.arrived(level, villager, potential.get());
		}
		if (!villager.getVillagerData().profession().is(VillagerProfession.NONE)
			|| villager.getBrain().hasMemoryValue(MemoryModuleType.JOB_SITE)
			|| level.getGameTime() < this.nextSearch) {
			return false;
		}
		this.nextSearch = level.getGameTime() + SEARCH_INTERVAL + level.getRandom().nextInt(SEARCH_INTERVAL);
		this.search(level, villager);
		return false;
	}

	/** At the job site it walked to: takes the job. */
	private boolean arrived(ServerLevel level, Villager villager, GlobalPos site) {
		if (site.dimension() != level.dimension() || !site.pos().closerToCenterThan(villager.position(), ARRIVE_DISTANCE)) {
			return false;
		}
		WorkerJobs.takeJob(level, villager, site.pos());
		return false;
	}

	private void search(ServerLevel level, Villager villager) {
		PoiManager poiManager = level.getPoiManager();
		Set<Pair<Holder<PoiType>, BlockPos>> sites = poiManager.findAllClosestFirstWithType(
				WorkerJobs::isJobSite, pos -> !WorkerJobs.isLeftBy(level, villager, pos), villager.blockPosition(), SCAN_RANGE, PoiManager.Occupancy.HAS_SPACE
			)
			.limit(5L)
			.collect(Collectors.toSet());
		Path path = AcquirePoi.findPathToPois(villager, sites);
		if (path == null || !path.canReach()) {
			return;
		}
		BlockPos target = path.getTarget();
		if (poiManager.take(WorkerJobs::isJobSite, (type, pos) -> pos.equals(target), target, 1).isPresent()) {
			villager.getBrain().setMemory(MemoryModuleType.POTENTIAL_JOB_SITE, GlobalPos.of(level.dimension(), target));
			level.debugSynchronizers().updatePoi(target);
		}
	}
}
