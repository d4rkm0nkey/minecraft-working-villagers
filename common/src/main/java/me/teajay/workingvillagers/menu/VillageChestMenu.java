package me.teajay.workingvillagers.menu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.job.VillageChests;
import me.teajay.workingvillagers.job.WorkerSupplies;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The village chest's screen: its slots like a chest's (three rows, six for a double
 * chest), and next to them the workers it serves and whether each finds what it needs in
 * the village chests (sent along in an {@link InfoSlot}).
 */
public class VillageChestMenu extends ChestMenu {
	public static final int SINGLE_ROWS = 3;
	public static final int DOUBLE_ROWS = 6;
	/** The list of workers is updated once a second. */
	private static final int UPDATE_TICKS = 20;
	private static final String TAG_WORKERS = "Workers";
	private static final String TAG_NAME = "Name";
	private static final String TAG_JOB = "Job";
	private static final String TAG_MISSING = "Missing";
	private static final String TAG_WAGE_DAYS = "WageDays";
	private static final String TAG_WORN_TOOLS = "WornTools";
	private static final String TAG_RADIUS = "Radius";
	private static final String TAG_ENABLED = "Enabled";

	private final InfoSlot info = new InfoSlot();
	/** Server: the (clicked half of the) village chest; null on the client. */
	private final @Nullable BlockPos pos;
	private final Inventory inventory;
	private int ticks;

	/** A worker the village chest serves; {@code missing}: the translation key of what it lacks and finds in no village chest. */
	public record Worker(Component name, Component job, Optional<String> missing) {
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
		this.addSlot(this.info);
		this.updateInfo();
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
		for (Villager worker : workers) {
			CompoundTag entry = new CompoundTag();
			entry.store(TAG_NAME, ComponentSerialization.CODEC, worker.getName());
			entry.store(TAG_JOB, ComponentSerialization.CODEC, worker.getVillagerData().profession().value().name());
			VillageChests.missing(level, worker).ifPresent(key -> entry.putString(TAG_MISSING, key));
			list.add(entry);
			chests.addAll(VillageChests.inReach(level, VillageChests.workplace(level, worker)));
			carried += WorkerSupplies.emeralds(level, worker);
		}
		tag.put(TAG_WORKERS, list);
		tag.putInt(TAG_WORN_TOOLS, WorkerSupplies.count(this.getContainer(), WorkerSupplies::isWorn));
		int wage = Config.DAILY_WAGE.get();
		if (wage > 0 && !workers.isEmpty()) {
			int emeralds = carried;
			for (BlockPos chest : chests) {
				Container container = level.isLoaded(chest) && level.getBlockEntity(chest) instanceof Container found ? found : null;
				if (container != null) {
					emeralds += container.countItem(WorkerSupplies.WAGE);
				}
			}
			tag.putInt(TAG_WAGE_DAYS, emeralds / (wage * workers.size()));
		}
		this.info.setInfo(tag);
	}

	/** Client: the workers the village chest serves. */
	public List<Worker> getWorkers() {
		List<Worker> workers = new ArrayList<>();
		for (CompoundTag entry : this.info.info().getListOrEmpty(TAG_WORKERS).compoundStream().toList()) {
			workers.add(new Worker(
				entry.read(TAG_NAME, ComponentSerialization.CODEC).orElse(Component.empty()),
				entry.read(TAG_JOB, ComponentSerialization.CODEC).orElse(Component.empty()),
				entry.getString(TAG_MISSING)
			));
		}
		return workers;
	}

	/** Client: for how many days the emeralds of the workers and their village chests pay them all; empty without wage. */
	public Optional<Integer> getWageDays() {
		return this.info.info().getInt(TAG_WAGE_DAYS);
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
