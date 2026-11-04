package ganymedes01.etfuturum.client.renderer.entity;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/** 26.3's dragon fireball as drawn (`DragonFireballRenderer`): its texture on a flat square twice its size, facing the camera, full bright. */
public class RenderDragonFireball extends Render {

	private static final ResourceLocation TEXTURE = new ResourceLocation("etfuturum", "textures/entity/enderdragon/dragon_fireball.png");

	@Override
	public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		GL11.glScalef(2.0F, 2.0F, 2.0F);
		bindEntityTexture(entity);
		GL11.glRotatef(180.0F - renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(-renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		tessellator.setBrightness(15728880);
		tessellator.setNormal(0.0F, 1.0F, 0.0F);
		tessellator.addVertexWithUV(-0.5, -0.25, 0.0, 0.0, 1.0);
		tessellator.addVertexWithUV(0.5, -0.25, 0.0, 1.0, 1.0);
		tessellator.addVertexWithUV(0.5, 0.75, 0.0, 1.0, 0.0);
		tessellator.addVertexWithUV(-0.5, 0.75, 0.0, 0.0, 0.0);
		tessellator.draw();
		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		GL11.glPopMatrix();
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		return TEXTURE;
	}
}
