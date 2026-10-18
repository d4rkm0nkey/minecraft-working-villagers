package me.teajay.workingvillagers.mixin;

import me.teajay.workingvillagers.carrier.RouteSelection;
import me.teajay.workingvillagers.station.StationPicks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While a player picks the containers of a new route or the chests of a workstation,
 * right-clicking a container selects it instead of opening it.
 */
@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
	@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
	private void workingvillagers$pickRouteContainer(
		ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir
	) {
		if (hand == InteractionHand.MAIN_HAND
			&& (RouteSelection.onUseBlock(player, hit.getBlockPos()) || StationPicks.onUseBlock(player, hit.getBlockPos(), hit.getDirection()))) {
			cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
		}
	}
}
