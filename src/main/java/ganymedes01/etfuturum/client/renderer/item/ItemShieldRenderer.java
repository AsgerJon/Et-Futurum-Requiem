package ganymedes01.etfuturum.client.renderer.item;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.client.model.ModelShield;
import ganymedes01.etfuturum.items.equipment.ItemShield;
import ganymedes01.etfuturum.lib.Reference;
import ganymedes01.etfuturum.offhand.Arm;
import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.FloatBuffer;

/**
 * The shield model in each view (spec §46.9), placed as modern places it: in the inventory from the slot (1.7.10 gives
 * slot coordinates there); in third person by undoing 1.7.10's held-block placement back to the arm; in first person
 * from the hand's space kept where 1.7.10 starts drawing the item. Raised while blocking. On the ground from the
 * item's bottom, with modern's lift and ground display; in an item frame as modern's frames place items.
 */
@SideOnly(Side.CLIENT)
public class ItemShieldRenderer implements IItemRenderer {

	private static final ResourceLocation TEXTURE = new ResourceLocation(Reference.MOD_ID, "textures/entity/shield/base_nopattern.png");
	/** The first-person hand's space and raise height (0 lowered to 1 up), kept each frame by combat.client.MixinItemRenderer. */
	public static final FloatBuffer FIRST_PERSON_HAND = BufferUtils.createFloatBuffer(16);
	public static float firstPersonRaise = 1.0F;
	private final ModelShield model = new ModelShield();

	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return type != ItemRenderType.FIRST_PERSON_MAP;
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return type != ItemRenderType.INVENTORY;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
		Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
		GL11.glPushMatrix();
		switch (type) {
			case INVENTORY -> {
				// modern's GUI item: centred in the slot, 16 pixels a unit, y up; then the shield's gui display. The
				// lighting is put back as it was: switching the GUI's item lights off darkened every 3D item drawn
				// after the shield (the held stack, the hotbar's blocks)
				GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LIGHTING_BIT);
				GL11.glEnable(GL11.GL_DEPTH_TEST);
				GL11.glEnable(GL12.GL_RESCALE_NORMAL);
				RenderHelper.enableGUIStandardItemLighting();
				GL11.glTranslatef(8.0F, 8.0F, 50.0F);
				GL11.glScalef(16.0F, -16.0F, 16.0F);
				display(2.0F, 3.0F, 0.0F, 15.0F, -25.0F, -5.0F, 0.65F);
				drawModel();
				GL11.glPopAttrib();
			}
			case ENTITY -> {
				// out of Forge's half size for 3D dropped items, down to the item's bottom (1.7.10 draws a dropped
				// item at its middle, 0.125 up), then modern's: lifted so the model's lowest point is a pixel off the
				// ground (its ground display puts that 0.046875 under the item's origin), and the ground display
				GL11.glScalef(2.0F, 2.0F, 2.0F);
				GL11.glTranslatef(0.0F, -0.125F + 0.046875F + 0.0625F, 0.0F);
				display(2.0F, 4.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.25F);
				drawModel();
			}
			case EQUIPPED -> {
				// the off-hand mixin draws the left arm's item in a mirrored space, with the front face flipped
				boolean leftArm = GL11.glGetInteger(GL11.GL_FRONT_FACE) == GL11.GL_CW;
				undoHeldBlockPlacement();
				if (leftArm) {
					GL11.glScalef(-1.0F, 1.0F, 1.0F);
					GL11.glFrontFace(GL11.GL_CCW);
				}
				// modern's item in hand, third person: from the arm, turned to point along it, then the shield's display
				GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
				GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
				GL11.glTranslatef((leftArm ? -1.0F : 1.0F) / 16.0F, 0.125F, -0.625F);
				boolean raised = isBlockingWithArm(leftArm, data);
				if (raised) {
					display(leftArm, leftArm ? 11.51F : -3.49F, leftArm ? 7.0F : 11.0F, leftArm ? 2.5F : -2.0F, 45.0F, 155.0F, 0.0F, 1.0F);
				} else {
					display(leftArm, 10.0F, 6.0F, leftArm ? 12.0F : -4.0F, 0.0F, 90.0F, 0.0F, 1.0F);
				}
				drawModel();
				if (leftArm) {
					GL11.glFrontFace(GL11.GL_CW);
				}
			}
			case EQUIPPED_FIRST_PERSON -> {
				// back to the hand's space kept by combat.client.MixinItemRenderer, then modern's placement: the arm
				// offset with the raise height, and the shield's first-person display (the swing isn't followed)
				FIRST_PERSON_HAND.rewind();
				GL11.glLoadMatrix(FIRST_PERSON_HAND);
				boolean leftArm = GL11.glGetInteger(GL11.GL_FRONT_FACE) == GL11.GL_CW;
				if (leftArm) {
					GL11.glScalef(-1.0F, 1.0F, 1.0F);
					GL11.glFrontFace(GL11.GL_CCW);
				}
				GL11.glTranslatef((leftArm ? -1.0F : 1.0F) * 0.56F, -0.52F - (1.0F - firstPersonRaise) * 0.6F, -0.72F);
				if (isBlockingWithArm(leftArm, data)) {
					display(leftArm, leftArm ? 5.0F : -15.0F, leftArm ? 5.0F : 3.25F, -11.0F, 0.0F, 180.0F, -5.0F, 1.25F);
				} else {
					display(leftArm, leftArm ? 10.0F : -10.0F, leftArm ? 0.0F : 1.75F, -10.0F, 0.0F, 180.0F, 5.0F, 1.25F);
				}
				drawModel();
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
	 * In an item frame, as modern's ItemFrameRenderer places an item: from the middle of the frame's cell (where
	 * combat.client.MixinRenderItemFrame calls this), turned to face out (modern's 180 - the facing's yaw is 1.7.10's
	 * frame yaw), 7/16 toward the wall, turned by the frame's rotation (1.7.10's quarter turns are two of modern's
	 * eighths), at half size, then the shield's fixed display. 1.7.10 drew framed items as dropped ones, placed by
	 * Forge's in-frame steps.
	 */
	public void renderInFrame(EntityItemFrame frame) {
		Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
		GL11.glPushMatrix();
		GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		GL11.glRotatef(frame.rotationYaw, 0.0F, 1.0F, 0.0F);
		GL11.glTranslatef(0.0F, 0.0F, 0.4375F);
		GL11.glRotatef(frame.getRotation() * 90.0F, 0.0F, 0.0F, 1.0F);
		GL11.glScalef(0.5F, 0.5F, 0.5F);
		display(-4.5F, 4.5F, -5.0F, 0.0F, 180.0F, 0.0F, 0.55F);
		drawModel();
		GL11.glPopAttrib();
		GL11.glPopMatrix();
	}

