package me.teajay.workingvillagers.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.crafting.CuttingRecipes;
import me.teajay.workingvillagers.station.StationType;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * The menu of the woodcutter, the stonecutter and the mill of the mining bench, a copy of the
 * vanilla stonecutter menu that also shows the order of the workstation, what its worker makes. The order is set
 * in the overview.
 *
 * <p>Unlike stonecutter recipes, custom recipes are not sent to the client. So the server
 * puts the possible results into hidden slots, which the menu synchronizes like any other
 * slot, and the screen draws the recipe buttons from them.
 */
public class CutterMenu extends AbstractContainerMenu {
	public static final int MAX_OPTIONS = 32;
	/** Button that opens the overview of the workstation (worker, chests, order). */
	public static final int STATION_BUTTON = 102;
	private static final int INPUT_SLOT = 0;
	private static final int RESULT_SLOT = 1;
	private static final int INV_SLOT_START = 2;
	private static final int INV_SLOT_END = 29;
	private static final int USE_ROW_SLOT_START = 29;
	private static final int USE_ROW_SLOT_END = 38;

	private final Kind kind;
	private final ContainerLevelAccess access;
	private final DataSlot selectedRecipeIndex = DataSlot.standalone();
	/** The item id of the order, 0 (air) for none. */
	private final DataSlot order = DataSlot.standalone();
	private final Level level;
	/** Server only: the recipes for the current input, in the order of the option slots. */
	private List<RecipeHolder<SingleItemRecipe>> recipesForInput = List.of();
	private ItemStack input = ItemStack.EMPTY;
	private long lastSoundTime;
	private final Slot inputSlot;
	private final Slot resultSlot;
	private Runnable slotUpdateListener = () -> {};
	public final Container container = new SimpleContainer(1) {
		@Override
		public void setChanged() {
			super.setChanged();
			CutterMenu.this.slotsChanged(this);
			CutterMenu.this.slotUpdateListener.run();
		}
	};
	private final ResultContainer resultContainer = new ResultContainer();
	private final SimpleContainer options = new SimpleContainer(MAX_OPTIONS);
	/** Holds a paper named after the worker of the station, empty if the station is free. */
	private final SimpleContainer worker = new SimpleContainer(1);

	public CutterMenu(Kind kind, int containerId, Inventory inventory) {
		this(kind, containerId, inventory, ContainerLevelAccess.NULL);
	}

