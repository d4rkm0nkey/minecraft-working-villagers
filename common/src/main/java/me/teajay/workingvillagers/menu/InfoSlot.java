package me.teajay.workingvillagers.menu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/**
 * A hidden slot that carries what a screen shows from the server to the client: a paper
 * with the information as custom data. The menu synchronizes it like any other slot, so
 * no network code is needed on either loader.
 */
public class InfoSlot extends Slot {
	public InfoSlot() {
		super(new SimpleContainer(1), 0, -10000, -10000);
	}

	/** Server: sets the information, the menu sends it if it changed. */
	public void setInfo(CompoundTag info) {
		ItemStack paper = new ItemStack(Items.PAPER);
		paper.set(DataComponents.CUSTOM_DATA, CustomData.of(info));
		if (!ItemStack.matches(paper, this.getItem())) {
			this.container.setItem(0, paper);
		}
	}

	/** Client: the information last sent by the server, empty before that. */
	public CompoundTag info() {
		CustomData data = this.getItem().get(DataComponents.CUSTOM_DATA);
		return data == null ? new CompoundTag() : data.copyTag();
	}

	@Override
	public boolean isActive() {
		return false;
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return false;
	}

	@Override
	public boolean mayPickup(Player player) {
		return false;
	}
}
