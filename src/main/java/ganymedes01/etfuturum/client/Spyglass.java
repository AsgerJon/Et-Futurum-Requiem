package ganymedes01.etfuturum.client;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ganymedes01.etfuturum.items.ItemSpyglass;
import ganymedes01.etfuturum.offhand.Arm;
import ganymedes01.etfuturum.offhand.Hand;
import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

/**
 * Looking through the spyglass on the client (SPEC-modern-movement.md §64.5; 26.3 {@code Hud.extractSpyglassOverlay},
 * {@code MouseHandler.turnPlayer}, {@code AbstractClientPlayer.getFieldOfViewModifier},
 * {@code FirstPersonHandsAndItemsRenderer}): in first person the scope over the screen, the hands hidden, the mouse
 * turning an eighth as far (the zoom is {@code spyglass.client.MixinEntityPlayerSP}'s, the turn
 * {@code MixinEntityRenderer}'s).
 */
public final class Spyglass {

	public static final Spyglass INSTANCE = new Spyglass();

	private static final ResourceLocation SCOPE = new ResourceLocation("textures/misc/spyglass_scope.png");

	/** 26.3 {@code Hud.scopeScale}: eases to 1.125 each frame, back to 0.5 in first person when not scoping. */
	private static float scopeScale = 0.5F;
	private static long lastFrame = -1L;

	private Spyglass() {
	}

	public static boolean isScoping(EntityPlayer player) {
		return ItemSpyglass.isScoping(player);
	}

	private static boolean firstPerson() {
		return Minecraft.getMinecraft().gameSettings.thirdPersonView == 0;
	}

	public static boolean scopingInFirstPerson() {
		Minecraft mc = Minecraft.getMinecraft();
		return mc.thePlayer != null && firstPerson() && isScoping(mc.thePlayer);
	}

	/** The mouse's turn: an eighth while scoping in first person (the sensitivity cubed without its x8), unless cinematic. */
	public static float turnScale() {
		return scopingInFirstPerson() && !Minecraft.getMinecraft().gameSettings.smoothCamera ? 0.125F : 1.0F;
	}

	public static float scopeScale() {
		return scopeScale;
	}

	public static boolean hidesHands() {
		return scopingInFirstPerson();
	}

	/** Whether the player's spyglass arm is the right one (the using hand, by EFR's off-hand and main arm). */
	public static boolean usesRightArm(EntityPlayer player) {
		if (!Offhand.isEnabled()) {
			return true;
		}
		Hand hand = Offhand.getUsingHand(player);
		return (hand == null ? Hand.MAIN : hand).arm(Offhand.isLeftHanded(player)) == Arm.RIGHT;
	}

	private static final FloatBuffer HEAD = BufferUtils.createFloatBuffer(16);
	private static EntityPlayer headOf;

	/**
	 * At the start of a player's held items, if the player scopes: the head's matrix, its pitch held to -30..90 degrees
	 * (26.3 {@code renderItemHeldToEye}), for the spyglass at the eye.
	 */
	public static void noteHead(ModelBiped model, EntityPlayer player) {
		headOf = null;
		if (!isScoping(player)) {
			return;
		}
		GL11.glPushMatrix();
		ModelRenderer head = model.bipedHead;
		float pitch = head.rotateAngleX;
		head.rotateAngleX = MathHelper.clamp_float(pitch, (float) (-Math.PI / 6.0D), (float) (Math.PI / 2.0D));
		head.postRender(0.0625F);
		head.rotateAngleX = pitch;
		HEAD.rewind();
		GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, HEAD);
		GL11.glPopMatrix();
		headOf = player;
	}

	/** Loads the noted head's matrix for this player; false if there is none. */
	public static boolean loadHead(EntityPlayer player) {
		if (headOf != player) {
			return false;
		}
		HEAD.rewind();
		GL11.glLoadMatrix(HEAD);
		return true;
	}

	/** Before powder snow's frost, as 26.3 draws them. */
	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onOverlay(RenderGameOverlayEvent.Post event) {
		if (event.type != RenderGameOverlayEvent.ElementType.HELMET) {
			return;
		}
		long now = Minecraft.getSystemTime();
		float ticks = lastFrame < 0L ? 0.0F : Math.min((now - lastFrame) / 50.0F, 10.0F);
		lastFrame = now;
		scopeScale += Math.min(0.5F * ticks, 1.0F) * (1.125F - scopeScale);
		if (!firstPerson()) {
			return;
		}
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.thePlayer != null && isScoping(mc.thePlayer)) {
			drawScope(event.resolution, scopeScale);
		} else {
			scopeScale = 0.5F;
		}
	}

	@SubscribeEvent
	public void onHand(RenderHandEvent event) {
		if (hidesHands()) {
			event.setCanceled(true);
		}
	}

	/** A square of the screen's short side times the scale, in the middle; black round it. */
	private static void drawScope(ScaledResolution resolution, float scale) {
		int screenWidth = resolution.getScaledWidth(), screenHeight = resolution.getScaledHeight();
		float side = Math.min(screenWidth, screenHeight);
		int width = MathHelper.floor_float(side * scale), height = width;
		int left = (screenWidth - width) / 2, top = (screenHeight - height) / 2, right = left + width, bottom = top + height;
		Minecraft mc = Minecraft.getMinecraft();
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(false);
		GL11.glEnable(GL11.GL_BLEND);
		OpenGlHelper.glBlendFunc(770, 771, 1, 0);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		mc.getTextureManager().bindTexture(SCOPE);
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		tessellator.addVertexWithUV(left, bottom, -90.0D, 0.0D, 1.0D);
		tessellator.addVertexWithUV(right, bottom, -90.0D, 1.0D, 1.0D);
		tessellator.addVertexWithUV(right, top, -90.0D, 1.0D, 0.0D);
		tessellator.addVertexWithUV(left, top, -90.0D, 0.0D, 0.0D);
		tessellator.draw();
		Gui.drawRect(0, bottom, screenWidth, screenHeight, 0xFF000000);
		Gui.drawRect(0, 0, screenWidth, top, 0xFF000000);
		Gui.drawRect(0, top, left, bottom, 0xFF000000);
		Gui.drawRect(right, top, screenWidth, bottom, 0xFF000000);
		GL11.glDepthMask(true);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	}
}
