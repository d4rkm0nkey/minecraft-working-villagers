package me.teajay.workingvillagers.menu;

import java.util.List;
import java.util.Optional;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.carrier.CarrierData;
import me.teajay.workingvillagers.carrier.Carriers;
import me.teajay.workingvillagers.carrier.Route;
import me.teajay.workingvillagers.carrier.RouteHolder;
import me.teajay.workingvillagers.job.PersonalChest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The carrier menu: the routes of a carrier (or of a basket in the hand), each with a button
 * to delete it, the carrier's personal chest and the contents of the basket.
 */
public class CarrierMenu extends AbstractContainerMenu {
	public static final int BASKET_X = 62;
	public static final int BASKET_Y = 145;
	public static final int INVENTORY_Y = 176;
	/** Button that switches between "only the employer" and "everybody" changing routes. */
	public static final int SHARE_BUTTON = 50;
	/** Button of a free route slot: closes the menu and lets the player pick the containers. */
	public static final int NEW_ROUTE_BUTTON = 60;
	/** Closes the menu, the player then right-clicks the carrier's personal chest. */
	public static final int PERSONAL_CHEST_BUTTON = 70;
	private static final String TAG_ROUTES = "Routes";
	private static final String TAG_ACTIVE = "Active";
	private static final String TAG_CAN_EDIT = "CanEdit";
	private static final String TAG_CAN_TAKE = "CanTake";
	private static final String TAG_SHARED = "Shared";
	private static final String TAG_CAN_SHARE = "CanShare";
	private static final String TAG_STATUS = "Status";

	/** Server only. */
	private final @Nullable RouteHolder holder;
	private final Player player;
	private final InfoSlot info = new InfoSlot();

	/** Client side. */
	public CarrierMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, null);
	}

	public CarrierMenu(int containerId, Inventory inventory, @Nullable RouteHolder holder) {
		super(WorkingVillagers.CARRIER_MENU.get(), containerId);
		this.holder = holder;
		this.player = inventory.player;
		Container basket = holder != null ? holder.basket() : new SimpleContainer(CarrierData.CAPACITY);
		for (int i = 0; i < CarrierData.CAPACITY; i++) {
			this.addSlot(new Slot(basket, i, BASKET_X + i * 18, BASKET_Y) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return false;
				}

				@Override
				public boolean mayPickup(Player player) {
					return CarrierMenu.this.canTakeFromBasket();
				}
			});
		}
		this.addStandardInventorySlots(inventory, 8, INVENTORY_Y);
		this.addSlot(this.info);
		this.updateInfo();
	}

	private void updateInfo() {
		if (this.holder == null) {
			return;
		}
		CompoundTag tag = new CompoundTag();
		tag.store(TAG_ROUTES, Route.LIST_CODEC, List.copyOf(this.holder.routes()));
		tag.putInt(TAG_ACTIVE, this.holder.activeRoute());
		tag.putBoolean(TAG_CAN_EDIT, this.holder.canEdit(this.player));
		tag.putBoolean(TAG_CAN_TAKE, this.holder.canTakeFromBasket());
		tag.putBoolean(TAG_SHARED, this.holder.shared());
		tag.putBoolean(TAG_CAN_SHARE, this.holder.canChangeSharing(this.player));
		this.holder.status().ifPresent(status -> tag.store(TAG_STATUS, ComponentSerialization.CODEC, status));
		Villager worker = this.holder.worker();
		if (worker != null && worker.level() instanceof ServerLevel level) {
			PersonalChestInfo.write(level, worker, tag);
		}
		this.info.setInfo(tag);
	}

	@Override
	public void broadcastChanges() {
		this.updateInfo();
		super.broadcastChanges();
	}

	public List<Route> getRoutes() {
		return this.info.info().read(TAG_ROUTES, Route.LIST_CODEC).orElse(List.of());
	}

	/** The route the carrier carries items for right now, -1 if none. */
	public int getActiveRoute() {
		return this.info.info().getIntOr(TAG_ACTIVE, -1);
	}

	public boolean canEdit() {
		return this.info.info().getBooleanOr(TAG_CAN_EDIT, false);
	}

	public boolean canTakeFromBasket() {
		return this.info.info().getBooleanOr(TAG_CAN_TAKE, false);
	}

	/** What the carrier is doing right now, empty for a basket. */
	public Optional<Component> getStatus() {
		return this.info.info().read(TAG_STATUS, ComponentSerialization.CODEC);
	}

	/** The carrier's personal chest, empty for a basket. */
	public Optional<PersonalChestInfo> getPersonalChest() {
		return PersonalChestInfo.read(this.info.info());
	}

	/** Whether everybody may change the routes. */
	public boolean isShared() {
		return this.info.info().getBooleanOr(TAG_SHARED, false);
	}

	/** Whether this player may decide who changes the routes. */
	public boolean canChangeSharing() {
		return this.info.info().getBooleanOr(TAG_CAN_SHARE, false);
	}

	/** The buttons 0 to 2 delete the route with that index. */
	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		if (buttonId == SHARE_BUTTON && this.holder != null && this.holder.canChangeSharing(player)) {
			this.holder.setShared(!this.holder.shared());
			this.updateInfo();
			return true;
		}
		if (buttonId == PERSONAL_CHEST_BUTTON) {
			Villager worker = this.holder == null ? null : this.holder.worker();
			if (worker == null || !this.holder.canEdit(player) || !(player instanceof ServerPlayer serverPlayer)) {
				return false;
			}
			PersonalChest.pick(serverPlayer, worker, picker -> Carriers.openMenu(worker, picker));
			return true;
		}
		if (buttonId == NEW_ROUTE_BUTTON) {
			if (this.holder == null || !this.holder.canEdit(player) || this.holder.routes().size() >= Route.MAX_ROUTES
				|| !(player instanceof ServerPlayer serverPlayer)) {
				return false;
			}
			this.holder.startSelection(serverPlayer);
			return true;
		}
		if (this.holder == null || !this.holder.canEdit(player) || buttonId < 0 || buttonId >= this.holder.routes().size()) {
			return false;
		}
		this.holder.removeRoute(buttonId);
		this.updateInfo();
		return true;
	}

	/** Server: renames a route, sent by the screen (see RenameRoutePayload). */
	public void renameRoute(Player player, int index, String name) {
		if (this.holder != null && this.holder.canEdit(player) && index >= 0 && index < this.holder.routes().size()) {
			this.holder.renameRoute(index, name);
			this.broadcastChanges();
		}
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = this.slots.get(slotIndex);
		if (slotIndex >= CarrierData.CAPACITY || !slot.hasItem() || !slot.mayPickup(player)) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		int inventoryEnd = CarrierData.CAPACITY + 36;
		if (!this.moveItemStackTo(stack, CarrierData.CAPACITY, inventoryEnd, true)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return original;
	}

	@Override
	public boolean stillValid(Player player) {
		return this.holder == null || this.holder.stillValid(player);
	}
}
