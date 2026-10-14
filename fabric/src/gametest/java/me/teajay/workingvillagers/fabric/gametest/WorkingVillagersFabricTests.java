package me.teajay.workingvillagers.fabric.gametest;

import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.gametest.WorkerTests;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * The test mod: registers the test functions; the test instances that run them are data
 * ({@code data/workingvillagers/test_instance}).
 */
public class WorkingVillagersFabricTests implements ModInitializer {
	@Override
	public void onInitialize() {
		WorkerTests.register((name, test) -> Registry.register(BuiltInRegistries.TEST_FUNCTION, WorkingVillagers.id(name), test));
	}
}
