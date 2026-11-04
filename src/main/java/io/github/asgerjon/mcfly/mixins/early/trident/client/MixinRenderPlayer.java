package io.github.asgerjon.mcfly.mixins.early.trident.client;

import ganymedes01.etfuturum.client.model.ModelRiptide;
import ganymedes01.etfuturum.movement.ModernMovement;
import ganymedes01.etfuturum.trident.Riptide;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A player in Riptide's spin, as modern draws one (SPEC-modern-movement.md §58.2): the body turned to lie along the look
 * and spun 75 degrees a tick (after the body's own turn; gliding keeps its own pose), and the spin effect around it,
 * drawn in the model's space where the held items are.
 */
@Mixin(RenderPlayer.class)
public abstract class MixinRenderPlayer {

	@Unique
	private static final ModelRiptide etfu$riptide = new ModelRiptide();

	@Inject(method = "rotateCorpse(Lnet/minecraft/client/entity/AbstractClientPlayer;FFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;rotateCorpse(Lnet/minecraft/entity/EntityLivingBase;FFF)V", shift = At.Shift.AFTER))
	private void etfu$spinRotation(AbstractClientPlayer player, float ageInTicks, float bodyYaw, float partialTicks, CallbackInfo ci) {
		if (!Riptide.isSpinning(player) || ModernMovement.isElytraFlying(player)) {
			return;
		}
		float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
		GL11.glRotatef(-90.0F - pitch, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(ageInTicks * -75.0F, 0.0F, 1.0F, 0.0F);
	}

	@Inject(method = "renderEquippedItems(Lnet/minecraft/client/entity/AbstractClientPlayer;F)V", at = @At("HEAD"))
	private void etfu$riptideEffect(AbstractClientPlayer player, float partialTicks, CallbackInfo ci) {
		if (Riptide.isSpinning(player)) {
			etfu$riptide.render(player.ticksExisted + partialTicks);
		}
	}
}
