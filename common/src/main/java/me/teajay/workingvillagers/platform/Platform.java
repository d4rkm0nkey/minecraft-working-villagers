package me.teajay.workingvillagers.platform;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;

/**
 * The few things the common code needs from the mod loader.
 */
public interface Platform {
	/** The directory for config files. */
	Path configDir();

	/**
	 * Registers an object. The returned supplier may only be queried once the loader has
	 * registered it (immediately on Fabric, during the registry events on NeoForge).
	 */
	<T> Supplier<T> register(ResourceKey<? extends Registry<T>> registry, String name, Supplier<? extends T> factory);

	/**
	 * Registers a point of interest type matching every state of the given block and makes
	 * the game aware of the block states.
	 */
	Supplier<PoiType> registerPoi(String name, Supplier<Block> block, int maxTickets, int validRange);

	/**
	 * Registers a menu type that needs no extra data to be opened on the client.
	 * (The vanilla MenuType constructor is only accessible through the loaders.)
	 */
	<T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenu(String name, MenuFactory<T> factory);

	/**
	 * Registers a packet from the client to the server. The handler runs on the server thread.
	 */
	<T extends CustomPacketPayload> void registerServerbound(
		CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec, BiConsumer<T, ServerPlayer> handler
	);

	/**
	 * The block transforms of a tool (an axe stripping logs, ...), including the ones other
	 * mods add. NeoForge adds those from a data map when the tool is used.
	 */
	default Iterable<BlockTransformer.BlockTransformData> blockTransforms(Holder<BlockTransformer> transformer) {
		return transformer.value().transforms();
	}

	@FunctionalInterface
	interface MenuFactory<T extends AbstractContainerMenu> {
		T create(int containerId, Inventory inventory);
	}
}
