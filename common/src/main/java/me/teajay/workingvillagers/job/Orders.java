package me.teajay.workingvillagers.job;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import me.teajay.workingvillagers.station.StationType;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;

/**
 * What the workers that make products to order (carpenter, cutters, kiln, fletcher, tanner,
 * weaponsmith, apothecary) have in common: where they take their material from, and what
 * happens when a product ordered once ({@link Workstations.Station.Batch}) is done.
 */
public final class Orders {
	/** How often {@link #hasSources} looks for village chests again. */
	private static final int RECHECK_TICKS = 40;
	/** Per level and station: when it last looked for village chests, and whether it found one. */
	private static final Map<ServerLevel, Map<BlockPos, Checked>> VILLAGE_CHESTS = new WeakHashMap<>();

	private record Checked(long time, boolean found) {
	}

	private Orders() {
	}

	/**
	 * The chests the worker takes its material from: the input chests of the station, then
	 * the village chests in reach of it (see {@link VillageChests}) that are not linked as
	 * output chests. A double chest counts once.
	 */
	public static List<BlockPos> sources(ServerLevel level, Workstations.Station station) {
		List<BlockPos> sources = new ArrayList<>(station.validInputs(level));
		for (BlockPos chest : VillageChests.inReach(level, station.pos())) {
			BlockPos other = Workstations.otherHalf(level, chest);
			if (station.linkedHalf(level, chest) == null && (other == null || !sources.contains(other))) {
				sources.add(chest);
			}
		}
		return sources;
	}

	/**
	 * Whether the worker has somewhere to take material from: an input chest or a village
	 * chest. Asked every tick while it works, so whether there is a village chest in reach is
	 * looked up only every {@link #RECHECK_TICKS}.
	 */
	public static boolean hasSources(ServerLevel level, Workstations.Station station) {
		if (!station.validInputs(level).isEmpty()) {
			return true;
		}
		Map<BlockPos, Checked> checked = VILLAGE_CHESTS.computeIfAbsent(level, key -> new HashMap<>());
		Checked last = checked.get(station.pos());
		long now = level.getGameTime();
		if (last == null || now - last.time() >= RECHECK_TICKS || now < last.time()) {
			last = new Checked(now, !VillageChests.inReach(level, station.pos()).isEmpty());
			checked.put(station.pos(), last);
		}
		return last.found();
	}

	/** Why a worker waits, as the key of its status (see {@link WorkerStatus}) and its arguments. */
	public record Idle(String key, Object... args) {
	}

	/**
	 * Why there is nothing of the order to make ({@link Workstations.Station#ordersByNeed} is
	 * empty): the stocks are full, then the status {@code <prefix>.stock_full} with how many
	 * the output chests hold of the emptiest one and its stock; or the orders are done.
	 */
	public static Idle nothingToMake(ServerLevel level, Workstations.Station station, String prefix) {
		Item emptiest = null;
		int stocked = Integer.MAX_VALUE;
		for (Item order : station.orders()) {
			if (station.stocked(order)) {
				int count = station.inOutputs(level, order);
				if (count < stocked) {
					stocked = count;
					emptiest = order;
				}
			}
		}
		return emptiest == null ? new Idle("order_done") : new Idle(prefix + ".stock_full", stocked, station.stock(emptiest));
	}

	/** How many more of the product of the order the worker should make, see {@link Workstations.Station#wanted}. */
	public static int wanted(ServerLevel level, Workstations.Station station, Item order) {
		return station.wanted(order, station.stocked(order) ? station.inOutputs(level, order) : 0);
	}

	/**
	 * Called when the worker has handed in what it made: takes the done orders whose products
	 * it carries no more out of the order, and the worker shows that they are done (see
	 * {@link WorkerNotice#done}). Returns the products that were done.
	 */
	public static List<Item> finish(ServerLevel level, Villager villager, Workstations.Station station) {
		SimpleContainer inventory = villager.getInventory();
		return finish(level, villager, station, order -> station.productsOf(order).stream().anyMatch(product -> inventory.countItem(product) > 0));
	}

	/** Like {@link #finish(ServerLevel, Villager, Workstations.Station)}, with {@code carried}: whether the worker still carries some of a product of the order. */
	public static List<Item> finish(ServerLevel level, Villager villager, Workstations.Station station, Predicate<Item> carried) {
		List<Item> finished = station.removeFinished(station.type() == StationType.KILN, carried);
		if (!finished.isEmpty()) {
			WorkerNotice.done(villager);
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getY() + villager.getBbHeight() + 0.3, villager.getZ(),
				8, 0.4, 0.3, 0.4, 0.0);
			level.playSound(null, villager.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
		}
		return finished;
	}
}
