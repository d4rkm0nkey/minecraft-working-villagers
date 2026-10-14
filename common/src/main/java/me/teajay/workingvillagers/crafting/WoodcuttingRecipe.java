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
 * A recipe of the woodcutter: one wooden item in, a fixed result out. Works like the
 * stonecutter and is defined in data packs as {@code workingvillagers:woodcutting}.
 */
public class WoodcuttingRecipe extends SingleItemRecipe {
	public static final MapCodec<WoodcuttingRecipe> MAP_CODEC = simpleMapCodec(WoodcuttingRecipe::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, WoodcuttingRecipe> STREAM_CODEC = simpleStreamCodec(WoodcuttingRecipe::new);

	public WoodcuttingRecipe(CommonInfo commonInfo, Ingredient ingredient, ItemStackTemplate result) {
		super(commonInfo, ingredient, result);
	}

	@Override
	public RecipeType<WoodcuttingRecipe> getType() {
		return WorkingVillagers.WOODCUTTING.get();
	}

	@Override
	public RecipeSerializer<WoodcuttingRecipe> getSerializer() {
		return WorkingVillagers.WOODCUTTING_SERIALIZER.get();
	}

	@Override
	public String group() {
		return "";
	}

	/** The result as shown in the woodcutter, without needing an input. */
	public ItemStack resultPreview() {
		return this.result().create();
	}

	@Override
	public List<RecipeDisplay> display() {
		return List.of(new StonecutterRecipeDisplay(
			this.input().display(),
			new SlotDisplay.ItemStackSlotDisplay(this.result()),
			new SlotDisplay.ItemSlotDisplay(WorkingVillagers.WOODCUTTER_ITEM.get())
		));
	}

	@Override
	public RecipeBookCategory recipeBookCategory() {
		// Like the stonecutter, the woodcutter has no recipe book
		return RecipeBookCategories.STONECUTTER;
	}
}
