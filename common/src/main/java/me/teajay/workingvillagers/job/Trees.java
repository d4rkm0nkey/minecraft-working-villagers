package me.teajay.workingvillagers.job;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Consumer;
import me.teajay.workingvillagers.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Finds natural trees: logs standing on dirt that are connected to non-persistent
 * (that is, not player placed) leaves. Log builds made by players are left alone.
 */
public final class Trees {
	private static final int SEARCH_BELOW = 6;
	private static final int SEARCH_ABOVE = 10;
	/** How far a tree may spread from its base. */
	private static final int MAX_TREE_RADIUS = 8;
	private static final int MAX_TREE_HEIGHT = 40;
	private static final int MAX_LOGS = 256;
	private static final int MIN_LEAVES = 3;
	/** How many connected logs (of several trees) are looked at to find a tree's own logs. */
	private static final int MAX_CLUSTER_LOGS = 4096;
	private static final int CLUSTER_BELOW = 4;

	private Trees() {
	}

	/**
	 * A tree to fell: all its logs, the lowest ones first.
	 */
	public record Tree(BlockPos base, List<BlockPos> logs) {
		public List<BlockPos> bottomLogs() {
			int minY = this.logs.getFirst().getY();
			return this.logs.stream().filter(pos -> pos.getY() == minY).toList();
		}
	}

