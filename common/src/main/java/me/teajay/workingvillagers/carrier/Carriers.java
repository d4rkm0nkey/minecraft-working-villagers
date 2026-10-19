package me.teajay.workingvillagers.carrier;

import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.job.HiredVillager;
import me.teajay.workingvillagers.job.WorkerJobs;
import me.teajay.workingvillagers.job.WorkerNames;
import me.teajay.workingvillagers.job.WorkerSupplies;
import me.teajay.workingvillagers.menu.CarrierMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Making a villager a carrier by handing it a basket, and taking the basket back.
 */
public final class Carriers {
	private Carriers() {
	}

	public static boolean isCarrier(Villager villager) {
		return villager.getVillagerData().profession().is(WorkingVillagers.CARRIER_PROFESSION);
	}

	/**
	 * The basket works as the contract: an unemployed adult villager takes it and becomes a
	 * carrier with the routes and items of the basket.
	 */
	public static InteractionResult appoint(Villager villager, Player player, ItemStack basket, InteractionHand hand) {
		if (!(villager.level() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		HiredVillager hired = (HiredVillager) villager;
		if (villager.isBaby() || !villager.getVillagerData().profession().is(VillagerProfession.NONE)
			|| hired.workingvillagers$isHired() && !player.getUUID().equals(hired.workingvillagers$getEmployer())) {
			villager.makeSound(SoundEvents.VILLAGER_NO);
			player.sendOverlayMessage(Component.translatable("message.workingvillagers.carrier.not_unemployed"));
			return InteractionResult.CONSUME;
		}

		hired.workingvillagers$setEmployer(player.getUUID());
		WorkerSupplies.onHired(level, villager);
		hired.workingvillagers$setCarrier(CarrierData.fromBasket(basket));
		WorkerNames.ensureName(villager);
		basket.consume(1, player);
		WorkerJobs.giveUpPotentialJobSite(villager);
		villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), WorkingVillagers.CARRIER_PROFESSION));
		villager.refreshBrain(level);
		villager.makeSound(SoundEvents.VILLAGER_YES);
		level.broadcastEntityEvent(villager, (byte) 14);
		player.sendSystemMessage(Component.translatable("message.workingvillagers.carrier.appointed", villager.getName()));
		return InteractionResult.SUCCESS_SERVER;
	}

	/**
	 * The employer takes the basket back (sneak + use with an empty hand), together with what
	 * the carrier carries (its emeralds). The villager is unemployed and no longer under
	 * contract.
	 */
	public static InteractionResult dismiss(Villager villager, Player player) {
		if (!(villager.level() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		HiredVillager hired = (HiredVillager) villager;
		if (hired.workingvillagers$isHired() && !player.getUUID().equals(hired.workingvillagers$getEmployer())) {
			player.sendOverlayMessage(Component.translatable("message.workingvillagers.carrier.not_yours"));
			return InteractionResult.CONSUME;
		}
		dropBasket(villager, player);
		// Its wage stays with the player, an unhired villager would keep it for good
		for (ItemStack stack : villager.getInventory().removeAllItems()) {
			if (!player.getInventory().add(stack)) {
				villager.spawnAtLocation(level, stack);
			}
		}
		hired.workingvillagers$setEmployer(null);
		villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.NONE));
		villager.refreshBrain(level);
		villager.makeSound(SoundEvents.VILLAGER_NO);
		player.sendSystemMessage(Component.translatable("message.workingvillagers.carrier.dismissed", villager.getName()));
		return InteractionResult.SUCCESS_SERVER;
	}

	/**
	 * Whether the player may change the routes of the carrier: its employer, everybody if
	 * the employer shared them, and operators (and the owner of a singleplayer world).
	 */
	public static boolean canEdit(Villager carrier, Player player) {
		CarrierData data = ((HiredVillager) carrier).workingvillagers$getCarrier();
		return canChangeSharing(carrier, player) || data != null && data.shared();
	}

	/** Whether the player may decide who changes the routes. */
	public static boolean canChangeSharing(Villager carrier, Player player) {
		HiredVillager hired = (HiredVillager) carrier;
		return !hired.workingvillagers$isHired() || player.getUUID().equals(hired.workingvillagers$getEmployer()) || isAdmin(player);
	}

	private static boolean isAdmin(Player player) {
		return player instanceof ServerPlayer serverPlayer
			&& (serverPlayer.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
				|| serverPlayer.level().getServer().isSingleplayerOwner(serverPlayer.nameAndId()));
	}

	/**
	 * Opens the carrier menu with the routes and the basket of the carrier.
	 */
	public static InteractionResult openMenu(Villager carrier, Player player) {
		if (!(carrier.level() instanceof ServerLevel)) {
			return InteractionResult.SUCCESS;
		}
		if (((HiredVillager) carrier).workingvillagers$getCarrier() == null) {
			return InteractionResult.PASS;
		}
		RouteHolder holder = RouteHolder.of(carrier);
		player.openMenu(new SimpleMenuProvider((containerId, inventory, user) -> new CarrierMenu(containerId, inventory, holder), holder.name()));
		return InteractionResult.SUCCESS_SERVER;
	}

	/**
	 * Gives the basket with routes and contents to the player, or drops it if there is no
	 * player (the carrier died) or no room.
	 */
	public static void dropBasket(Villager villager, @Nullable Player player) {
		HiredVillager hired = (HiredVillager) villager;
		CarrierData data = hired.workingvillagers$getCarrier();
		if (data == null || !(villager.level() instanceof ServerLevel level)) {
			return;
		}
		ItemStack basket = data.toBasket();
		hired.workingvillagers$setCarrier(null);
		if (player == null || !player.getInventory().add(basket)) {
			villager.spawnAtLocation(level, basket);
		}
	}
}
