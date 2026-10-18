package me.teajay.workingvillagers.station;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.teajay.workingvillagers.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * A tunnel for the miner, picked by the player with a right-click on the side of a block:
 * the tunnel goes from that block straight into the hill, away from the clicked side.
 *
 * <p>A <b>tunnel</b> is 3 wide and 3 high, the clicked block in the middle of its bottom row,
 * with wooden supports every 4 blocks like a mineshaft; side tunnels (1 wide, 2 high) go off
 * left and right every 4 blocks, between the supports. <b>Stairs</b> are 1 wide and go down
 * one block with every block forward.
 *
 * @param start the clicked block: the middle of the bottom row of the first section
 * @param direction where the tunnel goes, horizontal
 * @param length how many sections (blocks forward)
 * @param branches how long the side tunnels are, 0 for none (not for stairs)
 * @param stairs whether these are stairs down instead of a tunnel
 */
public record Tunnel(BlockPos start, Direction direction, int length, int branches, boolean stairs) {
	public static final int DEFAULT_LENGTH = 16;
	/** Side tunnels go off at the sections 1, 5, 9, ..., supports stand at 3, 7, 11, ... */
	private static final int SPACING = 4;
	/** Torches at the sections 6, 14, 22, ... (and the same in side tunnels and stairs). */
	private static final int TORCH_SPACING = 8;

	public static final Codec<Tunnel> CODEC = RecordCodecBuilder.create(i -> i.group(
		BlockPos.CODEC.fieldOf("start").forGetter(Tunnel::start),
		Direction.CODEC.fieldOf("direction").forGetter(Tunnel::direction),
		Codec.INT.optionalFieldOf("length", DEFAULT_LENGTH).forGetter(Tunnel::length),
		Codec.INT.optionalFieldOf("branches", 0).forGetter(Tunnel::branches),
		Codec.BOOL.optionalFieldOf("stairs", false).forGetter(Tunnel::stairs)
	).apply(i, Tunnel::new));

	/** What the miner puts into a section besides leaving it empty. */
	public enum Fixture { FENCE, PLANKS, TORCH }

	/**
	 * A part of the tunnel the miner digs as a whole before it goes on: one slice of the
	 * tunnel, one block of a side tunnel or one step of the stairs.
	 *
	 * @param cells the blocks that are dug out, in the order the miner digs them (from the top)
	 * @param floor the blocks under the bottom cells, closed with cobblestone where there is a hole
	 * @param shell the blocks around the cells (walls, ceiling, the next section), sealed where
	 *     there is water or lava, so digging the cells does not let it in
	 * @param stand where the miner can stand in this section once it is dug
	 * @param fixtures the cells that get a fence, planks or a torch once the section is dug
	 */
	public record Section(int index, List<BlockPos> cells, List<BlockPos> floor, List<BlockPos> shell, List<BlockPos> stand,
		Map<BlockPos, Fixture> fixtures) {
	}

	public Tunnel withLength(int length) {
		return new Tunnel(this.start, this.direction, Math.clamp(length, 1, Config.TUNNEL_MAX_LENGTH.get()), this.branches, this.stairs);
	}

	public Tunnel withBranches(int branches) {
		return new Tunnel(this.start, this.direction, this.length, Math.clamp(branches, 0, Config.TUNNEL_MAX_BRANCH.get()), this.stairs);
	}

	public Tunnel withStairs(boolean stairs) {
		return new Tunnel(this.start, this.direction, this.length, this.branches, stairs);
	}

	/** The same settings for a tunnel at another place. */
	public Tunnel at(BlockPos start, Direction direction) {
		return new Tunnel(start.immutable(), direction, this.length, this.branches, this.stairs);
	}

	/** A new tunnel with the default settings. */
	public static Tunnel of(BlockPos start, Direction direction) {
		return new Tunnel(start.immutable(), direction, Math.min(DEFAULT_LENGTH, Config.TUNNEL_MAX_LENGTH.get()), 0, false);
	}

	/** The length within the server's limit. */
	public int clampedLength() {
		return Math.clamp(this.length, 1, Config.TUNNEL_MAX_LENGTH.get());
	}

	public int clampedBranches() {
		return this.stairs ? 0 : Math.clamp(this.branches, 0, Config.TUNNEL_MAX_BRANCH.get());
	}

	/** Where the miner stands in front of the tunnel to dig its first section. */
	public List<BlockPos> entrance() {
		BlockPos front = this.start.relative(this.direction.getOpposite());
		if (this.stairs) {
			return List.of(front);
		}
		Direction side = this.direction.getClockWise();
		return List.of(front, front.relative(side), front.relative(side.getOpposite()));
	}

