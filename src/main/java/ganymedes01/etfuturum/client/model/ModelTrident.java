package ganymedes01.etfuturum.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Modern's trident model (SPEC-modern-movement.md §58.1): a pole 25 pixels long, a base, and three spikes, the side ones
 * a step lower, on a 32 by 32 texture. Drawn in hand and thrown, with the glint when enchanted.
 */
public class ModelTrident extends ModelBase {

	public static final ResourceLocation TEXTURE = new ResourceLocation("etfuturum", "textures/entity/trident/trident.png");
	private static final ResourceLocation GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");

	private final ModelRenderer pole;
	private final ModelRenderer base;
	private final ModelRenderer leftSpike;
	private final ModelRenderer middleSpike;
	private final ModelRenderer rightSpike;

	public ModelTrident() {
		textureWidth = 32;
		textureHeight = 32;
		pole = new ModelRenderer(this, 0, 6);
		pole.addBox(-0.5F, 2.0F, -0.5F, 1, 25, 1, 0.0F);
		base = new ModelRenderer(this, 4, 0);
		base.addBox(-1.5F, 0.0F, -0.5F, 3, 2, 1, 0.0F);
		leftSpike = new ModelRenderer(this, 4, 3);
		leftSpike.addBox(-2.5F, -3.0F, -0.5F, 1, 4, 1, 0.0F);
		middleSpike = new ModelRenderer(this, 0, 0);
		middleSpike.addBox(-0.5F, -4.0F, -0.5F, 1, 4, 1, 0.0F);
		rightSpike = new ModelRenderer(this, 4, 3);
		rightSpike.mirror = true;
		rightSpike.addBox(1.5F, -3.0F, -0.5F, 1, 4, 1, 0.0F);
	}

	/** The model with its texture, and the glint over it when it has one. */
	public void render(boolean glint) {
		Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
		renderParts();
		if (glint) {
			renderGlint();
		}
	}

	private void renderParts() {
		pole.render(0.0625F);
		base.render(0.0625F);
		leftSpike.render(0.0625F);
		middleSpike.render(0.0625F);
		rightSpike.render(0.0625F);
	}

	/** 1.7.10's glint for 3D models (as on armour): two scrolling layers drawn over the model's own faces. */
	private void renderGlint() {
		float time = Minecraft.getSystemTime() % 3000L / 3000.0F * 8.0F;
		Minecraft.getMinecraft().getTextureManager().bindTexture(GLINT);
		GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_LIGHTING_BIT);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDepthFunc(GL11.GL_EQUAL);
		GL11.glDepthMask(false);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glBlendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE);
		for (int layer = 0; layer < 2; layer++) {
			float strength = 0.76F;
			GL11.glColor4f(0.5F * strength, 0.25F * strength, 0.8F * strength, 1.0F);
			GL11.glMatrixMode(GL11.GL_TEXTURE);
			GL11.glLoadIdentity();
			GL11.glScalef(0.33333334F, 0.33333334F, 0.33333334F);
			GL11.glRotatef(30.0F - layer * 60.0F, 0.0F, 0.0F, 1.0F);
			GL11.glTranslatef(0.0F, time * (0.001F + layer * 0.003F) * 20.0F, 0.0F);
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			renderParts();
		}
		GL11.glMatrixMode(GL11.GL_TEXTURE);
		GL11.glLoadIdentity();
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glPopAttrib();
	}
}
