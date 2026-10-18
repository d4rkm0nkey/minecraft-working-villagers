package me.teajay.workingvillagers.menu;

import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.DataSlot;

/**
 * A workstation's own menu that tells its screen whether a hired worker works there, for the
 * face on the button to the overview (see {@code WorkerButton}): the blast furnace, the
 * smithing table and the kiln. The server fills a data slot when the menu opens.
 */
public interface StationWorker {
	boolean workingvillagers$hasWorker();

	/** A data slot that is 1 while a hired worker works at the station at {@code pos}. */
	static DataSlot slot(ServerLevel level, BlockPos pos) {
		DataSlot slot = DataSlot.standalone();
		slot.set(Workstations.findSetUp(level, pos).map(station -> station.worker() != null).orElse(false) ? 1 : 0);
		return slot;
	}
}
