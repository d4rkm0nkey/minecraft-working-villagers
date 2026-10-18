package me.teajay.workingvillagers.menu;

import java.util.Optional;
import me.teajay.workingvillagers.station.StationType;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.DataSlot;

/**
 * What the tabs of a workstation show besides their icons, as bits of one number: whether a
 * hired worker works there, whether the station is set up, and on which tab something is
 * missing (the red dot). The server works it out; the overview gets it with its info, the
 * screens of the blocks in a data slot (see {@link StationWorker}).
 */
public final class StationTabFlags {
	public static final int WORKER = 1;
	public static final int SET_UP = 2;
	/** No worker, or it lacks a tool. */
	public static final int WORK_PROBLEM = 4;
	/** The worker has no wage for today. */
	public static final int WAGE_PROBLEM = 8;
	public static final int CHESTS_PROBLEM = 16;
	/** No order, no pit, no tunnel. */
	public static final int ORDER_PROBLEM = 32;
	/** How often (in ticks) a block's screen works it out again. */
	private static final int UPDATE_TICKS = 20;

	private StationTabFlags() {
	}

	public static boolean has(int flags, int flag) {
		return (flags & flag) != 0;
	}

	public static int compute(ServerLevel level, BlockPos pos, StationType type) {
		Optional<Workstations.Station> found = Workstations.find(level, pos);
		if (found.isEmpty()) {
			return 0;
		}
		Workstations.Station station = found.get();
		int flags = Workstations.findSetUp(level, pos).isPresent() ? SET_UP : 0;
		Optional<SuppliesInfo> supplies = Optional.empty();
		if (station.worker() != null) {
			flags |= WORKER;
			if (level.getEntity(station.worker()) instanceof Villager villager) {
				CompoundTag tag = new CompoundTag();
				SuppliesInfo.write(level, villager, tag);
				supplies = SuppliesInfo.read(tag);
			}
		}
		if (station.worker() == null || supplies.flatMap(SuppliesInfo::firstMissing).isPresent()) {
			flags |= WORK_PROBLEM;
		}
		if (supplies.map(info -> !info.paid() && info.emeralds() < info.wage()).orElse(false)) {
			flags |= WAGE_PROBLEM;
		}
		if (station.validInputs(level).isEmpty() && station.validOutputs(level).isEmpty()) {
			flags |= CHESTS_PROBLEM;
		}
		boolean noOrder = switch (type) {
			case PIT_STAKE -> station.pit() == null;
			case MINING_BENCH -> station.tunnel() == null;
			case KILN -> false;
			default -> type.hasOrder() && (station.order() == null || type.hasWoods() && station.shape() != null && station.woods().isEmpty());
		};
		return noOrder ? flags | ORDER_PROBLEM : flags;
	}

	/**
	 * Server: a data slot with the flags of the station at {@code pos}, worked out again once
	 * a second while the menu is open.
	 */
	public static DataSlot slot(ServerLevel level, BlockPos pos, StationType type) {
		return new DataSlot() {
			private int value;
			private boolean known;
			private long computed;

			@Override
			public int get() {
				long now = level.getGameTime();
				if (!this.known || now - this.computed >= UPDATE_TICKS) {
					this.known = true;
					this.value = compute(level, pos, type);
					this.computed = now;
				}
				return this.value;
			}

			@Override
			public void set(int value) {
				this.value = value;
			}
		};
	}
}
