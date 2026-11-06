package io.github.asgerjon.mcfly.mixins.early.sculk;

import io.github.asgerjon.mcfly.potion.EffectBlend;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Effects' blends ticked on the client with the effects (SPEC-modern-movement.md §90.9, step H4; 26.3's `tickClient`). */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBaseBlend {

	@Inject(method = "updatePotionEffects", at = @At("HEAD"))
	private void mcfly$blend(CallbackInfo ci) {
		EntityLivingBase self = (EntityLivingBase) (Object) this;
		if (self.worldObj.isRemote) {
			EffectBlend.tick(self);
		}
	}
}
