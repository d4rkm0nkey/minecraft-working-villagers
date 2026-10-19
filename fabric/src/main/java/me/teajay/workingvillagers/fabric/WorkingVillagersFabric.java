package me.teajay.workingvillagers.fabric;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.platform.Platform;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PoiHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;

public class WorkingVillagersFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		WorkingVillagers.init(new FabricPlatform());

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
			.register(output -> {
				output.accept(WorkingVillagers.CHOPPING_BLOCK_ITEM.get());
				output.accept(WorkingVillagers.WOODCUTTER_ITEM.get());
				output.accept(WorkingVillagers.CARPENTER_BENCH_ITEM.get());
				output.accept(WorkingVillagers.PIT_STAKE_ITEM.get());
				output.accept(WorkingVillagers.KILN_ITEM.get());
				output.accept(WorkingVillagers.TRADING_COUNTER_ITEM.get());
				output.accept(WorkingVillagers.MINING_BENCH_ITEM.get());
				output.accept(WorkingVillagers.VILLAGE_CHEST_ITEM.get());
			});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			output.accept(WorkingVillagers.WORK_CONTRACT.get());
			output.accept(WorkingVillagers.WICKER_BASKET.get());
		});
	}

	/**
	 * Fabric registers everything right away.
	 */
	private static final class FabricPlatform implements Platform {
		@Override
		public Path configDir() {
			return FabricLoader.getInstance().getConfigDir();
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> Supplier<T> register(ResourceKey<? extends Registry<T>> registry, String name, Supplier<? extends T> factory) {
			Registry<T> target = (Registry<T>) BuiltInRegistries.REGISTRY.getValueOrThrow((ResourceKey) registry);
			T value = Registry.register(target, WorkingVillagers.id(name), factory.get());
			return () -> value;
		}

		@Override
		public Supplier<PoiType> registerPoi(String name, Supplier<Block> block, int maxTickets, int validRange) {
			PoiType value = PoiHelper.register(WorkingVillagers.id(name), maxTickets, validRange, block.get());
			return () -> value;
		}

		@Override
		public <T extends CustomPacketPayload> void registerServerbound(
			CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec, BiConsumer<T, ServerPlayer> handler
		) {
			PayloadTypeRegistry.serverboundPlay().register(type, codec);
			ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> handler.accept(payload, context.player()));
		}

		@Override
		public <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenu(String name, MenuFactory<T> factory) {
			MenuType<T> value = Registry.register(BuiltInRegistries.MENU, WorkingVillagers.id(name), new MenuType<>(factory::create, FeatureFlags.VANILLA_SET));
			return () -> value;
		}
	}
}
