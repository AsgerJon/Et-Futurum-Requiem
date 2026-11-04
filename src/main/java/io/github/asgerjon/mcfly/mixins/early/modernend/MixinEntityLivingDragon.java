package io.github.asgerjon.mcfly.mixins.early.modernend;

import ganymedes01.etfuturum.world.end.modern.dragon.ModernDragon;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A killing blow to the dragon in flight leaves it at 1 health, dying: it flies to the portal to die (26.3's
 * handleKillingBlow; SPEC-modern-movement.md §76.7).
 */
@Mixin(EntityLivingBase.class)
public class MixinEntityLivingDragon {

	@Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
	private void etfu$killingBlow(DamageSource source, CallbackInfo ci) {
		if ((Object) this instanceof EntityDragon dragon && ModernDragon.of(dragon) != null && ModernDragon.of(dragon).holdOffDeath()) {
			ci.cancel();
		}
	}
}
