package io.github.asgerjon.mcfly.workstation.client;

import io.github.asgerjon.mcfly.workstation.CookerMenu;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * A smoker's or blast furnace's screen (SPEC-modern-movement.md 92.3; 26.3's `AbstractFurnaceScreen`): its own
 * background (`gui/container/<kind>.png`), the flame from `sprites/container/<kind>/lit_progress` (14 by 14, its height
 * the fuel left, at 56, 36) and the arrow from `burn_progress` (24 by 16, its width the cook, at 79, 34); titled by its
 * name or its custom name. No recipe book (1.7.10 has none).
 */
public class CookerScreen extends GuiContainer {

	private final CookerMenu menu;
	private final ResourceLocation background;
	private final ResourceLocation litSprite;
	private final ResourceLocation burnSprite;

	public CookerScreen(CookerMenu menu) {
		super(menu);
		this.menu = menu;
		String kind = menu.cooker().kind().equals("smoking") ? "smoker" : "blast_furnace";
		background = new ResourceLocation("textures/gui/container/" + kind + ".png");
		litSprite = new ResourceLocation("textures/gui/sprites/container/" + kind + "/lit_progress.png");
		burnSprite = new ResourceLocation("textures/gui/sprites/container/" + kind + "/burn_progress.png");
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		String title = menu.cooker().hasCustomInventoryName() ? menu.cooker().getInventoryName() : I18n.format(menu.cooker().getInventoryName());
		fontRendererObj.drawString(title, (xSize - fontRendererObj.getStringWidth(title)) / 2, 6, 0x404040);
		fontRendererObj.drawString(I18n.format("container.inventory"), 8, ySize - 96 + 2, 0x404040);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		mc.getTextureManager().bindTexture(background);
		int left = guiLeft;
		int top = guiTop;
		drawTexturedModalRect(left, top, 0, 0, xSize, ySize);
		if (menu.isLit()) {
			int height = MathHelper.ceiling_float_int(menu.litProgress() * 13.0F) + 1;
			mc.getTextureManager().bindTexture(litSprite);
			Gui.func_146110_a(left + 56, top + 36 + 14 - height, 0.0F, 14 - height, 14, height, 14.0F, 14.0F);
		}
		int width = MathHelper.ceiling_float_int(menu.burnProgress() * 24.0F);
		if (width > 0) {
			mc.getTextureManager().bindTexture(burnSprite);
			Gui.func_146110_a(left + 79, top + 34, 0.0F, 0.0F, width, 16, 24.0F, 16.0F);
		}
	}
}
