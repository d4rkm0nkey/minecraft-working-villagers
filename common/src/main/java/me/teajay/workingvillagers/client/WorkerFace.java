package me.teajay.workingvillagers.client;

import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

/**
 * The face of a profession, drawn from the villager texture and the profession's clothes: the
 * icon of the worker tab (see {@link StationTabs}) and of the workers in the village chest.
 */
public final class WorkerFace {
	private static final Identifier VILLAGER = Identifier.withDefaultNamespace("textures/entity/villager/villager.png");
	private static final int TEXTURE_SIZE = 64;
	private static final int BRIGHT = 0xFFFFFFFF;
	private static final int DIMMED = 0xFF6A6A6A;

	private WorkerFace() {
	}

	/** The 16×16 face of the profession at x, y; dimmed if {@code bright} is false (nobody works there). */
	public static void face(GuiGraphicsExtractor graphics, int x, int y, ResourceKey<VillagerProfession> profession, boolean bright) {
		Identifier overlay = Identifier.fromNamespaceAndPath(profession.identifier().getNamespace(),
			"textures/entity/villager/profession/" + profession.identifier().getPath() + ".png");
		int color = bright ? BRIGHT : DIMMED;
		// The front of the head without its top row (8×8 of the 8×10), twice as large; the
		// profession's clothes and its hat layer over it, then the nose
		facePart(graphics, VILLAGER, x, y, 8, 9, color);
		facePart(graphics, overlay, x, y, 8, 9, color);
		facePart(graphics, overlay, x, y, 40, 9, color);
		graphics.blit(RenderPipelines.GUI_TEXTURED, VILLAGER, x + 6, y + 8, 26.0F, 2.0F, 4, 8, 2, 4, TEXTURE_SIZE, TEXTURE_SIZE, color);
	}

	private static void facePart(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int u, int v, int color) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, 16, 16, 8, 8, TEXTURE_SIZE, TEXTURE_SIZE, color);
	}

	/** The line for a tooltip: "Worker: Anton" or "Free". */
	public static Component workerLine(Optional<Component> worker) {
		return worker.map(name -> (Component) Component.translatable("gui.workingvillagers.worker", name))
			.orElse(Component.translatable("gui.workingvillagers.worker.none"));
	}
}
