package me.teajay.workingvillagers.mixin;

import me.teajay.workingvillagers.menu.CutterMenu;
import me.teajay.workingvillagers.menu.StationMenu;
import me.teajay.workingvillagers.menu.StationWorker;
import me.teajay.workingvillagers.station.StationType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes the vanilla blast furnace the smith's workstation: the button with the smith's face
 * in its screen (see {@code BlastFurnaceScreenMixin}) opens the overview of the station, and
 * the menu tells the screen whether a smith works there.
 */
@Mixin(BlastFurnaceMenu.class)
public abstract class BlastFurnaceMenuMixin extends AbstractFurnaceMenu implements StationWorker {
	@Unique
	private DataSlot workingvillagers$worker = DataSlot.standalone();

	private BlastFurnaceMenuMixin(MenuType<?> menuType, ResourceKey<RecipePropertySet> allowedInputs, RecipeBookType recipeBookType, int containerId, Inventory inventory) {
		super(menuType, allowedInputs, recipeBookType, containerId, inventory);
	}

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;)V", at = @At("TAIL"))
	private void workingvillagers$clientWorker(int containerId, Inventory inventory, CallbackInfo ci) {
		this.addDataSlot(this.workingvillagers$worker);
	}

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V", at = @At("TAIL"))
	private void workingvillagers$serverWorker(int containerId, Inventory inventory, Container container, ContainerData data, CallbackInfo ci) {
		if (container instanceof BlockEntity furnace && furnace.getLevel() instanceof ServerLevel level) {
			this.workingvillagers$worker = StationWorker.slot(level, furnace.getBlockPos());
		}
		this.addDataSlot(this.workingvillagers$worker);
	}

	@Override
	public boolean workingvillagers$hasWorker() {
		return this.workingvillagers$worker.get() == 1;
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		if (buttonId == CutterMenu.STATION_BUTTON
			&& ((AbstractFurnaceMenuAccessor) this).workingvillagers$getContainer() instanceof BlockEntity furnace
			&& furnace.getLevel() != null) {
			player.openMenu(StationMenu.provider(StationType.BLAST_FURNACE, furnace.getLevel(), furnace.getBlockPos()));
			return true;
		}
		return super.clickMenuButton(player, buttonId);
	}
}
