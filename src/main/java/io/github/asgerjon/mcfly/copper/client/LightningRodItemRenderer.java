package io.github.asgerjon.mcfly.copper.client;

import ganymedes01.etfuturum.client.renderer.item.ItemShieldRenderer;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * The lightning rod held in the third person (SPEC-modern-movement.md §58.3, §87): 26.3's `thirdperson_righthand`
 * display of its model (0 2 0.5, scale 0.4) from the arm, then the model system's drawing of the rod's item.
 */
public class LightningRodItemRenderer implements IItemRenderer {

	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return type == ItemRenderType.EQUIPPED;
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		// held as a 3D block: the placement ItemShieldRenderer.undoHeldBlockPlacement takes back
		return true;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
		Block block = Block.getBlockFromItem(stack.getItem());
		RenderBlocks renderBlocks = (RenderBlocks) data[0];
		GL11.glPushMatrix();
		GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		// the off-hand mixin draws the left arm's item in a mirrored space, with the front face flipped
		boolean leftArm = GL11.glGetInteger(GL11.GL_FRONT_FACE) == GL11.GL_CW;
		ItemShieldRenderer.undoHeldBlockPlacement();
		if (leftArm) {
			GL11.glScalef(-1.0F, 1.0F, 1.0F);
			GL11.glFrontFace(GL11.GL_CCW);
		}
		// modern's item in hand, third person: from the arm, turned to point along it, then the rod's own display
		GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
		GL11.glTranslatef((leftArm ? -1.0F : 1.0F) / 16.0F, 0.125F, -0.625F);
		ItemShieldRenderer.display(leftArm, 0.0F, 2.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.4F);
		// the model in 0..1 from here: the model system's item drawing centres it, so back by that first
		GL11.glTranslatef(0.5F, 0.5F, 0.5F);
		renderBlocks.renderBlockAsItem(block, 1, 1.0F);
		if (leftArm) {
			GL11.glFrontFace(GL11.GL_CW);
		}
		GL11.glPopAttrib();
		GL11.glPopMatrix();
	}
}
