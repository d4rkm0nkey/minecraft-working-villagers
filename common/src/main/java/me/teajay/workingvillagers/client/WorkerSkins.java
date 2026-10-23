package me.teajay.workingvillagers.client;

import java.util.Map;
import me.teajay.workingvillagers.WorkingVillagers;
import net.minecraft.resources.Identifier;

/**
 * Workers with a job that vanilla villagers have too keep the clothes of that vanilla job: a
 * hired fisherman still looks like a fisherman. Only the jobs that are new in this mod (the
 * lumberjack, the miner, the carrier, ...) have clothes of their own.
 */
public final class WorkerSkins {
	private static final Map<Identifier, Identifier> VANILLA_LOOKS = Map.ofEntries(
		Map.entry(WorkingVillagers.FISHER_PROFESSION.identifier(), Identifier.withDefaultNamespace("fisherman")),
		Map.entry(WorkingVillagers.FARMER_PROFESSION.identifier(), Identifier.withDefaultNamespace("farmer")),
		Map.entry(WorkingVillagers.BUTCHER_PROFESSION.identifier(), Identifier.withDefaultNamespace("butcher")),
		Map.entry(WorkingVillagers.SHEPHERD_PROFESSION.identifier(), Identifier.withDefaultNamespace("shepherd")),
		Map.entry(WorkingVillagers.FLETCHER_PROFESSION.identifier(), Identifier.withDefaultNamespace("fletcher")),
		Map.entry(WorkingVillagers.LIBRARIAN_PROFESSION.identifier(), Identifier.withDefaultNamespace("librarian")),
		Map.entry(WorkingVillagers.WEAPONSMITH_PROFESSION.identifier(), Identifier.withDefaultNamespace("weaponsmith")),
		Map.entry(WorkingVillagers.TANNER_PROFESSION.identifier(), Identifier.withDefaultNamespace("leatherworker")),
		Map.entry(WorkingVillagers.APOTHECARY_PROFESSION.identifier(), Identifier.withDefaultNamespace("cleric")),
		Map.entry(WorkingVillagers.STONECUTTER_PROFESSION.identifier(), Identifier.withDefaultNamespace("mason")),
		// Blast furnace or smithing table: the armorer's apron fits both
		Map.entry(WorkingVillagers.SMITH_PROFESSION.identifier(), Identifier.withDefaultNamespace("armorer"))
	);

	private WorkerSkins() {
	}

	/** The profession whose clothes (and hat) a villager with this profession wears. */
	public static Identifier look(Identifier profession) {
		return VANILLA_LOOKS.getOrDefault(profession, profession);
	}
}
