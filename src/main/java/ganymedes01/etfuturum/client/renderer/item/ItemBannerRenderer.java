package ganymedes01.etfuturum.client.renderer.item;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.tileentities.TileEntityBanner;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * A banner as an item, as 26.3 draws it (SPEC-modern-movement.md §66.10): its item definition's special banner model
 * (the standing banner, moved half a block in x and z and scaled two thirds, y and z flipped: which EFR's standing
 * banner renderer at the origin is, its 1.8 model sitting 12 pixels lower and lifted half a block), placed by
 * `template_banner.json`'s display for each view, and lit from the front in the GUI (its `gui_light: front`). The
 * places in hand come from the shield's renderer (ItemShieldRenderer), which follows 26.3's item rendering.
 */
public class ItemBannerRenderer implements IItemRenderer {

	private final TileEntityBanner banner = new TileEntityBanner();

	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return type != ItemRenderType.FIRST_PERSON_MAP;
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack stack, ItemRendererHelper helper) {
		return type != ItemRenderType.INVENTORY;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
		banner.setItemValues(stack);
		banner.isStanding = true;
		GL11.glPushMatrix();
		switch (type) {
			case INVENTORY -> {
				// modern's GUI item: centred in the slot, 16 pixels a unit, y up; the gui display; front light
				GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LIGHTING_BIT);
				GL11.glEnable(GL11.GL_DEPTH_TEST);
				GL11.glEnable(GL12.GL_RESCALE_NORMAL);
				frontLight();
				GL11.glTranslatef(8.0F, 8.0F, 50.0F);
				GL11.glScalef(16.0F, -16.0F, 16.0F);
				ItemShieldRenderer.display(false, 0.0F, -3.25F, 0.0F, 30.0F, 20.0F, 0.0F, 0.5325F);
				drawBanner();
				GL11.glPopAttrib();
			}
			case ENTITY -> {
				// out of Forge's quarter size (a dropped block item whose block's render type isn't a cube's, the
				// banner's), down to the item's bottom (1.7.10 draws a dropped item at its middle, 0.125 up), then
				// modern's: lifted so the pole's foot is a pixel off the ground (the ground display puts it 0.0625
				// under the item's origin), and the ground display
				GL11.glScalef(4.0F, 4.0F, 4.0F);
				GL11.glTranslatef(0.0F, -0.125F + 0.0625F + 0.0625F, 0.0F);
				ItemShieldRenderer.display(false, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.25F);
				drawBanner();
			}
			case EQUIPPED -> {
				boolean leftArm = GL11.glGetInteger(GL11.GL_FRONT_FACE) == GL11.GL_CW;
				ItemShieldRenderer.undoHeldBlockPlacement();
				if (leftArm) {
					GL11.glScalef(-1.0F, 1.0F, 1.0F);
					GL11.glFrontFace(GL11.GL_CCW);
				}
				// modern's item in hand, third person: from the arm, turned to point along it, then the banner's display
				GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
				GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
				GL11.glTranslatef((leftArm ? -1.0F : 1.0F) / 16.0F, 0.125F, -0.625F);
				ItemShieldRenderer.display(leftArm, 0.0F, 2.0F, 0.5F, 0.0F, 90.0F, 0.0F, 0.375F);
				drawBanner();
				if (leftArm) {
					GL11.glFrontFace(GL11.GL_CW);
				}
			}
			case EQUIPPED_FIRST_PERSON -> {
				if (!McFlyConfig.modernCombat) {
					// the hand's space is kept by the combat update's mixin: without it, EFR's old place
					GL11.glRotatef(225.0F, 0.0F, 1.0F, 0.0F);
					GL11.glTranslatef(-0.25F, 0.0625F, -1.0F);
					drawBanner();
					break;
				}
				// the hand's space kept by combat.client.MixinItemRenderer, modern's arm offset with the raise height,
				// then the banner's first-person display (the swing isn't followed)
				ItemShieldRenderer.FIRST_PERSON_HAND.rewind();
				GL11.glLoadMatrix(ItemShieldRenderer.FIRST_PERSON_HAND);
				boolean leftArm = GL11.glGetInteger(GL11.GL_FRONT_FACE) == GL11.GL_CW;
				if (leftArm) {
					GL11.glScalef(-1.0F, 1.0F, 1.0F);
					GL11.glFrontFace(GL11.GL_CCW);
				}
				GL11.glTranslatef((leftArm ? -1.0F : 1.0F) * 0.56F, -0.52F - (1.0F - ItemShieldRenderer.firstPersonRaise) * 0.6F, -0.72F);
				ItemShieldRenderer.display(leftArm, 0.0F, 0.0F, 0.0F, 0.0F, 90.0F, 0.0F, 0.375F);
				drawBanner();
				if (leftArm) {
					GL11.glFrontFace(GL11.GL_CW);
				}
			}
			default -> {
			}
		}
		GL11.glPopMatrix();
	}

	/**
	 * In an item frame, as modern's ItemFrameRenderer places an item (the shield's renderInFrame: from the middle of the
	 * frame's cell, where combat.client.MixinRenderItemFrame calls this, turned to face out, 7/16 toward the wall,
	 * turned by the frame's rotation, at half size), then the banner's fixed display.
	 */
	public void renderInFrame(EntityItemFrame frame) {
		banner.setItemValues(frame.getDisplayedItem());
		banner.isStanding = true;
		GL11.glPushMatrix();
		GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		GL11.glRotatef(frame.rotationYaw, 0.0F, 1.0F, 0.0F);
		GL11.glTranslatef(0.0F, 0.0F, 0.4375F);
		GL11.glRotatef(frame.getRotation() * ganymedes01.etfuturum.entities.ModernFrames.degreesPerTurn(), 0.0F, 0.0F, 1.0F);
		GL11.glScalef(0.5F, 0.5F, 0.5F);
		ItemShieldRenderer.display(false, 0.0F, 0.0F, 0.0F, 0.0F, 180.0F, 0.0F, 0.5F);
		drawBanner();
		GL11.glPopAttrib();
		GL11.glPopMatrix();
	}

	/** The special banner model: EFR's standing banner at the origin (see the class's note). */
	private void drawBanner() {
		TileEntityRendererDispatcher.instance.renderTileEntityAt(banner, 0.0, 0.0, 0.0, 0.0F);
	}

	/**
	 * 26.3's flat items' light (Lighting's ITEMS_FLAT): its two lights turned by Y -22.5 and X 135 degrees, in the eye's
	 * space (1.7.10's GUI lights are turned by -30 and 165 for blocks, 26.3's 3D items' light).
	 */
	static void frontLight() {
		GL11.glPushMatrix();
		GL11.glLoadIdentity();
		GL11.glRotatef(-22.5F, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(135.0F, 1.0F, 0.0F, 0.0F);
		RenderHelper.enableStandardItemLighting();
		GL11.glPopMatrix();
	}
}
