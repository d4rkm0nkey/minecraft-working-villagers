package me.teajay.workingvillagers.mixin;

import me.teajay.workingvillagers.menu.CutterMenu;
import me.teajay.workingvillagers.menu.StationMenu;
import me.teajay.workingvillagers.menu.StationTabFlags;
import me.teajay.workingvillagers.menu.StationWorker;
import me.teajay.workingvillagers.station.StationType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the vanilla loom the shepherd's workstation: the button with the shepherd's face in
 * its screen (see {@code LoomScreenMixin}) opens the overview of the station, and the menu
 * tells the screen whether a shepherd works there.
 */
@Mixin(LoomMenu.class)
public abstract class LoomMenuMixin extends AbstractContainerMenu implements StationWorker {
	@Shadow
	@Final
	private ContainerLevelAccess access;

	@Unique
	private DataSlot workingvillagers$worker = DataSlot.standalone();

	private LoomMenuMixin(MenuType<?> menuType, int containerId) {
		super(menuType, containerId);
	}

	/** The client constructor ends up here too; there the access does nothing. */
	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
	private void workingvillagers$worker(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
		access.execute((world, pos) -> {
			if (world instanceof ServerLevel serverLevel) {
				this.workingvillagers$worker = StationTabFlags.slot(serverLevel, pos.immutable(), StationType.LOOM);
			}
		});
		this.addDataSlot(this.workingvillagers$worker);
	}

	@Override
	public int workingvillagers$tabFlags() {
		return this.workingvillagers$worker.get();
	}

	/** The loom's own buttons pick banner patterns; this one is far past the last of them. */
	@Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
	private void workingvillagers$openStation(Player player, int buttonId, CallbackInfoReturnable<Boolean> cir) {
		if (buttonId == CutterMenu.STATION_BUTTON) {
			this.access.execute((level, pos) -> player.openMenu(StationMenu.provider(StationType.LOOM, level, pos)));
			cir.setReturnValue(true);
		}
	}
}
