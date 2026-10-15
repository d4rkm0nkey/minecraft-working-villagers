package me.teajay.workingvillagers.mixin;

import me.teajay.workingvillagers.job.HiredVillager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Hired villagers do not trample farmland when they jump or fall onto it: the farmer walks
 * through its fields all day, and other workers pass by.
 */
@Mixin(FarmlandBlock.class)
public abstract class FarmlandBlockMixin {
	@Redirect(
		method = "fallOn",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FarmlandBlock;turnToBaseBlock(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V")
	)
	private void workingvillagers$noTrampling(FarmlandBlock block, Entity entity, BlockState state, Level level, BlockPos pos) {
		if (!(entity instanceof HiredVillager hired && hired.workingvillagers$isHired())) {
			block.turnToBaseBlock(entity, state, level, pos);
		}
	}
}
