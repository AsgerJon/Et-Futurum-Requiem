package ganymedes01.etfuturum.mixins.early.combat;

import ganymedes01.etfuturum.combat.DamageRules;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The protection points worn, added up as modern does (spec §46.10); 1.7.10 capped them at 25 and then rolled a
 * random half to all of them. The caller (applyPotionDamageCalculations) counts at most 20, 4 % a point, as modern.
 */
@Mixin(EnchantmentHelper.class)
public class MixinEnchantmentHelper {

	@Inject(method = "getEnchantmentModifierDamage", at = @At("HEAD"), cancellable = true)
	private static void etfu$noRandomHalf(ItemStack[] stacks, DamageSource source, CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(DamageRules.protectionPoints(stacks, source));
	}
}