	/** The sections in the order the miner digs them: along the tunnel, each side tunnel when it gets there. */
	public List<Section> sections(boolean supports) {
		List<Section> sections = new ArrayList<>();
		int length = this.clampedLength();
		if (this.stairs) {
			for (int step = 0; step < length; step++) {
				sections.add(this.step(sections.size(), step));
			}
			return sections;
		}
		int branches = this.clampedBranches();
		Direction side = this.direction.getClockWise();
		for (int i = 0; i < length; i++) {
			sections.add(this.slice(sections.size(), i, supports));
			if (branches > 0 && i % SPACING == 1) {
				for (Direction way : new Direction[] {side.getOpposite(), side}) {
					for (int j = 0; j < branches; j++) {
						sections.add(this.branch(sections.size(), i, way, j));
					}
				}
			}
		}
		return sections;
	}

	/** A slice of the 3×3 tunnel. */
	private Section slice(int index, int i, boolean supports) {
		Direction side = this.direction.getClockWise();
		BlockPos center = this.start.relative(this.direction, i);
		List<BlockPos> cells = new ArrayList<>();
		for (int y = 2; y >= 0; y--) {
			for (int s : new int[] {0, -1, 1}) {
				cells.add(center.relative(side, s).above(y));
			}
		}
		List<BlockPos> floor = new ArrayList<>();
		List<BlockPos> stand = new ArrayList<>();
		List<BlockPos> shell = new ArrayList<>();
		for (int s = -1; s <= 1; s++) {
			BlockPos bottom = center.relative(side, s);
			floor.add(bottom.below());
			shell.add(bottom.above(3));
			for (int y = 0; y < 3; y++) {
				shell.add(bottom.relative(this.direction).above(y));
			}
		}
		for (int y = 0; y < 3; y++) {
			shell.add(center.relative(side, 2).above(y));
			shell.add(center.relative(side, -2).above(y));
		}
		Map<BlockPos, Fixture> fixtures = new LinkedHashMap<>();
		boolean support = supports && i % SPACING == SPACING - 1;
		if (support) {
			for (int s : new int[] {-1, 1}) {
				fixtures.put(center.relative(side, s), Fixture.FENCE);
				fixtures.put(center.relative(side, s).above(), Fixture.FENCE);
			}
			for (int s = -1; s <= 1; s++) {
				fixtures.put(center.relative(side, s).above(2), Fixture.PLANKS);
			}
			stand.add(center);
		} else {
			for (int s : new int[] {0, -1, 1}) {
				stand.add(center.relative(side, s));
			}
		}
		if (i % TORCH_SPACING == TORCH_SPACING - 2) {
			fixtures.put(center.relative(side, -1).above(), Fixture.TORCH);
		}
		return new Section(index, cells, floor, shell, stand, fixtures);
	}

	/** One block of a side tunnel (1 wide, 2 high) off the slice {@code i}, towards {@code way}. */
	private Section branch(int index, int i, Direction way, int j) {
		BlockPos base = this.start.relative(this.direction, i).relative(way, 2 + j);
		List<BlockPos> shell = new ArrayList<>(List.of(
			base.relative(this.direction), base.relative(this.direction).above(),
			base.relative(this.direction.getOpposite()), base.relative(this.direction.getOpposite()).above(),
			base.above(2), base.relative(way), base.relative(way).above()
		));
		Map<BlockPos, Fixture> fixtures = new LinkedHashMap<>();
		if (j % TORCH_SPACING == TORCH_SPACING - 2) {
			fixtures.put(base.above(), Fixture.TORCH);
		}
		return new Section(index, List.of(base.above(), base), List.of(base.below()), shell, List.of(base), fixtures);
	}

	/** One step of the stairs: 1 wide, 3 high (room for the head when walking down), one lower than the last. */
	private Section step(int index, int k) {
		BlockPos base = this.start.relative(this.direction, k).below(k);
		Direction side = this.direction.getClockWise();
		List<BlockPos> cells = List.of(base.above(2), base.above(), base);
		List<BlockPos> shell = new ArrayList<>();
		for (int y = 0; y < 3; y++) {
			shell.add(base.relative(side).above(y));
			shell.add(base.relative(side.getOpposite()).above(y));
			shell.add(base.relative(this.direction).above(y));
		}
		shell.add(base.above(3));
		Map<BlockPos, Fixture> fixtures = new LinkedHashMap<>();
		if (k % TORCH_SPACING == TORCH_SPACING - 2) {
			fixtures.put(base.above(), Fixture.TORCH);
		}
		return new Section(index, cells, List.of(base.below()), shell, List.of(base), fixtures);
	}
}
