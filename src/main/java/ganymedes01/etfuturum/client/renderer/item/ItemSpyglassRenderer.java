package ganymedes01.etfuturum.client.renderer.item;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.client.Spyglass;
import ganymedes01.etfuturum.client.model.SpyglassModel;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * The spyglass in hand, as 26.3 draws it (SPEC-modern-movement.md §64.5): the 3D `spyglass_in_hand` model, moved 2/16
 * down in third person, as it is in first person; while looked through, at the holder's eye (26.3
 * {@code PlayerItemInHandLayer.renderItemHeldToEye}) instead of in the hand. In the inventory, on the ground and in
 * frames the flat sprite (not handled here). The placement follows the trident's (ItemTridentRenderer).
 */
public class ItemSpyglassRenderer implements IItemRenderer {

	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		// first person needs the hand's space the combat update keeps (combat.client.MixinItemRenderer)
		return type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON && McFlyConfig.modernCombat;
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return true;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
		GL11.glPushMatrix();
		GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		// the off-hand mixin draws the left arm's item in a mirrored space, with the front face flipped
		boolean leftArm = GL11.glGetInteger(GL11.GL_FRONT_FACE) == GL11.GL_CW;
		EntityPlayer holder = data.length > 1 && data[1] instanceof EntityPlayer player ? player : null;
		if (type == ItemRenderType.EQUIPPED) {
			if (holder != null && Spyglass.isScoping(holder) && Spyglass.usesRightArm(holder) != leftArm && !holder.isSwingInProgress
					&& Spyglass.loadHead(holder)) {
				// at the eye: the head (its pitch held to -30..90 degrees), CustomHeadLayer.translateToHead, to the
				// side by 2.5 pixels, up by one, then the model's `head` display; the head's space isn't mirrored
				if (leftArm) {
					GL11.glFrontFace(GL11.GL_CCW);
				}
				GL11.glTranslatef(0.0F, -0.25F, 0.0F);
				GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
				GL11.glScalef(0.625F, -0.625F, -0.625F);
				GL11.glTranslatef((leftArm ? -2.5F : 2.5F) / 16.0F, -1.0F / 16.0F, 0.0F);
				ItemShieldRenderer.display(false, 0.0F, 0.0F, -16.0F, 90.0F, 0.0F, 0.0F, 1.6F);
			} else {
				ItemShieldRenderer.undoHeldBlockPlacement();
				unmirror(leftArm);
				GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
				GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
				GL11.glTranslatef((leftArm ? -1.0F : 1.0F) / 16.0F, 0.125F, -0.625F);
				ItemShieldRenderer.display(leftArm, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
			}
		} else {
			// back to the hand's space kept by combat.client.MixinItemRenderer, then 26.3's first-person placement
			ItemShieldRenderer.FIRST_PERSON_HAND.rewind();
			GL11.glLoadMatrix(ItemShieldRenderer.FIRST_PERSON_HAND);
			unmirror(leftArm);
			int invert = leftArm ? -1 : 1;
			GL11.glTranslatef(invert * 0.56F, -0.52F - (1.0F - ItemShieldRenderer.firstPersonRaise) * 0.6F, -0.72F);
			if (holder != null && ItemTridentRenderer.swingsArm(holder, leftArm)) {
				ItemTridentRenderer.swing(holder.getSwingProgress(ItemTridentRenderer.partialTicks()), invert);
			}
			// the model has no first-person display: only the centring
			ItemShieldRenderer.display(leftArm, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
		}
		SpyglassModel.render();
		if (leftArm) {
			GL11.glFrontFace(GL11.GL_CW);
		}
		GL11.glPopAttrib();
		GL11.glPopMatrix();
	}

	/** Undoes the off-hand mixin's mirror for the left arm, so 26.3's left-hand transforms apply as written. */
	private static void unmirror(boolean leftArm) {
		if (leftArm) {
			GL11.glScalef(-1.0F, 1.0F, 1.0F);
			GL11.glFrontFace(GL11.GL_CCW);
		}
	}
}
