package me.teajay.workingvillagers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.block.VillageChestBlockEntity;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.MultiblockChestResources;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BrightnessCombiner;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the village chest like the vanilla chest renderer (the lid opens and closes slowly,
 * a double chest as two halves), with the textures {@code textures/entity/chest/village.png},
 * {@code village_left.png} and {@code village_right.png}.
 */
public class VillageChestRenderer implements BlockEntityRenderer<VillageChestBlockEntity, ChestRenderState> {
	/** In the chest atlas, which takes every texture in {@code textures/entity/chest}. */
	public static final MultiblockChestResources<SpriteId> TEXTURES = new MultiblockChestResources<>(
		WorkingVillagers.id("village"), WorkingVillagers.id("village_left"), WorkingVillagers.id("village_right")
	).map(Sheets.CHEST_MAPPER::apply);

	private final SpriteGetter sprites;
	private final MultiblockChestResources<ChestModel> models;

	public VillageChestRenderer(BlockEntityRendererProvider.Context context) {
		this.sprites = context.sprites();
		this.models = ChestRenderer.LAYERS.map(layer -> new ChestModel(context.bakeLayer(layer)));
	}

	@Override
	public ChestRenderState createRenderState() {
		return new ChestRenderState();
	}

	@Override
	public void extractRenderState(
		VillageChestBlockEntity chest, ChestRenderState state, float partialTicks, Vec3 cameraPosition,
		ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
	) {
		BlockEntityRenderer.super.extractRenderState(chest, state, partialTicks, cameraPosition, breakProgress);
		BlockState block = chest.getBlockState();
		state.facing = block.getValue(ChestBlock.FACING);
		state.type = block.getValue(ChestBlock.TYPE);
		if (chest.getLevel() == null || !(block.getBlock() instanceof ChestBlock chestBlock)) {
			state.type = ChestType.SINGLE;
			state.open = chest.getOpenNess(partialTicks);
			return;
		}
		// A double chest opens both halves together and is lit evenly
		DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> halves = chestBlock.combine(block, chest.getLevel(), chest.getBlockPos(), true);
		state.open = halves.apply(ChestBlock.opennessCombiner(chest)).get(partialTicks);
		if (state.type != ChestType.SINGLE) {
			state.lightCoords = halves.apply(new BrightnessCombiner<>()).applyAsInt(state.lightCoords);
		}
	}

	@Override
	public void submit(ChestRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.mulPose(ChestRenderer.modelTransformation(state.facing));
		// Eased like the vanilla lid
		float closed = 1.0F - state.open;
		float open = 1.0F - closed * closed * closed;
		ChestModel model = this.models.select(state.type);
		SpriteId texture = TEXTURES.select(state.type);
		collector.submitModel(model, open, poseStack, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, texture, this.sprites, 0);
		if (state.breakProgress != null) {
			collector.order(1).submitCrumblingOverlay(model, open, poseStack, texture.renderType(model.renderType()), state.lightCoords,
				OverlayTexture.NO_OVERLAY, -1, state.breakProgress);
		}
		poseStack.popPose();
	}
}
