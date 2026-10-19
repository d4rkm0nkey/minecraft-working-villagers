package me.teajay.workingvillagers.mixin;

import me.teajay.workingvillagers.job.GuardPost;
import me.teajay.workingvillagers.job.Uniforms;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A monster killed by a guard's arrow: the guard counts it, notes where it fell (to pick up
 * the loot in the morning) and gets its experience (see {@link GuardPost#onKill}). The armor
 * of guards wears out like a player's (vanilla mobs' armor does not), see {@link Uniforms}.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Shadow
	protected boolean dead;

	@Shadow
	protected abstract void doHurtEquipment(DamageSource source, float damage, EquipmentSlot... slots);

	@Invoker("getBaseExperienceReward")
	public abstract int workingvillagers$baseExperience(ServerLevel level);

	@Inject(method = "hurtArmor", at = @At("HEAD"))
	private void workingvillagers$wearGuardArmor(DamageSource source, float damage, CallbackInfo ci) {
		if ((Object) this instanceof Villager villager && Uniforms.wearsArmor(villager)) {
			this.doHurtEquipment(source, damage, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD);
		}
	}

	@Inject(method = "die", at = @At("HEAD"))
	private void workingvillagers$killedByGuard(DamageSource source, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		// Like vanilla: only the first call counts
		if (self instanceof Enemy && !self.isRemoved() && !this.dead && source.getEntity() instanceof Villager villager && GuardPost.isGuard(villager)
			&& self.level() instanceof ServerLevel level) {
			GuardPost.onKill(level, villager, self, this.workingvillagers$baseExperience(level));
		}
	}
}
