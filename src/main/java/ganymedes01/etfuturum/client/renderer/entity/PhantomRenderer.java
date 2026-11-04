package ganymedes01.etfuturum.client.renderer.entity;

import ganymedes01.etfuturum.client.model.ModelPhantom;
import ganymedes01.etfuturum.entities.EntityPhantom;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * The phantom (SPEC-modern-movement.md §68.1, 26.3's `PhantomRenderer` and `PhantomEyesLayer`): its size's scale,
 * 26.3's offset (0, 1.3125, 0.1875), pitched with its flight, its shadow 0.75; the eyes drawn again on top at full
 * light, added (1.7.10's spider eyes' way, as 26.3's eyes render type).
 */
public class PhantomRenderer extends RenderLiving {

	private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/phantom/phantom.png");
	private static final ResourceLocation EYES = new ResourceLocation("textures/entity/phantom/phantom_eyes.png");

	public PhantomRenderer() {
		super(new ModelPhantom(), 0.75F);
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		return TEXTURE;
	}

	@Override
	protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
		float scale = 1.0F + 0.15F * (entity instanceof EntityPhantom phantom ? phantom.getPhantomSize() : 0);
		GL11.glScalef(scale, scale, scale);
		GL11.glTranslatef(0.0F, 1.3125F, 0.1875F);
	}

	@Override
	protected void rotateCorpse(EntityLivingBase entity, float age, float yaw, float partialTicks) {
		super.rotateCorpse(entity, age, yaw, partialTicks);
		GL11.glRotatef(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks, 1.0F, 0.0F, 0.0F);
	}

	@Override
	protected int shouldRenderPass(EntityLivingBase entity, int pass, float partialTicks) {
		if (pass != 0) {
			return -1;
		}
		bindTexture(EYES);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
		GL11.glDepthMask(!entity.isInvisible());
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		return 1;
	}
}
