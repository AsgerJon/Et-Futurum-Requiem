package io.github.asgerjon.mcfly.mixins.early.modernend;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A pearl that went through an end gateway (its portal cooldown running) puts its thrower on portal cooldown as it
 * lands, so they don't go straight back through one there (26.3's ThrownEnderpearl.onHit; SPEC-modern-movement.md
 * §76.10).
 */
@Mixin(EntityEnderPearl.class)
public abstract class MixinEntityEnderPearl extends EntityThrowable {

	private MixinEntityEnderPearl(World world) {
		super(world);
	}

	@Inject(method = "onImpact", at = @At("HEAD"))
	private void etfu$throwerPortalCooldown(MovingObjectPosition hit, CallbackInfo ci) {
		EntityLivingBase thrower = getThrower();
		if (!worldObj.isRemote && timeUntilPortal > 0 && thrower != null) {
			thrower.timeUntilPortal = thrower.getPortalCooldown();
		}
	}
}
