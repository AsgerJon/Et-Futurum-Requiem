package io.github.asgerjon.mcfly.mixins.early.endcredits;

import ganymedes01.etfuturum.core.utils.EndCredits;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * The End's exit portal shows the credits only once (SPEC-modern-movement.md §53.2, modern's seenCredits). 1.7.10's
 * way out stays: the player leaves the End and the client asks to respawn, which puts the player at their spawn point
 * with everything kept. A player who has seen the credits gets the notice with EndCredits.SKIP, and the client then
 * asks at once instead of showing them.
 */
@Mixin(EntityPlayerMP.class)
public class MixinEntityPlayerMP {

	@ModifyArg(method = "travelToDimension", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/play/server/S2BPacketChangeGameState;<init>(IF)V"), index = 1)
	private float etfu$creditsOnlyOnce(float value) {
		EntityPlayerMP player = (EntityPlayerMP) (Object) this;
		if (EndCredits.hasSeen(player)) {
			return EndCredits.SKIP;
		}
		EndCredits.setSeen(player, true);
		return value;
	}
}
