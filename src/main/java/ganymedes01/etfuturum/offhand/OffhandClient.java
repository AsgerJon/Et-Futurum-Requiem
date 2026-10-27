package ganymedes01.etfuturum.offhand;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.lib.Reference;
import ganymedes01.etfuturum.network.SwapHandsMessage;
import ganymedes01.etfuturum.spectator.SpectatorMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * The client side of the off-hand (SPEC-modern-movement.md §36.1, §36.7): the swap key (F by default, in the inventory
 * key category; the server does the swap) and the slot beside the hotbar. Registered on the FML bus for the key and
 * on the Forge bus for the overlay.
 */
public final class OffhandClient {
	public static final OffhandClient INSTANCE = new OffhandClient();
	public static KeyBinding keySwap;

	private static final ResourceLocation LEFT_SLOT = new ResourceLocation(Reference.MOD_ID, "textures/gui/hotbar_offhand_left.png");
	private static final ResourceLocation RIGHT_SLOT = new ResourceLocation(Reference.MOD_ID, "textures/gui/hotbar_offhand_right.png");

	private OffhandClient() {
	}

	public static void registerKey() {
		keySwap = new KeyBinding("key.etfuturum.swapOffhand", Keyboard.KEY_F, "key.categories.inventory");
		ClientRegistry.registerKeyBinding(keySwap);
	}

	@SubscribeEvent
	public void onKey(InputEvent.KeyInputEvent event) {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.thePlayer == null || keySwap == null) {
			return;
		}
		while (keySwap.isPressed()) {
			if (!SpectatorMode.isSpectator(mc.thePlayer)) {
				EtFuturum.networkWrapper.sendToServer(new SwapHandsMessage());
			}
		}
	}

	/**
	 * The off-hand slot beside the hotbar (§36.7), right after Forge's hotbar: shown only while the off-hand holds
	 * something, on the off-arm's side, with modern's own sprite, the item lit and popped like the hotbar's. Forge's
	 * HUD class replaces vanilla's overlay method, so this is an overlay event rather than a mixin.
	 */
	@SubscribeEvent
	public void onRenderHotbar(RenderGameOverlayEvent.Post event) {
		if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR || !Offhand.isEnabled()) {
			return;
		}
		Minecraft mc = Minecraft.getMinecraft();
		ItemStack stack = Offhand.getOffhand(mc.thePlayer);
		if (stack == null) {
			return;
		}
		int centre = event.resolution.getScaledWidth() / 2;
		int height = event.resolution.getScaledHeight();
		boolean offArmLeft = Hand.OFF.arm(Offhand.isLeftHanded(mc.thePlayer)) == Arm.LEFT;
		int slotX = offArmLeft ? centre - 91 - 29 : centre + 91;
		int itemX = offArmLeft ? centre - 91 - 26 : centre + 91 + 10;
		int itemY = height - 16 - 3;

		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		mc.getTextureManager().bindTexture(offArmLeft ? LEFT_SLOT : RIGHT_SLOT);
		Gui.func_146110_a(slotX, height - 23, 0.0F, 0.0F, 29, 24, 29.0F, 24.0F);
		GL11.glDisable(GL11.GL_BLEND);

		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		RenderHelper.enableGUIStandardItemLighting();
		RenderItem renderer = RenderItem.getInstance();
		float pop = stack.animationsToGo - event.partialTicks;
		if (pop > 0.0F) {
			GL11.glPushMatrix();
			float scale = 1.0F + pop / 5.0F;
			GL11.glTranslatef(itemX + 8, itemY + 12, 0.0F);
			GL11.glScalef(1.0F / scale, (scale + 1.0F) / 2.0F, 1.0F);
			GL11.glTranslatef(-(itemX + 8), -(itemY + 12), 0.0F);
		}
		renderer.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, itemX, itemY);
		if (pop > 0.0F) {
			GL11.glPopMatrix();
		}
		renderer.renderItemOverlayIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, itemX, itemY);
		RenderHelper.disableStandardItemLighting();
		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
	}
}
