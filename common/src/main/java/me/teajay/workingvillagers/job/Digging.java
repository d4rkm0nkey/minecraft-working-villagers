package me.teajay.workingvillagers.job;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What the workers that dig (the pit digger, the miner) have in common: how long a block
 * takes, which tool they take, how it wears out, what a block drops and where they can stand.
 */
final class Digging {
	/**
	 * A master (level 5) needs this much of the time of a novice (level 1), whose slowdown is
	 * set in the config.
	 */
	private static final float MASTER_SHARE = 5.0F / 8.0F;

	private Digging() {
	}

	/**
	 * The time a player with the same tool needs, times the slowdown for the worker's level
	 * (from {@code noviceSlowdown} for a novice), at least {@code minSeconds}. Plants and
	 * other blocks without hardness take a quarter of that.
	 */
	static int digTicks(ServerLevel level, Villager villager, BlockPos pos, BlockState state, ItemStack tool, double noviceSlowdown, double minSeconds) {
		float hardness = state.getDestroySpeed(level, pos);
		if (hardness <= 0.0F) {
			return Math.max(1, (int) Math.round(minSeconds * 5.0));
		}
		// Like BlockBehaviour#getDestroyProgress for a player
		float divisor = !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state) ? 30.0F : 100.0F;
		float ticks = (float) Math.ceil(hardness * divisor / Math.max(1.0F, tool.getDestroySpeed(state)));
		return Math.max((int) Math.round(minSeconds * 20.0), Math.round(ticks * slowdown(villager, (float) noviceSlowdown)));
	}

	/** From {@code novice} times as long as a player for a novice down to 5/8 of that for a master. */
	private static float slowdown(Villager villager, float novice) {
		float progress = Math.clamp((villager.getVillagerData().level() - 1) / 4.0F, 0.0F, 1.0F);
		return novice + (novice * MASTER_SHARE - novice) * progress;
	}

	/**
	 * The tool for the block among the shovels and pickaxes the worker carries: whichever is
	 * faster; one that gets the block's drops before one that does not.
	 */
	static ItemStack findTool(SimpleContainer inventory, BlockState state) {
		ItemStack best = ItemStack.EMPTY;
		for (ItemStack stack : inventory) {
			if (stack.isEmpty() || !WorkerSupplies.SHOVEL.item().test(stack) && !WorkerSupplies.PICKAXE.item().test(stack)) {
				continue;
			}
			boolean correct = !state.requiresCorrectToolForDrops() || stack.isCorrectToolForDrops(state);
			boolean bestCorrect = !best.isEmpty() && (!state.requiresCorrectToolForDrops() || best.isCorrectToolForDrops(state));
			if (best.isEmpty() || correct && !bestCorrect || correct == bestCorrect && stack.getDestroySpeed(state) > best.getDestroySpeed(state)) {
				best = stack;
			}
		}
		return best;
	}

	/** Whether the worker's tools are too weak to get anything out of the block (diamond ore with a stone pickaxe). */
	static boolean toolTooWeak(SimpleContainer inventory, BlockState state) {
		return state.requiresCorrectToolForDrops() && !findTool(inventory, state).isCorrectToolForDrops(state);
	}

	/** One use per block, like a player's tool; a broken tool is gone. */
	static void wearTool(ServerLevel level, Villager villager, ItemStack tool) {
		if (tool.isEmpty()) {
			return;
		}
		tool.hurtAndBreak(1, level, null, broken -> {
			villager.onEquippedItemBroken(broken, EquipmentSlot.MAINHAND);
			villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		});
		villager.getInventory().setChanged();
	}

	/** What digging the block gives, with the tool the worker would take. */
	static List<ItemStack> drops(ServerLevel level, Villager villager, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return Block.getDrops(state, level, pos, level.getBlockEntity(pos), villager, findTool(villager.getInventory(), state));
	}

	/** Whether all the stacks fit into the inventory. */
	static boolean fits(SimpleContainer inventory, List<ItemStack> stacks) {
		SimpleContainer copy = new SimpleContainer(inventory.getContainerSize());
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			copy.setItem(slot, inventory.getItem(slot).copy());
		}
		for (ItemStack stack : stacks) {
			if (!copy.addItem(stack.copy()).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	/** Two blocks of room (a ladder counts as room) above solid ground. */
	static boolean isStandable(ServerLevel level, BlockPos pos) {
		return isPassable(level, pos) && isPassable(level, pos.above())
			&& !isPassable(level, pos.below()) && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
	}

	static boolean isPassable(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.is(BlockTags.CLIMBABLE) || state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty();
	}
}
