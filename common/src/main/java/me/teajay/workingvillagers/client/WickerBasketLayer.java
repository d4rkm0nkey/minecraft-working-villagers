package me.teajay.workingvillagers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import me.teajay.workingvillagers.WorkingVillagers;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;

/**
 * Draws the wicker basket on the back of carriers.
 */
public class WickerBasketLayer extends RenderLayer<VillagerRenderState, VillagerModel> {
	private static final Identifier TEXTURE = WorkingVillagers.id("textures/entity/wicker_basket.png");
	private final WickerBasketModel model = new WickerBasketModel();

	public WickerBasketLayer(RenderLayerParent<VillagerRenderState, VillagerModel> renderer) {
		super(renderer);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, VillagerRenderState state, float yRot, float xRot) {
		if (state.isBaby || state.villagerData == null || !state.villagerData.profession().is(WorkingVillagers.CARRIER_PROFESSION)) {
			return;
		}
		coloredCutoutModelCopyLayerRender(this.model, TEXTURE, poseStack, submitNodeCollector, lightCoords, state, -1, 4);
	}
}
