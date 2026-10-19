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
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes the vanilla grindstone the weaponsmith's workstation: the tabs above its screen (see
 * {@code GrindstoneScreenMixin}) open the overview of the station, and the menu tells the screen what
 * they show (see {@link StationTabFlags}).
 */
@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin extends AbstractContainerMenu implements StationWorker {
	@Shadow
	@Final
	private ContainerLevelAccess access;

	@Unique
	private DataSlot workingvillagers$worker = DataSlot.standalone();

	private GrindstoneMenuMixin(MenuType<?> menuType, int containerId) {
		super(menuType, containerId);
	}

	/** The client constructor ends up here too; there the access does nothing. */
	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
	private void workingvillagers$worker(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
		access.execute((world, pos) -> {
			if (world instanceof ServerLevel serverLevel) {
				this.workingvillagers$worker = StationTabFlags.slot(serverLevel, pos.immutable(), StationType.GRINDSTONE);
			}
		});
		this.addDataSlot(this.workingvillagers$worker);
	}

	@Override
	public int workingvillagers$tabFlags() {
		return this.workingvillagers$worker.get();
	}

	/** The grindstone has no buttons of its own. */
	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		if (buttonId == CutterMenu.STATION_BUTTON) {
			this.access.execute((level, pos) -> player.openMenu(StationMenu.provider(StationType.GRINDSTONE, level, pos)));
			return true;
		}
		return super.clickMenuButton(player, buttonId);
	}
}
