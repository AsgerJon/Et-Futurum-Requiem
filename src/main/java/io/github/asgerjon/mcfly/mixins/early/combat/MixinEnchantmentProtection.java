package io.github.asgerjon.mcfly.mixins.early.combat;

import ganymedes01.etfuturum.combat.DamageRules;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Protection enchantments as modern's (spec §46.10): their points (1.7.10's grew with the square of the level), and
 * Fire Protection's shorter burning and Blast Protection's smaller explosion push by the levels worn added up (1.7.10
 * took the highest one, and its push cut rounded down to nothing).
 */
@Mixin(EnchantmentProtection.class)
public class MixinEnchantmentProtection {

	@Shadow
	@Final
	public int protectionType;

	@Inject(method = "calcModifierDamage", at = @At("HEAD"), cancellable = true)
	private void etfu$modernPoints(int level, DamageSource source, CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(DamageRules.protectionPoints(protectionType, level, source));
	}

	@Inject(method = "getFireTimeForEntity", at = @At("HEAD"), cancellable = true)
	private static void etfu$burningByLevelsWorn(Entity entity, int ticks, CallbackInfoReturnable<Integer> cir) {
		if (entity instanceof EntityLivingBase living) {
			cir.setReturnValue(DamageRules.burningTicks(living, ticks));
		}
	}

	@Inject(method = "func_92092_a", at = @At("HEAD"), cancellable = true)
	private static void etfu$explosionPushByLevelsWorn(Entity entity, double knockback, CallbackInfoReturnable<Double> cir) {
		if (entity instanceof EntityLivingBase living) {
			cir.setReturnValue(DamageRules.explosionKnockback(living, knockback));
		}
	}
}
