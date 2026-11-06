package io.github.asgerjon.mcfly.mixins.early.sculk.client;

import io.github.asgerjon.mcfly.potion.DarknessView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Darkness's fog (SPEC-modern-movement.md §90.9, step H4; 26.3's `DarknessFogEnvironment`): from the render distance
 * toward 15 by its blend, starting at 3/4. Blindness's fog comes first, as 26.3's order has it.
 */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRendererDarkness {

	@Shadow
	private float farPlaneDistance;

	@Inject(method = "setupFog", at = @At("TAIL"))
	private void mcfly$darknessFog(int mode, float partialTicks, CallbackInfo ci) {
		EntityLivingBase camera = Minecraft.getMinecraft().renderViewEntity;
		if (camera == null || camera.isPotionActive(Potion.blindness)) {
			return;
		}
		float end = DarknessView.fogEnd(farPlaneDistance, partialTicks);
		if (end < farPlaneDistance) {
			GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
			GL11.glFogf(GL11.GL_FOG_START, end * 0.75F);
			GL11.glFogf(GL11.GL_FOG_END, end);
		}
	}
}
