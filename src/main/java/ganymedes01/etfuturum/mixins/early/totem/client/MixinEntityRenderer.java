package ganymedes01.etfuturum.mixins.early.totem.client;

import ganymedes01.etfuturum.client.TotemActivation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The totem flying at the screen (SPEC-modern-movement.md §55.2), drawn after the hand in the hand's projection. */
@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

	@Shadow
	private Minecraft mc;

	@Inject(method = "renderHand", at = @At("TAIL"))
	private void etfu$totemOverTheView(float partialTicks, int pass, CallbackInfo ci) {
		TotemActivation.render(mc, partialTicks);
	}
}
