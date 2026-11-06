package io.github.asgerjon.mcfly.mixins.early.elytra.client;

import io.github.asgerjon.mcfly.elytra.FallFlying;
import io.github.asgerjon.mcfly.elytra.GlidingPlayer;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A glider's body (SPEC-modern-movement.md §21.1, §85), as 26.3's `AvatarRenderer.setupRotations`, after the body's yaw:
 * pitched toward lying along the look over its first 10 ticks of gliding (the share the square of its ticks over 100),
 * then turned about its own length into the way it's moving (the signed angle between its motion and its look, across).
 */
@Mixin(RenderPlayer.class)
public class MixinRenderPlayer {

	@Inject(method = "rotateCorpse(Lnet/minecraft/client/entity/AbstractClientPlayer;FFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;rotateCorpse(Lnet/minecraft/entity/EntityLivingBase;FFF)V", shift = At.Shift.AFTER))
	private void mcfly$glidingBody(AbstractClientPlayer player, float ageInTicks, float bodyYaw, float partialTicks, CallbackInfo ci) {
		if (!FallFlying.isFallFlying(player)) {
			return;
		}
		GlidingPlayer glider = (GlidingPlayer) player;
		float ticks = glider.etfu$getFallFlyingTicks() + partialTicks;
		float share = MathHelper.clamp_float(ticks * ticks / 100.0F, 0.0F, 1.0F);
		float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
		GL11.glRotatef(share * (-90.0F - pitch), 1.0F, 0.0F, 0.0F);
		Vec3 look = player.getLook(partialTicks);
		double[] motion = glider.etfu$getElytraAnimation().getMotion(player, partialTicks);
		double motionLength = motion[0] * motion[0] + motion[2] * motion[2];
		double lookLength = look.xCoord * look.xCoord + look.zCoord * look.zCoord;
		if (motionLength > 1.0E-5F && lookLength > 1.0E-5F) {
			double dot = (motion[0] * look.xCoord + motion[2] * look.zCoord) / Math.sqrt(motionLength * lookLength);
			double across = motion[0] * look.zCoord - motion[2] * look.xCoord;
			GL11.glRotatef((float) Math.toDegrees(Math.signum(across) * Math.acos(Math.min(1.0D, Math.abs(dot)))), 0.0F, 1.0F, 0.0F);
		}
	}
}
