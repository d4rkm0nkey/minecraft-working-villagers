package me.teajay.workingvillagers.carrier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import me.teajay.workingvillagers.WorkingVillagers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * What a carrier has: its routes and the contents of its basket. While the basket is in a
 * player's hands, the same data lives on the basket item.
 */
public class CarrierData {
	/** Stacks a novice carrier can carry. Will grow with the level later. */
	public static final int CAPACITY = 6;
	static final String TAG_ROUTES = "Routes";
	static final String TAG_SHARED = "Shared";

	public static final Codec<CarrierData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Route.LIST_CODEC.optionalFieldOf("routes", List.of()).forGetter(data -> data.routes),
		ItemStack.CODEC.listOf().optionalFieldOf("basket", List.of()).forGetter(CarrierData::contents),
		Codec.INT.optionalFieldOf("load_route", -1).forGetter(data -> data.loadRoute),
		Codec.BOOL.optionalFieldOf("shared", false).forGetter(data -> data.shared)
	).apply(i, CarrierData::new));

	private final List<Route> routes;
	private final SimpleContainer basket = new SimpleContainer(CAPACITY);
	/** The route the items in the basket belong to, -1 if unknown. */
	private int loadRoute;
	/** Whether every player may change the routes, not only the employer. */
	private boolean shared;

	public CarrierData(List<Route> routes, List<ItemStack> contents, int loadRoute) {
		this(routes, contents, loadRoute, false);
	}

	public CarrierData(List<Route> routes, List<ItemStack> contents, int loadRoute, boolean shared) {
		this.routes = new ArrayList<>(routes);
		contents.forEach(this.basket::addItem);
		this.loadRoute = loadRoute;
		this.shared = shared;
	}

	public boolean shared() {
		return this.shared;
	}

	public void setShared(boolean shared) {
		this.shared = shared;
	}

	public List<Route> routes() {
		return this.routes;
	}

	public SimpleContainer basket() {
		return this.basket;
	}

	public int loadRoute() {
		return this.loadRoute;
	}

	public void setLoadRoute(int loadRoute) {
		this.loadRoute = loadRoute;
	}

	/**
	 * Removes a route. If the basket holds items of that route, it no longer knows where they
	 * belong, so the carrier brings them to the target of its first route.
	 */
	public void removeRoute(int index) {
		this.routes.remove(index);
		if (this.loadRoute == index) {
			this.loadRoute = -1;
		} else if (this.loadRoute > index) {
			this.loadRoute--;
		}
	}

	/** The route the carrier is carrying items for right now, -1 if none. */
	public int activeRoute() {
		return this.basket.isEmpty() ? -1 : this.loadRoute;
	}

	private List<ItemStack> contents() {
		return this.basket.getItems().stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList();
	}

	/** Takes routes and contents from a basket item. */
	public static CarrierData fromBasket(ItemStack basket) {
		ItemContainerContents contents = basket.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
		List<ItemStack> items = new ArrayList<>();
		contents.nonEmptyItemCopyStream().forEach(items::add);
		CustomData data = basket.get(DataComponents.CUSTOM_DATA);
		boolean shared = data != null && data.copyTag().getBooleanOr(TAG_SHARED, false);
		return new CarrierData(readRoutes(basket), items, -1, shared);
	}

	/** A basket item with these routes and contents. */
	public ItemStack toBasket() {
		ItemStack basket = new ItemStack(WorkingVillagers.WICKER_BASKET.get());
		basket.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.contents()));
		writeRoutes(basket, this.routes);
		if (this.shared) {
			CustomData.update(DataComponents.CUSTOM_DATA, basket, tag -> tag.putBoolean(TAG_SHARED, true));
		}
		return basket;
	}

	public static List<Route> readRoutes(ItemStack basket) {
		CustomData data = basket.get(DataComponents.CUSTOM_DATA);
		return data == null ? List.of() : data.copyTag().read(TAG_ROUTES, Route.LIST_CODEC).orElse(List.of());
	}

	public static void writeRoutes(ItemStack basket, List<Route> routes) {
		CustomData.update(DataComponents.CUSTOM_DATA, basket, tag -> tag.store(TAG_ROUTES, Route.LIST_CODEC, List.copyOf(routes)));
	}
}
