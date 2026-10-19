package me.teajay.workingvillagers.job;

import java.util.Map;
import me.teajay.workingvillagers.WorkingVillagers;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.jspecify.annotations.Nullable;

/**
 * Which workers of this mod trade like which vanilla villagers: the farmer like the farmer,
 * the stone sawyer like the mason, the shepherd, the butcher and the fisher like theirs, the smith like
 * the armorer (at the blast furnace) or the toolsmith (at the smithing table). Professions of
 * one family keep their trades when a villager changes between them, e.g. a vanilla farmer
 * that is hired and becomes the farmer of this mod.
 */
public final class TradeFamilies {
	private static final Map<ResourceKey<VillagerProfession>, String> FAMILIES = Map.ofEntries(
		Map.entry(VillagerProfession.FARMER, "farmer"),
		Map.entry(WorkingVillagers.FARMER_PROFESSION, "farmer"),
		Map.entry(VillagerProfession.MASON, "mason"),
		Map.entry(WorkingVillagers.STONECUTTER_PROFESSION, "mason"),
		Map.entry(VillagerProfession.SHEPHERD, "shepherd"),
		Map.entry(WorkingVillagers.SHEPHERD_PROFESSION, "shepherd"),
		Map.entry(VillagerProfession.BUTCHER, "butcher"),
		Map.entry(WorkingVillagers.BUTCHER_PROFESSION, "butcher"),
		Map.entry(VillagerProfession.FISHERMAN, "fisherman"),
		Map.entry(WorkingVillagers.FISHER_PROFESSION, "fisherman"),
		Map.entry(VillagerProfession.FLETCHER, "fletcher"),
		Map.entry(WorkingVillagers.FLETCHER_PROFESSION, "fletcher"),
		Map.entry(VillagerProfession.CLERIC, "cleric"),
		Map.entry(WorkingVillagers.APOTHECARY_PROFESSION, "cleric"),
		Map.entry(VillagerProfession.LEATHERWORKER, "leatherworker"),
		Map.entry(WorkingVillagers.TANNER_PROFESSION, "leatherworker"),
		Map.entry(VillagerProfession.WEAPONSMITH, "weaponsmith"),
		Map.entry(WorkingVillagers.WEAPONSMITH_PROFESSION, "weaponsmith"),
		Map.entry(VillagerProfession.LIBRARIAN, "librarian"),
		Map.entry(WorkingVillagers.LIBRARIAN_PROFESSION, "librarian"),
		Map.entry(VillagerProfession.ARMORER, "smith"),
		Map.entry(VillagerProfession.TOOLSMITH, "smith"),
		Map.entry(WorkingVillagers.SMITH_PROFESSION, "smith")
	);

	private TradeFamilies() {
	}

	/** The family of the profession, null if it has none (a worker that trades with nobody, a librarian). */
	public static @Nullable String of(Holder<VillagerProfession> profession) {
		return profession.unwrapKey().map(FAMILIES::get).orElse(null);
	}

	/** Whether a villager changing from one profession to the other keeps its trades. */
	public static boolean keepsTrades(Holder<VillagerProfession> from, Holder<VillagerProfession> to) {
		String family = of(from);
		return family != null && family.equals(of(to));
	}
}
