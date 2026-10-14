package me.teajay.workingvillagers.item;

import me.teajay.workingvillagers.job.HiredVillager;
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
 * Used on an unemployed villager, it hires the villager and gives it a name. A hired villager
 * takes a workstation of this mod nearby instead of the usual village jobs.
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
		if (villager.isBaby() || !villager.getVillagerData().profession().is(VillagerProfession.NONE)) {
			villager.makeSound(SoundEvents.VILLAGER_NO);
			player.sendOverlayMessage(Component.translatable("message.workingvillagers.contract.not_unemployed"));
			return InteractionResult.CONSUME;
		}

		hired.workingvillagers$setEmployer(player.getUUID());
		String name = WorkerNames.ensureName(villager);
		stack.consume(1, player);
		villager.makeSound(SoundEvents.VILLAGER_YES);
		// Happy villager particles
		level.broadcastEntityEvent(villager, (byte) 14);
		player.sendOverlayMessage(Component.translatable("message.workingvillagers.contract.hired", name));
		return InteractionResult.SUCCESS_SERVER;
	}
}
