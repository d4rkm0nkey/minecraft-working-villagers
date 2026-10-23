package me.teajay.workingvillagers.station;

import java.util.UUID;
import me.teajay.workingvillagers.Config;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * How big a pit the pit digger may dig: the blocks of ground inside the ring and the depth.
 * Both grow with its level, from the novice's to the master's (see {@link Config}); the side
 * of the pit grows evenly, so 8×8 to 16×16 by default.
 */
public record PitLimits(int area, int depth) {
	private static final int NOVICE = 1;
	private static final int MASTER = 5;

	public static PitLimits forLevel(int villagerLevel) {
		float progress = Math.clamp((villagerLevel - NOVICE) / (float) (MASTER - NOVICE), 0.0F, 1.0F);
		double noviceSide = Math.sqrt(Config.PIT_NOVICE_AREA.get());
		double side = noviceSide + (Math.sqrt(Config.PIT_MASTER_AREA.get()) - noviceSide) * progress;
		int noviceDepth = Config.PIT_NOVICE_DEPTH.get();
		int depth = Math.round(noviceDepth + (Config.PIT_MASTER_DEPTH.get() - noviceDepth) * progress);
		return new PitLimits((int) Math.round(side * side), depth);
	}

	public static PitLimits novice() {
		return forLevel(NOVICE);
	}

	public static PitLimits of(Villager villager) {
		return forLevel(villager.getVillagerData().level());
	}

	/** The limits of the pit digger at the stake; a novice's while there is none (or it is not loaded). */
	public static PitLimits at(ServerLevel level, Workstations.Station station) {
		UUID worker = station.worker();
		return worker != null && level.getEntity(worker) instanceof Villager villager ? of(villager) : novice();
	}

	/** The deepest any pit digger digs: the depth set at a stake goes up to this. */
	public static int maxDepth() {
		return Math.max(Config.PIT_NOVICE_DEPTH.get(), Config.PIT_MASTER_DEPTH.get());
	}
}
