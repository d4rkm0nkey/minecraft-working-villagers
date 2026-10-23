package me.teajay.workingvillagers.station;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import me.teajay.workingvillagers.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

/**
 * A pit for the pit digger, marked by the player with a ring of string on the ground. The
 * pit is everything inside the ring (seen from above), dug down to {@code depth} blocks below
 * the highest string; the blocks under the strings stay as the rim.
 *
 * @param ring the strings around the pit; the pit digger waits while one of them is gone
 * @param inside the columns inside the ring (only x and z count)
 * @param exit the string next to the ladder, where the pit digger steps out of the pit
 * @param ladder the top of the ladder: the column inside the ring next to {@code exit}, one
 *     block below that string
 * @param top the height of the highest string; the depth counts from there, so the strings
 *     cannot make the pit deeper after it was picked
 */
public record Pit(List<BlockPos> ring, List<BlockPos> inside, BlockPos exit, BlockPos ladder, int top) {
	public static final int DEFAULT_DEPTH = 4;
	/** Bedrock and the bottom of the world stay out of reach. */
	private static final int ABOVE_BOTTOM = 5;
	/** More strings than this are not followed, the ring is too big anyway. */
	private static final int MAX_STRINGS = 1024;

	public static final Codec<Pit> CODEC = RecordCodecBuilder.create(i -> i.group(
		BlockPos.CODEC.listOf().fieldOf("ring").forGetter(Pit::ring),
		BlockPos.CODEC.listOf().fieldOf("inside").forGetter(Pit::inside),
		BlockPos.CODEC.fieldOf("exit").forGetter(Pit::exit),
		BlockPos.CODEC.fieldOf("ladder").forGetter(Pit::ladder),
		Codec.INT.fieldOf("top").forGetter(Pit::top)
	).apply(i, Pit::new));

	/** The side of the ladder column the ladder hangs on: towards the exit. */
	public Direction ladderFacing() {
		return Direction.getApproximateNearest(this.ladder.getX() - this.exit.getX(), 0, this.ladder.getZ() - this.exit.getZ());
	}

	/** The lowest layer that is dug; the block below stays as the floor of the pit. */
	public int bottom(ServerLevel level, int depth) {
		return Math.max(this.top - depth, level.getMinY() + ABOVE_BOTTOM);
	}

	/** Whether the column (x and z of the position) is inside the ring. */
	public boolean contains(BlockPos pos) {
		for (BlockPos column : this.inside) {
			if (column.getX() == pos.getX() && column.getZ() == pos.getZ()) {
				return true;
			}
		}
		return false;
	}

	/** Whether every string of the ring is still where it was when the pit was picked. */
	public boolean isIntact(ServerLevel level) {
		for (BlockPos string : this.ring) {
			if (level.isLoaded(string) && !level.getBlockState(string).is(Blocks.TRIPWIRE)) {
				return false;
			}
		}
		return true;
	}

	/** Whether all strings are in loaded chunks. */
	public boolean isLoaded(ServerLevel level) {
		return this.ring.stream().allMatch(level::isLoaded);
	}

	/** How wide and long the pit is, seen from above. */
	public int width() {
		return this.inside.stream().mapToInt(BlockPos::getX).max().orElse(0) - this.inside.stream().mapToInt(BlockPos::getX).min().orElse(0) + 1;
	}

	public int length() {
		return this.inside.stream().mapToInt(BlockPos::getZ).max().orElse(0) - this.inside.stream().mapToInt(BlockPos::getZ).min().orElse(0) + 1;
	}

