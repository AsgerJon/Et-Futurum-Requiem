package io.github.asgerjon.mcfly.mixins.early.spectator.client;

import io.github.asgerjon.mcfly.spectator.Spectator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** A spectator's third-person camera isn't pulled in by the blocks behind it (26.3's spectator camera passes through). */
@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

	@Shadow
	private Minecraft mc;

	@ModifyVariable(method = "orientCamera", at = @At("STORE"), ordinal = 7, name = "d6")
	private double mcfly$noCameraBlocking(double distance) {
		return Spectator.isSpectator(mc.thePlayer) && mc.renderViewEntity == mc.thePlayer ? Double.MAX_VALUE : distance;
	}
}
