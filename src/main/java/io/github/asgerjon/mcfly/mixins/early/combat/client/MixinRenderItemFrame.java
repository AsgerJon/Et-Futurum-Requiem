package io.github.asgerjon.mcfly.mixins.early.combat.client;

import io.github.asgerjon.mcfly.banner.client.BannerItemRenderer;
import io.github.asgerjon.mcfly.banner.Banners;
import ganymedes01.etfuturum.client.renderer.item.ItemShieldRenderer;
import ganymedes01.etfuturum.items.equipment.ItemShield;
import net.minecraft.client.renderer.tileentity.RenderItemFrame;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A shield or a banner in an item frame is placed as modern's frames place items (spec §46.9, §66.10), by its
 * renderer, which needs the frame's facing and rotation; 1.7.10 drew framed items as dropped ones, which can't see the frame.
 */
@Mixin(RenderItemFrame.class)
public class MixinRenderItemFrame {

	@Inject(method = "func_82402_b", at = @At("HEAD"), cancellable = true)
	private void etfu$shieldAsModern(EntityItemFrame frame, CallbackInfo ci) {
		ItemStack stack = frame.getDisplayedItem();
		if (stack == null) {
			return;
		}
		IItemRenderer renderer = MinecraftForgeClient.getItemRenderer(stack, IItemRenderer.ItemRenderType.ENTITY);
		if (stack.getItem() instanceof ItemShield && renderer instanceof ItemShieldRenderer shield) {
			shield.renderInFrame(frame);
			ci.cancel();
		} else if (Banners.isBanner(stack) && renderer instanceof BannerItemRenderer banner) {
			// a banner too, by its fixed display (spec §66.10)
			banner.renderInFrame(frame);
			ci.cancel();
		}
	}
}
