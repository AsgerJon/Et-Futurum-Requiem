package io.github.asgerjon.mcfly.mixins.early.witherskull;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityWitherSkull;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A wither skull hits as 26.3's (SPEC-modern-movement.md §61.4): its own damage, `witherSkull`, from the skull with its
 * shooter as the attacker, a projectile ("was shot by a skull from"). 1.7.10 dealt it as the shooter's melee, which
 * nothing (an armour stand, a death message) could tell from a bite.
 */
@Mixin(EntityWitherSkull.class)
public abstract class MixinEntityWitherSkull {

	@WrapOperation(method = "onImpact", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/DamageSource;causeMobDamage(Lnet/minecraft/entity/EntityLivingBase;)Lnet/minecraft/util/DamageSource;"))
	private DamageSource etfu$skullDamage(EntityLivingBase shooter, Operation<DamageSource> original) {
		return new EntityDamageSourceIndirect("witherSkull", (Entity) (Object) this, shooter).setProjectile();
	}
}
