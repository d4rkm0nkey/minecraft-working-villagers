package me.teajay.workingvillagers.item;

import me.teajay.workingvillagers.job.ChopTrees;
import me.teajay.workingvillagers.job.HiredVillager;
import me.teajay.workingvillagers.job.WorkerJobs;
import me.teajay.workingvillagers.job.WorkerNames;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Used on an adult villager, it hires the villager. An unemployed one takes a workstation of
 * this mod nearby instead of the usual village jobs. One with a vanilla job stays at its job
 * site and becomes the worker of this mod there (a farmer at its composter, a mason at its
 * stonecutter, ...), unless it has traded already: it would lose its trades.
 */
public class WorkContractItem extends Item {
	public WorkContractItem(Properties properties) {
		super(properties);
	}

	/**
	 * Called from the villager interaction (before trading), see VillagerMixin.
	 */
	public static InteractionResult hire(Villager villager, Player player, ItemStack stack, InteractionHand hand) {
		if (!(villager.level() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		HiredVillager hired = (HiredVillager) villager;
		if (hired.workingvillagers$isHired()) {
			player.sendOverlayMessage(Component.translatable("message.workingvillagers.contract.already_hired"));
			return InteractionResult.CONSUME;
		}
		boolean employed = !villager.getVillagerData().profession().is(VillagerProfession.NONE);
		Component problem = villager.isBaby() ? Component.translatable("message.workingvillagers.contract.not_unemployed")
			: employed ? WorkerJobs.checkKeepJobSite(level, villager) : null;
		if (problem != null) {
			villager.makeSound(SoundEvents.VILLAGER_NO);
			player.sendOverlayMessage(problem);
			return InteractionResult.CONSUME;
		}

		hired.workingvillagers$setEmployer(player.getUUID());
		hired.workingvillagers$markHired(ChopTrees.today(level));
		WorkerNames.ensureName(villager);
		stack.consume(1, player);
		if (employed) {
			WorkerJobs.keepJobSite(level, villager);
			player.sendOverlayMessage(Component.translatable("message.workingvillagers.contract.hired_here", villager.getName(),
				villager.getVillagerData().profession().value().name()));
			return InteractionResult.SUCCESS_SERVER;
		}
		villager.makeSound(SoundEvents.VILLAGER_YES);
		// Happy villager particles
		level.broadcastEntityEvent(villager, (byte) 14);
		player.sendOverlayMessage(Component.translatable("message.workingvillagers.contract.hired", villager.getName()));
		return InteractionResult.SUCCESS_SERVER;
	}
}
