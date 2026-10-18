package me.teajay.workingvillagers.mixin.client;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import me.teajay.workingvillagers.client.GuiPanels;
import me.teajay.workingvillagers.network.RenameContainerPayload;
import me.teajay.workingvillagers.station.ContainerNames;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A "✎" button at the top right of chest and barrel screens: it turns the title into a text
 * field, and the name entered there is the name the workstation and carrier menus show.
 */
@Mixin(ContainerScreen.class)
public abstract class ContainerScreenMixin extends AbstractContainerScreen<ChestMenu> {
	@Unique
	private static final int RENAME_Y = 3;
	@Unique
	private static final int RENAME_SIZE = 12;

	/** The container the screen belongs to, null if it is none that can be named (ender chest, minecart). */
	@Unique
	private @Nullable BlockPos workingvillagers$pos;
	@Unique
	private boolean workingvillagers$posKnown;
	/** The text field while the container is renamed, null otherwise. */
	@Unique
	private @Nullable EditBox workingvillagers$nameBox;

	private ContainerScreenMixin(ChestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void init() {
		super.init();
		// init() runs again on resize, which also drops the widgets
		this.workingvillagers$nameBox = null;
		if (!this.workingvillagers$posKnown) {
			// The screen opens right after the right-click, so the crosshair is still on the block
			this.workingvillagers$posKnown = true;
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
				&& minecraft.level != null && minecraft.level.getBlockEntity(hit.getBlockPos()) instanceof BaseContainerBlockEntity) {
				this.workingvillagers$pos = hit.getBlockPos();
			}
		}
	}

	/** After the chest texture (ContainerScreen's own method, so it is added to, not replaced). */
	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void workingvillagers$extractRenameButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (this.workingvillagers$pos == null || this.workingvillagers$nameBox != null) {
			return;
		}
		boolean over = this.workingvillagers$isOverRename(mouseX, mouseY);
		GuiPanels.button(graphics, this.font, this.workingvillagers$renameX(), this.topPos + RENAME_Y, RENAME_SIZE, "✎", over);
		if (over) {
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (this.workingvillagers$nameBox == null) {
			super.extractLabels(graphics, mouseX, mouseY);
			return;
		}
		// The text field takes the place of the title
		graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, GuiPanels.TEXT_COLOR, false);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		if (this.workingvillagers$nameBox == null && this.workingvillagers$isOverRename(mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(this.font, Component.translatable("gui.workingvillagers.container.rename"), mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		EditBox box = this.workingvillagers$nameBox;
		if (box != null && !box.isMouseOver(event.x(), event.y())) {
			this.workingvillagers$finishRename(true);
		}
		if (this.workingvillagers$nameBox == null && this.workingvillagers$isOverRename(event.x(), event.y())) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			this.workingvillagers$startRename();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		EditBox box = this.workingvillagers$nameBox;
		if (box != null) {
			if (event.isConfirmation()) {
				this.workingvillagers$finishRename(true);
				return true;
			}
			if (event.isEscape()) {
				this.workingvillagers$finishRename(false);
				return true;
			}
			// Keep keys like E (inventory) from closing the screen while typing
			box.keyPressed(event);
			return true;
		}
		return super.keyPressed(event);
	}

	@Unique
	private void workingvillagers$startRename() {
		// A title the player gave is literal text; vanilla titles ("Chest") start the field empty
		String current = this.title.getContents() instanceof PlainTextContents ? this.title.getString() : "";
		EditBox box = new EditBox(this.font, this.leftPos + 7, this.topPos + 4, this.imageWidth - 14, 11,
			Component.translatable("gui.workingvillagers.container.rename"));
		box.setMaxLength(ContainerNames.MAX_LENGTH);
		box.setValue(current);
		box.setHint(Component.translatable("gui.workingvillagers.container.rename.hint").withStyle(ChatFormatting.DARK_GRAY));
		this.workingvillagers$nameBox = box;
		this.addRenderableWidget(box);
		this.setFocused(box);
		box.setFocused(true);
	}

	/** Ends renaming; sends the new name if {@code save}. */
	@Unique
	private void workingvillagers$finishRename(boolean save) {
		EditBox box = this.workingvillagers$nameBox;
		if (box == null) {
			return;
		}
		if (save && this.workingvillagers$pos != null) {
			String name = ContainerNames.clean(box.getValue());
			this.minecraft.getConnection().send(new ServerboundCustomPayloadPacket(
				new RenameContainerPayload(this.menu.containerId, this.workingvillagers$pos, name)
			));
			// The server does not send a new title to an open screen
			((ScreenAccessor) this).workingvillagers$setTitle(name.isEmpty() ? this.workingvillagers$defaultTitle() : Component.literal(name));
		}
		this.removeWidget(box);
		this.workingvillagers$nameBox = null;
	}

	/** The vanilla title of the container without a name. */
	@Unique
	private Component workingvillagers$defaultTitle() {
		if (this.menu.getRowCount() == 6 && this.minecraft.level.getBlockState(this.workingvillagers$pos).getBlock() instanceof ChestBlock) {
			return Component.translatable("container.chestDouble");
		}
		return this.minecraft.level.getBlockState(this.workingvillagers$pos).getBlock().getName();
	}

	@Unique
	private int workingvillagers$renameX() {
		return this.leftPos + this.imageWidth - 7 - RENAME_SIZE;
	}

	@Unique
	private boolean workingvillagers$isOverRename(double mouseX, double mouseY) {
		return this.workingvillagers$pos != null
			&& GuiPanels.isOver(mouseX, mouseY, this.workingvillagers$renameX(), this.topPos + RENAME_Y, RENAME_SIZE, RENAME_SIZE);
	}
}
