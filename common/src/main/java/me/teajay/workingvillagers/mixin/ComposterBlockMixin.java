package me.teajay.workingvillagers.mixin;

import me.teajay.workingvillagers.menu.StationMenu;
import me.teajay.workingvillagers.station.StationType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the vanilla composter the farmer's workstation: right-click with an empty hand opens
 * its overview. Vanilla does nothing then, unless the composter is full: then the bone meal
 * comes out first. Composting with an item in the hand stays vanilla.
 */
@Mixin(ComposterBlock.class)
public abstract class ComposterBlockMixin {
	@Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
	private void workingvillagers$overview(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
		if (state.getValue(ComposterBlock.LEVEL) < ComposterBlock.READY && player.getMainHandItem().isEmpty()) {
			if (!level.isClientSide()) {
				player.openMenu(StationMenu.provider(StationType.COMPOSTER, level, pos));
			}
			cir.setReturnValue(InteractionResult.SUCCESS);
		}
	}
}
