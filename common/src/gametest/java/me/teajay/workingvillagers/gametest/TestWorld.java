package me.teajay.workingvillagers.gametest;

import java.util.function.Predicate;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.item.WorkContractItem;
import me.teajay.workingvillagers.job.HiredVillager;
import me.teajay.workingvillagers.job.WorkerJobs;
import me.teajay.workingvillagers.job.WorkerStatus;
import me.teajay.workingvillagers.job.WorkerSupplies;
import me.teajay.workingvillagers.station.Workstations;
import me.teajay.workingvillagers.station.Workstations.ChestRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Setting up the tests the way a player would: placing stations and chests, hiring with a
 * contract, assigning, linking chests and handing over wage and tools. All positions are
 * relative to the test structure.
 */
final class TestWorld {
	private TestWorld() {
	}

	/** An adult villager without a profession, hired with a work contract by a mock player. */
	static Villager hireVillager(GameTestHelper helper, BlockPos pos) {
		Villager villager = helper.spawn(EntityTypes.VILLAGER, pos);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack contract = new ItemStack(WorkingVillagers.WORK_CONTRACT.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, contract);
		WorkContractItem.hire(villager, player, contract, InteractionHand.MAIN_HAND);
		if (!((HiredVillager) villager).workingvillagers$isHired()) {
			throw helper.assertionException(pos, "The work contract did not hire the villager");
		}
		return villager;
	}

	/** Places a workstation and makes the villager work there. */
	static void assign(GameTestHelper helper, Villager villager, BlockPos station) {
		Component problem = WorkerJobs.assign(helper.getLevel(), villager, helper.absolutePos(station));
		if (problem != null) {
			throw helper.assertionException(station, "Could not assign the worker: " + problem.getString());
		}
	}

	static Workstations.Station station(GameTestHelper helper, BlockPos pos) {
		return Workstations.find(helper.getLevel(), helper.absolutePos(pos))
			.orElseThrow(() -> helper.assertionException(pos, "No workstation here"));
	}

	/** A chest facing south (towards +z), with the given items in it. */
	static BlockPos chest(GameTestHelper helper, BlockPos pos, ItemStack... items) {
		helper.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
		Container container = container(helper, pos);
		for (int i = 0; i < items.length; i++) {
			container.setItem(i, items[i]);
		}
		return pos;
	}

	/** A village chest with the given items in it. */
	static BlockPos villageChest(GameTestHelper helper, BlockPos pos, ItemStack... items) {
		helper.setBlock(pos, WorkingVillagers.VILLAGE_CHEST.get());
		Container container = container(helper, pos);
		for (int i = 0; i < items.length; i++) {
			container.setItem(i, items[i]);
		}
		return pos;
	}

	/**
	 * Empties and removes a village chest, so no worker of a test that runs later nearby
	 * takes from it.
	 */
	static void removeVillageChest(GameTestHelper helper, BlockPos pos) {
		container(helper, pos).clearContent();
		helper.setBlock(pos, Blocks.AIR);
	}

	static void link(GameTestHelper helper, BlockPos station, BlockPos chest, ChestRole role) {
		station(helper, station).setRole(helper.absolutePos(chest), role);
	}

	/** Gives the worker the items (wage, tool, seeds) the way a player does: right-click with them. */
	static void give(GameTestHelper helper, Villager villager, ItemStack... items) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		for (ItemStack item : items) {
			player.setItemInHand(InteractionHand.MAIN_HAND, item.copy());
			WorkerSupplies.give(villager, player, InteractionHand.MAIN_HAND);
			if (!player.getMainHandItem().isEmpty()) {
				throw helper.assertionException(villager.blockPosition(), "The worker did not take " + item);
			}
		}
	}

	static Container container(GameTestHelper helper, BlockPos pos) {
		Container container = Workstations.getContainer(helper.getLevel(), helper.absolutePos(pos));
		if (container == null) {
			throw helper.assertionException(pos, "No container here");
		}
		return container;
	}

	static int count(GameTestHelper helper, BlockPos pos, Predicate<ItemStack> filter) {
		Container container = container(helper, pos);
		int count = 0;
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot);
			if (!stack.isEmpty() && filter.test(stack)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	/**
	 * Fails unless the container holds at least {@code min} matching items. The message says
	 * what the worker is doing, which usually tells why it did not deliver.
	 */
	static void assertHolds(GameTestHelper helper, Villager worker, BlockPos pos, Predicate<ItemStack> filter, int min, String what) {
		int count = count(helper, pos, filter);
		if (count < min) {
			throw helper.assertionException(pos, "Expected at least " + min + " " + what + ", found " + count + "; worker: " + describe(worker));
		}
	}

	/** The worker's status (as in its menu, as translation key), profession, activity and what it carries. */
	static String describe(Villager worker) {
		StringBuilder carried = new StringBuilder();
		for (ItemStack stack : worker.getInventory()) {
			if (!stack.isEmpty()) {
				carried.append(carried.isEmpty() ? "" : " ").append(stack.getCount()).append(' ').append(stack.getItem());
			}
		}
		return WorkerStatus.describe(worker).getString()
			+ ", " + worker.getVillagerData().profession().getRegisteredName()
			+ ", " + worker.getBrain().getActiveNonCoreActivity().map(Object::toString).orElse("no activity")
			+ ", carries: " + (carried.isEmpty() ? "nothing" : carried);
	}

	/**
	 * An oak tree: a trunk of {@code height} logs from {@code base} up, on {@code ground}, and
	 * a crown of leaves. With {@code natural} the leaves are not persistent, like a grown
	 * tree; otherwise they count as placed by a player.
	 */
	static void oakTree(GameTestHelper helper, BlockPos base, int height, boolean natural, Block ground) {
		helper.setBlock(base.below(), ground);
		for (int y = 0; y < height; y++) {
			helper.setBlock(base.above(y), Blocks.OAK_LOG);
		}
		BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState()
			.setValue(LeavesBlock.PERSISTENT, !natural)
			.setValue(LeavesBlock.DISTANCE, 1);
		BlockPos top = base.above(height - 1);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				for (int dy = -1; dy <= 1; dy++) {
					BlockPos pos = top.offset(dx, dy, dz);
					if (helper.getBlockState(pos).isAir()) {
						helper.setBlock(pos, leaves);
					}
				}
			}
		}
	}
}
