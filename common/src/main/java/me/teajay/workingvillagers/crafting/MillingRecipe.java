package me.teajay.workingvillagers.crafting;

import com.mojang.serialization.MapCodec;
import java.util.List;
import me.teajay.workingvillagers.WorkingVillagers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;

/**
 * A recipe of the mill of the mining bench: one block of stone or gravel in, a fixed result
 * out (cobblestone to gravel, gravel to sand, ...). Works like the stonecutter and is defined
 * in data packs as {@code workingvillagers:milling}.
 */
public class MillingRecipe extends SingleItemRecipe {
	public static final MapCodec<MillingRecipe> MAP_CODEC = simpleMapCodec(MillingRecipe::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, MillingRecipe> STREAM_CODEC = simpleStreamCodec(MillingRecipe::new);

	public MillingRecipe(CommonInfo commonInfo, Ingredient ingredient, ItemStackTemplate result) {
		super(commonInfo, ingredient, result);
	}

	@Override
	public RecipeType<MillingRecipe> getType() {
		return WorkingVillagers.MILLING.get();
	}

	@Override
	public RecipeSerializer<MillingRecipe> getSerializer() {
		return WorkingVillagers.MILLING_SERIALIZER.get();
	}

	@Override
	public String group() {
		return "";
	}

	/** The result as shown in the mill, without needing an input. */
	public ItemStack resultPreview() {
		return this.result().create();
	}

	@Override
	public List<RecipeDisplay> display() {
		return List.of(new StonecutterRecipeDisplay(
			this.input().display(),
			new SlotDisplay.ItemStackSlotDisplay(this.result()),
			new SlotDisplay.ItemSlotDisplay(WorkingVillagers.MINING_BENCH_ITEM.get())
		));
	}

	@Override
	public RecipeBookCategory recipeBookCategory() {
		// Like the stonecutter, the mill has no recipe book
		return RecipeBookCategories.STONECUTTER;
	}
}
