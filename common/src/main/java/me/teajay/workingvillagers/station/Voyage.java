package me.teajay.workingvillagers.station;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * The route of a hauler from its jetty to another one, as a player rowed it with the freight
 * book (see {@link VoyageRecorder}): the water the boat went through, one block of water every
 * few blocks, from where the player got into the boat at this jetty to where it got out at the
 * other one. The hauler rows straight from one waypoint to the next.
 */
public record Voyage(BlockPos destination, List<BlockPos> waypoints) {
	public static final Codec<Voyage> CODEC = RecordCodecBuilder.create(i -> i.group(
		BlockPos.CODEC.fieldOf("destination").forGetter(Voyage::destination),
		BlockPos.CODEC.listOf().fieldOf("waypoints").forGetter(Voyage::waypoints)
	).apply(i, Voyage::new));

	public Voyage {
		waypoints = List.copyOf(waypoints);
	}

	/** The same water the other way round, for the jetty at the other end. */
	public Voyage reversed(BlockPos home) {
		return new Voyage(home, this.waypoints.reversed());
	}

	/** Where the boat lies at this jetty. */
	public BlockPos start() {
		return this.waypoints.getFirst();
	}

	/** Where the boat lies at the other jetty. */
	public BlockPos end() {
		return this.waypoints.getLast();
	}

	/** How far the boat goes from one jetty to the other, in blocks. */
	public int length() {
		double length = 0;
		for (int i = 1; i < this.waypoints.size(); i++) {
			length += Math.sqrt(this.waypoints.get(i - 1).distSqr(this.waypoints.get(i)));
		}
		return (int) Math.round(length);
	}
}
