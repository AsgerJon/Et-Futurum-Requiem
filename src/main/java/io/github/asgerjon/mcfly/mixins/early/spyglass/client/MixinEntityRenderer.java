package io.github.asgerjon.mcfly.mixins.early.spyglass.client;

import ganymedes01.etfuturum.client.Spyglass;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** 26.3's scoping mouse: an eighth of the turn in first person, unless cinematic (SPEC-modern-movement.md §64.5). */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

	@ModifyArg(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityClientPlayerMP;setAngles(FF)V"), index = 0)
	private float etfu$scopeYaw(float yaw) {
		return yaw * Spyglass.turnScale();
	}

	@ModifyArg(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityClientPlayerMP;setAngles(FF)V"), index = 1)
	private float etfu$scopePitch(float pitch) {
		return pitch * Spyglass.turnScale();
	}
}
