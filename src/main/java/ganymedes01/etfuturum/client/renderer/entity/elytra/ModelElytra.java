package ganymedes01.etfuturum.client.renderer.entity.elytra;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * Two 10 x 20 x 2 wings, inflated by 1, pivoting at x = ±5 (§21.3). The right wing mirrors the left one.
 */
public class ModelElytra extends ModelBase {
	private final ModelRenderer leftWing = new ModelRenderer(this, 22, 0);
	private final ModelRenderer rightWing = new ModelRenderer(this, 22, 0);

	public ModelElytra() {
		this.leftWing.addBox(-10.0F, 0.0F, 0.0F, 10, 20, 2, 1.0F);
		this.leftWing.rotationPointX = 5.0F;
		this.rightWing.mirror = true;
		this.rightWing.addBox(0.0F, 0.0F, 0.0F, 10, 20, 2, 1.0F);
		this.rightWing.rotationPointX = -5.0F;
	}

	/**
	 * The left wing gets the angles as given, the right wing the same pitch with yaw and roll flipped. Both move down
	 * 3 units while crouching.
	 */
	public void setWingAngles(float rotX, float rotY, float rotZ, boolean crouching) {
		this.leftWing.rotationPointY = crouching ? 3.0F : 0.0F;
		this.leftWing.rotateAngleX = rotX;
		this.leftWing.rotateAngleY = rotY;
		this.leftWing.rotateAngleZ = rotZ;
		this.rightWing.rotationPointY = this.leftWing.rotationPointY;
		this.rightWing.rotateAngleX = rotX;
		this.rightWing.rotateAngleY = -rotY;
		this.rightWing.rotateAngleZ = -rotZ;
	}

	public void renderWings(float scale) {
		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		GL11.glDisable(GL11.GL_CULL_FACE);
		this.leftWing.render(scale);
		this.rightWing.render(scale);
	}
}
