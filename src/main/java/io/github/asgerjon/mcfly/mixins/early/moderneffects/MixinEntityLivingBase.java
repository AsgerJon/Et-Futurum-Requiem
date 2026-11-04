package io.github.asgerjon.mcfly.mixins.early.moderneffects;

import ganymedes01.etfuturum.potion.ModernEffects;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A killed mob or player is removed at the end of its death animation, where 26.3 runs its effects' `onMobRemoved`
 * with `KILLED` (wind charged, weaving, oozing; spec §69.11).
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase {

	@Inject(method = "onDeathUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;setDead()V"))
	private void etfu$killedRemoved(CallbackInfo ci) {
		ModernEffects.onKilledRemoved((EntityLivingBase) (Object) this);
	}
}
