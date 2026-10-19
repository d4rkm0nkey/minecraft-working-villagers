package me.teajay.workingvillagers.mixin;

import me.teajay.workingvillagers.block.PitStakeBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * String connects to the pit stake like to a tripwire hook: the ring of string that touches
 * the stake is the pit.
 */
@Mixin(TripWireBlock.class)
public abstract class TripWireBlockMixin {
	@Inject(method = "shouldConnectTo", at = @At("HEAD"), cancellable = true)
	private void workingvillagers$connectToPitStake(BlockState state, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (state.getBlock() instanceof PitStakeBlock) {
			cir.setReturnValue(true);
		}
	}
}
