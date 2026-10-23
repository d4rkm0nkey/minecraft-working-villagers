package me.teajay.workingvillagers.fabric.gametest.mixin;

import me.teajay.workingvillagers.gametest.simulation.SimDeliveries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The simulation notes which villager is thinking, to tell whose deliveries go into a chest. */
@Mixin(Villager.class)
public abstract class VillagerTickMixin {
	@Inject(method = "customServerAiStep", at = @At("HEAD"))
	private void workingvillagersTest$enter(ServerLevel level, CallbackInfo ci) {
		SimDeliveries.enter((Villager) (Object) this);
	}

	@Inject(method = "customServerAiStep", at = @At("RETURN"))
	private void workingvillagersTest$exit(ServerLevel level, CallbackInfo ci) {
		SimDeliveries.exit();
	}
}
