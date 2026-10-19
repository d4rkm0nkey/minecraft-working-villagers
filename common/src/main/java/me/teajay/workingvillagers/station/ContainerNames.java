package me.teajay.workingvillagers.station;

import java.util.ArrayList;
import java.util.List;
import me.teajay.workingvillagers.mixin.BaseContainerBlockEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The names of chests, barrels and other containers. They are the vanilla custom names (the
 * ones an anvil gives), so the container's own screen shows them as its title, too.
 */
public final class ContainerNames {
	public static final int MAX_LENGTH = 24;

	private ContainerNames() {
	}

	/** The container's name, or the name of its block if it has none. */
	public static Component name(Level level, BlockPos pos) {
		Component custom = customName(level, pos);
		if (custom != null) {
			return custom;
		}
		BlockState state = level.getBlockState(pos);
		return state.isAir() ? Component.translatable("gui.workingvillagers.unknown_block") : state.getBlock().getName();
	}

	/** The name given to the container (or to the other half of a double chest), null if none. */
	public static @Nullable Component customName(Level level, BlockPos pos) {
		for (BaseContainerBlockEntity container : halves(level, pos)) {
			if (container.getCustomName() != null) {
				return container.getCustomName();
			}
		}
		return null;
	}

	/** Names the container, both halves of a double chest; an empty name removes it. */
	public static void rename(Level level, BlockPos pos, String name) {
		String clean = clean(name);
		Component component = clean.isEmpty() ? null : Component.literal(clean);
		for (BaseContainerBlockEntity container : halves(level, pos)) {
			((BaseContainerBlockEntityAccessor) container).workingvillagers$setName(component);
			container.setChanged();
		}
	}

	/** Trims the name and cuts it to the maximum length. */
	public static String clean(String name) {
		String trimmed = name.strip();
		return trimmed.length() > MAX_LENGTH ? trimmed.substring(0, MAX_LENGTH) : trimmed;
	}

	/** The block entity at the position and, for a double chest, the one of the other half. */
	public static List<BaseContainerBlockEntity> halves(Level level, BlockPos pos) {
		List<BaseContainerBlockEntity> halves = new ArrayList<>(2);
		if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof BaseContainerBlockEntity container)) {
			return halves;
		}
		halves.add(container);
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof ChestBlock && ChestBlock.getBlockType(state) != DoubleBlockCombiner.BlockType.SINGLE) {
			BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
			if (level.isLoaded(other) && level.getBlockEntity(other) instanceof BaseContainerBlockEntity otherContainer) {
				halves.add(otherContainer);
			}
		}
		return halves;
	}
}
