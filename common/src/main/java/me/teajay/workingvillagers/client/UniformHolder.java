package me.teajay.workingvillagers.client;

/**
 * The color of a guard's uniform in its render state (see {@code VillagerRenderStateMixin}):
 * the color of the leather chestplate it wears, 0 for none.
 */
public interface UniformHolder {
	int workingvillagers$getUniform();

	void workingvillagers$setUniform(int color);
}
