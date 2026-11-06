package io.github.asgerjon.mcfly.mixins.early.boat;

import io.github.asgerjon.mcfly.boat.McFlyBoat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.stats.StatList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Distance by boat for our boats too, either seat (SPEC-modern-movement.md 91.12; 26.3's `BOAT_ONE_CM`, by the
 * passenger's movement across): 1.7.10 counts its own boat only.
 */
@Mixin(EntityPlayer.class)
public abstract class MixinBoatStat {

	@Inject(method = "addMountedMovementStat", at = @At("TAIL"))
	private void mcfly$byBoat(double dx, double dy, double dz, CallbackInfo ci) {
		EntityPlayer self = (EntityPlayer) (Object) this;
		if (McFlyBoat.boatOf(self.ridingEntity) != null) {
			int distance = (int) Math.round(Math.sqrt(dx * dx + dz * dz) * 100.0D);
			if (distance > 0) {
				self.addStat(StatList.distanceByBoatStat, distance);
			}
		}
	}
}
