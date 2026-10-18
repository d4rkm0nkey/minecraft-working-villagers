package me.teajay.workingvillagers.mixin;

import me.teajay.workingvillagers.job.GuardPost;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.VillagerPanicTrigger;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Guards do not panic when monsters come near or hit them: that is their work. */
@Mixin(VillagerPanicTrigger.class)
public abstract class VillagerPanicTriggerMixin {
	@Inject(method = "start(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/villager/Villager;J)V", at = @At("HEAD"), cancellable = true)
	private void workingvillagers$guardsDoNotPanic(ServerLevel level, Villager villager, long timestamp, CallbackInfo ci) {
		if (GuardPost.isGuard(villager)) {
			ci.cancel();
		}
	}
}
