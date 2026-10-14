package me.teajay.workingvillagers.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.crafting.CarpenterRecipes;
import me.teajay.workingvillagers.station.StationType;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;

/**
 * The menu of the carpenter's bench: a list of every recipe of the bench. The player picks
 * one and takes the result, made of the ingredients in their inventory. A box below the
 * result shows the order of the workstation, which is set in the overview.
 *
 * <p>Crafting recipes are not sent to the client, so the server puts the results into
 * hidden slots (with the ingredients as lore, and a mark if the player has them) and the
 * screen draws the recipe buttons from them.
 */
public class CarpenterMenu extends AbstractContainerMenu {
	public static final int MAX_OPTIONS = 128;
	/** Button that opens the overview of the workstation (worker, chests, order). */
	public static final int STATION_BUTTON = 202;
	public static final int RESULT_X = 154;
	public static final int RESULT_Y = 23;
	public static final int INVENTORY_Y = 102;
	private static final String TAG_CRAFTABLE = "WorkingVillagersCraftable";
	private static final int RESULT_SLOT = 0;
	private static final int INV_SLOT_START = 1;
	private static final int INV_SLOT_END = 28;
	private static final int USE_ROW_SLOT_END = 37;

	private final ContainerLevelAccess access;
	private final Player player;
	private final Level level;
	private final DataSlot selectedRecipeIndex = DataSlot.standalone();
	/** The item id of the order, 0 (air) for none. */
	private final DataSlot order = DataSlot.standalone();
	/** Server only: the recipes, in the order of the option slots. */
	private List<CraftingRecipe> recipes = List.of();
	/** Server only: the player's inventory as the options were last marked, to notice changes. */
	private List<ItemStack> lastInventory = List.of();
	private long lastSoundTime;
	private final ResultContainer resultContainer = new ResultContainer();
	private final Slot resultSlot;
	private final SimpleContainer options = new SimpleContainer(MAX_OPTIONS);
	/** Holds a paper named after the worker of the station, empty if the station is free. */
	private final SimpleContainer worker = new SimpleContainer(1);

