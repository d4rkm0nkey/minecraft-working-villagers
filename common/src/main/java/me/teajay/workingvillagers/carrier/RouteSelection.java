package me.teajay.workingvillagers.carrier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import me.teajay.workingvillagers.item.WickerBasketItem;
import me.teajay.workingvillagers.job.HiredVillager;
import me.teajay.workingvillagers.station.ContainerNames;
import me.teajay.workingvillagers.station.StationPicks;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import org.jspecify.annotations.Nullable;

/**
 * Picking the containers of a new route: started from a free route slot in the carrier
 * menu, the player then right-clicks the source and the target. Clicking the source again
 * (either half of a double chest) cancels; an unfinished selection runs out after two minutes.
 */
public final class RouteSelection {
	private static final long TIMEOUT_TICKS = 2400L;
	private static final Map<UUID, Pending> PENDING = new HashMap<>();

	private RouteSelection() {
	}

	/**
	 * Who gets the route: a carrier, or the basket in the player's hand.
	 */
	private record Pending(@Nullable UUID carrier, @Nullable InteractionHand basketHand, @Nullable BlockPos source, long until) {
		Pending withSource(BlockPos source) {
			return new Pending(this.carrier, this.basketHand, source.immutable(), this.until);
		}
	}

	public static void startForCarrier(ServerPlayer player, Villager carrier) {
		start(player, new Pending(carrier.getUUID(), null, null, player.level().getGameTime() + TIMEOUT_TICKS));
	}

	public static void startForBasket(ServerPlayer player, InteractionHand hand) {
		start(player, new Pending(null, hand, null, player.level().getGameTime() + TIMEOUT_TICKS));
	}

	private static void start(ServerPlayer player, Pending pending) {
		StationPicks.cancel(player);
		PENDING.put(player.getUUID(), pending);
		player.closeContainer();
		player.sendOverlayMessage(Component.translatable("message.workingvillagers.route.pick_source"));
	}

	public static void cancel(ServerPlayer player) {
		PENDING.remove(player.getUUID());
	}

	/**
	 * Handles a right-click on a block. Returns true if the click belonged to a route
	 * selection, so the container does not open.
	 */
	public static boolean onUseBlock(ServerPlayer player, BlockPos pos) {
		Pending pending = PENDING.get(player.getUUID());
		if (pending == null) {
			return false;
		}
		ServerLevel level = player.level();
		if (level.getGameTime() > pending.until()) {
			PENDING.remove(player.getUUID());
			return false;
		}
		if (Workstations.getContainer(level, pos) == null) {
			return false;
		}

		if (pending.source() == null) {
			PENDING.put(player.getUUID(), pending.withSource(pos));
			mark(level, pos);
			player.sendOverlayMessage(Component.translatable("message.workingvillagers.route.source_selected", ContainerNames.name(level, pos)));
			return true;
		}
		PENDING.remove(player.getUUID());
		// The other half of a double chest is the same container
		if (pending.source().equals(pos) || pending.source().equals(Workstations.otherHalf(level, pos))) {
			player.sendOverlayMessage(Component.translatable("message.workingvillagers.route.cancelled"));
			return true;
		}

		RouteHolder holder = holderFor(player, pending);
		if (holder == null) {
			player.sendOverlayMessage(Component.translatable("message.workingvillagers.route.holder_gone"));
			return true;
		}
		Route route = new Route(pending.source(), pos.immutable());
		Component problem = Route.checkNew(level, holder.routes(), route);
		if (problem != null) {
			player.sendOverlayMessage(problem);
			return true;
		}
		holder.addRoute(route);
		mark(level, pos);
		int count = holder.routes().size();
		player.sendOverlayMessage(Component.translatable("message.workingvillagers.route.added", route.describe(level), count, Route.MAX_ROUTES));
		return true;
	}

	private static @Nullable RouteHolder holderFor(ServerPlayer player, Pending pending) {
		if (pending.carrier() != null) {
			return player.level().getEntity(pending.carrier()) instanceof Villager carrier
				&& ((HiredVillager) carrier).workingvillagers$getCarrier() != null
				&& Carriers.canEdit(carrier, player)
				? RouteHolder.of(carrier)
				: null;
		}
		if (pending.basketHand() != null && player.getItemInHand(pending.basketHand()).getItem() instanceof WickerBasketItem) {
			return RouteHolder.of(player, pending.basketHand());
		}
		return null;
	}

	private static void mark(ServerLevel level, BlockPos pos) {
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.0);
		level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.2F);
	}
}
