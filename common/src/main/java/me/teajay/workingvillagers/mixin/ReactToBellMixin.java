package me.teajay.workingvillagers.mixin;

import me.teajay.workingvillagers.job.GuardPost;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.ReactToBell;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Guards do not hide when the bell rings: they ring it themselves. */
@Mixin(ReactToBell.class)
public abstract class ReactToBellMixin {
	@Inject(method = "lambda$create$2", at = @At("HEAD"), cancellable = true)
	private static void workingvillagers$guardsStay(ServerLevel level, LivingEntity entity, long timestamp, CallbackInfoReturnable<Boolean> cir) {
		if (entity instanceof Villager villager && GuardPost.isGuard(villager)) {
			cir.setReturnValue(false);
		}
	}
}
