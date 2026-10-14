package me.teajay.workingvillagers.network;

import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.carrier.Route;
import me.teajay.workingvillagers.menu.CarrierMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sent by the carrier screen when the player renames a route. Menu buttons only carry a
 * number, so the name needs a packet of its own.
 */
public record RenameRoutePayload(int containerId, int route, String name) implements CustomPacketPayload {
	public static final Type<RenameRoutePayload> TYPE = new Type<>(WorkingVillagers.id("rename_route"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RenameRoutePayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, RenameRoutePayload::containerId,
		ByteBufCodecs.VAR_INT, RenameRoutePayload::route,
		ByteBufCodecs.stringUtf8(Route.MAX_NAME_LENGTH * 4), RenameRoutePayload::name,
		RenameRoutePayload::new
	);

	@Override
	public Type<RenameRoutePayload> type() {
		return TYPE;
	}

	/** Server thread: renames the route if the player still has that menu open. */
	public void handle(ServerPlayer player) {
		if (player.containerMenu instanceof CarrierMenu menu && menu.containerId == this.containerId) {
			menu.renameRoute(player, this.route, this.name);
		}
	}
}
