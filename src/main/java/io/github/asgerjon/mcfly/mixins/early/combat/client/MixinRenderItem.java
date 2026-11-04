package io.github.asgerjon.mcfly.mixins.early.combat.client;

import ganymedes01.etfuturum.combat.ICombatPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** An item on cooldown shows a white overlay over its slot, shrinking from the top as the cooldown runs out (spec §46.9). */
@Mixin(RenderItem.class)
public class MixinRenderItem {

	@Inject(method = "renderItemOverlayIntoGUI(Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At("TAIL"))
	private void etfu$cooldownOverlay(FontRenderer font, TextureManager textures, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
		Minecraft mc = Minecraft.getMinecraft();
		if (stack == null || !(mc.thePlayer instanceof ICombatPlayer player)) {
			return;
		}
		float left = player.etfu$getCooldowns().remaining(stack.getItem(), mc.timer.renderPartialTicks);
		if (left <= 0.0F) {
			return;
		}
		int top = y + MathHelper.floor_float(16.0F * (1.0F - left));
		int bottom = top + MathHelper.ceiling_float_int(16.0F * left);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		tessellator.setColorRGBA(255, 255, 255, 127);
		tessellator.addVertex(x, bottom, 0.0D);
		tessellator.addVertex(x + 16, bottom, 0.0D);
		tessellator.addVertex(x + 16, top, 0.0D);
		tessellator.addVertex(x, top, 0.0D);
		tessellator.draw();
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
	}
}
