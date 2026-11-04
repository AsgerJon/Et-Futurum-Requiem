package io.github.asgerjon.mcfly.mixins.early.powdersnow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import ganymedes01.etfuturum.ducks.IFreezingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Powder snow's state on every entity (SPEC-modern-movement.md §62.1, §62.3): frozen ticks (saved as
 * {@code TicksFrozen}), in powder snow this tick and last, and 26.3's stuck multiplier, applied to the next move as
 * 1.7.10's cobweb does with its own.
 */
@Mixin(Entity.class)
public abstract class MixinEntity implements IFreezingEntity {

	@Shadow
	public boolean noClip;
	@Shadow
	public double motionX;
	@Shadow
	public double motionY;
	@Shadow
	public double motionZ;

	@Shadow
	public abstract void extinguish();

	@Unique
	private int etfu$ticksFrozen;
	@Unique
	private boolean etfu$inPowderSnow;
	@Unique
	private boolean etfu$wasInPowderSnow;
	@Unique
	private boolean etfu$stuck;
	@Unique
	private double etfu$stuckX;
	@Unique
	private double etfu$stuckY;
	@Unique
	private double etfu$stuckZ;
	@Unique
	private boolean etfu$movedSideways;
	@Unique
	private boolean etfu$extinguishPending;
	@Unique
	private int etfu$lastSentTicks;
	@Unique
	private boolean etfu$climbRequested;

	@Override
	public void etfu$requestClimb() {
		etfu$climbRequested = true;
	}

	@Override
	public boolean etfu$takeClimbRequest() {
		boolean requested = etfu$climbRequested;
		etfu$climbRequested = false;
		return requested;
	}

	@Override
	public int etfu$getTicksFrozen() {
		return etfu$ticksFrozen;
	}

	@Override
	public void etfu$setTicksFrozen(int ticks) {
		etfu$ticksFrozen = ticks;
	}

	@Override
	public boolean etfu$isInPowderSnow() {
		return etfu$inPowderSnow;
	}

	@Override
	public void etfu$setInPowderSnow(boolean in) {
		etfu$inPowderSnow = in;
	}

	@Override
	public boolean etfu$wasInPowderSnow() {
		return etfu$wasInPowderSnow;
	}

	@Override
	public void etfu$makeStuck(double x, double y, double z) {
		etfu$stuck = true;
		etfu$stuckX = x;
		etfu$stuckY = y;
		etfu$stuckZ = z;
	}

	@Override
	public boolean etfu$movedSideways() {
		return etfu$movedSideways;
	}

	@Override
	public void etfu$extinguishAfterCollisions() {
		etfu$extinguishPending = true;
	}

	@Override
	public int etfu$lastSentTicks() {
		return etfu$lastSentTicks;
	}

	@Override
	public void etfu$setLastSentTicks(int ticks) {
		etfu$lastSentTicks = ticks;
	}

	/** 26.3 {@code baseTick}: last tick's "in powder snow" kept, this tick's found again by the move. */
	@Inject(method = "onEntityUpdate", at = @At("HEAD"))
	private void etfu$powderSnowTick(CallbackInfo ci) {
		etfu$wasInPowderSnow = etfu$inPowderSnow;
		etfu$inPowderSnow = false;
	}

	/**
	 * 26.3 {@code move}: stuck, the move times the multiplier and the motion 0. Not for the server's own player: it only
	 * checks the moves its client made, which were already slowed there.
	 */
	@WrapMethod(method = "moveEntity")
	private void etfu$stuckInPowderSnow(double x, double y, double z, Operation<Void> original) {
		etfu$movedSideways = x != 0.0D || z != 0.0D;
		if (etfu$stuck) {
			etfu$stuck = false;
			if (!noClip && !((Object) this instanceof EntityPlayerMP)) {
				x *= etfu$stuckX;
				y *= etfu$stuckY;
				z *= etfu$stuckZ;
				motionX = 0.0D;
				motionY = 0.0D;
				motionZ = 0.0D;
			}
		}
		original.call(x, y, z);
	}

	/** 26.3's EXTINGUISH effect, after every block the entity is in (so each burning-melt check saw it burning). */
	@Inject(method = "func_145775_I", at = @At("TAIL"))
	private void etfu$extinguishAfterBlocks(CallbackInfo ci) {
		if (etfu$extinguishPending) {
			etfu$extinguishPending = false;
			extinguish();
		}
	}

	/** 26.3 {@code igniteForTicks}: catching fire thaws at once. */
	@Inject(method = "setFire", at = @At("TAIL"))
	private void etfu$fireThaws(int seconds, CallbackInfo ci) {
		etfu$ticksFrozen = 0;
	}

	@Inject(method = "writeToNBT", at = @At("TAIL"))
	private void etfu$saveFrozen(NBTTagCompound tag, CallbackInfo ci) {
		if (etfu$ticksFrozen > 0) {
			tag.setInteger("TicksFrozen", etfu$ticksFrozen);
		}
	}

	@Inject(method = "readFromNBT", at = @At("TAIL"))
	private void etfu$loadFrozen(NBTTagCompound tag, CallbackInfo ci) {
		etfu$ticksFrozen = tag.getInteger("TicksFrozen");
	}
}
