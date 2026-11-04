package io.github.asgerjon.mcfly.mixins.early.powdersnow;

import ganymedes01.etfuturum.entities.Freezing;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Powder snow resets the fall distance (SPEC-modern-movement.md §62.1; 26.3 {@code makeStuckInBlock}). The server's
 * player checks its client's move first and handles the fall after it ({@code NetHandlerPlayServer.processPlayer}):
 * reset there in the check, the landing on powder snow (its fall sound) was lost; so it is reset once the fall is
 * handled, each packet the player is in it, and sinking deep still deals no fall damage.
 */
@Mixin(EntityPlayerMP.class)
public abstract class MixinEntityPlayerMP {

	@Inject(method = "handleFalling", at = @At("TAIL"))
	private void etfu$noFallInPowderSnow(double y, boolean onGround, CallbackInfo ci) {
		EntityPlayerMP player = (EntityPlayerMP) (Object) this;
		if (Freezing.isInPowderSnow(player)) {
			player.fallDistance = 0.0F;
		}
	}
}
