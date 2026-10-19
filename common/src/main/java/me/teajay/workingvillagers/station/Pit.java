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
	 * Finds the ring of string the player clicked (a string, or the block below one) and the
	 * pit inside it, for the station at {@code station}. Returns the pit, or why there is none.
	 */
	public static Either<Pit, Component> find(ServerLevel level, BlockPos clicked, BlockPos station) {
		BlockPos start = stringAt(level, clicked);
		if (start == null) {
			return Either.right(Component.translatable("message.workingvillagers.pit.no_string"));
		}
		// The strings that touch each other (straight, maybe one block up or down), on solid ground
		Map<Long, BlockPos> strings = new LinkedHashMap<>();
		Deque<BlockPos> open = new ArrayDeque<>();
		if (isOnGround(level, start)) {
			strings.put(column(start), start);
			open.add(start);
		}
		while (!open.isEmpty()) {
			BlockPos string = open.poll();
			for (Direction side : Direction.Plane.HORIZONTAL) {
				for (int dy = -1; dy <= 1; dy++) {
					BlockPos next = string.relative(side).above(dy);
					if (!strings.containsKey(column(next)) && level.isLoaded(next) && level.getBlockState(next).is(Blocks.TRIPWIRE) && isOnGround(level, next)) {
						if (strings.size() >= MAX_STRINGS) {
							return Either.right(Component.translatable("message.workingvillagers.pit.too_big", "> " + Config.PIT_MAX_AREA.get(), Config.PIT_MAX_AREA.get()));
						}
						strings.put(column(next), next);
						open.add(next);
					}
				}
			}
		}

		List<BlockPos> inside = inside(strings.keySet());
		if (inside.isEmpty()) {
			return Either.right(Component.translatable("message.workingvillagers.pit.not_closed"));
		}
		int max = Config.PIT_MAX_AREA.get();
		if (inside.size() > max) {
			return Either.right(Component.translatable("message.workingvillagers.pit.too_big", inside.size(), max));
		}
		if (inside.size() < 2) {
			return Either.right(Component.translatable("message.workingvillagers.pit.too_small"));
		}
		// The ring: the strings next to the inside; strings hanging off it do not count
		Set<Long> insideColumns = columns(inside);
		List<BlockPos> ring = strings.values().stream()
			.filter(string -> Direction.Plane.HORIZONTAL.stream().anyMatch(side -> insideColumns.contains(column(string.relative(side)))))
			.toList();
		int top = ring.stream().mapToInt(BlockPos::getY).max().orElseThrow();
		int low = ring.stream().mapToInt(BlockPos::getY).min().orElseThrow();
		if (top - low > Config.PIT_MAX_UNEVENNESS.get()) {
			return Either.right(Component.translatable("message.workingvillagers.pit.uneven", top - low, Config.PIT_MAX_UNEVENNESS.get()));
		}
		int reach = Config.CHEST_DISTANCE.get();
		if (ring.stream().anyMatch(string -> !string.closerThan(station, reach))) {
			return Either.right(Component.translatable("message.workingvillagers.pit.too_far", reach));
		}
		// The ladder: on the edge, in the column inside closest to the station
		BlockPos exit = null;
		BlockPos ladder = null;
		for (BlockPos string : ring) {
			for (Direction side : Direction.Plane.HORIZONTAL) {
				BlockPos column = string.relative(side).below();
				if (insideColumns.contains(column(column))
					&& (ladder == null || column.distSqr(station) < ladder.distSqr(station))) {
					exit = string;
					ladder = column;
				}
			}
		}
		List<BlockPos> columns = inside.stream().map(pos -> new BlockPos(pos.getX(), top, pos.getZ())).toList();
		return Either.left(new Pit(List.copyOf(ring), columns, exit, ladder, top));
	}

	/** The string at the clicked block, or on top of it. */
	private static @Nullable BlockPos stringAt(ServerLevel level, BlockPos clicked) {
		if (level.getBlockState(clicked).is(Blocks.TRIPWIRE)) {
			return clicked.immutable();
		}
		return level.getBlockState(clicked.above()).is(Blocks.TRIPWIRE) ? clicked.above().immutable() : null;
	}

	/** A string over a hole does not count. */
	private static boolean isOnGround(ServerLevel level, BlockPos string) {
		return level.getBlockState(string.below()).isFaceSturdy(level, string.below(), Direction.UP);
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
