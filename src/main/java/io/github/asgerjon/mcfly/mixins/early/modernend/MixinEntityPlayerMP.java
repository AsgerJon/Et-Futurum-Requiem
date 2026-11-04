package io.github.asgerjon.mcfly.mixins.early.modernend;

import ganymedes01.etfuturum.world.end.modern.ModernEndArrival;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.7.10's player, going into the End, is first sent to the entrance facing (0, 0), its pitch reset before the transfer;
 * 26.3 keeps the pitch (SPEC-modern-movement.md §76.5), so it's noted before that.
 */
@Mixin(EntityPlayerMP.class)
public class MixinEntityPlayerMP {

	@Inject(method = "travelToDimension", at = @At("HEAD"))
	private void etfu$keepPitch(int dimension, CallbackInfo ci) {
		ModernEndArrival.rememberPitch((EntityPlayerMP) (Object) this);
	}
}