	/** The same pit with the ladder at the string the player clicked, or why that is not possible. */
	public Either<Pit, Component> withLadderAt(BlockPos clicked, ServerLevel level) {
		BlockPos string = stringAt(level, clicked);
		if (string == null || !this.ring.contains(string)) {
			return Either.right(Component.translatable("message.workingvillagers.pit.ladder_not_ring"));
		}
		Set<Long> inside = columns(this.inside);
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockPos column = string.relative(side);
			if (inside.contains(column(column))) {
				return Either.left(new Pit(this.ring, this.inside, string, column.below(), this.top));
			}
		}
		return Either.right(Component.translatable("message.workingvillagers.pit.ladder_not_ring"));
	}

	/**
	 * Why the strings at a stake are no pit: the message, the strings that are the problem
	 * (shown red, may be empty) and all strings that touch the stake.
	 */
	public record Problem(Component message, List<BlockPos> spots, List<BlockPos> strings) {
		private static Problem of(Component message, List<BlockPos> spots, Map<Long, BlockPos> strings, BlockPos stake) {
			return new Problem(message, List.copyOf(spots), strings.values().stream().filter(string -> !string.equals(stake)).toList());
		}
	}

	/**
	 * The ring of string that touches the pit stake and the pit inside it, or why there is
	 * none. The stake may stand in the ring like a string, or right next to it. The pit may
	 * have at most {@code maxArea} blocks of ground (see {@link PitLimits}).
	 */
	public static Either<Pit, Problem> fromStake(ServerLevel level, BlockPos stake, int maxArea) {
		Map<Long, BlockPos> strings = new LinkedHashMap<>();
		strings.put(column(stake), stake.immutable());
		return trace(level, strings, stake.immutable(), maxArea);
	}

	private static boolean touchesString(ServerLevel level, BlockPos stake) {
		for (Direction side : Direction.Plane.HORIZONTAL) {
			for (int dy = -1; dy <= 1; dy++) {
				if (level.getBlockState(stake.relative(side).above(dy)).is(Blocks.TRIPWIRE)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Follows the strings from the pit stake (the start) and finds the pit they enclose. The
	 * stake closes the ring like a string but is no string of the ring itself.
	 */
	private static Either<Pit, Problem> trace(ServerLevel level, Map<Long, BlockPos> strings, BlockPos stake, int maxArea) {
		// The strings that touch each other (straight, maybe one block up or down), on ground
		Deque<BlockPos> open = new ArrayDeque<>(strings.values());
		while (!open.isEmpty()) {
			BlockPos string = open.poll();
			for (Direction side : Direction.Plane.HORIZONTAL) {
				for (int dy = -1; dy <= 1; dy++) {
					BlockPos next = string.relative(side).above(dy);
					if (!strings.containsKey(column(next)) && level.isLoaded(next) && level.getBlockState(next).is(Blocks.TRIPWIRE) && isOnGround(level, next)) {
						if (strings.size() >= MAX_STRINGS) {
							return Either.right(Problem.of(Component.translatable("message.workingvillagers.pit.too_big", "> " + maxArea, maxArea),
								List.copyOf(strings.values()), strings, stake));
						}
						strings.put(column(next), next);
						open.add(next);
					}
				}
			}
		}

		if (strings.size() == 1) {
			return Either.right(Problem.of(Component.translatable("message.workingvillagers.pit.no_ring"), List.of(), strings, stake));
		}
		List<BlockPos> inside = inside(strings.keySet());
		if (inside.isEmpty()) {
			BlockPos end = openEnd(strings, stake);
			return Either.right(end == null
				? Problem.of(Component.translatable("message.workingvillagers.pit.not_closed"), List.of(), strings, stake)
				: Problem.of(Component.translatable("message.workingvillagers.pit.not_closed_at", end.getX(), end.getY(), end.getZ()), List.of(end), strings, stake));
		}
		if (inside.size() > maxArea) {
			return Either.right(Problem.of(Component.translatable("message.workingvillagers.pit.too_big", inside.size(), maxArea),
				strings.values().stream().filter(string -> !string.equals(stake)).toList(), strings, stake));
		}
		if (inside.size() < 2) {
			return Either.right(Problem.of(Component.translatable("message.workingvillagers.pit.too_small"),
				strings.values().stream().filter(string -> !string.equals(stake)).toList(), strings, stake));
		}
		// The ring: the strings next to the inside; strings hanging off it do not count
		Set<Long> insideColumns = columns(inside);
		List<BlockPos> ring = strings.values().stream()
			.filter(string -> !string.equals(stake))
			.filter(string -> Direction.Plane.HORIZONTAL.stream().anyMatch(side -> insideColumns.contains(column(string.relative(side)))))
			.toList();
		if (ring.isEmpty()) {
			return Either.right(Problem.of(Component.translatable("message.workingvillagers.pit.not_closed"), List.of(), strings, stake));
		}
		int top = ring.stream().mapToInt(BlockPos::getY).max().orElseThrow();
		int low = ring.stream().mapToInt(BlockPos::getY).min().orElseThrow();
		if (top - low > Config.PIT_MAX_UNEVENNESS.get()) {
			return Either.right(Problem.of(Component.translatable("message.workingvillagers.pit.uneven", top - low, Config.PIT_MAX_UNEVENNESS.get()),
				ring.stream().filter(string -> string.getY() == top || string.getY() == low).toList(), strings, stake));
		}
		// The ladder: on the edge, in the column inside closest to the stake
		BlockPos exit = null;
		BlockPos ladder = null;
		for (BlockPos string : ring) {
			for (Direction side : Direction.Plane.HORIZONTAL) {
				BlockPos column = string.relative(side).below();
				if (insideColumns.contains(column(column))
					&& (ladder == null || column.distSqr(stake) < ladder.distSqr(stake))) {
					exit = string;
					ladder = column;
				}
			}
		}
		List<BlockPos> columns = inside.stream().map(pos -> new BlockPos(pos.getX(), top, pos.getZ())).toList();
		return Either.left(new Pit(List.copyOf(ring), columns, exit, ladder, top));
	}

	/**
	 * Pit stake: takes the ring of string that touches the stake as the pit, e.g. after the
	 * player laid or changed it. A ladder the player moved stays where it is while its string
	 * is still part of the ring. Without a closed ring the pit picked before stays, so the pit
	 * digger waits for the ring to be mended instead of forgetting the pit. Returns why there
	 * is no pit at the stake, if there is none; nothing for a pit picked in an earlier version,
	 * whose ring does not touch the stake. The pit digger at the stake decides how big the pit
	 * may be ({@link PitLimits}).
	 */
	public static Optional<Problem> attach(ServerLevel level, Workstations.Station station) {
		Either<Pit, Problem> found = fromStake(level, station.pos(), PitLimits.at(level, station).area());
		Optional<Pit> pit = found.left();
		if (pit.isEmpty()) {
			boolean earlierPick = station.pit() != null && !touchesString(level, station.pos());
			return earlierPick ? Optional.empty() : found.right();
		}
		Pit current = station.pit();
		Pit next = pit.get();
		if (current != null && next.ring.contains(current.exit)) {
			next = next.withLadderAt(current.exit, level).left().orElse(next);
		}
		if (!next.equals(current)) {
			if (current == null || !Set.copyOf(current.ring).equals(Set.copyOf(next.ring))) {
				// The stake took a new or changed ring
				for (BlockPos string : next.ring) {
					level.sendParticles(ParticleTypes.HAPPY_VILLAGER, string.getX() + 0.5, string.getY() + 0.3, string.getZ() + 0.5, 2, 0.2, 0.1, 0.2, 0.0);
				}
			}
			station.setPit(next);
		}
		return Optional.empty();
	}

	/** The string at the clicked block, or on top of it. */
	private static @Nullable BlockPos stringAt(ServerLevel level, BlockPos clicked) {
		if (level.getBlockState(clicked).is(Blocks.TRIPWIRE)) {
			return clicked.immutable();
		}
		return level.getBlockState(clicked.above()).is(Blocks.TRIPWIRE) ? clicked.above().immutable() : null;
	}

	/**
	 * A string over a hole does not count. Any block to stand on is ground, also dirt paths,
	 * farmland and slabs, which are not full blocks.
	 */
	private static boolean isOnGround(ServerLevel level, BlockPos string) {
		BlockPos below = string.below();
		return !level.getBlockState(below).getCollisionShape(level, below).isEmpty();
	}

	/**
	 * Where an open ring ends: the string with fewer than two neighbours, closest to the stake,
	 * so the player knows where to look for the gap; the stake counts as a neighbour. Null if
	 * every string has two neighbours (e.g. strings that only touch at a corner).
	 */
	private static @Nullable BlockPos openEnd(Map<Long, BlockPos> strings, BlockPos stake) {
		BlockPos end = null;
		for (BlockPos string : strings.values()) {
			if (string.equals(stake)) {
				continue;
			}
			long neighbours = Direction.Plane.HORIZONTAL.stream().filter(side -> strings.containsKey(column(string.relative(side)))).count();
			if (neighbours < 2 && (end == null || string.distSqr(stake) < end.distSqr(stake))) {
				end = string;
			}
		}
		return end;
	}

	/**
	 * The columns enclosed by the strings: everything in their bounding box that cannot be
	 * reached from outside without crossing a string. The outside may also step diagonally:
	 * strings only connect straight, so two strings touching at a corner leave a gap.
	 */
	private static List<BlockPos> inside(Set<Long> strings) {
		int minX = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxZ = Integer.MIN_VALUE;
		for (long string : strings) {
			minX = Math.min(minX, ChunkPos.getX(string));
			maxX = Math.max(maxX, ChunkPos.getX(string));
			minZ = Math.min(minZ, ChunkPos.getZ(string));
			maxZ = Math.max(maxZ, ChunkPos.getZ(string));
		}
		// One column of margin all around, so the outside is connected
		minX--;
		minZ--;
		maxX++;
		maxZ++;
		Set<Long> outside = new HashSet<>();
		Deque<long[]> open = new ArrayDeque<>();
		open.add(new long[] {minX, minZ});
		outside.add(ChunkPos.pack(minX, minZ));
		while (!open.isEmpty()) {
			long[] cell = open.poll();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					int x = (int) cell[0] + dx;
					int z = (int) cell[1] + dz;
					long key = ChunkPos.pack(x, z);
					if (x >= minX && x <= maxX && z >= minZ && z <= maxZ && !strings.contains(key) && outside.add(key)) {
						open.add(new long[] {x, z});
					}
				}
			}
		}
		List<BlockPos> inside = new ArrayList<>();
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				long key = ChunkPos.pack(x, z);
				if (!outside.contains(key) && !strings.contains(key)) {
					inside.add(new BlockPos(x, 0, z));
				}
			}
		}
		inside.sort(Comparator.<BlockPos>comparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
		return inside;
	}

	private static long column(BlockPos pos) {
		return ChunkPos.pack(pos.getX(), pos.getZ());
	}

	private static Set<Long> columns(List<BlockPos> positions) {
		Set<Long> columns = new HashSet<>();
		positions.forEach(pos -> columns.add(column(pos)));
		return columns;
	}

	/** The ring string in this column, if there is one. */
	public Optional<BlockPos> ringAt(BlockPos pos) {
		return this.ring.stream().filter(string -> string.getX() == pos.getX() && string.getZ() == pos.getZ()).findFirst();
	}
}
