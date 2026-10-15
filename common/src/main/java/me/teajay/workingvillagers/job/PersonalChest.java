package me.teajay.workingvillagers.job;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.carrier.CarrierData;
import me.teajay.workingvillagers.carrier.RouteSelection;
import me.teajay.workingvillagers.station.StationPicks;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * The personal chest of a worker: the player puts the wage ({@link Config#DAILY_WAGE} emeralds per
 * work day) and the work materials (the lumberjack's axe and saplings) into it, the worker
 * fetches them itself. Without the chest, or without its wage for the day, a worker does
 * not work.
 *
 * <p>An instance belongs to one work behavior and runs the visits to the chest; the static
 * part picks the chest in the world, started with a button in the worker's menu.
 */
public final class PersonalChest {
	public static final Item WAGE = Items.EMERALD;
	private static final float SPEED = 0.5F;
	/** How long the worker waits before it looks into its chest again. */
	private static final int RETRY_TICKS = 100;
	private static final long PICK_TIMEOUT_TICKS = 2400L;
	private static final Map<UUID, Pick> PICKS = new HashMap<>();

	/**
	 * Something the worker keeps in its inventory: fetched from the chest when it has fewer
	 * than {@code min}, up to {@code max}. Without a {@code required} one the worker waits
	 * with the status {@code missingKey}.
	 */
	public record Supply(Predicate<ItemStack> item, int min, int max, boolean required, String missingKey) {
		int carried(SimpleContainer inventory) {
			int count = 0;
			for (ItemStack stack : inventory) {
				if (!stack.isEmpty() && this.item.test(stack)) {
					count += stack.getCount();
				}
			}
			return count;
		}
	}

	private @Nullable ChestTrip trip;
	/** What the current trip is for. */
	private List<Supply> tripSupplies = List.of();
	private int waitTicks;

	/**
	 * Takes care of the personal chest before the next piece of work: fetches the wage of
	 * the day and the missing supplies. Returns true while that keeps the worker busy (on its
	 * way to the chest, or waiting because the chest, the wage or a required supply is
	 * missing), so the work has to wait; false when the worker may work.
	 */
	public boolean busy(ServerLevel level, Villager villager, List<Supply> supplies) {
		if (this.trip != null) {
			ChestTrip.Result result = this.trip.tick(level, villager, container -> takeOut(level, villager, container, this.tripSupplies));
			if (result == ChestTrip.Result.RUNNING) {
				WorkerStatus.task(villager, "personal_chest.visit");
				return true;
			}
			this.trip = null;
			if (result == ChestTrip.Result.FAILED) {
				this.waitFor(villager, null, "chest_unreachable");
			}
			return true; // look again next tick
		}
		if (this.waitTicks > 0) {
			this.waitTicks--;
			return true;
		}

		BlockPos chest = ((HiredVillager) villager).workingvillagers$getPersonalChest();
		Container container = chest == null ? null : Workstations.getContainer(level, chest);
		int wage = Config.DAILY_WAGE.get();
		if (container == null) {
			if (wage == 0 && supplies.stream().noneMatch(Supply::required)) {
				// Works for free and needs no tools: the chest is optional
				return false;
			}
			this.waitFor(villager, null, "personal_chest.missing");
			return true;
		}
		boolean wageDue = wage > 0 && !isPaid(level, villager);
		if (wageDue && countIn(container, stack -> stack.is(WAGE)) < wage) {
			this.waitFor(villager, chest, "personal_chest.no_wage");
			return true;
		}
		boolean fetch = wageDue;
		String missing = null;
		for (Supply supply : supplies) {
			if (supply.carried(villager.getInventory()) >= supply.min()) {
				continue;
			}
			if (countIn(container, supply.item()) > 0) {
				fetch = true;
			} else if (supply.required() && missing == null) {
				missing = supply.missingKey();
			}
		}
		if (fetch) {
			// Takes what is there, even if something required is still missing
			this.trip = ChestTrip.to(level, villager, chest);
			this.tripSupplies = supplies;
			WorkerStatus.task(villager, "personal_chest.visit");
			return true;
		}
		if (missing != null) {
			this.waitFor(villager, chest, missing);
			return true;
		}
		return false;
	}

	/** Breaks off a visit, e.g. when the work hours are over. */
	public void stop(Villager villager) {
		if (this.trip != null) {
			this.trip.close(villager);
			this.trip = null;
		}
		this.waitTicks = 0;
	}

	/** Waits a while (next to the chest, if there is one) and tells the menus why. */
	private void waitFor(Villager villager, @Nullable BlockPos chest, String reason) {
		WorkerStatus.task(villager, reason);
		this.waitTicks = RETRY_TICKS;
		if (chest != null && !chest.closerToCenterThan(villager.position(), 3.0)) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(chest, SPEED, 2));
		}
	}

	private static void takeOut(ServerLevel level, Villager villager, Container container, List<Supply> supplies) {
		int wage = Config.DAILY_WAGE.get();
		if (wage > 0 && !isPaid(level, villager) && removeFrom(container, stack -> stack.is(WAGE), wage) == wage) {
			((HiredVillager) villager).workingvillagers$setPaidDay(ChopTrees.today(level));
		}
		SimpleContainer inventory = villager.getInventory();
		for (Supply supply : supplies) {
			int carried = supply.carried(inventory);
			if (carried < supply.min()) {
				Chests.take(container, inventory, supply.item(), supply.max() - carried);
			}
		}
	}

	/** Whether the worker has taken its wage for today. */
	public static boolean isPaid(ServerLevel level, Villager villager) {
		return ((HiredVillager) villager).workingvillagers$getPaidDay() >= ChopTrees.today(level);
	}

	public static int countIn(Container container, Predicate<ItemStack> filter) {
		int count = 0;
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot);
			if (!stack.isEmpty() && filter.test(stack)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	/** Removes up to {@code max} matching items from the container, returns how many. */
	private static int removeFrom(Container container, Predicate<ItemStack> filter, int max) {
		int removed = 0;
		for (int slot = 0; slot < container.getContainerSize() && removed < max; slot++) {
			ItemStack stack = container.getItem(slot);
			if (!stack.isEmpty() && filter.test(stack)) {
				int amount = Math.min(stack.getCount(), max - removed);
				stack.shrink(amount);
				removed += amount;
			}
		}
		return removed;
	}

	/** The worker's personal chest, if it has one and it is loaded. */
	public static Optional<Container> containerOf(ServerLevel level, Villager villager) {
		BlockPos chest = ((HiredVillager) villager).workingvillagers$getPersonalChest();
		return Optional.ofNullable(chest == null ? null : Workstations.getContainer(level, chest));
	}

	// Picking the chest in the world

	/** Who picks a chest for which worker. */
	private record Pick(UUID worker, long until) {
	}

	/**
	 * Closes the menu; the player then right-clicks the chest for the worker (clicking its
	 * current chest again takes it away).
	 */
	public static void pick(ServerPlayer player, Villager worker) {
		StationPicks.cancel(player);
		RouteSelection.cancel(player);
		PICKS.put(player.getUUID(), new Pick(worker.getUUID(), player.level().getGameTime() + PICK_TIMEOUT_TICKS));
		player.closeContainer();
		player.sendOverlayMessage(Component.translatable("message.workingvillagers.personal_chest.pick", worker.getName()));
	}

	public static void cancel(ServerPlayer player) {
		PICKS.remove(player.getUUID());
	}

	/**
	 * Handles a right-click on a block. Returns true if it picked the chest, so the container
	 * does not open.
	 */
	public static boolean onUseBlock(ServerPlayer player, BlockPos pos) {
		Pick pick = PICKS.get(player.getUUID());
		ServerLevel level = player.level();
		if (pick == null || level.getGameTime() > pick.until()) {
			PICKS.remove(player.getUUID());
			return false;
		}
		if (Workstations.getContainer(level, pos) == null) {
			return false;
		}
		if (!(level.getEntity(pick.worker()) instanceof Villager worker) || !worker.isAlive()) {
			PICKS.remove(player.getUUID());
			player.sendOverlayMessage(Component.translatable("message.workingvillagers.personal_chest.worker_gone"));
			return true;
		}
		HiredVillager hired = (HiredVillager) worker;
		boolean current = pos.equals(hired.workingvillagers$getPersonalChest());
		if (!current && !pos.closerThan(anchor(level, worker), Config.PERSONAL_CHEST_DISTANCE.get())) {
			// Still picking: the player can click a closer chest
			player.sendOverlayMessage(Component.translatable("message.workingvillagers.personal_chest.too_far", Config.PERSONAL_CHEST_DISTANCE.get()));
			return true;
		}
		PICKS.remove(player.getUUID());
		if (current) {
			hired.workingvillagers$setPersonalChest(null);
			player.sendOverlayMessage(Component.translatable("message.workingvillagers.personal_chest.removed", worker.getName()));
		} else {
			hired.workingvillagers$setPersonalChest(pos);
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.0);
			level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.2F);
			player.sendOverlayMessage(Component.translatable("message.workingvillagers.personal_chest.set", worker.getName()));
		}
		return true;
	}

	/** Where the worker works: its workstation, the source of its first route, or where it is. */
	static BlockPos anchor(ServerLevel level, Villager worker) {
		Optional<GlobalPos> jobSite = worker.getBrain().getMemory(MemoryModuleType.JOB_SITE);
		if (jobSite.isPresent() && jobSite.get().dimension() == level.dimension()) {
			return jobSite.get().pos();
		}
		CarrierData carrier = ((HiredVillager) worker).workingvillagers$getCarrier();
		if (carrier != null && !carrier.routes().isEmpty()) {
			return carrier.routes().getFirst().source();
		}
		return worker.blockPosition();
	}
}
