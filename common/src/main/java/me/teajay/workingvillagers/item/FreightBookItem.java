package me.teajay.workingvillagers.item;

import me.teajay.workingvillagers.station.StationType;
import me.teajay.workingvillagers.station.VoyageRecorder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.jspecify.annotations.Nullable;

/**
 * The freight book records the route of a hauler: used on a jetty it starts, then the player
 * rows a boat to the other jetty and uses it there (see {@link VoyageRecorder}).
 */
public class FreightBookItem extends Item {
	public FreightBookItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (StationType.of(context.getLevel().getBlockState(context.getClickedPos())) != StationType.JETTY) {
			return InteractionResult.PASS;
		}
		if (context.getPlayer() instanceof ServerPlayer player) {
			VoyageRecorder.use(player, context.getClickedPos(), context.getItemInHand());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void inventoryTick(ItemStack itemStack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		if (owner instanceof ServerPlayer player) {
			VoyageRecorder.tick(player, itemStack);
		}
	}
}
