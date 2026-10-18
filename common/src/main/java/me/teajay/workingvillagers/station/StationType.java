package me.teajay.workingvillagers.station;

import java.util.List;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.crafting.WoodTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
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
	CARPENTER_BENCH(true),
	/** The farmer only delivers its harvest. */
	COMPOSTER(false),
	/** The smith smelts whatever it finds in the input chests, there is no order. */
	BLAST_FURNACE(true),
	/** The pit digger only delivers what it digs. */
	PIT_STAKE(false),
	/** The kiln operator fires everything, or only what makes the order. */
	KILN(true),
	/** The shop assistant sells the goods of the input chests, the emeralds go to the output chests. */
	TRADING_COUNTER(true),
	/** The miner only delivers what it digs; the bench itself is a mill for players. */
	MINING_BENCH(false),
	/**
	 * The smith repairs tools: worn ones from the village chests nearby and damaged ones from
	 * the input chests, with the material from the input chests.
	 */
	SMITHING_TABLE(true),
	/** The shepherd shears and breeds the sheep around the loom; it only delivers the wool. */
	LOOM(false),
	/**
	 * The butcher breeds and slaughters the animals around the smoker and smokes the meat;
	 * feed and coal can come from input chests.
	 */
	SMOKER(true);

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

	/** Whether the block has a screen of its own, which the overview leads back to. */
	public boolean hasOwnScreen() {
		return this == WOODCUTTER || this == STONECUTTER || this == CARPENTER_BENCH || this == BLAST_FURNACE || this == KILN || this == MINING_BENCH
			|| this == SMITHING_TABLE || this == LOOM || this == SMOKER;
	}

	/** Whether the worker needs an order, set in the order column of the overview. */
	public boolean hasOrder() {
		return this == WOODCUTTER || this == STONECUTTER || this == CARPENTER_BENCH || this == KILN;
	}

	/**
	 * Whether the station is a vanilla block that is also the job site of a vanilla
	 * profession. It only counts as a workstation of this mod once it has been set up.
	 */
	public boolean isVanilla() {
		return this == STONECUTTER || this == COMPOSTER || this == BLAST_FURNACE || this == SMITHING_TABLE || this == LOOM || this == SMOKER;
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

	/** The profession of this mod that works here. */
	public ResourceKey<VillagerProfession> profession() {
		return switch (this) {
			case CHOPPING_BLOCK -> WorkingVillagers.LUMBERJACK_PROFESSION;
			case WOODCUTTER -> WorkingVillagers.WOODCUTTER_PROFESSION;
			case STONECUTTER -> WorkingVillagers.STONECUTTER_PROFESSION;
			case CARPENTER_BENCH -> WorkingVillagers.CARPENTER_PROFESSION;
			case COMPOSTER -> WorkingVillagers.FARMER_PROFESSION;
			case BLAST_FURNACE, SMITHING_TABLE -> WorkingVillagers.SMITH_PROFESSION;
			case PIT_STAKE -> WorkingVillagers.PIT_DIGGER_PROFESSION;
			case KILN -> WorkingVillagers.KILN_OPERATOR_PROFESSION;
			case TRADING_COUNTER -> WorkingVillagers.SHOP_ASSISTANT_PROFESSION;
			case MINING_BENCH -> WorkingVillagers.MINER_PROFESSION;
			case LOOM -> WorkingVillagers.SHEPHERD_PROFESSION;
			case SMOKER -> WorkingVillagers.BUTCHER_PROFESSION;
		};
	}

	public Component displayName() {
		return switch (this) {
			case CHOPPING_BLOCK -> WorkingVillagers.CHOPPING_BLOCK.get().getName();
			case WOODCUTTER -> WorkingVillagers.WOODCUTTER_BLOCK.get().getName();
			case STONECUTTER -> Blocks.STONECUTTER.getName();
			case CARPENTER_BENCH -> WorkingVillagers.CARPENTER_BENCH.get().getName();
			case COMPOSTER -> Blocks.COMPOSTER.getName();
			case BLAST_FURNACE -> Blocks.BLAST_FURNACE.getName();
			case PIT_STAKE -> WorkingVillagers.PIT_STAKE.get().getName();
			case KILN -> WorkingVillagers.KILN.get().getName();
			case TRADING_COUNTER -> WorkingVillagers.TRADING_COUNTER.get().getName();
			case MINING_BENCH -> WorkingVillagers.MINING_BENCH.get().getName();
			case SMITHING_TABLE -> Blocks.SMITHING_TABLE.getName();
			case LOOM -> Blocks.LOOM.getName();
			case SMOKER -> Blocks.SMOKER.getName();
		};
	}

	public Block block() {
		return switch (this) {
			case CHOPPING_BLOCK -> WorkingVillagers.CHOPPING_BLOCK.get();
			case WOODCUTTER -> WorkingVillagers.WOODCUTTER_BLOCK.get();
			case STONECUTTER -> Blocks.STONECUTTER;
			case CARPENTER_BENCH -> WorkingVillagers.CARPENTER_BENCH.get();
			case COMPOSTER -> Blocks.COMPOSTER;
			case BLAST_FURNACE -> Blocks.BLAST_FURNACE;
			case PIT_STAKE -> WorkingVillagers.PIT_STAKE.get();
			case KILN -> WorkingVillagers.KILN.get();
			case TRADING_COUNTER -> WorkingVillagers.TRADING_COUNTER.get();
			case MINING_BENCH -> WorkingVillagers.MINING_BENCH.get();
			case SMITHING_TABLE -> Blocks.SMITHING_TABLE;
			case LOOM -> Blocks.LOOM;
			case SMOKER -> Blocks.SMOKER;
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
		} else if (state.is(Blocks.COMPOSTER)) {
			return COMPOSTER;
		} else if (state.is(Blocks.BLAST_FURNACE)) {
			return BLAST_FURNACE;
		} else if (state.is(WorkingVillagers.PIT_STAKE.get())) {
			return PIT_STAKE;
		} else if (state.is(WorkingVillagers.KILN.get())) {
			return KILN;
		} else if (state.is(WorkingVillagers.TRADING_COUNTER.get())) {
			return TRADING_COUNTER;
		} else if (state.is(WorkingVillagers.MINING_BENCH.get())) {
			return MINING_BENCH;
		} else if (state.is(Blocks.SMITHING_TABLE)) {
			return SMITHING_TABLE;
		} else if (state.is(Blocks.LOOM)) {
			return LOOM;
		} else if (state.is(Blocks.SMOKER)) {
			return SMOKER;
		}
		return null;
	}
}
