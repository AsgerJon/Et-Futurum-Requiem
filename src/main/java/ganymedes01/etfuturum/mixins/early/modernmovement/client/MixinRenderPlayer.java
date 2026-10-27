package ganymedes01.etfuturum.mixins.early.modernmovement.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.movement.FirstPersonArm;
import ganymedes01.etfuturum.movement.IPosePlayer;
import ganymedes01.etfuturum.movement.ModernMovement;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderPlayer.class)
public class MixinRenderPlayer {

	/** The first-person arm keeps its neutral pose: no crouch offset, no swimming stroke (FirstPersonArm). */
	@Inject(method = "renderFirstPersonArm", at = @At("HEAD"))
	private void etfu$firstPersonArmBegins(CallbackInfo ci) {
		FirstPersonArm.rendering = true;
	}

	@Inject(method = "renderFirstPersonArm", at = @At("RETURN"))
	private void etfu$firstPersonArmEnds(CallbackInfo ci) {
		FirstPersonArm.rendering = false;
	}

	/**
	 * Tilts the body into the swimming/crawling position (§14.1), after the normal body yaw rotation. In water the body
	 * follows the look pitch; out of water (crawling) it lies flat. The shift keeps it from pivoting around the feet.
	 */
	@Inject(method = "rotateCorpse(Lnet/minecraft/client/entity/AbstractClientPlayer;FFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;rotateCorpse(Lnet/minecraft/entity/EntityLivingBase;FFF)V", shift = At.Shift.AFTER))
	private void etfu$rotateForSwimming(AbstractClientPlayer player, float ageInTicks, float bodyYaw, float partialTicks, CallbackInfo ci) {
		if (!(player instanceof IPosePlayer) || ModernMovement.isElytraFlying(player)) {
			return;
		}
		IPosePlayer posePlayer = (IPosePlayer) player;
		float swimAmount = posePlayer.etfu$getSwimAmount(partialTicks);
		if (swimAmount <= 0.0F) {
			return;
		}

		float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
		float targetPitch = player.isInWater() ? -90.0F - pitch : -90.0F;
		GL11.glRotatef(swimAmount * targetPitch, 1.0F, 0.0F, 0.0F);
		if (ModernMovement.isVisuallySwimming(player)) {
			GL11.glTranslatef(0.0F, -1.0F, 0.3F);
		}
	}

	/**
	 * The crouch pose of the model (and both armour models) follows the real crouching state instead of the shift
	 * key (§14).
	 */
	@ModifyExpressionValue(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;isSneaking()Z", ordinal = 0))
	private boolean etfu$crouchModel(boolean sneaking, @Local(argsOnly = true) AbstractClientPlayer player) {
		return ConfigMixins.enableModernSneaking ? ModernMovement.isVisuallyCrouching(player) : sneaking;
	}

	/**
	 * 1.7.10 lowers crouching players by 0.125, but not your own player. Modern lowers every crouching player by 2/16
	 * (§14.2): switch off the vanilla shift and apply it to the render y below.
	 */
	@ModifyExpressionValue(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;isSneaking()Z", ordinal = 1))
	private boolean etfu$skipVanillaCrouchOffset(boolean sneaking) {
		return sneaking && !ConfigMixins.enableModernSneaking;
	}

	@ModifyArg(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V"), index = 2)
	private double etfu$crouchOffset(double y, @Local(argsOnly = true) AbstractClientPlayer player) {
		if (ConfigMixins.enableModernSneaking && ModernMovement.isVisuallyCrouching(player)) {
			return y - 2.0D / 16.0D;
		}
		return y;
	}

	/**
	 * The cape's crouch tilt follows the real crouching state as well.
	 */
	@ModifyExpressionValue(method = "renderEquippedItems(Lnet/minecraft/client/entity/AbstractClientPlayer;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;isSneaking()Z", ordinal = 0))
	private boolean etfu$crouchCape(boolean sneaking, @Local(argsOnly = true) AbstractClientPlayer player) {
		return ConfigMixins.enableModernSneaking ? ModernMovement.isVisuallyCrouching(player) : sneaking;
	}
}
