package me.teajay.workingvillagers.mixin.client;

import me.teajay.workingvillagers.client.WickerBasketLayer;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the basket on the back of carriers.
 */
@Mixin(VillagerRenderer.class)
public abstract class VillagerRendererMixin extends AgeableMobRenderer<Villager, VillagerRenderState, VillagerModel> {
	private VillagerRendererMixin(EntityRendererProvider.Context context, VillagerModel adultModel, VillagerModel babyModel, float shadow) {
		super(context, adultModel, babyModel, shadow);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void workingvillagers$addBasketLayer(EntityRendererProvider.Context context, CallbackInfo ci) {
		this.addLayer(new WickerBasketLayer(this));
	}
}
