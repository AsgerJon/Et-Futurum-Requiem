/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Quark Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Quark
 * <p>
 * Quark is Open Source and distributed under the
 * [ADD-LICENSE-HERE]
 * <p>
 * File Created @ [21/03/2016, 00:14:23 (GMT)]
 */
package ganymedes01.etfuturum.client.renderer.entity.elytra;

import ganymedes01.etfuturum.elytra.IElytraPlayer;
import ganymedes01.etfuturum.elytra.ElytraAnimationState;
import ganymedes01.etfuturum.items.equipment.ItemArmorElytra;
import ganymedes01.etfuturum.movement.ModernMovement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class LayerBetterElytra {

	private static final ResourceLocation TEXTURE_ELYTRA = new ResourceLocation("textures/entity/elytra.png");
	private static final ModelElytra modelElytra = new ModelElytra();
	protected static final ResourceLocation ENCHANTED_ITEM_GLINT_RES = new ResourceLocation("textures/misc/enchanted_item_glint.png");

	/**
	 * Draws the wings of a player wearing an elytra, once per frame, in the player model's space (§21.3). Uses the
	 * player's cape texture if they have one and show it, else the elytra texture.
	 * <p>
	 * Angelica hooks this method by its signature (before the first glPushMatrix, after the first glPopAttrib) to give
	 * shader packs a material id for the elytra, so keep the signature and those two calls. Only partialTicks and scale
	 * are used.
	 */
	public static void doRenderLayer(EntityLivingBase entityIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float scale) {
		ItemStack itemstack = ItemArmorElytra.getElytra(entityIn);
		if (itemstack == null || !(entityIn instanceof AbstractClientPlayer) || !(entityIn instanceof IElytraPlayer)) {
			return;
		}
		AbstractClientPlayer player = (AbstractClientPlayer) entityIn;
		ElytraAnimationState animation = ((IElytraPlayer) player).etfu$getElytraAnimation();
		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		boolean showCape = player.func_152122_n()/*hasCape*/ && !player.getHideCape();
		Minecraft.getMinecraft().renderEngine.bindTexture(showCape ? player.getLocationCape() : TEXTURE_ELYTRA);

		GL11.glPushMatrix();
		GL11.glTranslatef(0.0F, 0.0F, 0.125F);
		modelElytra.setWingAngles(animation.getRotX(partialTicks), animation.getRotY(partialTicks), animation.getRotZ(partialTicks),
				ModernMovement.isVisuallyCrouching(player));
		modelElytra.renderWings(scale);

		if (itemstack.isItemEnchanted()) {
			renderGlint(player, partialTicks, scale);
		}

		GL11.glPopMatrix();
		GL11.glPopAttrib();
	}

	private static void renderGlint(AbstractClientPlayer player, float partialTicks, float scale) {
		float f = player.ticksExisted + partialTicks;
		Minecraft.getMinecraft().renderEngine.bindTexture(ENCHANTED_ITEM_GLINT_RES);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDepthFunc(GL11.GL_EQUAL);
		GL11.glDepthMask(false);
		float f1 = 0.5F;
		GL11.glColor4f(f1, f1, f1, 1.0F);

		for (int i = 0; i < 2; ++i) {
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glBlendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE);
			float f2 = 0.76F;
			GL11.glColor4f(0.5F * f2, 0.25F * f2, 0.8F * f2, 1.0F);
			GL11.glMatrixMode(GL11.GL_TEXTURE);
			GL11.glLoadIdentity();
			float f3 = 0.33333334F;
			GL11.glScalef(f3, f3, f3);
			GL11.glRotatef(30.0F - i * 60.0F, 0.0F, 0.0F, 1.0F);
			GL11.glTranslatef(0.0F, f * (0.001F + i * 0.003F) * 20.0F, 0.0F);
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			modelElytra.renderWings(scale);
		}

		GL11.glMatrixMode(GL11.GL_TEXTURE);
		GL11.glLoadIdentity();
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glDepthMask(true);
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		GL11.glDisable(GL11.GL_BLEND);
	}
}
