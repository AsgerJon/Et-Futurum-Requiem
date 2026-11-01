package ganymedes01.etfuturum.mixins.early.combat;

import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionAttackDamage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Strength + 3 attack damage a level and Weakness − 4 a level, both added (spec §46.10; DamageRules.install makes
 * Strength's modifier an add). 1.7.10's Strength multiplied by 1 + 1.3 a level, its Weakness took 0.5.
 */
@Mixin(PotionAttackDamage.class)
public class MixinPotionAttackDamage {

	@Inject(method = "func_111183_a", at = @At("HEAD"), cancellable = true)
	private void etfu$modernAmount(int amplifier, AttributeModifier modifier, CallbackInfoReturnable<Double> cir) {
		cir.setReturnValue(((Potion) (Object) this).id == Potion.weakness.id ? -4.0D * (amplifier + 1) : 3.0D * (amplifier + 1));
	}
}
