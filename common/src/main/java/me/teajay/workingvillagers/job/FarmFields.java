package me.teajay.workingvillagers.job;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.station.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.Nullable;

/**
 * The work of the farmer: harvest the ripe crops on the farmland around its composter,
 * plant the farmland again and bring the harvest to one of the chests linked to the
 * composter. Without a linked chest the farmer does not work.
 *
 * <p>It only works farmland that is already there (it does not till), so it never changes
 * anything but crops. A harvested spot gets the same crop again, empty farmland the crop of
 * its neighbours if the farmer has seeds for it, otherwise the seeds it has most of. It keeps
 * up to {@link Config#KEEP_SEEDS} of every kind of seed (seeds, carrots, potatoes, ...) and
 * takes more from its {@link PersonalChest} when it runs out. It needs a hoe from that chest,
 * which wears out like a player's, one use per harvested crop; without a hoe it waits.
 *
 * <p>When it waits it stands next to the composter, not on it: jumping down from there it
 * would land on the fields.
 */
public class FarmFields extends Behavior<Villager> {
	private static final float SPEED = 0.5F;
	/** How far below and above the composter the fields may be. */
	private static final int FIELD_DOWN = 3;
	private static final int FIELD_UP = 2;
	private static final int WALK_TIMEOUT = 300;
	private static final int SEARCH_COOLDOWN = 100;
	/** How long a spot that could not be reached is ignored. */
	private static final long UNREACHABLE_COOLDOWN = 1200L;
	/** How close (sideways) the farmer has to be to a crop to work on it. */
	private static final double REACH = 1.5;
	private static final PersonalChest.Supply HOE_SUPPLY = new PersonalChest.Supply(FarmFields::isHoe, 1, 1, true, "farmer.no_hoe");

	private enum State { SEARCH, WALK, WORK, DELIVER, WAIT }

	private State state = State.SEARCH;
	private int stateTicks;
	private int waitTicks = SEARCH_COOLDOWN;
	private @Nullable BlockPos target;
	private String waitReason = "farmer.growing";
	private @Nullable ChestTrip trip;
	private final Map<BlockPos, Long> unreachable = new HashMap<>();
	private final PersonalChest personalChest = new PersonalChest();

	public FarmFields() {
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
		if (!villager.getVillagerData().profession().is(WorkingVillagers.FARMER_PROFESSION)) {
			return false;
		}
		Workstations.Station workplace = this.getWorkplace(level, villager);
		return workplace != null && !workplace.validOutputs(level).isEmpty();
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
		this.setState(State.SEARCH);
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
		Workstations.Station workplace = this.getWorkplace(level, villager);
		if (workplace == null) {
			return;
		}
		this.stateTicks++;
		this.unreachable.values().removeIf(until -> until < timestamp);
		if (this.state == State.SEARCH && !hasDeliverables(villager.getInventory())
			&& this.personalChest.busy(level, villager, List.of(HOE_SUPPLY, seedSupply()))) {
			return; // the chest sets the status
		}

		switch (this.state) {
			case SEARCH -> this.search(level, villager, workplace);
			case WALK -> this.walk(level, villager, timestamp);
			case WORK -> this.work(level, villager);
			case DELIVER -> this.deliver(level, villager, workplace);
			case WAIT -> {
				if (this.stateTicks >= this.waitTicks) {
					this.setState(State.SEARCH);
				}
			}
		}
		this.report(villager);
	}

	private void search(ServerLevel level, Villager villager, Workstations.Station workplace) {
		SimpleContainer inventory = villager.getInventory();
		if (hasDeliverables(inventory) && !inventory.canAddItem(new ItemStack(Items.WHEAT, 64))) {
			this.setState(State.DELIVER);
			return;
		}
		boolean[] seedsMissing = {false};
		Optional<BlockPos> found = this.findWork(level, villager, workplace.pos(), seedsMissing);
		if (found.isPresent()) {
			this.target = found.get();
			this.setState(State.WALK);
		} else if (hasDeliverables(inventory)) {
			this.setState(State.DELIVER);
		} else {
			this.rest(level, villager, workplace, SEARCH_COOLDOWN, seedsMissing[0] ? "farmer.no_seeds" : "farmer.growing");
		}
	}

