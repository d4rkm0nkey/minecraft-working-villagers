package me.teajay.workingvillagers.mixin;

import java.util.function.BooleanSupplier;
import me.teajay.workingvillagers.station.StationNotes;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void workingvillagers$tickNotes(BooleanSupplier haveTime, CallbackInfo ci) {
		StationNotes.tick((ServerLevel) (Object) this);
	}
}
