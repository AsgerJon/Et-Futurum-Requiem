package ganymedes01.etfuturum.mixins.early.fov.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The narrowing under water (fov * 60 / 70) is scaled by the FOV Effects setting, as modern's
 * modifyFovBasedOnDeathOrFluid multiplies by lerp(effectScale, 1, 60 / 70): the 60 becomes 70 - 10 * scale.
 * The death zoom is not scaled in either version.
 */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
	@ModifyExpressionValue(method = "getFOVModifier", at = @At(value = "CONSTANT", args = "floatValue=60.0"))
	private float etfu$scaleTheWaterNarrowing(float sixty) {
		return 70.0F - 10.0F * ConfigFunctions.fovEffectScale;
	}
}
