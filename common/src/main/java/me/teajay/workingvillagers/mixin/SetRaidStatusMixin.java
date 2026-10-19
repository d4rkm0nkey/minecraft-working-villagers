package me.teajay.workingvillagers.mixin;

import me.teajay.workingvillagers.job.GuardPost;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.SetRaidStatus;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Guards do not take part in the vanilla raid behavior (fleeing, hiding): they fight (see {@link GuardPost#activity}). */
@Mixin(SetRaidStatus.class)
public abstract class SetRaidStatusMixin {
	@Inject(method = "lambda$create$1", at = @At("HEAD"), cancellable = true)
	private static void workingvillagers$guardsFight(ServerLevel level, LivingEntity entity, long timestamp, CallbackInfoReturnable<Boolean> cir) {
		if (entity instanceof Villager villager && GuardPost.isGuard(villager)) {
			cir.setReturnValue(false);
		}
	}
}
