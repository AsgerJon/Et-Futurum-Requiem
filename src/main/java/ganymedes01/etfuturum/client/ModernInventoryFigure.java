package ganymedes01.etfuturum.client;

import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * 1.7.10's inventory figure drawing (GuiInventory.func_147046_a) at a position between pixels, so the figure can stand
 * where 26.3 puts it (SPEC-modern-movement.md §61.5): the feet at x feetY, looking along yawRef and pitchRef as 1.7.10.
 */
public final class ModernInventoryFigure {

	private ModernInventoryFigure() {
	}

	public static void draw(float x, float feetY, int size, float yawRef, float pitchRef, EntityLivingBase entity) {
		GL11.glEnable(GL11.GL_COLOR_MATERIAL);
		GL11.glPushMatrix();
		GL11.glTranslatef(x, feetY, 50.0F);
		GL11.glScalef(-size, size, size);
		GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
		float body = entity.renderYawOffset;
		float yaw = entity.rotationYaw;
		float pitch = entity.rotationPitch;
		float prevHead = entity.prevRotationYawHead;
		float head = entity.rotationYawHead;
		GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
		RenderHelper.enableStandardItemLighting();
		GL11.glRotatef(-135.0F, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(-((float) Math.atan(pitchRef / 40.0F)) * 20.0F, 1.0F, 0.0F, 0.0F);
		entity.renderYawOffset = (float) Math.atan(yawRef / 40.0F) * 20.0F;
		entity.rotationYaw = (float) Math.atan(yawRef / 40.0F) * 40.0F;
		entity.rotationPitch = -((float) Math.atan(pitchRef / 40.0F)) * 20.0F;
		entity.rotationYawHead = entity.rotationYaw;
		entity.prevRotationYawHead = entity.rotationYaw;
		GL11.glTranslatef(0.0F, entity.yOffset, 0.0F);
		RenderManager.instance.playerViewY = 180.0F;
		RenderManager.instance.renderEntityWithPosYaw(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
		entity.renderYawOffset = body;
		entity.rotationYaw = yaw;
		entity.rotationPitch = pitch;
		entity.prevRotationYawHead = prevHead;
		entity.rotationYawHead = head;
		GL11.glPopMatrix();
		RenderHelper.disableStandardItemLighting();
		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		net.minecraft.client.renderer.OpenGlHelper.setActiveTexture(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		net.minecraft.client.renderer.OpenGlHelper.setActiveTexture(net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit);
	}
}
