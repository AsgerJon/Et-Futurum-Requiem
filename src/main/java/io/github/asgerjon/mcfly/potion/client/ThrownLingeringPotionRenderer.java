package io.github.asgerjon.mcfly.potion.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.potion.ThrownLingeringPotion;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * A thrown lingering potion (SPEC-modern-movement.md §82): its item, each layer in its colour (the bottle, the liquid),
 * half size, turned to the camera, as 1.7.10 draws a thrown splash potion.
 */
@SideOnly(Side.CLIENT)
public class ThrownLingeringPotionRenderer extends Render {

	@Override
	public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
		ItemStack stack = ((ThrownLingeringPotion) entity).potion();
		if (stack == null || stack.getItem() == null) {
			return;
		}
		int passes = stack.getItem().requiresMultipleRenderPasses() ? stack.getItem().getRenderPasses(stack.getItemDamage()) : 1;
		GL11.glPushMatrix();
		GL11.glTranslatef((float) x, (float) y, (float) z);
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		GL11.glScalef(0.5F, 0.5F, 0.5F);
		bindEntityTexture(entity);
		for (int pass = 0; pass < passes; pass++) {
			IIcon icon = stack.getItem().getIcon(stack, pass);
			if (icon == null) {
				continue;
			}
			int colour = stack.getItem().getColorFromItemStack(stack, pass);
			GL11.glPushMatrix();
			GL11.glColor3f((colour >> 16 & 0xFF) / 255.0F, (colour >> 8 & 0xFF) / 255.0F, (colour & 0xFF) / 255.0F);
			drawFacingCamera(icon);
			GL11.glPopMatrix();
		}
		GL11.glColor3f(1.0F, 1.0F, 1.0F);
		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		GL11.glPopMatrix();
	}

	private void drawFacingCamera(IIcon icon) {
		Tessellator tessellator = Tessellator.instance;
		GL11.glRotatef(180.0F - renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(-renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
		tessellator.startDrawingQuads();
		tessellator.setNormal(0.0F, 1.0F, 0.0F);
		tessellator.addVertexWithUV(-0.5, -0.25, 0.0, icon.getMinU(), icon.getMaxV());
		tessellator.addVertexWithUV(0.5, -0.25, 0.0, icon.getMaxU(), icon.getMaxV());
		tessellator.addVertexWithUV(0.5, 0.75, 0.0, icon.getMaxU(), icon.getMinV());
		tessellator.addVertexWithUV(-0.5, 0.75, 0.0, icon.getMinU(), icon.getMinV());
		tessellator.draw();
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		return TextureMap.locationItemsTexture;
	}
}
