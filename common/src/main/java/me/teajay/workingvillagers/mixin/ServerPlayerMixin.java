package me.teajay.workingvillagers.mixin;

import me.teajay.workingvillagers.station.PitPreviews;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A player holding string sees the pits at the pit stakes nearby (see {@link PitPreviews}). */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void workingvillagers$previewPits(CallbackInfo ci) {
		PitPreviews.tick((ServerPlayer) (Object) this);
	}
}
