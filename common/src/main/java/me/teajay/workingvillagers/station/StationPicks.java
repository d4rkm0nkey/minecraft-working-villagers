package me.teajay.workingvillagers.station;

import com.mojang.datafixers.util.Either;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.carrier.RouteSelection;
import me.teajay.workingvillagers.job.WorkerJobs;
import me.teajay.workingvillagers.menu.StationMenu;
import me.teajay.workingvillagers.station.Workstations.ChestRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

/**
 * Picking things for a workstation in the world, started with a button in its menu: the
 * worker (right-click a hired villager), chests (right-click containers, each click links
 * or unlinks one), at a pit stake the pit and its ladder (right-click a string).
 * Right-clicking the workstation ends picking chests and opens its menu
 * again; unfinished picking runs out after two minutes.
 */
public final class StationPicks {
	private static final long TIMEOUT_TICKS = 2400L;
	private static final Map<UUID, Pending> PENDING = new HashMap<>();

	private StationPicks() {
	}

	/** Pit stake: picking the ring of the pit, or the string where the ladder goes. */
	private enum PitPick { RING, LADDER }

	/**
	 * What is picked for which station; {@code role} and {@code pit} are null when picking
	 * the worker.
	 */
	private record Pending(BlockPos station, @Nullable ChestRole role, @Nullable PitPick pit, long until) {
	}

	public static void pickWorker(ServerPlayer player, BlockPos station) {
		start(player, new Pending(station.immutable(), null, null, player.level().getGameTime() + TIMEOUT_TICKS),
			Component.translatable("message.workingvillagers.assign.pick"));
	}

	public static void pickChests(ServerPlayer player, BlockPos station, ChestRole role) {
		start(player, new Pending(station.immutable(), role, null, player.level().getGameTime() + TIMEOUT_TICKS),
			Component.translatable(role == ChestRole.INPUT ? "message.workingvillagers.chests.pick_input" : "message.workingvillagers.chests.pick_output"));
	}

	/** Pit stake: the player right-clicks a string of the ring around the pit. */
	public static void pickPit(ServerPlayer player, BlockPos station) {
		start(player, new Pending(station.immutable(), null, PitPick.RING, player.level().getGameTime() + TIMEOUT_TICKS),
			Component.translatable("message.workingvillagers.pit.pick"));
	}

	/** Pit stake: the player right-clicks the string of the ring where the ladder goes. */
	public static void pickLadder(ServerPlayer player, BlockPos station) {
		start(player, new Pending(station.immutable(), null, PitPick.LADDER, player.level().getGameTime() + TIMEOUT_TICKS),
			Component.translatable("message.workingvillagers.pit.pick_ladder"));
	}

	private static void start(ServerPlayer player, Pending pending, Component message) {
		RouteSelection.cancel(player);
		PENDING.put(player.getUUID(), pending);
		player.closeContainer();
		player.sendOverlayMessage(message);
	}

	public static void cancel(ServerPlayer player) {
		PENDING.remove(player.getUUID());
	}

	private static @Nullable Pending pending(ServerPlayer player) {
		Pending pending = PENDING.get(player.getUUID());
		if (pending != null && player.level().getGameTime() > pending.until()) {
			PENDING.remove(player.getUUID());
			return null;
		}
		return pending;
	}

	/**
	 * Handles a right-click on a villager. Returns true if it belonged to picking a worker.
	 */
	public static boolean onUseVillager(ServerPlayer player, Villager villager) {
		Pending pending = pending(player);
		if (pending == null || pending.role() != null || pending.pit() != null) {
			return false;
		}
		PENDING.remove(player.getUUID());
		ServerLevel level = player.level();
		if (Workstations.find(level, pending.station()).isEmpty()) {
			return true;
		}
		Component problem = WorkerJobs.assign(level, villager, pending.station());
		if (problem != null) {
			player.sendOverlayMessage(problem);
			return true;
		}
		mark(level, villager.blockPosition().above());
		player.sendOverlayMessage(Component.translatable("message.workingvillagers.assign.done", villager.getName(),
			StationType.of(level.getBlockState(pending.station())).displayName()));
		return true;
	}

