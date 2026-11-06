package io.github.asgerjon.mcfly.mixins.early.gameevent;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A fireball (a ghast's, a blaze's, a wither skull): `projectile_shoot` on its first tick, by its owner, kept as 26.3's `HasBeenShot`;
 * `projectile_land` after its impact (SPEC-modern-movement.md §90.9, step H1; 26.3's `Projectile`).
 */
@Mixin(EntityFireball.class)
public abstract class MixinEntityFireball {

	@Shadow
	public EntityLivingBase shootingEntity;

	@Unique
	private boolean mcfly$shot;

	@Inject(method = "onUpdate", at = @At("HEAD"))
	private void mcfly$shoot(CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (!mcfly$shot && !self.worldObj.isRemote) {
			mcfly$shot = true;
			GameEventHooks.projectileShot(self, shootingEntity);
		}
	}

	@WrapOperation(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/EntityFireball;onImpact(Lnet/minecraft/util/MovingObjectPosition;)V"))
	private void mcfly$impact(EntityFireball self, MovingObjectPosition hit, Operation<Void> original) {
		original.call(self, hit);
		GameEventHooks.projectileLanded(self, hit);
	}

	@Inject(method = "writeEntityToNBT", at = @At("TAIL"))
	private void mcfly$saveShot(NBTTagCompound tag, CallbackInfo ci) {
		tag.setBoolean("HasBeenShot", mcfly$shot);
	}

	@Inject(method = "readEntityFromNBT", at = @At("TAIL"))
	private void mcfly$loadShot(NBTTagCompound tag, CallbackInfo ci) {
		mcfly$shot = tag.getBoolean("HasBeenShot");
	}
}
