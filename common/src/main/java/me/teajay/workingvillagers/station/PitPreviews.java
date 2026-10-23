package me.teajay.workingvillagers.station;

import com.mojang.datafixers.util.Either;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.network.PitPreviewPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.item.Items;

/**
 * While a player holds string, the client outlines the pit at every pit stake nearby, or the
 * strings with the problem in red (see {@link PitPreviewPayload}). The server checks the
 * rings like the stake does and sends the result when it changes. When the result at a stake
 * changes, particles show it where the player lays the string: green on a new pit, angry
 * ones on the strings with the problem.
 */
public final class PitPreviews {
	/** How far (horizontally, and up or down) a stake may be from the player. */
	public static final int RANGE = 24;
	private static final int INTERVAL = 10;
	/** What each player was sent last; a new player object (after joining, respawning) is sent its preview anew. */
	private static final Map<ServerPlayer, PitPreviewPayload> SENT = new WeakHashMap<>();

	private PitPreviews() {
	}

	public static void tick(ServerPlayer player) {
		if (player.tickCount % INTERVAL != 0) {
			return;
		}
		ServerLevel level = player.level();
		PitPreviewPayload payload = holdsString(player) ? preview(level, player.blockPosition()) : PitPreviewPayload.EMPTY;
		PitPreviewPayload last = SENT.get(player);
		if (payload.equals(last)) {
			return;
		}
		if (last != null) {
			showChanges(level, player, last, payload);
		}
		SENT.put(player, payload);
		WorkingVillagers.platform().sendToClient(player, payload);
	}

	private static boolean holdsString(ServerPlayer player) {
		return player.getMainHandItem().is(Items.STRING) || player.getOffhandItem().is(Items.STRING);
	}

	/** The pits (or what is wrong with the rings) at the stakes in range of the position. */
	public static PitPreviewPayload preview(ServerLevel level, BlockPos center) {
		List<PitPreviewPayload.Stake> stakes = new ArrayList<>();
		level.getPoiManager()
			.getInSquare(poi -> poi.is(WorkingVillagers.PIT_DIGGER_POI), center, RANGE, PoiManager.Occupancy.ANY)
			.map(record -> record.getPos())
			.filter(stake -> Math.abs(stake.getY() - center.getY()) <= RANGE)
			.sorted()
			.forEach(stake -> stakes.add(at(level, stake)));
		return new PitPreviewPayload(List.copyOf(stakes));
	}

	private static PitPreviewPayload.Stake at(ServerLevel level, BlockPos stake) {
		Optional<Workstations.Station> station = Workstations.find(level, stake);
		PitLimits limits = station.map(found -> PitLimits.at(level, found)).orElseGet(PitLimits::novice);
		Either<Pit, Pit.Problem> found = Pit.fromStake(level, stake, limits.area());
		Optional<Pit> pit = found.left();
		if (pit.isEmpty()) {
			Pit.Problem problem = found.right().orElseThrow();
			// A pit picked in an earlier version, whose ring does not touch the stake, still counts
			Pit earlier = station.map(Workstations.Station::pit).orElse(null);
			if (earlier != null && problem.strings().isEmpty()) {
				pit = Optional.of(earlier);
			} else {
				return new PitPreviewPayload.Stake(stake, problem.strings(), problem.spots(), List.of(), 0);
			}
		}
		int depth = Math.min(station.map(Workstations.Station::depth).orElse(Pit.DEFAULT_DEPTH), limits.depth());
		return new PitPreviewPayload.Stake(stake, pit.get().ring(), List.of(), pit.get().inside(), pit.get().bottom(level, depth));
	}

	/** Particles at the stakes whose pit or problem changed since the last preview. */
	private static void showChanges(ServerLevel level, ServerPlayer player, PitPreviewPayload last, PitPreviewPayload now) {
		Map<BlockPos, PitPreviewPayload.Stake> before = new HashMap<>();
		last.stakes().forEach(stake -> before.put(stake.stake(), stake));
		for (PitPreviewPayload.Stake stake : now.stakes()) {
			PitPreviewPayload.Stake previous = before.get(stake.stake());
			if (previous == null || previous.equals(stake)) {
				continue;
			}
			if (stake.isPit() && !previous.isPit()) {
				particles(level, player, ParticleTypes.HAPPY_VILLAGER, stake.strings());
			} else if (!stake.spots().isEmpty() && !stake.spots().equals(previous.spots())) {
				particles(level, player, ParticleTypes.ANGRY_VILLAGER, stake.spots());
			}
		}
	}

	private static void particles(ServerLevel level, ServerPlayer player, ParticleOptions particle, List<BlockPos> strings) {
		// At most a few dozen, spread over the ring
		int step = Math.max(1, strings.size() / 32);
		for (int i = 0; i < strings.size(); i += step) {
			BlockPos string = strings.get(i);
			level.sendParticles(player, particle, false, false, string.getX() + 0.5, string.getY() + 0.4, string.getZ() + 0.5, 1, 0.15, 0.1, 0.15, 0.0);
		}
	}
}
