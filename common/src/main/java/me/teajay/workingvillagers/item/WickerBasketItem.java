package me.teajay.workingvillagers.item;

import me.teajay.workingvillagers.carrier.RouteHolder;
import me.teajay.workingvillagers.menu.CarrierMenu;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The carrier's basket, its workstation and contract at once. Handing it to an unemployed
 * villager makes it a carrier (see Carriers). Used in the air, it opens the carrier menu for
 * the routes on the basket, so they can be set up before handing it over.
 */
public class WickerBasketItem extends Item {
	public WickerBasketItem(Properties properties) {
		super(properties);
	}

	/** Used in the air, it opens the menu for the routes stored on the basket. */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel) {
			RouteHolder holder = RouteHolder.of(player, hand);
			player.openMenu(new SimpleMenuProvider((containerId, inventory, user) -> new CarrierMenu(containerId, inventory, holder), holder.name()));
		}
		return InteractionResult.SUCCESS;
	}
}
