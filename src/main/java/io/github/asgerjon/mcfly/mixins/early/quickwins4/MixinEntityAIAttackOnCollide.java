package io.github.asgerjon.mcfly.mixins.early.quickwins4;

import ganymedes01.etfuturum.ducks.IAggressive;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.3's {@code ZombieAttackGoal} for zombies (SPEC-modern-movement.md §63.9): aggressive from 5 ticks after the attack
 * starts while the next attack is under half its 20 ticks away (1.7.10's {@code attackTick}, the same count); not
 * when it stops. For skeletons 26.3's plain {@code MeleeAttackGoal} (§64.1): aggressive from start to stop.
 */
@Mixin(EntityAIAttackOnCollide.class)
public abstract class MixinEntityAIAttackOnCollide {

	@Shadow
	EntityCreature attacker;
	@Shadow
	int attackTick;

	@Unique
	private int etfu$raiseArmTicks;

	@Inject(method = "startExecuting", at = @At("TAIL"))
	private void etfu$start(CallbackInfo ci) {
		etfu$raiseArmTicks = 0;
		// a skeleton's melee goal (§64.1): 26.3's MeleeAttackGoal, aggressive from its start
		if (attacker instanceof EntitySkeleton && attacker instanceof IAggressive) {
			((IAggressive) attacker).etfu$setAggressive(true);
		}
	}

	@Inject(method = "updateTask", at = @At("TAIL"))
	private void etfu$tick(CallbackInfo ci) {
		if (attacker instanceof EntityZombie && attacker instanceof IAggressive) {
			etfu$raiseArmTicks++;
			((IAggressive) attacker).etfu$setAggressive(etfu$raiseArmTicks >= 5 && attackTick < 10);
		}
	}

	@Inject(method = "resetTask", at = @At("TAIL"))
	private void etfu$stop(CallbackInfo ci) {
		if (attacker instanceof IAggressive) {
			((IAggressive) attacker).etfu$setAggressive(false);
		}
	}
}
