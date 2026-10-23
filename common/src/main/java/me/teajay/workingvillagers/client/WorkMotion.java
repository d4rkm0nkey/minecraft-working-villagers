package me.teajay.workingvillagers.client;

import me.teajay.workingvillagers.WorkingVillagers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Holder;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Ease;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * How a worker holds its arms apart at work and moves them when it swings. Vanilla villagers
 * keep their arms crossed and ignore the swing; workers at work get two arms (see
 * {@code VillagerModelMixin}) and hold the item in the right hand, the way players do.
 * <p>
 * The jobs play the sound of a blow as they start the swing, so the blows land early in it.
 */
public enum WorkMotion {
	/** Arms crossed, as vanilla villagers: not at work. */
	NONE,
	/** Swings the axe, pickaxe, shovel or hoe like a player. */
	STRIKE,
	/** The smiths: blows with the right hand while the left one holds the piece. */
	HAMMER,
	/** Low snips with the shears. */
	SHEAR,
	/** Aims the bow with both arms. */
	DRAW,
	/** Swings the fishing rod back over the shoulder and casts it forward. */
	CAST,
	/** Both hands in front at work: crafting, filling furnaces, handing over goods. */
	HANDLE;

	/** How long a worker keeps its arms apart after a swing, so that they do not cross between two strokes. */
	private static final int APART_AFTER_SWING_TICKS = 60;

	public static WorkMotion of(Villager villager, int ticksSinceSwing) {
		Holder<VillagerProfession> profession = villager.getVillagerData().profession();
		if (villager.isBaby() || !profession.unwrapKey().map(key -> key.identifier().getNamespace().equals(WorkingVillagers.MOD_ID)).orElse(false)) {
			return NONE;
		}
		ItemStack held = villager.getMainHandItem();
		if (held.isEmpty() && ticksSinceSwing > APART_AFTER_SWING_TICKS) {
			return NONE;
		}
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
		if (profession.is(WorkingVillagers.SMITH_PROFESSION) || profession.is(WorkingVillagers.WEAPONSMITH_PROFESSION)) {
			return HAMMER;
		}
		return HANDLE;
	}

	/**
	 * Poses the arms apart, after vanilla posed the head and the legs.
	 *
	 * @param swing the progress of the swing, from 0 to 1, 0 when the villager does not swing
	 */
	public void pose(ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm, float swing, float walkPos, float walkSpeed) {
		// Swing with the steps, like players
		rightArm.xRot = Mth.cos(walkPos * 0.6662F + Mth.PI) * walkSpeed;
		leftArm.xRot = Mth.cos(walkPos * 0.6662F) * walkSpeed;
		float bump = swing > 0.0F ? Mth.sin(swing * Mth.PI) : 0.0F;
		switch (this) {
			case NONE -> {
			}
			case STRIKE -> {
				rightArm.xRot = rightArm.xRot * 0.5F - Mth.PI / 10.0F;
				whack(head, body, rightArm, leftArm, swing, 1.4F);
			}
			case HAMMER -> {
				rightArm.xRot = rightArm.xRot * 0.5F - Mth.PI / 10.0F;
				leftArm.xRot = -0.8F;
				leftArm.yRot = 0.3F;
				whack(head, body, rightArm, leftArm, swing, 1.2F);
				leftArm.xRot = -0.8F;
				head.xRot += 0.2F * bump;
			}
			case SHEAR -> {
				rightArm.xRot = -0.7F - 0.3F * bump;
				rightArm.yRot = -0.15F;
				rightArm.zRot = 0.1F * Mth.sin(swing * 6.0F * Mth.PI) * bump;
				leftArm.xRot = -0.6F;
				leftArm.yRot = 0.2F;
				head.xRot += 0.2F * bump;
			}
			case DRAW -> {
				rightArm.yRot = -0.1F + head.yRot;
				leftArm.yRot = 0.1F + head.yRot + 0.4F;
				rightArm.xRot = -Mth.HALF_PI + head.xRot + 0.25F * bump;
				leftArm.xRot = -Mth.HALF_PI + head.xRot;
			}
			case CAST -> {
				rightArm.xRot = rightArm.xRot * 0.5F - Mth.PI / 10.0F
					- keys(swing, 0.0F, 0.0F, 0.3F, 2.4F, 0.5F, 0.7F, 1.0F, 0.0F);
			}
			case HANDLE -> {
				// Working hands, one up while the other one is down
				float work = swing > 0.0F ? Mth.sin(swing * 2.0F * Mth.PI) : 0.0F;
				rightArm.xRot = -0.75F - 0.35F * work * bump;
				rightArm.yRot = -0.2F;
				leftArm.xRot = -0.75F + 0.35F * work * bump;
				leftArm.yRot = 0.2F;
				head.xRot += 0.2F * bump;
			}
		}
	}

	/** The swing of a player's arm, from {@code HumanoidModel.setupAttackAnimation}, the body turning with it. */
	private static void whack(ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm, float swing, float strength) {
		if (swing <= 0.0F) {
			return;
		}
		float shoulder = rightArm.getInitialPose().x();
		body.yRot = Mth.sin(Mth.sqrt(swing) * Mth.TWO_PI) * 0.2F;
		rightArm.z = Mth.sin(body.yRot) * -shoulder;
		rightArm.x = Mth.cos(body.yRot) * shoulder;
		leftArm.z = Mth.sin(body.yRot) * shoulder;
		leftArm.x = -Mth.cos(body.yRot) * shoulder;
		rightArm.yRot += body.yRot;
		leftArm.yRot += body.yRot;
		leftArm.xRot += body.yRot;
		float lift = Mth.sin(Ease.outQuart(swing) * Mth.PI);
		float aim = Mth.sin(swing * Mth.PI) * -(head.xRot - 0.7F) * 0.75F;
		rightArm.xRot -= lift * strength + aim;
		rightArm.yRot += body.yRot * 2.0F;
		rightArm.zRot += Mth.sin(swing * Mth.PI) * -0.4F;
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
