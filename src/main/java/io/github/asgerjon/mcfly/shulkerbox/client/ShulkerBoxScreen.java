package io.github.asgerjon.mcfly.shulkerbox.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.shulkerbox.ShulkerBoxMenu;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * A shulker box's screen (SPEC-modern-movement.md §81; 26.3's `ShulkerBoxScreen`): 26.3's `shulker_box.png`, 176 by 167,
 * the box's name at (8, 6) and the inventory's above the player's slots.
 */
@SideOnly(Side.CLIENT)
public class ShulkerBoxScreen extends GuiContainer {

	private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/shulker_box.png");

	private final IInventory box;
	private final InventoryPlayer player;

	public ShulkerBoxScreen(InventoryPlayer player, IInventory box) {
		super(new ShulkerBoxMenu(player, box));
		this.box = box;
		this.player = player;
		ySize = 167;
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		fontRendererObj.drawString(box.hasCustomInventoryName() ? box.getInventoryName() : I18n.format(box.getInventoryName()), 8, 6, 0x404040);
		fontRendererObj.drawString(I18n.format(player.getInventoryName()), 8, ySize - 94, 0x404040);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		mc.getTextureManager().bindTexture(TEXTURE);
		drawTexturedModalRect((width - xSize) / 2, (height - ySize) / 2, 0, 0, xSize, ySize);
	}
}
