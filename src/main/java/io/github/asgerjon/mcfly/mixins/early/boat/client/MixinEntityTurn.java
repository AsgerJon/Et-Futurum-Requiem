package io.github.asgerjon.mcfly.mixins.early.boat.client;

import io.github.asgerjon.mcfly.boat.McFlyBoat;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A passenger turning its view in one of our boats is kept within 105 degrees of it (SPEC-modern-movement.md 91.12;
 * 26.3's `Entity.turn` calling `onPassengerTurned`): the mouse turns the client's player here.
 */
@Mixin(Entity.class)
public abstract class MixinEntityTurn {

	@Inject(method = "setAngles", at = @At("TAIL"))
	private void mcfly$keptInTheBoat(float yaw, float pitch, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		McFlyBoat boat = McFlyBoat.boatOf(self.ridingEntity);
		if (boat != null) {
			boat.onPassengerTurned(self);
		}
	}
}
