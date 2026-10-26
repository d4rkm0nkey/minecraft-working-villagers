package me.teajay.workingvillagers.job;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import me.teajay.workingvillagers.carrier.CarrierData;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Implemented by every villager (via mixin): who hired it with a work contract, and the
 * state of its work that has to survive a restart.
 */
public interface HiredVillager {
	@Nullable UUID workingvillagers$getEmployer();

	void workingvillagers$setEmployer(@Nullable UUID employer);

	default boolean workingvillagers$isHired() {
		return this.workingvillagers$getEmployer() != null;
	}

	/**
	 * How much the worker has done on the given day (e.g. logs chopped). Starts at 0 on
	 * every new day. Adding work takes the wage of the day (see {@link WorkerSupplies#earned}).
	 */
	int workingvillagers$getWorkDone(long day);

	void workingvillagers$addWorkDone(long day, int amount);

	/**
	 * Where a lumberjack still has to plant saplings (see {@link ChopTrees.Planting}), the
	 * oldest first. Changed in place.
	 */
	List<ChopTrees.Planting> workingvillagers$getPlantings();

	/** Routes and basket of a carrier, null for every other villager. */
	@Nullable CarrierData workingvillagers$getCarrier();

	void workingvillagers$setCarrier(@Nullable CarrierData carrier);

	/**
	 * What the worker is doing at work right now, set by its work behavior; null when it
	 * is not working. Not saved.
	 */
	@Nullable Component workingvillagers$getTask();

	void workingvillagers$setTask(@Nullable Component task);

	/**
	 * On the client: the tick count of the villager when it was last seen swinging, so that a
	 * worker keeps its arms apart between two strokes (see {@code WorkMotion}). Not saved.
	 */
	int workingvillagers$getLastSwing();

	void workingvillagers$setLastSwing(int tick);

	/**
	 * Whether the current task is a problem the player has to solve (no wage, no tool, full
	 * chests, ...). Not saved.
	 */
	boolean workingvillagers$hasProblem();

	void workingvillagers$setProblem(boolean problem);

	/**
	 * Whether the current task is work that teaches (not waiting, not a problem), see
	 * {@link WorkerLevels#tick}. Not saved.
	 */
	boolean workingvillagers$isWorking();

	void workingvillagers$setWorking(boolean working);

	/** The seconds of work not yet turned into villager experience (see {@link WorkerLevels#tick}). */
	float workingvillagers$getLearned();

	void workingvillagers$setLearned(float seconds);

	/**
	 * The profession the worker learned its level in, null if it never had a job of this
	 * mod. A worker that takes another job starts as a novice (see {@link WorkerJobs#takeJob}).
	 */
	@Nullable String workingvillagers$getCareer();

	void workingvillagers$setCareer(@Nullable String career);

	/** A guard, a weaponsmith: the experience it has not bottled yet. */
	int workingvillagers$getExperience();

	void workingvillagers$setExperience(int experience);

	/**
	 * Whether the emeralds the worker carried before there were wage boxes are in the wage box
	 * of its workstation (see {@link WorkerSupplies#storeCarriedWage}). True for every worker
	 * hired since; false for workers saved by 0.3.0 until they have done it.
	 */
	boolean workingvillagers$isWageStored();

	void workingvillagers$setWageStored(boolean stored);

	/** The last day the worker took its wage, -1 if never. */
	long workingvillagers$getPaidDay();

	void workingvillagers$setPaidDay(long day);

	/**
	 * The day the villager was hired for the first time, -1 if never (or before this was
	 * kept). See {@link WorkerSupplies#onHired}.
	 */
	long workingvillagers$getHiredDay();

	void workingvillagers$setHiredDay(long day);

	/** The sign above the worker's head, see {@link WorkerNotice}. Not saved. */
	byte workingvillagers$getNotice();

	void workingvillagers$setNotice(byte notice);

	/** Until when (tick count) the worker shows that an order is done, see {@link WorkerNotice#done}. Not saved. */
	int workingvillagers$getDoneUntil();

	void workingvillagers$setDoneUntil(int tick);

	/** The worker's purse, hunger and lunch bag, see {@link Meals}. */
	MealData workingvillagers$getMeals();

	/** Whether the worker is stuck, see {@link StuckWorkers}. Not saved. */
	StuckWorkers.Watch workingvillagers$getStuckWatch();

	/** The container the worker has open right now, so its lid stays open. */
	void workingvillagers$setOpenedContainer(@Nullable BlockPos pos);
}
