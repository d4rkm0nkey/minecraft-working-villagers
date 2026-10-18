package me.teajay.workingvillagers.neoforge;

import com.google.common.collect.ImmutableSet;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.platform.Platform;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.core.registries.Registries;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.DataMapHooks;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(WorkingVillagers.MOD_ID)
public class WorkingVillagersNeoForge {
	public WorkingVillagersNeoForge(IEventBus modBus) {
		NeoForgePlatform platform = new NeoForgePlatform();
		WorkingVillagers.init(platform);
		platform.registers.values().forEach(register -> register.register(modBus));
		modBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
			PayloadRegistrar registrar = event.registrar("1");
			platform.payloads.forEach(registration -> registration.accept(registrar));
		});

		modBus.addListener(BuildCreativeModeTabContentsEvent.class, event -> {
			if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
				event.accept(WorkingVillagers.CHOPPING_BLOCK_ITEM.get());
				event.accept(WorkingVillagers.WOODCUTTER_ITEM.get());
				event.accept(WorkingVillagers.CARPENTER_BENCH_ITEM.get());
				event.accept(WorkingVillagers.VILLAGE_CHEST_ITEM.get());
			} else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
				event.accept(WorkingVillagers.WORK_CONTRACT.get());
				event.accept(WorkingVillagers.WICKER_BASKET.get());
			}
		});
	}

	/**
	 * NeoForge registers through deferred registers during the registry events. NeoForge maps
	 * the block states of a registered PoiType to it by itself.
	 */
	private static final class NeoForgePlatform implements Platform {
		private final Map<ResourceKey<?>, DeferredRegister<?>> registers = new LinkedHashMap<>();
		/** Payloads are registered during an event, after WorkingVillagers.init has run. */
		private final List<Consumer<PayloadRegistrar>> payloads = new ArrayList<>();

		@Override
		public Path configDir() {
			return FMLPaths.CONFIGDIR.get();
		}

		@Override
		public Iterable<BlockTransformer.BlockTransformData> blockTransforms(Holder<BlockTransformer> transformer) {
			return DataMapHooks.getAllTransformers(transformer);
		}

		@Override
		public <T extends CustomPacketPayload> void registerServerbound(
			CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec, BiConsumer<T, ServerPlayer> handler
		) {
			this.payloads.add(registrar -> registrar.playToServer(type, codec, (payload, context) -> {
				if (context.player() instanceof ServerPlayer player) {
					handler.accept(payload, player);
				}
			}));
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> Supplier<T> register(ResourceKey<? extends Registry<T>> registry, String name, Supplier<? extends T> factory) {
			DeferredRegister<T> register = (DeferredRegister<T>) this.registers.computeIfAbsent(
				registry, key -> DeferredRegister.create(registry, WorkingVillagers.MOD_ID)
			);
			return register.register(name, factory);
		}

		@Override
		public Supplier<PoiType> registerPoi(String name, Supplier<Block> block, int maxTickets, int validRange) {
			return this.register(Registries.POINT_OF_INTEREST_TYPE, name, () -> new PoiType(
				ImmutableSet.copyOf(block.get().getStateDefinition().getPossibleStates()), maxTickets, validRange
			));
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenu(String name, MenuFactory<T> factory) {
			Supplier<MenuType<?>> registered = this.register(Registries.MENU, name, () -> new MenuType<>(factory::create, FeatureFlags.VANILLA_SET));
			return () -> (MenuType<T>) registered.get();
		}
	}
}
