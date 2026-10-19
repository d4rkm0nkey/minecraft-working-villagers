package me.teajay.workingvillagers.block;

import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.job.VillageChests;
import me.teajay.workingvillagers.menu.VillageChestMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.jspecify.annotations.Nullable;

/**
 * The village chest: a chest every worker nearby takes its wage, tools and what it uses up
 * from (see {@link VillageChests}). It is a vanilla chest with its own texture: the lid,
 * the sounds, cats and blocks on top work the same, and two of them side by side form a
 * double chest.
 */
public class VillageChestBlock extends ChestBlock {
	public VillageChestBlock(Properties properties) {
		super(() -> WorkingVillagers.VILLAGE_CHEST_BLOCK_ENTITY.get(), SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new VillageChestBlockEntity(pos, state);
	}

	/** The village chest's own screen, with six rows for a double chest. */
	@Override
	protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof VillageChestBlockEntity chest)) {
			return null;
		}
		if (state.getValue(TYPE) == ChestType.SINGLE) {
			return chest;
		}
		// Both halves in the vanilla order, null while a cat sits on it or a block is on top
		Container container = getContainer(this, state, level, pos, false);
		if (container == null) {
			return null;
		}
		Component name = chest.getCustomName();
		if (name == null && level.getBlockEntity(getConnectedBlockPos(pos, state)) instanceof VillageChestBlockEntity other) {
			name = other.getCustomName();
		}
		return new SimpleMenuProvider(
			(containerId, inventory, player) -> new VillageChestMenu(VillageChestMenu.DOUBLE_ROWS, containerId, inventory, container, pos),
			name != null ? name : Component.translatable("block." + WorkingVillagers.MOD_ID + ".village_chest")
		);
	}
}
