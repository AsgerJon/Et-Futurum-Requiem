package ganymedes01.etfuturum.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Riptide's spin effect around a spinning player (SPEC-modern-movement.md §58.2): two translucent tubes, 16 by 32 by 16
 * pixels at 0.75 and 1.5 scale, one above the other, turning at 50 and 55 degrees a tick, on a 64 by 64 texture.
 */
public class ModelRiptide extends ModelBase {

	private static final ResourceLocation TEXTURE = new ResourceLocation("etfuturum", "textures/entity/trident/trident_riptide.png");

	private final ModelRenderer[] tubes = new ModelRenderer[2];

	public ModelRiptide() {
		textureWidth = 64;
		textureHeight = 64;
		for (int i = 0; i < tubes.length; i++) {
			float yOffset = -3.2F + 9.6F * (i + 1);
			tubes[i] = new ModelRenderer(this, 0, 0);
			tubes[i].addBox(-8.0F, -16.0F + yOffset, -8.0F, 16, 32, 16, 0.0F);
		}
	}

	/** Drawn in the player model's space, at this age in ticks. */
	public void render(float ageInTicks) {
		Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
		GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		for (int i = 0; i < tubes.length; i++) {
			float scale = 0.75F * (i + 1);
			tubes[i].rotateAngleY = MathHelper.wrapAngleTo180_float(ageInTicks * -(45 + (i + 1) * 5)) * (float) Math.PI / 180.0F;
			GL11.glPushMatrix();
			GL11.glScalef(scale, scale, scale);
			tubes[i].render(0.0625F);
			GL11.glPopMatrix();
		}
		GL11.glPopAttrib();
	}
}
