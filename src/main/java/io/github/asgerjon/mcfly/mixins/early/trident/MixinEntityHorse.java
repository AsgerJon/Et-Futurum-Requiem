package io.github.asgerjon.mcfly.mixins.early.trident;

import ganymedes01.etfuturum.trident.ISkeletonTrapHorse;
import ganymedes01.etfuturum.trident.SkeletonTrap;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The skeleton horse trap (SPEC-modern-movement.md §58.3): a skeleton horse (type 4) can be one, saved as 26.3 saves
 * it. A trap springs when a living player comes near, else it counts its ticks and goes at 18 000 unless persistent.
 */
@Mixin(EntityHorse.class)
public abstract class MixinEntityHorse extends EntityAnimal implements ISkeletonTrapHorse {

	@Unique
	private boolean etfu$trap;
	@Unique
	private int etfu$trapTime;

	protected MixinEntityHorse(World world) {
		super(world);
	}

	@Shadow
	public abstract int getHorseType();

	@Override
	public boolean etfu$isTrap() {
		return etfu$trap;
	}

	@Override
	public void etfu$setTrap(boolean trap) {
		etfu$trap = trap;
	}

	@Inject(method = "onLivingUpdate", at = @At("TAIL"))
	private void etfu$skeletonTrap(CallbackInfo ci) {
		if (!etfu$trap || worldObj.isRemote) {
			return;
		}
		EntityHorse horse = (EntityHorse) (Object) this;
		if (SkeletonTrap.playerNear(horse)) {
			SkeletonTrap.spring(horse);
		} else if (!persistenceRequired && etfu$trapTime++ >= SkeletonTrap.LIFE) {
			setDead();
		}
	}

	@Inject(method = "writeEntityToNBT", at = @At("TAIL"))
	private void etfu$writeTrap(NBTTagCompound tag, CallbackInfo ci) {
		if (getHorseType() == 4) {
			tag.setBoolean("SkeletonTrap", etfu$trap);
			tag.setInteger("SkeletonTrapTime", etfu$trapTime);
		}
	}

	@Inject(method = "readEntityFromNBT", at = @At("TAIL"))
	private void etfu$readTrap(NBTTagCompound tag, CallbackInfo ci) {
		etfu$trap = getHorseType() == 4 && tag.getBoolean("SkeletonTrap");
		etfu$trapTime = tag.getInteger("SkeletonTrapTime");
	}
}
