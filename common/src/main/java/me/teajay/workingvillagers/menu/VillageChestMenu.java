package me.teajay.workingvillagers.menu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.job.Meals;
import me.teajay.workingvillagers.job.VillageChests;
import me.teajay.workingvillagers.job.WorkerLevels;
import me.teajay.workingvillagers.job.WorkerSupplies;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The village chest's menu: its slots like a chest's (three rows, six for a double chest),
 * and the workers it serves and whether each finds what it needs in the village chests
 * (sent along in an {@link InfoSlot}). The screen shows the slots and the workers on two
 * tabs; on the client the slots are only there on the chest tab.
 */
public class VillageChestMenu extends ChestMenu {
	public static final int SINGLE_ROWS = 3;
	public static final int DOUBLE_ROWS = 6;
	/** The list of workers is updated once a second. */
	private static final int UPDATE_TICKS = 20;
	private static final String TAG_WORKERS = "Workers";
	private static final String TAG_NAME = "Name";
	private static final String TAG_JOB = "Job";
	private static final String TAG_PROFESSION = "Profession";
	private static final String TAG_MISSING = "Missing";
	private static final String TAG_WAGE_DAYS = "WageDays";
	private static final String TAG_FED_WAGE_DAYS = "FedWageDays";
	/** What the wage days show at most: the workers cost (next to) nothing. */
	private static final int MAX_WAGE_DAYS = 999;
	private static final String TAG_WORN_TOOLS = "WornTools";
	private static final String TAG_RADIUS = "Radius";
	private static final String TAG_ENABLED = "Enabled";

	private final InfoSlot info = new InfoSlot();
	/** Server: the (clicked half of the) village chest; null on the client. */
	private final @Nullable BlockPos pos;
	private final Inventory inventory;
	private int ticks;
	/** Client: whether the chest tab is open. */
	private boolean slotsShown = true;

	/**
	 * A worker the village chest serves; {@code name}: its own name, empty if it has none;
	 * {@code missing}: the translation key of what it lacks and finds in no village chest.
	 */
	public record Worker(Optional<Component> name, Component job, ResourceKey<VillagerProfession> profession, Optional<String> missing) {
	}

	/** Client side, single chest. */
	public static VillageChestMenu single(int containerId, Inventory inventory) {
		return new VillageChestMenu(SINGLE_ROWS, containerId, inventory, new SimpleContainer(SINGLE_ROWS * 9), null);
	}

	/** Client side, double chest. */
	public static VillageChestMenu doubleChest(int containerId, Inventory inventory) {
		return new VillageChestMenu(DOUBLE_ROWS, containerId, inventory, new SimpleContainer(DOUBLE_ROWS * 9), null);
	}

	public VillageChestMenu(int rows, int containerId, Inventory inventory, Container chest, @Nullable BlockPos pos) {
		super(rows == DOUBLE_ROWS ? WorkingVillagers.VILLAGE_CHEST_DOUBLE_MENU.get() : WorkingVillagers.VILLAGE_CHEST_MENU.get(),
			containerId, inventory, chest, rows);
		this.pos = pos;
		this.inventory = inventory;
		if (pos == null) {
			// The chest's and the inventory's slots, but hidden on the workers tab
			for (int i = 0; i < this.slots.size(); i++) {
				Slot slot = this.slots.get(i);
				Slot hideable = new Slot(slot.container, slot.getContainerSlot(), slot.x, slot.y) {
					@Override
					public boolean isActive() {
						return VillageChestMenu.this.slotsShown;
					}
				};
				hideable.index = i;
				this.slots.set(i, hideable);
			}
		}
		this.addSlot(this.info);
		this.updateInfo();
	}

	/** Client: shows or hides the slots (the chest tab is open or not). */
	public void showSlots(boolean shown) {
		this.slotsShown = shown;
	}

	@Override
	public void broadcastChanges() {
		if (++this.ticks % UPDATE_TICKS == 0) {
			this.updateInfo();
		}
		super.broadcastChanges();
	}

