package ganymedes01.etfuturum.client.renderer.block;

import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;

/**
 * Kelp (§29): the plant as crossed squares in the cutout pass, the water it stands in as a source in the translucent
 * pass, like the bubble column.
 */
@ThreadSafeISBRH(perThread = false)
public class BlockKelpRenderer extends BlockModelBase {

	public BlockKelpRenderer(int modelID) {
		super(modelID);
		set2DInventory();
	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
		if (ForgeHooksClient.getWorldRenderPass() == 1) {
			return renderer.renderBlockLiquid(Blocks.water, x, y, z);
		}
		Tessellator tessellator = Tessellator.instance;
		tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
		tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
		renderer.drawCrossedSquares(block.getIcon(world, x, y, z, 2), x, y, z, 1.0F);
		return true;
	}
}
