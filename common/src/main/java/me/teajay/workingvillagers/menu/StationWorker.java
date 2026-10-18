package me.teajay.workingvillagers.menu;

/**
 * The menu of a workstation block (woodcutter, stonecutter, mill, carpenter's bench, kiln,
 * blast furnace, smithing table, loom), which tells its screen what the tabs above it show:
 * the {@link StationTabFlags} of the station, synced in a data slot.
 */
public interface StationWorker {
	int workingvillagers$tabFlags();
}
