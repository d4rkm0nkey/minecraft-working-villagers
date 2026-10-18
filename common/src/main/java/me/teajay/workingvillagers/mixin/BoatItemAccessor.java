package me.teajay.workingvillagers.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.BoatItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Which boat a boat item puts on the water, for the fisher. */
@Mixin(BoatItem.class)
public interface BoatItemAccessor {
	@Accessor("entityType")
	EntityType<? extends AbstractBoat> workingvillagers$getEntityType();
}
