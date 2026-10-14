package me.teajay.workingvillagers.job;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * Hired villagers get a name, so the player can tell them apart and find the name again on
 * the note at their workstation.
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
	 * Gives the villager a random name unless it already has one. Returns its name.
	 */
	public static String ensureName(Villager villager) {
		if (!villager.hasCustomName()) {
			villager.setCustomName(Component.literal(NAMES.get(villager.getRandom().nextInt(NAMES.size()))));
		}
		return villager.getCustomName().getString();
	}
}
