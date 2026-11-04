package io.github.asgerjon.mcfly.mixins.early.moderneffects;

import ganymedes01.etfuturum.potion.ModernEffects;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Weaving halves a cobweb's hold (26.3's `WebBlock`: 0.5, 0.25, 0.5 of the motion, not 0.25, 0.05, 0.25; spec
 * §69.11): the move is scaled up by the difference before 1.7.10's web cuts it.
 */
@Mixin(Entity.class)
public abstract class MixinEntity {

	@Shadow
	protected boolean isInWeb;

	@ModifyVariable(method = "moveEntity", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private double etfu$wovenX(double x) {
		return isInWeb ? x * ModernEffects.webFactors((Entity) (Object) this)[0] / 0.25D : x;
	}

	@ModifyVariable(method = "moveEntity", at = @At("HEAD"), argsOnly = true, ordinal = 1)
	private double etfu$wovenY(double y) {
		return isInWeb ? y * ModernEffects.webFactors((Entity) (Object) this)[1] / 0.05D : y;
	}

	@ModifyVariable(method = "moveEntity", at = @At("HEAD"), argsOnly = true, ordinal = 2)
	private double etfu$wovenZ(double z) {
		return isInWeb ? z * ModernEffects.webFactors((Entity) (Object) this)[2] / 0.25D : z;
	}
}
