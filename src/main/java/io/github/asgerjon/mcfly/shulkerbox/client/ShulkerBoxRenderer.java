package io.github.asgerjon.mcfly.shulkerbox.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.client.model.ModelShulker;
import io.github.asgerjon.mcfly.dye.DyeColor;
import io.github.asgerjon.mcfly.shulkerbox.ShulkerBoxBlockEntity;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * A shulker box as 26.3 draws it (SPEC-modern-movement.md §81; 26.3's `ShulkerBoxRenderer`): the shulker's shell (its
 * base and lid, no head) at the block's middle, a hair under full size, turned so its lid opens the box's way, the lid
 * rising half a block and turning 270 degrees as it opens; its colour's shulker texture.
 */
@SideOnly(Side.CLIENT)
public class ShulkerBoxRenderer extends TileEntitySpecialRenderer {

	private static final ResourceLocation PLAIN = new ResourceLocation("textures/entity/shulker/shulker.png");
	private static final ResourceLocation[] COLOURED = new ResourceLocation[16];

	static {
		for (DyeColor colour : DyeColor.values()) {
			COLOURED[colour.id()] = new ResourceLocation("textures/entity/shulker/shulker_" + colour.getName() + ".png");
		}
	}

	private final ModelShulker model = new ModelShulker();

	/** Its texture by its colour (0 plain, else a dye colour's id plus one). */
	public static ResourceLocation texture(int colour) {
		return colour > 0 && colour <= 16 ? COLOURED[colour - 1] : PLAIN;
	}

	@Override
	public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
		ShulkerBoxBlockEntity box = (ShulkerBoxBlockEntity) tile;
		draw(x, y, z, box.getWorldObj() != null ? box.facing() : 1, box.getProgress(partialTicks), box.colour());
	}

	/** The box at a place, opening its way (1.7.10's directions), as open as given, in its colour. */
	public void draw(double x, double y, double z, int facing, float progress, int colour) {
		bindTexture(texture(colour));
		GL11.glPushMatrix();
		GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glTranslatef((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
		GL11.glScalef(0.9995F, 0.9995F, 0.9995F);
		// 26.3's Direction.getRotation (the box's up turned to its facing)
		switch (facing) {
			case 0 -> GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
			case 2 -> {
				GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
				GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
			}
			case 3 -> GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
			case 4 -> {
				GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
				GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
			}
			case 5 -> {
				GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
				GL11.glRotatef(-90.0F, 0.0F, 0.0F, 1.0F);
			}
			default -> {
			}
		}
		GL11.glScalef(1.0F, -1.0F, -1.0F);
		GL11.glTranslatef(0.0F, -1.0F, 0.0F);
		model.base.render(0.0625F);
		model.lid.setRotationPoint(0.0F, 24.0F - progress * 0.5F * 16.0F, 0.0F);
		model.lid.rotateAngleY = 270.0F * progress * (float) (Math.PI / 180.0);
		model.lid.render(0.0625F);
		GL11.glPopAttrib();
		GL11.glPopMatrix();
	}
}