	public static Optional<Tree> findNearest(ServerLevel level, BlockPos center, Map<BlockPos, Long> ignored) {
		List<BlockPos> bases = new ArrayList<>();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int radius = Config.TREE_SEARCH_RADIUS.get();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				for (int dy = -SEARCH_BELOW; dy <= SEARCH_ABOVE; dy++) {
					pos.setWithOffset(center, dx, dy, dz);
					if (isBase(level, pos) && !ignored.containsKey(pos)) {
						bases.add(pos.immutable());
					}
				}
			}
		}
		bases.sort(Comparator.comparingDouble(base -> base.distSqr(center)));

		Set<BlockPos> checked = new HashSet<>();
		for (BlockPos base : bases) {
			if (checked.contains(base)) {
				continue;
			}
			Tree tree = collect(level, base);
			if (tree != null) {
				return Optional.of(tree);
			}
			// A rejected structure: skip its other bases (e.g. the rest of a 2x2 trunk)
			checked.add(base);
		}
		return Optional.empty();
	}

	public static boolean isBase(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos).is(BlockTags.LOGS) && level.getBlockState(pos.below()).is(BlockTags.DIRT);
	}

	/**
	 * Collects the tree growing from {@code base}, or null if it is not a natural tree.
	 *
	 * <p>Trees planted close together touch each other with their logs, so the connected
	 * logs may be several trees. Every log belongs to the trunk it is closest to (counted
	 * in steps from log to log), and only the logs of the trunk of {@code base} are its tree.
	 * The logs of a 2x2 trunk next to each other on the ground are one trunk.
	 */
	public static @Nullable Tree collect(ServerLevel level, BlockPos base) {
		if (!isBase(level, base)) {
			return null;
		}
		Set<BlockPos> cluster = connectedLogs(level, base);
		if (cluster == null) {
			return null;
		}
		Map<BlockPos, Integer> trunks = trunks(level, cluster);
		int own = trunks.get(base);

		// Grow all trunks at once, a log goes to the trunk that reaches it first. Steps
		// straight up or sideways are shorter than diagonal ones, so a trunk keeps its own
		// logs even where a neighbor touches it diagonally.
		Map<BlockPos, Integer> owner = new HashMap<>();
		Map<BlockPos, Double> distance = new HashMap<>();
		PriorityQueue<Step> queue = new PriorityQueue<>(Comparator.comparingDouble(Step::distance));
		trunks.forEach((foot, trunk) -> queue.add(new Step(foot, trunk, 0.0)));
		while (!queue.isEmpty()) {
			Step step = queue.poll();
			if (owner.containsKey(step.pos())) {
				continue;
			}
			owner.put(step.pos(), step.trunk());
			forEachNeighbor(step.pos(), next -> {
				if (cluster.contains(next) && !owner.containsKey(next)) {
					double cost = step.distance() + Math.sqrt(next.distSqr(step.pos()));
					if (cost < distance.getOrDefault(next, Double.MAX_VALUE)) {
						distance.put(next, cost);
						queue.add(new Step(next, step.trunk(), cost));
					}
				}
			});
		}

		Set<BlockPos> logs = new HashSet<>();
		owner.forEach((pos, trunk) -> {
			if (trunk == own && withinTree(base, pos)) {
				logs.add(pos);
			}
		});
		if (logs.size() > MAX_LOGS || countLeaves(level, logs) < MIN_LEAVES) {
			return null;
		}
		List<BlockPos> sorted = new ArrayList<>(logs);
		sorted.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY).thenComparingDouble(pos -> pos.distSqr(base)));
		return new Tree(base, List.copyOf(sorted));
	}

	/** A log reached from a trunk, on the way to find the closest trunk of every log. */
	private record Step(BlockPos pos, int trunk, double distance) {
	}

	/** All logs connected to the base (also diagonally), null if there are too many. */
	private static @Nullable Set<BlockPos> connectedLogs(ServerLevel level, BlockPos base) {
		Set<BlockPos> logs = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		logs.add(base);
		queue.add(base);
		boolean[] tooMany = {false};
		while (!queue.isEmpty() && !tooMany[0]) {
			BlockPos current = queue.poll();
			forEachNeighbor(current, next -> {
				if (!logs.contains(next) && withinCluster(base, next) && level.getBlockState(next).is(BlockTags.LOGS)) {
					if (logs.size() >= MAX_CLUSTER_LOGS) {
						tooMany[0] = true;
						return;
					}
					logs.add(next);
					queue.add(next);
				}
			});
		}
		return tooMany[0] ? null : logs;
	}

	/**
	 * The trunks among the logs: logs standing on dirt, numbered, with the feet of a 2x2
	 * trunk (side by side at the same height) sharing a number.
	 */
	private static Map<BlockPos, Integer> trunks(ServerLevel level, Set<BlockPos> logs) {
		Map<BlockPos, Integer> trunks = new HashMap<>();
		int next = 0;
		for (BlockPos log : logs) {
			if (trunks.containsKey(log) || !isBase(level, log)) {
				continue;
			}
			int id = next++;
			ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			trunks.put(log, id);
			queue.add(log);
			while (!queue.isEmpty()) {
				BlockPos current = queue.poll();
				for (Direction direction : Direction.Plane.HORIZONTAL) {
					BlockPos side = current.relative(direction);
					if (logs.contains(side) && !trunks.containsKey(side) && isBase(level, side)) {
						trunks.put(side, id);
						queue.add(side);
					}
				}
			}
		}
		return trunks;
	}

	private static int countLeaves(ServerLevel level, Set<BlockPos> logs) {
		Set<BlockPos> leaves = new HashSet<>();
		for (BlockPos log : logs) {
			forEachNeighbor(log, next -> {
				if (!logs.contains(next) && isNaturalLeaves(level.getBlockState(next))) {
					leaves.add(next);
				}
			});
		}
		return leaves.size();
	}

	/** The 26 blocks around a position. */
	private static void forEachNeighbor(BlockPos pos, Consumer<BlockPos> action) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx != 0 || dy != 0 || dz != 0) {
						action.accept(pos.offset(dx, dy, dz));
					}
				}
			}
		}
	}

	private static boolean withinTree(BlockPos base, BlockPos pos) {
		int dy = pos.getY() - base.getY();
		return dy >= 0 && dy <= MAX_TREE_HEIGHT
			&& Math.abs(pos.getX() - base.getX()) <= MAX_TREE_RADIUS
			&& Math.abs(pos.getZ() - base.getZ()) <= MAX_TREE_RADIUS;
	}

	/** Where neighboring trees are looked at: wider than a tree, also a bit lower (slopes). */
	private static boolean withinCluster(BlockPos base, BlockPos pos) {
		int dy = pos.getY() - base.getY();
		return dy >= -CLUSTER_BELOW && dy <= MAX_TREE_HEIGHT
			&& Math.abs(pos.getX() - base.getX()) <= 2 * MAX_TREE_RADIUS
			&& Math.abs(pos.getZ() - base.getZ()) <= 2 * MAX_TREE_RADIUS;
	}

	private static boolean isNaturalLeaves(BlockState state) {
		return state.is(BlockTags.LEAVES) && state.hasProperty(LeavesBlock.PERSISTENT) && !state.getValue(LeavesBlock.PERSISTENT);
	}

	/**
	 * The sapling that grows the tree a log belongs to, e.g. oak_log to oak_sapling.
	 */
	public static Optional<Block> saplingFor(BlockState log) {
		Identifier id = BuiltInRegistries.BLOCK.getKey(log.getBlock());
		String path = id.getPath();
		if (path.equals("mangrove_log")) {
			return BuiltInRegistries.BLOCK.getOptional(id.withPath("mangrove_propagule"));
		}
		if (!path.endsWith("_log") || path.startsWith("stripped_")) {
			return Optional.empty();
		}
		return BuiltInRegistries.BLOCK.getOptional(id.withPath(path.substring(0, path.length() - "_log".length()) + "_sapling"))
			.filter(sapling -> sapling.defaultBlockState().is(BlockTags.SAPLINGS));
	}
}
