package me.teajay.workingvillagers.client;

import net.minecraft.client.renderer.item.ItemStackRenderState;

/**
 * The work of a villager in its render state (see {@code VillagerRenderStateMixin}): how
 * {@code VillagerModelMixin} poses its arms and what {@link WorkItemLayer} puts in its hand.
 */
public interface WorkPoseHolder {
	WorkMotion workingvillagers$getWorkMotion();

	/** The progress of the swing, from 0 to 1, 0 when the villager does not swing. */
	float workingvillagers$getWorkSwing();

	void workingvillagers$setWorkPose(WorkMotion motion, float swing);

	/** The held item as players hold it, shown when the arms are apart. */
	ItemStackRenderState workingvillagers$getHandItem();
}
