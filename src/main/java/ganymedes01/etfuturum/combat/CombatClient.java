package ganymedes01.etfuturum.combat;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.offhand.Arm;
import ganymedes01.etfuturum.offhand.Hand;
import ganymedes01.etfuturum.offhand.Offhand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

/** The attack indicator (spec §46.4): under the crosshair, or beside the hotbar, as attackIndicator says. */
@SideOnly(Side.CLIENT)
public final class CombatClient {

	public static final CombatClient INSTANCE = new CombatClient();

	private static final ResourceLocation CROSSHAIR_FULL = texture("crosshair_attack_indicator_full");
	private static final ResourceLocation CROSSHAIR_BACKGROUND = texture("crosshair_attack_indicator_background");
	private static final ResourceLocation CROSSHAIR_PROGRESS = texture("crosshair_attack_indicator_progress");
	private static final ResourceLocation HOTBAR_BACKGROUND = texture("hotbar_attack_indicator_background");
	private static final ResourceLocation HOTBAR_PROGRESS = texture("hotbar_attack_indicator_progress");

	private CombatClient() {
	}

	private static ResourceLocation texture(String name) {
		return new ResourceLocation(McFly.DOMAIN, "textures/gui/hud/" + name + ".png");
	}

	@SubscribeEvent
	public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.thePlayer == null) {
			return;
		}
		if (event.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS && "crosshair".equals(McFlyConfig.attackIndicator)
				&& mc.gameSettings.thirdPersonView == 0) {
			drawUnderCrosshair(mc, event.resolution.getScaledWidth(), event.resolution.getScaledHeight());
		} else if (event.type == RenderGameOverlayEvent.ElementType.HOTBAR && "hotbar".equals(McFlyConfig.attackIndicator)) {
			drawBesideHotbar(mc, event.resolution.getScaledWidth(), event.resolution.getScaledHeight());
		}
	}

	/** A bar filling under the crosshair while charging; the full icon when aimed at a living thing at full charge. */
	private static void drawUnderCrosshair(Minecraft mc, int width, int height) {
		EntityPlayer player = mc.thePlayer;
		float charge = AttackCooldown.charge(player, 0.0F);
		boolean full = charge >= 1.0F && mc.pointedEntity instanceof EntityLivingBase && mc.pointedEntity.isEntityAlive()
			&& AttackCooldown.delay(player) > 5.0F;
		if (!full && charge >= 1.0F) {
			return;
		}
		int x = width / 2 - 8;
		int y = height / 2 - 7 + 16;
		GL11.glEnable(GL11.GL_BLEND);
		OpenGlHelper.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR, 1, 0);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		if (full) {
			mc.getTextureManager().bindTexture(CROSSHAIR_FULL);
			Gui.func_146110_a(x, y, 0.0F, 0.0F, 16, 16, 16.0F, 16.0F);
		} else {
			mc.getTextureManager().bindTexture(CROSSHAIR_BACKGROUND);
			Gui.func_146110_a(x, y, 0.0F, 0.0F, 16, 4, 16.0F, 4.0F);
			int progress = (int) (charge * 17.0F);
			mc.getTextureManager().bindTexture(CROSSHAIR_PROGRESS);
			Gui.func_146110_a(x, y, 0.0F, 0.0F, progress, 4, 16.0F, 4.0F);
		}
		OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
		GL11.glDisable(GL11.GL_BLEND);
		// the HUD's icons bound again, as the crosshair left them: Forge draws the boss bar next without binding them
		mc.getTextureManager().bindTexture(Gui.icons);
	}

	/** An icon filling from the bottom while charging, on the side of the hotbar away from the off-hand slot. */
	private static void drawBesideHotbar(Minecraft mc, int width, int height) {
		EntityPlayer player = mc.thePlayer;
		float charge = AttackCooldown.charge(player, 0.0F);
		if (charge >= 1.0F) {
			return;
		}
		int centre = width / 2;
		boolean offArmRight = Hand.OFF.arm(Offhand.isEnabled() && Offhand.isLeftHanded(player)) == Arm.RIGHT;
		int x = offArmRight ? centre - 91 - 22 : centre + 91 + 6;
		int y = height - 20;
		int progress = (int) (charge * 19.0F);
		GL11.glEnable(GL11.GL_BLEND);
		OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		mc.getTextureManager().bindTexture(HOTBAR_BACKGROUND);
		Gui.func_146110_a(x, y, 0.0F, 0.0F, 18, 18, 18.0F, 18.0F);
		mc.getTextureManager().bindTexture(HOTBAR_PROGRESS);
		Gui.func_146110_a(x, y + 18 - progress, 0.0F, 18 - progress, 18, progress, 18.0F, 18.0F);
		GL11.glDisable(GL11.GL_BLEND);
		// the HUD's icons bound again, as the hotbar left them, for what Forge draws after it
		mc.getTextureManager().bindTexture(Gui.icons);
	}
}
