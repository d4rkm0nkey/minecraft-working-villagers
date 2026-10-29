package me.teajay.workingvillagers;

import com.mojang.brigadier.CommandDispatcher;
import java.util.List;
import java.util.UUID;
import me.teajay.workingvillagers.job.WorkerChunks;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

/**
 * The commands of the mod, for operators:
 * <ul>
 * <li>{@code /workingvillagers chunks}: every worker that works without a player nearby (see
 * {@link WorkerChunks}), with its employer, where it is, whether its chunks are loaded and how
 * many.</li>
 * <li>{@code /workingvillagers chunks off <worker>}: switches it off for the worker, also while
 * it is not loaded.</li>
 * </ul>
 */
public final class WorkingVillagersCommands {
	private WorkingVillagersCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(WorkingVillagers.MOD_ID)
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("chunks")
				.executes(context -> listChunks(context.getSource()))
				.then(Commands.literal("off")
					.then(Commands.argument("worker", UuidArgument.uuid())
						.executes(context -> switchOff(context.getSource(), UuidArgument.getUuid(context, "worker")))))));
	}

	private static int listChunks(CommandSourceStack source) {
		MinecraftServer server = source.getServer();
		List<WorkerChunks.Listed> workers = WorkerChunks.list(server);
		if (workers.isEmpty()) {
			source.sendSuccess(() -> Component.translatable("commands.workingvillagers.chunks.none"), false);
			return 0;
		}
		int total = workers.stream().mapToInt(WorkerChunks.Listed::chunks).sum();
		source.sendSuccess(() -> Component.translatable("commands.workingvillagers.chunks.header", workers.size(), total), false);
		for (WorkerChunks.Listed listed : workers) {
			WorkerChunks.Entry entry = listed.entry();
			String off = "/" + WorkingVillagers.MOD_ID + " chunks off " + entry.worker();
			MutableComponent line = Component.translatable("commands.workingvillagers.chunks.entry",
				entry.name().isEmpty() ? entry.worker().toString() : entry.name(),
				playerName(server, entry.employer()),
				entry.dimension().identifier().toString(),
				entry.chunk().getMiddleBlockX(),
				entry.chunk().getMiddleBlockZ(),
				listed.state() == WorkerChunks.State.ACTIVE
					? Component.translatable("commands.workingvillagers.chunks.loaded", listed.chunks()).withStyle(ChatFormatting.GREEN)
					: listed.state().describe().copy().withStyle(ChatFormatting.GOLD));
			line.append(" ").append(Component.translatable("commands.workingvillagers.chunks.off_button").withStyle(style -> style
				.withColor(ChatFormatting.RED)
				.withClickEvent(new ClickEvent.SuggestCommand(off))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal(off)))));
			source.sendSuccess(() -> line, false);
		}
		return workers.size();
	}

	private static int switchOff(CommandSourceStack source, UUID worker) {
		if (!WorkerChunks.switchOff(source.getServer(), worker)) {
			source.sendFailure(Component.translatable("commands.workingvillagers.chunks.not_found", worker.toString()));
			return 0;
		}
		source.sendSuccess(() -> Component.translatable("commands.workingvillagers.chunks.switched_off", worker.toString()), true);
		return 1;
	}

	/** The name of the player, also while offline if the server knows it; the UUID otherwise. */
	private static String playerName(MinecraftServer server, UUID id) {
		ServerPlayer player = server.getPlayerList().getPlayer(id);
		if (player != null) {
			return player.getGameProfile().name();
		}
		return server.services().nameToIdCache().get(id).map(NameAndId::name).orElse(id.toString());
	}
}
