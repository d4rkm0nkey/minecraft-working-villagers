package me.teajay.workingvillagers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import me.teajay.workingvillagers.job.WorkerNotice;
import me.teajay.workingvillagers.network.WorkerNoticePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the sign of a worker (see {@link WorkerNotice}) above its head like a name tag: a red
 * "!" while material is missing, a green "✔" when an order is done. Above the name, if that
 * is shown. The server sends the signs ({@link WorkerNoticePayload}).
 */
public final class WorkerNoticeRenderer {
	/** How far the sign is seen, like the name of a villager. */
	private static final double DISTANCE = 32.0;
	private static final Component MISSING = Component.literal("!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
	private static final Component DONE = Component.literal("✔").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
	/** How long a sign stays without being sent again (the server sends it every two seconds). */
	private static final int EXPIRE_TICKS = 100;
	/** The signs the server sent, by entity id, with the client tick they came. */
	private static final Map<Integer, Received> SIGNS = new ConcurrentHashMap<>();

	private WorkerNoticeRenderer() {
	}

	private record Received(byte notice, int tick) {
	}

	/** A sign from the server. */
	public static void receive(WorkerNoticePayload payload) {
		Minecraft minecraft = Minecraft.getInstance();
		if (payload.notice() == WorkerNotice.NONE || minecraft.level == null) {
			SIGNS.remove(payload.entity());
		} else {
			SIGNS.put(payload.entity(), new Received(payload.notice(), minecraft.player == null ? 0 : minecraft.player.tickCount));
		}
	}

	/** Puts the villager's sign into its render state. */
	public static void extract(Villager villager, EntityRenderState state, float partialTicks) {
		NoticeHolder holder = (NoticeHolder) state;
		Received received = SIGNS.get(villager.getId());
		Minecraft minecraft = Minecraft.getInstance();
		if (received != null && (minecraft.player == null || minecraft.player.tickCount - received.tick() > EXPIRE_TICKS
			|| minecraft.player.tickCount < received.tick())) {
			SIGNS.remove(villager.getId(), received);
			received = null;
		}
		byte notice = received == null ? WorkerNotice.NONE : received.notice();
		if (notice == WorkerNotice.NONE || state.distanceToCameraSq > DISTANCE * DISTANCE) {
			holder.workingvillagers$setNotice(WorkerNotice.NONE, null);
			return;
		}
		holder.workingvillagers$setNotice(notice, villager.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, villager.getYRot(partialTicks)));
	}

	public static void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!(state instanceof NoticeHolder holder) || holder.workingvillagers$getNotice() == WorkerNotice.NONE) {
			return;
		}
		Component sign = holder.workingvillagers$getNotice() == WorkerNotice.DONE ? DONE : MISSING;
		// One line above the name tag, if there is one
		collector.submitNameTag(poseStack, holder.workingvillagers$getNoticeAttachment(), state.nameTag != null ? -10 : 0, sign, true, state.lightCoords, camera);
	}

	/** The sign in the render state of a villager (see {@code VillagerRenderStateMixin}). */
	public interface NoticeHolder {
		byte workingvillagers$getNotice();

		@Nullable Vec3 workingvillagers$getNoticeAttachment();

		void workingvillagers$setNotice(byte notice, @Nullable Vec3 attachment);
	}
}
