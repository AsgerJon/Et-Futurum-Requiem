package io.github.asgerjon.mcfly.mixins.early.boat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.asgerjon.mcfly.boat.McFlyBoat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A mob getting out of one of our boats is put where 26.3's boat puts it (SPEC-modern-movement.md 91.12): 1.7.10 puts
 * any rider on top of its vehicle (`Entity.mountEntity(null)`); players are `MixinBoatDismount`'s. The server's.
 */
@Mixin(Entity.class)
public abstract class MixinBoatMobDismount {

	@WrapOperation(method = "mountEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setLocationAndAngles(DDDFF)V"))
	private void mcfly$outOfABoat(Entity self, double x, double y, double z, float yaw, float pitch, Operation<Void> original) {
		if (self instanceof EntityLivingBase living && !self.worldObj.isRemote) {
			double[] to = McFlyBoat.dismountLocation(self.ridingEntity, living);
			if (to != null) {
				original.call(self, to[0], to[1], to[2], yaw, pitch);
				return;
			}
		}
		original.call(self, x, y, z, yaw, pitch);
	}
}