	public CutterMenu(Kind kind, int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(kind.menuType().get(), containerId);
		this.kind = kind;
		this.access = access;
		this.level = inventory.player.level();
		this.inputSlot = this.addSlot(new Slot(this.container, 0, 20, 33));
		this.resultSlot = this.addSlot(new Slot(this.resultContainer, 1, 143, 33) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}

			@Override
			public void onTake(Player player, ItemStack carried) {
				carried.onCraftedBy(player, carried.getCount());
				CutterMenu.this.resultContainer.awardUsedRecipes(player, List.of(CutterMenu.this.inputSlot.getItem()));
				ItemStack remaining = CutterMenu.this.inputSlot.remove(1);
				if (!remaining.isEmpty()) {
					CutterMenu.this.setupResultSlot(CutterMenu.this.selectedRecipeIndex.get());
				}

				access.execute((level, pos) -> {
					long gameTime = level.getGameTime();
					if (CutterMenu.this.lastSoundTime != gameTime) {
						level.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
						CutterMenu.this.lastSoundTime = gameTime;
					}
				});
				super.onTake(player, carried);
			}
		});
		this.addStandardInventorySlots(inventory, 8, 84);
		for (int i = 0; i < MAX_OPTIONS; i++) {
			this.addSlot(new OptionSlot(this.options, i));
		}
		this.addSlot(new OptionSlot(this.worker, 0));
		this.addDataSlot(this.selectedRecipeIndex);
		this.addDataSlot(this.order);
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

	/**
	 * Who works at this station, empty if it is free.
	 */
	public Optional<Component> getWorker() {
		ItemStack paper = this.worker.getItem(0);
		return paper.isEmpty() ? Optional.empty() : Optional.of(paper.getHoverName());
	}

	/** Server only: the settings of the workstation this menu belongs to. */
	private Optional<Workstations.Station> station() {
		return this.access.evaluate((level, pos) -> level instanceof ServerLevel serverLevel ? Workstations.find(serverLevel, pos) : Optional.<Workstations.Station>empty())
			.flatMap(station -> station);
	}

	/**
	 * What the worker of this station makes, empty if it has no order.
	 */
	public ItemStack getOrder() {
		Item item = BuiltInRegistries.ITEM.byId(this.order.get());
		return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
	}

	/** Whether the station has an order (shown below the result); the mill of the mining bench has none. */
	public boolean hasOrder() {
		return this.kind != WorkingVillagers.MILL_KIND;
	}

	/** The profession that works at the station: sawyer, stone sawyer or (at the mill) miner. */
	public ResourceKey<VillagerProfession> profession() {
		if (this.kind == WorkingVillagers.WOODCUTTER_KIND) {
			return WorkingVillagers.WOODCUTTER_PROFESSION;
		}
		return this.kind == WorkingVillagers.STONECUTTER_KIND ? WorkingVillagers.STONECUTTER_PROFESSION : WorkingVillagers.MINER_PROFESSION;
	}

	/** Whether this is the woodcutter's menu, whose order can use several wood types. */
	public boolean isWoodcutter() {
		return this.kind == WorkingVillagers.WOODCUTTER_KIND;
	}

	public int getSelectedRecipeIndex() {
		return this.selectedRecipeIndex.get();
	}

	/**
	 * The results the current input can be cut into, as shown on the recipe buttons.
	 */
	public List<ItemStack> getVisibleOptions() {
		List<ItemStack> visible = new ArrayList<>();
		for (int i = 0; i < MAX_OPTIONS && !this.options.getItem(i).isEmpty(); i++) {
			visible.add(this.options.getItem(i));
		}
		return visible;
	}

	public int getNumberOfVisibleRecipes() {
		return this.getVisibleOptions().size();
	}

	public boolean hasInputItem() {
		return this.inputSlot.hasItem() && this.getNumberOfVisibleRecipes() > 0;
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(this.access, player, this.kind.block().get());
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
		if (this.selectedRecipeIndex.get() == buttonId) {
			return false;
		}
		if (this.isValidRecipeIndex(buttonId)) {
			this.selectedRecipeIndex.set(buttonId);
			this.setupResultSlot(buttonId);
		}
		return true;
	}

	private boolean isValidRecipeIndex(int buttonId) {
		return buttonId >= 0 && buttonId < this.getNumberOfVisibleRecipes();
	}

	@Override
	public void slotsChanged(Container container) {
		ItemStack input = this.inputSlot.getItem();
		if (!input.is(this.input.getItem())) {
			this.input = input.copy();
			this.setupRecipeList(input);
		}
	}

	private void setupRecipeList(ItemStack item) {
		this.selectedRecipeIndex.set(-1);
		this.resultSlot.set(ItemStack.EMPTY);
		if (!(this.level instanceof ServerLevel serverLevel)) {
			// The server sends the options
			return;
		}
		this.recipesForInput = item.isEmpty() ? List.of() : CuttingRecipes.forInput(serverLevel, this.kind.recipeType().get(), item)
			.stream().limit(MAX_OPTIONS).toList();
		for (int i = 0; i < MAX_OPTIONS; i++) {
			this.options.setItem(i, i < this.recipesForInput.size() ? CuttingRecipes.preview(this.recipesForInput.get(i).value()) : ItemStack.EMPTY);
		}
	}

	private void setupResultSlot(int index) {
		if (this.level.isClientSide()) {
			return;
		}
		if (index >= 0 && index < this.recipesForInput.size()) {
			RecipeHolder<SingleItemRecipe> recipe = this.recipesForInput.get(index);
			this.resultContainer.setRecipeUsed(recipe);
			this.resultSlot.set(recipe.value().assemble(new SingleRecipeInput(this.container.getItem(0))));
		} else {
			this.resultSlot.set(ItemStack.EMPTY);
			this.resultContainer.setRecipeUsed(null);
		}
		this.broadcastChanges();
	}

	public void registerUpdateListener(Runnable slotUpdateListener) {
		this.slotUpdateListener = slotUpdateListener;
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
		return target.container != this.resultContainer && target.container != this.options && target.container != this.worker && super.canTakeItemForPickAll(carried, target);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		ItemStack clicked = ItemStack.EMPTY;
		Slot slot = this.slots.get(slotIndex);
		if (slot != null && slot.hasItem() && slotIndex < USE_ROW_SLOT_END) {
			ItemStack stack = slot.getItem();
			Item item = stack.getItem();
			clicked = stack.copy();
			if (slotIndex == RESULT_SLOT) {
				item.onCraftedBy(stack, player);
				if (!this.moveItemStackTo(stack, INV_SLOT_START, USE_ROW_SLOT_END, true)) {
					return ItemStack.EMPTY;
				}
				slot.onQuickCraft(stack, clicked);
			} else if (slotIndex == INPUT_SLOT) {
				if (!this.moveItemStackTo(stack, INV_SLOT_START, USE_ROW_SLOT_END, false)) {
					return ItemStack.EMPTY;
				}
			} else if (this.kind.acceptsInput().test(this.level, stack)) {
				if (!this.moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
					return ItemStack.EMPTY;
				}
			} else if (slotIndex >= INV_SLOT_START && slotIndex < INV_SLOT_END) {
				if (!this.moveItemStackTo(stack, USE_ROW_SLOT_START, USE_ROW_SLOT_END, false)) {
					return ItemStack.EMPTY;
				}
			} else if (slotIndex >= USE_ROW_SLOT_START && !this.moveItemStackTo(stack, INV_SLOT_START, INV_SLOT_END, false)) {
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
		this.resultContainer.removeItemNoUpdate(1);
		this.access.execute((level, pos) -> this.clearContainer(player, this.container));
	}

	/**
	 * What makes a woodcutter or a stonecutter.
	 *
	 * @param acceptsInput whether shift-clicking an item moves it into the input slot
	 */
	public record Kind(
		Supplier<MenuType<CutterMenu>> menuType,
		Supplier<? extends RecipeType<?>> recipeType,
		Supplier<Block> block,
		BiPredicate<Level, ItemStack> acceptsInput
	) {
	}

	/**
	 * A hidden slot that only carries a recipe result to the client.
	 */
	private static final class OptionSlot extends Slot {
		OptionSlot(Container container, int index) {
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
