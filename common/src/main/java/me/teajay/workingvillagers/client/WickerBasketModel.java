package me.teajay.workingvillagers.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;

/**
 * The basket on a carrier's back, in the coordinates of the villager model: the body spans
 * x -4..4 and y 0..12 (downwards), the robe ends at z 3.5 on the back.
 */
public class WickerBasketModel extends EntityModel<VillagerRenderState> {
	public WickerBasketModel() {
		super(createLayer().bakeRoot());
	}

	private static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild(
			"basket",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-3.5F, 1.0F, 3.5F, 7.0F, 9.0F, 4.0F)
				.texOffs(0, 14).addBox(-4.0F, 0.5F, 3.0F, 8.0F, 1.0F, 5.0F),
			PartPose.ZERO
		);
		return LayerDefinition.create(mesh, 32, 32);
	}
}
