package me.teajay.workingvillagers.entity;

import me.teajay.workingvillagers.WorkingVillagers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The bobber of a fisher, with the line to its rod (see {@code FisherBobberRenderer}). Only
 * for the looks: the fisher ({@code GoFishing}) casts it, moves it and takes it in again; the
 * catch does not depend on it. It flies in an arc to the water, floats there and dips when a
 * fish bites. It is not saved, and goes away by itself once its fisher no longer holds a rod.
 */
public class FisherBobber extends Entity {
	private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(FisherBobber.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> BITING = SynchedEntityData.defineId(FisherBobber.class, EntityDataSerializers.BOOLEAN);
	/** How long the cast flies. */
	private static final int FLIGHT_TICKS = 12;
	/** How far from its fisher the bobber may be before it goes away. */
	private static final double MAX_DISTANCE = 32.0;

	private Vec3 from = Vec3.ZERO;
	private Vec3 to = Vec3.ZERO;
	private int flight = FLIGHT_TICKS;

	public FisherBobber(EntityType<? extends FisherBobber> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	/** Casts a bobber from the fisher's rod to the water surface at {@code to}. */
	public static @Nullable FisherBobber cast(ServerLevel level, Villager fisher, Vec3 to) {
		FisherBobber bobber = WorkingVillagers.FISHER_BOBBER.get().create(level, EntitySpawnReason.TRIGGERED);
		if (bobber == null) {
			return null;
		}
		bobber.entityData.set(OWNER, fisher.getId());
		bobber.from = fisher.getEyePosition().add(fisher.getLookAngle().scale(0.8));
		bobber.to = to;
		bobber.flight = 0;
		bobber.snapTo(bobber.from.x, bobber.from.y, bobber.from.z, 0.0F, 0.0F);
		return level.addFreshEntity(bobber) ? bobber : null;
	}

	public @Nullable Villager getOwner() {
		return this.level().getEntity(this.entityData.get(OWNER)) instanceof Villager villager ? villager : null;
	}

	public boolean isBiting() {
		return this.entityData.get(BITING);
	}

	/** A fish bites: the bobber dips under the surface. */
	public void setBiting(boolean biting) {
		this.entityData.set(BITING, biting);
	}

	/** Whether the cast has landed on the water. */
	public boolean isFloating() {
		return this.flight >= FLIGHT_TICKS;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			return;
		}
		Villager owner = this.getOwner();
		if (owner == null || !owner.isAlive() || !owner.getMainHandItem().is(Items.FISHING_ROD) || owner.distanceTo(this) > MAX_DISTANCE) {
			this.discard();
			return;
		}
		if (this.flight < FLIGHT_TICKS) {
			this.flight++;
			float t = (float) this.flight / FLIGHT_TICKS;
			double arc = Math.sin(t * Mth.PI) * 1.5;
			this.setPos(Mth.lerp(t, this.from.x, this.to.x), Mth.lerp(t, this.from.y, this.to.y) + arc, Mth.lerp(t, this.from.z, this.to.z));
		}
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
		entityData.define(OWNER, 0);
		entityData.define(BITING, false);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE_ENTITY;
	}

	@Override
	public boolean isIgnoringBlockTriggers() {
		return true;
	}

	@Override
	public final boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}
}
