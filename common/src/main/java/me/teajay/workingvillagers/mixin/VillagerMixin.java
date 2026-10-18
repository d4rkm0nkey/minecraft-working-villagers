package me.teajay.workingvillagers.mixin;

import java.util.UUID;
import me.teajay.workingvillagers.carrier.CarrierData;
import me.teajay.workingvillagers.carrier.Carriers;
import me.teajay.workingvillagers.item.WickerBasketItem;
import me.teajay.workingvillagers.item.WorkContractItem;
import me.teajay.workingvillagers.job.HiredVillager;
import me.teajay.workingvillagers.job.StuckWorkers;
import me.teajay.workingvillagers.job.WorkerJobs;
import me.teajay.workingvillagers.job.WorkerSupplies;
import me.teajay.workingvillagers.station.StationPicks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Villager.class)
public abstract class VillagerMixin implements HiredVillager, ContainerUser {
	@Unique
	private static final String TAG_EMPLOYER = "WorkingVillagersEmployer";
	@Unique
	private static final String TAG_WORK_DAY = "WorkingVillagersDay";
	@Unique
	private static final String TAG_WORK_DONE = "WorkingVillagersDone";

	@Unique
	private @Nullable UUID workingvillagers$employer;
	@Unique
	private long workingvillagers$workDay = -1L;
	@Unique
	private int workingvillagers$workDone;
	@Unique
	private static final String TAG_CARRIER = "WorkingVillagersCarrier";
	@Unique
	private static final String TAG_PAID_DAY = "WorkingVillagersPaidDay";

	@Unique
	private @Nullable BlockPos workingvillagers$openedContainer;
	@Unique
	private @Nullable CarrierData workingvillagers$carrier;
	@Unique
	private @Nullable Component workingvillagers$task;
	@Unique
	private boolean workingvillagers$problem;
	@Unique
	private long workingvillagers$paidDay = -1L;
	@Unique
	private final StuckWorkers.Watch workingvillagers$stuckWatch = new StuckWorkers.Watch();

	@Override
	public StuckWorkers.Watch workingvillagers$getStuckWatch() {
		return this.workingvillagers$stuckWatch;
	}

	@Override
	public boolean workingvillagers$hasProblem() {
		return this.workingvillagers$problem;
	}

	@Override
	public void workingvillagers$setProblem(boolean problem) {
		this.workingvillagers$problem = problem;
	}

	@Override
	public long workingvillagers$getPaidDay() {
		return this.workingvillagers$paidDay;
	}

	@Override
	public void workingvillagers$setPaidDay(long day) {
		this.workingvillagers$paidDay = day;
	}

	@Override
	public @Nullable Component workingvillagers$getTask() {
		return this.workingvillagers$task;
	}

	@Override
	public void workingvillagers$setTask(@Nullable Component task) {
		this.workingvillagers$task = task;
	}

	@Override
	public @Nullable CarrierData workingvillagers$getCarrier() {
		return this.workingvillagers$carrier;
	}

	@Override
	public void workingvillagers$setCarrier(@Nullable CarrierData carrier) {
		this.workingvillagers$carrier = carrier;
	}

	@Override
	public int workingvillagers$getWorkDone(long day) {
		return day == this.workingvillagers$workDay ? this.workingvillagers$workDone : 0;
	}

	@Override
	public void workingvillagers$addWorkDone(long day, int amount) {
		this.workingvillagers$workDone = this.workingvillagers$getWorkDone(day) + amount;
		this.workingvillagers$workDay = day;
	}

	@Override
	public void workingvillagers$setOpenedContainer(@Nullable BlockPos pos) {
		this.workingvillagers$openedContainer = pos;
	}

	/** Like the copper golem: both halves of a double chest count as open. */
	@Override
	public boolean hasContainerOpen(ContainerOpenersCounter container, BlockPos blockPos) {
		BlockPos opened = this.workingvillagers$openedContainer;
		if (opened == null) {
			return false;
		}
		BlockState state = ((Villager) (Object) this).level().getBlockState(opened);
		return opened.equals(blockPos)
			|| state.getBlock() instanceof ChestBlock
				&& state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
				&& ChestBlock.getConnectedBlockPos(opened, state).equals(blockPos);
	}

	@Override
	public double getContainerInteractionRange() {
		return 3.0;
	}

	@Override
	public @Nullable UUID workingvillagers$getEmployer() {
		return this.workingvillagers$employer;
	}

