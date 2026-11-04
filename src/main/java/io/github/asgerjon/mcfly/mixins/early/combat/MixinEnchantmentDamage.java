package io.github.asgerjon.mcfly.mixins.early.combat;

import net.minecraft.enchantment.EnchantmentDamage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Damage enchantments as modern's (spec §46.10): Sharpness 0.5 + 0.5 a level (1.7.10 1.25 a level); Smite and Bane of
 * Arthropods keep 2.5 a level. Bane's Slowness IV lasts 1.5 s, up to 0.5 s a level more, at random.
 */
@Mixin(EnchantmentDamage.class)
public class MixinEnchantmentDamage {

	@Shadow
	@Final
	public int damageType;

	@Inject(method = "func_152376_a", at = @At("HEAD"), cancellable = true)
	private void etfu$modernSharpness(int level, EnumCreatureAttribute target, CallbackInfoReturnable<Float> cir) {
		if (damageType == 0) {
			cir.setReturnValue(0.5F + 0.5F * level);
		}
	}

	@Inject(method = "func_151368_a", at = @At("HEAD"), cancellable = true)
	private void etfu$modernBaneSlowness(EntityLivingBase user, Entity target, int level, CallbackInfo ci) {
		if (damageType != 2) {
			return;
		}
		if (target instanceof EntityLivingBase living && living.getCreatureAttribute() == EnumCreatureAttribute.ARTHROPOD) {
			living.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 30 + user.getRNG().nextInt(10 * Math.max(0, level - 1) + 1), 3));
		}
		ci.cancel();
	}
}
