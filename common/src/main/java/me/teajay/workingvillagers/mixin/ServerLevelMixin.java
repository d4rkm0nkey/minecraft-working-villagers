package me.teajay.workingvillagers.mixin;

import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every workstation is a point of interest, so vanilla tells the level about every change
 * of its block; a station that is gone drops the emeralds of its wage box (see
 * {@link Workstations#onBlockChanged}), however it went: broken, blown up, replaced.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
	@Inject(method = "updatePOIOnBlockStateChange", at = @At("TAIL"))
	private void workingvillagers$dropWage(BlockPos pos, BlockState before, BlockState after, CallbackInfo ci) {
		Workstations.onBlockChanged((ServerLevel) (Object) this, pos, before, after);
	}
}
