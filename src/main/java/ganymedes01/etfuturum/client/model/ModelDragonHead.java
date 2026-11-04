package ganymedes01.etfuturum.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import org.lwjgl.opengl.GL11;

/**
 * 26.3's DragonHeadModel (SPEC-modern-movement.md §76.12, 7d): the dragon's head (upper lip, head, scales and
 * nostrils) at three quarters of its size, and its jaw, which opens and shuts while the head is powered. On the
 * dragon's texture (256 by 256, the same layout as 1.7.10's dragon).
 */
public class ModelDragonHead extends ModelBase {

	private final ModelRenderer head;
	private final ModelRenderer jaw;

	public ModelDragonHead() {
		textureWidth = 256;
		textureHeight = 256;
		head = new ModelRenderer(this);
		head.setTextureOffset(176, 44).addBox(-6.0F, -1.0F, -24.0F, 12, 5, 16);
		head.setTextureOffset(112, 30).addBox(-8.0F, -8.0F, -10.0F, 16, 16, 16);
		head.mirror = true;
		head.setTextureOffset(0, 0).addBox(-5.0F, -12.0F, -4.0F, 2, 4, 6);
		head.setTextureOffset(112, 0).addBox(-5.0F, -3.0F, -22.0F, 2, 2, 4);
		head.mirror = false;
		head.setTextureOffset(0, 0).addBox(3.0F, -12.0F, -4.0F, 2, 4, 6);
		head.setTextureOffset(112, 0).addBox(3.0F, -3.0F, -22.0F, 2, 2, 4);
		jaw = new ModelRenderer(this);
		jaw.setTextureOffset(176, 65).addBox(-6.0F, 0.0F, -16.0F, 12, 4, 16);
		jaw.setRotationPoint(0.0F, 4.0F, -8.0F);
		head.addChild(jaw);
	}

	/**
	 * The head turned by the yaw (degrees), its jaw at the animation's place (26.3: (sin(pos pi / 5) + 1) / 5 radians);
	 * the head's pose 7.99 up and scaled by three quarters, as 26.3's.
	 */
	public void render(float yaw, float animation) {
		jaw.rotateAngleX = (float) (Math.sin(animation * (float) Math.PI * 0.2F) + 1.0) * 0.2F;
		GL11.glPushMatrix();
		GL11.glTranslatef(0.0F, -7.986666F * 0.0625F, 0.0F);
		GL11.glRotatef(yaw, 0.0F, 1.0F, 0.0F);
		GL11.glScalef(0.75F, 0.75F, 0.75F);
		head.render(0.0625F);
		GL11.glPopMatrix();
	}
}
