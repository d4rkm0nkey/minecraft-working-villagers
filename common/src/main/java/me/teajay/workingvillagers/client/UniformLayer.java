package me.teajay.workingvillagers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import me.teajay.workingvillagers.WorkingVillagers;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;

/**
 * Villagers show no armor. A guard in a leather chestplate gets its tabard in the color of the
 * chestplate instead: the uniform of the village, dyed by the tanner.
 */
public class UniformLayer extends RenderLayer<VillagerRenderState, VillagerModel> {
	/** The tabard of the guard, in grey, tinted with the color. */
	private static final Identifier TEXTURE = WorkingVillagers.id("textures/entity/villager/guard_tabard.png");

	public UniformLayer(RenderLayerParent<VillagerRenderState, VillagerModel> renderer) {
		super(renderer);
	}

	/** The color of the uniform the villager wears, 0 for none. */
	public static int colorOf(Villager villager) {
		if (!villager.getVillagerData().profession().is(WorkingVillagers.GUARD_PROFESSION)) {
			return 0;
		}
		ItemStack chest = villager.getItemBySlot(EquipmentSlot.CHEST);
		if (!chest.is(Items.LEATHER_CHESTPLATE)) {
			return 0;
		}
		DyedItemColor color = chest.get(DataComponents.DYED_COLOR);
		return 0xFF000000 | (color == null ? DyedItemColor.LEATHER_COLOR : color.rgb());
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, VillagerRenderState state, float yRot, float xRot) {
		int color = ((UniformHolder) state).workingvillagers$getUniform();
		if (color == 0 || state.isBaby || state.isInvisible) {
			return;
		}
		// After the clothes of the profession (orders 1 to 3, see VillagerProfessionLayer)
		renderColoredCutoutModel(this.getParentModel(), TEXTURE, poseStack, submitNodeCollector, lightCoords, state, color, 4);
	}
}