	/** Modern's display transform (translation in pixels, rotations X, Y, Z, scale), then its centring. */
	private static void display(float x, float y, float z, float rotX, float rotY, float rotZ, float scale) {
		display(false, x, y, z, rotX, rotY, rotZ, scale);
	}

	/** For the left hand modern mirrors the translation's x and the Y and Z rotations. */
	private static void display(boolean leftHand, float x, float y, float z, float rotX, float rotY, float rotZ, float scale) {
		GL11.glTranslatef((leftHand ? -x : x) / 16.0F, y / 16.0F, z / 16.0F);
		GL11.glRotatef(rotX, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(leftHand ? -rotY : rotY, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(leftHand ? -rotZ : rotZ, 0.0F, 0.0F, 1.0F);
		GL11.glScalef(scale, scale, scale);
		GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
	}

	/**
	 * Back to the arm: the inverse of what 1.7.10 does to a held 3D item after the arm's postRender
	 * (RenderPlayer.renderEquippedItems: translate, the block branch's translate, turns and scale; Forge's block
	 * helper's -0.5 centring), so modern's placement can follow.
	 */
	private static void undoHeldBlockPlacement() {
		GL11.glTranslatef(0.5F, 0.5F, 0.5F);
		GL11.glScalef(-1.0F / 0.375F, -1.0F / 0.375F, 1.0F / 0.375F);
		GL11.glRotatef(-45.0F, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(-20.0F, 1.0F, 0.0F, 0.0F);
		GL11.glTranslatef(0.0F, -0.1875F, 0.3125F);
		GL11.glTranslatef(0.0625F, -0.4375F, -0.0625F);
	}

	private void drawModel() {
		GL11.glScalef(1.0F, -1.0F, -1.0F);
		model.render();
	}

	/** The holder (the second render argument in the hand views) has a shield raised. */
	private static boolean isBlocking(Object... data) {
		return data.length > 1 && data[1] instanceof EntityPlayer player && player.isUsingItem() && player.getItemInUse() != null
			&& player.getItemInUse().getItem() instanceof ItemShield;
	}

	/** Raised in this arm: the hand using the shield is the one on this arm. */
	private static boolean isBlockingWithArm(boolean leftArm, Object... data) {
		if (!isBlocking(data)) {
			return false;
		}
		EntityPlayer player = (EntityPlayer) data[1];
		boolean usingLeftArm = Offhand.getUsingHand(player).arm(Offhand.isEnabled() && Offhand.isLeftHanded(player)) == Arm.LEFT;
		return usingLeftArm == leftArm;
	}
}
