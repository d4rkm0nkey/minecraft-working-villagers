package me.teajay.workingvillagers.mixin;

import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Brain.class)
public interface BrainAccessor {
	@Accessor("activityRequirements")
	Map<Activity, Set<Pair<MemoryModuleType<?>, MemoryStatus>>> workingvillagers$getActivityRequirements();
}
