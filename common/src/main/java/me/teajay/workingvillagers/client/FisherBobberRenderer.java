package me.teajay.workingvillagers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.teajay.workingvillagers.entity.FisherBobber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the bobber of a fisher with the line to the tip of its rod, like vanilla draws the
 * bobber of a player (see {@code FishingHookRenderer}). The bobber bobs on the water and dips
 * when a fish bites.
 */
public class FisherBobberRenderer extends EntityRenderer<FisherBobber, FisherBobberRenderer.State> {
	private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/fishing/fishing_hook.png");
	private static final RenderType RENDER_TYPE = RenderTypes.entityCutoutCull(TEXTURE);
	private static final int LINE_STEPS = 16;
	/** How deep the bobber dips when a fish bites. */
	private static final float DIP = 0.3F;

	public FisherBobberRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRender(FisherBobber entity, Frustum culler, double camX, double camY, double camZ, float partialTicks) {
		return super.shouldRender(entity, culler, camX, camY, camZ, partialTicks) && entity.getOwner() != null;
	}

	/** The vanilla bobber's state, and how far the bobber is above (or below) its position. */
	public static class State extends FishingHookRenderState {
		public float bob;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(FisherBobber entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		// Bobbing on the water, dipped at a bite
		state.bob = entity.isBiting() ? -DIP : 0.03F * Mth.sin(state.ageInTicks * 0.15F);
		Villager owner = entity.getOwner();
		if (owner == null) {
			state.lineOriginOffset = Vec3.ZERO;
			return;
		}
		Vec3 hook = entity.getPosition(partialTicks).add(0.0, 0.25 + state.bob, 0.0);
		state.lineOriginOffset = rodTip(owner, partialTicks).subtract(hook);
	}

	/** The tip of the rod in the fisher's right hand, held out in front (see {@code WorkMotion.CAST}). */
	private static Vec3 rodTip(Villager owner, float partialTicks) {
		float yaw = Mth.lerp(partialTicks, owner.yBodyRotO, owner.yBodyRot) * Mth.DEG_TO_RAD;
		double sin = Mth.sin(yaw);
		double cos = Mth.cos(yaw);
		double right = 0.35 * owner.getScale();
		double forward = 0.8 * owner.getScale();
		return owner.getEyePosition(partialTicks).add(-cos * right - sin * forward, -0.45 * owner.getScale(), -sin * right + cos * forward);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.0F, state.bob, 0.0F);
		poseStack.pushPose();
		poseStack.scale(0.5F, 0.5F, 0.5F);
		poseStack.rotate(camera.orientation);
		submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
			vertex(buffer, pose, state.lightCoords, 0.0F, 0, 0, 1);
			vertex(buffer, pose, state.lightCoords, 1.0F, 0, 1, 1);
			vertex(buffer, pose, state.lightCoords, 1.0F, 1, 1, 0);
			vertex(buffer, pose, state.lightCoords, 0.0F, 1, 0, 0);
		});
		poseStack.popPose();
		float xa = (float) state.lineOriginOffset.x;
		float ya = (float) state.lineOriginOffset.y;
		float za = (float) state.lineOriginOffset.z;
		float width = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
		submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
			for (int i = 0; i < LINE_STEPS; i++) {
				float a0 = (float) i / LINE_STEPS;
				float a1 = (float) (i + 1) / LINE_STEPS;
				lineVertex(xa, ya, za, buffer, pose, a0, a1, width);
				lineVertex(xa, ya, za, buffer, pose, a1, a0, width);
			}
		});
		poseStack.popPose();
		super.submit(state, poseStack, submitNodeCollector, camera);
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, int lightCoords, float x, int y, int u, int v) {
		buffer.addVertex(pose, x - 0.5F, y - 0.5F, 0.0F)
			.setColor(-1)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(lightCoords)
			.setNormal(pose, 0.0F, 1.0F, 0.0F);
	}

	/** A point of the line, sagging like vanilla's. */
	private static void lineVertex(float xa, float ya, float za, VertexConsumer buffer, PoseStack.Pose pose, float a, float next, float width) {
		float x = xa * a;
		float y = ya * (a * a + a) * 0.5F + 0.25F;
		float z = za * a;
		float nx = xa * next - x;
		float ny = ya * (next * next + next) * 0.5F + 0.25F - y;
		float nz = za * next - z;
		float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
		buffer.addVertex(pose, x, y, z).setColor(0xFF000000).setNormal(pose, nx / length, ny / length, nz / length).setLineWidth(width);
	}

	@Override
	protected boolean affectedByCulling(FisherBobber entity) {
		return false;
	}
}
