package me.teajay.workingvillagers.network;

import me.teajay.workingvillagers.WorkingVillagers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent to the players near a worker: the sign above its head (see {@code WorkerNotice}), when
 * it changes and again now and then while there is one.
 */
public record WorkerNoticePayload(int entity, byte notice) implements CustomPacketPayload {
	public static final Type<WorkerNoticePayload> TYPE = new Type<>(WorkingVillagers.id("worker_notice"));
	public static final StreamCodec<RegistryFriendlyByteBuf, WorkerNoticePayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, WorkerNoticePayload::entity,
		ByteBufCodecs.BYTE, WorkerNoticePayload::notice,
		WorkerNoticePayload::new
	);

	@Override
	public Type<WorkerNoticePayload> type() {
		return TYPE;
	}
}
