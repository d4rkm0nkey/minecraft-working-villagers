package me.teajay.workingvillagers.gametest.simulation;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.netty.channel.embedded.EmbeddedChannel;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.job.WorkerChunks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.util.Util;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;

/**
 * The village simulation: {@code /wvsim build [wings]} builds the village where you stand,
 * the monitor then records into {@code <world>/simulation}. {@code /wvsim run <days>} lets
 * the server sprint through the days and writes the summary.
 *
 * <p>Started with {@code -Dwvsim.auto=true} (the {@code runSimulation} task), a dedicated
 * server builds the village on its own, keeps a stand-in player above it (mobs only spawn and
 * crops only grow near players), sprints {@code wvsim.days} days and stops.
 *
 * <p>With {@code -Dwvsim.far=true} the workers work without a player nearby instead (see
 * {@link WorkerChunks}): shortly after the start every worker is switched to it, the
 * forceload is taken away and the stand-in moves {@link #FAR_OFFSET} blocks away. Only the
 * chunks of the workers then tick; the customers stand still.
 */
public final class Simulation {
	private static final boolean AUTO = Boolean.getBoolean("wvsim.auto");
	private static final int AUTO_WINGS = Integer.getInteger("wvsim.wings", 2);
	private static final int AUTO_DAYS = Integer.getInteger("wvsim.days", 5);
	/** The view distance of the stand-in player: the chunks loaded and ticked around it. */
	private static final int VIEW = Integer.getInteger("wvsim.view", 6);
	private static final boolean AUTO_WALLED = Boolean.getBoolean("wvsim.walled");
	private static final boolean AUTO_SPRINT = Boolean.parseBoolean(System.getProperty("wvsim.sprint", "true"));
	private static final boolean AUTO_FAR = Boolean.getBoolean("wvsim.far");
	/** How far (east) the stand-in moves away from the village with {@code wvsim.far}. */
	private static final int FAR_OFFSET = 3000;
	/** Ticks after the start when the workers are switched to working without a player, -1: not (any more). */
	private static int farAt = -1;
	/** Whether the stand-in stays away from the village. */
	private static boolean far;

	private static SimRegistry registry;
	private static SimMonitor monitor;
	private static List<String> warnings = new ArrayList<>();
	private static ServerPlayer standIn;
	private static EmbeddedChannel standInChannel;
	private static Field keepAliveTime;
	/** The last day to run, -1 to run on. */
	private static long stopAfterDay = -1;
	private static boolean haltWhenDone;
	private static int ticks;

	private Simulation() {
	}

