package me.teajay.workingvillagers.mixin.client;

import me.teajay.workingvillagers.client.UniformHolder;
import me.teajay.workingvillagers.client.UniformLayer;
import me.teajay.workingvillagers.client.WickerBasketLayer;
import me.teajay.workingvillagers.client.WorkMotion;
import me.teajay.workingvillagers.client.WorkItemLayer;
import me.teajay.workingvillagers.client.WorkPoseHolder;
import me.teajay.workingvillagers.client.WorkerNoticeRenderer;
import me.teajay.workingvillagers.job.HiredVillager;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the basket on the back of carriers and the uniform of guards, lets workers hold
 * their arms apart at work (see {@code WorkMotion}) and passes on the sign above their head.
 */
@Mixin(VillagerRenderer.class)
public abstract class VillagerRendererMixin extends AgeableMobRenderer<Villager, VillagerRenderState, VillagerModel> {
	private VillagerRendererMixin(EntityRendererProvider.Context context, VillagerModel adultModel, VillagerModel babyModel, float shadow) {
		super(context, adultModel, babyModel, shadow);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void workingvillagers$addLayers(EntityRendererProvider.Context context, CallbackInfo ci) {
		this.addLayer(new WickerBasketLayer(this));
		this.addLayer(new UniformLayer(this));
		this.addLayer(new WorkItemLayer(this));
	}

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/npc/villager/Villager;Lnet/minecraft/client/renderer/entity/state/VillagerRenderState;F)V",
		at = @At("TAIL"))
	private void workingvillagers$extractWorkerLooks(Villager villager, VillagerRenderState state, float partialTicks, CallbackInfo ci) {
		((UniformHolder) state).workingvillagers$setUniform(UniformLayer.colorOf(villager));
		WorkerNoticeRenderer.extract(villager, state, partialTicks);
		HiredVillager hired = (HiredVillager) villager;
		if (villager.isSwinging()) {
			hired.workingvillagers$setLastSwing(villager.tickCount);
		}
		WorkMotion motion = WorkMotion.of(villager, villager.tickCount - hired.workingvillagers$getLastSwing());
		WorkPoseHolder pose = (WorkPoseHolder) state;
		pose.workingvillagers$setWorkPose(motion, villager.getSwingAnimation(partialTicks));
		if (motion == WorkMotion.NONE) {
			pose.workingvillagers$getHandItem().clear();
		} else {
			// In the right hand instead of on the crossed arms
			this.itemModelResolver.updateForLiving(pose.workingvillagers$getHandItem(), villager.getMainHandItem(), ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, villager);
			state.heldItem.clear();
		}
	}
}
