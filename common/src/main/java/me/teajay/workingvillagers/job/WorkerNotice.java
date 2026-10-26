package me.teajay.workingvillagers.job;

import java.util.Set;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.network.WorkerNoticePayload;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;

/**
 * The sign above a worker's head (drawn by {@code WorkerNoticeRenderer}): a "!" while it
 * waits for material it finds in none of its chests, a "✔" for a few seconds after it handed
 * in an order that is done (see {@link Orders#finish}). The players nearby get it when it
 * changes and again every {@link #RESEND_TICKS} while there is one, so players coming closer
 * see it too, and a sign whose end they missed goes away on its own.
 */
public final class WorkerNotice {
	public static final byte NONE = 0;
	public static final byte MISSING = 1;
	public static final byte DONE = 2;
	/** How long the "✔" stays. */
	private static final int DONE_TICKS = 100;
	/** How often the sign is sent again while there is one; the client drops it after {@code WorkerNoticeRenderer.EXPIRE_TICKS}. */
	private static final int RESEND_TICKS = 40;
	/** How far from the worker players get its sign. */
	private static final double SEND_DISTANCE = 48.0;
	/** The statuses (see {@link WorkerStatus}) that mean material is missing. */
	private static final Set<String> MISSING_MATERIAL = Set.of(
		"saw.no_material", "carpenter.waiting_for", "carpenter.missing", "kiln.no_ore", "kiln.no_fuel", "smith.no_ore", "smith.no_fuel",
		"fletcher.no_material", "apothecary.no_material", "apothecary.no_fuel", "tanner.no_material", "weaponsmith.no_material",
		"librarian.no_bottles", "librarian.no_lapis"
	);

	private WorkerNotice() {
	}

	/** The worker shows for a while that an order is done. */
	public static void done(Villager villager) {
		((HiredVillager) villager).workingvillagers$setDoneUntil(villager.tickCount + DONE_TICKS);
	}

	/** Called every tick for hired workers: updates the sign from what the worker is doing and sends it. */
	static void tick(ServerLevel level, Villager villager) {
		HiredVillager hired = (HiredVillager) villager;
		byte notice = NONE;
		if (villager.tickCount < hired.workingvillagers$getDoneUntil()) {
			notice = DONE;
		} else if (villager.getBrain().isActive(Activity.WORK) && missesMaterial(hired.workingvillagers$getTask())) {
			notice = MISSING;
		}
		boolean changed = hired.workingvillagers$getNotice() != notice;
		if (changed) {
			hired.workingvillagers$setNotice(notice);
		}
		if (changed || notice != NONE && villager.tickCount % RESEND_TICKS == 0) {
			WorkerNoticePayload payload = new WorkerNoticePayload(villager.getId(), notice);
			for (ServerPlayer player : level.players()) {
				if (player.distanceToSqr(villager) < SEND_DISTANCE * SEND_DISTANCE) {
					WorkingVillagers.platform().sendToClient(player, payload);
				}
			}
		}
	}

	private static boolean missesMaterial(Component task) {
		if (task == null || !(task.getContents() instanceof TranslatableContents contents)) {
			return false;
		}
		String key = contents.getKey();
		return key.startsWith(WorkerStatus.PREFIX) && MISSING_MATERIAL.contains(key.substring(WorkerStatus.PREFIX.length()));
	}
}
