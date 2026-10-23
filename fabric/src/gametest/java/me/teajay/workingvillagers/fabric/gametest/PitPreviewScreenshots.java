package me.teajay.workingvillagers.fabric.gametest;

import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.client.StationScreen;
import me.teajay.workingvillagers.menu.StationMenu;
import me.teajay.workingvillagers.station.StationType;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Not a test: takes screenshots of the outline a player holding string sees at pit stakes
 * ({@code ./gradlew :fabric:runClientGameTest}, the pictures land in
 * {@code fabric/build/run/clientGameTest/screenshots}): a closed ring as the outline of the pit,
 * an open ring with its end in red, and a ring that is too big for a novice, all red.
 */
public class PitPreviewScreenshots implements FabricClientGameTest {
	/** On a mound of stone, so the pit has room to go down in the flat world. */
	private static final BlockPos ORIGIN = new BlockPos(40, -52, 40);

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("gamemode creative @a");
			world.getServer().runCommand("time set noon");
			world.getServer().runOnServer(server -> setUp(server.overworld()));
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STRING));
				// Flying above the corner of the mound, looking down at the rings
				player.getAbilities().flying = true;
				player.onUpdateAbilities();
				player.teleportTo(ORIGIN.getX() - 4.5, ORIGIN.getY() + 7, ORIGIN.getZ() - 4.5);
			});
			context.waitTicks(30);
			world.getConnection().waitForChunksRender();
			context.getInput().lookAt(ORIGIN.offset(9, -2, 9));
			context.runOnClient(minecraft -> {
				minecraft.gui.toastManager().clear();
				minecraft.gui.hud.getChat().clearMessages(false);
			});
			context.waitTicks(10);
			context.takeScreenshot("pit_preview");
			// The settings tab of each stake: the pit, or what is wrong with the ring
			settings(context, world, "pit_settings_pit", ORIGIN.offset(-1, 0, 3));
			settings(context, world, "pit_settings_open", ORIGIN.offset(9, 0, 2));
			settings(context, world, "pit_settings_too_big", ORIGIN.offset(-1, 0, 15));
		}
	}

	private static void settings(ClientGameTestContext context, TestSingleplayerContext world, String name, BlockPos stake) {
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			player.teleportTo(stake.getX() - 1.5, stake.getY(), stake.getZ() + 0.5);
			player.openMenu(StationMenu.provider(StationType.PIT_STAKE, player.level(), stake));
		});
		context.waitFor(minecraft -> minecraft.gui.screen() instanceof StationScreen);
		context.runOnClient(minecraft -> ((StationScreen) minecraft.gui.screen()).showTab("order"));
		context.waitTicks(15);
		context.takeScreenshot(name);
		context.setScreen(() -> null);
		context.waitTicks(2);
	}

	private static void setUp(ServerLevel level) {
		for (BlockPos pos : BlockPos.betweenClosed(ORIGIN.offset(-2, -8, -2), ORIGIN.offset(30, -1, 30))) {
			level.setBlockAndUpdate(pos, pos.getY() == ORIGIN.getY() - 1 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState());
		}
		// A closed ring around 4×5, the stake next to it
		ring(level, ORIGIN, 6, 7, null);
		level.setBlockAndUpdate(ORIGIN.offset(-1, 0, 3), WorkingVillagers.PIT_STAKE.get().defaultBlockState());
		// An open ring
		BlockPos open = ORIGIN.offset(10, 0, 0);
		ring(level, open, 6, 6, open.offset(5, 0, 3));
		level.setBlockAndUpdate(open.offset(-1, 0, 2), WorkingVillagers.PIT_STAKE.get().defaultBlockState());
		// Too big for a novice: 10×10 inside
		BlockPos big = ORIGIN.offset(0, 0, 10);
		ring(level, big, 12, 12, null);
		level.setBlockAndUpdate(big.offset(-1, 0, 5), WorkingVillagers.PIT_STAKE.get().defaultBlockState());
	}

	/** A rectangle of string from {@code corner}, {@code sizeX} by {@code sizeZ}, with a gap at {@code gap} if not null. */
	private static void ring(ServerLevel level, BlockPos corner, int sizeX, int sizeZ, BlockPos gap) {
		for (int x = 0; x < sizeX; x++) {
			for (int z = 0; z < sizeZ; z++) {
				BlockPos pos = corner.offset(x, 0, z);
				boolean edge = x == 0 || z == 0 || x == sizeX - 1 || z == sizeZ - 1;
				if (edge && !pos.equals(gap)) {
					level.setBlockAndUpdate(pos, Blocks.TRIPWIRE.defaultBlockState());
				}
			}
		}
	}
}
