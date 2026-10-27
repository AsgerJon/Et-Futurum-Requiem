package ganymedes01.etfuturum.mixins.early.fov.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The FOV multiplier (speed, flying, a drawn bow) is scaled by the FOV Effects setting, as modern's
 * getFieldOfViewModifier ends with lerp(effectScale, 1, modifier). The renderer's smoothing and clamp stay.
 */
@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP {
	@ModifyReturnValue(method = "getFOVMultiplier", at = @At("RETURN"))
	private float etfu$scaleTheFovEffect(float multiplier) {
		return 1.0F + (multiplier - 1.0F) * ConfigFunctions.fovEffectScale;
	}
}
