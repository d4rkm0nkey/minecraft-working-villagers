package me.teajay.workingvillagers.mixin;

import me.teajay.workingvillagers.menu.CutterMenu;
import me.teajay.workingvillagers.menu.StationMenu;
import me.teajay.workingvillagers.menu.StationTabFlags;
import me.teajay.workingvillagers.menu.StationWorker;
import me.teajay.workingvillagers.station.StationType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes the vanilla brewing stand the apothecary's workstation: the tabs above its screen (see
 * {@code BrewingStandScreenMixin}) open the overview of the station, and the menu tells the
 * screen what they show (see {@link StationTabFlags}).
 */
@Mixin(BrewingStandMenu.class)
public abstract class BrewingStandMenuMixin extends AbstractContainerMenu implements StationWorker {
	@Shadow
	@Final
	private Container brewingStand;
	@Unique
	private DataSlot workingvillagers$worker = DataSlot.standalone();

	private BrewingStandMenuMixin(MenuType<?> menuType, int containerId) {
		super(menuType, containerId);
	}

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V", at = @At("TAIL"))
	private void workingvillagers$worker(int containerId, Inventory inventory, Container container, ContainerData data, CallbackInfo ci) {
		if (container instanceof BlockEntity stand && stand.getLevel() instanceof ServerLevel level) {
			this.workingvillagers$worker = StationTabFlags.slot(level, stand.getBlockPos(), StationType.BREWING_STAND);
		}
		this.addDataSlot(this.workingvillagers$worker);
	}

	@Override
	public int workingvillagers$tabFlags() {
		return this.workingvillagers$worker.get();
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		if (buttonId == CutterMenu.STATION_BUTTON && this.brewingStand instanceof BlockEntity stand && stand.getLevel() != null) {
			player.openMenu(StationMenu.provider(StationType.BREWING_STAND, stand.getLevel(), stand.getBlockPos()));
			return true;
		}
		return super.clickMenuButton(player, buttonId);
	}
}
