package io.github.asgerjon.mcfly.mixins.early.quickwins4;

import ganymedes01.etfuturum.ducks.IAggressive;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIArrowAttack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 26.3's {@code RangedBowAttackGoal}: aggressive while it runs (SPEC-modern-movement.md §64.1). */
@Mixin(EntityAIArrowAttack.class)
public abstract class MixinEntityAIArrowAttack {

	@Shadow
	@Final
	private EntityLiving entityHost;

	@Inject(method = "updateTask", at = @At("TAIL"))
	private void etfu$aggressive(CallbackInfo ci) {
		if (entityHost instanceof IAggressive) {
			((IAggressive) entityHost).etfu$setAggressive(true);
		}
	}

	@Inject(method = "resetTask", at = @At("TAIL"))
	private void etfu$calm(CallbackInfo ci) {
		if (entityHost instanceof IAggressive) {
			((IAggressive) entityHost).etfu$setAggressive(false);
		}
	}
}
