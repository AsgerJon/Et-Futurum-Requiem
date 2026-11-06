package io.github.asgerjon.mcfly.wood.client;

import ganymedes01.etfuturum.client.Spyglass;
import io.github.asgerjon.mcfly.wood.Signs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;

/**
 * A sign's line as 26.3 draws it (SPEC-modern-movement.md 91.8, 67.2; `AbstractSignRenderer.submitSignText`): plain text
 * in its dye's colour times 0.4, lit as the sign; glowing text in the dye's own colour at full light, over an outline
 * (the line at the 8 points around it) in the times-0.4 colour when the camera is within 16 blocks or scoping, and
 * always for black, whose outline is 0xF0EBCC. (The fork's, moved from EFR's package.)
 */
public final class SignTextDrawing {

	private SignTextDrawing() {
	}

	public static void draw(FontRenderer font, String line, int x, int y, String dye, boolean glowing, TileEntity tile) {
		int textColor = Signs.textColor(dye, glowing);
		if (!glowing) {
			font.drawString(line, x, y, textColor);
			return;
		}
		float lastX = OpenGlHelper.lastBrightnessX, lastY = OpenGlHelper.lastBrightnessY;
		boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
		GL11.glDisable(GL11.GL_LIGHTING);
		int outline = Signs.outlineColor(dye, true);
		if (outline >= 0 && (Signs.outlineAlways(dye, true) || outlineVisible(tile))) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					if (dx != 0 || dy != 0) {
						font.drawString(line, x + dx, y + dy, outline);
					}
				}
			}
		}
		font.drawString(line, x, y, textColor);
		if (lighting) {
			GL11.glEnable(GL11.GL_LIGHTING);
		}
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
	}

	/** 26.3's `isOutlineVisible`: scoping in first person, or the camera within 16 blocks of the sign's middle. */
	private static boolean outlineVisible(TileEntity tile) {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.thePlayer != null && mc.gameSettings.thirdPersonView == 0 && Spyglass.isScoping(mc.thePlayer)) {
			return true;
		}
		EntityLivingBase camera = mc.renderViewEntity;
		return camera != null && camera.getDistanceSq(tile.xCoord + 0.5D, tile.yCoord + 0.5D, tile.zCoord + 0.5D) < 16.0D * 16.0D;
	}
}
