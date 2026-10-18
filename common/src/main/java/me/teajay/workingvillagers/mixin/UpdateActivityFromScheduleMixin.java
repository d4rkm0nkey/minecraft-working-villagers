package me.teajay.workingvillagers.mixin;

import me.teajay.workingvillagers.job.GuardPost;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.UpdateActivityFromSchedule;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Guards work at night: instead of the villager schedule they follow their own (see
 * {@link GuardPost#activity}). Every package of a villager ends with this behavior, so it
 * also sets the guard back to work after a raid or the bell changed its activity.
 */
@Mixin(UpdateActivityFromSchedule.class)
public abstract class UpdateActivityFromScheduleMixin {
	@Inject(method = "lambda$create$1", at = @At("HEAD"), cancellable = true)
	private static void workingvillagers$guardSchedule(ServerLevel level, LivingEntity entity, long timestamp, CallbackInfoReturnable<Boolean> cir) {
		if (entity instanceof Villager villager && GuardPost.isGuard(villager)) {
			villager.getBrain().setActiveActivityIfPossible(GuardPost.activity(level));
			cir.setReturnValue(true);
		}
	}
}
