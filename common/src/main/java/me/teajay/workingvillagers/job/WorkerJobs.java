package me.teajay.workingvillagers.job;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
 * The jobs of hired villagers at their workstations (chopping block: lumberjack, woodcutter:
 * sawyer, stonecutter: stone sawyer, carpenter's bench: carpenter, composter: farmer, blast
 * furnace: smith, kiln: kiln operator, trading counter: shop assistant). A hired villager without a job looks for a free one itself (see
 * {@link FindWorkplace}); the player can also give it one in the station menu, and take it
 * away again there. Hired villagers do not take any vanilla job.
 *
 * <p>Vanilla job acquisition only looks at job sites in the {@code acquirable_job_site} tag,
 * which the blocks of this mod are not part of. The stonecutter,
 * the composter and the blast furnace are in the tag, ordinary villagers still become masons,
 * farmers and armorers there.
 */
public final class WorkerJobs {
	private static final Map<ResourceKey<PoiType>, ResourceKey<VillagerProfession>> JOBS = Map.of(
		WorkingVillagers.LUMBERJACK_POI, WorkingVillagers.LUMBERJACK_PROFESSION,
		WorkingVillagers.WOODCUTTER_POI, WorkingVillagers.WOODCUTTER_PROFESSION,
		WorkingVillagers.CARPENTER_POI, WorkingVillagers.CARPENTER_PROFESSION,
		PoiTypes.MASON, WorkingVillagers.STONECUTTER_PROFESSION,
		PoiTypes.FARMER, WorkingVillagers.FARMER_PROFESSION,
		PoiTypes.ARMORER, WorkingVillagers.SMITH_PROFESSION,
		WorkingVillagers.KILN_OPERATOR_POI, WorkingVillagers.KILN_OPERATOR_PROFESSION,
		WorkingVillagers.SHOP_ASSISTANT_POI, WorkingVillagers.SHOP_ASSISTANT_PROFESSION
	);
	private static final int CHECK_INTERVAL = 20;
	/** How long a worker taken away from a station does not take it again by itself. */
	private static final long LEFT_TICKS = 24000L;
	/** The station each worker was last taken away from, and until when it avoids it. Not saved. */
	private static final Map<UUID, Left> LEFT = new HashMap<>();

	private record Left(GlobalPos station, long until) {
	}

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
		// A hired villager does not take ordinary village jobs (a lectern, a smithing table),
		// only the ones of this mod; it gets their profession when it arrives (FindWorkplace).
		if (unemployed && villager.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE)
			.filter(site -> site.dimension() != level.dimension() || !isJobSite(level, site.pos()) || isLeftBy(level, villager, site.pos()))
			.isPresent()) {
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
			WorkerStatus.complain(level, villager, CHECK_INTERVAL);
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
		// A job site it is walking to is given up, also if it is this one
		villager.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);
		villager.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
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
		takeJob(level, villager, station);
		return null;
	}

	/**
	 * The villager takes the job at the workstation, whose job site it has already taken
	 * from the POI manager: job site memory, the profession of this mod, the note.
	 */
	static void takeJob(ServerLevel level, Villager villager, BlockPos station) {
		Optional<ResourceKey<VillagerProfession>> profession = level.getPoiManager().getType(station)
			.flatMap(Holder::unwrapKey)
			.map(JOBS::get);
		villager.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
		if (profession.isEmpty()) {
			level.getPoiManager().release(station);
			return;
		}
		LEFT.remove(villager.getUUID());
		String name = WorkerNames.ensureName(villager);
		villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(level.dimension(), station));
		Workstations.find(level, station).ifPresent(found -> found.setWorker(villager.getUUID(), name));
		villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), profession.get()));
		villager.refreshBrain(level);
		villager.makeSound(SoundEvents.VILLAGER_YES);
		level.broadcastEntityEvent(villager, (byte) 14);
	}

	private static boolean isJobSite(ServerLevel level, BlockPos pos) {
		return level.getPoiManager().getType(pos).map(WorkerJobs::isJobSite).orElse(false);
	}

	/** Whether the worker was taken away from this station not long ago, so it does not go back. */
	static boolean isLeftBy(ServerLevel level, Villager villager, BlockPos station) {
		Left left = LEFT.get(villager.getUUID());
		if (left == null) {
			return false;
		}
		if (level.getGameTime() > left.until()) {
			LEFT.remove(villager.getUUID());
			return false;
		}
		return left.station().equals(GlobalPos.of(level.dimension(), station));
	}

	/**
	 * Takes the worker away from its workstation: it is unemployed again, but stays hired
	 * and can be given another workstation.
	 */
	public static void unassign(ServerLevel level, Villager villager) {
		villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
			.ifPresent(site -> LEFT.put(villager.getUUID(), new Left(site, level.getGameTime() + LEFT_TICKS)));
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
