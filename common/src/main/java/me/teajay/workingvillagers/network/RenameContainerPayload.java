package me.teajay.workingvillagers.network;

import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.station.ContainerNames;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;

/**
 * Sent by a chest or barrel screen when the player names the container. The screen only
 * knows the block from the crosshair, so the server checks that the open menu really is
 * that container.
 */
public record RenameContainerPayload(int containerId, BlockPos pos, String name) implements CustomPacketPayload {
	public static final Type<RenameContainerPayload> TYPE = new Type<>(WorkingVillagers.id("rename_container"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RenameContainerPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, RenameContainerPayload::containerId,
		BlockPos.STREAM_CODEC, RenameContainerPayload::pos,
		ByteBufCodecs.stringUtf8(ContainerNames.MAX_LENGTH * 4), RenameContainerPayload::name,
		RenameContainerPayload::new
	);

	@Override
	public Type<RenameContainerPayload> type() {
		return TYPE;
	}

	/** Server thread: names the container if the player still has it open. */
	public void handle(ServerPlayer player) {
		if (!(player.containerMenu instanceof ChestMenu menu) || menu.containerId != this.containerId || !menu.stillValid(player)) {
			return;
		}
		if (!(player.level().getBlockEntity(this.pos) instanceof BaseContainerBlockEntity container) || !isShownBy(menu, container)) {
			return;
		}
		ContainerNames.rename(player.level(), this.pos, this.name);
	}

	private static boolean isShownBy(ChestMenu menu, BaseContainerBlockEntity container) {
		Container shown = menu.getContainer();
		return shown == container || shown instanceof CompoundContainer compound && compound.contains(container);
	}
}
