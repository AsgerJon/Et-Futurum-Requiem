package ganymedes01.etfuturum.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.Random;

/**
 * The totem flying at the screen when a totem saves the player (SPEC-modern-movement.md §55.2), as modern's: 40 ticks;
 * it comes close, spins about the vertical and wobbles, drifting towards a random side, then falls back. Drawn in the
 * hand's projection after the hand, full bright, not with the HUD hidden.
 */
public final class TotemActivation {

	private static final int LENGTH = 40;
	private static final Random random = new Random();

	private static ItemStack item;
	private static int ticks;
	private static float offX;
	private static float offY;

	private TotemActivation() {
	}

	public static void activate(ItemStack stack) {
		item = stack.copy();
		ticks = LENGTH;
		offX = random.nextFloat() * 2.0F - 1.0F;
		offY = random.nextFloat() * 2.0F - 1.0F;
	}

	/** Each client tick the game isn't paused. */
	public static void tick() {
		if (ticks > 0 && --ticks == 0) {
			item = null;
		}
	}

	/** Ticks of the animation left; 0 when none is showing. */
	public static int ticksLeft() {
		return item != null ? ticks : 0;
	}

	public static void render(Minecraft mc, float partialTicks) {
		if (item == null || ticks <= 0 || mc.gameSettings.hideGUI) {
			return;
		}
		float progress = (LENGTH - ticks + partialTicks) / LENGTH;
		float p2 = progress * progress;
		float p3 = progress * p2;
		float eased = 10.25F * p3 * p2 - 24.95F * p2 * p2 + 25.5F * p3 - 13.8F * p2 + 4.0F * progress;
		float turn = eased * (float) Math.PI;
		float aspect = (float) mc.displayWidth / mc.displayHeight;
		float drift = Math.abs(MathHelper.sin(turn * 2.0F));
		float wobble = 6.0F * MathHelper.cos(progress * 8.0F);

		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPushMatrix();
		GL11.glLoadIdentity();
		RenderHelper.enableStandardItemLighting();
		GL11.glTranslatef(offX * 0.3F * aspect * drift, offY * 0.3F * drift, -10.0F + 9.0F * MathHelper.sin(turn));
		GL11.glScalef(0.8F, 0.8F, 0.8F);
		GL11.glRotatef(900.0F * Math.abs(MathHelper.sin(turn)), 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(wobble, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(wobble, 0.0F, 0.0F, 1.0F);
		// 1.7.10 draws a flat item from its corner, one sixteenth thick: its middle to the origin
		GL11.glTranslatef(-0.5F, -0.5F, 0.03125F);
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
		IIcon icon = item.getIconIndex();
		if (icon != null) {
			mc.getTextureManager().bindTexture(mc.getTextureManager().getResourceLocation(item.getItemSpriteNumber()));
			ItemRenderer.renderItemIn2D(Tessellator.instance, icon.getMaxU(), icon.getMinV(), icon.getMinU(), icon.getMaxV(),
				icon.getIconWidth(), icon.getIconHeight(), 0.0625F);
		}
		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		RenderHelper.disableStandardItemLighting();
		GL11.glPopMatrix();
	}
}
