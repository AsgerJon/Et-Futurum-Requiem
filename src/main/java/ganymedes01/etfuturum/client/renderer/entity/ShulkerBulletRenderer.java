package ganymedes01.etfuturum.client.renderer.entity;

import ganymedes01.etfuturum.client.model.ModelShulkerBullet;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * 26.3's ShulkerBulletRenderer (SPEC-modern-movement.md §76.13): fully lit, 0.15 up, spinning with its age, at half size,
 * then again half as big again and see-through (26.3's 0x26 alpha) around it.
 */
public class ShulkerBulletRenderer extends Render {

	private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/shulker/spark.png");
	private final ModelShulkerBullet model = new ModelShulkerBullet();

	public ShulkerBulletRenderer() {
		shadowSize = 0.0F;
	}

	private static float rotLerp(float from, float to, float partialTick) {
		return from + partialTick * MathHelper.wrapAngleTo180_float(to - from);
	}

	@Override
	public void doRender(Entity entity, double x, double y, double z, float entityYaw, float partialTick) {
		float yaw = rotLerp(entity.prevRotationYaw, entity.rotationYaw, partialTick);
		float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTick;
		float age = entity.ticksExisted + partialTick;
		GL11.glPushMatrix();
		GL11.glTranslatef((float) x, (float) y + 0.15F, (float) z);
		GL11.glRotatef(MathHelper.sin(age * 0.1F) * 180.0F, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(MathHelper.cos(age * 0.1F) * 180.0F, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(MathHelper.sin(age * 0.15F) * 360.0F, 0.0F, 0.0F, 1.0F);
		GL11.glScalef(-0.5F, -0.5F, 0.5F);
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
		bindEntityTexture(entity);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		model.render(yaw, pitch, 0.0625F);
		GL11.glScalef(1.5F, 1.5F, 1.5F);
		GL11.glEnable(GL11.GL_BLEND);
		OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 0x26 / 255.0F);
		model.render(yaw, pitch, 0.0625F);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glPopMatrix();
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		return TEXTURE;
	}
}
