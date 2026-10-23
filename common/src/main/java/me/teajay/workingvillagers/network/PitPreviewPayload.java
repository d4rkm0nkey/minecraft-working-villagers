package me.teajay.workingvillagers.network;

import io.netty.buffer.ByteBuf;
import java.util.List;
import me.teajay.workingvillagers.WorkingVillagers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent to a player who holds string: the pits at the pit stakes nearby, for the outline the
 * client draws (see {@code PitPreviews}). Empty when the player holds no string (anymore).
 */
public record PitPreviewPayload(List<Stake> stakes) implements CustomPacketPayload {
	public static final PitPreviewPayload EMPTY = new PitPreviewPayload(List.of());
	public static final Type<PitPreviewPayload> TYPE = new Type<>(WorkingVillagers.id("pit_preview"));
	private static final StreamCodec<ByteBuf, List<BlockPos>> POSITIONS = BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list());
	public static final StreamCodec<RegistryFriendlyByteBuf, PitPreviewPayload> CODEC = StreamCodec.composite(
		Stake.CODEC.apply(ByteBufCodecs.list()), PitPreviewPayload::stakes,
		PitPreviewPayload::new
	);

	/**
	 * One stake: the strings that touch it, the ones that are the problem (red), and for a
	 * pit the columns inside (at the height of the highest string), the lowest layer dug and
	 * its storeys (the heights of the floors, the columns of the pillars). {@code inside} is
	 * empty if the strings are no pit.
	 */
	public record Stake(BlockPos stake, List<BlockPos> strings, List<BlockPos> spots, List<BlockPos> inside, int bottom, List<Integer> floors, List<BlockPos> pillars) {
		static final StreamCodec<ByteBuf, Stake> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, Stake::stake,
			POSITIONS, Stake::strings,
			POSITIONS, Stake::spots,
			POSITIONS, Stake::inside,
			ByteBufCodecs.INT, Stake::bottom,
			ByteBufCodecs.INT.apply(ByteBufCodecs.list()), Stake::floors,
			POSITIONS, Stake::pillars,
			Stake::new
		);

		public boolean isPit() {
			return !this.inside.isEmpty();
		}
	}

	@Override
	public Type<PitPreviewPayload> type() {
		return TYPE;
	}
}
