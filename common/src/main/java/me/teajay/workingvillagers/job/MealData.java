package me.teajay.workingvillagers.job;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * What a worker has for its meals (see {@link Meals}): the emeralds in its purse, how hungry
 * it is, the food in its lunch bag and what it ate last. Kept apart from its pockets, so the
 * work never hands in its food or pays a wage from its purse.
 */
public class MealData {
	/** Stacks of food the lunch bag holds. */
	public static final int BAG_SLOTS = 4;

	public static final Codec<MealData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.optionalFieldOf("purse", 0).forGetter(data -> data.purse),
		Codec.INT.optionalFieldOf("upkeep_owed", 0).forGetter(data -> data.upkeepOwed),
		Codec.FLOAT.optionalFieldOf("hunger", 0.0F).forGetter(data -> data.hunger),
		Codec.STRING.listOf().optionalFieldOf("recent", List.of()).forGetter(data -> data.recent),
		ItemStack.CODEC.listOf().optionalFieldOf("bag", List.of()).forGetter(MealData::contents)
	).apply(i, MealData::new));

	private int purse;
	/** Hundredths of an emerald of upkeep not spent yet: a wage of 1 at 50 % spends an emerald every other day. */
	private int upkeepOwed;
	/** Food points the worker misses: 0 is full, {@link Meals#dailyFood} is hungry. */
	private float hunger;
	/** The kinds of food ({@link Meals#kind}) of its last meals, the latest last. */
	private final List<String> recent;
	private final SimpleContainer bag = new SimpleContainer(BAG_SLOTS);

	public MealData() {
		this(0, 0, 0.0F, List.of(), List.of());
	}

	private MealData(int purse, int upkeepOwed, float hunger, List<String> recent, List<ItemStack> contents) {
		this.purse = purse;
		this.upkeepOwed = upkeepOwed;
		this.hunger = hunger;
		this.recent = new ArrayList<>(recent);
		contents.forEach(this.bag::addItem);
	}

	public int purse() {
		return this.purse;
	}

	public void setPurse(int purse) {
		this.purse = Math.max(0, purse);
	}

	int upkeepOwed() {
		return this.upkeepOwed;
	}

	void setUpkeepOwed(int upkeepOwed) {
		this.upkeepOwed = upkeepOwed;
	}

	public float hunger() {
		return this.hunger;
	}

	public void setHunger(float hunger) {
		this.hunger = Math.max(0.0F, hunger);
	}

	public List<String> recent() {
		return this.recent;
	}

	public SimpleContainer bag() {
		return this.bag;
	}

	/** Whether there is nothing worth saving: a worker that was never paid and never ate. */
	public boolean isEmpty() {
		return this.purse == 0 && this.upkeepOwed == 0 && this.hunger == 0.0F && this.recent.isEmpty() && this.bag.isEmpty();
	}

	private List<ItemStack> contents() {
		List<ItemStack> contents = new ArrayList<>();
		for (ItemStack stack : this.bag) {
			if (!stack.isEmpty()) {
				contents.add(stack.copy());
			}
		}
		return contents;
	}
}
