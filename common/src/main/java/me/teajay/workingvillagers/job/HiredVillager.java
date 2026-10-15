package me.teajay.workingvillagers.job;

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
	 * every new day.
	 */
	int workingvillagers$getWorkDone(long day);

	void workingvillagers$addWorkDone(long day, int amount);

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
	 * Whether the current task is a problem the player has to solve (no wage, no tool, full
	 * chests, ...). Not saved.
	 */
	boolean workingvillagers$hasProblem();

	void workingvillagers$setProblem(boolean problem);

	/** The last day the worker took its wage, -1 if never. */
	long workingvillagers$getPaidDay();

	void workingvillagers$setPaidDay(long day);

	/** Whether the worker is stuck, see {@link StuckWorkers}. Not saved. */
	StuckWorkers.Watch workingvillagers$getStuckWatch();

	/** The container the worker has open right now, so its lid stays open. */
	void workingvillagers$setOpenedContainer(@Nullable BlockPos pos);
}
