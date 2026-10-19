package me.teajay.workingvillagers.job;

import java.util.List;
import me.teajay.workingvillagers.WorkingVillagers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.jspecify.annotations.Nullable;

/**
 * The armor of the guards: a guard puts on armor it is given or finds in a village chest, one
 * piece for each slot. Armor wears out like a player's (see {@code LivingEntityMixin}); a piece
 * about to break it takes off and gives back like a worn tool, so the smith can repair it.
 * Dyed leather armor colors the guard's tabard (see {@code UniformLayer}): the uniform of the
 * village, dyed by the tanner.
 */
public final class Uniforms {
	public static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

	private Uniforms() {
	}

	/** Whether the worker wears armor: only guards, the others do not fight. */
	public static boolean wearsArmor(Villager villager) {
		return villager.getVillagerData().profession().is(WorkingVillagers.GUARD_PROFESSION);
	}

	private static TagKey<Item> tag(EquipmentSlot slot) {
		return switch (slot) {
			case HEAD -> ItemTags.HEAD_ARMOR;
			case CHEST -> ItemTags.CHEST_ARMOR;
			case LEGS -> ItemTags.LEG_ARMOR;
			default -> ItemTags.FOOT_ARMOR;
		};
	}

	/** The slot the armor is worn in, null if the stack is no armor. */
	public static @Nullable EquipmentSlot slotOf(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		for (EquipmentSlot slot : SLOTS) {
			if (stack.is(tag(slot))) {
				return slot;
			}
		}
		return null;
	}

	public static boolean isArmor(ItemStack stack) {
		return slotOf(stack) != null;
	}

	/** Whether the guard has no armor for the slot, neither worn nor carried (only worn out ones). */
	public static boolean lacks(Villager villager, EquipmentSlot slot) {
		ItemStack worn = villager.getItemBySlot(slot);
		if (!worn.isEmpty() && !WorkerSupplies.isWorn(worn)) {
			return false;
		}
		return !villager.getInventory().hasAnyMatching(stack -> slotOf(stack) == slot && !WorkerSupplies.isWorn(stack));
	}

	/**
	 * Takes off armor about to break (into the inventory, to give back) and puts on armor it
	 * carries where nothing is worn.
	 */
	public static void equip(ServerLevel level, Villager villager) {
		if (!wearsArmor(villager)) {
			return;
		}
		SimpleContainer inventory = villager.getInventory();
		for (EquipmentSlot slot : SLOTS) {
			ItemStack worn = villager.getItemBySlot(slot);
			if (!worn.isEmpty() && WorkerSupplies.isWorn(worn) && inventory.canAddItem(worn)) {
				inventory.addItem(worn);
				villager.setItemSlot(slot, ItemStack.EMPTY);
				worn = ItemStack.EMPTY;
			}
			if (!worn.isEmpty()) {
				continue;
			}
			for (int i = 0; i < inventory.getContainerSize(); i++) {
				ItemStack stack = inventory.getItem(i);
				if (slotOf(stack) == slot && !WorkerSupplies.isWorn(stack)) {
					villager.setItemSlot(slot, stack.split(1));
					inventory.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
					// It drops whole when the guard dies, like the rest of its things
					villager.setDropChance(slot, 2.0F);
					Equippable equippable = villager.getItemBySlot(slot).get(DataComponents.EQUIPPABLE);
					if (equippable != null) {
						level.playSound(null, villager, equippable.equipSound().value(), SoundSource.NEUTRAL, 1.0F, 1.0F);
					}
					break;
				}
			}
		}
	}
}
