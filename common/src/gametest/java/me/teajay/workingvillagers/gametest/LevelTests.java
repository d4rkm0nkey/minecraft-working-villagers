package me.teajay.workingvillagers.gametest;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.job.HiredVillager;
import me.teajay.workingvillagers.job.WorkerJobs;
import me.teajay.workingvillagers.job.WorkerLevels;
import me.teajay.workingvillagers.station.Workstations.ChestRole;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The levels of the workers: rising by work, a master nearby, what a level costs and unlocks. */
final class LevelTests {
	private static final BlockPos WORKER = new BlockPos(6, 1, 6);
	private static final BlockPos STATION = new BlockPos(8, 1, 8);
	private static final BlockPos INPUT = new BlockPos(10, 1, 6);
	private static final BlockPos OUTPUT = new BlockPos(12, 1, 6);
	private static final BlockPos OTHER_STATION = new BlockPos(4, 1, 12);

	private LevelTests() {
	}

	static void register(BiConsumer<String, Consumer<GameTestHelper>> registry) {
		registry.accept("worker_rises_a_level", LevelTests::workerRisesALevel);
		registry.accept("worker_learns_by_working", LevelTests::workerLearnsByWorking);
		registry.accept("waiting_teaches_nothing", LevelTests::waitingTeachesNothing);
		registry.accept("master_teaches_faster", LevelTests::masterTeachesFaster);
		registry.accept("novice_sawyer_waits_for_doors", LevelTests::noviceSawyerWaitsForDoors);
		registry.accept("level_sets_wage_and_area", LevelTests::levelSetsWageAndArea);
		registry.accept("other_profession_starts_over", LevelTests::otherProfessionStartsOver);
	}

	/** A sawyer at the woodcutter with planks to cut. */
	private static Villager sawyer(GameTestHelper helper, String shape, ItemStack... input) {
		helper.setBlock(STATION, WorkingVillagers.WOODCUTTER_BLOCK.get());
		TestWorld.station(helper, STATION).setWood("oak", true);
		TestWorld.station(helper, STATION).setShape(shape);
		TestWorld.chest(helper, INPUT, input);
		TestWorld.chest(helper, OUTPUT);
		TestWorld.link(helper, STATION, INPUT, ChestRole.INPUT);
		TestWorld.link(helper, STATION, OUTPUT, ChestRole.OUTPUT);
		Villager villager = TestWorld.hireVillager(helper, WORKER);
		TestWorld.assign(helper, villager, STATION);
		TestWorld.give(helper, villager, new ItemStack(Items.EMERALD, 3));
		return villager;
	}

	/** At 10 experience a novice becomes an apprentice, like a vanilla villager after trading. */
	private static void workerRisesALevel(GameTestHelper helper) {
		Villager worker = sawyer(helper, "planks");
		worker.setVillagerXp(9);
		WorkerLevels.addExperience(helper.getLevel(), worker, 1);
		helper.assertTrue(worker.getVillagerData().level() == WorkerLevels.APPRENTICE, "Not an apprentice with 10 experience: " + worker.getVillagerData().level());
		WorkerLevels.addExperience(helper.getLevel(), worker, 60);
		helper.assertTrue(worker.getVillagerData().level() == WorkerLevels.JOURNEYMAN, "Not a journeyman with 70 experience: " + worker.getVillagerData().level());
		helper.succeed();
	}

	/** A sawyer that saws for more than 20 seconds gets experience. */
	private static void workerLearnsByWorking(GameTestHelper helper) {
		Villager worker = sawyer(helper, "planks", new ItemStack(Items.OAK_LOG, 64), new ItemStack(Items.OAK_LOG, 64));
		helper.succeedWhen(() -> helper.assertTrue(worker.getVillagerXp() > 0, "No experience yet: " + TestWorld.describe(worker)));
	}

	/** A sawyer without material waits and learns nothing. */
	private static void waitingTeachesNothing(GameTestHelper helper) {
		Villager worker = sawyer(helper, "planks");
		helper.runAfterDelay(600, () -> {
			helper.assertTrue(worker.getVillagerXp() == 0, "Learned while waiting: " + worker.getVillagerXp() + ", " + TestWorld.describe(worker));
			helper.assertTrue(((HiredVillager) worker).workingvillagers$getLearned() == 0.0F, "Learned while waiting: " + ((HiredVillager) worker).workingvillagers$getLearned());
			helper.succeed();
		});
	}