	@Override
	public void workingvillagers$setEmployer(@Nullable UUID employer) {
		this.workingvillagers$employer = employer;
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void workingvillagers$saveEmployer(ValueOutput output, CallbackInfo ci) {
		output.storeNullable(TAG_EMPLOYER, UUIDUtil.CODEC, this.workingvillagers$employer);
		output.storeNullable(TAG_CARRIER, CarrierData.CODEC, this.workingvillagers$carrier);
		output.putLong(TAG_PAID_DAY, this.workingvillagers$paidDay);
		if (this.workingvillagers$workDone > 0) {
			output.putLong(TAG_WORK_DAY, this.workingvillagers$workDay);
			output.putInt(TAG_WORK_DONE, this.workingvillagers$workDone);
		}
	}

	@Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
	private void workingvillagers$readEmployer(ValueInput input, CallbackInfo ci) {
		this.workingvillagers$employer = input.read(TAG_EMPLOYER, UUIDUtil.CODEC).orElse(null);
		this.workingvillagers$carrier = input.read(TAG_CARRIER, CarrierData.CODEC).orElse(null);
		this.workingvillagers$paidDay = input.getLongOr(TAG_PAID_DAY, -1L);
		this.workingvillagers$workDay = input.getLongOr(TAG_WORK_DAY, -1L);
		this.workingvillagers$workDone = input.getIntOr(TAG_WORK_DONE, 0);
	}

	/**
	 * While picking a worker for a workstation, the villager is picked. Work contract and
	 * basket used on a villager do their thing instead of opening
	 * the trade screen. Using a carrier with an empty hand opens its menu, sneak-using it takes
	 * its basket back.
	 */
	@Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
	private void workingvillagers$useTools(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		Villager villager = (Villager) (Object) this;
		ItemStack stack = player.getItemInHand(hand);
		if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer serverPlayer && StationPicks.onUseVillager(serverPlayer, villager)) {
			cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
		} else if (stack.getItem() instanceof WorkContractItem) {
			cir.setReturnValue(WorkContractItem.hire(villager, player, stack, hand));
		} else if (stack.getItem() instanceof WickerBasketItem) {
			cir.setReturnValue(Carriers.appoint(villager, player, stack, hand));
		} else if (WorkerSupplies.give(villager, player, hand) instanceof InteractionResult given) {
			cir.setReturnValue(given);
		} else if (stack.isEmpty() && hand == InteractionHand.MAIN_HAND && Carriers.isCarrier(villager)) {
			cir.setReturnValue(player.isShiftKeyDown() ? Carriers.dismiss(villager, player) : Carriers.openMenu(villager, player));
		}
	}

	/**
	 * A carrier drops its basket with everything in it, every worker what it carries: its
	 * emeralds, tools and what it was working with. A workstation it was walking to is free
	 * again.
	 */
	@Inject(method = "die", at = @At("HEAD"))
	private void workingvillagers$dropBasket(DamageSource source, CallbackInfo ci) {
		Villager villager = (Villager) (Object) this;
		// Vanilla would keep a workstation of this mod taken that the villager was walking to
		WorkerJobs.giveUpPotentialJobSite(villager);
		Carriers.dropBasket(villager, null);
		if (this.workingvillagers$employer != null && villager.level() instanceof ServerLevel level) {
			for (ItemStack stack : villager.getInventory().removeAllItems()) {
				villager.spawnAtLocation(level, stack);
			}
		}
	}

	/**
	 * Workers pick up the emeralds, tools and seeds or saplings thrown to them, but only as
	 * long as they want more of them (vanilla villagers would also pick up every seed).
	 */
	@Inject(method = "wantsToPickUp", at = @At("HEAD"), cancellable = true)
	private void workingvillagers$wantsSupplies(ServerLevel level, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		Villager villager = (Villager) (Object) this;
		if (WorkerSupplies.isSupply(villager, stack)) {
			cir.setReturnValue(WorkerSupplies.accepts(villager, stack) > 0);
		}
	}

	/** Takes only what the worker still wants of a thrown stack, not the whole stack. */
	@Inject(method = "pickUpItem", at = @At("HEAD"), cancellable = true)
	private void workingvillagers$pickUpSupplies(ServerLevel level, ItemEntity entity, CallbackInfo ci) {
		if (WorkerSupplies.pickUp((Villager) (Object) this, entity)) {
			ci.cancel();
		}
	}

	/**
	 * A carrier has no job site, but vanilla only lets villagers with one work. Lifts that
	 * requirement for carriers, so they work during the work hours like everybody else.
	 */
	@Inject(method = "registerBrainGoals", at = @At("TAIL"))
	private void workingvillagers$carrierWorkHours(Brain<Villager> brain, CallbackInfo ci) {
		if (Carriers.isCarrier((Villager) (Object) this)) {
			((BrainAccessor) brain).workingvillagers$getActivityRequirements().put(Activity.WORK, java.util.Set.of());
		}
	}

	@Inject(method = "customServerAiStep", at = @At("TAIL"))
	private void workingvillagers$tickJob(ServerLevel level, CallbackInfo ci) {
		WorkerJobs.tick(level, (Villager) (Object) this);
	}
}
