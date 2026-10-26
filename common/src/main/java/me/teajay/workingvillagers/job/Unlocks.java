package me.teajay.workingvillagers.job;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * What a worker can do only from a level on (see {@link WorkerLevels}). Products and materials
 * are in item tags per profession and level, {@code workingvillagers:unlocks/<profession>/<level>}
 * (e.g. {@code unlocks/woodcutter/journeyman} for signs), so datapacks can change them; the
 * other work is an {@link Ability}. A novice can do whatever keeps the village going. With
 * {@link Config#UNLOCKS} off every worker can do everything.
 */
public final class Unlocks {
	private static final String[] LEVEL_NAMES = {"novice", "apprentice", "journeyman", "expert", "master"};
	/** How many items of the next level the menu names. */
	private static final int SHOWN_ITEMS = 4;

	/** Work that is no product or material. */
	public enum Ability {
		BIG_TREES(WorkingVillagers.LUMBERJACK_PROFESSION, WorkerLevels.JOURNEYMAN),
		BREEDING(WorkingVillagers.SHEPHERD_PROFESSION, WorkerLevels.APPRENTICE),
		SMOKING(WorkingVillagers.BUTCHER_PROFESSION, WorkerLevels.JOURNEYMAN),
		BOAT(WorkingVillagers.FISHER_PROFESSION, WorkerLevels.JOURNEYMAN),
		STAIRS(WorkingVillagers.MINER_PROFESSION, WorkerLevels.APPRENTICE),
		BRANCHES(WorkingVillagers.MINER_PROFESSION, WorkerLevels.JOURNEYMAN),
		FOLLOW_ORES(WorkingVillagers.MINER_PROFESSION, WorkerLevels.EXPERT),
		HUNTING(WorkingVillagers.GUARD_PROFESSION, WorkerLevels.JOURNEYMAN),
		SECOND_ROUTE(WorkingVillagers.CARRIER_PROFESSION, WorkerLevels.APPRENTICE),
		THIRD_ROUTE(WorkingVillagers.CARRIER_PROFESSION, WorkerLevels.JOURNEYMAN),
		DISENCHANTING(WorkingVillagers.WEAPONSMITH_PROFESSION, WorkerLevels.JOURNEYMAN),
		ENCHANTED_REPAIR(WorkingVillagers.SMITH_PROFESSION, WorkerLevels.JOURNEYMAN);

		private final ResourceKey<VillagerProfession> profession;
		private final int level;

		Ability(ResourceKey<VillagerProfession> profession, int level) {
			this.profession = profession;
			this.level = level;
		}

		public int level() {
			return this.level;
		}

		public Component displayName() {
			return Component.translatable("unlock.workingvillagers." + this.name().toLowerCase(java.util.Locale.ROOT));
		}
	}

	private Unlocks() {
	}

	/** Whether the worker can do that already. */
	public static boolean can(Villager villager, Ability ability) {
		return !Config.UNLOCKS.get() || villager.getVillagerData().level() >= ability.level;
	}

	/** The tag of what a worker of the profession makes or uses only from the level on. */
	public static TagKey<Item> tag(ResourceKey<VillagerProfession> profession, int level) {
		return TagKey.create(Registries.ITEM, WorkingVillagers.id("unlocks/" + profession.identifier().getPath() + "/" + LEVEL_NAMES[level - WorkerLevels.NOVICE]));
	}

	/** From which level a worker of the profession makes or uses the item (a novice if it is in none of the tags). */
	public static int level(ResourceKey<VillagerProfession> profession, Item item) {
		if (!Config.UNLOCKS.get()) {
			return WorkerLevels.NOVICE;
		}
		Holder<Item> holder = BuiltInRegistries.ITEM.wrapAsHolder(item);
		for (int level = WorkerLevels.MASTER; level > WorkerLevels.NOVICE; level--) {
			if (holder.is(tag(profession, level))) {
				return level;
			}
		}
		return WorkerLevels.NOVICE;
	}

	/** From which level a worker of the profession makes or uses all of the items. */
	public static int level(ResourceKey<VillagerProfession> profession, Collection<Item> items) {
		int level = WorkerLevels.NOVICE;
		for (Item item : items) {
			level = Math.max(level, level(profession, item));
		}
		return level;
	}

	/** Whether the worker makes or uses the item already (always for a worker without a profession of this mod). */
	public static boolean can(Villager villager, Item item) {
		return profession(villager).map(key -> villager.getVillagerData().level() >= level(key, item)).orElse(true);
	}

	public static boolean can(Villager villager, ItemStack stack) {
		return stack.isEmpty() || can(villager, stack.getItem());
	}

	/** Whether the worker makes or uses all of the items already. */
	public static boolean canAll(Villager villager, Collection<Item> items) {
		return profession(villager).map(key -> villager.getVillagerData().level() >= level(key, items)).orElse(true);
	}

	/**
	 * From which level the worker makes the result from the ingredients: the level of the
	 * result, and for each ingredient that of the earliest item it takes (a novice for a worker
	 * without a profession of this mod).
	 */
	public static int level(Villager villager, Item result, List<Ingredient> ingredients) {
		return profession(villager).map(key -> level(key, result, ingredients)).orElse(WorkerLevels.NOVICE);
	}

	public static int level(ResourceKey<VillagerProfession> profession, Item result, List<Ingredient> ingredients) {
		int level = level(profession, result);
		for (Ingredient ingredient : ingredients) {
			level = Math.max(level, ingredient.items().mapToInt(item -> level(profession, item.value())).min().orElse(WorkerLevels.NOVICE));
		}
		return level;
	}

	/** Whether the worker makes the result from the ingredients already. */
	public static boolean canMake(Villager villager, Item result, List<Ingredient> ingredients) {
		return villager.getVillagerData().level() >= level(villager, result, ingredients);
	}

	/** Lectern: the highest level the librarian enchants at, from 10 for a novice to 30 for a master. */
	public static int enchantingLevel(Villager villager) {
		return Config.UNLOCKS.get() ? Math.round(WorkerLevels.between(villager, 10.0F, 30.0F)) : 30;
	}

	/** The status of a worker whose order it cannot do yet: "Waits: can do that as a journeyman". */
	public static Component lockedStatus(int level) {
		return Component.translatable("status.workingvillagers.locked", WorkerLevels.name(level));
	}

	/** The tooltip line of something the worker can do only later: "From journeyman". */
	public static Component from(int level) {
		return Component.translatable("gui.workingvillagers.unlock.from", WorkerLevels.name(level));
	}

	/**
	 * What the worker's next level brings: its abilities and a few of the products and
	 * materials of the next level's tag. Empty for a master or when nothing comes.
	 */
	public static Optional<Component> next(Villager villager) {
		int next = villager.getVillagerData().level() + 1;
		Optional<ResourceKey<VillagerProfession>> profession = profession(villager);
		if (!Config.UNLOCKS.get() || next > WorkerLevels.MASTER || profession.isEmpty()) {
			return Optional.empty();
		}
		List<Component> parts = new ArrayList<>();
		for (Ability ability : Ability.values()) {
			if (ability.profession.equals(profession.get()) && ability.level == next) {
				parts.add(ability.displayName());
			}
		}
		if (profession.get().equals(WorkingVillagers.LIBRARIAN_PROFESSION)) {
			parts.add(Component.translatable("unlock.workingvillagers.enchanting_level", Math.round(10.0F + 20.0F * WorkerLevels.progress(next))));
		}
		List<Item> items = collect(tag(profession.get(), next));
		items.stream().limit(SHOWN_ITEMS).forEach(item -> parts.add(item.getDefaultInstance().getHoverName()));
		if (items.size() > SHOWN_ITEMS) {
			parts.add(Component.literal("…"));
		}
		if (parts.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(Component.translatable("gui.workingvillagers.unlock.next", WorkerLevels.name(next), ComponentUtils.formatList(parts, Component.literal(", "))));
	}

	private static List<Item> collect(TagKey<Item> tag) {
		List<Item> items = new ArrayList<>();
		BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(holder -> items.add(holder.value()));
		return items;
	}

	private static Optional<ResourceKey<VillagerProfession>> profession(Villager villager) {
		return villager.getVillagerData().profession().unwrapKey()
			.filter(key -> key.identifier().getNamespace().equals(WorkingVillagers.MOD_ID));
	}
}
