package io.github.asgerjon.mcfly.wood.client;

import io.github.asgerjon.mcfly.wood.Shelves;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;

/**
 * A shelf's three items (SPEC-modern-movement.md 91.10; 26.3's `ShelfRenderer`): from the block's middle, turned to face
 * out, at x -0.3125, 0, 0.3125 by slot and 0.25 toward the front, a quarter of a block (half a framed item's size, as
 * 1.7.10 draws framed items at about half a block); blocks turned half a turn (`block/block`'s `on_shelf`), set on the
 * bottom with `align_items_to_bottom`.
 */
public class ShelfRenderer extends TileEntitySpecialRenderer {

	/** A facing's 26.3 `toYRot` (0 north 180, 1 south 0, 2 west 90, 3 east 270). */
	private static final float[] Y_ROT = {180.0F, 0.0F, 90.0F, 270.0F};

	/** On the client in init. */
	public static void register() {
		cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(Shelves.Tile.class, new ShelfRenderer());
	}

	@Override
	public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
		if (!(tile instanceof Shelves.Tile shelf) || tile.getWorldObj() == null) {
			return;
		}
		float yRot = -Y_ROT[tile.getBlockMetadata() & 3];
		for (int slot = 0; slot < 3; slot++) {
			ItemStack stack = shelf.getStackInSlot(slot);
			if (stack == null) {
				continue;
			}
			GL11.glPushMatrix();
			GL11.glTranslated(x + 0.5D, y + 0.5D, z + 0.5D);
			GL11.glRotatef(yRot, 0.0F, 1.0F, 0.0F);
			GL11.glTranslatef((slot - 1) * 0.3125F, shelf.alignItemsToBottom ? -0.25F : 0.0F, -0.25F);
			if (stack.getItem() instanceof ItemBlock) {
				GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
			}
			GL11.glScalef(0.5F, 0.5F, 0.5F);
			// framed items stand on their middle: bring it to the shelf's centre line
			GL11.glTranslatef(0.0F, shelf.alignItemsToBottom ? 0.0F : -0.25F, 0.0F);
			EntityItem item = new EntityItem(tile.getWorldObj(), 0.0D, 0.0D, 0.0D, stack.copy());
			item.getEntityItem().stackSize = 1;
			item.hoverStart = 0.0F;
			RenderItem.renderInFrame = true;
			RenderManager.instance.renderEntityWithPosYaw(item, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F);
			RenderItem.renderInFrame = false;
			GL11.glPopMatrix();
		}
	}
}
