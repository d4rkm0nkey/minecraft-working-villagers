package me.teajay.workingvillagers.neoforge.gametest;

import java.util.function.Consumer;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.gametest.WorkerTests;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The test mod: registers the test functions; the test instances that run them are data
 * ({@code data/workingvillagers/test_instance}).
 */
@Mod(WorkingVillagersNeoForgeTests.MOD_ID)
public class WorkingVillagersNeoForgeTests {
	public static final String MOD_ID = "workingvillagers_test";

	public WorkingVillagersNeoForgeTests(IEventBus modBus) {
		DeferredRegister<Consumer<GameTestHelper>> functions = DeferredRegister.create(Registries.TEST_FUNCTION, WorkingVillagers.MOD_ID);
		WorkerTests.register((name, test) -> functions.register(name, () -> test));
		functions.register(modBus);
	}
}
