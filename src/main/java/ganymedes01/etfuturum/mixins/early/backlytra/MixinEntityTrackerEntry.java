package ganymedes01.etfuturum.mixins.early.backlytra;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.elytra.FallFlying;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTrackerEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityTrackerEntry.class)
public class MixinEntityTrackerEntry {

	@Shadow
	public Entity myEntity;

	/**
	 * Gliding players' velocity is sent to everyone tracking them, like modern does for anything fall flying (§18.4).
	 * Other clients use it for the wings, the body roll and the limb slow-down.
	 */
	@ModifyExpressionValue(method = "sendLocationToAllClients", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityTrackerEntry;sendVelocityUpdates:Z", ordinal = 0))
	private boolean etfu$sendGlidingVelocity(boolean sendVelocityUpdates) {
		return sendVelocityUpdates || FallFlying.isFallFlying(myEntity);
	}
}
