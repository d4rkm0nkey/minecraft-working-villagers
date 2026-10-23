package me.teajay.workingvillagers.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.teajay.workingvillagers.network.PitPreviewPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the pits at the pit stakes nearby while the player holds string, like the outline of
 * a structure block: a pit as the outline of what is dug (at the top of the ring, and through
 * the ground the floor and the corners), a ring that is no pit as a frame around each string, red where the problem is. The
 * server sends what to draw ({@link PitPreviewPayload}); the outline is drawn anew every tick.
 */
public final class PitPreviewRenderer {
	private static final int PIT_COLOR = 0xFFFFFFFF;
	/** The floor and the corners lie in the ground: drawn through it, fainter. */
	private static final int BELOW_COLOR = 0x99FFFFFF;
	private static final int STRING_COLOR = 0xFFB0B0B0;
	private static final int PROBLEM_COLOR = 0xFFFF4040;
	private static final float WIDTH = 2.0F;
	private static volatile List<PitPreviewPayload.Stake> stakes = List.of();

	private PitPreviewRenderer() {
	}

	public static void receive(PitPreviewPayload payload) {
		stakes = payload.stakes();
	}

	/** At the end of a client tick, which collects the gizmos of the tick. */
	public static void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null) {
			stakes = List.of();
			return;
		}
		if (!player.getMainHandItem().is(Items.STRING) && !player.getOffhandItem().is(Items.STRING)) {
			return;
		}
		for (PitPreviewPayload.Stake stake : stakes) {
			if (stake.isPit()) {
				outlinePit(stake);
			} else {
				Set<BlockPos> spots = Set.copyOf(stake.spots());
				for (BlockPos string : stake.strings()) {
					boolean problem = spots.contains(string);
					Gizmos.cuboid(new AABB(string).setMaxY(string.getY() + 0.125), GizmoStyle.stroke(problem ? PROBLEM_COLOR : STRING_COLOR, WIDTH));
				}
			}
		}
	}

	/**
	 * The edges of the columns inside the ring that border the outside: lines at the top of the
	 * ring and on the floor, and upright ones where the edge turns.
	 */
	private static void outlinePit(PitPreviewPayload.Stake stake) {
		Set<Long> inside = new HashSet<>();
		stake.inside().forEach(column -> inside.add(column(column.getX(), column.getZ())));
		int top = stake.inside().getFirst().getY();
		double topY = top + 0.02;
		double floorY = stake.bottom() + 0.02;
		// The corners of the edges (in half blocks), with the axes of the edges that meet there
		Map<Long, Integer> corners = new HashMap<>();
		for (BlockPos column : stake.inside()) {
			for (Direction side : Direction.Plane.HORIZONTAL) {
				if (inside.contains(column(column.getX() + side.getStepX(), column.getZ() + side.getStepZ()))) {
					continue;
				}
				// The edge of the column on that side, from one corner to the other
				Direction along = side.getClockWise();
				double x = column.getX() + 0.5 + side.getStepX() * 0.5;
				double z = column.getZ() + 0.5 + side.getStepZ() * 0.5;
				double fromX = x - along.getStepX() * 0.5;
				double fromZ = z - along.getStepZ() * 0.5;
				double toX = x + along.getStepX() * 0.5;
				double toZ = z + along.getStepZ() * 0.5;
				Gizmos.line(new Vec3(fromX, topY, fromZ), new Vec3(toX, topY, toZ), PIT_COLOR, WIDTH);
				Gizmos.line(new Vec3(fromX, floorY, fromZ), new Vec3(toX, floorY, toZ), BELOW_COLOR, WIDTH).setAlwaysOnTop();
				int axis = along.getAxis() == Direction.Axis.X ? 1 : 2;
				corners.merge(column(Math.round(fromX * 2), Math.round(fromZ * 2)), axis, (a, b) -> a | b);
				corners.merge(column(Math.round(toX * 2), Math.round(toZ * 2)), axis, (a, b) -> a | b);
			}
		}
		// Upright where the edge turns
		corners.forEach((corner, axes) -> {
			if (axes == 3) {
				double x = (corner >> 32) / 2.0;
				double z = (int) (long) corner / 2.0;
				Gizmos.line(new Vec3(x, floorY, z), new Vec3(x, topY, z), BELOW_COLOR, WIDTH).setAlwaysOnTop();
			}
		});
	}

	private static long column(long x, long z) {
		return x << 32 | z & 0xFFFFFFFFL;
	}
}
