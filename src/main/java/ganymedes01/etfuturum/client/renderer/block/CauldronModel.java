package ganymedes01.etfuturum.client.renderer.block;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCauldron;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

/**
 * 26.3's {@code block/cauldron} model drawn in 1.7.10 (SPEC-modern-movement.md §63.5), for 1.7.10's cauldron and EFR's
 * (lava, powder snow, potion and dyed water; all draw with the cauldron's icons): walls 2 thick from 3 to 16, a floor
 * from 3 to 4 inside, L-shaped legs at the corners; no ambient occlusion, faces shaded as 1.7.10's flat blocks; the
 * contents one face at their height over 2 to 14. A face on the block's edge is culled by its neighbour and lit by it,
 * as 26.3's {@code cullface} faces are.
 */
public final class CauldronModel {

	private static final int NONE = 0, SIDE = 1, TOP = 2, BOTTOM = 3, INSIDE = 4;

	/** from x y z, to x y z (sixteenths), then the texture of the down, up, north, south, west, east faces. */
	private static final int[][] ELEMENTS = {
			{0, 3, 0, 2, 16, 16, INSIDE, TOP, SIDE, SIDE, SIDE, SIDE},
			{2, 3, 2, 14, 4, 14, INSIDE, INSIDE, NONE, NONE, NONE, NONE},
			{14, 3, 0, 16, 16, 16, INSIDE, TOP, SIDE, SIDE, SIDE, SIDE},
			{2, 3, 0, 14, 16, 2, INSIDE, TOP, SIDE, SIDE, NONE, NONE},
			{2, 3, 14, 14, 16, 16, INSIDE, TOP, SIDE, SIDE, NONE, NONE},
			{0, 0, 0, 4, 3, 2, BOTTOM, NONE, SIDE, SIDE, SIDE, SIDE},
			{0, 0, 2, 2, 3, 4, BOTTOM, NONE, NONE, SIDE, SIDE, SIDE},
			{12, 0, 0, 16, 3, 2, BOTTOM, NONE, SIDE, SIDE, SIDE, SIDE},
			{14, 0, 2, 16, 3, 4, BOTTOM, NONE, NONE, SIDE, SIDE, SIDE},
			{0, 0, 14, 4, 3, 16, BOTTOM, NONE, SIDE, SIDE, SIDE, SIDE},
			{0, 0, 12, 2, 3, 14, BOTTOM, NONE, SIDE, NONE, SIDE, SIDE},
			{12, 0, 14, 16, 3, 16, BOTTOM, NONE, SIDE, SIDE, SIDE, SIDE},
			{14, 0, 12, 16, 3, 14, BOTTOM, NONE, SIDE, NONE, SIDE, SIDE}};

	/** 1.7.10's (and 26.3's) flat shading: down, up, north, south, west, east. */
	private static final float[] SHADE = {0.5F, 1.0F, 0.8F, 0.8F, 0.6F, 0.6F};
	/** 1.7.10's side numbers for down, up, north, south, west, east. */
	private static final int[] SIDES = {0, 1, 2, 3, 4, 5};
	private static final int[][] OFFSETS = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

	private CauldronModel() {
	}

	public static boolean isEnabled() {
		return McFlyConfig.modernCauldrons;
	}

	/** The empty cauldron; leaves the colour white (the block's multiplier) and the brightness its own. */
	public static void render(IBlockAccess world, int x, int y, int z, Block block, RenderBlocks renderer) {
		Tessellator tessellator = Tessellator.instance;
		float[] colour = colour(world, x, y, z, block);
		IIcon[] icons = {null, block.getIcon(2, 0), block.getIcon(1, 0), block.getIcon(0, 0), BlockCauldron.getCauldronIcon("inner")};
		int own = block.getMixedBrightnessForBlock(world, x, y, z);
		boolean ao = renderer.enableAO;
		renderer.enableAO = false;
		for (int[] element : ELEMENTS) {
			renderer.setRenderBounds(element[0] / 16.0D, element[1] / 16.0D, element[2] / 16.0D,
					element[3] / 16.0D, element[4] / 16.0D, element[5] / 16.0D);
			for (int face = 0; face < 6; face++) {
				int texture = element[6 + face];
				if (texture == NONE) {
					continue;
				}
				boolean edge = onEdge(element, face);
				int nx = x + OFFSETS[face][0], ny = y + OFFSETS[face][1], nz = z + OFFSETS[face][2];
				if (edge && !renderer.renderAllFaces && !block.shouldSideBeRendered(world, nx, ny, nz, SIDES[face])) {
					continue;
				}
				tessellator.setBrightness(edge ? block.getMixedBrightnessForBlock(world, nx, ny, nz) : own);
				float shade = SHADE[face];
				tessellator.setColorOpaque_F(colour[0] * shade, colour[1] * shade, colour[2] * shade);
				IIcon icon = icons[texture];
				switch (face) {
					case 0 -> renderer.renderFaceYNeg(block, x, y, z, icon);
					case 1 -> renderer.renderFaceYPos(block, x, y, z, icon);
					case 2 -> renderer.renderFaceZNeg(block, x, y, z, icon);
					case 3 -> renderer.renderFaceZPos(block, x, y, z, icon);
					case 4 -> renderer.renderFaceXNeg(block, x, y, z, icon);
					default -> renderer.renderFaceXPos(block, x, y, z, icon);
				}
			}
		}
		renderer.enableAO = ao;
		renderer.setRenderBoundsFromBlock(block);
		tessellator.setBrightness(own);
		tessellator.setColorOpaque_F(colour[0], colour[1], colour[2]);
	}

	/**
	 * The contents' face at {@code height} (of the block) in the colour the caller set: 26.3's over 2 to 14 when the
	 * modern model is on, else 1.7.10's over the whole block.
	 */
	public static void content(RenderBlocks renderer, Block block, int x, int y, int z, double height, IIcon icon) {
		if (isEnabled()) {
			renderer.setRenderBounds(2.0D / 16.0D, 0.0D, 2.0D / 16.0D, 14.0D / 16.0D, height, 14.0D / 16.0D);
			renderer.renderFaceYPos(block, x, y, z, icon);
			renderer.setRenderBoundsFromBlock(block);
		} else {
			renderer.renderFaceYPos(block, x, y - 1.0D + height, z, icon);
		}
	}

	private static boolean onEdge(int[] element, int face) {
		return switch (face) {
			case 0 -> element[1] == 0;
			case 1 -> element[4] == 16;
			case 2 -> element[2] == 0;
			case 3 -> element[5] == 16;
			case 4 -> element[0] == 0;
			default -> element[3] == 16;
		};
	}

	private static float[] colour(IBlockAccess world, int x, int y, int z, Block block) {
		int multiplier = block.colorMultiplier(world, x, y, z);
		float r = (multiplier >> 16 & 255) / 255.0F, g = (multiplier >> 8 & 255) / 255.0F, b = (multiplier & 255) / 255.0F;
		if (EntityRenderer.anaglyphEnable) {
			float ar = (r * 30.0F + g * 59.0F + b * 11.0F) / 100.0F;
			float ag = (r * 30.0F + g * 70.0F) / 100.0F;
			float ab = (r * 30.0F + b * 70.0F) / 100.0F;
			r = ar;
			g = ag;
			b = ab;
		}
		return new float[]{r, g, b};
	}
}
