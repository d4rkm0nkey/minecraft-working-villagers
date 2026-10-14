package me.teajay.workingvillagers.job;

import com.google.common.collect.ImmutableMap;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.crafting.CuttingRecipes;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The work at a woodcutter or stonecutter: fetch material from an input chest, cut it at
 * the job site into what the station's order asks for and bring the products to an output
 * chest. Without an order, input chest or output chest the worker does not work, nor
 * before it took its wage from its {@link PersonalChest}.
 *
 * <p>The worker stands in front of the station and makes {@link Config#CUT_ITEMS_PER_SECOND} items
 * per second, slower than a player. Material and products are kept in the villager's
 * inventory, so nothing is lost when the work is interrupted.
 */
public class CutAtStation extends Behavior<Villager> {
	private static final float SPEED = 0.5F;
	/** A single cut never takes less than this, even if it makes only one item. */
	private static final int MIN_CUT_TICKS = 20;
	private static final int WALK_TIMEOUT = 400;
	private static final int IDLE_TICKS = 100;
	/** Fallback if there is no free spot next to the station. */
	private static final double STATION_REACH = 2.0;
	private static final long RECIPE_REFRESH_TICKS = 1200L;

	private enum State { PLAN, FETCH, WALK_TO_STATION, CUT, DELIVER, WAIT }

	private final ResourceKey<VillagerProfession> profession;
	private final Supplier<? extends RecipeType<?>> recipeType;
	private State state = State.PLAN;
	private int stateTicks;
	private @Nullable ChestTrip trip;
	private @Nullable BlockPos stationSpot;
	/** Ticks the current cut takes, 0 if the worker is not cutting anything. */
	private int cutTicks;
	private List<Item> recipesFor = List.of();
	private List<SingleItemRecipe> recipes = List.of();
	private long recipesTime;
	/** Why the worker waits, and what it cuts right now, for the status in the menu. */
	private String waitReason = "saw.no_material";
	private ItemStack cutting = ItemStack.EMPTY;
	private final PersonalChest personalChest = new PersonalChest();

	public CutAtStation(ResourceKey<VillagerProfession> profession, Supplier<? extends RecipeType<?>> recipeType) {
		super(ImmutableMap.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT), 24000);
		this.profession = profession;
		this.recipeType = recipeType;
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
		if (!villager.getVillagerData().profession().is(this.profession)) {
			return false;
		}
		Workstations.Station station = this.getWorkplace(level, villager);
		return station != null
			&& station.order() != null
			&& !station.validInputs(level).isEmpty()
			&& !station.validOutputs(level).isEmpty();
	}

	private Workstations.@Nullable Station getWorkplace(ServerLevel level, Villager villager) {
		Optional<GlobalPos> jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
		if (jobSite.isEmpty() || jobSite.get().dimension() != level.dimension()) {
			return null;
		}
		return Workstations.find(level, jobSite.get().pos()).filter(station -> station.type().isProcessing()).orElse(null);
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
		villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		this.personalChest.stop(villager);
		if (this.trip != null) {
			this.trip.close(villager);
			this.trip = null;
		}
	}

	@Override
	protected void tick(ServerLevel level, Villager villager, long timestamp) {
		Workstations.Station station = this.getWorkplace(level, villager);
		if (station == null || station.order() == null) {
			return;
		}
		this.stateTicks++;
		if (this.state == State.PLAN && this.personalChest.busy(level, villager, List.of())) {
			return; // wage first, the chest sets the status
		}
		List<SingleItemRecipe> recipes = this.recipesFor(level, station.products(), timestamp);
		Predicate<ItemStack> isMaterial = stack -> station.acceptsMaterial(stack) && recipes.stream().anyMatch(recipe -> recipe.input().test(stack));

		switch (this.state) {
			case PLAN -> this.plan(level, villager, station, isMaterial);
			case FETCH -> this.fetch(level, villager, isMaterial);
			case WALK_TO_STATION -> this.walkToStation(villager, station);
			case CUT -> this.cut(level, villager, station, recipes, isMaterial);
			case DELIVER -> this.deliver(level, villager, station, isMaterial);
			case WAIT -> {
				if (this.stateTicks >= IDLE_TICKS) {
					this.setState(State.PLAN);
				}
			}
		}
		this.report(villager);
	}

	/** Tells the menus what the worker is doing. */
	private void report(Villager villager) {
		switch (this.state) {
			case PLAN, FETCH -> WorkerStatus.task(villager, "saw.fetch");
			case WALK_TO_STATION -> WorkerStatus.task(villager, "saw.walk");
			case CUT -> WorkerStatus.task(villager, "saw.cut", this.cutting.isEmpty() ? Component.literal("…") : this.cutting.getHoverName());
			case DELIVER -> WorkerStatus.task(villager, "deliver");
			case WAIT -> WorkerStatus.task(villager, this.waitReason);
		}
	}

	private void plan(ServerLevel level, Villager villager, Workstations.Station station, Predicate<ItemStack> isMaterial) {
		SimpleContainer inventory = villager.getInventory();
		if (inventory.hasAnyMatching(isProduct(isMaterial))) {
			this.setState(State.DELIVER);
		} else if (inventory.hasAnyMatching(isMaterial)) {
			this.goToStation(level, villager, station);
		} else {
			Optional<BlockPos> chest = station.validInputs(level).stream()
				.filter(pos -> Chests.contains(level, pos, isMaterial))
				.min(Comparator.comparingDouble(pos -> pos.distToCenterSqr(villager.position())));
			if (chest.isPresent()) {
				this.trip = ChestTrip.to(level, villager, chest.get());
				this.setState(State.FETCH);
			} else {
				this.idle(villager, station, "saw.no_material");
			}
		}
	}

	private void fetch(ServerLevel level, Villager villager, Predicate<ItemStack> isMaterial) {
		if (this.trip == null) {
			this.setState(State.PLAN);
			return;
		}
		ChestTrip.Result result = this.trip.tick(level, villager, container -> Chests.take(container, villager.getInventory(), isMaterial, Config.CUT_BATCH.get()));
		if (result != ChestTrip.Result.RUNNING) {
			this.trip = null;
			this.setState(result == ChestTrip.Result.DONE ? State.PLAN : State.WAIT);
			this.waitReason = "chest_unreachable";
		}
	}

	private void goToStation(ServerLevel level, Villager villager, Workstations.Station station) {
		this.stationSpot = StandSpots.find(level, station.pos(), villager).orElse(null);
		this.setState(State.WALK_TO_STATION);
	}

	private void walkToStation(Villager villager, Workstations.Station station) {
		boolean there = this.stationSpot != null ? StandSpots.isAt(villager, this.stationSpot) : station.pos().closerToCenterThan(villager.position(), STATION_REACH);
		if (there) {
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			this.cutTicks = 0;
			this.setState(State.CUT);
		} else if (this.stateTicks > WALK_TIMEOUT) {
			this.waitReason = "saw.station_unreachable";
			this.setState(State.WAIT);
		} else if (this.stateTicks % 20 == 1) {
			BlockPos target = this.stationSpot != null ? this.stationSpot : station.pos();
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, SPEED, this.stationSpot != null ? 0 : 1));
			villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(station.pos()));
		}
	}

	private void cut(ServerLevel level, Villager villager, Workstations.Station station, List<SingleItemRecipe> recipes, Predicate<ItemStack> isMaterial) {
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(station.pos()));
		SimpleContainer inventory = villager.getInventory();
		int slot = this.findMaterial(inventory, isMaterial);
		if (slot < 0) {
			this.finishCutting(villager);
			return;
		}
		ItemStack material = inventory.getItem(slot);
		SingleItemRecipe recipe = recipes.stream().filter(r -> r.input().test(material)).findFirst().orElseThrow();
		ItemStack result = recipe.assemble(new SingleRecipeInput(material));
		this.cutting = result;
		if (this.cutTicks == 0) {
			// Start the next cut
			if (!inventory.canAddItem(result) && material.getCount() > 1) {
				this.finishCutting(villager); // no room for the products, deliver first
				return;
			}
			this.cutTicks = Math.max(MIN_CUT_TICKS, result.getCount() * 20 / Config.CUT_ITEMS_PER_SECOND.get());
			this.stateTicks = 0;
			villager.setItemSlot(EquipmentSlot.MAINHAND, material.copyWithCount(1));
			villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
		}

		if (this.stateTicks % 4 == 0) {
			this.sawdust(level, station.pos(), material);
		}
		if (this.stateTicks % 10 == 0) {
			villager.swingForAttack(InteractionHand.MAIN_HAND);
		}
		if (this.stateTicks < this.cutTicks) {
			return;
		}

		material.shrink(1);
		inventory.setItem(slot, material.isEmpty() ? ItemStack.EMPTY : material);
		ItemStack rest = inventory.addItem(result);
		if (!rest.isEmpty()) {
			villager.spawnAtLocation(level, rest);
		}
		level.playSound(null, station.pos(), SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 0.5F, 1.0F);
		this.cutTicks = 0;
	}

	private int findMaterial(SimpleContainer inventory, Predicate<ItemStack> isMaterial) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty() && isMaterial.test(stack)) {
				return slot;
			}
		}
		return -1;
	}

	private void finishCutting(Villager villager) {
		villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		this.cutTicks = 0;
		this.setState(State.DELIVER);
	}

	/** Chips of the material flying off the saw. */
	private void sawdust(ServerLevel level, BlockPos station, ItemStack material) {
		BlockState state = material.getItem() instanceof BlockItem blockItem
			? blockItem.getBlock().defaultBlockState()
			: level.getBlockState(station);
		level.sendParticles(
			new BlockParticleOption(ParticleTypes.BLOCK, state),
			station.getX() + 0.5, station.getY() + 0.7, station.getZ() + 0.5,
			4, 0.15, 0.05, 0.15, 0.05
		);
	}

	private void deliver(ServerLevel level, Villager villager, Workstations.Station station, Predicate<ItemStack> isMaterial) {
		Predicate<ItemStack> isProduct = isProduct(isMaterial);
		if (this.trip == null) {
			if (!villager.getInventory().hasAnyMatching(isProduct)) {
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
		ChestTrip.Result result = this.trip.tick(level, villager, container -> Chests.deposit(villager.getInventory(), container, isProduct));
		if (result != ChestTrip.Result.RUNNING) {
			// If not everything fit, the next round picks another chest with room
			this.trip = null;
			this.setState(result == ChestTrip.Result.DONE ? State.PLAN : State.WAIT);
			this.waitReason = "chest_unreachable";
		}
	}

	/** Nothing to do: wait at the job site. */
	private void idle(Villager villager, Workstations.Station station, String reason) {
		this.waitReason = reason;
		villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(station.pos(), SPEED, 2));
		this.setState(State.WAIT);
	}

	/**
	 * The recipes for the products, looked up again now and then in case the recipes were reloaded.
	 */
	private List<SingleItemRecipe> recipesFor(ServerLevel level, List<Item> products, long timestamp) {
		if (!products.equals(this.recipesFor) || timestamp - this.recipesTime > RECIPE_REFRESH_TICKS) {
			this.recipes = products.stream().flatMap(product -> CuttingRecipes.forResult(level, this.recipeType.get(), product).stream()).toList();
			this.recipesFor = products;
			this.recipesTime = timestamp;
		}
		return this.recipes;
	}

	/**
	 * Everything the worker hands in: all but food and the material for the current order.
	 * Material of an earlier order is handed in as well.
	 */
	private static Predicate<ItemStack> isProduct(Predicate<ItemStack> isMaterial) {
		return stack -> Chests.isNotFood(stack) && !isMaterial.test(stack);
	}

	private void setState(State state) {
		this.state = state;
		this.stateTicks = 0;
	}
}
