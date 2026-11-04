package io.github.asgerjon.mcfly.mixins.early.quickwins4;

import ganymedes01.etfuturum.ducks.ILastDamage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The last damage taken and when (26.3 {@code lastDamageSource}, {@code lastDamageStamp}; spec §63.4). */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends Entity implements ILastDamage {

	@Unique
	private DamageSource etfu$lastDamageSource;
	@Unique
	private long etfu$lastDamageTime;

	public MixinEntityLivingBase(World world) {
		super(world);
	}

	/** Where 1.7.10 takes the hurt (past the invulnerable time), as 26.3's {@code hurtServer} notes it. */
	@Inject(method = "attackEntityFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;damageEntity(Lnet/minecraft/util/DamageSource;F)V"))
	private void etfu$noteDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		etfu$lastDamageSource = source;
		etfu$lastDamageTime = worldObj.getTotalWorldTime();
	}

	@Override
	public DamageSource etfu$lastDamage(int timeout) {
		return worldObj.getTotalWorldTime() - etfu$lastDamageTime > timeout ? null : etfu$lastDamageSource;
	}
}
