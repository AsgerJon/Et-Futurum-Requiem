package io.github.asgerjon.mcfly.model.client;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.opengl.GL11;

/**
 * McFly's model renderer (SPEC-modern-movement.md §84): a `ModelledBlock`'s parts in the world and its item's in the
 * inventory, each a 26.3 model drawn by `ModelDrawer`. A breaking block's cracks go over its outline (1.7.10's override
 * texture is drawn as a plain cube there).
 */
@SideOnly(Side.CLIENT)
public final class ModelBlockRenderer implements ISimpleBlockRenderingHandler {

	private final int id;
	private final boolean item3d;

	private ModelBlockRenderer(int id, boolean item3d) {
		this.id = id;
		this.item3d = item3d;
	}

	/** On the client in init (called from McFly: a static call, so a server never loads this class). */
	public static void register() {
		cpw.mods.fml.client.registry.RenderingRegistry.registerBlockHandler(new ModelBlockRenderer(McFlyRenderIds.MODEL, true));
		cpw.mods.fml.client.registry.RenderingRegistry.registerBlockHandler(new ModelBlockRenderer(McFlyRenderIds.MODEL_FLAT_ITEM, false));
	}

	@Override
	public void renderInventoryBlock(Block block, int meta, int modelId, RenderBlocks renderer) {
		ModelledBlock modelled = (ModelledBlock) block;
		GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
		for (ModelPart part : modelled.itemParts(meta)) {
			ModelDrawer.drawInventory(part.model, part.turnX, part.turnY, texture -> modelled.modelIcon(texture, meta));
		}
		GL11.glTranslatef(0.5F, 0.5F, 0.5F);
	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
		if (renderer.hasOverrideBlockTexture()) {
			// the cracks of a block being broken: over its outline
			block.setBlockBoundsBasedOnState(world, x, y, z);
			renderer.setRenderBoundsFromBlock(block);
			return renderer.renderStandardBlock(block, x, y, z);
		}
		ModelledBlock modelled = (ModelledBlock) block;
		boolean drew = false;
		int meta = world.getBlockMetadata(x, y, z);
		double[] offset = modelled.modelOffset(world, x, y, z);
		net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.instance;
		if (offset != null) {
			tessellator.addTranslation((float) offset[0], 0.0F, (float) offset[1]);
		}
		boolean emissive = modelled.emissive(world, x, y, z);
		for (ModelPart part : modelled.modelParts(world, x, y, z)) {
			drew |= ModelDrawer.drawInWorld(world, x, y, z, block, part.model, part.turnX, part.turnY, part.uvlock, texture -> modelled.modelIcon(texture, meta), emissive);
		}
		if (offset != null) {
			tessellator.addTranslation((float) -offset[0], 0.0F, (float) -offset[1]);
		}
		return drew;
	}

	@Override
	public boolean shouldRender3DInInventory(int modelId) {
		return item3d;
	}

	@Override
	public int getRenderId() {
		return id;
	}
}
