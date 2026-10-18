package me.teajay.workingvillagers.menu;

import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Server: the wage box of a workstation as a container of one slot, for the wage tab of the
 * overview. It reads and writes the station's emeralds, so the worker taking its wage
 * meanwhile shows up in the slot.
 */
final class WageBox implements Container {
	private final Workstations.Station station;
	private ItemStack stack = ItemStack.EMPTY;

	WageBox(Workstations.Station station) {
		this.station = station;
	}

	/** The stack as the station has it; a stack the slot is working on stays as long as it matches. */
	private ItemStack current() {
		int wage = this.station.wage();
		if (this.stack.isEmpty() ? wage != 0 : !this.stack.is(Items.EMERALD) || this.stack.getCount() != wage) {
			this.stack = wage == 0 ? ItemStack.EMPTY : new ItemStack(Items.EMERALD, wage);
		}
		return this.stack;
	}

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return this.station.wage() == 0;
	}

	@Override
	public ItemStack getItem(int slot) {
		return this.current();
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		ItemStack taken = this.current().split(count);
		this.setChanged();
		return taken;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack taken = this.current();
		this.stack = ItemStack.EMPTY;
		this.setChanged();
		return taken;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		this.stack = stack;
		this.setChanged();
	}

	@Override
	public int getMaxStackSize() {
		return Workstations.MAX_WAGE;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return stack.is(Items.EMERALD);
	}

	@Override
	public void setChanged() {
		this.station.setWage(this.stack.is(Items.EMERALD) ? this.stack.getCount() : 0);
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
		this.stack = ItemStack.EMPTY;
		this.setChanged();
	}
}
