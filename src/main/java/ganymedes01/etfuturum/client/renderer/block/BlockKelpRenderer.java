package ganymedes01.etfuturum.client.renderer.block;

import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;

/**
 * Kelp (§29): the plant as crossed squares in the cutout pass, the water it stands in as a source in the translucent
 * pass, like the bubble column. Flat in the inventory (its item's icon).
 */
@ThreadSafeISBRH(perThread = false)
public class BlockKelpRenderer implements ISimpleBlockRenderingHandler {

	private final int renderId;

	public BlockKelpRenderer(int renderId) {
		this.renderId = renderId;
	}

	@Override
	public void renderInventoryBlock(Block block, int meta, int modelId, RenderBlocks renderer) {
		// flat in the inventory: Forge draws the item's icon, never this
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

	@Override
	public boolean shouldRender3DInInventory(int modelId) {
		return false;
	}

	@Override
	public int getRenderId() {
		return renderId;
	}
}
