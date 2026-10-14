package me.teajay.workingvillagers.job;

import java.util.function.Consumer;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import org.jspecify.annotations.Nullable;

/**
 * A worker's visit to a chest: walk to the front of the chest, open it (the lid opens like
 * for a player), move the items, close it again.
 */
final class ChestTrip {
	private static final float SPEED = 0.5F;
	private static final int WALK_TIMEOUT = 400;
	/** Ticks between opening the chest and moving the items. */
	private static final int TRANSFER_AT = 10;
	/** Ticks between opening and closing the chest. */
	private static final int CLOSE_AT = 25;
	/** Fallback if there is no free spot next to the chest. */
	private static final double REACH = 2.5;

	enum Result { RUNNING, DONE, FAILED }

	private final BlockPos chest;
	private final @Nullable BlockPos spot;
	private int ticks;
	private boolean arrived;
	private @Nullable Container opened;

	private ChestTrip(BlockPos chest, @Nullable BlockPos spot) {
		this.chest = chest;
		this.spot = spot;
	}

	static ChestTrip to(ServerLevel level, Villager villager, BlockPos chest) {
		return new ChestTrip(chest, StandSpots.find(level, chest, villager).orElse(null));
	}

	BlockPos chest() {
		return this.chest;
	}

	/**
	 * Advances the trip. {@code transfer} is called once, while the chest is open.
	 */
	Result tick(ServerLevel level, Villager villager, Consumer<Container> transfer) {
		this.ticks++;
		if (!this.arrived) {
			boolean there = this.spot != null ? StandSpots.isAt(villager, this.spot) : this.chest.closerToCenterThan(villager.position(), REACH);
			if (there) {
				this.arrived = true;
				this.ticks = 0;
				villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			} else if (this.ticks > WALK_TIMEOUT) {
				return Result.FAILED;
			} else if (this.ticks % 20 == 1) {
				BlockPos target = this.spot != null ? this.spot : this.chest;
				villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, SPEED, this.spot != null ? 0 : 1));
				villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(this.chest));
			}
			return Result.RUNNING;
		}

		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(this.chest));
		if (this.ticks == 1) {
			this.opened = Workstations.getContainer(level, this.chest);
			if (this.opened == null) {
				return Result.FAILED;
			}
			((HiredVillager) villager).workingvillagers$setOpenedContainer(this.chest);
			this.opened.startOpen((ContainerUser) villager);
		} else if (this.ticks == TRANSFER_AT && this.opened != null) {
			transfer.accept(this.opened);
			this.opened.setChanged();
		} else if (this.ticks >= CLOSE_AT) {
			this.close(villager);
			return Result.DONE;
		}
		return Result.RUNNING;
	}

	/** Closes the chest if it is still open, also when the trip is cut short. */
	void close(Villager villager) {
		if (this.opened != null) {
			this.opened.stopOpen((ContainerUser) villager);
			this.opened = null;
		}
		((HiredVillager) villager).workingvillagers$setOpenedContainer(null);
	}
}
