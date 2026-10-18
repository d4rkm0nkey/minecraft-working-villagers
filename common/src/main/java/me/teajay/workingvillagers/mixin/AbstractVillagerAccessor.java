package me.teajay.workingvillagers.mixin;

import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The offers of a villager as they are, without generating them (see {@code VillagerMixin}). */
@Mixin(AbstractVillager.class)
public interface AbstractVillagerAccessor {
	@Accessor("offers")
	@Nullable MerchantOffers workingvillagers$getOffers();
}
