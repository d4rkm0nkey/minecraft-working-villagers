package me.teajay.workingvillagers.job;

import java.util.Optional;
import net.minecraft.network.chat.Component;
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

	private WorkerStatus() {
	}

	/** Sets the current work step, e.g. {@code task(villager, "lumberjack.chop", 40)}. */
	public static void task(Villager villager, String key, Object... args) {
		((HiredVillager) villager).workingvillagers$setTask(Component.translatable(PREFIX + key, args));
	}

	/** A status text, e.g. why a worker waits. */
	public static Component component(String key, Object... args) {
		return translatable(key, args);
	}

	public static void clear(Villager villager) {
		((HiredVillager) villager).workingvillagers$setTask(null);
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
