package ganymedes01.etfuturum.mixins.early.modernmovement.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

/**
 * Sky with the eyes below the horizon (§13.4). 1.7.10 draws a dark blue plane at the horizon height (sea level, 63 in
 * the overworld) on every sky render. With the eyes below that height the plane hangs above the camera and covers the
 * whole sky, which a swimmer's eyes (0.4 above the feet) do in any ocean. Modern only darkens below the camera in that
 * case, so the plane is skipped there; above the horizon it stays, as 1.7.10's far-horizon band.
 */
@Mixin(RenderGlobal.class)
public class MixinRenderGlobalHorizon {

	@Shadow
	private Minecraft mc;

	@Shadow
	private WorldClient theWorld;

	/**
	 * The second sky-list call after the horizon check draws the plane at the horizon height (the first is modern's
	 * dark disc, 4 blocks below the camera).
	 */
	@WrapWithCondition(method = "renderSky", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glCallList(I)V", ordinal = 1), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/WorldClient;getHorizon()D")))
	private boolean etfu$noHorizonPlaneOverTheSky(int list, @Local(argsOnly = true) float partialTicks) {
		return mc.thePlayer.getPosition(partialTicks).yCoord - theWorld.getHorizon() >= 0.0D;
	}
}
