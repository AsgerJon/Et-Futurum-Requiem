package io.github.asgerjon.mcfly.banner.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.banner.BannerBlockEntity;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * A placed banner as 26.3 draws it (SPEC-modern-movement.md §80; 26.3's `BannerRenderer`): at the block's middle,
 * turned by its rotation (22.5 degrees a step) or by its wall's facing, two thirds the size with y and z flipped, its
 * canvas on the model, the flag swaying on a cycle of 100 ticks offset by where it is. Drawn at the origin with no
 * world, it's the banner as an item (`BannerItemRenderer`).
 */
@SideOnly(Side.CLIENT)
public class BannerRenderer extends TileEntitySpecialRenderer {

	private static final float SIZE = 0.6666667F;

	private final BannerModel standing = new BannerModel(true);
	private final BannerModel wall = new BannerModel(false);

	/** A wall banner's turn by its meta (2 north, 3 south, 4 west, 5 east): 26.3's `Direction.toYRot`. */
	private static float wallYaw(int meta) {
		return switch (meta) {
			case 2 -> 180.0F;
			case 4 -> 90.0F;
			case 5 -> 270.0F;
			default -> 0.0F;
		};
	}

	@Override
	public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
		BannerBlockEntity banner = (BannerBlockEntity) tile;
		boolean onGround = banner.isStanding();
		int meta = banner.getWorldObj() != null ? banner.getBlockMetadata() : 0;
		long time = banner.getWorldObj() != null ? banner.getWorldObj().getTotalWorldTime() : 0L;
		float phase = (Math.floorMod(banner.xCoord * 7L + banner.yCoord * 9L + banner.zCoord * 13L + time, 100L) + partialTicks) / 100.0F;
		BannerModel model = onGround ? standing : wall;
		model.sway(phase);
		ResourceLocation canvas = BannerCanvas.of(banner);
		if (canvas == null) {
			return;
		}
		GL11.glPushMatrix();
		GL11.glTranslatef((float) x + 0.5F, (float) y, (float) z + 0.5F);
		GL11.glRotatef(-(onGround ? meta * 22.5F : wallYaw(meta)), 0.0F, 1.0F, 0.0F);
		GL11.glScalef(SIZE, -SIZE, -SIZE);
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		bindTexture(canvas);
		model.render();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glPopMatrix();
	}
}
