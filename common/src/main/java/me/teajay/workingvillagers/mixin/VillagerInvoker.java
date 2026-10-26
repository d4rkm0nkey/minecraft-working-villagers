package me.teajay.workingvillagers.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Rises a villager a level, with its new trades (see {@code WorkerLevels}). */
@Mixin(Villager.class)
public interface VillagerInvoker {
	@Invoker("increaseMerchantCareer")
	void workingvillagers$increaseMerchantCareer(ServerLevel level);
}
