package me.teajay.workingvillagers.job;

import java.util.function.Consumer;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.mixin.VillagerInvoker;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * The levels of the workers, the vanilla villager levels from novice to master: workers rise
 * by working (and by trading, like vanilla villagers), and a higher level makes them faster,
 * spares their tools, widens their work area, unlocks work (see {@link Unlocks}) and costs
 * more wage. The numbers grow evenly from the novice's to the master's.
 */
public final class WorkerLevels {
	public static final int NOVICE = VillagerData.MIN_VILLAGER_LEVEL;
	public static final int APPRENTICE = NOVICE + 1;
	public static final int JOURNEYMAN = NOVICE + 2;
	public static final int EXPERT = NOVICE + 3;
	public static final int MASTER = VillagerData.MAX_VILLAGER_LEVEL;

	private WorkerLevels() {
	}

	/** From 0 for a novice to 1 for a master. */
	public static float progress(int level) {
		return Math.clamp((level - NOVICE) / (float) (MASTER - NOVICE), 0.0F, 1.0F);
	}

	public static float progress(Villager villager) {
		return progress(villager.getVillagerData().level());
	}

	/** The value for the worker's level, evenly between the novice's and the master's. */
	public static float between(Villager villager, float novice, float master) {
		return novice + (master - novice) * progress(villager);
	}

	/** How much of a novice's time a step of the work takes the worker: 1 for a novice. */
	public static float timeShare(Villager villager) {
		return between(villager, 1.0F, (float) Config.MASTER_TIME.get());
	}

	/** The ticks a step of the work takes the worker that takes a novice {@code noviceTicks}, at least 1. */
	public static int ticks(Villager villager, int noviceTicks) {
		return Math.max(1, Math.round(noviceTicks * timeShare(villager)));
	}

	/** The radius (or length) of a work area for the level: {@code master} for a master, a share of it for the others. */
	public static int area(int level, int master) {
		float novice = master * Config.NOVICE_AREA.get() / 100.0F;
		return Math.max(1, Math.round(novice + (master - novice) * progress(level)));
	}

	public static int area(Villager villager, int master) {
		return area(villager.getVillagerData().level(), master);
	}

	/** The level of the worker at the workstation, a novice's if it has none (or it is not loaded). */
	public static int levelAt(ServerLevel level, BlockPos station) {
		return Workstations.find(level, station)
			.map(Workstations.Station::worker)
			.map(level::getEntity)
			.filter(Villager.class::isInstance)
			.map(entity -> ((Villager) entity).getVillagerData().level())
			.orElse(NOVICE);
	}

	/**
	 * Wears the tool by {@code amount} uses, like {@link ItemStack#hurtAndBreak}, but each use
	 * is spared with the worker's chance (none for a novice, see {@link Config#MASTER_TOOL_SAVING}).
	 */
	public static void wear(ServerLevel level, Villager villager, ItemStack tool, int amount, Consumer<ItemStack> onBroken) {
		float chance = between(villager, 0.0F, Config.MASTER_TOOL_SAVING.get() / 100.0F);
		int uses = 0;
		for (int i = 0; i < amount; i++) {
			if (chance <= 0.0F || villager.getRandom().nextFloat() >= chance) {
				uses++;
			}
		}
		if (uses > 0) {
			tool.hurtAndBreak(uses, level, null, onBroken);
		}
	}

	/** The emeralds a worker of the level takes per work day: from the novice's to the master's, rounded down. */
	public static int dailyWage(int level) {
		int novice = Config.DAILY_WAGE.get();
		int master = Math.max(novice, Config.MASTER_DAILY_WAGE.get());
		return (int) Math.floor(novice + (master - novice) * progress(level) + 1.0E-4);
	}

	/** The worker's wage per work day, with the fee for working without a player nearby (see {@link WorkerChunks#fee}). */
	public static int dailyWage(Villager villager) {
		return dailyWage(villager.getVillagerData().level()) + WorkerChunks.fee(villager);
	}

	/** The name of the level, e.g. "Journeyman". */
	public static Component name(int level) {
		return Component.translatable("merchant.level." + Math.clamp(level, NOVICE, MASTER));
	}

	/**
	 * Called every {@code interval} ticks for a worker: while it works (and does not wait, see
	 * {@link HiredVillager#workingvillagers$isWorking}) it gathers experience, faster with a
	 * master of its profession nearby, and rises a level once it has enough.
	 */
	static void tick(ServerLevel level, Villager villager, int interval) {
		HiredVillager hired = (HiredVillager) villager;
		int current = villager.getVillagerData().level();
		if (current >= MASTER || !villager.getBrain().isActive(Activity.WORK) || !hired.workingvillagers$isWorking()
			|| hired.workingvillagers$getStuckWatch().isStuck()) {
			return;
		}
		float seconds = interval / 20.0F * learningFactor(level, villager);
		float learned = hired.workingvillagers$getLearned() + seconds;
		int per = Config.SECONDS_PER_EXPERIENCE.get();
		int points = (int) (learned / per);
		hired.workingvillagers$setLearned(learned - points * per);
		if (points > 0) {
			addExperience(level, villager, points);
		}
	}

	/** Adds villager experience, like a trade, and rises a level once there is enough. */
	public static void addExperience(ServerLevel level, Villager villager, int points) {
		villager.setVillagerXp(villager.getVillagerXp() + points);
		int current = villager.getVillagerData().level();
		if (VillagerData.canLevelUp(current) && villager.getVillagerXp() >= VillagerData.getMaxXpPerLevel(current)) {
			((VillagerInvoker) villager).workingvillagers$increaseMerchantCareer(level);
			// Happy particles and a cheer, like after a trade that brings a level
			level.broadcastEntityEvent(villager, (byte) 14);
			villager.makeSound(SoundEvents.VILLAGER_CELEBRATE);
		}
	}

	/** How fast the worker learns: {@link Config#MENTOR_FACTOR} times as fast with a master of its profession nearby, else 1. */
	public static float learningFactor(ServerLevel level, Villager villager) {
		return villager.getVillagerData().level() < MASTER && hasMentor(level, villager) ? (float) Config.MENTOR_FACTOR.get() : 1.0F;
	}

	/** Whether a hired master of the same profession is within {@link Config#MENTOR_RADIUS}. */
	private static boolean hasMentor(ServerLevel level, Villager villager) {
		AABB area = villager.getBoundingBox().inflate(Config.MENTOR_RADIUS.get());
		return !level.getEntitiesOfClass(Villager.class, area, other -> other != villager
			&& other.getVillagerData().level() >= MASTER
			&& other.getVillagerData().profession().equals(villager.getVillagerData().profession())
			&& ((HiredVillager) other).workingvillagers$isHired()
		).isEmpty();
	}
}
