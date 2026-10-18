package me.teajay.workingvillagers.mixin;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.job.CarryItems;
import me.teajay.workingvillagers.carrier.KeepCarrierJob;
import me.teajay.workingvillagers.job.ChopTrees;
import me.teajay.workingvillagers.job.CraftAtBench;
import me.teajay.workingvillagers.job.CutAtStation;
import me.teajay.workingvillagers.job.DigPit;
import me.teajay.workingvillagers.job.DigTunnel;
import me.teajay.workingvillagers.job.FarmFields;
import me.teajay.workingvillagers.job.FindWorkplace;
import me.teajay.workingvillagers.job.SellGoods;
import me.teajay.workingvillagers.job.SmeltAtFurnace;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.GiveGiftToHero;
import net.minecraft.world.entity.ai.behavior.ResetProfession;
import net.minecraft.world.entity.ai.behavior.UpdateActivityFromSchedule;
import net.minecraft.world.entity.ai.behavior.VillagerGoalPackages;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces the vanilla work activity (standing at the job site) of the professions of this
 * mod with their actual work.
 */
@Mixin(VillagerGoalPackages.class)
public abstract class VillagerGoalPackagesMixin {
	@Inject(method = "getWorkPackage", at = @At("HEAD"), cancellable = true)
	private static void workingvillagers$lumberjackWork(
		Holder<VillagerProfession> profession,
		float speedModifier,
		CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir
	) {
		BehaviorControl<? super Villager> work = null;
		if (profession.is(WorkingVillagers.LUMBERJACK_PROFESSION)) {
			work = new ChopTrees();
		} else if (profession.is(WorkingVillagers.WOODCUTTER_PROFESSION)) {
			work = new CutAtStation(WorkingVillagers.WOODCUTTER_PROFESSION, WorkingVillagers.WOODCUTTING);
		} else if (profession.is(WorkingVillagers.STONECUTTER_PROFESSION)) {
			work = new CutAtStation(WorkingVillagers.STONECUTTER_PROFESSION, () -> RecipeType.STONECUTTING);
		} else if (profession.is(WorkingVillagers.CARPENTER_PROFESSION)) {
			work = new CraftAtBench();
		} else if (profession.is(WorkingVillagers.CARRIER_PROFESSION)) {
			work = new CarryItems();
		} else if (profession.is(WorkingVillagers.FARMER_PROFESSION)) {
			work = new FarmFields();
		} else if (profession.is(WorkingVillagers.SMITH_PROFESSION)) {
			work = new SmeltAtFurnace(SmeltAtFurnace.Kind.SMITH);
		} else if (profession.is(WorkingVillagers.KILN_OPERATOR_PROFESSION)) {
			work = new SmeltAtFurnace(SmeltAtFurnace.Kind.KILN);
		} else if (profession.is(WorkingVillagers.PIT_DIGGER_PROFESSION)) {
			work = new DigPit();
		} else if (profession.is(WorkingVillagers.MINER_PROFESSION)) {
			work = new DigTunnel();
		} else if (profession.is(WorkingVillagers.SHOP_ASSISTANT_PROFESSION)) {
			work = new SellGoods();
		}
		if (work != null) {
			cir.setReturnValue(ImmutableList.of(
				Pair.of(5, work),
				Pair.of(3, new GiveGiftToHero(100)),
				Pair.of(99, UpdateActivityFromSchedule.create())
			));
		}
	}

	/**
	 * Hired villagers without a job look for a workstation of this mod. Before vanilla's
	 * {@code AssignProfessionFromJobSite} (priority 10), which would give the vanilla
	 * profession at a stonecutter, composter or blast furnace.
	 */
	@Inject(method = "getCorePackage", at = @At("RETURN"), cancellable = true)
	private static void workingvillagers$findWorkplace(
		Holder<VillagerProfession> profession,
		float speedModifier,
		CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir
	) {
		cir.setReturnValue(ImmutableList.<Pair<Integer, ? extends BehaviorControl<? super Villager>>>builder()
			.addAll(cir.getReturnValue())
			.add(Pair.of(9, new FindWorkplace()))
			.build());
	}

	/** Carriers have no job site, vanilla would make them unemployed again. */
	@Redirect(
		method = "getCorePackage",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/behavior/ResetProfession;create()Lnet/minecraft/world/entity/ai/behavior/BehaviorControl;")
	)
	private static BehaviorControl<Villager> workingvillagers$keepCarrierJob() {
		return new KeepCarrierJob(ResetProfession.create());
	}
}
