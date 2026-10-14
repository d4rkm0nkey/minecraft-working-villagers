package me.teajay.workingvillagers.carrier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.teajay.workingvillagers.job.HiredVillager;
import me.teajay.workingvillagers.job.WorkerStatus;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jspecify.annotations.Nullable;

/**
 * Where the routes shown in the carrier menu live: with a carrier, or on a basket in the
 * player's hand that is not handed over yet.
 */
public interface RouteHolder {
	/** Who or what the routes belong to, the title of the menu. */
	Component name();

	List<Route> routes();

	void removeRoute(int index);

	void addRoute(Route route);

	void renameRoute(int index, String name);

	/** Lets the player pick the containers of a new route by clicking them. */
	void startSelection(ServerPlayer player);

	/** The route the basket holds items for, -1 if none. */
	int activeRoute();

	/** The contents of the basket. */
	Container basket();

	/** Whether the player may take items out of the basket in the menu. */
	boolean canTakeFromBasket();

	/** Whether the player may delete routes. */
	boolean canEdit(Player player);

	/** Whether the player may decide who changes the routes. */
	default boolean canChangeSharing(Player player) {
		return false;
	}

	boolean shared();

	/** What the carrier is doing right now, empty for a basket. */
	default Optional<Component> status() {
		return Optional.empty();
	}

	default void setShared(boolean shared) {
	}

	boolean stillValid(Player player);

	/** The carrier, null for a basket. */
	default @Nullable Villager worker() {
		return null;
	}

	static RouteHolder of(Villager carrier) {
		return new RouteHolder() {
			@Override
			public Villager worker() {
				return carrier;
			}

			private CarrierData data() {
				CarrierData data = ((HiredVillager) carrier).workingvillagers$getCarrier();
				return data != null ? data : new CarrierData(List.of(), List.of(), -1);
			}

			@Override
			public Component name() {
				return Component.translatable("gui.workingvillagers.carrier.title", carrier.getName());
			}

			@Override
			public List<Route> routes() {
				return this.data().routes();
			}

			@Override
			public void removeRoute(int index) {
				this.data().removeRoute(index);
			}

			@Override
			public void addRoute(Route route) {
				this.data().routes().add(route);
			}

			@Override
			public void renameRoute(int index, String name) {
				this.data().routes().set(index, this.data().routes().get(index).withName(name));
			}

			@Override
			public void startSelection(ServerPlayer player) {
				RouteSelection.startForCarrier(player, carrier);
			}

			@Override
			public int activeRoute() {
				return this.data().activeRoute();
			}

			@Override
			public Container basket() {
				return this.data().basket();
			}

			@Override
			public boolean canTakeFromBasket() {
				return true;
			}

			@Override
			public boolean canEdit(Player player) {
				return Carriers.canEdit(carrier, player);
			}

			@Override
			public boolean canChangeSharing(Player player) {
				return Carriers.canChangeSharing(carrier, player);
			}

			@Override
			public boolean shared() {
				return this.data().shared();
			}

			@Override
			public Optional<Component> status() {
				return Optional.of(WorkerStatus.describe(carrier));
			}

			@Override
			public void setShared(boolean shared) {
				this.data().setShared(shared);
			}

			@Override
			public boolean stillValid(Player player) {
				return carrier.isAlive() && Carriers.isCarrier(carrier) && ((HiredVillager) carrier).workingvillagers$getCarrier() != null
					&& player.distanceToSqr(carrier) < 12.0 * 12.0;
			}
		};
	}

	/** The routes stored on the basket in the player's hand. Its contents can only be looked at. */
	static RouteHolder of(Player owner, InteractionHand hand) {
		ItemStack basket = owner.getItemInHand(hand);
		SimpleContainer contents = new SimpleContainer(CarrierData.CAPACITY);
		basket.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream().forEach(contents::addItem);
		return new RouteHolder() {
			@Override
			public Component name() {
				return basket.getHoverName();
			}

			@Override
			public List<Route> routes() {
				return CarrierData.readRoutes(basket);
			}

			@Override
			public void removeRoute(int index) {
				List<Route> routes = new ArrayList<>(this.routes());
				routes.remove(index);
				CarrierData.writeRoutes(basket, routes);
			}

			@Override
			public void addRoute(Route route) {
				List<Route> routes = new ArrayList<>(this.routes());
				routes.add(route);
				CarrierData.writeRoutes(basket, routes);
			}

			@Override
			public void renameRoute(int index, String name) {
				List<Route> routes = new ArrayList<>(this.routes());
				routes.set(index, routes.get(index).withName(name));
				CarrierData.writeRoutes(basket, routes);
			}

			@Override
			public void startSelection(ServerPlayer player) {
				RouteSelection.startForBasket(player, hand);
			}

			@Override
			public int activeRoute() {
				return -1;
			}

			@Override
			public Container basket() {
				return contents;
			}

			@Override
			public boolean canTakeFromBasket() {
				return false;
			}

			@Override
			public boolean canEdit(Player player) {
				return true;
			}

			@Override
			public boolean shared() {
				return false;
			}

			@Override
			public boolean stillValid(Player player) {
				return player.getItemInHand(hand) == basket;
			}
		};
	}
}
