package io.github.asgerjon.mcfly.mixins.early.horses;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.entities.ModernHorses;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The skeleton horse's swim sound (SPEC-modern-movement.md §64.6, 26.3's `SkeletonHorse.getSwimSound` and
 * `playSwimSound`): off the ground its own, at `min(0.1, volume × 25)`; on the ground under water none, the step on
 * the block plays alone (26.3 plays the swim sound only when a step made none; 1.7.10 plays both). Ordinal 0 is
 * `moveEntity`'s swim sound, the one after `getSwimSound`.
 */
@Mixin(Entity.class)
public abstract class MixinEntity {

	@WrapOperation(method = "moveEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;playSound(Ljava/lang/String;FF)V", ordinal = 0))
	private void etfu$skeletonHorseSwim(Entity entity, String sound, float volume, float pitch, Operation<Void> original) {
		if (!ModernHorses.isSkeletonHorse(entity)) {
			original.call(entity, sound, volume, pitch);
		} else if (!entity.onGround) {
			original.call(entity, ModernHorses.SWIM, Math.min(0.1F, volume * 25.0F), pitch);
		}
	}
}
