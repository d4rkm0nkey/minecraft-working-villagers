package me.teajay.workingvillagers.carrier;

import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * Wraps the vanilla behavior that makes villagers without a job site unemployed again, so it
 * leaves carriers alone (they have no job site, the basket is their job).
 */
public class KeepCarrierJob implements BehaviorControl<Villager> {
	private final BehaviorControl<Villager> resetProfession;

	public KeepCarrierJob(BehaviorControl<Villager> resetProfession) {
		this.resetProfession = resetProfession;
	}

	@Override
	public Behavior.Status getStatus() {
		return this.resetProfession.getStatus();
	}

	@Override
	public Set<MemoryModuleType<?>> getRequiredMemories() {
		return this.resetProfession.getRequiredMemories();
	}

	@Override
	public boolean tryStart(ServerLevel level, Villager body, long timestamp) {
		return !Carriers.isCarrier(body) && this.resetProfession.tryStart(level, body, timestamp);
	}

	@Override
	public void tickOrStop(ServerLevel level, Villager body, long timestamp) {
		this.resetProfession.tickOrStop(level, body, timestamp);
	}

	@Override
	public void doStop(ServerLevel level, Villager body, long timestamp) {
		this.resetProfession.doStop(level, body, timestamp);
	}

	@Override
	public String debugString() {
		return this.resetProfession.debugString();
	}
}
