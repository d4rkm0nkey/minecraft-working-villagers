package me.teajay.workingvillagers.mixin.client;

import me.teajay.workingvillagers.client.UniformHolder;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Carries the color of a guard's uniform to {@code UniformLayer}. */
@Mixin(VillagerRenderState.class)
public abstract class VillagerRenderStateMixin implements UniformHolder {
	@Unique
	private int workingvillagers$uniform;

	@Override
	public int workingvillagers$getUniform() {
		return this.workingvillagers$uniform;
	}

	@Override
	public void workingvillagers$setUniform(int color) {
		this.workingvillagers$uniform = color;
	}
}
