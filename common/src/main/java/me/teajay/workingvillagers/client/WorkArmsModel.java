package me.teajay.workingvillagers.client;

import com.mojang.blaze3d.vertex.PoseStack;

/** The villager model with the arms apart of workers (see {@code VillagerModelMixin}). */
public interface WorkArmsModel {
	void workingvillagers$translateToRightHand(PoseStack poseStack);
}
