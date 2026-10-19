package me.teajay.workingvillagers.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Sets the custom name of a container, which vanilla only does from the placed item. */
@Mixin(BaseContainerBlockEntity.class)
public interface BaseContainerBlockEntityAccessor {
	@Accessor("name")
	void workingvillagers$setName(@Nullable Component name);
}
