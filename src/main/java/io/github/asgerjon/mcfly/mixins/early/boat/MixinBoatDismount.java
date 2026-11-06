package io.github.asgerjon.mcfly.mixins.early.boat;

import io.github.asgerjon.mcfly.boat.McFlyBoat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A player getting out of one of our boats is put where 26.3's boat puts it (SPEC-modern-movement.md 91.12;
 * `McFlyBoat.dismountLocation`): 1.7.10 calls this for players only (`EntityPlayer.mountEntity`), on the server.
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinBoatDismount {

	@Inject(method = "dismountEntity", at = @At("HEAD"), cancellable = true)
	private void mcfly$outOfABoat(Entity vehicle, CallbackInfo ci) {
		EntityLivingBase self = (EntityLivingBase) (Object) this;
		double[] to = McFlyBoat.dismountLocation(vehicle, self);
		if (to != null) {
			self.setPositionAndUpdate(to[0], to[1], to[2]);
			ci.cancel();
		}
	}
}