	public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("wvsim")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("build")
				.executes(context -> build(context, 2, false))
				.then(Commands.argument("wings", IntegerArgumentType.integer(1, 8))
					.executes(context -> build(context, IntegerArgumentType.getInteger(context, "wings"), false))
					.then(Commands.literal("walled")
						.executes(context -> build(context, IntegerArgumentType.getInteger(context, "wings"), true)))))
			.then(Commands.literal("run")
				.then(Commands.argument("days", IntegerArgumentType.integer(1, 100))
					.executes(context -> run(context.getSource().getServer(), IntegerArgumentType.getInteger(context, "days"), false))))
			.then(Commands.literal("summary")
				.executes(context -> {
					summary(context.getSource().getServer());
					context.getSource().sendSuccess(() -> Component.literal("Summary written to " + outDir(context.getSource().getServer())), false);
					return 1;
				})));
	}

	private static int build(CommandContext<CommandSourceStack> context, int wings, boolean walled) {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayer();
		BlockPos origin = BlockPos.containing(source.getPosition());
		ServerPlayer employer = player != null ? player : standIn(source.getServer());
		SimVillage village = build(source.getServer(), source.getLevel(), employer, origin, wings, walled);
		source.sendSuccess(() -> Component.literal("Built " + wings + " wings: " + village.registry().workers.size() + " workers, "
			+ village.registry().traders.size() + " customers; " + village.warnings().size() + " warnings (see the log). Recording into "
			+ outDir(source.getServer())), true);
		return 1;
	}

	private static SimVillage build(MinecraftServer server, ServerLevel level, ServerPlayer employer, BlockPos origin, int wings, boolean walled) {
		long started = System.currentTimeMillis();
		SimVillage village = SimVillage.build(level, employer, origin, wings, walled);
		warnings = village.warnings();
		SimMonitor.LOG.info("Built the village with {} wings in {} ms: {} workers, {} customers", wings, System.currentTimeMillis() - started,
			village.registry().workers.size(), village.registry().traders.size());
		for (String warning : warnings) {
			SimMonitor.LOG.warn("Layout: {}", warning);
		}
		registry = village.registry();
		Path out = outDir(server);
		try {
			registry.save(out.resolve("registry.json"));
			Files.write(out.resolve("warnings.txt"), warnings);
		} catch (IOException e) {
			SimMonitor.LOG.warn("Could not save the registry", e);
		}
		monitor = new SimMonitor(registry, out, level);
		return village;
	}

	private static int run(MinecraftServer server, int days, boolean halt) {
		long today = server.overworld().getOverworldClockTime() / 24000L;
		stopAfterDay = today + days - 1;
		haltWhenDone = halt;
		command(server, "tick sprint " + (days * 24000L - server.overworld().getOverworldClockTime() % 24000L + 100));
		return 1;
	}

	static Path outDir(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve("simulation");
	}

	private static void command(MinecraftServer server, String command) {
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
	}

	public static void serverStarted(MinecraftServer server) {
		Path file = outDir(server).resolve("registry.json");
		if (Files.exists(file)) {
			try {
				registry = SimRegistry.load(file);
				monitor = new SimMonitor(registry, outDir(server), server.overworld());
				SimMonitor.LOG.info("Simulation found: {} workers, recording on", registry.workers.size());
			} catch (IOException | RuntimeException e) {
				SimMonitor.LOG.warn("Could not load the simulation", e);
			}
		}
		if (!AUTO) {
			return;
		}
		ServerLevel level = server.overworld();
		if (registry == null) {
			command(server, "time set 0");
			command(server, "weather clear");
			build(server, level, standIn(server), new BlockPos(0, 0, 0), AUTO_WINGS, AUTO_WALLED);
		}
		standIn(server);
		// Every wing ticks, also those beyond the stand-in's simulation distance (a forceload
		// command takes at most 256 chunks, so in strips)
		BlockPos center = registry.center;
		int half = SimVillage.WING_DEPTH + 8;
		int minZ = center.getZ() - (registry.wings + 1) / 2 * SimVillage.WING_LENGTH / 2 - 16;
		int maxZ = center.getZ() + (registry.wings + 1) / 2 * SimVillage.WING_LENGTH / 2 + 16;
		for (int z = minZ; z <= maxZ; z += 160) {
			command(server, "forceload add " + (center.getX() - half) + " " + z + " " + (center.getX() + half) + " " + Math.min(maxZ, z + 159));
		}
		if (AUTO_FAR) {
			// Once every worker is loaded and has ticked
			farAt = ticks + 100;
		}
		if (AUTO_SPRINT) {
			run(server, AUTO_DAYS, true);
		} else {
			stopAfterDay = server.overworld().getOverworldClockTime() / 24000L + AUTO_DAYS - 1;
			haltWhenDone = true;
		}
	}

	/**
	 * A stand-in player hovering in creative mode above the middle of the village: mobs spawn,
	 * crops and saplings grow and chunks tick only near players. Its connection goes nowhere.
	 */
	private static ServerPlayer standIn(MinecraftServer server) {
		if (standIn != null && !standIn.isRemoved() && server.getPlayerList().getPlayer(standIn.getUUID()) != null) {
			return standIn;
		}
		ServerLevel level = server.overworld();
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.nameUUIDFromBytes("wvsim".getBytes()), "Simulator"), false);
		net.minecraft.server.level.ClientInformation info = net.minecraft.server.level.ClientInformation.createDefault();
		info = new net.minecraft.server.level.ClientInformation(info.language(), VIEW, info.chatVisibility(), info.chatColors(), info.modelCustomisation(),
			info.mainHand(), info.textFilteringEnabled(), info.allowsListing(), info.particleStatus());
		ServerPlayer player = new ServerPlayer(server, level, cookie.gameProfile(), info);
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		standInChannel = new EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(connection, player, new CommonListenerCookie(cookie.gameProfile(), cookie.latency(), info, cookie.transferred()));
		player.setGameMode(GameType.CREATIVE);
		player.getAbilities().mayfly = true;
		player.getAbilities().flying = true;
		player.onUpdateAbilities();
		BlockPos center = registry != null ? registry.center : BlockPos.ZERO;
		int ground = registry != null ? registry.ground : level.getSeaLevel();
		player.teleportTo(level, center.getX() + (far ? FAR_OFFSET : 0) + 0.5, ground + 30, center.getZ() + 0.5, java.util.Set.of(), 0, 0, true);
		standIn = player;
		return player;
	}

	public static void tickStart(MinecraftServer server) {
		if (monitor != null) {
			monitor.tickStart();
		}
	}

	public static void tickEnd(MinecraftServer server) {
		if (standIn != null) {
			keepAlive(standIn);
			standInChannel.releaseOutbound();
			if (++ticks % 1200 == 0 && (standIn.isRemoved() || server.getPlayerList().getPlayer(standIn.getUUID()) == null)) {
				SimMonitor.LOG.warn("The stand-in player left, bringing it back");
				standIn(server);
			}
			if (farAt >= 0 && ticks >= farAt) {
				farAt = -1;
				goFar(server);
			}
		}
		if (monitor == null) {
			return;
		}
		monitor.tickEnd(server);
		if (stopAfterDay >= 0 && server.overworld().getOverworldClockTime() >= (stopAfterDay + 1) * 24000L) {
			stopAfterDay = -1;
			summary(server);
			command(server, "tick sprint stop");
			SimMonitor.LOG.info("Simulation done, results in {}", outDir(server));
			if (haltWhenDone) {
				server.halt(false);
			}
		}
	}

	public static void died(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
		if (monitor != null && entity instanceof net.minecraft.world.entity.npc.villager.Villager villager && entity.level() instanceof ServerLevel level) {
			monitor.died(level, villager, source);
		}
	}

	public static void converted(net.minecraft.world.entity.Entity from, net.minecraft.world.entity.Entity into) {
		if (monitor != null && from instanceof net.minecraft.world.entity.npc.villager.Villager villager && from.level() instanceof ServerLevel level) {
			monitor.converted(level, villager, into);
		}
	}

	/**
	 * Switches every worker to working without a player nearby (at no fee, all of them), takes
	 * the forceload away and moves the stand-in away from the village.
	 */
	private static void goFar(MinecraftServer server) {
		Config.CHUNK_LOADING_FEE.set(0);
		Config.CHUNK_LOADING_MAX_WORKERS.set(256);
		ServerLevel level = server.overworld();
		int on = 0;
		int missing = 0;
		for (SimRegistry.Worker worker : registry.workers) {
			if (level.getEntity(worker.uuid()) instanceof net.minecraft.world.entity.npc.villager.Villager villager) {
				if (WorkerChunks.isOn(level, villager) || WorkerChunks.toggle(level, villager, standIn) != null && WorkerChunks.isOn(level, villager)) {
					on++;
				}
			} else {
				missing++;
			}
		}
		command(server, "forceload remove all");
		far = true;
		standIn.teleportTo(level, registry.center.getX() + FAR_OFFSET + 0.5, registry.ground + 30, registry.center.getZ() + 0.5, java.util.Set.of(), 0, 0, true);
		SimMonitor.LOG.info("Far: {} workers work without a player, {} not found; the stand-in is {} blocks away", on, missing, FAR_OFFSET);
	}

	private static void summary(MinecraftServer server) {
		if (monitor != null) {
			monitor.writeSummary(server, warnings);
		}
	}

	/** Keeps the stand-in's connection from timing out: it never answers the keep-alive. */
	private static void keepAlive(ServerPlayer player) {
		try {
			if (keepAliveTime == null) {
				keepAliveTime = ServerCommonPacketListenerImpl.class.getDeclaredField("keepAliveTime");
				keepAliveTime.setAccessible(true);
			}
			keepAliveTime.setLong(player.connection, Util.getMillis());
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Cannot keep the stand-in player connected", e);
		}
	}
}
