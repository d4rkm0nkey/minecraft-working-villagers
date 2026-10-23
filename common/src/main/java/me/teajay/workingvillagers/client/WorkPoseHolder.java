package me.teajay.workingvillagers.client;

/**
 * The swing of a villager at work in its render state (see {@code VillagerRenderStateMixin}),
 * moved into its arms by {@code VillagerModelMixin}.
 */
public interface WorkPoseHolder {
	WorkMotion workingvillagers$getWorkMotion();

	/** The progress of the swing, from 0 to 1, 0 when the villager does not swing. */
	float workingvillagers$getWorkSwing();

	void workingvillagers$setWorkPose(WorkMotion motion, float swing);
}
