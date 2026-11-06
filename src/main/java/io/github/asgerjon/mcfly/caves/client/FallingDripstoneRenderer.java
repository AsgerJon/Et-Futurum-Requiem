package io.github.asgerjon.mcfly.caves.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.caves.DripstoneBlocks;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.model.client.ModelDrawer;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** A falling stalactite drawn as its block's 26.3 model (SPEC-modern-movement.md §90.7), as 26.3's falling block renderer draws a block's model. */
@SideOnly(Side.CLIENT)
public final class FallingDripstoneRenderer extends Render {

	public FallingDripstoneRenderer() {
		shadowSize = 0.0F;
	}

	@Override
	public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
		int meta = ((DripstoneBlocks.Falling) entity).meta;
		ModelledBlock block = (ModelledBlock) McFlyBlocks.POINTED_DRIPSTONE.get();
		bindEntityTexture(entity);
		GL11.glPushMatrix();
		GL11.glTranslated(x - 0.5D, y - 0.5D, z - 0.5D);
		GL11.glDisable(GL11.GL_LIGHTING);
		for (ModelPart part : block.itemParts(meta)) {
			ModelDrawer.drawInventory(part.model, part.turnX, part.turnY, block::modelIcon);
		}
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glPopMatrix();
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		return TextureMap.locationBlocksTexture;
	}
}
