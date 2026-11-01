package ganymedes01.etfuturum.mixins.early.firstpersoncamera.client;

import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * The first-person camera at the eyes, as modern's Camera (SPEC-modern-movement.md §53.5). 1.7.10's orientCamera moves
 * it 0.1 back in first person, its only -0.1 (unitTAS' calibration measured the difference against 26.3).
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

	@ModifyConstant(method = "orientCamera", constant = @Constant(floatValue = -0.1F))
	private float etfu$cameraAtTheEyes(float back) {
		return 0.0F;
	}
}
