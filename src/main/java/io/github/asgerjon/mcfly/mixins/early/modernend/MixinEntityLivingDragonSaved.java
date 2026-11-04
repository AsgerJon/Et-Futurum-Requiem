package io.github.asgerjon.mcfly.mixins.early.modernend;

import ganymedes01.etfuturum.world.end.modern.dragon.ModernDragon;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The dragon's phase, death time and sitting damage saved under 26.3's keys (SPEC-modern-movement.md §76.7). */
@Mixin(EntityLiving.class)
public class MixinEntityLivingDragonSaved {

	@Inject(method = "writeEntityToNBT", at = @At("TAIL"))
	private void etfu$save(NBTTagCompound tag, CallbackInfo ci) {
		if ((Object) this instanceof EntityDragon dragon && ModernDragon.of(dragon) != null) {
			ModernDragon.of(dragon).save(tag);
		}
	}

	@Inject(method = "readEntityFromNBT", at = @At("TAIL"))
	private void etfu$load(NBTTagCompound tag, CallbackInfo ci) {
		if ((Object) this instanceof EntityDragon dragon && ModernDragon.of(dragon) != null) {
			ModernDragon.of(dragon).load(tag);
		}
	}
}
