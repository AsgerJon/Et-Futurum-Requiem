package io.github.asgerjon.mcfly.mixins.early.trident;

import ganymedes01.etfuturum.trident.Impaling;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Impaling in melee (SPEC-modern-movement.md §58.4): its bonus joins the enchantment damage 1.7.10 works out against
 * a target, where Sharpness's is, so modern combat scales it by the charge as 26.3 does. 1.7.10 asks enchantments
 * only for the target's creature attribute, which knows nothing of aquatic mobs.
 */
@Mixin(EnchantmentHelper.class)
public abstract class MixinEnchantmentHelper {

	@Inject(method = "getEnchantmentModifierLiving", at = @At("RETURN"), cancellable = true)
	private static void etfu$impaling(EntityLivingBase attacker, EntityLivingBase target, CallbackInfoReturnable<Float> cir) {
		float bonus = Impaling.bonus(attacker.getHeldItem(), target);
		if (bonus > 0.0F) {
			cir.setReturnValue(cir.getReturnValue() + bonus);
		}
	}
}
