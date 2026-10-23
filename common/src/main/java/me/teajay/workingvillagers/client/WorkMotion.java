package me.teajay.workingvillagers.client;

import me.teajay.workingvillagers.WorkingVillagers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Holder;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * How a villager moves its crossed arms while it swings at work. Vanilla villagers ignore the
 * swing: the jobs swing on every stroke, and these motions make the strokes seen.
 * <p>
 * The arms of a villager are one part, so both arms move together, as if the villager held the
 * tool in both hands. The held item follows the arms (see {@code CrossedArmsItemLayer}).
 * <p>
 * The jobs play the sound of a blow as they start the swing, so the blows land early in it.
 */
public enum WorkMotion {
	/** No swing to show, such as for wandering traders. */
	NONE,
	/** Lifts the axe, pickaxe, shovel or hoe over the head and brings it down on the block it looks at. */
	STRIKE,
	/** Short blows of the smiths on their workstation. */
	HAMMER,
	/** Low snips with the shears. */
	SHEAR,
	/** Raises and draws the bow, then lets go. */
	DRAW,
	/** Swings the fishing rod back and casts it forward. */
	CAST,
	/** Reaches forward and works with the hands: crafting, filling furnaces, handing over goods. */
	HANDLE;

	public static WorkMotion of(Villager villager) {
		ItemStack held = villager.getMainHandItem();
		if (held.is(ItemTags.AXES) || held.is(ItemTags.PICKAXES) || held.is(ItemTags.SHOVELS) || held.is(ItemTags.HOES)) {
			return STRIKE;
		}
		if (held.is(Items.SHEARS)) {
			return SHEAR;
		}
		if (held.is(Items.BOW) || held.is(Items.CROSSBOW)) {
			return DRAW;
		}
		if (held.is(Items.FISHING_ROD)) {
			return CAST;
		}
		Holder<VillagerProfession> profession = villager.getVillagerData().profession();
		if (profession.is(WorkingVillagers.SMITH_PROFESSION) || profession.is(WorkingVillagers.WEAPONSMITH_PROFESSION)) {
			return HAMMER;
		}
		return HANDLE;
	}

	/**
	 * Moves the arms and the head away from their pose at rest.
	 *
	 * @param swing the progress of the swing, from 0 to 1, 0 when the villager does not swing
	 * @param pitch where the head looks, in radians, down positive
	 */
	public void apply(ModelPart head, ModelPart arms, float swing, float pitch) {
		if (swing <= 0.0F) {
			return;
		}
		float bump = Mth.sin(swing * Mth.PI);
		switch (this) {
			case NONE -> {
			}
			case STRIKE -> {
				// Lower when it digs down in a pit than when it digs ahead in a tunnel
				float down = Mth.clamp(0.2F + pitch * 0.8F, -0.4F, 0.9F);
				arms.xRot += keys(swing, 0.0F, 0.0F, 0.2F, -1.7F, 0.3F, down, 1.0F, 0.0F);
				head.xRot += keys(swing, 0.0F, 0.0F, 0.2F, -0.15F, 0.3F, 0.25F, 1.0F, 0.0F);
			}
			case HAMMER -> {
				arms.xRot += keys(swing, 0.0F, 0.0F, 0.2F, -0.9F, 0.3F, 0.3F, 1.0F, 0.0F);
				head.xRot += 0.2F * bump;
			}
			case SHEAR -> {
				arms.xRot += 0.25F * bump;
				arms.z -= 1.5F * bump;
				arms.zRot += 0.12F * Mth.sin(swing * 6.0F * Mth.PI) * bump;
				head.xRot += 0.2F * bump;
			}
			case DRAW -> {
				float raised = keys(swing, 0.0F, 0.0F, 0.25F, 1.0F, 0.6F, 1.0F, 1.0F, 0.0F);
				arms.xRot -= 0.85F * raised;
				arms.z += 1.0F * raised;
				head.xRot -= 0.1F * raised;
			}
			case CAST -> {
				arms.xRot += keys(swing, 0.0F, 0.0F, 0.3F, -1.4F, 0.45F, -0.6F, 1.0F, 0.0F);
			}
			case HANDLE -> {
				arms.xRot -= 0.35F * bump;
				arms.z -= 2.0F * bump;
				head.xRot += 0.25F * bump;
			}
		}
	}

	/**
	 * The value at the time, eased between key frames.
	 *
	 * @param frames pairs of time and value, in the order of time
	 */
	private static float keys(float time, float... frames) {
		for (int i = 2; i < frames.length; i += 2) {
			if (time <= frames[i]) {
				float from = frames[i - 2];
				float progress = (time - from) / (frames[i] - from);
				float eased = progress * progress * (3.0F - 2.0F * progress);
				return Mth.lerp(eased, frames[i - 1], frames[i + 1]);
			}
		}
		return frames[frames.length - 1];
	}
}
