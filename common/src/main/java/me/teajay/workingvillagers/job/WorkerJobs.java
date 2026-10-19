package me.teajay.workingvillagers.job;

import java.util.HashMap;
import java.util.List;
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
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The jobs of hired villagers at their workstations (chopping block: lumberjack, woodcutter:
 * sawyer, stonecutter: stone sawyer, carpenter's bench: carpenter, composter: farmer, blast
 * furnace: smith, pit stake: pit digger, kiln: kiln operator, trading counter: shop assistant,
 * mining bench: miner, loom: shepherd, smoker: butcher, barrel: fisher, fletching table:
 * fletcher, target: guard, brewing stand: apothecary, cauldron: tanner, grindstone: weaponsmith, lectern: librarian). A hired villager without a job looks for a free one itself (see
 * {@link FindWorkplace}); the player can also give it one in the station menu, and take it
 * away again there. Hired villagers do not take any vanilla job.
 *
 * <p>Vanilla job acquisition only looks at job sites in the {@code acquirable_job_site} tag,
 * which the blocks of this mod are not part of. The stonecutter, the composter, the blast
 * furnace, the smithing table, the loom, the smoker, the barrel, the fletching table, the
 * brewing stand, the cauldron, the grindstone and the lectern are in the tag, ordinary
 * villagers still become masons, farmers, armorers, toolsmiths, shepherds, butchers,
 * fishermen, fletchers, clerics, leatherworkers, weaponsmiths and librarians there.
 */
