package ganymedes01.etfuturum.client.renderer.entity;

import ganymedes01.etfuturum.client.model.ModelTrident;
import ganymedes01.etfuturum.entities.EntityThrownTrident;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * A thrown trident as modern draws it (SPEC-modern-movement.md §58.1): the trident model turned along its flight
 * (yaw - 90 about the vertical, pitch + 90 about z), spikes first, with the glint when its item is enchanted.
 */
public class ThrownTridentRenderer extends Render {

	private final ModelTrident model = new ModelTrident();

	@Override
	public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
		EntityThrownTrident trident = (EntityThrownTrident) entity;
		GL11.glPushMatrix();
		GL11.glTranslatef((float) x, (float) y, (float) z);
		GL11.glRotatef(trident.prevRotationYaw + (trident.rotationYaw - trident.prevRotationYaw) * partialTicks - 90.0F, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(trident.prevRotationPitch + (trident.rotationPitch - trident.prevRotationPitch) * partialTicks + 90.0F, 0.0F, 0.0F, 1.0F);
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		model.render(trident.isFoil());
		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		GL11.glPopMatrix();
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		return ModelTrident.TEXTURE;
	}
}
