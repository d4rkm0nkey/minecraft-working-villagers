package me.teajay.workingvillagers.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import me.teajay.workingvillagers.client.WorkArmsModel;
import me.teajay.workingvillagers.client.WorkMotion;
import me.teajay.workingvillagers.client.WorkPoseHolder;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives the villager model two arms besides the crossed ones, shown instead of them while a
 * worker works (see {@code WorkMotion}).
 * <p>
 * The arms are cut from the texture of the crossed arms: the upper arm from the arm, the hand
 * from the forearms. So the clothes of the professions and resource packs fit them.
 */
@Mixin(VillagerModel.class)
public abstract class VillagerModelMixin extends EntityModel<VillagerRenderState> implements WorkArmsModel {
	@Unique
	private static final String RIGHT_ARM = "workingvillagers_right_arm";
	@Unique
	private static final String LEFT_ARM = "workingvillagers_left_arm";

	@Shadow
	@Final
	private ModelPart head;
	@Shadow
	@Final
	private ModelPart arms;
	/** Null in the model of babies, which do not work. */
	@Unique
	private @Nullable ModelPart workingvillagers$body;
	@Unique
	private @Nullable ModelPart workingvillagers$rightArm;
	@Unique
	private @Nullable ModelPart workingvillagers$leftArm;

	private VillagerModelMixin(ModelPart root) {
		super(root);
	}

	@Inject(method = "createBodyModel", at = @At("RETURN"))
	private static void workingvillagers$addArms(CallbackInfoReturnable<MeshDefinition> cir) {
		PartDefinition root = cir.getReturnValue().getRoot();
		root.addOrReplaceChild(RIGHT_ARM, CubeListBuilder.create()
				.texOffs(44, 22).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F)
				.texOffs(40, 38).addBox(-3.0F, 6.0F, -2.0F, 4.0F, 4.0F, 4.0F),
			PartPose.offset(-5.0F, 2.0F, 0.0F));
		root.addOrReplaceChild(LEFT_ARM, CubeListBuilder.create().mirror()
				.texOffs(44, 22).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F)
				.texOffs(40, 38).addBox(-1.0F, 6.0F, -2.0F, 4.0F, 4.0F, 4.0F),
			PartPose.offset(5.0F, 2.0F, 0.0F));
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void workingvillagers$findArms(ModelPart root, CallbackInfo ci) {
		if (root.hasChild(RIGHT_ARM) && root.hasChild(LEFT_ARM) && root.hasChild("body")) {
			this.workingvillagers$body = root.getChild("body");
			this.workingvillagers$rightArm = root.getChild(RIGHT_ARM);
			this.workingvillagers$leftArm = root.getChild(LEFT_ARM);
		}
	}

	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/VillagerRenderState;)V", at = @At("TAIL"))
	private void workingvillagers$poseArms(VillagerRenderState state, CallbackInfo ci) {
		ModelPart body = this.workingvillagers$body;
		ModelPart rightArm = this.workingvillagers$rightArm;
		ModelPart leftArm = this.workingvillagers$leftArm;
		if (body == null || rightArm == null || leftArm == null) {
			return;
		}
		WorkPoseHolder pose = (WorkPoseHolder) state;
		WorkMotion motion = pose.workingvillagers$getWorkMotion();
		boolean apart = motion != WorkMotion.NONE;
		this.arms.visible = !apart;
		rightArm.visible = apart;
		leftArm.visible = apart;
		if (apart) {
			motion.pose(this.head, body, rightArm, leftArm, pose.workingvillagers$getWorkSwing(), state.walkAnimationPos, state.walkAnimationSpeed);
		}
	}

	@Override
	public void workingvillagers$translateToRightHand(PoseStack poseStack) {
		this.root.translateAndRotate(poseStack);
		if (this.workingvillagers$rightArm != null) {
			this.workingvillagers$rightArm.translateAndRotate(poseStack);
		}
	}
}
