package ganymedes01.etfuturum.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

/** 26.3's ShulkerBulletModel (SPEC-modern-movement.md §76.13): three crossed slabs, turned by the bullet's yaw and pitch. */
public class ModelShulkerBullet extends ModelBase {

	private final ModelRenderer main;

	public ModelShulkerBullet() {
		textureWidth = 64;
		textureHeight = 32;
		main = new ModelRenderer(this);
		main.setTextureOffset(0, 0).addBox(-4.0F, -4.0F, -1.0F, 8, 8, 2);
		main.setTextureOffset(0, 10).addBox(-1.0F, -4.0F, -4.0F, 2, 8, 8);
		main.setTextureOffset(20, 0).addBox(-4.0F, -1.0F, -4.0F, 8, 2, 8);
	}

	/** Drawn at the yaw and pitch (degrees). */
	public void render(float yaw, float pitch, float scale) {
		main.rotateAngleY = yaw * ((float) Math.PI / 180.0F);
		main.rotateAngleX = pitch * ((float) Math.PI / 180.0F);
		main.render(scale);
	}
}
