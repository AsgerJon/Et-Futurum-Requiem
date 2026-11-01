package ganymedes01.etfuturum.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.IChatComponent;
import org.lwjgl.opengl.GL11;

/**
 * Modern's overlay message (spec §53.3): one line, white with a shadow, centred 68 above the screen's bottom (clear of
 * the hotbar, health and armour), for 60 ticks, fading out over its last 20. A new message replaces the old one.
 */
public final class OverlayMessageHud {

	private static IChatComponent message;
	private static int time;

	private OverlayMessageHud() {
	}

	public static void show(IChatComponent text) {
		message = text;
		time = 60;
	}

	public static void clear() {
		message = null;
		time = 0;
	}

	/** Each client tick the game isn't paused. */
	public static void tick() {
		if (time > 0) {
			time--;
		}
	}

	/** The text showing now, unformatted, or null (for tests). */
	public static String shown() {
		return time > 0 && message != null ? message.getUnformattedText() : null;
	}

	/** Drawn just before the chat, where modern draws it. */
	public static void render(Minecraft mc, int screenWidth, int screenHeight, float partialTicks) {
		if (message == null || time <= 0) {
			return;
		}
		int alpha = Math.min(255, (int) ((time - partialTicks) * 255.0F / 20.0F));
		// 1.7.10's font draws a colour with almost no alpha fully opaque
		if (alpha <= 8) {
			return;
		}
		FontRenderer font = mc.fontRenderer;
		String text = message.getFormattedText();
		GL11.glPushMatrix();
		GL11.glTranslatef(screenWidth / 2, screenHeight - 68, 0.0F);
		GL11.glEnable(GL11.GL_BLEND);
		OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
		font.drawStringWithShadow(text, -font.getStringWidth(text) / 2, -4, 0xFFFFFF | alpha << 24);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glPopMatrix();
	}
}
