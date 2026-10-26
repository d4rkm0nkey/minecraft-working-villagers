package me.teajay.workingvillagers.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import me.teajay.workingvillagers.client.WorkerNoticeRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the sign above a worker's head with the name tags, see {@link WorkerNoticeRenderer}. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
	@Inject(method = "submit", at = @At("TAIL"))
	private void workingvillagers$submitNotice(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		WorkerNoticeRenderer.submit(state, poseStack, collector, camera);
	}
}
