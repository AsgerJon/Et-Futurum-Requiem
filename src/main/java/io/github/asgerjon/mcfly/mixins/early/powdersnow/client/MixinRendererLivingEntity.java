package io.github.asgerjon.mcfly.mixins.early.powdersnow.client;

import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.entities.Freezing;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Shaking when fully frozen (SPEC-modern-movement.md §62.3; 26.3 {@code LivingEntityRenderer.setupRotations}): the
 * body turns by cos(floor(age) * 3.25) * pi * 0.4 degrees.
 */
@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity {

	@ModifyVariable(method = "rotateCorpse", at = @At("HEAD"), argsOnly = true, ordinal = 1)
	private float etfu$shakeWhenFrozen(float bodyYaw, @Local(argsOnly = true) EntityLivingBase entity, @Local(argsOnly = true, ordinal = 0) float age) {
		// 26.3: a skeleton shakes while turning into a stray (it never freezes), the rest when fully frozen
		if (Freezing.isFullyFrozen(entity) || Freezing.isConvertingToStray(entity)) {
			return bodyYaw + (float) (Math.cos(MathHelper.floor_float(age) * 3.25D) * Math.PI * 0.4D);
		}
		return bodyYaw;
	}
}