	/**
	 * The closest spot above farmland with a ripe crop, or empty farmland the farmer has seeds
	 * for. Sets {@code seedsMissing} if there is empty farmland but no seeds.
	 */
	private Optional<BlockPos> findWork(ServerLevel level, Villager villager, BlockPos center, boolean[] seedsMissing) {
		int radius = Config.FIELD_RADIUS.get();
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -FIELD_DOWN, -radius), center.offset(radius, FIELD_UP, radius))) {
			if (this.unreachable.containsKey(pos) || !(level.getBlockState(pos.below()).getBlock() instanceof FarmlandBlock)) {
				continue;
			}
			BlockState state = level.getBlockState(pos);
			boolean work = isRipe(state);
			if (!work && state.isAir()) {
				work = seedFor(level, pos, villager.getInventory()) >= 0;
				seedsMissing[0] |= !work;
			}
			double distance = pos.distToCenterSqr(villager.position());
			if (work && distance < bestDistance) {
				best = pos.immutable();
				bestDistance = distance;
			}
		}
		return Optional.ofNullable(best);
	}

	/** Seeds from the personal chest when the farmer has none left. */
	private static PersonalChest.Supply seedSupply() {
		return new PersonalChest.Supply(FarmFields::isSeed, 1, Config.KEEP_SEEDS.get(), false, "");
	}

	private static boolean isRipe(BlockState state) {
		return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
	}

	static boolean isHoe(ItemStack stack) {
		return stack.is(ItemTags.HOES);
	}

	public static boolean isSeed(ItemStack stack) {
		return stack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS) && stack.getItem() instanceof BlockItem;
	}

	/**
	 * The inventory slot with the seeds to plant at the spot, -1 if none fits: the crop of a
	 * neighbouring spot if the farmer has seeds for it, so rows stay the same, otherwise the
	 * seeds it has most of.
	 */
	private static int seedFor(ServerLevel level, BlockPos pos, SimpleContainer inventory) {
		for (Direction side : Direction.Plane.HORIZONTAL) {
			Block neighbour = level.getBlockState(pos.relative(side)).getBlock();
			if (neighbour instanceof CropBlock) {
				int slot = slotOf(inventory, neighbour.asItem());
				if (slot >= 0 && canPlant(level, pos, inventory.getItem(slot))) {
					return slot;
				}
			}
		}
		int best = -1;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (isSeed(stack) && canPlant(level, pos, stack) && (best < 0 || stack.getCount() > inventory.getItem(best).getCount())) {
				best = slot;
			}
		}
		return best;
	}

	private static boolean canPlant(ServerLevel level, BlockPos pos, ItemStack seed) {
		return seed.getItem() instanceof BlockItem item && item.getBlock().defaultBlockState().canSurvive(level, pos);
	}

	private static int slotOf(SimpleContainer inventory, Item item) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (inventory.getItem(slot).is(item)) {
				return slot;
			}
		}
		return -1;
	}

	private void walk(ServerLevel level, Villager villager, long timestamp) {
		if (this.target == null) {
			this.setState(State.SEARCH);
			return;
		}
		if (isWithinReach(villager, this.target)) {
			ItemStack hoe = findHoe(villager.getInventory());
			if (hoe.isEmpty()) {
				this.target = null;
				this.setState(State.SEARCH); // fetches a new one first
				return;
			}
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			villager.setItemSlot(EquipmentSlot.MAINHAND, hoe.copy());
			villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
			this.setState(State.WORK);
		} else if (this.stateTicks > WALK_TIMEOUT) {
			this.unreachable.put(this.target, timestamp + UNREACHABLE_COOLDOWN);
			this.target = null;
			this.setState(State.SEARCH);
		} else if (this.stateTicks % 20 == 1) {
			this.walkTo(villager, this.target, 1);
		}
	}

	/**
	 * Whether the farmer can reach the crop: next to it or on it, also from below a raised bed
	 * or from the edge above a sunken one.
	 */
	private static boolean isWithinReach(Villager villager, BlockPos crop) {
		double dx = villager.getX() - (crop.getX() + 0.5);
		double dz = villager.getZ() - (crop.getZ() + 0.5);
		double dy = villager.getY() - crop.getY();
		return dx * dx + dz * dz < REACH * REACH && dy > -1.5 && dy < 1.5;
	}

	/** Harvests the ripe crop and plants it again, or plants the empty farmland. */
	private void work(ServerLevel level, Villager villager) {
		BlockPos pos = this.target;
		if (pos == null) {
			this.setState(State.SEARCH);
			return;
		}
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(pos));
		if (this.stateTicks % 10 == 0) {
			villager.swingForAttack(InteractionHand.MAIN_HAND);
		}
		if (this.stateTicks < Math.max(1, Math.round(Config.SECONDS_PER_CROP.get() * 20))) {
			return;
		}
		this.target = null;
		this.setState(State.SEARCH);
		BlockState state = level.getBlockState(pos);
		SimpleContainer inventory = villager.getInventory();
		if (isRipe(state)) {
			List<ItemStack> drops = Block.getDrops(state, level, pos, null, villager, ItemStack.EMPTY);
			level.destroyBlock(pos, false, villager);
			for (ItemStack drop : drops) {
				ItemStack rest = inventory.addItem(drop);
				if (!rest.isEmpty()) {
					villager.spawnAtLocation(level, rest);
				}
			}
			((HiredVillager) villager).workingvillagers$addWorkDone(ChopTrees.today(level), 1);
			wearHoe(level, villager);
			// The same crop again
			int slot = slotOf(inventory, state.getBlock().asItem());
			if (slot >= 0 && canPlant(level, pos, inventory.getItem(slot))) {
				plant(level, villager, pos, slot);
			}
		} else if (state.isAir() && level.getBlockState(pos.below()).getBlock() instanceof FarmlandBlock) {
			int slot = seedFor(level, pos, inventory);
			if (slot >= 0) {
				plant(level, villager, pos, slot);
			}
		}
	}

	/** One use of the hoe per harvested crop; a broken hoe is gone. */
	private static void wearHoe(ServerLevel level, Villager villager) {
		ItemStack hoe = findHoe(villager.getInventory());
		if (hoe.isEmpty()) {
			return;
		}
		hoe.hurtAndBreak(1, level, null, broken -> {
			villager.onEquippedItemBroken(broken, EquipmentSlot.MAINHAND);
			villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		});
		villager.getInventory().setChanged();
	}

	private static ItemStack findHoe(SimpleContainer inventory) {
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && isHoe(stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static void plant(ServerLevel level, Villager villager, BlockPos pos, int slot) {
		SimpleContainer inventory = villager.getInventory();
		ItemStack seed = inventory.getItem(slot);
		BlockState crop = ((BlockItem) seed.getItem()).getBlock().defaultBlockState();
		level.setBlockAndUpdate(pos, crop);
		level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(villager, crop));
		level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
		seed.shrink(1);
		inventory.setItem(slot, seed.isEmpty() ? ItemStack.EMPTY : seed);
	}

	private void deliver(ServerLevel level, Villager villager, Workstations.Station workplace) {
		if (this.trip == null) {
			villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			if (!hasDeliverables(villager.getInventory())) {
				this.setState(State.SEARCH);
				return;
			}
			Optional<BlockPos> chest = Chests.closest(workplace.validOutputs(level).stream().filter(pos -> Chests.hasRoom(level, pos)).toList(), villager);
			if (chest.isEmpty()) {
				this.rest(level, villager, workplace, SEARCH_COOLDOWN, "chests_full");
				return;
			}
			this.trip = ChestTrip.to(level, villager, chest.get());
		}
		ChestTrip.Result result = this.trip.tick(level, villager, container -> handIn(villager.getInventory(), container));
		if (result == ChestTrip.Result.RUNNING) {
			return;
		}
		this.trip = null;
		if (result == ChestTrip.Result.FAILED) {
			this.rest(level, villager, workplace, SEARCH_COOLDOWN, "chest_unreachable");
		} else {
			// Did not all fit: the next round picks another chest with room
			this.setState(hasDeliverables(villager.getInventory()) ? State.DELIVER : State.SEARCH);
		}
	}

	/**
	 * Goes back to the composter and waits next to it. To a free spot beside it: walking to the
	 * composter itself, the farmer would climb on it and jump down onto the fields.
	 */
	private void rest(ServerLevel level, Villager villager, Workstations.Station workplace, int ticks, String reason) {
		villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		Optional<BlockPos> spot = StandSpots.find(level, workplace.pos(), villager);
		if (spot.isPresent()) {
			if (!StandSpots.isAt(villager, spot.get())) {
				this.walkTo(villager, spot.get(), 0);
			}
			villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(workplace.pos()));
		} else {
			this.walkTo(villager, workplace.pos(), 2);
		}
		this.waitReason = reason;
		this.waitTicks = ticks;
		this.setState(State.WAIT);
	}

	private void report(Villager villager) {
		switch (this.state) {
			case SEARCH -> WorkerStatus.task(villager, "farmer.search");
			case WALK, WORK -> WorkerStatus.task(villager, "farmer.work");
			case DELIVER -> WorkerStatus.task(villager, "deliver");
			case WAIT -> WorkerStatus.task(villager, this.waitReason);
		}
	}

	private static boolean hasDeliverables(SimpleContainer inventory) {
		for (int amount : amountsToHandIn(inventory)) {
			if (amount > 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * How much of each inventory slot the farmer hands in: the whole harvest, food included,
	 * but the hoe and the seeds it keeps for planting.
	 */
	private static int[] amountsToHandIn(SimpleContainer inventory) {
		int[] amounts = new int[inventory.getContainerSize()];
		Map<Item, Integer> keptSeeds = new HashMap<>();
		for (int slot = 0; slot < amounts.length; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.isEmpty() || isHoe(stack)) {
				continue;
			}
			int keep = 0;
			if (isSeed(stack)) {
				int kept = keptSeeds.getOrDefault(stack.getItem(), 0);
				keep = Math.min(stack.getCount(), Config.KEEP_SEEDS.get() - kept);
				keptSeeds.put(stack.getItem(), kept + keep);
			}
			amounts[slot] = stack.getCount() - keep;
		}
		return amounts;
	}

	private static void handIn(SimpleContainer inventory, Container container) {
		int[] amounts = amountsToHandIn(inventory);
		for (int slot = 0; slot < amounts.length; slot++) {
			if (amounts[slot] <= 0) {
				continue;
			}
			ItemStack stack = inventory.getItem(slot);
			ItemStack rest = HopperBlockEntity.addItem(null, container, stack.copyWithCount(amounts[slot]), null);
			int kept = stack.getCount() - amounts[slot] + rest.getCount();
			inventory.setItem(slot, kept <= 0 ? ItemStack.EMPTY : stack.copyWithCount(kept));
		}
	}

	private void walkTo(Villager villager, BlockPos pos, int closeEnough) {
		villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(pos, SPEED, closeEnough));
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(pos));
	}

	private void setState(State state) {
		this.state = state;
		this.stateTicks = 0;
	}
}
