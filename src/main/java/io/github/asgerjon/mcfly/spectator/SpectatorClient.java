package io.github.asgerjon.mcfly.spectator;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;

/**
 * Spectator mode on the client (SPEC-modern-movement.md §83): unseen by players who aren't spectating (but creative
 * ones); seen by spectators as a head only; no hand, hotbar, held item names or armour; a crosshair only over what it
 * can open or follow; no blocks inside it drawn, no fog under liquids, no fire or suffocation overlay; always flying;
 * from a followed entity's eyes (which aren't drawn in first person).
 */
@SideOnly(Side.CLIENT)
public final class SpectatorClient {

	public static final SpectatorClient INSTANCE = new SpectatorClient();

	private boolean canSelect;
	private boolean refreshModel;
	private boolean hadHeldItemTooltips;

	private SpectatorClient() {
	}

	/** The client knows only its own player's game type. */
	static boolean isClientSpectator(EntityPlayer player) {
		Minecraft mc = Minecraft.getMinecraft();
		return player == mc.thePlayer && mc.playerController != null && mc.playerController.currentGameType == Spectator.GAME_TYPE;
	}

	/** The client's mining of a block stops at once. */
	static void stopHitting(EntityPlayer player) {
		Minecraft mc = Minecraft.getMinecraft();
		if (player == mc.thePlayer && mc.playerController != null) {
			mc.playerController.stepSoundTickCounter = -5;
			mc.playerController.isHittingBlock = false;
		}
	}

	/** "Press Sneak to dismount", as following an entity shows it. */
	static void showLetGoHint() {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.ingameGUI != null) {
			mc.ingameGUI.func_110326_a(I18n.format("mount.onboard", GameSettings.getKeyDisplayString(mc.gameSettings.keyBindSneak.getKeyCode())), false);
		}
	}

	private static void showBody(ModelBiped model, boolean shown) {
		model.bipedHead.showModel = shown;
		model.bipedHeadwear.showModel = shown;
		model.bipedBody.showModel = shown;
		model.bipedRightArm.showModel = shown;
		model.bipedLeftArm.showModel = shown;
		model.bipedRightLeg.showModel = shown;
		model.bipedLeftLeg.showModel = shown;
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onRenderPlayer(RenderPlayerEvent.Pre event) {
		EntityPlayer viewer = Minecraft.getMinecraft().thePlayer;
		if (viewer != null && Spectator.isSpectator(event.entityPlayer) && event.entityPlayer != viewer
			&& !Spectator.isSpectator(viewer) && !viewer.capabilities.isCreativeMode) {
			event.setCanceled(true);
			return;
		}
		if (Spectator.following(event.entityPlayer) != null) {
			event.setCanceled(true);
		} else if (Spectator.isSpectator(event.entityPlayer)) {
			showBody(event.renderer.modelBipedMain, false);
			event.renderer.modelBipedMain.bipedHead.showModel = true;
			event.renderer.modelBipedMain.bipedHeadwear.showModel = true;
		} else {
			showBody(event.renderer.modelBipedMain, true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onRenderArmour(RenderPlayerEvent.Specials.Pre event) {
		if (Spectator.isSpectator(event.entityPlayer)) {
			event.setCanceled(true);
		}
	}

	/** The entity followed isn't drawn from its own eyes. */
	@SubscribeEvent
	public void onRenderLiving(RenderLivingEvent.Pre event) {
		Minecraft mc = Minecraft.getMinecraft();
		Entity followed = Spectator.following(mc.thePlayer);
		if (followed != null && followed == event.entity && Spectator.isSpectator(mc.thePlayer) && mc.gameSettings.thirdPersonView == 0) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onOverlay(RenderGameOverlayEvent.Pre event) {
		Minecraft mc = Minecraft.getMinecraft();
		if (!Spectator.isSpectator(mc.thePlayer)) {
			return;
		}
		if (event.type == RenderGameOverlayEvent.ElementType.HOTBAR || event.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS && !canSelect) {
			event.setCanceled(true);
		}
		if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
			hadHeldItemTooltips = mc.gameSettings.heldItemTooltips;
			mc.gameSettings.heldItemTooltips = false;
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onOverlayDone(RenderGameOverlayEvent.Post event) {
		if (event.type == RenderGameOverlayEvent.ElementType.ALL && Spectator.isSpectator(Minecraft.getMinecraft().thePlayer)) {
			Minecraft.getMinecraft().gameSettings.heldItemTooltips = hadHeldItemTooltips;
		}
	}

	/** No hand; once back, the player's model drawn once off screen so the hand shows again. */
	@SubscribeEvent
	public void onHand(RenderHandEvent event) {
		Minecraft mc = Minecraft.getMinecraft();
		if (Spectator.isSpectator(mc.thePlayer)) {
			event.setCanceled(true);
			refreshModel = true;
		} else if (refreshModel) {
			refreshModel = false;
			RenderManager.instance.renderEntityWithPosYaw(mc.thePlayer, -180.0D, -180.0D, -180.0D, 0.0F, 0.0F);
		}
	}

	@SubscribeEvent
	public void onBlockOverlay(RenderBlockOverlayEvent event) {
		if (Spectator.isSpectator(Minecraft.getMinecraft().thePlayer)) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onFog(EntityViewRenderEvent.FogDensity event) {
		if (event.entity instanceof EntityPlayer player && Spectator.isSpectator(player) && event.block.getMaterial().isLiquid()) {
			event.setCanceled(true);
			event.density = 0;
		}
	}

	/** The crosshair and outline only over what it can open, or an entity it isn't following yet. */
	@SubscribeEvent
	public void onHighlight(DrawBlockHighlightEvent event) {
		if (!Spectator.isSpectator(event.player)) {
			return;
		}
		canSelect = Spectator.canOpen(Minecraft.getMinecraft().theWorld.getTileEntity(event.target.blockX, event.target.blockY, event.target.blockZ))
			|| event.target.entityHit != null && Spectator.following(event.player) == null;
		if (!canSelect) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
		if (player == null || !Spectator.isSpectator(player)) {
			return;
		}
		if (!player.capabilities.isFlying) {
			player.capabilities.isFlying = true;
			player.sendPlayerAbilities();
		}
		Entity followed = Spectator.following(player);
		if (followed != null) {
			if (followed.isDead || player.isSneaking()) {
				Spectator.FOLLOWING.remove(player);
				return;
			}
			Spectator.follow(player, followed);
		}
	}
}
