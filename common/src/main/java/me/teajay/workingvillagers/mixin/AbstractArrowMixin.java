package me.teajay.workingvillagers.mixin;

import me.teajay.workingvillagers.job.GuardPost;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The arrows of a guard only hit monsters: they fly through villagers, players, golems and
 * animals.
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {
	@Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
	private void workingvillagers$onlyMonsters(Entity target, CallbackInfoReturnable<Boolean> cir) {
		if (((AbstractArrow) (Object) this).getOwner() instanceof Villager villager && GuardPost.isGuard(villager) && !(target instanceof Enemy)) {
			cir.setReturnValue(false);
		}
	}
}
