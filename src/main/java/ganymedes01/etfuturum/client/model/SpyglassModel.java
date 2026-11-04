package ganymedes01.etfuturum.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

/**
 * 26.3's {@code item/spyglass_in_hand} model (SPEC-modern-movement.md §64.5): two boxes in sixteenths of the block, the
 * eyepiece over the tube, their faces' UVs on {@code spyglass_model.png} as the model gives them, drawn in the model's
 * own space (y up, [0, 1]), each face's orientation as 26.3's block models map UVs.
 */
public final class SpyglassModel {

	private static final ResourceLocation TEXTURE = new ResourceLocation("etfuturum", "textures/item/spyglass_model.png");

	private SpyglassModel() {
	}

	public static void render() {
		Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
		Tessellator t = Tessellator.instance;
		t.startDrawingQuads();
		box(t, 7.0D, 8.5D, 7.0D, 9.0D, 13.5D, 9.0D, new double[]{0, 2, 2, 7}, new double[]{0, 0, 2, 2}, null);
		box(t, 6.9D, 2.4D, 6.9D, 9.1D, 8.6D, 9.1D, new double[]{0, 7, 2, 13}, new double[]{0, 5, 2, 7}, new double[]{0, 13, 2, 15});
		t.draw();
	}

	/** A box from and to (sixteenths): the four sides with one UV, the top and the bottom (null: none) their own. */
	private static void box(Tessellator t, double x1, double y1, double z1, double x2, double y2, double z2, double[] side, double[] up, double[] down) {
		x1 /= 16.0D;
		y1 /= 16.0D;
		z1 /= 16.0D;
		x2 /= 16.0D;
		y2 /= 16.0D;
		z2 /= 16.0D;
		double u1 = side[0] / 16.0D, v1 = side[1] / 16.0D, u2 = side[2] / 16.0D, v2 = side[3] / 16.0D;
		// north (-z): seen from outside, +x to the left
		t.setNormal(0.0F, 0.0F, -1.0F);
		t.addVertexWithUV(x2, y2, z1, u1, v1);
		t.addVertexWithUV(x2, y1, z1, u1, v2);
		t.addVertexWithUV(x1, y1, z1, u2, v2);
		t.addVertexWithUV(x1, y2, z1, u2, v1);
		// south (+z): +x to the right
		t.setNormal(0.0F, 0.0F, 1.0F);
		t.addVertexWithUV(x1, y2, z2, u1, v1);
		t.addVertexWithUV(x1, y1, z2, u1, v2);
		t.addVertexWithUV(x2, y1, z2, u2, v2);
		t.addVertexWithUV(x2, y2, z2, u2, v1);
		// west (-x): +z to the right
		t.setNormal(-1.0F, 0.0F, 0.0F);
		t.addVertexWithUV(x1, y2, z1, u1, v1);
		t.addVertexWithUV(x1, y1, z1, u1, v2);
		t.addVertexWithUV(x1, y1, z2, u2, v2);
		t.addVertexWithUV(x1, y2, z2, u2, v1);
		// east (+x): -z to the right
		t.setNormal(1.0F, 0.0F, 0.0F);
		t.addVertexWithUV(x2, y2, z2, u1, v1);
		t.addVertexWithUV(x2, y1, z2, u1, v2);
		t.addVertexWithUV(x2, y1, z1, u2, v2);
		t.addVertexWithUV(x2, y2, z1, u2, v1);
		// up: +x right, north at the top
		t.setNormal(0.0F, 1.0F, 0.0F);
		t.addVertexWithUV(x1, y2, z1, up[0] / 16.0D, up[1] / 16.0D);
		t.addVertexWithUV(x1, y2, z2, up[0] / 16.0D, up[3] / 16.0D);
		t.addVertexWithUV(x2, y2, z2, up[2] / 16.0D, up[3] / 16.0D);
		t.addVertexWithUV(x2, y2, z1, up[2] / 16.0D, up[1] / 16.0D);
		if (down != null) {
			// down: +x right, south at the top
			t.setNormal(0.0F, -1.0F, 0.0F);
			t.addVertexWithUV(x1, y1, z2, down[0] / 16.0D, down[1] / 16.0D);
			t.addVertexWithUV(x1, y1, z1, down[0] / 16.0D, down[3] / 16.0D);
			t.addVertexWithUV(x2, y1, z1, down[2] / 16.0D, down[3] / 16.0D);
			t.addVertexWithUV(x2, y1, z2, down[2] / 16.0D, down[1] / 16.0D);
		}
	}
}
