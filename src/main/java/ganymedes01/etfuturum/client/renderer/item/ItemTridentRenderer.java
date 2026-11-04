package ganymedes01.etfuturum.client.renderer.item;

import ganymedes01.etfuturum.client.model.ModelTrident;
import ganymedes01.etfuturum.items.equipment.ItemTrident;
import ganymedes01.etfuturum.offhand.Arm;
import ganymedes01.etfuturum.offhand.Hand;
import ganymedes01.etfuturum.offhand.Offhand;
import ganymedes01.etfuturum.trident.Riptide;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * The trident in hand, as modern draws it (SPEC-modern-movement.md §58.1): the 3D model with the `trident_in_hand`
 * transforms, `trident_throwing` in third person while charging, and modern's first-person charge (pulled back over 10
 * ticks, shaking). In the inventory, on the ground and in item frames it stays the flat sprite (not handled here). The
 * placement follows ItemShieldRenderer's, which this shares.
 */
public class ItemTridentRenderer implements IItemRenderer {

	private final ModelTrident model = new ModelTrident();

	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON;
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
		boolean charging = isChargingWithArm(holder, leftArm);
		if (type == ItemRenderType.EQUIPPED) {
			ItemShieldRenderer.undoHeldBlockPlacement();
			unmirror(leftArm);
			// modern's item in hand, third person: from the arm, turned to point along it, then the trident's display
			GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
			GL11.glTranslatef((leftArm ? -1.0F : 1.0F) / 16.0F, 0.125F, -0.625F);
			if (charging) {
				ItemShieldRenderer.display(leftArm, 8.0F, -17.0F, leftArm ? -7.0F : 9.0F, 0.0F, 90.0F, 180.0F, 1.0F);
			} else {
				ItemShieldRenderer.display(leftArm, leftArm ? 3.0F : 11.0F, 17.0F, leftArm ? 12.0F : -2.0F, 0.0F, 60.0F, 0.0F, 1.0F);
			}
		} else {
			// back to the hand's space kept by combat.client.MixinItemRenderer, then modern's first-person placement
			ItemShieldRenderer.FIRST_PERSON_HAND.rewind();
			GL11.glLoadMatrix(ItemShieldRenderer.FIRST_PERSON_HAND);
			unmirror(leftArm);
			int invert = leftArm ? -1 : 1;
			GL11.glTranslatef(invert * 0.56F, -0.52F - (1.0F - ItemShieldRenderer.firstPersonRaise) * 0.6F, -0.72F);
			if (charging) {
				chargeTransform(holder, stack, invert);
			} else if (holder != null && Riptide.isSpinning(holder)) {
				// modern's hand in a Riptide spin (§58.2)
				GL11.glTranslatef(invert * -0.4F, 0.8F, 0.3F);
				GL11.glRotatef(invert * 65.0F, 0.0F, 1.0F, 0.0F);
				GL11.glRotatef(invert * -85.0F, 0.0F, 0.0F, 1.0F);
			} else if (holder != null && swingsArm(holder, leftArm)) {
				swing(holder.getSwingProgress(partialTicks()), invert);
			}
			ItemShieldRenderer.display(leftArm, leftArm ? 13.0F : -3.0F, 17.0F, 1.0F, 0.0F, leftArm ? 90.0F : -90.0F, leftArm ? -25.0F : 25.0F, 1.0F);
		}
		GL11.glScalef(1.0F, -1.0F, -1.0F);
		model.render(stack.hasEffect(0));
		if (leftArm) {
			GL11.glFrontFace(GL11.GL_CW);
		}
		GL11.glPopAttrib();
		GL11.glPopMatrix();
	}

	/** Undoes the off-hand mixin's mirror for the left arm, so modern's left-hand transforms apply as written. */
	private static void unmirror(boolean leftArm) {
		if (leftArm) {
			GL11.glScalef(-1.0F, 1.0F, 1.0F);
			GL11.glFrontFace(GL11.GL_CCW);
		}
	}

	/** Modern's first-person trident charge: raised and turned, pulled back and stretched over 10 ticks, shaking. */
	private static void chargeTransform(EntityPlayer holder, ItemStack stack, int invert) {
		GL11.glTranslatef(invert * -0.5F, 0.7F, 0.1F);
		GL11.glRotatef(-55.0F, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(invert * 35.3F, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(invert * -9.785F, 0.0F, 0.0F, 1.0F);
		float timeHeld = stack.getMaxItemUseDuration() - (holder.getItemInUseCount() - partialTicks() + 1.0F);
		float power = Math.min(timeHeld / 10.0F, 1.0F);
		if (power > 0.1F) {
			float shake = MathHelper.sin((timeHeld - 0.1F) * 1.3F) * (power - 0.1F);
			GL11.glTranslatef(0.0F, shake * 0.004F, 0.0F);
		}
		GL11.glTranslatef(0.0F, 0.0F, power * 0.2F);
		GL11.glScalef(1.0F, 1.0F, 1.0F + power * 0.2F);
		GL11.glRotatef(invert * -45.0F, 0.0F, 1.0F, 0.0F);
	}

	/** Modern's swing of a held item in first person. */
	static void swing(float progress, int invert) {
		float root = MathHelper.sqrt_float(progress);
		GL11.glTranslatef(invert * -0.4F * MathHelper.sin(root * (float) Math.PI), 0.2F * MathHelper.sin(root * (float) Math.PI * 2.0F),
			-0.2F * MathHelper.sin(progress * (float) Math.PI));
		GL11.glRotatef(invert * (45.0F + MathHelper.sin(progress * progress * (float) Math.PI) * -20.0F), 0.0F, 1.0F, 0.0F);
		float sideways = MathHelper.sin(root * (float) Math.PI);
		GL11.glRotatef(invert * sideways * -20.0F, 0.0F, 0.0F, 1.0F);
		GL11.glRotatef(sideways * -80.0F, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(invert * -45.0F, 0.0F, 1.0F, 0.0F);
	}

	/** The holder is charging a trident with the hand on this arm. */
	public static boolean isChargingWithArm(EntityPlayer holder, boolean leftArm) {
		if (holder == null || !holder.isUsingItem() || holder.getItemInUse() == null || !(holder.getItemInUse().getItem() instanceof ItemTrident)) {
			return false;
		}
		return armOf(holder, Offhand.getUsingHand(holder)) == (leftArm ? Arm.LEFT : Arm.RIGHT);
	}

	static boolean swingsArm(EntityPlayer holder, boolean leftArm) {
		return holder.isSwingInProgress && armOf(holder, Offhand.getSwingingHand(holder)) == (leftArm ? Arm.LEFT : Arm.RIGHT);
	}

	private static Arm armOf(EntityPlayer holder, Hand hand) {
		return hand.arm(Offhand.isEnabled() && Offhand.isLeftHanded(holder));
	}

	static float partialTicks() {
		return Minecraft.getMinecraft().timer.renderPartialTicks;
	}
}
