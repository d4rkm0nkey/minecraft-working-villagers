package me.teajay.workingvillagers.fabric;

import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.client.CarpenterScreen;
import me.teajay.workingvillagers.client.CarrierScreen;
import me.teajay.workingvillagers.client.CutterScreen;
import me.teajay.workingvillagers.client.KilnScreen;
import me.teajay.workingvillagers.client.StationScreen;
import me.teajay.workingvillagers.client.VillageChestRenderer;
import me.teajay.workingvillagers.client.VillageChestScreen;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class WorkingVillagersFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(WorkingVillagers.WOODCUTTER_MENU.get(), CutterScreen::new);
		MenuScreens.register(WorkingVillagers.STONECUTTER_MENU.get(), CutterScreen::new);
		MenuScreens.register(WorkingVillagers.CHOPPING_BLOCK_MENU.get(), StationScreen::new);
		MenuScreens.register(WorkingVillagers.WOODCUTTER_STATION_MENU.get(), StationScreen::new);
		MenuScreens.register(WorkingVillagers.STONECUTTER_STATION_MENU.get(), StationScreen::new);
		MenuScreens.register(WorkingVillagers.CARRIER_MENU.get(), CarrierScreen::new);
		MenuScreens.register(WorkingVillagers.CARPENTER_BENCH_MENU.get(), CarpenterScreen::new);
		MenuScreens.register(WorkingVillagers.CARPENTER_STATION_MENU.get(), StationScreen::new);
		MenuScreens.register(WorkingVillagers.COMPOSTER_STATION_MENU.get(), StationScreen::new);
		MenuScreens.register(WorkingVillagers.BLAST_FURNACE_STATION_MENU.get(), StationScreen::new);
		MenuScreens.register(WorkingVillagers.SMITHING_TABLE_STATION_MENU.get(), StationScreen::new);
		MenuScreens.register(WorkingVillagers.LOOM_STATION_MENU.get(), StationScreen::new);
		MenuScreens.register(WorkingVillagers.SMOKER_STATION_MENU.get(), StationScreen::new);
		MenuScreens.register(WorkingVillagers.BARREL_STATION_MENU.get(), StationScreen::new);
		MenuScreens.register(WorkingVillagers.FLETCHING_TABLE_STATION_MENU.get(), StationScreen::new);
		MenuScreens.register(WorkingVillagers.PIT_STAKE_MENU.get(), StationScreen::new);
		MenuScreens.register(WorkingVillagers.KILN_STATION_MENU.get(), StationScreen::new);
		MenuScreens.register(WorkingVillagers.KILN_MENU.get(), KilnScreen::new);
		MenuScreens.register(WorkingVillagers.TRADING_COUNTER_MENU.get(), StationScreen::new);
		MenuScreens.register(WorkingVillagers.MINING_BENCH_MENU.get(), StationScreen::new);
		MenuScreens.register(WorkingVillagers.MILL_MENU.get(), CutterScreen::new);
		MenuScreens.register(WorkingVillagers.VILLAGE_CHEST_MENU.get(), VillageChestScreen::new);
		MenuScreens.register(WorkingVillagers.VILLAGE_CHEST_DOUBLE_MENU.get(), VillageChestScreen::new);
		BlockEntityRendererRegistry.register(WorkingVillagers.VILLAGE_CHEST_BLOCK_ENTITY.get(), VillageChestRenderer::new);
	}
}
