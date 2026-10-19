package me.teajay.workingvillagers.job;

import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;

/**
 * What a worker is doing right now, for the menus: the step of its work while it works
 * (set by the work behaviors), otherwise what the villager's day brings (sleeping, meeting
 * the others at the bell, free time, fleeing).
 */
public final class WorkerStatus {
	private static final String PREFIX = "status.workingvillagers.";
	/** How often a worker that cannot work shakes its head at a player nearby. */
	private static final int COMPLAIN_TICKS = 200;
	private static final double COMPLAIN_DISTANCE = 8.0;
	/** The statuses the player has to do something about. */
	private static final Set<String> PROBLEMS = Set.of(
		"no_wage", "lumberjack.no_axe", "farmer.no_hoe", "farmer.no_seeds", "shepherd.no_shears", "shepherd.no_sheep", "shepherd.no_wheat", "guard.no_bow", "guard.no_arrows", "guard.post_unreachable", "fletcher.no_order", "fletcher.no_material", "fletcher.table_unreachable", "apothecary.no_order", "apothecary.no_recipe", "apothecary.no_material", "apothecary.no_fuel", "apothecary.no_water", "apothecary.stand_unreachable", "tanner.no_order", "tanner.no_material", "tanner.no_water", "tanner.cauldron_unreachable", "fisher.no_rod", "fisher.no_water", "fisher.water_unreachable", "butcher.no_weapon", "butcher.no_animals", "butcher.no_feed", "butcher.smoker_unreachable",
		"chests_full", "chest_unreachable", "saw.station_unreachable", "carpenter.bench_unreachable", "smith.furnace_unreachable",
		"saw.no_material", "carpenter.no_recipe", "carpenter.waiting_for", "carpenter.inventory_full", "smith.no_ore", "smith.no_fuel", "smith.no_material", "smith.table_unreachable",
		"kiln.no_ore", "kiln.no_fuel", "kiln.furnace_unreachable", "shop.no_goods", "shop.no_buyer",
		"pit_digger.no_shovel", "pit_digger.no_pickaxe", "pit_digger.no_ladders", "pit_digger.no_pit", "pit_digger.ring_broken",
		"pit_digger.blocked_liquid", "pit_digger.blocked_tool", "pit_digger.unreachable", "pit_digger.ladder_blocked", "pit_digger.not_loaded",
		"miner.no_tunnel", "miner.not_loaded", "miner.blocked", "miner.blocked_tool", "miner.no_seal_blocks", "miner.unreachable"
	);

	private WorkerStatus() {
	}

	/**
	 * Sets the current work step, e.g. {@code task(villager, "lumberjack.chop", 40)}. For the
	 * {@link #PROBLEMS} the worker also shakes its head at players nearby, see {@link #complain}.
	 */
	public static void task(Villager villager, String key, Object... args) {
		((HiredVillager) villager).workingvillagers$setTask(Component.translatable(PREFIX + key, args));
		((HiredVillager) villager).workingvillagers$setProblem(PROBLEMS.contains(key));
	}

	/** A status text, e.g. why a worker waits. */
	public static Component component(String key, Object... args) {
		return translatable(key, args);
	}

	public static void clear(Villager villager) {
		((HiredVillager) villager).workingvillagers$setTask(null);
		((HiredVillager) villager).workingvillagers$setProblem(false);
	}

	/**
	 * Called every {@code interval} ticks: a worker that cannot work shakes its head now and
	 * then while a player is close, like a villager that does not want to trade.
	 */
	static void complain(ServerLevel level, Villager villager, int interval) {
		HiredVillager hired = (HiredVillager) villager;
		// No task at work: the work cannot start (no chest linked, no order)
		boolean problem = villager.getBrain().isActive(Activity.WORK)
			&& (hired.workingvillagers$hasProblem() || hired.workingvillagers$getTask() == null || hired.workingvillagers$getStuckWatch().isStuck());
		if (!problem || villager.tickCount % COMPLAIN_TICKS >= interval || villager.isSleeping()) {
			return;
		}
		Player player = level.getNearestPlayer(villager, COMPLAIN_DISTANCE);
		if (player != null && !player.isSpectator()) {
			villager.setUnhappyCounter(40);
			villager.makeSound(SoundEvents.VILLAGER_NO);
			villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(player, true));
		}
	}

	public static Component describe(Villager villager) {
		Brain<Villager> brain = villager.getBrain();
		if (villager.isSleeping()) {
			return translatable("sleeping");
		}
		if (brain.isActive(Activity.PANIC)) {
			return translatable("panic");
		}
		if (brain.isActive(Activity.HIDE) || brain.isActive(Activity.PRE_RAID) || brain.isActive(Activity.RAID)) {
			return translatable("raid");
		}
		Optional<LivingEntity> partner = brain.getMemory(MemoryModuleType.INTERACTION_TARGET);
		if (partner.isPresent() && partner.get() instanceof Villager other && !brain.isActive(Activity.WORK)) {
			return translatable("chatting_with", other.getName());
		}
		if (brain.isActive(Activity.WORK)) {
			if (((HiredVillager) villager).workingvillagers$getStuckWatch().isStuck()) {
				return translatable("stuck");
			}
			Component task = ((HiredVillager) villager).workingvillagers$getTask();
			return task != null ? task : translatable("no_work");
		}
		if (brain.isActive(Activity.REST)) {
			return translatable("going_to_bed");
		}
		if (brain.isActive(Activity.MEET)) {
			return translatable("meeting");
		}
		return translatable("free_time");
	}

	private static Component translatable(String key, Object... args) {
		return Component.translatable(PREFIX + key, args);
	}
}