	/** Like a chest, but never into the hidden info slot. */
	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		int chestSlots = this.getRowCount() * 9;
		int end = this.slots.size() - 1;
		Slot slot = this.slots.get(slotIndex);
		if (slot == this.info || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack clicked = stack.copy();
		if (slotIndex < chestSlots) {
			if (!this.moveItemStackTo(stack, chestSlots, end, true)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.moveItemStackTo(stack, 0, chestSlots, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return clicked;
	}

	private void updateInfo() {
		BlockPos pos = this.pos;
		if (pos == null || !(this.inventory.player.level() instanceof ServerLevel level)) {
			return;
		}
		CompoundTag tag = new CompoundTag();
		tag.putBoolean(TAG_ENABLED, Config.VILLAGE_CHESTS.get());
		tag.putInt(TAG_RADIUS, Config.VILLAGE_CHEST_RADIUS.get());
		List<Villager> workers = VillageChests.served(level, pos);
		ListTag list = new ListTag();
		Set<BlockPos> chests = new HashSet<>();
		int carried = 0;
		int wage = 0;
		double cost = 0.0;
		double fedCost = 0.0;
		int eatingOut = 0;
		for (Villager worker : workers) {
			wage += WorkerLevels.dailyWage(worker);
			CompoundTag entry = new CompoundTag();
			if (worker.hasCustomName()) {
				entry.store(TAG_NAME, ComponentSerialization.CODEC, worker.getName());
			}
			entry.store(TAG_JOB, ComponentSerialization.CODEC, worker.getVillagerData().profession().value().name());
			worker.getVillagerData().profession().unwrapKey().ifPresent(key -> entry.putString(TAG_PROFESSION, key.identifier().toString()));
			VillageChests.missing(level, worker).ifPresent(key -> entry.putString(TAG_MISSING, key));
			list.add(entry);
			chests.addAll(VillageChests.inReach(level, VillageChests.workplace(level, worker)));
			carried += WorkerSupplies.emeralds(level, worker);
			boolean eatsOut = Meals.eatsOut(worker);
			eatingOut += eatsOut ? 1 : 0;
			cost += Meals.dailyCost(worker, !eatsOut);
			fedCost += Meals.dailyCost(worker, true);
		}
		tag.put(TAG_WORKERS, list);
		tag.putInt(TAG_WORN_TOOLS, WorkerSupplies.count(this.getContainer(), WorkerSupplies::isWorn));
		if (wage > 0) {
			int emeralds = carried;
			for (BlockPos chest : chests) {
				Container container = level.isLoaded(chest) && level.getBlockEntity(chest) instanceof Container found ? found : null;
				if (container != null) {
					emeralds += container.countItem(WorkerSupplies.WAGE);
				}
			}
			// The food money of a worker that buys its food here comes back; of one that eats out it is gone
			tag.putInt(TAG_WAGE_DAYS, wageDays(emeralds, cost));
			if (eatingOut > 0) {
				tag.putInt(TAG_FED_WAGE_DAYS, wageDays(emeralds, fedCost));
			}
		}
		this.info.setInfo(tag);
	}

	private static int wageDays(int emeralds, double daily) {
		return daily <= 0.0 ? MAX_WAGE_DAYS : (int) Math.min(MAX_WAGE_DAYS, emeralds / daily);
	}

	/** Client: the workers the village chest serves. */
	public List<Worker> getWorkers() {
		List<Worker> workers = new ArrayList<>();
		for (CompoundTag entry : this.info.info().getListOrEmpty(TAG_WORKERS).compoundStream().toList()) {
			Identifier profession = entry.getString(TAG_PROFESSION).map(Identifier::tryParse).orElse(null);
			workers.add(new Worker(
				entry.read(TAG_NAME, ComponentSerialization.CODEC),
				entry.read(TAG_JOB, ComponentSerialization.CODEC).orElse(Component.empty()),
				ResourceKey.create(Registries.VILLAGER_PROFESSION, profession != null ? profession : WorkingVillagers.LUMBERJACK_PROFESSION.identifier()),
				entry.getString(TAG_MISSING)
			));
		}
		return workers;
	}

	/**
	 * Client: for how many days the emeralds of the workers and their village chests pay them
	 * all, counting the food money of those that buy their food at village chests as coming
	 * back (see {@link Meals}); empty without wage.
	 */
	public Optional<Integer> getWageDays() {
		return this.info.info().getInt(TAG_WAGE_DAYS);
	}

	/** Client: for how many days they would if those that eat out bought their food at village chests too; empty if none eats out. */
	public Optional<Integer> getFedWageDays() {
		return this.info.info().getInt(TAG_FED_WAGE_DAYS);
	}

	/** Client: how many worn tools lie in the chest, given back by workers (see {@link WorkerSupplies#isWorn}). */
	public int getWornTools() {
		return this.info.info().getIntOr(TAG_WORN_TOOLS, 0);
	}

	public int getRadius() {
		return this.info.info().getIntOr(TAG_RADIUS, 48);
	}

	/** Client: false if the server switched village chests off. */
	public boolean isEnabled() {
		return this.info.info().getBooleanOr(TAG_ENABLED, true);
	}
}
