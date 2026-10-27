package ganymedes01.etfuturum.mixins.early.arrowmomentum;

import ganymedes01.etfuturum.movement.IKnownMovement;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * An arrow a player shoots carries the player's own movement, as since 1.9 (modern's shootFromRotation adds the
 * shooter's known movement to the launch, the vertical part only while airborne). 1.7.10 launched every arrow from
 * a standstill, so shooting while sprinting, riding or gliding fell short of modern. Mobs shoot through another
 * constructor and are untouched, as in modern.
 */
@Mixin(EntityArrow.class)
public abstract class MixinEntityArrow {

	@Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;F)V", at = @At("TAIL"))
	private void etfu$inheritShooterMovement(World world, EntityLivingBase shooter, float velocity, CallbackInfo ci) {
		if (shooter instanceof IKnownMovement known) {
			EntityArrow self = (EntityArrow) (Object) this;
			self.motionX += known.etfu$knownMotionX();
			if (!shooter.onGround) {
				self.motionY += known.etfu$knownMotionY();
			}
			self.motionZ += known.etfu$knownMotionZ();
		}
	}
}
