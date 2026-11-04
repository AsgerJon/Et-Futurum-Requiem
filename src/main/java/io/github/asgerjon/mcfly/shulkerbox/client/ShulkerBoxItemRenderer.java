package io.github.asgerjon.mcfly.shulkerbox.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.shulkerbox.ShulkerBoxItem;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

/**
 * A shulker box as an item (SPEC-modern-movement.md §81): the box closed, facing up, in its colour, as a block item is
 * drawn (centred in the slot and on the ground, from its corner in the hand).
 */
@SideOnly(Side.CLIENT)
public class ShulkerBoxItemRenderer implements IItemRenderer {

	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return type != ItemRenderType.FIRST_PERSON_MAP;
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return true;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		ShulkerBoxRenderer renderer = (ShulkerBoxRenderer) TileEntityRendererDispatcher.instance.getSpecialRendererByClass(io.github.asgerjon.mcfly.shulkerbox.ShulkerBoxBlockEntity.class);
		if (renderer == null) {
			return;
		}
		boolean centred = type == ItemRenderType.ENTITY || type == ItemRenderType.INVENTORY;
		float at = centred ? -0.5F : 0.0F;
		GL11.glPushMatrix();
		renderer.draw(at, at, at, 1, 0.0F, ShulkerBoxItem.colour(item));
		GL11.glPopMatrix();
	}
}
