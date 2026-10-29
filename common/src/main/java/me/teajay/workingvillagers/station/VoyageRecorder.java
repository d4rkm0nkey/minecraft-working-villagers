package me.teajay.workingvillagers.station;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import me.teajay.workingvillagers.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Records the route of a hauler while a player rows it (see {@link Voyage}): the player uses
 * the freight book on one jetty, rows a boat to the other one and uses the book on that jetty.
 * While the player sits in a boat, a waypoint is noted every {@link #SPACING} blocks, and before
 * every bend: wherever the straight line from the last waypoint would leave the water. The route
 * is open water the player has rowed, so the hauler can row it too.
 *
 * <p>The route goes to both jetties (the second one gets it the other way round). A recording
 * only lives until the server stops; the glint of the book shows that one is running.
 */
public final class VoyageRecorder {
	/** About how far apart the waypoints are on a straight stretch. */
	private static final int SPACING = 16;
	/** How far from its jetty the boat may lie at either end. */
	public static final int JETTY_REACH = 16;
	private static final Map<UUID, Recording> RECORDINGS = new HashMap<>();

	private VoyageRecorder() {
	}

	private static final class Recording {
		private final ResourceKey<Level> dimension;
		private final BlockPos start;
		private final List<BlockPos> waypoints = new ArrayList<>();
		/** Where the boat was when the player was last seen in it. */
		private @Nullable BlockPos last;

		private Recording(ResourceKey<Level> dimension, BlockPos start) {
			this.dimension = dimension;
			this.start = start;
		}

		/** The waypoints with the last place of the boat, where the player got out. */
		private List<BlockPos> route() {
			List<BlockPos> route = new ArrayList<>(this.waypoints);
			if (this.last != null && (route.isEmpty() || !route.getLast().equals(this.last))) {
				route.add(this.last);
			}
			return route;
		}
	}

	/**
	 * The player used the freight book on a jetty: starts recording there, cancels the
	 * recording at the jetty it started at, or finishes it at another jetty.
	 */
	public static void use(ServerPlayer player, BlockPos jetty, ItemStack book) {
		ServerLevel level = player.level();
		Recording recording = RECORDINGS.get(player.getUUID());
		if (recording == null || recording.dimension != level.dimension() || StationType.of(level.getBlockState(recording.start)) != StationType.JETTY) {
			RECORDINGS.put(player.getUUID(), new Recording(level.dimension(), jetty.immutable()));
			book.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
			level.playSound(null, jetty, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
			player.sendSystemMessage(Component.translatable("message.workingvillagers.voyage.started"));
			return;
		}
		if (recording.start.equals(jetty)) {
			stop(player, book);
			player.sendSystemMessage(Component.translatable("message.workingvillagers.voyage.cancelled"));
			return;
		}
		Component problem = finish(level, recording, jetty);
		if (problem != null) {
			// The recording goes on: the player can row on, or use the book at the start to cancel
			player.sendSystemMessage(problem);
			return;
		}
		stop(player, book);
		level.playSound(null, jetty, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 1.0F);
		Voyage voyage = Workstations.find(level, jetty).map(Workstations.Station::voyage).orElseThrow();
		player.sendSystemMessage(Component.translatable("message.workingvillagers.voyage.done",
			voyage.length(), voyage.waypoints().size(), recording.start.toShortString(), jetty.toShortString()));
	}

	/** Gives both jetties the route, or tells why it is no route. */
	private static @Nullable Component finish(ServerLevel level, Recording recording, BlockPos end) {
		List<BlockPos> route = recording.route();
		if (route.size() < 2) {
			return Component.translatable("message.workingvillagers.voyage.no_water");
		}
		if (!near(route.getFirst(), recording.start)) {
			return Component.translatable("message.workingvillagers.voyage.start_far", JETTY_REACH);
		}
		if (!near(route.getLast(), end)) {
			return Component.translatable("message.workingvillagers.voyage.end_far", JETTY_REACH);
		}
		Optional<Workstations.Station> from = Workstations.find(level, recording.start);
		Optional<Workstations.Station> to = Workstations.find(level, end);
		if (from.isEmpty() || to.isEmpty()) {
			return Component.translatable("message.workingvillagers.voyage.no_water");
		}
		Voyage voyage = new Voyage(end, route);
		if (voyage.length() > Config.HAUL_MAX_LENGTH.get()) {
			return Component.translatable("message.workingvillagers.voyage.too_long", voyage.length(), Config.HAUL_MAX_LENGTH.get());
		}
		from.get().setVoyage(voyage);
		to.get().setVoyage(voyage.reversed(recording.start));
		return null;
	}

	private static boolean near(BlockPos water, BlockPos jetty) {
		return Mth.square(water.getX() - jetty.getX()) + Mth.square(water.getZ() - jetty.getZ()) <= JETTY_REACH * JETTY_REACH
			&& Math.abs(water.getY() - jetty.getY()) <= JETTY_REACH;
	}

	private static void stop(ServerPlayer player, ItemStack book) {
		RECORDINGS.remove(player.getUUID());
		book.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
	}

	/**
	 * Every tick for every freight book in a player's inventory: notes the waypoints while the
	 * player rows. A book that still glints from a recording that is gone (the server stopped)
	 * stops glinting.
	 */
	public static void tick(ServerPlayer player, ItemStack book) {
		Recording recording = RECORDINGS.get(player.getUUID());
		if (recording == null) {
			if (book.has(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)) {
				book.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
			}
			return;
		}
		ServerLevel level = player.level();
		if (recording.dimension != level.dimension() || !(player.getVehicle() instanceof AbstractBoat boat)) {
			return;
		}
		BlockPos here = waterUnder(level, boat);
		if (here == null || here.equals(recording.last)) {
			return;
		}
		if (recording.waypoints.isEmpty()) {
			recording.waypoints.add(here);
		} else {
			BlockPos waypoint = recording.waypoints.getLast();
			if (!isWaterOnTheWay(level, waypoint, here)) {
				// Around a bend: the last place still in sight of the waypoint
				recording.waypoints.add(recording.last != null && !recording.last.equals(waypoint) ? recording.last : here);
			} else if (waypoint.distSqr(here) >= SPACING * SPACING) {
				recording.waypoints.add(here);
			}
		}
		recording.last = here;
		if (player.tickCount % 20 == 0) {
			Voyage sofar = new Voyage(recording.start, recording.route());
			player.sendOverlayMessage(Component.translatable("message.workingvillagers.voyage.recording", sofar.length(), sofar.waypoints().size()));
		}
	}

	/** The block of water the boat floats on, null if it is not on water. */
	public static @Nullable BlockPos waterUnder(ServerLevel level, AbstractBoat boat) {
		BlockPos pos = BlockPos.containing(boat.getX(), boat.getY() - 0.1, boat.getZ());
		for (BlockPos candidate : List.of(pos, pos.below())) {
			if (level.getFluidState(candidate).is(FluidTags.WATER)) {
				return candidate.immutable();
			}
		}
		return null;
	}

	/** Whether there is water with room for a boat above it all the way, in a straight line. */
	public static boolean isWaterOnTheWay(ServerLevel level, BlockPos from, BlockPos to) {
		double distance = Math.sqrt(from.distSqr(to));
		for (double along = 0; along <= distance; along += 0.5) {
			double t = distance == 0 ? 0 : along / distance;
			BlockPos pos = BlockPos.containing(Mth.lerp(t, from.getX(), to.getX()) + 0.5, Mth.lerp(t, from.getY(), to.getY()) + 0.5,
				Mth.lerp(t, from.getZ(), to.getZ()) + 0.5);
			boolean water = level.getFluidState(pos).is(FluidTags.WATER) || level.getFluidState(pos.below()).is(FluidTags.WATER);
			if (!water || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
				return false;
			}
		}
		return true;
	}
}
