package ganymedes01.etfuturum.client.renderer.block;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

/**
 * Powder snow as 26.3's {@code block/powder_snow} model (SPEC-modern-movement.md §62.1): each face drawn outward and,
 * 0.002 inside, inward too, both culled by the neighbour on that side, so its walls show from inside it.
 */
public class BlockPowderSnowRenderer implements ISimpleBlockRenderingHandler {

	private static final double INSET = 0.002D;
	/** 1.7.10's face shading: down, up, north, south, west, east. */
	private static final float[] SHADE = {0.5F, 1.0F, 0.8F, 0.8F, 0.6F, 0.6F};

	private final int renderId;

	public BlockPowderSnowRenderer(int renderId) {
		this.renderId = renderId;
	}

	@Override
	public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
		renderer.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
		boolean drawn = renderer.renderStandardBlock(block, x, y, z);
		Tessellator tessellator = Tessellator.instance;
		tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
		int[][] towards = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
		for (int side = 0; side < 6; side++) {
			int nx = x + towards[side][0], ny = y + towards[side][1], nz = z + towards[side][2];
			if (!renderer.renderAllFaces && !block.shouldSideBeRendered(world, nx, ny, nz, side)) {
				continue;
			}
			IIcon icon = renderer.getBlockIcon(block, world, x, y, z, side);
			// the inner face looks the other way: its shade is the opposite side's
			float shade = SHADE[side ^ 1];
			tessellator.setColorOpaque_F(shade, shade, shade);
			switch (side) {
				case 0:
					renderer.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, INSET, 1.0D);
					renderer.renderFaceYPos(block, x, y, z, icon);
					break;
				case 1:
					renderer.setRenderBounds(0.0D, 1.0D - INSET, 0.0D, 1.0D, 1.0D, 1.0D);
					renderer.renderFaceYNeg(block, x, y, z, icon);
					break;
				case 2:
					renderer.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, INSET);
					renderer.renderFaceZPos(block, x, y, z, icon);
					break;
				case 3:
					renderer.setRenderBounds(0.0D, 0.0D, 1.0D - INSET, 1.0D, 1.0D, 1.0D);
					renderer.renderFaceZNeg(block, x, y, z, icon);
					break;
				case 4:
					renderer.setRenderBounds(0.0D, 0.0D, 0.0D, INSET, 1.0D, 1.0D);
					renderer.renderFaceXPos(block, x, y, z, icon);
					break;
				default:
					renderer.setRenderBounds(1.0D - INSET, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
					renderer.renderFaceXNeg(block, x, y, z, icon);
					break;
			}
			drawn = true;
		}
		renderer.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
		return drawn;
	}

	@Override
	public boolean shouldRender3DInInventory(int modelId) {
		return false;
	}

	@Override
	public int getRenderId() {
		return renderId;
	}
}
