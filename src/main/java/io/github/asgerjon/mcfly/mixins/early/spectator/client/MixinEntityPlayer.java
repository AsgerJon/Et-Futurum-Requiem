package io.github.asgerjon.mcfly.mixins.early.spectator.client;

import io.github.asgerjon.mcfly.spectator.Spectator;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A spectator sees invisible players (spec §83). */
@Mixin(EntityPlayer.class)
public class MixinEntityPlayer {

	@Inject(method = "isInvisibleToPlayer", at = @At("HEAD"), cancellable = true)
	private void mcfly$spectatorsSeeAll(EntityPlayer viewer, CallbackInfoReturnable<Boolean> cir) {
		if (Spectator.isSpectator(viewer)) {
			cir.setReturnValue(false);
		}
	}
}
