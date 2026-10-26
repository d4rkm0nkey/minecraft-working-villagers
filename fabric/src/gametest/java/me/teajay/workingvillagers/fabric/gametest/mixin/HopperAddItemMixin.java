package me.teajay.workingvillagers.fabric.gametest.mixin;

import me.teajay.workingvillagers.gametest.simulation.SimDeliveries;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Workers put items into chests like a hopper: the simulation counts what arrives. */
@Mixin(HopperBlockEntity.class)
public abstract class HopperAddItemMixin {
	@Unique
	private static Item workingvillagersTest$item;
	@Unique
	private static int workingvillagersTest$count;

	@Inject(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;",
		at = @At("HEAD"))
	private static void workingvillagersTest$before(Container from, Container into, ItemStack stack, Direction side, CallbackInfoReturnable<ItemStack> cir) {
		workingvillagersTest$item = stack.getItem();
		workingvillagersTest$count = stack.getCount();
	}

	@Inject(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;",
		at = @At("RETURN"))
	private static void workingvillagersTest$after(Container from, Container into, ItemStack stack, Direction side, CallbackInfoReturnable<ItemStack> cir) {
		int stored = workingvillagersTest$count - cir.getReturnValue().getCount();
		if (from == null && stored > 0) {
			SimDeliveries.stored(into, workingvillagersTest$item, stored);
		}
	}
}
