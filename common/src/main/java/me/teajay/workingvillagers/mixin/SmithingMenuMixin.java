package me.teajay.workingvillagers.mixin;

import me.teajay.workingvillagers.menu.CutterMenu;
import me.teajay.workingvillagers.menu.StationMenu;
import me.teajay.workingvillagers.menu.StationWorker;
import me.teajay.workingvillagers.station.StationType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes the vanilla smithing table the smith's second workstation: the button with the
 * smith's face in its screen (see {@code SmithingScreenMixin}) opens the overview of the
 * station, and the menu tells the screen whether a smith works there.
 */
@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin extends ItemCombinerMenu implements StationWorker {
	@Unique
	private DataSlot workingvillagers$worker = DataSlot.standalone();

	private SmithingMenuMixin(MenuType<?> menuType, int containerId, Inventory inventory, ContainerLevelAccess access, ItemCombinerMenuSlotDefinition slots) {
		super(menuType, containerId, inventory, access, slots);
	}

	/** All constructors end up here; on the client the access does nothing. */
	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
	private void workingvillagers$worker(int containerId, Inventory inventory, ContainerLevelAccess access, Level level, CallbackInfo ci) {
		access.execute((world, pos) -> {
			if (world instanceof ServerLevel serverLevel) {
				this.workingvillagers$worker = StationWorker.slot(serverLevel, pos);
			}
		});
		this.addDataSlot(this.workingvillagers$worker);
	}

	@Override
	public boolean workingvillagers$hasWorker() {
		return this.workingvillagers$worker.get() == 1;
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		if (buttonId == CutterMenu.STATION_BUTTON) {
			this.access.execute((level, pos) -> player.openMenu(StationMenu.provider(StationType.SMITHING_TABLE, level, pos)));
			return true;
		}
		return super.clickMenuButton(player, buttonId);
	}
}
