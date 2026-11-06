package io.github.asgerjon.mcfly.mixins.early.gameevent;

import io.github.asgerjon.mcfly.gameevent.GameEvents;
import net.minecraft.entity.effect.EntityLightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A bolt's strike posts `lightning_strike` at it with its thunder (SPEC-modern-movement.md §90.9, step H1; 26.3's `LightningBolt.tick`). */
@Mixin(EntityLightningBolt.class)
public abstract class MixinEntityLightningBolt {

	@Inject(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSoundEffect(DDDLjava/lang/String;FF)V", ordinal = 0))
	private void mcfly$strike(CallbackInfo ci) {
		GameEvents.postAt((EntityLightningBolt) (Object) this, "lightning_strike");
	}
}
