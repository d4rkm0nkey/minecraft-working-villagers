package me.teajay.workingvillagers.mixin;

import java.util.function.BooleanSupplier;
import me.teajay.workingvillagers.job.WorkerChunks;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
	/**
	 * Every workstation is a point of interest, so vanilla tells the level about every change
	 * of its block; a station that is gone drops the emeralds of its wage box (see
	 * {@link Workstations#onBlockChanged}), however it went: broken, blown up, replaced.
	 */
	@Inject(method = "updatePOIOnBlockStateChange", at = @At("TAIL"))
	private void workingvillagers$dropWage(BlockPos pos, BlockState before, BlockState after, CallbackInfo ci) {
		Workstations.onBlockChanged((ServerLevel) (Object) this, pos, before, after);
	}

	/** Loads workers that work without a player nearby again, see {@link WorkerChunks#tick}. */
	@Inject(method = "tick", at = @At("HEAD"))
	private void workingvillagers$loadWorkers(BooleanSupplier haveTime, CallbackInfo ci) {
		WorkerChunks.tick((ServerLevel) (Object) this);
	}
}
