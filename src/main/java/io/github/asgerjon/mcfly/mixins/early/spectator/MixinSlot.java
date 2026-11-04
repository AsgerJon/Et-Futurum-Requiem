package io.github.asgerjon.mcfly.mixins.early.spectator;

import io.github.asgerjon.mcfly.spectator.Spectator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A spectator takes nothing from a slot (spec §83). */
@Mixin(Slot.class)
public class MixinSlot {

	@Inject(method = "canTakeStack", at = @At("HEAD"), cancellable = true)
	private void mcfly$spectatorsTakeNothing(EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
		if (Spectator.isSpectator(player)) {
			cir.setReturnValue(false);
		}
	}
}
