package me.teajay.workingvillagers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Draws the held item in the right hand of a worker with its arms apart, the way
 * {@code ItemInHandLayer} draws it for players. With crossed arms vanilla's
 * {@code CrossedArmsItemLayer} draws it.
 */
public class WorkItemLayer extends RenderLayer<VillagerRenderState, VillagerModel> {
	public WorkItemLayer(RenderLayerParent<VillagerRenderState, VillagerModel> renderer) {
		super(renderer);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, VillagerRenderState state, float yRot, float xRot) {
		WorkPoseHolder pose = (WorkPoseHolder) state;
		ItemStackRenderState item = pose.workingvillagers$getHandItem();
		if (pose.workingvillagers$getWorkMotion() == WorkMotion.NONE || item.isEmpty()) {
			return;
		}
		poseStack.pushPose();
		((WorkArmsModel) this.getParentModel()).workingvillagers$translateToRightHand(poseStack);
		poseStack.rotateDegrees(Axis.XP, -90.0F);
		poseStack.rotateDegrees(Axis.YP, 180.0F);
		poseStack.translate(1.0F / 16.0F, 2.0F / 16.0F, -10.0F / 16.0F);
		item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
	}
}
