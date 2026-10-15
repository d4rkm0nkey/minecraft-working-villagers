package me.teajay.workingvillagers.job;

import java.util.List;
import me.teajay.workingvillagers.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * Hired villagers can get a name ({@link Config#RANDOM_NAMES}), so the player can tell them
 * apart and find the name again on the note at their workstation.
 */
public final class WorkerNames {
	private static final List<String> NAMES = List.of(
		"Anton", "Berta", "Carl", "Dora", "Emil", "Frieda", "Gustav", "Hanna", "Ida", "Jakob",
		"Klara", "Ludwig", "Martha", "Norbert", "Olga", "Paul", "Rosa", "Sepp", "Thea", "Ulrich",
		"Vera", "Walter", "Wilma", "Alma", "Bruno", "Erna", "Fritz", "Greta", "Hugo", "Ilse",
		"Kurt", "Lotte", "Max", "Nora", "Otto", "Paula", "Rudi", "Selma", "Theo", "Zita"
	);

	private WorkerNames() {
	}

	/**
	 * Gives the villager a random name unless it already has one, if names are switched on
	 * ({@link Config#RANDOM_NAMES}; vanilla villagers have none, a name tag gives one).
	 * Returns its name, empty if it has none.
	 */
	public static String ensureName(Villager villager) {
		if (!villager.hasCustomName() && Config.RANDOM_NAMES.get()) {
			villager.setCustomName(Component.literal(NAMES.get(villager.getRandom().nextInt(NAMES.size()))));
		}
		return villager.hasCustomName() ? villager.getCustomName().getString() : "";
	}
}
