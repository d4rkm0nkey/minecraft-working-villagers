package me.teajay.workingvillagers.job;

import java.util.Map;
import java.util.Optional;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.carrier.Carriers;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.jspecify.annotations.Nullable;

/**
 * The jobs of hired villagers: the player gives a hired villager a workstation (chopping
 * block: lumberjack, woodcutter: sawyer, stonecutter: stone sawyer, carpenter's bench:
 * carpenter) in the station menu,
 * and takes it away again there. Hired villagers do not take any job by themselves.
 *
 * <p>Vanilla job acquisition only looks at job sites in the {@code acquirable_job_site} tag,
 * which the chopping block, the woodcutter and the carpenter's bench are not part of. The stonecutter is in the
 * tag, ordinary villagers still become masons there.
 */
public final class WorkerJobs {
	private static final Map<ResourceKey<PoiType>, ResourceKey<VillagerProfession>> JOBS = Map.of(
		WorkingVillagers.LUMBERJACK_POI, WorkingVillagers.LUMBERJACK_PROFESSION,
		WorkingVillagers.WOODCUTTER_POI, WorkingVillagers.WOODCUTTER_PROFESSION,
		WorkingVillagers.CARPENTER_POI, WorkingVillagers.CARPENTER_PROFESSION,
		PoiTypes.MASON, WorkingVillagers.STONECUTTER_PROFESSION
	);
	private static final int CHECK_INTERVAL = 20;

	private WorkerJobs() {
	}

	public static boolean isJobSite(Holder<PoiType> poi) {
		return poi.unwrapKey().map(JOBS::containsKey).orElse(false);
	}

	public static void tick(ServerLevel level, Villager villager) {
		HiredVillager hired = (HiredVillager) villager;
		if (!hired.workingvillagers$isHired() || villager.isBaby()) {
			return;
		}
		boolean unemployed = villager.getVillagerData().profession().is(VillagerProfession.NONE);
		// A hired villager does not take ordinary village jobs. Checked every tick: next to a
		// stonecutter, vanilla would make it a mason in the tick after it picked the job site.
		if (unemployed && villager.getBrain().hasMemoryValue(MemoryModuleType.POTENTIAL_JOB_SITE)) {
			villager.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);
			villager.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
		}
		boolean worker = isWorker(villager);
		if (worker) {
			StuckWorkers.noteWalking(villager);
		}
		if (villager.tickCount % CHECK_INTERVAL != 0) {
			return;
		}
		String name = WorkerNames.ensureName(villager);
		if (worker) {
			StuckWorkers.check(level, villager, CHECK_INTERVAL);
		}
		if (!unemployed) {
			// Keep the station's note up to date (renamed worker, worker from before names existed)
			if (!villager.getVillagerData().profession().unwrapKey().map(JOBS::containsValue).orElse(false)) {
				return;
			}
			villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
				.filter(jobSite -> jobSite.dimension() == level.dimension())
				.flatMap(jobSite -> Workstations.find(level, jobSite.pos()))
				.filter(station -> !station.hasWorker(villager.getUUID(), name))
				.ifPresent(station -> station.setWorker(villager.getUUID(), name));
		}
	}

	/** Whether the villager has one of the jobs of this mod (carriers included). */
	private static boolean isWorker(Villager villager) {
		return Carriers.isCarrier(villager)
			|| villager.getVillagerData().profession().unwrapKey().map(JOBS::containsValue).orElse(false);
	}

	/**
	 * Gives the hired villager the workstation as its job site, with the matching profession.
	 * A job site it had before is given up. Returns why it did not work, or null.
	 */
	public static @Nullable Component assign(ServerLevel level, Villager villager, BlockPos station) {
		HiredVillager hired = (HiredVillager) villager;
		if (!hired.workingvillagers$isHired()) {
			return Component.translatable("message.workingvillagers.assign.not_hired");
		}
		if (villager.isBaby() || Carriers.isCarrier(villager)) {
			return Component.translatable("message.workingvillagers.assign.not_possible");
		}
		PoiManager poiManager = level.getPoiManager();
		Optional<ResourceKey<PoiType>> type = poiManager.getType(station).flatMap(Holder::unwrapKey).filter(JOBS::containsKey);
		if (type.isEmpty()) {
			return Component.translatable("message.workingvillagers.assign.not_possible");
		}
		Optional<GlobalPos> current = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
		if (current.isPresent() && current.get().pos().equals(station) && current.get().dimension() == level.dimension()) {
			return Component.translatable("message.workingvillagers.assign.already_here", villager.getName());
		}
		boolean free = poiManager.getInSquare(WorkerJobs::isJobSite, station, 0, PoiManager.Occupancy.HAS_SPACE)
			.anyMatch(poi -> poi.getPos().equals(station));
		if (!free) {
			return Component.translatable("message.workingvillagers.assign.occupied");
		}
		if (current.isPresent()) {
			release(level, villager);
		}
		Optional<BlockPos> taken = poiManager.take(WorkerJobs::isJobSite, (poi, pos) -> pos.equals(station), station, 1);
		if (taken.isEmpty()) {
			return Component.translatable("message.workingvillagers.assign.occupied");
		}
		String name = WorkerNames.ensureName(villager);
		villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(level.dimension(), station));
		Workstations.find(level, station).ifPresent(found -> found.setWorker(villager.getUUID(), name));
		villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), JOBS.get(type.get())));
		villager.refreshBrain(level);
		villager.makeSound(SoundEvents.VILLAGER_YES);
		level.broadcastEntityEvent(villager, (byte) 14);
		return null;
	}

	/**
	 * Takes the worker away from its workstation: it is unemployed again, but stays hired
	 * and can be given another workstation.
	 */
	public static void unassign(ServerLevel level, Villager villager) {
		release(level, villager);
		villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.NONE));
		villager.refreshBrain(level);
		villager.makeSound(SoundEvents.VILLAGER_NO);
	}

	/** Gives up the job site, so the workstation is free. */
	private static void release(ServerLevel level, Villager villager) {
		villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
			.filter(jobSite -> jobSite.dimension() == level.dimension())
			.ifPresent(jobSite -> Workstations.find(level, jobSite.pos()).ifPresent(Workstations.Station::clearWorker));
		villager.releasePoi(MemoryModuleType.JOB_SITE);
		villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
	}
}
