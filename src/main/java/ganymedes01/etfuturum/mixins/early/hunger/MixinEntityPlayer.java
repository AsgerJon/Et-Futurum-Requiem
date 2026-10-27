package ganymedes01.etfuturum.mixins.early.hunger;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Modern's exhaustion costs for what the player does (1.11+ for walking and swimming, 1.9+ for the rest). 1.7.10's
 * in brackets: sprinting 0.1 per metre (same), walking and sneaking nothing (0.01), swimming and diving 0.01 per
 * metre (0.015), a jump 0.05 and a sprint jump 0.2 (0.2 and 0.8), a hit landed 0.1 (0.3), damage taken 0.1 (0.3;
 * see MixinDamageSource) and none from starving.
 */
@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer {

	@ModifyArg(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;addExhaustion(F)V", ordinal = 0))
	private float etfu$sprintJumpCost(float exhaustion) {
		return 0.2F;
	}

	@ModifyArg(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;addExhaustion(F)V", ordinal = 1))
	private float etfu$jumpCost(float exhaustion) {
		return 0.05F;
	}

	@ModifyArg(method = "attackTargetEntityWithCurrentItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;addExhaustion(F)V"))
	private float etfu$attackCost(float exhaustion) {
		return 0.1F;
	}

	/** Diving and swimming: the 0.015 per metre of both branches becomes 0.01. */
	@ModifyConstant(method = "addMovementStat", constant = @Constant(floatValue = 0.015F))
	private float etfu$waterMovementCost(float original) {
		return 0.01F;
	}

	/** Walking (and sneaking, which 1.7.10 does not tell apart): the fourth movement cost, nothing in modern. */
	@ModifyArg(method = "addMovementStat", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;addExhaustion(F)V", ordinal = 3))
	private float etfu$walkingCost(float exhaustion) {
		return 0.0F;
	}

	/** Starving costs no exhaustion in modern; 1.7.10 charged the default for it. */
	@ModifyArg(method = "damageEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;addExhaustion(F)V"))
	private float etfu$noCostForStarving(float exhaustion, @Local(argsOnly = true) DamageSource source) {
		return source == DamageSource.starve ? 0.0F : exhaustion;
	}
}
