package ganymedes01.etfuturum.client.renderer.block;

import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

@ThreadSafeISBRH(perThread = false)
public class BlockLavaCauldronRenderer extends BlockCauldronBaseRenderer {

	public BlockLavaCauldronRenderer(int modelID) {
		super(modelID);
		set2DInventory();
	}

	@Override
	public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
		super.renderWorldBlock(world, x, y, z, block, modelId, renderer);
		IIcon lava = BlockLiquid.getLiquidIcon("lava_still");
		CauldronModel.content(renderer, block, x, y, z, 0.9375D, lava);
		return true;
	}

}
