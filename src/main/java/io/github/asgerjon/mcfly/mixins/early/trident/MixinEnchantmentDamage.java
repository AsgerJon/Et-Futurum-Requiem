package io.github.asgerjon.mcfly.mixins.early.trident;

import ganymedes01.etfuturum.ModEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentDamage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sharpness, Smite and Bane of Arthropods refuse Impaling (SPEC-modern-movement.md §58.4), as 26.3's damage set
 * does: 1.7.10's anvil asks only the incoming enchantment, so both sides must say no.
 */
@Mixin(EnchantmentDamage.class)
public abstract class MixinEnchantmentDamage {

	@Inject(method = "canApplyTogether", at = @At("HEAD"), cancellable = true)
	private void etfu$notWithImpaling(Enchantment other, CallbackInfoReturnable<Boolean> cir) {
		if (other != null && other == ModEnchantments.impaling) {
			cir.setReturnValue(false);
		}
	}
}
