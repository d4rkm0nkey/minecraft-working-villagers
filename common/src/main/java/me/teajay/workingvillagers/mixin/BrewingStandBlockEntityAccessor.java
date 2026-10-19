package me.teajay.workingvillagers.mixin;

import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Whether a brewing stand brews and has fuel left, for the apothecary. */
@Mixin(BrewingStandBlockEntity.class)
public interface BrewingStandBlockEntityAccessor {
	@Accessor("brewTime")
	int workingvillagers$getBrewTime();

	@Accessor("fuel")
	int workingvillagers$getFuel();
}
