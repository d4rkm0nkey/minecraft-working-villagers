package me.teajay.workingvillagers.fabric.gametest;

import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.gametest.WorkerTests;
import me.teajay.workingvillagers.gametest.simulation.Simulation;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * The test mod: registers the test functions; the test instances that run them are data
 * ({@code data/workingvillagers/test_instance}). Also adds the village simulation
 * ({@code /wvsim}, see {@link Simulation}).
 */
public class WorkingVillagersFabricTests implements ModInitializer {
	@Override
	public void onInitialize() {
		WorkerTests.register((name, test) -> Registry.register(BuiltInRegistries.TEST_FUNCTION, WorkingVillagers.id(name), test));
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> Simulation.registerCommands(dispatcher));
		ServerLifecycleEvents.SERVER_STARTED.register(Simulation::serverStarted);
		ServerTickEvents.START_SERVER_TICK.register(Simulation::tickStart);
		ServerTickEvents.END_SERVER_TICK.register(Simulation::tickEnd);
		ServerLivingEntityEvents.AFTER_DEATH.register(Simulation::died);
		ServerLivingEntityEvents.MOB_CONVERSION.register((from, into, context) -> Simulation.converted(from, into));
	}
}