	/** Next to a master sawyer a novice learns 1.5 times as fast, without one at the normal pace. */
	private static void masterTeachesFaster(GameTestHelper helper) {
		Villager novice = sawyer(helper, "planks");
		helper.assertTrue(WorkerLevels.learningFactor(helper.getLevel(), novice) == 1.0F, "Learns faster without a master");
		helper.setBlock(OTHER_STATION, WorkingVillagers.WOODCUTTER_BLOCK.get());
		Villager master = TestWorld.hireVillager(helper, OTHER_STATION.east());
		TestWorld.assign(helper, master, OTHER_STATION);
		TestWorld.setLevel(master, WorkerLevels.MASTER);
		helper.assertTrue(WorkerLevels.learningFactor(helper.getLevel(), novice) == 1.5F, "Learns at " + WorkerLevels.learningFactor(helper.getLevel(), novice) + " next to a master");
		// The master itself learns nothing more
		helper.assertTrue(WorkerLevels.learningFactor(helper.getLevel(), master) == 1.0F, "The master learns faster");
		helper.succeed();
	}

	/** Doors come from the apprentice: a novice waits and says so, an apprentice saws them. */
	private static void noviceSawyerWaitsForDoors(GameTestHelper helper) {
		Villager worker = sawyer(helper, "door", new ItemStack(Items.OAK_LOG, 8));
		helper.runAfterDelay(200, () -> {
			Component task = ((HiredVillager) worker).workingvillagers$getTask();
			boolean locked = task != null && task.getContents() instanceof TranslatableContents contents && contents.getKey().equals("status.workingvillagers.locked");
			helper.assertTrue(locked, "The novice does not wait for its level: " + TestWorld.describe(worker));
			helper.assertTrue(TestWorld.count(helper, OUTPUT, stack -> stack.is(Items.OAK_DOOR)) == 0, "The novice sawed doors");
			TestWorld.setLevel(worker, WorkerLevels.APPRENTICE);
		});
		helper.succeedWhen(() -> TestWorld.assertHolds(helper, worker, OUTPUT, stack -> stack.is(Items.OAK_DOOR), 1, "oak doors in the output chest"));
	}

	/** The wage grows from 1 to 3 emeralds, the work area from the novice's share to the full radius. */
	private static void levelSetsWageAndArea(GameTestHelper helper) {
		int[] wages = {1, 1, 2, 2, 3};
		for (int level = WorkerLevels.NOVICE; level <= WorkerLevels.MASTER; level++) {
			helper.assertTrue(WorkerLevels.dailyWage(level) == wages[level - 1], "Wage of level " + level + ": " + WorkerLevels.dailyWage(level));
		}
		helper.assertTrue(WorkerLevels.area(WorkerLevels.MASTER, 24) == 24, "The master's area is not the full radius");
		helper.assertTrue(WorkerLevels.area(WorkerLevels.NOVICE, 24) <= WorkerLevels.area(WorkerLevels.JOURNEYMAN, 24), "The area shrinks with the level");
		helper.succeed();
	}

	/** A journeyman sawyer that becomes a lumberjack starts again as a novice. */
	private static void otherProfessionStartsOver(GameTestHelper helper) {
		Villager worker = sawyer(helper, "planks");
		TestWorld.setLevel(worker, WorkerLevels.JOURNEYMAN);
		worker.setVillagerXp(80);
		WorkerJobs.unassign(helper.getLevel(), worker);
		helper.setBlock(OTHER_STATION, WorkingVillagers.CHOPPING_BLOCK.get());
		TestWorld.assign(helper, worker, OTHER_STATION);
		helper.assertTrue(worker.getVillagerData().profession().is(WorkingVillagers.LUMBERJACK_PROFESSION), "Not a lumberjack: " + TestWorld.describe(worker));
		helper.assertTrue(worker.getVillagerData().level() == WorkerLevels.NOVICE && worker.getVillagerXp() == 0,
			"Kept its level in another profession: " + worker.getVillagerData().level() + ", " + worker.getVillagerXp());
		helper.succeed();
	}
}
