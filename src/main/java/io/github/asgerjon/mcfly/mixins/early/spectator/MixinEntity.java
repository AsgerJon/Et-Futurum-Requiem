package io.github.asgerjon.mcfly.mixins.early.spectator;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** What passes through blocks isn't inside one (spec §83): a spectator doesn't suffocate. */
@Mixin(Entity.class)
public class MixinEntity {

	@Shadow
	public boolean noClip;

	@Inject(method = "isEntityInsideOpaqueBlock", at = @At("HEAD"), cancellable = true)
	private void mcfly$notInsideWhenThrough(CallbackInfoReturnable<Boolean> cir) {
		if (noClip) {
			cir.setReturnValue(false);
		}
	}
}
