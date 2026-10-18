package me.teajay.workingvillagers.job;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The work of the shop assistant at the trading counter: sell the goods from the input
 * chests to the villagers around, through their own trades, and bring the emeralds to the
 * output chests.
 *
 * <p>A customer is an ordinary (not hired) adult villager with a profession within
 * {@link Config#TRADE_RADIUS} blocks of the counter that buys one of the goods for emeralds:
 * the farmer wheat, the fletcher sticks, the librarian paper. The trades work as for a
 * player: the price depends on the demand, a trade is sold out after its uses until the
 * villager restocks at its workstation, and the villager earns experience and rises in level.
 *
 * <p>For one customer it fetches as much of the goods as the customer's trades take, walks
 * to it, trades one after the other, and brings the emeralds to an output chest; what it
 * could not sell goes back into an input chest. The emeralds it earns are kept apart from its
 * wage.
 */
public class SellGoods extends Behavior<Villager> {
	private static final float SPEED = 0.5F;
	private static final int WALK_TIMEOUT = 400;
	private static final int IDLE_TICKS = 200;
	/** How close it has to be to the customer to trade. */
	private static final double TRADE_REACH = 2.5;
	/** At most this many items of goods it carries to a customer. */
	private static final int MAX_GOODS = 64;
	/** It brings its earnings to a chest once it has this many, even if there is more to sell. */
	private static final int DELIVER_AT = 32;

	private enum State { PLAN, FETCH, WALK, TRADE, DELIVER, RETURN, WAIT }

	/** A customer and the trades of it the goods are for. */
	private record Sale(Villager customer, List<MerchantOffer> offers) {
	}

	private State state = State.PLAN;
	private int stateTicks;
	private String waitReason = "shop.no_goods";
	private Object[] waitArgs = {};
	private final WorkerSupplies supplies = new WorkerSupplies();
	private @Nullable ChestTrip trip;
	private @Nullable Sale sale;
	/** The emeralds it earned and still has to deliver; the others are its wage. Not saved. */
	private int earned;

	public SellGoods() {
		super(ImmutableMap.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT), 24000);
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
		return this.canWork(level, villager);
	}

	@Override
	protected boolean canStillUse(ServerLevel level, Villager villager, long timestamp) {
		return this.canWork(level, villager) && villager.getBrain().isActive(Activity.WORK);
	}

	private boolean canWork(ServerLevel level, Villager villager) {
		if (!villager.getVillagerData().profession().is(WorkingVillagers.SHOP_ASSISTANT_PROFESSION)) {
			return false;
		}
		Workstations.Station station = this.getWorkplace(level, villager);
		return station != null && !station.validInputs(level).isEmpty() && !station.validOutputs(level).isEmpty();
	}

	private Workstations.@Nullable Station getWorkplace(ServerLevel level, Villager villager) {
		Optional<GlobalPos> jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
		if (jobSite.isEmpty() || jobSite.get().dimension() != level.dimension()) {
			return null;
		}
		return Workstations.find(level, jobSite.get().pos()).orElse(null);
	}

	@Override
	protected void start(ServerLevel level, Villager villager, long timestamp) {
		this.setState(State.PLAN);
	}

	@Override
	protected void stop(ServerLevel level, Villager villager, long timestamp) {
		WorkerStatus.clear(villager);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
		this.supplies.stop(villager);
		this.sale = null;
		if (this.trip != null) {
			this.trip.close(villager);
			this.trip = null;
		}
	}

	@Override
	protected void tick(ServerLevel level, Villager villager, long timestamp) {
		Workstations.Station station = this.getWorkplace(level, villager);
		if (station == null) {
			return;
		}
		this.stateTicks++;
		this.earned = Math.min(this.earned, WorkerSupplies.count(villager.getInventory(), WorkerSupplies::isWage));
		if (this.state == State.PLAN && this.supplies.busy(level, villager)) {
			return; // wage first, the supplies set the status
		}
		switch (this.state) {
			case PLAN -> this.plan(level, villager, station);
			case FETCH -> this.fetch(level, villager);
			case WALK -> this.walk(villager);
			case TRADE -> this.trade(level, villager);
			case DELIVER -> this.deliver(level, villager, station);
			case RETURN -> this.returnGoods(level, villager, station);
			case WAIT -> {
				if (this.stateTicks >= IDLE_TICKS) {
					this.setState(State.PLAN);
				}
			}
		}
		this.report(villager);
	}

	private void report(Villager villager) {
		switch (this.state) {
			case PLAN, FETCH -> WorkerStatus.task(villager, "shop.fetch");
			case WALK -> WorkerStatus.task(villager, "shop.walk", this.customerName());
			case TRADE -> WorkerStatus.task(villager, "shop.trade", this.customerName());
			case DELIVER -> WorkerStatus.task(villager, "deliver");
			case RETURN -> WorkerStatus.task(villager, "shop.return");
			case WAIT -> WorkerStatus.task(villager, this.waitReason, this.waitArgs);
		}
	}

	private Object customerName() {
		return this.sale == null ? "?" : this.sale.customer().getName();
	}

	// --- Planning ---

	private void plan(ServerLevel level, Villager villager, Workstations.Station station) {
		SimpleContainer inventory = villager.getInventory();
		if (this.earned >= DELIVER_AT) {
			this.setState(State.DELIVER);
			return;
		}
		boolean carriesGoods = inventory.hasAnyMatching(stack -> !stack.isEmpty() && isGoods(stack));
		List<ItemStack> stock = this.stock(level, villager, station);
		Optional<Sale> next = this.findSale(level, villager, station, stock);
		if (next.isEmpty()) {
			if (this.earned > 0) {
				this.setState(State.DELIVER);
			} else if (carriesGoods) {
				this.setState(State.RETURN);
			} else {
				this.idle(villager, station, stock.isEmpty() ? "shop.no_goods" : this.waitReason);
			}
			return;
		}
		this.sale = next.get();
		// Fetch what the trades take that it does not carry yet
		if (this.missing(inventory, this.sale).isEmpty()) {
			this.setState(State.WALK);
			return;
		}
		Optional<BlockPos> chest = station.validInputs(level).stream()
			.filter(pos -> Chests.contains(level, pos, stack -> this.wanted(inventory, this.sale, stack) > 0))
			.min(Comparator.comparingDouble(pos -> pos.distToCenterSqr(villager.position())));
		if (chest.isEmpty()) {
			this.setState(State.WALK); // sells what it carries
			return;
		}
		this.trip = ChestTrip.to(level, villager, chest.get());
		this.setState(State.FETCH);
	}

	/** Goods are everything it hands in or sells: not its wage, not food. */
	private static boolean isGoods(ItemStack stack) {
		return Chests.isHandedIn(stack);
	}

	/** The goods in the input chests and in its pockets, one stack per item with the total count. */
	private List<ItemStack> stock(ServerLevel level, Villager villager, Workstations.Station station) {
		List<ItemStack> stock = new ArrayList<>();
		List<Container> containers = new ArrayList<>();
		station.validInputs(level).forEach(pos -> {
			Container container = Workstations.getContainer(level, pos);
			if (container != null) {
				containers.add(container);
			}
		});
		containers.add(villager.getInventory());
		for (Container container : containers) {
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);
				if (stack.isEmpty() || !isGoods(stack)) {
					continue;
				}
				Optional<ItemStack> same = stock.stream().filter(other -> ItemStack.isSameItemSameComponents(other, stack)).findFirst();
				if (same.isPresent()) {
					same.get().grow(stack.getCount());
				} else {
					stock.add(stack.copy());
				}
			}
		}
		return stock;
	}

	/**
	 * The nearest customer that buys something of the stock for emeralds, with its trades
	 * that are not sold out and of which there is enough for at least one trade.
	 */
	private Optional<Sale> findSale(ServerLevel level, Villager villager, Workstations.Station station, List<ItemStack> stock) {
		boolean soldOut = false;
		boolean buyer = false;
		AABB area = new AABB(station.pos()).inflate(Config.TRADE_RADIUS.get());
		List<Villager> customers = level.getEntitiesOfClass(Villager.class, area, SellGoods::isCustomer);
		customers.sort(Comparator.comparingDouble(customer -> customer.distanceToSqr(villager)));
		for (Villager customer : customers) {
			List<MerchantOffer> offers = new ArrayList<>();
			for (MerchantOffer offer : customer.getOffers()) {
				if (!offer.getResult().is(WorkerSupplies.WAGE) || !offer.getCostB().isEmpty()) {
					continue;
				}
				ItemStack cost = offer.getCostA();
				boolean have = stock.stream().anyMatch(stack -> offer.getItemCostA().test(stack) && stack.getCount() >= cost.getCount());
				if (!have) {
					continue;
				}
				buyer = true;
				if (offer.isOutOfStock()) {
					soldOut = true;
				} else {
					offers.add(offer);
				}
			}
			if (!offers.isEmpty()) {
				return Optional.of(new Sale(customer, offers));
			}
		}
		this.waitReason = buyer && soldOut ? "shop.sold_out" : "shop.no_buyer";
		return Optional.empty();
	}

	/** An ordinary adult villager with a job, not busy with a player. */
	private static boolean isCustomer(Villager villager) {
		return villager.isAlive() && !villager.isBaby() && !villager.isSleeping() && !villager.isTrading()
			&& !((HiredVillager) villager).workingvillagers$isHired()
			&& !villager.getVillagerData().profession().is(VillagerProfession.NONE)
			&& !villager.getVillagerData().profession().is(VillagerProfession.NITWIT);
	}

	/** How many more of the item it wants for the trades of the sale, beyond what it carries. */
	private int wanted(SimpleContainer inventory, Sale sale, ItemStack stack) {
		int wanted = 0;
		for (MerchantOffer offer : sale.offers()) {
			if (offer.getItemCostA().test(stack)) {
				int left = offer.getMaxUses() - offer.getUses();
				wanted += left * offer.getCostA().getCount();
			}
		}
		wanted = Math.min(wanted, MAX_GOODS);
		return Math.max(0, wanted - inventory.countItem(stack.getItem()));
	}

	/** The trades of the sale it does not carry enough goods for yet. */
	private List<MerchantOffer> missing(SimpleContainer inventory, Sale sale) {
		return sale.offers().stream().filter(offer -> !carriesFor(inventory, offer)).toList();
	}

	private static boolean carriesFor(SimpleContainer inventory, MerchantOffer offer) {
		int count = 0;
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && offer.getItemCostA().test(stack)) {
				count += stack.getCount();
			}
		}
		return count >= offer.getCostA().getCount();
	}

	private void fetch(ServerLevel level, Villager villager) {
		if (this.trip == null || this.sale == null) {
			this.setState(State.PLAN);
			return;
		}
		Sale sale = this.sale;
		ChestTrip.Result result = this.trip.tick(level, villager, container -> {
			SimpleContainer inventory = villager.getInventory();
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);
				if (stack.isEmpty() || !isGoods(stack)) {
					continue;
				}
				int wanted = this.wanted(inventory, sale, stack);
				if (wanted > 0) {
					Chests.take(container, inventory, other -> ItemStack.isSameItemSameComponents(other, stack), wanted);
				}
			}
		});
		if (result != ChestTrip.Result.RUNNING) {
			this.trip = null;
			if (result == ChestTrip.Result.DONE) {
				this.setState(State.WALK);
			} else {
				this.waitFor("chest_unreachable");
			}
		}
	}

	// --- Trading ---

	private void walk(Villager villager) {
		if (this.sale == null || !isCustomer(this.sale.customer())) {
			this.setState(State.PLAN);
			return;
		}
		Villager customer = this.sale.customer();
		if (customer.closerThan(villager, TRADE_REACH)) {
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			this.setState(State.TRADE);
		} else if (this.stateTicks > WALK_TIMEOUT) {
			this.sale = null;
			this.setState(State.PLAN);
		} else if (this.stateTicks % 20 == 1) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(customer, false), SPEED, 1));
			villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(customer, true));
		}
	}

	/**
	 * Trades with the customer, one trade a while: the goods go to the customer, the
	 * emeralds into its pockets, the customer's trade is used once and earns experience.
	 */
	private void trade(ServerLevel level, Villager villager) {
		Sale sale = this.sale;
		if (sale == null || !isCustomer(sale.customer())) {
			this.setState(State.PLAN);
			return;
		}
		Villager customer = sale.customer();
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(customer, true));
		// The customer stops for the trade
		customer.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		customer.getNavigation().stop();
		customer.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(villager, true));
		if (!customer.closerThan(villager, TRADE_REACH + 1.0)) {
			this.setState(State.WALK);
			return;
		}
		int interval = Math.max(1, (int) Math.round(Config.SECONDS_PER_TRADE.get() * 20.0));
		if (this.stateTicks % interval != 0) {
			return;
		}
		SimpleContainer inventory = villager.getInventory();
		for (MerchantOffer offer : sale.offers()) {
			if (offer.isOutOfStock() || !carriesFor(inventory, offer)) {
				continue;
			}
			removeCost(inventory, offer);
			ItemStack emeralds = offer.assemble();
			customer.notifyTrade(offer);
			ItemStack rest = inventory.addItem(emeralds);
			this.earned += emeralds.getCount() - rest.getCount();
			if (!rest.isEmpty()) {
				villager.spawnAtLocation(level, rest);
			}
			((HiredVillager) villager).workingvillagers$addWorkDone(ChopTrees.today(level), emeralds.getCount());
			villager.swingForAttack(InteractionHand.MAIN_HAND);
			level.playSound(null, customer.blockPosition(), customer.getNotifyTradeSound(), SoundSource.NEUTRAL, 1.0F, 1.0F);
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, customer.getX(), customer.getY() + 1.8, customer.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
			return;
		}
		// Nothing more to sell to this customer
		this.sale = null;
		this.setState(State.PLAN);
	}

	private static void removeCost(SimpleContainer inventory, MerchantOffer offer) {
		int left = offer.getCostA().getCount();
		for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty() && offer.getItemCostA().test(stack)) {
				int taken = Math.min(left, stack.getCount());
				stack.shrink(taken);
				left -= taken;
				inventory.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
			}
		}
	}

	// --- Delivering ---

	private void deliver(ServerLevel level, Villager villager, Workstations.Station station) {
		if (this.trip == null) {
			if (this.earned <= 0) {
				this.setState(State.PLAN);
				return;
			}
			Optional<BlockPos> chest = Chests.closest(station.validOutputs(level).stream().filter(pos -> Chests.hasRoom(level, pos)).toList(), villager);
			if (chest.isEmpty()) {
				this.idle(villager, station, "chests_full");
				return;
			}
			this.trip = ChestTrip.to(level, villager, chest.get());
		}
		ChestTrip.Result result = this.trip.tick(level, villager, container -> {
			ItemStack rest = Chests.store(container, new ItemStack(WorkerSupplies.WAGE, this.earned));
			int stored = this.earned - rest.getCount();
			villager.getInventory().removeItemType(WorkerSupplies.WAGE, stored);
			this.earned -= stored;
		});
		if (result != ChestTrip.Result.RUNNING) {
			this.trip = null;
			if (result == ChestTrip.Result.DONE) {
				this.setState(State.PLAN);
			} else {
				this.waitFor("chest_unreachable");
			}
		}
	}

	/** Brings the goods it could not sell back into an input chest. */
	private void returnGoods(ServerLevel level, Villager villager, Workstations.Station station) {
		if (this.trip == null) {
			if (!villager.getInventory().hasAnyMatching(stack -> !stack.isEmpty() && isGoods(stack))) {
				this.setState(State.PLAN);
				return;
			}
			Optional<BlockPos> chest = Chests.closest(station.validInputs(level).stream().filter(pos -> Chests.hasRoom(level, pos)).toList(), villager);
			if (chest.isEmpty()) {
				this.idle(villager, station, "chests_full");
				return;
			}
			this.trip = ChestTrip.to(level, villager, chest.get());
		}
		ChestTrip.Result result = this.trip.tick(level, villager, container -> Chests.deposit(villager.getInventory(), container, SellGoods::isGoods));
		if (result != ChestTrip.Result.RUNNING) {
			this.trip = null;
			if (result == ChestTrip.Result.DONE) {
				this.idle(villager, station, this.waitReason);
			} else {
				this.waitFor("chest_unreachable");
			}
		}
	}

	private void waitFor(String reason) {
		this.waitReason = reason;
		this.waitArgs = new Object[0];
		this.setState(State.WAIT);
	}

	/** Nothing to do right now: wait at the counter. */
	private void idle(Villager villager, Workstations.Station station, String reason) {
		villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(station.pos(), SPEED, 2));
		this.waitFor(reason);
	}

	private void setState(State state) {
		this.state = state;
		this.stateTicks = 0;
	}
}
