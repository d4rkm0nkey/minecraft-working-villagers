package me.teajay.workingvillagers.mixin;

import me.teajay.workingvillagers.carrier.RouteSelection;
import me.teajay.workingvillagers.menu.StationMenu;
import me.teajay.workingvillagers.station.StationPicks;
import me.teajay.workingvillagers.station.StationType;
import me.teajay.workingvillagers.station.Workstations;
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
 * right-clicking a container selects it instead of opening it. Right-clicking a workstation
 * with emeralds puts them into its wage box instead of opening it. Blocks without a screen of
 * their own (the fletching table, the target, the cauldron) open their overview on a right-click with an
 * empty hand.
 */
@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
	@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
	private void workingvillagers$pickRouteContainer(
		ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir
	) {
		if (hand == InteractionHand.MAIN_HAND
			&& (RouteSelection.onUseBlock(player, hit.getBlockPos()) || StationPicks.onUseBlock(player, hit.getBlockPos(), hit.getDirection())
				|| Workstations.depositWage(player, hit.getBlockPos(), stack))) {
			cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
			return;
		}
		if (hand == InteractionHand.MAIN_HAND && stack.isEmpty() && !player.isSecondaryUseActive()) {
			StationType type = StationType.of(level.getBlockState(hit.getBlockPos()));
			if (type == StationType.FLETCHING_TABLE || type == StationType.TARGET || type == StationType.CAULDRON) {
				player.openMenu(StationMenu.provider(type, level, hit.getBlockPos()));
				cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
			}
		}
	}
}
