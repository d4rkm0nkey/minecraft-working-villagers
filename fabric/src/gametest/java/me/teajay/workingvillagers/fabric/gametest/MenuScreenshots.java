package me.teajay.workingvillagers.fabric.gametest;

import java.util.List;
import java.util.function.BiConsumer;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.carrier.Carriers;
import me.teajay.workingvillagers.carrier.Route;
import me.teajay.workingvillagers.carrier.RouteHolder;
import me.teajay.workingvillagers.client.CarrierScreen;
import me.teajay.workingvillagers.client.StationScreen;
import me.teajay.workingvillagers.client.StationTabs;
import me.teajay.workingvillagers.client.VillageChestScreen;
import me.teajay.workingvillagers.item.WorkContractItem;
import me.teajay.workingvillagers.job.HiredVillager;
import me.teajay.workingvillagers.job.WorkerJobs;
import me.teajay.workingvillagers.job.WorkerNotice;
import me.teajay.workingvillagers.job.WorkerSupplies;
import me.teajay.workingvillagers.menu.StationMenu;
import me.teajay.workingvillagers.station.StationType;
import me.teajay.workingvillagers.station.Workstations.ChestRole;
import me.teajay.workingvillagers.station.Workstations;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Not a test: takes a screenshot of every menu of the mod, in a small and a large window
 * ({@code ./gradlew :fabric:runClientGameTest}, the pictures land in
 * {@code fabric/build/run/clientGameTest/screenshots}). Each workstation is set up with a
 * worker, chests and supplies first, so the menus show what a player sees.
 */
