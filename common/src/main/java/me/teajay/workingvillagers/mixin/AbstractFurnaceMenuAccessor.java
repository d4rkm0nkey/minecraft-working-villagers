package me.teajay.workingvillagers.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The furnace behind a furnace menu: on the server its block entity. */
@Mixin(AbstractFurnaceMenu.class)
public interface AbstractFurnaceMenuAccessor {
	@Accessor("container")
	Container workingvillagers$getContainer();
}
