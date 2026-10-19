package me.teajay.workingvillagers.carrier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import me.teajay.workingvillagers.Config;
import me.teajay.workingvillagers.station.ContainerNames;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A transport route of a carrier: everything from the source container goes to the target.
 * The name is optional, without one the route is called by its number.
 */
public record Route(BlockPos source, BlockPos target, String name) {
	public static final int MAX_ROUTES = 3;
	/** Maximum distance between the source and the target of a route. */
	/** Maximum distance between the sources of two routes of the same carrier. */
	public static final int MAX_NAME_LENGTH = 24;

	public static final Codec<Route> CODEC = RecordCodecBuilder.create(i -> i.group(
		BlockPos.CODEC.fieldOf("source").forGetter(Route::source),
		BlockPos.CODEC.fieldOf("target").forGetter(Route::target),
		Codec.STRING.optionalFieldOf("name", "").forGetter(Route::name)
	).apply(i, Route::new));
	public static final Codec<List<Route>> LIST_CODEC = CODEC.listOf();

	public Route(BlockPos source, BlockPos target) {
		this(source, target, "");
	}

	public Route withName(String name) {
		return new Route(this.source, this.target, cleanName(name));
	}

	/** Trims the name and cuts it to the maximum length. */
	public static String cleanName(String name) {
		String trimmed = name.strip();
		return trimmed.length() > MAX_NAME_LENGTH ? trimmed.substring(0, MAX_NAME_LENGTH) : trimmed;
	}

	/** Whether the route connects the same two containers. */
	public boolean sameChests(Route other) {
		return this.source.equals(other.source) && this.target.equals(other.target);
	}

	/**
	 * Why the route cannot be added to the others, or null if it can.
	 */
	public static @Nullable Component checkNew(Level level, List<Route> routes, Route route) {
		if (routes.size() >= MAX_ROUTES) {
			return Component.translatable("message.workingvillagers.route.full", MAX_ROUTES);
		}
		if (routes.stream().anyMatch(route::sameChests)) {
			return Component.translatable("message.workingvillagers.route.exists", route.describe(level));
		}
		if (!route.source.closerThan(route.target, Config.ROUTE_LENGTH.get() + 0.5)) {
			return Component.translatable("message.workingvillagers.route.too_long", Config.ROUTE_LENGTH.get());
		}
		for (Route other : routes) {
			if (!other.source.closerThan(route.source, Config.ROUTE_SOURCE_SPREAD.get() + 0.5)) {
				return Component.translatable("message.workingvillagers.route.sources_too_far", Config.ROUTE_SOURCE_SPREAD.get());
			}
		}
		return null;
	}

	/** The name, or "Route n" for a route without one. */
	public Component title(int index) {
		return this.name.isEmpty() ? Component.translatable("gui.workingvillagers.carrier.route", index + 1) : Component.literal(this.name);
	}

	/** "Source → Target", by the names of the containers. */
	public Component describe(Level level) {
		return Component.empty().append(ContainerNames.name(level, this.source)).append(" → ").append(ContainerNames.name(level, this.target));
	}
}
