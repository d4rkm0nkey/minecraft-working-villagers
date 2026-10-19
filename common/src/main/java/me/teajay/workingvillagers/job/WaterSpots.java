package me.teajay.workingvillagers.job;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.npc.villager.Villager;
import org.jspecify.annotations.Nullable;

/** Where a worker stands to fill a bottle or a bucket: on a bank next to a water source. */
final class WaterSpots {
	private WaterSpots() {
	}

	/** A spot next to water (a source block) around the station, the closest to the villager. */
	static Optional<BlockPos> findShore(ServerLevel level, BlockPos stand, int radius, Villager villager) {
		List<BlockPos> spots = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(stand.offset(-radius, -3, -radius), stand.offset(radius, 2, radius))) {
			if (!isWater(level, pos)) {
				continue;
			}
			for (Direction side : Direction.Plane.HORIZONTAL) {
				for (BlockPos candidate : List.of(pos.relative(side), pos.relative(side).above())) {
					if (StandSpots.isStandable(level, candidate)) {
						spots.add(candidate.immutable());
					}
				}
			}
		}
		return spots.stream().min(Comparator.comparingDouble(spot -> spot.distToCenterSqr(villager.position())));
	}

	static @Nullable BlockPos closestWater(ServerLevel level, BlockPos spot) {
		for (Direction side : Direction.Plane.HORIZONTAL) {
			for (BlockPos pos : List.of(spot.relative(side), spot.relative(side).below())) {
				if (isWater(level, pos)) {
					return pos;
				}
			}
		}
		return null;
	}

	static boolean isWater(ServerLevel level, BlockPos pos) {
		return level.getFluidState(pos).is(FluidTags.WATER) && level.getFluidState(pos).isSource();
	}
}
