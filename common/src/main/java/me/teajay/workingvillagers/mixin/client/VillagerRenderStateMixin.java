package me.teajay.workingvillagers.mixin.client;

import me.teajay.workingvillagers.client.UniformHolder;
import me.teajay.workingvillagers.client.WorkMotion;
import me.teajay.workingvillagers.client.WorkPoseHolder;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Carries the color of a guard's uniform to {@code UniformLayer} and the work to {@code VillagerModelMixin}. */
@Mixin(VillagerRenderState.class)
public abstract class VillagerRenderStateMixin implements UniformHolder, WorkPoseHolder {
	@Unique
	private int workingvillagers$uniform;
	@Unique
	private WorkMotion workingvillagers$workMotion = WorkMotion.NONE;
	@Unique
	private float workingvillagers$workSwing;
	@Unique
	private final ItemStackRenderState workingvillagers$handItem = new ItemStackRenderState();

	@Override
	public int workingvillagers$getUniform() {
		return this.workingvillagers$uniform;
	}

	@Override
	public void workingvillagers$setUniform(int color) {
		this.workingvillagers$uniform = color;
	}

	@Override
	public WorkMotion workingvillagers$getWorkMotion() {
		return this.workingvillagers$workMotion;
	}

	@Override
	public float workingvillagers$getWorkSwing() {
		return this.workingvillagers$workSwing;
	}

	@Override
	public void workingvillagers$setWorkPose(WorkMotion motion, float swing) {
		this.workingvillagers$workMotion = motion;
		this.workingvillagers$workSwing = swing;
	}

	@Override
	public ItemStackRenderState workingvillagers$getHandItem() {
		return this.workingvillagers$handItem;
	}
}
