package me.teajay.workingvillagers.mixin;

import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.menu.CutterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the vanilla stonecutter a workstation: it opens the cutter menu (the stonecutter
 * menu plus the order of the station).
 */
@Mixin(StonecutterBlock.class)
public abstract class StonecutterBlockMixin {
	@Inject(method = "getMenuProvider", at = @At("HEAD"), cancellable = true)
	private void workingvillagers$cutterMenu(BlockState state, Level level, BlockPos pos, CallbackInfoReturnable<MenuProvider> cir) {
		cir.setReturnValue(new SimpleMenuProvider(
			(containerId, inventory, player) -> new CutterMenu(WorkingVillagers.STONECUTTER_KIND, containerId, inventory, ContainerLevelAccess.create(level, pos)),
			Component.translatable("container.stonecutter")
		));
	}
}
