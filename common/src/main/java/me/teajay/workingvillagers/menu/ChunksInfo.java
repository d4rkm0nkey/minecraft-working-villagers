package me.teajay.workingvillagers.menu;

import java.util.Optional;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.job.HiredVillager;
import me.teajay.workingvillagers.job.WorkerChunks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;

/**
 * What the menus show about working without a player nearby (see {@link WorkerChunks}):
 * whether it is on, why the chunks are loaded or not, how many and what it costs.
 *
 * @param on whether the employer switched it on
 * @param canSwitch whether the player who has the menu open may switch it
 * @param state whether the chunks are loaded and if not, why (empty while it is off)
 * @param chunks how many chunks the worker keeps ticking
 * @param fee the emeralds per work day it costs on top of the wage
 */
public record ChunksInfo(boolean on, boolean canSwitch, Optional<Component> state, int chunks, int fee) {
	private static final String TAG = "Chunks";
	private static final String TAG_ON = "On";
	private static final String TAG_CAN_SWITCH = "CanSwitch";
	private static final String TAG_STATE = "State";
	private static final String TAG_COUNT = "Count";
	private static final String TAG_FEE = "Fee";

	/** Nothing is written if the server does not allow it or the villager is not hired. */
	static void write(ServerLevel level, Villager worker, Player player, CompoundTag info) {
		if (!Config.CHUNK_LOADING.get() || !((HiredVillager) worker).workingvillagers$isHired()) {
			return;
		}
		CompoundTag tag = new CompoundTag();
		tag.putBoolean(TAG_ON, WorkerChunks.isOn(level, worker));
		tag.putBoolean(TAG_CAN_SWITCH, WorkerChunks.canSwitch(worker, player));
		WorkerChunks.state(level, worker).ifPresent(state -> tag.store(TAG_STATE, ComponentSerialization.CODEC, state.describe()));
		tag.putInt(TAG_COUNT, WorkerChunks.chunkCount(level, worker));
		tag.putInt(TAG_FEE, Config.CHUNK_LOADING_FEE.get());
		info.put(TAG, tag);
	}

	/** Empty if the menu shows no hired worker or the server does not allow it. */
	static Optional<ChunksInfo> read(CompoundTag info) {
		return info.getCompound(TAG).map(tag -> new ChunksInfo(
			tag.getBooleanOr(TAG_ON, false),
			tag.getBooleanOr(TAG_CAN_SWITCH, false),
			tag.read(TAG_STATE, ComponentSerialization.CODEC),
			tag.getIntOr(TAG_COUNT, 0),
			tag.getIntOr(TAG_FEE, 0)
		));
	}
}
