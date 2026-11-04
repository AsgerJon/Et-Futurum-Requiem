package ganymedes01.etfuturum.client.renderer.block;

import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.BlockPowderSnowCauldron;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.IBlockAccess;

/** The cauldron, then powder snow as its content at (6 + 3 * level) / 16 (spec §62.8; 26.3's {@code powder_snow_cauldron_level*}). */
public class BlockPowderSnowCauldronRenderer extends BlockCauldronBaseRenderer {

	public BlockPowderSnowCauldronRenderer(int modelID) {
		super(modelID);
		set2DInventory();
	}

	@Override
	public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
		super.renderWorldBlock(world, x, y, z, block, modelId, renderer);
		int level = world.getBlockMetadata(x, y, z);
		Tessellator.instance.setColorOpaque_F(1.0F, 1.0F, 1.0F);
		CauldronModel.content(renderer, block, x, y, z, BlockPowderSnowCauldron.contentHeight16(level) / 16.0D,
				McFlyBlocks.POWDER_SNOW.get().getIcon(1, 0));
		return true;
	}
}
