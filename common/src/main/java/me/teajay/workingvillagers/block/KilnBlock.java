package me.teajay.workingvillagers.block;

import me.teajay.workingvillagers.WorkingVillagers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The kiln, a brick furnace for building materials: glass, stone, bricks, terracotta,
 * charcoal (see {@link KilnBlockEntity}). Players use it like a furnace; it is the
 * workstation of the kiln operator, its overview opens with the "i" button in its screen.
 */
public class KilnBlock extends AbstractFurnaceBlock {
	public KilnBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new KilnBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel serverLevel
			? createTickerHelper(type, WorkingVillagers.KILN_BLOCK_ENTITY.get(),
				(innerLevel, pos, innerState, kiln) -> KilnBlockEntity.serverTick(serverLevel, pos, innerState, kiln))
			: null;
	}

	@Override
	protected void openContainer(Level level, BlockPos pos, Player player) {
		if (level.getBlockEntity(pos) instanceof KilnBlockEntity kiln) {
			player.openMenu((MenuProvider) kiln);
		}
	}

	/** Like a furnace: crackling and smoke out of the front while it burns. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) {
			return;
		}
		double x = pos.getX() + 0.5;
		double y = pos.getY();
		double z = pos.getZ() + 0.5;
		if (random.nextDouble() < 0.1) {
			level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
		}
		Direction direction = state.getValue(FACING);
		Direction.Axis axis = direction.getAxis();
		double side = random.nextDouble() * 0.6 - 0.3;
		double dx = axis == Direction.Axis.X ? direction.getStepX() * 0.52 : side;
		double dy = random.nextDouble() * 6.0 / 16.0;
		double dz = axis == Direction.Axis.Z ? direction.getStepZ() * 0.52 : side;
		level.addParticle(ParticleTypes.SMOKE, x + dx, y + dy, z + dz, 0.0, 0.0, 0.0);
		level.addParticle(ParticleTypes.FLAME, x + dx, y + dy, z + dz, 0.0, 0.0, 0.0);
		// The chimney on top
		if (random.nextDouble() < 0.3) {
			level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 1.1, z, 0.0, 0.03, 0.0);
		}
	}
}
