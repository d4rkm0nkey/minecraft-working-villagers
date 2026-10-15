package me.teajay.workingvillagers.station;

import com.mojang.math.Transformation;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import me.teajay.workingvillagers.job.WorkerJobs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * The paper notes on the front of the workstations: the name of the worker, or an empty
 * note if the station is free. They are vanilla text displays, so they need nothing on the
 * client. Notes are kept up to date for the stations near players. A vanilla station
 * (stonecutter, blast furnace) only gets a note once it has been set up as a workstation;
 * the composter has no front and gets none.
 */
public final class StationNotes {
	private static final String TAG = "workingvillagers_note";
	/** Tag prefix for the station position, e.g. {@code workingvillagers_note_at_1_-60_5}. */
	private static final String TAG_AT = "workingvillagers_note_at_";
	/** Tag prefix for what the note shows, to notice when it has to change. */
	private static final String TAG_CONTENT = "workingvillagers_note_content_";
	private static final int INTERVAL = 40;
	private static final int RANGE = 32;
	private static final int PAPER_COLOR = 0xFFEDE3C6;
	private static final int INK_COLOR = 0x3B2A1A;
	private static final float SCALE = 0.4F;
	/** The empty note is as wide as a short name. */
	private static final String BLANK = "        ";

	private StationNotes() {
	}

	public static void tick(ServerLevel level) {
		if (level.getGameTime() % INTERVAL != 0 || level.players().isEmpty()) {
			return;
		}
		Set<BlockPos> updated = new HashSet<>();
		for (ServerPlayer player : level.players()) {
			updateAround(level, player.blockPosition(), updated);
		}
	}

	/**
	 * Updates the notes of the stations around the position and removes the notes of
	 * stations that are gone. Stations in {@code updated} are skipped and added to it.
	 */
	public static void updateAround(ServerLevel level, BlockPos center, Set<BlockPos> updated) {
		level.getPoiManager()
			.getInRange(WorkerJobs::isJobSite, center, RANGE, PoiManager.Occupancy.ANY)
			.map(PoiRecord::getPos)
			.filter(updated::add)
			.forEach(pos -> update(level, pos));
		AABB around = new AABB(center).inflate(RANGE);
		for (Display.TextDisplay note : level.getEntitiesOfClass(Display.TextDisplay.class, around, entity -> entity.entityTags().contains(TAG))) {
			BlockPos at = stationOf(note);
			if (at == null || findStation(level, at).isEmpty()) {
				note.discard();
			}
		}
	}

	private static Optional<Workstations.Station> findStation(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		StationType type = StationType.of(state);
		if (type == null || !state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			return Optional.empty();
		}
		return type.isVanilla() ? Workstations.findSetUp(level, pos) : Workstations.find(level, pos);
	}

	private static void update(ServerLevel level, BlockPos pos) {
		Optional<Workstations.Station> station = findStation(level, pos);
		if (station.isEmpty()) {
			return;
		}
		Direction front = level.getBlockState(pos).getValue(BlockStateProperties.HORIZONTAL_FACING);
		Optional<Component> occupant = station.get().occupant(level);
		String text = occupant.map(Component::getString).orElse(BLANK);
		String content = TAG_CONTENT + Integer.toHexString((text + front).hashCode());

		List<Display.TextDisplay> notes = level.getEntitiesOfClass(
			Display.TextDisplay.class, new AABB(pos).inflate(1.0), entity -> entity.entityTags().contains(tagAt(pos))
		);
		if (notes.size() == 1 && notes.getFirst().entityTags().contains(content)) {
			return;
		}
		notes.forEach(Entity::discard);
		spawn(level, pos, station.get().type(), front, text, content);
	}

	private static void spawn(ServerLevel level, BlockPos pos, StationType type, Direction front, String text, String content) {
		RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
		CompoundTag tag = new CompoundTag();
		tag.putString("id", "minecraft:text_display");
		tag.put("text", ComponentSerialization.CODEC.encodeStart(ops, Component.literal(text).withColor(INK_COLOR)).getOrThrow());
		tag.putInt("background", PAPER_COLOR);
		tag.putInt("line_width", 80);
		tag.putString("billboard", "fixed");
		tag.put("transformation", Transformation.EXTENDED_CODEC.encodeStart(
			ops, new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(SCALE), new Quaternionf())
		).getOrThrow());
		ListTag tags = new ListTag();
		tags.add(StringTag.valueOf(TAG));
		tags.add(StringTag.valueOf(tagAt(pos)));
		tags.add(StringTag.valueOf(content));
		tag.put("Tags", tags);

		// The chopping block is narrower than a full block
		double offset = (type == StationType.CHOPPING_BLOCK ? 6.0 / 16.0 : 0.5) + 0.01;
		double x = pos.getX() + 0.5 + front.getStepX() * offset;
		double y = pos.getY() + 0.2;
		double z = pos.getZ() + 0.5 + front.getStepZ() * offset;
		Entity note = EntityType.loadEntityRecursive(tag, level, new EntitySpawnRequest(EntitySpawnReason.TRIGGERED, false), entity -> {
			entity.snapTo(x, y, z, front.toYRot(), 0.0F);
			return entity;
		});
		if (note != null) {
			level.tryAddFreshEntityWithPassengers(note);
		}
	}

	private static String tagAt(BlockPos pos) {
		return TAG_AT + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
	}

	private static @Nullable BlockPos stationOf(Entity note) {
		for (String tag : note.entityTags()) {
			if (tag.startsWith(TAG_AT)) {
				String[] parts = tag.substring(TAG_AT.length()).split("_");
				try {
					return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
				} catch (RuntimeException e) {
					return null;
				}
			}
		}
		return null;
	}
}
