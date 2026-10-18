package me.teajay.workingvillagers.client;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.jspecify.annotations.Nullable;

/**
 * The button at the top right of a workstation's own screen (woodcutter, stonecutter, mill,
 * carpenter's bench, blast furnace, kiln, smithing table) that opens the station's overview:
 * a vanilla button with the face of the profession that works there, the size of the recipe
 * book button. Without a hired worker the face is dimmed.
 */
public final class WorkerButton {
	public static final int X = 150;
	public static final int Y = 2;
	public static final int WIDTH = 20;
	public static final int HEIGHT = 18;
	private static final Identifier BUTTON = Identifier.withDefaultNamespace("widget/button");
	private static final Identifier BUTTON_HIGHLIGHTED = Identifier.withDefaultNamespace("widget/button_highlighted");
	private static final Identifier VILLAGER = Identifier.withDefaultNamespace("textures/entity/villager/villager.png");
	private static final int TEXTURE_SIZE = 64;
	private static final int BRIGHT = 0xFFFFFFFF;
	private static final int DIMMED = 0xFF6A6A6A;

	private WorkerButton() {
	}

	/** Draws the button for the screen whose top left corner is at {@code left}, {@code top}. */
	public static void extract(GuiGraphicsExtractor graphics, int left, int top, int mouseX, int mouseY, ResourceKey<VillagerProfession> profession, boolean worker) {
		boolean over = isOver(mouseX, mouseY, left, top);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, over ? BUTTON_HIGHLIGHTED : BUTTON, left + X, top + Y, WIDTH, HEIGHT);
		Identifier overlay = Identifier.fromNamespaceAndPath(profession.identifier().getNamespace(),
			"textures/entity/villager/profession/" + profession.identifier().getPath() + ".png");
		int color = worker ? BRIGHT : DIMMED;
		int x = left + X + 2;
		int y = top + Y + 1;
		// The front of the head without its top row (8×8 of the 8×10), twice as large; the
		// profession's clothes and its hat layer over it, then the nose
		face(graphics, VILLAGER, x, y, 8, 9, color);
		face(graphics, overlay, x, y, 8, 9, color);
		face(graphics, overlay, x, y, 40, 9, color);
		graphics.blit(RenderPipelines.GUI_TEXTURED, VILLAGER, x + 6, y + 8, 26.0F, 2.0F, 4, 8, 2, 4, TEXTURE_SIZE, TEXTURE_SIZE, color);
		if (over) {
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}
	}

	private static void face(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int u, int v, int color) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, 16, 16, 8, 8, TEXTURE_SIZE, TEXTURE_SIZE, color);
	}

	public static boolean isOver(double mouseX, double mouseY, int left, int top) {
		return GuiPanels.isOver(mouseX, mouseY, left + X, top + Y, WIDTH, HEIGHT);
	}

	/** What the button does, and below it who works there ({@code worker}: that line, null if unknown). */
	public static List<FormattedCharSequence> tooltip(@Nullable Component worker) {
		List<FormattedCharSequence> lines = new ArrayList<>();
		lines.add(Component.translatable("gui.workingvillagers.station.open").getVisualOrderText());
		if (worker != null) {
			lines.add(worker.copy().withStyle(ChatFormatting.GRAY).getVisualOrderText());
		}
		return lines;
	}

	/** The line for the tooltip: "Worker: Anton" or "Free". */
	public static Component workerLine(Optional<Component> worker) {
		return worker.map(name -> (Component) Component.translatable("gui.workingvillagers.worker", name))
			.orElse(Component.translatable("gui.workingvillagers.worker.none"));
	}

	/** Opens the overview: the menu on the server handles the button. */
	public static void click(Minecraft minecraft, int containerId, int buttonId) {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		minecraft.gameMode.handleInventoryButtonClick(containerId, buttonId);
	}
}
