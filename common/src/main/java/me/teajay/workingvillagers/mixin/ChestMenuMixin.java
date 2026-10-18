package me.teajay.workingvillagers.mixin;

import me.teajay.workingvillagers.menu.CutterMenu;
import me.teajay.workingvillagers.menu.StationMenu;
import me.teajay.workingvillagers.menu.StationTabFlags;
import me.teajay.workingvillagers.menu.StationWorker;
import me.teajay.workingvillagers.station.StationType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes the vanilla barrel the fisher's workstation: the barrel opens a chest menu, which
 * tells its screen what the tabs above it show (see {@code ContainerScreenMixin}); for any
 * other chest the data slot stays 0 and there are no tabs. The tabs open the overview.
 */
@Mixin(ChestMenu.class)
public abstract class ChestMenuMixin extends AbstractContainerMenu implements StationWorker {
	@Unique
	private DataSlot workingvillagers$tabs = DataSlot.standalone();
	/** Server: the barrel this menu belongs to, null for any other chest. */
	@Unique
	private @Nullable BlockPos workingvillagers$barrel;

	private ChestMenuMixin(MenuType<?> menuType, int containerId) {
		super(menuType, containerId);
	}

	/** All constructors end up here, on the client with an empty container. */
	@Inject(method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;I)V", at = @At("TAIL"))
	private void workingvillagers$tabs(MenuType<?> type, int containerId, Inventory inventory, Container container, int rows, CallbackInfo ci) {
		if (container instanceof BarrelBlockEntity barrel && barrel.getLevel() instanceof ServerLevel level) {
			this.workingvillagers$barrel = barrel.getBlockPos();
			this.workingvillagers$tabs = StationTabFlags.slot(level, barrel.getBlockPos(), StationType.BARREL);
		}
		this.addDataSlot(this.workingvillagers$tabs);
	}

	@Override
	public int workingvillagers$tabFlags() {
		return this.workingvillagers$tabs.get();
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		BlockPos barrel = this.workingvillagers$barrel;
		if (buttonId == CutterMenu.STATION_BUTTON && barrel != null) {
			player.openMenu(StationMenu.provider(StationType.BARREL, player.level(), barrel));
			return true;
		}
		return super.clickMenuButton(player, buttonId);
	}
}
