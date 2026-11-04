package io.github.asgerjon.mcfly.banner.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

/**
 * 26.3's banner (SPEC-modern-movement.md §80; its `BannerModel` and `BannerFlagModel`, 64 by 64): standing, a pole, the
 * bar on it and the flag hanging from the bar; on a wall, the bar and the flag against the wall. The flag sways.
 */
@SideOnly(Side.CLIENT)
public class BannerModel extends ModelBase {

	private final ModelRenderer pole;
	private final ModelRenderer bar;
	private final ModelRenderer flag;

	public BannerModel(boolean standing) {
		textureWidth = 64;
		textureHeight = 64;
		if (standing) {
			pole = new ModelRenderer(this, 44, 0);
			pole.addBox(-1.0F, -42.0F, -1.0F, 2, 42, 2);
		} else {
			pole = null;
		}
		bar = new ModelRenderer(this, 0, 42);
		bar.addBox(-10.0F, standing ? -44.0F : -20.5F, standing ? -1.0F : 9.5F, 20, 2, 2);
		flag = new ModelRenderer(this, 0, 0);
		flag.addBox(-10.0F, 0.0F, -2.0F, 20, 40, 1);
		flag.setRotationPoint(0.0F, standing ? -44.0F : -20.5F, standing ? 0.0F : 10.5F);
	}

	/** The flag's sway at a phase (0 to 1, a cycle of 100 ticks): 26.3's `BannerFlagModel.setupAnim`. */
	public void sway(float phase) {
		flag.rotateAngleX = (-0.0125F + 0.01F * (float) Math.cos(Math.PI * 2.0D * phase)) * (float) Math.PI;
	}

	public void render() {
		float scale = 0.0625F;
		if (pole != null) {
			pole.render(scale);
		}
		bar.render(scale);
		flag.render(scale);
	}
}