	/**
	 * Handles a right-click on a block. Returns true if it belonged to picking chests (or
	 * ended it by clicking the workstation), so the container does not open.
	 */
	public static boolean onUseBlock(ServerPlayer player, BlockPos pos) {
		Pending pending = pending(player);
		if (pending != null && pending.pit() != null) {
			return onPickPit(player, pending, pos);
		}
		if (pending == null || pending.role() == null) {
			return false;
		}
		ServerLevel level = player.level();
		Optional<Workstations.Station> found = Workstations.find(level, pending.station());
		if (found.isEmpty()) {
			PENDING.remove(player.getUUID());
			return false;
		}
		Workstations.Station station = found.get();
		if (pos.equals(pending.station())) {
			PENDING.remove(player.getUUID());
			player.openMenu(StationMenu.provider(station.type(), level, pos));
			return true;
		}
		if (Workstations.getContainer(level, pos) == null) {
			return false;
		}
		ChestRole current = station.role(pos);
		if (current == pending.role()) {
			station.setRole(pos, null);
			player.sendOverlayMessage(Component.translatable("message.workingvillagers.chests.unlinked", station.chestCount(), Config.MAX_CHESTS.get()));
		} else if (current == null && !station.canReach(pos)) {
			player.sendOverlayMessage(Component.translatable("message.workingvillagers.chests.too_far", Config.CHEST_DISTANCE.get()));
		} else if (current == null && station.chestCount() >= Config.MAX_CHESTS.get()) {
			player.sendOverlayMessage(Component.translatable("message.workingvillagers.chests.full", Config.MAX_CHESTS.get()));
		} else {
			station.setRole(pos, pending.role());
			mark(level, pos);
			player.sendOverlayMessage(Component.translatable(
				pending.role() == ChestRole.INPUT ? "message.workingvillagers.chests.linked_input" : "message.workingvillagers.chests.linked_output",
				station.chestCount(), Config.MAX_CHESTS.get()
			));
		}
		return true;
	}

	/**
	 * A right-click while picking the pit or the ladder: a string (or the block below it)
	 * picks; the station ends picking and opens its menu again; anything else does what it
	 * does otherwise.
	 */
	private static boolean onPickPit(ServerPlayer player, Pending pending, BlockPos pos) {
		ServerLevel level = player.level();
		Optional<Workstations.Station> found = Workstations.find(level, pending.station());
		if (found.isEmpty()) {
			PENDING.remove(player.getUUID());
			return false;
		}
		Workstations.Station station = found.get();
		if (pos.equals(pending.station())) {
			PENDING.remove(player.getUUID());
			player.openMenu(StationMenu.provider(station.type(), level, pos));
			return true;
		}
		if (!level.getBlockState(pos).is(Blocks.TRIPWIRE) && !level.getBlockState(pos.above()).is(Blocks.TRIPWIRE)) {
			return false;
		}
		Either<Pit, Component> result;
		if (pending.pit() == PitPick.LADDER) {
			Pit pit = station.pit();
			if (pit == null) {
				PENDING.remove(player.getUUID());
				player.sendOverlayMessage(Component.translatable("message.workingvillagers.pit.none"));
				return true;
			}
			result = pit.withLadderAt(pos, level);
		} else {
			result = Pit.find(level, pos, pending.station());
		}
		Optional<Component> problem = result.right();
		if (problem.isPresent()) {
			player.sendOverlayMessage(problem.get());
			return true;
		}
		Pit pit = result.left().orElseThrow();
		PENDING.remove(player.getUUID());
		station.setPit(pit);
		for (BlockPos string : pit.ring()) {
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, string.getX() + 0.5, string.getY() + 0.3, string.getZ() + 0.5, 2, 0.2, 0.1, 0.2, 0.0);
		}
		mark(level, pit.ladder());
		player.sendOverlayMessage(pending.pit() == PitPick.LADDER
			? Component.translatable("message.workingvillagers.pit.ladder_set")
			: Component.translatable("message.workingvillagers.pit.picked", pit.width(), pit.length(), pit.inside().size()));
		return true;
	}

	private static void mark(ServerLevel level, BlockPos pos) {
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.0);
		level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.2F);
	}
}
