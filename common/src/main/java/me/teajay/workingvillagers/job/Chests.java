package me.teajay.workingvillagers.job;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * Moving items between a worker's inventory and the chests linked to its workstation.
 */
final class Chests {
	private Chests() {
	}

	/** Villagers keep their food (for breeding), workers hand in everything else. */
	static boolean isNotFood(ItemStack stack) {
		return !stack.isEmpty() && !stack.has(DataComponents.VILLAGER_FOOD);
	}

	static Optional<BlockPos> closest(List<BlockPos> chests, Villager villager) {
		return chests.stream().min(Comparator.comparingDouble(pos -> pos.distToCenterSqr(villager.position())));
	}

	/**
	 * Puts the matching items of the inventory into the container. Returns whether everything fit.
	 */
	static boolean deposit(SimpleContainer inventory, Container container, Predicate<ItemStack> filter) {
		boolean allStored = true;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.isEmpty() || !filter.test(stack)) {
				continue;
			}
			ItemStack rest = HopperBlockEntity.addItem(null, container, stack.copy(), null);
			inventory.setItem(slot, rest);
			allStored &= rest.isEmpty();
		}
		return allStored;
	}

	/**
	 * Takes up to {@code max} matching items out of the container into the inventory. Returns
	 * how many were taken.
	 */
	static int take(Container container, SimpleContainer inventory, Predicate<ItemStack> filter, int max) {
		int taken = 0;
		for (int slot = 0; slot < container.getContainerSize() && taken < max; slot++) {
			ItemStack stack = container.getItem(slot);
			if (stack.isEmpty() || !filter.test(stack)) {
				continue;
			}
			ItemStack wanted = stack.copyWithCount(Math.min(stack.getCount(), max - taken));
			ItemStack rest = inventory.addItem(wanted);
			int moved = wanted.getCount() - rest.getCount();
			if (moved == 0) {
				break; // inventory full
			}
			stack.shrink(moved);
			taken += moved;
		}
		return taken;
	}

	static boolean contains(ServerLevel level, BlockPos pos, Predicate<ItemStack> filter) {
		Container container = Workstations.getContainer(level, pos);
		// hasAnyMatching also tests the empty slots
		return container != null && container.hasAnyMatching(stack -> !stack.isEmpty() && filter.test(stack));
	}

	static int emptySlots(ServerLevel level, BlockPos pos) {
		Container container = Workstations.getContainer(level, pos);
		if (container == null) {
			return 0;
		}
		int empty = 0;
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			if (container.getItem(slot).isEmpty()) {
				empty++;
			}
		}
		return empty;
	}

	static boolean hasRoom(ServerLevel level, BlockPos pos) {
		Container container = Workstations.getContainer(level, pos);
		if (container == null) {
			return false;
		}
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot);
			if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
				return true;
			}
		}
		return false;
	}
}
