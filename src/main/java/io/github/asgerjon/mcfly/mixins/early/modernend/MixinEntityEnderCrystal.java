package io.github.asgerjon.mcfly.mixins.early.modernend;

import ganymedes01.etfuturum.world.end.modern.EndCrystals;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.7.10's end crystal as 26.3's (SPEC-modern-movement.md §76.6): `EndCrystals` has the rules. */
@Mixin(EntityEnderCrystal.class)
public class MixinEntityEnderCrystal {

	@Inject(method = "entityInit", at = @At("TAIL"))
	private void etfu$define(CallbackInfo ci) {
		EndCrystals.define((EntityEnderCrystal) (Object) this);
	}

	/** A crystal with a beam is drawn off-screen and from far (26.3's shouldRender), its beam reaching far. */
	@Inject(method = "onUpdate", at = @At("HEAD"))
	private void etfu$beamSeenFar(CallbackInfo ci) {
		EntityEnderCrystal crystal = (EntityEnderCrystal) (Object) this;
		if (crystal.worldObj.isRemote) {
			boolean beam = EndCrystals.beamTarget(crystal) != null;
			crystal.ignoreFrustumCheck = beam;
			crystal.renderDistanceWeight = beam ? 10.0 : 1.0;
		}
	}

	/** 1.7.10's tick puts fire over any block in the End: 26.3's only on air (its spin and health kept). */
	@Inject(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/MathHelper;floor_double(D)I", ordinal = 0), cancellable = true)
	private void etfu$fire(CallbackInfo ci) {
		EndCrystals.tick((EntityEnderCrystal) (Object) this);
		ci.cancel();
	}

	@Inject(method = "attackEntityFrom", at = @At("HEAD"), cancellable = true)
	private void etfu$hurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(EndCrystals.hurt((EntityEnderCrystal) (Object) this, source));
	}

	@Inject(method = "writeEntityToNBT", at = @At("TAIL"))
	private void etfu$save(NBTTagCompound tag, CallbackInfo ci) {
		EndCrystals.save((EntityEnderCrystal) (Object) this, tag);
	}

	@Inject(method = "readEntityFromNBT", at = @At("TAIL"))
	private void etfu$load(NBTTagCompound tag, CallbackInfo ci) {
		EndCrystals.load((EntityEnderCrystal) (Object) this, tag);
	}
}
