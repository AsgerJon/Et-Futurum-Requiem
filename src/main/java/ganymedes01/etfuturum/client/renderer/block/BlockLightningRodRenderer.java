package ganymedes01.etfuturum.client.renderer.block;

import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;
import ganymedes01.etfuturum.client.OpenGLHelper;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

/**
 * 26.3's lightning rod model (`template_lightning_rod.json`): a 4 x 4 x 4 head on a 2 x 2 x 12 shaft, pointing up,
 * turned to the way the rod points. Its faces take the shading of 1.7.10's plain lighting.
 */
@ThreadSafeISBRH(perThread = false)
public class BlockLightningRodRenderer extends BlockModelBase {

	/** The model's boxes in pixels: from x y z, to x y z. */
	private static final double[][] BOXES = {{6, 12, 6, 10, 16, 10}, {7, 0, 7, 9, 12, 9}};
	/** Each box's faces as down, up, north, south, west, east: u1 v1 u2 v2 in pixels, or null for none. */
	private static final double[][][] UVS = {
		{{0, 0, 4, 4}, {4, 4, 0, 0}, {0, 0, 4, 4}, {0, 0, 4, 4}, {0, 0, 4, 4}, {0, 0, 4, 4}},
		{{0, 4, 2, 6}, null, {0, 4, 2, 16}, {0, 4, 2, 16}, {0, 4, 2, 16}, {0, 4, 2, 16}}};
	/** 1.7.10's shade for a face looking down, up, north, south, west, east. */
	private static final float[] SHADES = {0.5F, 1.0F, 0.8F, 0.8F, 0.6F, 0.6F};

	public BlockLightningRodRenderer(int modelID) {
		super(modelID);
	}

	@Override
	public void renderInventoryBlock(Block block, int meta, int modelID, RenderBlocks renderer) {
		Tessellator tessellator = Tessellator.instance;
		OpenGLHelper.translate(-0.5F, -0.5F, -0.5F);
		tessellator.startDrawingQuads();
		draw(tessellator, renderer.getBlockIconFromSideAndMetadata(block, 0, 1), 1, 0, 0, 0, true);
		tessellator.draw();
		OpenGLHelper.translate(0.5F, 0.5F, 0.5F);
	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
		int meta = world.getBlockMetadata(x, y, z);
		IIcon icon = renderer.hasOverrideBlockTexture() ? renderer.overrideBlockTexture : renderer.getBlockIconFromSideAndMetadata(block, 0, meta);
		Tessellator.instance.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
		draw(Tessellator.instance, icon, meta & 7, x, y, z, false);
		return true;
	}

	@Override
	public boolean shouldRender3DInInventory(int modelId) {
		return true;
	}

	/** The model pointing toward facing (a ForgeDirection), its block's corner at x y z; normals instead of shades for the inventory. */
	private static void draw(Tessellator tessellator, IIcon icon, int facing, double x, double y, double z, boolean inventory) {
		for (int box = 0; box < BOXES.length; box++) {
			double[] b = BOXES[box];
			for (int face = 0; face < 6; face++) {
				double[] uv = UVS[box][face];
				if (uv == null) {
					continue;
				}
				// the face's corners as seen from outside: top left, bottom left, bottom right, top right
				double[][] corners = new double[4][];
				double[][] st = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
				for (int c = 0; c < 4; c++) {
					corners[c] = turn(corner(b, face, st[c][0], st[c][1]), facing);
				}
				int seen = turnSide(face, facing);
				if (inventory) {
					int[] normal = NORMALS[seen];
					tessellator.setNormal(normal[0], normal[1], normal[2]);
				} else {
					tessellator.setColorOpaque_F(SHADES[seen], SHADES[seen], SHADES[seen]);
				}
				for (int c = 0; c < 4; c++) {
					double u = uv[0] + st[c][0] * (uv[2] - uv[0]);
					double v = uv[1] + st[c][1] * (uv[3] - uv[1]);
					tessellator.addVertexWithUV(x + corners[c][0], y + corners[c][1], z + corners[c][2], icon.getInterpolatedU(u), icon.getInterpolatedV(v));
				}
			}
		}
	}

	private static final int[][] NORMALS = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

	/**
	 * A point of a box's face, in blocks, the pointing-up model's: s across it left to right and t down it, as the
	 * face is seen from outside (the up face with north at the top, the down face with south at the top).
	 */
	private static double[] corner(double[] b, int face, double s, double t) {
		double x1 = b[0] / 16, y1 = b[1] / 16, z1 = b[2] / 16, x2 = b[3] / 16, y2 = b[4] / 16, z2 = b[5] / 16;
		switch (face) {
			case 0:
				return new double[] {x1 + s * (x2 - x1), y1, z2 - t * (z2 - z1)};
			case 1:
				return new double[] {x1 + s * (x2 - x1), y2, z1 + t * (z2 - z1)};
			case 2:
				return new double[] {x2 - s * (x2 - x1), y2 - t * (y2 - y1), z1};
			case 3:
				return new double[] {x1 + s * (x2 - x1), y2 - t * (y2 - y1), z2};
			case 4:
				return new double[] {x1, y2 - t * (y2 - y1), z1 + s * (z2 - z1)};
			default:
				return new double[] {x2, y2 - t * (y2 - y1), z2 - s * (z2 - z1)};
		}
	}

	/** A point of the pointing-up model turned about the block's middle to point toward facing (each turn a proper rotation). */
	private static double[] turn(double[] p, int facing) {
		double x = p[0], y = p[1], z = p[2];
		switch (facing) {
			case 0:
				return new double[] {x, 1 - y, 1 - z};
			case 2:
				return new double[] {x, z, 1 - y};
			case 3:
				return new double[] {x, 1 - z, y};
			case 4:
				return new double[] {1 - y, x, z};
			case 5:
				return new double[] {y, 1 - x, z};
			default:
				return p;
		}
	}

	/** Which way a face of the pointing-up model looks once turned toward facing. */
	private static int turnSide(int face, int facing) {
		int[] normal = NORMALS[face];
		double[] tip = turn(new double[] {0.5 + normal[0], 0.5 + normal[1], 0.5 + normal[2]}, facing);
		for (int side = 0; side < 6; side++) {
			int[] n = NORMALS[side];
			if (Math.abs(tip[0] - 0.5 - n[0]) < 0.01 && Math.abs(tip[1] - 0.5 - n[1]) < 0.01 && Math.abs(tip[2] - 0.5 - n[2]) < 0.01) {
				return side;
			}
		}
		return face;
	}
}
