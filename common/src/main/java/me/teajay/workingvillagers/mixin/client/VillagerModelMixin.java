package me.teajay.workingvillagers.mixin.client;

import me.teajay.workingvillagers.client.WorkPoseHolder;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Moves the crossed arms of a villager with its swing at work, see {@code WorkMotion}.
 */
@Mixin(VillagerModel.class)
public abstract class VillagerModelMixin {
	@Shadow
	@Final
	private ModelPart head;
	@Shadow
	@Final
	private ModelPart arms;

	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/VillagerRenderState;)V", at = @At("TAIL"))
	private void workingvillagers$animateWork(VillagerRenderState state, CallbackInfo ci) {
		WorkPoseHolder pose = (WorkPoseHolder) state;
		pose.workingvillagers$getWorkMotion().apply(this.head, this.arms, pose.workingvillagers$getWorkSwing(), state.xRot * Mth.DEG_TO_RAD);
	}
}
