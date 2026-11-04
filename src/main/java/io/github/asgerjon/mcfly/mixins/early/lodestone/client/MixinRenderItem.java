package io.github.asgerjon.mcfly.mixins.early.lodestone.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import ganymedes01.etfuturum.client.LodestoneNeedle;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A dropped lodestone compass points by the item as it turns, unless an item frame is drawing it; in a screen, a
 * spinning needle's offset comes from where it's drawn (SPEC-modern-movement.md §57.2).
 */
@Mixin(RenderItem.class)
public class MixinRenderItem {

	/** Wrapped whole, so the item stops being named however its drawing ends (a cancelled one skipped a RETURN hook). */
	@WrapMethod(method = "doRender(Lnet/minecraft/entity/item/EntityItem;DDDFF)V")
	private void etfu$shownOnTheGround(EntityItem item, double x, double y, double z, float yaw, float partialTicks, Operation<Void> original) {
		boolean showing = LodestoneNeedle.startShowing(item);
		try {
			original.call(item, x, y, z, yaw, partialTicks);
		} finally {
			if (showing) {
				LodestoneNeedle.stopShowing(item);
			}
		}
	}

	// Forge's own overload, which the vanilla ones call: it has the same name in every environment, so nothing to remap
	@Inject(method = "renderItemIntoGUI(Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/item/ItemStack;IIZ)V", at = @At("HEAD"), remap = false)
	private void etfu$inScreen(FontRenderer font, TextureManager textures, ItemStack stack, int x, int y, boolean effect, CallbackInfo ci) {
		LodestoneNeedle.inScreenAt(x, y);
	}
}