public class MenuScreenshots implements FabricClientGameTest {
	private static final List<StationType> STATIONS = List.of(StationType.values());

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("time set noon");
			world.getServer().runOnServer(MenuScreenshots::setUp);
			context.waitTicks(40);
			fisherInBoat(context, world);
			guardsInUniform(context, world);
			carpenterMissesMaterial(context, world);
			for (int[] size : new int[][] {{854, 480}, {1920, 1080}}) {
				context.getInput().resizeWindow(size[0], size[1]);
				context.waitTicks(5);
				String suffix = "_" + size[0];
				for (int i = 0; i < STATIONS.size(); i++) {
					StationType type = STATIONS.get(i);
					BlockPos pos = stationPos(i);
					String name = type.name().toLowerCase(java.util.Locale.ROOT);
					tabs(context, world, name + "_overview", suffix, pos, type, (server, player) ->
						player.openMenu(StationMenu.provider(type, player.level(), pos)));
					if (type.hasOwnScreen()) {
						shot(context, world, name + "_block" + suffix, pos, (server, player) -> {
							BlockState state = player.level().getBlockState(pos);
							player.openMenu(state.getMenuProvider(player.level(), pos));
						});
					}
				}
				if (size[0] == 854) {
					clickWageTab(context, world);
				}
				// A vanilla block nobody set up: only the block and the worker tab
				BlockPos plain = stationPos(STATIONS.size() + 1);
				shot(context, world, "stonecutter_plain" + suffix, plain, (server, player) ->
					player.openMenu(player.level().getBlockState(plain).getMenuProvider(player.level(), plain)));
				BlockPos empty = stationPos(STATIONS.size());
				tabs(context, world, "chopping_block_empty", suffix, empty, StationType.CHOPPING_BLOCK, (server, player) ->
					player.openMenu(StationMenu.provider(StationType.CHOPPING_BLOCK, player.level(), empty)));
				tabs(context, world, "village_chest", suffix, villageChestPos(), List.of("chest", "workers"), (server, player) -> {
					BlockPos pos = villageChestPos();
					player.openMenu(player.level().getBlockState(pos).getMenuProvider(player.level(), pos));
				});
				shot(context, world, "chest" + suffix, chestPos(0, false), (server, player) -> {
					BlockPos pos = chestPos(0, false);
					player.openMenu(player.level().getBlockState(pos).getMenuProvider(player.level(), pos));
				});
				tabs(context, world, "carrier", suffix, origin().offset(4, 0, -4), List.of("routes", "basket"), (server, player) -> {
					Villager carrier = player.level().getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(96),
						villager -> Carriers.isCarrier(villager)).getFirst();
					// It walks its routes: right next to it
					player.teleportTo(carrier.getX(), carrier.getY(), carrier.getZ() - 1.0);
					Carriers.openMenu(carrier, player);
				});
			}
		}
	}

	/** Opens the overview and takes a screenshot of each of its tabs. */
	private static void tabs(ClientGameTestContext context, TestSingleplayerContext world, String name, String suffix, BlockPos near, StationType type,
		BiConsumer<MinecraftServer, ServerPlayer> open) {
		List<String> tabs = new java.util.ArrayList<>(List.of("work", "wage", "chests"));
		if (StationTabs.hasOrderTab(type)) {
			tabs.add("order");
		}
		tabs(context, world, name, suffix, near, tabs, open);
	}

	/** Opens a screen with tabs and takes a screenshot of each of them. */
	private static void tabs(ClientGameTestContext context, TestSingleplayerContext world, String name, String suffix, BlockPos near, List<String> tabs,
		BiConsumer<MinecraftServer, ServerPlayer> open) {
		open(context, world, near, open);
		for (String tab : tabs) {
			context.runOnClient(minecraft -> {
				switch (minecraft.gui.screen()) {
					case StationScreen screen -> screen.showTab(tab);
					case CarrierScreen screen -> screen.showTab(tab);
					case VillageChestScreen screen -> screen.showTab(tab);
					default -> throw new IllegalStateException("No tabs: " + minecraft.gui.screen());
				}
			});
			context.waitTicks(3);
			context.takeScreenshot(name + "_" + tab + suffix);
		}
		context.setScreen(() -> null);
		context.waitTicks(2);
	}

	/**
	 * A click on the wage tab above the woodcutter's screen opens the overview on that tab, in
	 * the same place.
	 */
	private static void clickWageTab(ClientGameTestContext context, TestSingleplayerContext world) {
		BlockPos pos = stationPos(STATIONS.indexOf(StationType.WOODCUTTER));
		open(context, world, pos, (server, player) -> player.openMenu(player.level().getBlockState(pos).getMenuProvider(player.level(), pos)));
		double[] cursor = context.computeOnClient(minecraft -> {
			Screen screen = minecraft.gui.screen();
			double scale = minecraft.getWindow().getGuiScale();
			// The third tab (block, worker, wage) above a 176 × 166 window
			return new double[] {((screen.width - 176) / 2 + 2 * 27 + 13) * scale, ((screen.height - 166) / 2 - 14) * scale};
		});
		context.getInput().setCursorPos(cursor[0], cursor[1]);
		context.waitTicks(2);
		// 0: the left mouse button
		context.getInput().pressMouse(0);
		context.waitFor(minecraft -> minecraft.gui.screen() instanceof StationScreen);
		context.waitTicks(5);
		context.runOnClient(minecraft -> {
			String tab = ((StationScreen) minecraft.gui.screen()).shownTab();
			if (!tab.equals("wage")) {
				throw new AssertionError("The overview opened on the tab " + tab + ", not on the wage tab");
			}
		});
		context.takeScreenshot("woodcutter_wage_from_tab_854");
		context.setScreen(() -> null);
		context.waitTicks(2);
	}

	/** Opens a menu next to {@code near} (menus close when the player is far off) and takes the screenshot. */
	private static void shot(ClientGameTestContext context, TestSingleplayerContext world, String name, BlockPos near,
		BiConsumer<MinecraftServer, ServerPlayer> open) {
		open(context, world, near, open);
		context.takeScreenshot(name);
		context.setScreen(() -> null);
		context.waitTicks(2);
	}

	private static void open(ClientGameTestContext context, TestSingleplayerContext world, BlockPos near, BiConsumer<MinecraftServer, ServerPlayer> open) {
		world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst()
			.teleportTo(near.getX() + 0.5, near.getY(), near.getZ() - 1.5));
		context.waitTicks(3);
		// The rename button of a chest needs the crosshair on it, like after a right-click
		context.getInput().lookAt(near);
		context.waitTicks(3);
		world.getServer().runOnServer(server -> open.accept(server, server.getPlayerList().getPlayers().getFirst()));
		context.waitFor(minecraft -> minecraft.gui.screen() instanceof AbstractContainerScreen<?>);
		context.runOnClient(minecraft -> {
			minecraft.gui.toastManager().clear();
			minecraft.gui.hud.getChat().clearMessages(false);
			minecraft.gui.hud.setOverlayMessage(Component.empty(), false);
		});
		context.waitTicks(10);
		context.getInput().setCursorPos(0, 0);
		context.waitTicks(2);
	}

	/** The barrel of the fisher with the lake, away from the other stations. */
	private static BlockPos barrelPos() {
		return origin().offset(0, 0, -17);
	}

	/** A lake south of the barrel, its surface one block below the ground. */
	private static void lake(ServerLevel level) {
		for (int x = -12; x <= 12; x++) {
			for (int z = -42; z <= -19; z++) {
				for (int y = 1; y <= 2; y++) {
					level.setBlockAndUpdate(origin().offset(x, -y, z), net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
				}
			}
		}
		level.setBlockAndUpdate(barrelPos(), net.minecraft.world.level.block.Blocks.BARREL.defaultBlockState());
		chest(level, barrelPos().east(2));
		Workstations.find(level, barrelPos()).orElseThrow().setRole(barrelPos().east(2), ChestRole.OUTPUT);
	}

	/** Waits until the fisher has rowed out onto the lake and takes a picture from the shore. */
	private static void fisherInBoat(ClientGameTestContext context, TestSingleplayerContext world) {
		for (int i = 0; i < 90; i++) {
			boolean out = world.getServer().computeOnServer(server -> server.overworld().getEntitiesOfClass(Villager.class,
					new net.minecraft.world.phys.AABB(barrelPos()).inflate(40), villager -> villager.getVehicle() != null).stream()
				.anyMatch(villager -> villager.getVehicle().position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(barrelPos())) > 8.0));
			if (out) {
				break;
			}
			context.waitTicks(20);
		}
		world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst()
			.teleportTo(barrelPos().getX() + 3.5, barrelPos().getY(), barrelPos().getZ() + 3.5));
		context.waitTicks(5);
		BlockPos boat = world.getServer().computeOnServer(server -> server.overworld().getEntitiesOfClass(Villager.class,
				new net.minecraft.world.phys.AABB(barrelPos()).inflate(40), villager -> villager.getVehicle() != null).stream()
			.findFirst().map(villager -> villager.getVehicle().blockPosition()).orElse(barrelPos().offset(0, 0, -14)));
		context.getInput().lookAt(boat);
		context.runOnClient(minecraft -> {
			minecraft.gui.toastManager().clear();
			minecraft.gui.hud.getChat().clearMessages(false);
		});
		context.waitTicks(10);
		context.takeScreenshot("fisher_in_boat");
	}

	/** Three guards side by side: in a red and in an undyed leather chestplate, and without one. */
	private static void guardsInUniform(ClientGameTestContext context, TestSingleplayerContext world) {
		BlockPos center = uniformPos();
		world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst()
			.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() - 3.5));
		context.waitTicks(5);
		context.getInput().lookAt(center.above());
		context.runOnClient(minecraft -> {
			minecraft.gui.toastManager().clear();
			minecraft.gui.hud.getChat().clearMessages(false);
		});
		context.waitTicks(10);
		context.takeScreenshot("guards_in_uniform");
	}

	/** The carpenter, whose order lacks planks, with the "!" above its head. */
	private static void carpenterMissesMaterial(ClientGameTestContext context, TestSingleplayerContext world) {
		BlockPos bench = stationPos(STATIONS.indexOf(StationType.CARPENTER_BENCH));
		// It takes its wage first, then finds no planks
		for (int waited = 0; waited < 600 && !world.getServer().computeOnServer(server -> carpenter(server, bench) instanceof HiredVillager worker
			&& worker.workingvillagers$getNotice() == WorkerNotice.MISSING); waited += 10) {
			context.waitTicks(10);
		}
		world.getServer().runOnServer(server -> {
			Villager carpenter = (Villager) carpenter(server, bench);
			server.getPlayerList().getPlayers().getFirst().teleportTo(carpenter.getX() + 0.5, carpenter.getY(), carpenter.getZ() - 3.5);
		});
		context.waitTicks(5);
		context.getInput().lookAt(world.getServer().computeOnServer(server -> carpenter(server, bench).blockPosition().above()));
		context.runOnClient(minecraft -> {
			minecraft.gui.toastManager().clear();
			minecraft.gui.hud.getChat().clearMessages(false);
		});
		context.waitTicks(10);
		context.takeScreenshot("carpenter_misses_material");
	}

	private static net.minecraft.world.entity.@org.jspecify.annotations.Nullable Entity carpenter(MinecraftServer server, BlockPos bench) {
		ServerLevel level = server.overworld();
		java.util.UUID worker = Workstations.find(level, bench).map(Workstations.Station::worker).orElse(null);
		return worker == null ? null : level.getEntity(worker);
	}

	private static BlockPos uniformPos() {
		return origin().offset(24, 0, -10);
	}

	private static void uniforms(ServerLevel level, ServerPlayer player) {
		ItemStack[] chests = {
			net.minecraft.world.item.component.DyedItemColor.applyDyes(new ItemStack(Items.LEATHER_CHESTPLATE), List.of(net.minecraft.world.item.DyeColor.RED)),
			new ItemStack(Items.LEATHER_CHESTPLATE), ItemStack.EMPTY
		};
		for (int i = 0; i < chests.length; i++) {
			BlockPos target = uniformPos().offset(-2 + 2 * i, 0, 2);
			level.setBlockAndUpdate(target, net.minecraft.world.level.block.Blocks.TARGET.defaultBlockState());
			Villager guard = hire(level, player, target.north(2));
			WorkerJobs.assign(level, guard, target);
			guard.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, chests[i]);
			guard.setNoAi(true);
			guard.snapTo(target.getX() + 0.5, target.getY(), target.getZ() - 1.5, 180.0F, 0.0F);
			guard.setYHeadRot(180.0F);
			guard.setYBodyRot(180.0F);
		}
	}

	private static BlockPos origin() {
		return new BlockPos(0, -60, 0);
	}

	private static BlockPos stationPos(int i) {
		return origin().offset(-30 + i * 6, 0, 8);
	}

	private static BlockPos chestPos(int i, boolean input) {
		return stationPos(i).offset(input ? -2 : 2, 0, 0);
	}

	private static BlockPos villageChestPos() {
		return origin().offset(0, 0, -6);
	}

	private static void setUp(MinecraftServer server) {
		ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
		ServerLevel level = player.level();
		BlockPos ground = origin();
		player.teleportTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5);
		for (int i = 0; i < STATIONS.size(); i++) {
			StationType type = STATIONS.get(i);
			BlockPos pos = stationPos(i);
			level.setBlockAndUpdate(pos, type.block().defaultBlockState());
			chest(level, chestPos(i, false), new ItemStack(Items.OAK_LOG, 23));
			Workstations.Station station = Workstations.find(level, pos).orElseThrow();
			station.setRole(chestPos(i, false), ChestRole.OUTPUT);
			if (type.isProcessing()) {
				chest(level, chestPos(i, true), new ItemStack(Items.IRON_INGOT, 8));
				station.setRole(chestPos(i, true), ChestRole.INPUT);
			}
			if (type == StationType.FLETCHING_TABLE) {
				station.setOrder(Items.ARROW);
				station.setStock(64);
			}
			if (type == StationType.LECTERN) {
				level.setBlockAndUpdate(pos.west(), net.minecraft.world.level.block.Blocks.ENCHANTING_TABLE.defaultBlockState());
			}
			if (type == StationType.GRINDSTONE) {
				station.setOrder(Items.IRON_AXE);
				station.setStock(4);
			}
			if (type == StationType.CAULDRON) {
				station.setOrder(Items.LEATHER_CHESTPLATE);
				station.setDye(net.minecraft.world.item.DyeColor.RED);
				station.setStock(4);
			}
			if (type == StationType.BREWING_STAND) {
				station.setOrder(Items.POTION, "minecraft:healing");
				station.setStock(6);
			}
			if (type == StationType.WOODCUTTER) {
				station.setShape("planks");
				station.setWood("oak", true);
			}
			if (type == StationType.CARPENTER_BENCH) {
				// Chests ordered once, barrels kept in stock; without planks the carpenter shows a "!"
				station.orderProduct(Items.CHEST);
				station.setBatch(Items.CHEST, 64);
				station.orderProduct(Items.BARREL);
				station.setStock(Items.BARREL, 16);
			}
			if (type == StationType.KILN) {
				station.toggleOrder(Items.GLASS);
				station.setBatch(Items.GLASS, 32);
			}
			if (type == StationType.PIT_STAKE) {
				// A ring of string behind the stake, which the stake takes as its pit
				for (int z = 1; z <= 4; z++) {
					for (int x = -1; x <= 1; x++) {
						if (x != 0 || z == 1 || z == 4) {
							level.setBlockAndUpdate(pos.offset(x, 0, -z), Blocks.TRIPWIRE.defaultBlockState());
						}
					}
				}
			}
			Villager villager = hire(level, player, pos.offset(0, 0, 2));
			Component problem = WorkerJobs.assign(level, villager, pos);
			if (problem != null) {
				throw new IllegalStateException(type + ": " + problem.getString());
			}
			give(player, villager, new ItemStack(Items.EMERALD, 3));
			ItemStack worn = new ItemStack(Items.IRON_AXE);
			worn.setDamageValue(100);
			give(player, villager, worn, new ItemStack(Items.IRON_HOE), new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.IRON_PICKAXE),
				new ItemStack(Items.OAK_SAPLING, 5), new ItemStack(Items.WHEAT_SEEDS, 12), new ItemStack(Items.LADDER, 30), new ItemStack(Items.TORCH, 3),
				new ItemStack(Items.SHEARS), new ItemStack(Items.WHEAT, 10), new ItemStack(Items.IRON_SWORD), new ItemStack(Items.CARROT, 8),
				new ItemStack(Items.COAL, 5), new ItemStack(Items.FISHING_ROD), new ItemStack(Items.BOW), new ItemStack(Items.ARROW, 48));
		}
		level.setBlockAndUpdate(stationPos(STATIONS.size() + 1), Blocks.STONECUTTER.defaultBlockState());
		uniforms(level, player);
		// A fisher with a boat at a lake
		lake(level);
		Villager fisher = hire(level, player, barrelPos().offset(1, 0, 1));
		WorkerJobs.assign(level, fisher, barrelPos());
		give(player, fisher, new ItemStack(Items.EMERALD, 3), new ItemStack(Items.FISHING_ROD), new ItemStack(Items.OAK_BOAT));
		// A chopping block nobody works at, without chests
		level.setBlockAndUpdate(stationPos(STATIONS.size()), StationType.CHOPPING_BLOCK.block().defaultBlockState());
		// A village chest with a worker in reach that lacks something
		level.setBlockAndUpdate(villageChestPos(), WorkingVillagers.VILLAGE_CHEST.get().defaultBlockState());
		Container village = (Container) level.getBlockEntity(villageChestPos());
		village.setItem(0, new ItemStack(Items.EMERALD, 20));
		village.setItem(1, new ItemStack(Items.IRON_AXE));
		// A carrier with two routes
		Villager carrier = spawnVillager(level, origin().offset(4, 0, -4));
		ItemStack basket = new ItemStack(WorkingVillagers.WICKER_BASKET.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, basket);
		Carriers.appoint(carrier, player, basket, InteractionHand.MAIN_HAND);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		RouteHolder holder = RouteHolder.of(carrier);
		holder.addRoute(new Route(chestPos(0, false), chestPos(1, false)));
		holder.addRoute(new Route(chestPos(1, false), villageChestPos()));
		player.getInventory().add(new ItemStack(Items.OAK_PLANKS, 40));
	}

	private static void chest(ServerLevel level, BlockPos pos, ItemStack... items) {
		level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
		Container container = (Container) level.getBlockEntity(pos);
		for (int i = 0; i < items.length; i++) {
			container.setItem(i, items[i]);
		}
	}

	private static Villager spawnVillager(ServerLevel level, BlockPos pos) {
		Villager villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
		villager.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		level.addFreshEntity(villager);
		return villager;
	}

	static Villager hire(ServerLevel level, ServerPlayer player, BlockPos pos) {
		Villager villager = spawnVillager(level, pos);
		ItemStack contract = new ItemStack(WorkingVillagers.WORK_CONTRACT.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, contract);
		WorkContractItem.hire(villager, player, contract, InteractionHand.MAIN_HAND);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		return villager;
	}

	/** What the worker takes of it, the way a player gives it; the rest is dropped. */
	private static void give(ServerPlayer player, Villager villager, ItemStack... items) {
		for (ItemStack item : items) {
			player.setItemInHand(InteractionHand.MAIN_HAND, item.copy());
			WorkerSupplies.give(villager, player, InteractionHand.MAIN_HAND);
		}
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
	}
}
