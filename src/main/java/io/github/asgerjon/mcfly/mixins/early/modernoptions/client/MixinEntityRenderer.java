package io.github.asgerjon.mcfly.mixins.early.modernoptions.client;

import ganymedes01.etfuturum.client.options.ControlOptions;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** 26.3's Invert Mouse X (spec §54.12): both of 1.7.10's mouse turns (smooth camera or not) through the option. */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

	@ModifyArg(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityClientPlayerMP;setAngles(FF)V"), index = 0)
	private float etfu$invertMouseX(float yaw) {
		return ControlOptions.turnYaw(yaw);
	}
}
