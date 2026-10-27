package ganymedes01.etfuturum.mixins.early.backlytra.client;

import ganymedes01.etfuturum.elytra.FallFlying;
import ganymedes01.etfuturum.elytra.IElytraPlayer;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderPlayer.class)
public class MixinRenderPlayer {

	/**
	 * The gliding body (§21.1), after the normal body yaw rotation: pitched towards lying along the look direction over
	 * the first 10 ticks, then turned about its own axis into the direction it's actually travelling.
	 */
	@Inject(method = "rotateCorpse(Lnet/minecraft/client/entity/AbstractClientPlayer;FFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;rotateCorpse(Lnet/minecraft/entity/EntityLivingBase;FFF)V", shift = At.Shift.AFTER))
	private void etfu$rotateForGliding(AbstractClientPlayer player, float ageInTicks, float bodyYaw, float partialTicks, CallbackInfo ci) {
		if (!FallFlying.isFallFlying(player)) {
			return;
		}
		IElytraPlayer elytraPlayer = (IElytraPlayer) player;
		float time = elytraPlayer.etfu$getFallFlyingTicks() + partialTicks;
		float scale = MathHelper.clamp_float(time * time / 100.0F, 0.0F, 1.0F);
		float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
		GL11.glRotatef(scale * (-90.0F - pitch), 1.0F, 0.0F, 0.0F);

		Vec3 look = player.getLook(partialTicks);
		double[] motion = elytraPlayer.etfu$getElytraAnimation().getMotion(player, partialTicks);
		double motionH = motion[0] * motion[0] + motion[2] * motion[2];
		double lookH = look.xCoord * look.xCoord + look.zCoord * look.zCoord;
		if (motionH > 1.0E-5F && lookH > 1.0E-5F) {
			double dot = (motion[0] * look.xCoord + motion[2] * look.zCoord) / Math.sqrt(motionH * lookH);
			double side = motion[0] * look.zCoord - motion[2] * look.xCoord;
			double angle = Math.signum(side) * Math.acos(Math.min(1.0D, Math.abs(dot)));
			GL11.glRotatef((float) Math.toDegrees(angle), 0.0F, 1.0F, 0.0F);
		}
	}
}