	/** Client side. */
	public CarpenterMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, ContainerLevelAccess.NULL);
	}

	public CarpenterMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(WorkingVillagers.CARPENTER_BENCH_MENU.get(), containerId);
		this.access = access;
		this.player = inventory.player;
		this.level = inventory.player.level();
		this.resultSlot = this.addSlot(new Slot(this.resultContainer, 0, RESULT_X, RESULT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}

			@Override
			public boolean mayPickup(Player player) {
				return CarpenterMenu.this.level.isClientSide() || CarpenterMenu.this.canCraft();
			}

			@Override
			public void onTake(Player player, ItemStack carried) {
				CarpenterMenu.this.craft(player, carried);
				super.onTake(player, carried);
			}
		});
		this.addStandardInventorySlots(inventory, 8, INVENTORY_Y);
		for (int i = 0; i < MAX_OPTIONS; i++) {
			this.addSlot(new HiddenSlot(this.options, i));
		}
		this.addSlot(new HiddenSlot(this.worker, 0));
		this.addDataSlot(this.selectedRecipeIndex);
		this.addDataSlot(this.order);
		this.selectedRecipeIndex.set(-1);

		if (this.level instanceof ServerLevel serverLevel) {
			this.recipes = CarpenterRecipes.all(serverLevel).stream().limit(MAX_OPTIONS).toList();
			this.updateOptions();
		}
		this.station().ifPresent(station -> {
			if (station.order() != null) {
				this.order.set(BuiltInRegistries.ITEM.getId(station.order()));
			}
			if (this.level instanceof ServerLevel serverLevel) {
				station.occupant(serverLevel).ifPresent(name -> {
					ItemStack paper = new ItemStack(Items.PAPER);
					paper.set(DataComponents.CUSTOM_NAME, name);
					this.worker.setItem(0, paper);
				});
			}
		});
	}

	/** Server only: the settings of the workstation this menu belongs to. */
	private Optional<Workstations.Station> station() {
		return this.access.evaluate((level, pos) -> level instanceof ServerLevel serverLevel ? Workstations.find(serverLevel, pos) : Optional.<Workstations.Station>empty())
			.flatMap(station -> station);
	}

	/** Who works at this station, empty if it is free. */
	public Optional<Component> getWorker() {
		ItemStack paper = this.worker.getItem(0);
		return paper.isEmpty() ? Optional.empty() : Optional.of(paper.getHoverName());
	}

	/** What the worker of this station makes, empty if it has no order. */
	public ItemStack getOrder() {
		Item item = BuiltInRegistries.ITEM.byId(this.order.get());
		return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
	}

	public int getSelectedRecipeIndex() {
		return this.selectedRecipeIndex.get();
	}

	/** The results of all recipes, as shown on the recipe buttons (with the ingredients as lore). */
	public List<ItemStack> getOptions() {
		List<ItemStack> visible = new ArrayList<>();
		for (int i = 0; i < MAX_OPTIONS && !this.options.getItem(i).isEmpty(); i++) {
			visible.add(this.options.getItem(i));
		}
		return visible;
	}

	/** Whether the player has the ingredients for the recipe of the option. */
	public static boolean isCraftable(ItemStack option) {
		CustomData data = option.get(DataComponents.CUSTOM_DATA);
		return data != null && data.copyTag().getBooleanOr(TAG_CRAFTABLE, false);
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(this.access, player, WorkingVillagers.CARPENTER_BENCH.get());
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		if (buttonId == STATION_BUTTON) {
			this.access.execute((level, pos) -> {
				StationType type = StationType.of(level.getBlockState(pos));
				if (type != null) {
					player.openMenu(StationMenu.provider(type, level, pos));
				}
			});
			return true;
		}
		if (buttonId < 0 || buttonId >= MAX_OPTIONS || this.options.getItem(buttonId).isEmpty() || this.selectedRecipeIndex.get() == buttonId) {
			return false;
		}
		this.selectedRecipeIndex.set(buttonId);
		this.setupResultSlot();
		return true;
	}

	private CraftingRecipe selectedRecipe() {
		int index = this.selectedRecipeIndex.get();
		return index >= 0 && index < this.recipes.size() ? this.recipes.get(index) : null;
	}

	/** Server only: whether the player has the ingredients of the selected recipe. */
	private boolean canCraft() {
		CraftingRecipe recipe = this.selectedRecipe();
		if (recipe == null) {
			return false;
		}
		StackedItemContents contents = new StackedItemContents();
		this.player.getInventory().fillStackedContents(contents);
		return contents.canCraft(recipe, null);
	}

	/** Server only: takes the ingredients for the result the player took out of the inventory. */
	private void craft(Player player, ItemStack carried) {
		CraftingRecipe recipe = this.selectedRecipe();
		if (recipe == null || this.level.isClientSide()) {
			return;
		}
		List<ItemStack> made = CarpenterRecipes.craft(player.getInventory(), Inventory.INVENTORY_SIZE, Inventory::isUsableForCrafting, recipe);
		carried.onCraftedBy(player, carried.getCount());
		// The first one is the result the player took, the rest is what the ingredients leave behind
		for (ItemStack remainder : made.subList(Math.min(1, made.size()), made.size())) {
			player.getInventory().placeItemBackInInventory(remainder, Prediction.PREDICTED);
		}
		this.access.execute((level, pos) -> {
			long gameTime = level.getGameTime();
			if (this.lastSoundTime != gameTime) {
				level.playSound(null, pos, SoundEvents.AXE_STRIP.value(), SoundSource.BLOCKS, 0.6F, 1.2F);
				this.lastSoundTime = gameTime;
			}
		});
		this.updateOptions();
		this.setupResultSlot();
	}

	private void setupResultSlot() {
		if (this.level.isClientSide()) {
			return;
		}
		CraftingRecipe recipe = this.selectedRecipe();
		this.resultSlot.set(recipe != null && this.canCraft() ? CarpenterRecipes.preview(recipe) : ItemStack.EMPTY);
	}

	/** Server only: puts the recipes into the option slots and marks the ones the player has the ingredients for. */
	private void updateOptions() {
		StackedItemContents contents = new StackedItemContents();
		this.player.getInventory().fillStackedContents(contents);
		Style loreStyle = Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY);
		for (int i = 0; i < MAX_OPTIONS; i++) {
			if (i >= this.recipes.size()) {
				this.options.setItem(i, ItemStack.EMPTY);
				continue;
			}
			CraftingRecipe recipe = this.recipes.get(i);
			ItemStack option = CarpenterRecipes.preview(recipe);
			List<Component> lore = new ArrayList<>();
			lore.add(Component.translatable("gui.workingvillagers.carpenter.needs").setStyle(loreStyle));
			CarpenterRecipes.describeIngredients(recipe).forEach(line -> lore.add(line.copy().setStyle(loreStyle)));
			option.set(DataComponents.LORE, new ItemLore(lore));
			if (contents.canCraft(recipe, null)) {
				CompoundTag tag = new CompoundTag();
				tag.putBoolean(TAG_CRAFTABLE, true);
				option.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
			}
			if (!ItemStack.matches(option, this.options.getItem(i))) {
				this.options.setItem(i, option);
			}
		}
		this.lastInventory = this.inventorySnapshot();
	}

	private List<ItemStack> inventorySnapshot() {
		List<ItemStack> snapshot = new ArrayList<>(Inventory.INVENTORY_SIZE);
		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			snapshot.add(this.player.getInventory().getItem(slot).copy());
		}
		return snapshot;
	}

	@Override
	public void broadcastChanges() {
		if (!this.level.isClientSide() && !this.recipes.isEmpty()) {
			List<ItemStack> now = this.inventorySnapshot();
			boolean changed = now.size() != this.lastInventory.size();
			for (int slot = 0; !changed && slot < now.size(); slot++) {
				changed = !ItemStack.matches(now.get(slot), this.lastInventory.get(slot));
			}
			if (changed) {
				this.updateOptions();
				this.setupResultSlot();
			}
		}
		super.broadcastChanges();
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
		return target.container != this.resultContainer && target.container != this.options && target.container != this.worker
			&& super.canTakeItemForPickAll(carried, target);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		ItemStack clicked = ItemStack.EMPTY;
		Slot slot = this.slots.get(slotIndex);
		if (slot != null && slot.hasItem() && slotIndex < USE_ROW_SLOT_END) {
			ItemStack stack = slot.getItem();
			clicked = stack.copy();
			if (slotIndex == RESULT_SLOT) {
				if (!this.moveItemStackTo(stack, INV_SLOT_START, USE_ROW_SLOT_END, true)) {
					return ItemStack.EMPTY;
				}
				slot.onQuickCraft(stack, clicked);
			} else if (slotIndex < INV_SLOT_END) {
				if (!this.moveItemStackTo(stack, INV_SLOT_END, USE_ROW_SLOT_END, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(stack, INV_SLOT_START, INV_SLOT_END, false)) {
				return ItemStack.EMPTY;
			}

			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			}
			slot.setChanged();
			if (stack.getCount() == clicked.getCount()) {
				return ItemStack.EMPTY;
			}
			slot.onTake(player, stack);
			if (slotIndex == RESULT_SLOT) {
				player.drop(stack, false, Prediction.PREDICTED);
			}
			this.broadcastChanges();
		}
		return clicked;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.resultContainer.removeItemNoUpdate(0);
	}

	/** A hidden slot that only carries something to the client. */
	private static final class HiddenSlot extends Slot {
		HiddenSlot(Container container, int index) {
			super(container, index, -10000, -10000);
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
}
