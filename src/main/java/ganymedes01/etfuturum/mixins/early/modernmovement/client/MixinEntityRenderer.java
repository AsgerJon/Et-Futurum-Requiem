package ganymedes01.etfuturum.mixins.early.modernmovement.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.movement.ModernMovement;
import ganymedes01.etfuturum.movement.PlayerPose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Smooth camera height when the pose changes (§13.1). The camera keeps its own eye height, which moves halfway to the
 * pose's eye height every tick and is interpolated between ticks. Aiming is not smoothed: it uses getEyeHeight.
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

	@Shadow
	private Minecraft mc;

	@Unique
	private float etfu$cameraEyeHeight = PlayerPose.STANDING_EYE_HEIGHT;

	@Unique
	private float etfu$cameraEyeHeightOld = PlayerPose.STANDING_EYE_HEIGHT;

	@Inject(method = "updateRenderer", at = @At("HEAD"))
	private void etfu$tickCameraEyeHeight(CallbackInfo ci) {
		etfu$cameraEyeHeightOld = etfu$cameraEyeHeight;
		float target = ModernMovement.getTargetCameraEyeHeight(mc.renderViewEntity);
		etfu$cameraEyeHeight += (target - etfu$cameraEyeHeight) * 0.5F;
	}

	/**
	 * orientCamera places the camera at posY - (yOffset - 1.62), for the first-person view and for the third-person
	 * pivot and its collision check alike. Raising the yOffset read by (1.62 - eye height) moves all of them to the
	 * smoothed eye height.
	 */
	@ModifyExpressionValue(method = "orientCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;yOffset:F", ordinal = 0))
	private float etfu$smoothCameraEyeHeight(float yOffset, @Local(argsOnly = true) float partialTicks) {
		if (mc.renderViewEntity == null || mc.renderViewEntity.isPlayerSleeping()) {
			return yOffset;
		}
		float eyeHeight = etfu$cameraEyeHeightOld + (etfu$cameraEyeHeight - etfu$cameraEyeHeightOld) * partialTicks;
		return yOffset + (PlayerPose.STANDING_EYE_HEIGHT - eyeHeight);
	}
}
