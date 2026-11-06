package io.github.asgerjon.mcfly.mixins.early.gameevent;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.asgerjon.mcfly.gameevent.GameEvent;
import io.github.asgerjon.mcfly.gameevent.GameEventHooks;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * An arrow's `projectile_shoot` on its first tick, by its shooter, kept as 26.3's `HasBeenShot`; its `projectile_land`
 * on hitting an entity (at it, hurt or not: 1.7.10's hit point) or a block, with its thud (SPEC-modern-movement.md
 * §90.9, step H1; 26.3's `Projectile`).
 */
@Mixin(EntityArrow.class)
public abstract class MixinEntityArrow {

	@Shadow
	public Entity shootingEntity;
	@Shadow
	private int field_145791_d;
	@Shadow
	private int field_145792_e;
	@Shadow
	private int field_145789_f;
	@Shadow
	private Block field_145790_g;
	@Shadow
	private int inData;

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

	@WrapOperation(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z"))
	private boolean mcfly$hitEntity(Entity hit, DamageSource source, float amount, Operation<Boolean> original) {
		boolean hurt = original.call(hit, source, amount);
		Entity self = (Entity) (Object) this;
		if (!self.worldObj.isRemote) {
			// 1.7.10's hit point for an entity: its position
			GameEvents.post(self.worldObj, GameEvent.get("projectile_land"), hit.posX, hit.posY, hit.posZ, self, null, 0);
		}
		return hurt;
	}

	@Inject(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/EntityArrow;playSound(Ljava/lang/String;FF)V", ordinal = 1))
	private void mcfly$hitBlock(CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		GameEvents.postAtBlock(self.worldObj, "projectile_land", field_145791_d, field_145792_e, field_145789_f, self, field_145790_g, inData);
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
