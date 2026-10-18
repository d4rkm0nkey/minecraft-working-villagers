package me.teajay.workingvillagers.station;

import java.util.Set;
import net.minecraft.world.entity.Display;

/**
 * The paper notes with the worker's name that earlier versions put on the front of the
 * workstations: vanilla text displays tagged {@code workingvillagers_note}, or
 * {@code letmework_note} from before the mod was renamed. They are gone; a note left in a
 * world removes itself the first time it ticks (see {@code DisplayMixin}).
 *
 * <p>To be removed (with {@code DisplayMixin}) in version 0.5.0: the notes were dropped after
 * 0.3.0, two releases are time enough for worlds to be cleaned up.
 */
public final class StationNotes {
	private static final Set<String> TAGS = Set.of("workingvillagers_note", "letmework_note");

	private StationNotes() {
	}

	/** Whether the display is a note of an earlier version. */
	public static boolean isOldNote(Display display) {
		return display.entityTags().stream().anyMatch(TAGS::contains);
	}
}
