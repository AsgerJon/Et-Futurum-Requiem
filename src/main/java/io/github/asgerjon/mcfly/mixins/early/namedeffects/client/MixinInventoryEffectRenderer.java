package io.github.asgerjon.mcfly.mixins.early.namedeffects.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.potion.EffectIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Effect icons by name (spec §70.3, stage C): an effect with `textures/mob_effect/<name>.png` (26.3's way, shipped by
 * EFR for most of 26.3's, or by a mod or pack) is drawn from it in the inventory, instead of 1.7.10's shared sheet.
 */
@Mixin(InventoryEffectRenderer.class)
public abstract class MixinInventoryEffectRenderer {

	@WrapOperation(method = "func_147044_g", at = @At(value = "INVOKE", target = "Lnet/minecraft/potion/Potion;hasStatusIcon()Z"))
	private boolean etfu$sheetIcon(Potion potion, Operation<Boolean> original) {
		return EffectIcons.texture(potion) == null && original.call(potion);
	}

	@WrapOperation(method = "func_147044_g", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/potion/Potion;renderInventoryEffect(IILnet/minecraft/potion/PotionEffect;Lnet/minecraft/client/Minecraft;)V"))
	private void etfu$namedIcon(Potion potion, int x, int y, PotionEffect effect, Minecraft mc, Operation<Void> original) {
		original.call(potion, x, y, effect, mc);
		ResourceLocation icon = EffectIcons.texture(potion);
		if (icon != null) {
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			mc.getTextureManager().bindTexture(icon);
			Gui.func_146110_a(x + 6, y + 7, 0, 0, 18, 18, 18, 18);
		}
	}
}
