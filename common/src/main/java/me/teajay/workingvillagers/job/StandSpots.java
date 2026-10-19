package me.teajay.workingvillagers.job;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Finds the block a worker stands on while it uses a block: in front of it if the block has
 * a front (chests, barrels, the cutters), otherwise on the free side closest to the worker.
 */
final class StandSpots {
	private StandSpots() {
	}

	/**
	 * The front of a block, if it has one: the side its {@code facing} property points to.
	 */
	static @Nullable Direction front(BlockState state) {
		if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
		}
		if (state.hasProperty(BlockStateProperties.FACING)) {
			Direction facing = state.getValue(BlockStateProperties.FACING);
			return facing.getAxis().isHorizontal() ? facing : null;
		}
		return null;
	}

	/**
	 * Where to stand to use the block at {@code target}: the front if it is free, otherwise
	 * the closest free side. A spot one block lower also counts, for blocks standing on
	 * something.
	 */
	static Optional<BlockPos> find(ServerLevel level, BlockPos target, Villager villager) {
		Direction front = front(level.getBlockState(target));
		if (front != null) {
			for (BlockPos candidate : List.of(target.relative(front), target.relative(front).below())) {
				if (isStandable(level, candidate)) {
					return Optional.of(candidate);
				}
			}
		}
		List<BlockPos> candidates = new ArrayList<>();
		for (Direction side : Direction.Plane.HORIZONTAL) {
			candidates.add(target.relative(side));
			candidates.add(target.relative(side).below());
		}
		return candidates.stream()
			.filter(candidate -> isStandable(level, candidate))
			.min(Comparator.comparingDouble(candidate -> candidate.distToCenterSqr(villager.position())));
	}

	/**
	 * Where to stand next to any of the given blocks (e.g. the trunk of a tree).
	 */
	static Optional<BlockPos> findNextTo(ServerLevel level, List<BlockPos> targets, Villager villager) {
		List<BlockPos> candidates = new ArrayList<>();
		for (BlockPos target : targets) {
			for (Direction side : Direction.Plane.HORIZONTAL) {
				// also a block up or down, e.g. next to a tree on a slope
				for (int dy = -1; dy <= 1; dy++) {
					candidates.add(target.relative(side).above(dy));
				}
			}
		}
		return candidates.stream()
			.filter(candidate -> !targets.contains(candidate) && isStandable(level, candidate))
			.min(Comparator.comparingDouble(candidate -> candidate.distToCenterSqr(villager.position())));
	}

	/**
	 * Two blocks of room above ground. Ground is a block whose top is at least half high and
	 * not higher than a full block: also dirt paths, farmland and slabs, but not fences.
	 */
	static boolean isStandable(ServerLevel level, BlockPos pos) {
		BlockPos below = pos.below();
		VoxelShape ground = level.getBlockState(below).getCollisionShape(level, below);
		return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
			&& level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
			&& !ground.isEmpty() && ground.max(Direction.Axis.Y) >= 0.5 && ground.max(Direction.Axis.Y) <= 1.0;
	}

	/** Whether the villager stands on the spot (its feet are within the block, roughly centered). */
	static boolean isAt(Villager villager, BlockPos spot) {
		double dx = villager.getX() - (spot.getX() + 0.5);
		double dz = villager.getZ() - (spot.getZ() + 0.5);
		return dx * dx + dz * dz < 0.6 * 0.6 && Math.abs(villager.getY() - spot.getY()) < 1.0;
	}
}
