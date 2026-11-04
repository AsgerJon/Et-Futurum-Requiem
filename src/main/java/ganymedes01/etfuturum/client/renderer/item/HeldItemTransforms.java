package ganymedes01.etfuturum.client.renderer.item;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

/**
 * Items held in third person placed as 26.3 places them (SPEC-modern-movement.md §63.6; 26.3 {@code ItemInHandLayer}
 * and the item models' {@code thirdperson_*hand} displays), for players (both hands) and the biped mobs. From the
 * arm: rotate x -90, y 180, move (+-1, 2, -10)/16; then the display of the item's model (26.3 mirrors its x move and
 * its y and z turns for the left hand, after the model's own left-hand values); then the model centred, here 1.7.10's
 * item mapped onto it. Items with their own equipped renderer (EFR's shield, trident) keep 1.7.10's way.
 */
public final class HeldItemTransforms {

	/** move x y z (sixteenths), turn x y z (degrees), scale: the right hand's, then the left hand's as in the model. */
	private static final float[][] GENERATED = {{0, 3, 1, 0, 0, 0, 0.55F}, {0, 3, 1, 0, 0, 0, 0.55F}};
	private static final float[][] HANDHELD = {{0, 4, 0.5F, 0, -90, 55, 0.85F}, {0, 4, 0.5F, 0, 90, -55, 0.85F}};
	private static final float[][] HANDHELD_ROD = {{0, 4, 2.5F, 0, 90, 55, 0.85F}, {0, 4, 2.5F, 0, -90, -55, 0.85F}};
	private static final float[][] BOW = {{-1, -2, 2.5F, -80, 260, -40, 0.9F}, {-1, -2, 2.5F, -80, -280, 40, 0.9F}};
	private static final float[][] BLOCK = {{0, 2.5F, 0, 75, 45, 0, 0.375F}, {0, 2.5F, 0, 75, 45, 0, 0.375F}};

	private static final FloatBuffer ARM = BufferUtils.createFloatBuffer(16);
	private static boolean armNoted;

	private HeldItemTransforms() {
	}

	public static boolean isEnabled() {
		return McFlyConfig.modernHeldItems;
	}

	/** Whether the stack is drawn the modern way: not with its own equipped renderer. */
	public static boolean applies(ItemStack stack) {
		return isEnabled() && stack != null && stack.getItem() != null
				&& MinecraftForgeClient.getItemRenderer(stack, IItemRenderer.ItemRenderType.EQUIPPED) == null;
	}

	/** At the start of an entity's held-item pass. */
	public static void forgetArm() {
		armNoted = false;
	}

	/** Right after the right arm's {@code postRender}: the matrix the item is placed from. */
	public static void noteArm() {
		if (isEnabled()) {
			GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, ARM);
			armNoted = true;
		}
	}

	/** One of the held item's render passes: from the noted arm, the modern way, or 1.7.10's as it was set up. */
	public static void renderFromArm(ItemStack stack, Runnable draw) {
		if (!armNoted || !applies(stack)) {
			draw.run();
			return;
		}
		GL11.glPushMatrix();
		GL11.glLoadMatrix(ARM);
		apply(stack, false);
		draw.run();
		GL11.glPopMatrix();
	}

	/**
	 * From the arm's {@code postRender} to where 1.7.10's {@code ItemRenderer.renderItem} draws the stack as 26.3's
	 * model would be (only for stacks that {@link #applies}).
	 */
	public static void apply(ItemStack stack, boolean leftArm) {
		Item item = stack.getItem();
		boolean block = stack.getItemSpriteNumber() == 0 && item instanceof ItemBlock
				&& RenderBlocks.renderItemIn3d(Block.getBlockFromItem(item).getRenderType());
		float[] display = (block ? BLOCK : item instanceof ItemBow ? BOW : item.isFull3D()
				? item.shouldRotateAroundWhenRendering() ? HANDHELD_ROD : HANDHELD : GENERATED)[leftArm ? 1 : 0];
		float flip = leftArm ? -1.0F : 1.0F;
		GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
		GL11.glTranslatef(flip / 16.0F, 2.0F / 16.0F, -10.0F / 16.0F);
		GL11.glTranslatef(flip * display[0] / 16.0F, display[1] / 16.0F, display[2] / 16.0F);
		GL11.glRotatef(display[3], 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(flip * display[4], 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(flip * display[5], 0.0F, 0.0F, 1.0F);
		GL11.glScalef(display[6], display[6], display[6]);
		if (block) {
			// 1.7.10 draws a block item centred (renderBlockAsItem), where 26.3's model is after its -0.5
			return;
		}
		// 26.3's flat item, centred, reads right from +z; 1.7.10's spans [0,1] x [0,1] x [-1/16,0] mirrored in x,
		// reading right from -z: turned half round about y and moved onto it
		GL11.glTranslatef(0.5F, -0.5F, -1.0F / 32.0F);
		GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
		// undoing ItemRenderer.renderItem's own placing of flat items
		GL11.glTranslatef(0.9375F, 0.0625F, 0.0F);
		GL11.glRotatef(-335.0F, 0.0F, 0.0F, 1.0F);
		GL11.glRotatef(-50.0F, 0.0F, 1.0F, 0.0F);
		GL11.glScalef(1.0F / 1.5F, 1.0F / 1.5F, 1.0F / 1.5F);
		GL11.glTranslatef(0.0F, 0.3F, 0.0F);
	}
}