public final class WorkerJobs {
	private static final Map<ResourceKey<PoiType>, ResourceKey<VillagerProfession>> JOBS = Map.ofEntries(
		Map.entry(WorkingVillagers.LUMBERJACK_POI, WorkingVillagers.LUMBERJACK_PROFESSION),
		Map.entry(WorkingVillagers.WOODCUTTER_POI, WorkingVillagers.WOODCUTTER_PROFESSION),
		Map.entry(WorkingVillagers.CARPENTER_POI, WorkingVillagers.CARPENTER_PROFESSION),
		Map.entry(PoiTypes.MASON, WorkingVillagers.STONECUTTER_PROFESSION),
		Map.entry(PoiTypes.FARMER, WorkingVillagers.FARMER_PROFESSION),
		Map.entry(PoiTypes.ARMORER, WorkingVillagers.SMITH_PROFESSION),
		Map.entry(PoiTypes.TOOLSMITH, WorkingVillagers.SMITH_PROFESSION),
		Map.entry(PoiTypes.SHEPHERD, WorkingVillagers.SHEPHERD_PROFESSION),
		Map.entry(PoiTypes.BUTCHER, WorkingVillagers.BUTCHER_PROFESSION),
		Map.entry(PoiTypes.FISHERMAN, WorkingVillagers.FISHER_PROFESSION),
		Map.entry(PoiTypes.FLETCHER, WorkingVillagers.FLETCHER_PROFESSION),
		Map.entry(PoiTypes.CLERIC, WorkingVillagers.APOTHECARY_PROFESSION),
		Map.entry(PoiTypes.LEATHERWORKER, WorkingVillagers.TANNER_PROFESSION),
		Map.entry(PoiTypes.WEAPONSMITH, WorkingVillagers.WEAPONSMITH_PROFESSION),
		Map.entry(PoiTypes.LIBRARIAN, WorkingVillagers.LIBRARIAN_PROFESSION),
		Map.entry(WorkingVillagers.GUARD_POI, WorkingVillagers.GUARD_PROFESSION),
		Map.entry(WorkingVillagers.PIT_DIGGER_POI, WorkingVillagers.PIT_DIGGER_PROFESSION),
		Map.entry(WorkingVillagers.KILN_OPERATOR_POI, WorkingVillagers.KILN_OPERATOR_PROFESSION),
		Map.entry(WorkingVillagers.SHOP_ASSISTANT_POI, WorkingVillagers.SHOP_ASSISTANT_PROFESSION),
		Map.entry(WorkingVillagers.MINER_POI, WorkingVillagers.MINER_PROFESSION)
	);
	private static final int CHECK_INTERVAL = 20;
	/** How close to its workstation a worker fills up its trades. */
	private static final double RESTOCK_DISTANCE = 4.0;
	/** How far from a station a villager that holds it is looked for before its ticket is given back. */
	private static final double LOST_TICKET_RANGE = 64.0;
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
		if (!hired.workingvillagers$isHired()) {
			if (villager.tickCount % CHECK_INTERVAL == 0) {
				keepOffSetUpStation(level, villager);
			}
			return;
		}
		if (villager.isBaby()) {
			return;
		}
		boolean unemployed = villager.getVillagerData().profession().is(VillagerProfession.NONE);
		// A hired villager does not take ordinary village jobs (a lectern, a smithing table),
		// only the ones of this mod; it gets their profession when it arrives (FindWorkplace).
		if (unemployed && villager.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE)
			.filter(site -> site.dimension() != level.dimension() || !isJobSite(level, site.pos()) || isLeftBy(level, villager, site.pos()))
			.isPresent()) {
			giveUpPotentialJobSite(villager);
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
			restock(level, villager);
			Uniforms.equip(level, villager);
			WorkerSupplies.storeCarriedWage(level, villager);
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

	/**
	 * A worker that trades (see {@link TradeFamilies}) fills up its trades at its workstation
	 * during work hours, like a vanilla villager at its job site (whose work it does not do).
	 */
	private static void restock(ServerLevel level, Villager villager) {
		if (!villager.getBrain().isActive(Activity.WORK) || !villager.shouldRestock(level)) {
			return;
		}
		villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
			.filter(site -> site.dimension() == level.dimension() && site.pos().closerToCenterThan(villager.position(), RESTOCK_DISTANCE))
			.ifPresent(site -> villager.restock());
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
		giveUpPotentialJobSite(villager);
		boolean free = poiManager.getInSquare(WorkerJobs::isJobSite, station, 0, PoiManager.Occupancy.HAS_SPACE)
			.anyMatch(poi -> poi.getPos().equals(station));
		if (!free) {
			Component problem = displaceOrdinaryVillager(level, station);
			if (problem != null) {
				return problem;
			}
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
	 * Why a villager with a vanilla job cannot be hired and keep working at its job site:
	 * a nitwit, no job site, or no job of this mod there (a librarian). Null if it can (see
	 * {@link #keepJobSite}); one that traded keeps its trades (see {@link TradeFamilies}).
	 */
	public static @Nullable Component checkKeepJobSite(ServerLevel level, Villager villager) {
		Holder<VillagerProfession> profession = villager.getVillagerData().profession();
		if (profession.is(VillagerProfession.NITWIT)) {
			return Component.translatable("message.workingvillagers.contract.nitwit");
		}
		Optional<GlobalPos> site = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).filter(pos -> pos.dimension() == level.dimension());
		boolean job = site.flatMap(pos -> level.getPoiManager().getType(pos.pos())).flatMap(Holder::unwrapKey).filter(JOBS::containsKey).isPresent();
		if (!job) {
			return Component.translatable("message.workingvillagers.contract.no_job", profession.value().name());
		}
		return null;
	}

	/**
	 * A villager with a vanilla job that was just hired (see {@link #checkKeepJobSite}) stays
	 * at its job site, as the worker of this mod there: the farmer at its composter, the
	 * mason as stone sawyer at its stonecutter, ...
	 */
	public static void keepJobSite(ServerLevel level, Villager villager) {
		villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).ifPresent(site -> takeJob(level, villager, site.pos()));
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

	/**
	 * Gives up the job site the villager is walking to, so the workstation is free again.
	 * Vanilla's {@code releasePoi} only frees job sites in the {@code acquirable_job_site} tag,
	 * which the workstations of this mod are not part of: they stayed taken for good.
	 */
	public static void giveUpPotentialJobSite(Villager villager) {
		villager.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).ifPresent(site -> {
			ServerLevel level = villager.level() instanceof ServerLevel serverLevel ? serverLevel.getServer().getLevel(site.dimension()) : null;
			if (level != null && level.getPoiManager().getType(site.pos()).isPresent()) {
				level.getPoiManager().release(site.pos());
				level.debugSynchronizers().updatePoi(site.pos());
			}
		});
		villager.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
	}

	/**
	 * Frees workstations of this mod around the position that count as taken although
	 * nobody works there or walks there: tickets lost by earlier versions (see
	 * {@link #giveUpPotentialJobSite}). A station keeps its ticket while it has a worker
	 * (also one that is not loaded) or a villager nearby has it as its job site or is on its
	 * way there. Vanilla job sites (stonecutter, composter, blast furnace) are left alone,
	 * their tickets are given back by vanilla.
	 */
	static void freeLostJobSites(ServerLevel level, BlockPos center, int range) {
		PoiManager poiManager = level.getPoiManager();
		List<BlockPos> taken = poiManager.getInRange(
				poi -> poi.unwrapKey().map(key -> JOBS.containsKey(key) && key.identifier().getNamespace().equals(WorkingVillagers.MOD_ID)).orElse(false),
				center, range, PoiManager.Occupancy.IS_OCCUPIED)
			.map(PoiRecord::getPos)
			.toList();
		for (BlockPos pos : taken) {
			Optional<Workstations.Station> station = Workstations.find(level, pos);
			if (station.isEmpty() || station.get().worker() != null) {
				continue;
			}
			GlobalPos site = GlobalPos.of(level.dimension(), pos);
			boolean claimed = !level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(LOST_TICKET_RANGE), villager ->
				villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).filter(site::equals).isPresent()
					|| villager.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).filter(site::equals).isPresent()
			).isEmpty();
			if (!claimed) {
				poiManager.release(pos);
				level.debugSynchronizers().updatePoi(pos);
			}
		}
	}

	/**
	 * An ordinary villager does not take a vanilla workstation (stonecutter, composter, blast
	 * furnace) that is set up as a workstation of this mod (chests, order or worker): the job
	 * site it is walking to is given up again.
	 */
	private static void keepOffSetUpStation(ServerLevel level, Villager villager) {
		Optional<GlobalPos> potential = villager.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
		if (potential.isPresent() && potential.get().dimension() == level.dimension() && isJobSite(level, potential.get().pos())
			&& Workstations.findSetUp(level, potential.get().pos()).isPresent()) {
			giveUpPotentialJobSite(villager);
		}
	}

	/**
	 * Makes room at a vanilla workstation for a hired worker: an ordinary villager that
	 * works there and has never traded loses its job, like vanilla villagers that lose their
	 * job site. One that has traded keeps it. Returns why the station stays taken, or null.
	 */
	private static @Nullable Component displaceOrdinaryVillager(ServerLevel level, BlockPos station) {
		GlobalPos site = GlobalPos.of(level.dimension(), station);
		List<Villager> holders = level.getEntitiesOfClass(Villager.class, new AABB(station).inflate(LOST_TICKET_RANGE), villager ->
			villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).filter(site::equals).isPresent());
		if (holders.isEmpty()) {
			return Component.translatable("message.workingvillagers.assign.occupied");
		}
		Villager holder = holders.getFirst();
		if (((HiredVillager) holder).workingvillagers$isHired()) {
			return Component.translatable("message.workingvillagers.assign.occupied");
		}
		if (holder.getVillagerXp() > 0) {
			return Component.translatable("message.workingvillagers.assign.occupied_trader", holder.getName());
		}
		holder.releasePoi(MemoryModuleType.JOB_SITE);
		holder.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
		holder.setVillagerData(holder.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.NONE));
		holder.refreshBrain(level);
		level.broadcastEntityEvent(holder, (byte) 13);
		return null;
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
