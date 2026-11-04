package io.github.asgerjon.mcfly.mixins.early.horses;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import ganymedes01.etfuturum.entities.ModernHorses;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAISwimming;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The skeleton horse doesn't float (SPEC-modern-movement.md §64.6): 26.3's `SkeletonHorse.addBehaviourGoals` is empty,
 * so it has no `FloatGoal`, and it sinks to the bottom, ridden or not. 1.7.10 gives every horse type this task in its
 * constructor, before the type is known, so the task asks the type each time.
 */
@Mixin(EntityAISwimming.class)
public abstract class MixinEntityAISwimming {

	@Shadow
	private EntityLiving theEntity;

	@ModifyReturnValue(method = "shouldExecute", at = @At("RETURN"))
	private boolean etfu$skeletonHorsesSink(boolean swims) {
		return swims && !ModernHorses.isSkeletonHorse(theEntity);
	}
}
