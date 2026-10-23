package me.teajay.workingvillagers.mixin.client;

import me.teajay.workingvillagers.client.WorkerSkins;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Workers with a vanilla job wear the vanilla clothes of it (see {@code WorkerSkins}), as
 * villagers and as zombie villagers. The hat comes along, it is read from the same texture.
 */
@Mixin(VillagerProfessionLayer.class)
public abstract class VillagerProfessionLayerMixin {
	@ModifyVariable(method = "getIdentifier(Ljava/lang/String;Lnet/minecraft/resources/Identifier;)Lnet/minecraft/resources/Identifier;",
		at = @At("HEAD"), argsOnly = true)
	private Identifier workingvillagers$vanillaLook(Identifier key, String type) {
		return type.equals("profession") ? WorkerSkins.look(key) : key;
	}
}
