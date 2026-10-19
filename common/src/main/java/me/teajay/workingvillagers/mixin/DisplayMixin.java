package me.teajay.workingvillagers.mixin;

import me.teajay.workingvillagers.station.StationNotes;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Removes the station notes of earlier versions, see {@link StationNotes}. */
@Mixin(Display.class)
public abstract class DisplayMixin {
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void workingvillagers$removeOldNote(CallbackInfo ci) {
		Display display = (Display) (Object) this;
		if (!display.level().isClientSide() && StationNotes.isOldNote(display)) {
			display.discard();
			ci.cancel();
		}
	}
}
