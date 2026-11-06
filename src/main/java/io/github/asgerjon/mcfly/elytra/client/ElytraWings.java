package io.github.asgerjon.mcfly.elytra.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.movement.ModernMovement;
import io.github.asgerjon.mcfly.elytra.ElytraAnimationState;
import io.github.asgerjon.mcfly.elytra.ElytraItem;
import io.github.asgerjon.mcfly.elytra.GlidingPlayer;
import io.github.asgerjon.mcfly.spectator.Spectator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderPlayerEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * The wings of a player wearing an elytra (SPEC-modern-movement.md §21.2, §21.3, §85), 26.3's `ElytraModel` drawn as its
 * `WingsLayer` draws it: two 10 by 20 by 2 boxes inflated by 1, pivoting 5 out to each side, the right one mirrored,
 * their angles from the player's animation (down 3 while crouching), 0.125 back; the player's cape as their texture when
 * the cape shows, else the elytra's; the glint when enchanted. Through Forge's `RenderPlayerEvent.Specials`: the cape
 * isn't drawn while an elytra is worn (Pre), the wings are (Post), not on a spectator.
 */
@SideOnly(Side.CLIENT)
public final class ElytraWings {

	public static final ElytraWings INSTANCE = new ElytraWings();

	private static final ResourceLocation ELYTRA = new ResourceLocation("textures/entity/elytra.png");
	private static final ResourceLocation GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");

	private final Model model = new Model();

	private ElytraWings() {
	}

	@SubscribeEvent
	public void hideTheCape(RenderPlayerEvent.Specials.Pre event) {
		if (ElytraItem.worn(event.entityPlayer) != null) {
			event.renderCape = false;
		}
	}

	@SubscribeEvent
	public void drawTheWings(RenderPlayerEvent.Specials.Post event) {
		if (!(event.entityPlayer instanceof AbstractClientPlayer) || Spectator.isSpectator(event.entityPlayer)) {
			return;
		}
		ItemStack elytra = ElytraItem.worn(event.entityPlayer);
		if (elytra != null) {
			draw((AbstractClientPlayer) event.entityPlayer, elytra, event.partialRenderTick);
		}
	}

	private void draw(AbstractClientPlayer player, ItemStack elytra, float partialTicks) {
		ElytraAnimationState animation = ((GlidingPlayer) player).etfu$getElytraAnimation();
		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		boolean cape = player.func_152122_n() && !player.getHideCape();
		Minecraft.getMinecraft().renderEngine.bindTexture(cape ? player.getLocationCape() : ELYTRA);
		GL11.glPushMatrix();
		GL11.glTranslatef(0.0F, 0.0F, 0.125F);
		model.pose(animation.getRotX(partialTicks), animation.getRotY(partialTicks), animation.getRotZ(partialTicks), ModernMovement.isVisuallyCrouching(player));
		model.renderWings();
		if (elytra.isItemEnchanted()) {
			glint(player.ticksExisted + partialTicks);
		}
		GL11.glPopMatrix();
		GL11.glPopAttrib();
	}

	/** The enchantment glint over the wings, in two passes, as 1.7.10 puts it on armour. */
	private void glint(float time) {
		Minecraft.getMinecraft().renderEngine.bindTexture(GLINT);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDepthFunc(GL11.GL_EQUAL);
		GL11.glDepthMask(false);
		for (int pass = 0; pass < 2; pass++) {
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glColor4f(0.38F, 0.19F, 0.608F, 1.0F);
			GL11.glBlendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE);
			GL11.glMatrixMode(GL11.GL_TEXTURE);
			GL11.glLoadIdentity();
			GL11.glScalef(1.0F / 3.0F, 1.0F / 3.0F, 1.0F / 3.0F);
			GL11.glRotatef(30.0F - pass * 60.0F, 0.0F, 0.0F, 1.0F);
			GL11.glTranslatef(0.0F, time * (0.001F + pass * 0.003F) * 20.0F, 0.0F);
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			model.renderWings();
		}
		GL11.glMatrixMode(GL11.GL_TEXTURE);
		GL11.glLoadIdentity();
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glDepthMask(true);
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		GL11.glDisable(GL11.GL_BLEND);
	}

	/** 26.3's `ElytraModel` (64 by 32). */
	private static final class Model extends ModelBase {
		private final ModelRenderer leftWing = new ModelRenderer(this, 22, 0);
		private final ModelRenderer rightWing = new ModelRenderer(this, 22, 0);

		Model() {
			leftWing.addBox(-10.0F, 0.0F, 0.0F, 10, 20, 2, 1.0F);
			leftWing.setRotationPoint(5.0F, 0.0F, 0.0F);
			rightWing.mirror = true;
			rightWing.addBox(0.0F, 0.0F, 0.0F, 10, 20, 2, 1.0F);
			rightWing.setRotationPoint(-5.0F, 0.0F, 0.0F);
		}

		/** 26.3's `setupAnim`: the left wing as given, the right one with its yaw and roll the other way. */
		void pose(float rotX, float rotY, float rotZ, boolean crouching) {
			leftWing.rotationPointY = crouching ? 3.0F : 0.0F;
			leftWing.rotateAngleX = rotX;
			leftWing.rotateAngleY = rotY;
			leftWing.rotateAngleZ = rotZ;
			rightWing.rotationPointY = leftWing.rotationPointY;
			rightWing.rotateAngleX = rotX;
			rightWing.rotateAngleY = -rotY;
			rightWing.rotateAngleZ = -rotZ;
		}

		void renderWings() {
			GL11.glDisable(GL12.GL_RESCALE_NORMAL);
			GL11.glDisable(GL11.GL_CULL_FACE);
			leftWing.render(0.0625F);
			rightWing.render(0.0625F);
		}
	}
}
