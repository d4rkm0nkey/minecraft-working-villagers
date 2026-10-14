package me.teajay.workingvillagers.station;

import java.util.List;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.crafting.WoodTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The blocks a hired worker can work at.
 */
public enum StationType {
	/** The lumberjack only delivers, it fetches nothing. */
	CHOPPING_BLOCK(false),
	WOODCUTTER(true),
	STONECUTTER(true),
	CARPENTER_BENCH(true);

	private final boolean processing;

	StationType(boolean processing) {
		this.processing = processing;
	}

	/**
	 * Whether the worker processes items: it takes them from input chests, works on them
	 * according to the order of the station and brings the results to the output chests.
	 */
	public boolean isProcessing() {
		return this.processing;
	}

	/**
	 * The products whose order is a shape plus wood types (see {@link WoodTypes}), empty
	 * where every order is one exact item.
	 */
	public List<String> shapes() {
		return switch (this) {
			case WOODCUTTER -> WoodTypes.SHAPES;
			case CARPENTER_BENCH -> WoodTypes.CARPENTER_SHAPES;
			default -> List.of();
		};
	}

	/** Whether the order is set in the station's overview, with wood types. */
	public boolean hasWoods() {
		return !this.shapes().isEmpty();
	}

	public Component displayName() {
		return switch (this) {
			case CHOPPING_BLOCK -> WorkingVillagers.CHOPPING_BLOCK.get().getName();
			case WOODCUTTER -> WorkingVillagers.WOODCUTTER_BLOCK.get().getName();
			case STONECUTTER -> Blocks.STONECUTTER.getName();
			case CARPENTER_BENCH -> WorkingVillagers.CARPENTER_BENCH.get().getName();
		};
	}

	public Block block() {
		return switch (this) {
			case CHOPPING_BLOCK -> WorkingVillagers.CHOPPING_BLOCK.get();
			case WOODCUTTER -> WorkingVillagers.WOODCUTTER_BLOCK.get();
			case STONECUTTER -> Blocks.STONECUTTER;
			case CARPENTER_BENCH -> WorkingVillagers.CARPENTER_BENCH.get();
		};
	}

	public static @Nullable StationType of(BlockState state) {
		if (state.is(WorkingVillagers.CHOPPING_BLOCK.get())) {
			return CHOPPING_BLOCK;
		} else if (state.is(WorkingVillagers.WOODCUTTER_BLOCK.get())) {
			return WOODCUTTER;
		} else if (state.is(Blocks.STONECUTTER)) {
			return STONECUTTER;
		} else if (state.is(WorkingVillagers.CARPENTER_BENCH.get())) {
			return CARPENTER_BENCH;
		}
		return null;
	}
}
