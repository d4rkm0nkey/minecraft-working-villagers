package me.teajay.workingvillagers.neoforge;

import me.teajay.workingvillagers.WorkingVillagers;
import me.teajay.workingvillagers.client.CarpenterScreen;
import me.teajay.workingvillagers.client.CarrierScreen;
import me.teajay.workingvillagers.client.CutterScreen;
import me.teajay.workingvillagers.client.StationScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = WorkingVillagers.MOD_ID, dist = Dist.CLIENT)
public class WorkingVillagersNeoForgeClient {
	public WorkingVillagersNeoForgeClient(IEventBus modBus) {
		modBus.addListener(RegisterMenuScreensEvent.class, event -> {
			event.register(WorkingVillagers.WOODCUTTER_MENU.get(), CutterScreen::new);
			event.register(WorkingVillagers.STONECUTTER_MENU.get(), CutterScreen::new);
			event.register(WorkingVillagers.CHOPPING_BLOCK_MENU.get(), StationScreen::new);
			event.register(WorkingVillagers.WOODCUTTER_STATION_MENU.get(), StationScreen::new);
			event.register(WorkingVillagers.STONECUTTER_STATION_MENU.get(), StationScreen::new);
			event.register(WorkingVillagers.CARRIER_MENU.get(), CarrierScreen::new);
			event.register(WorkingVillagers.CARPENTER_BENCH_MENU.get(), CarpenterScreen::new);
			event.register(WorkingVillagers.CARPENTER_STATION_MENU.get(), StationScreen::new);
			event.register(WorkingVillagers.COMPOSTER_STATION_MENU.get(), StationScreen::new);
			event.register(WorkingVillagers.BLAST_FURNACE_STATION_MENU.get(), StationScreen::new);
		});
	}
}
