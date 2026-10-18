package me.teajay.workingvillagers.mixin.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Changes the title of an open screen, for a chest that was just named. */
@Mixin(Screen.class)
public interface ScreenAccessor {
	@Mutable
	@Accessor("title")
	void workingvillagers$setTitle(Component title);
}
