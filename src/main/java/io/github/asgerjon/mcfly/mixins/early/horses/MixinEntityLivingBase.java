package io.github.asgerjon.mcfly.mixins.early.horses;

import ganymedes01.etfuturum.entities.ModernHorses;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Horses are pushed through water as 26.3's mobs are (SPEC-modern-movement.md §64.6, 26.3's `travelInWater`): 0.02 a
 * tick in 1.7.10's water branch of `moveEntityWithHeading` (1.7.10 pushes AI mobs 0.04). This `@ModifyArg` chains with
 * `modernmovement.MixinEntityLivingBase`'s on the same argument (Depth Strider, which horses don't wear). Their sinking
 * and the skeleton horse's drag are constants that hook already holds: see {@link ganymedes01.etfuturum.entities.ModernHorses#waterDrag}.
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends Entity {

	protected MixinEntityLivingBase(World world) {
		super(world);
	}

	/** Ordinal 0 is the water branch's `moveFlying`. */
	@ModifyArg(method = "moveEntityWithHeading", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;moveFlying(FFF)V", ordinal = 0), index = 2)
	private float etfu$horsePush(float push) {
		return (Object) this instanceof EntityHorse ? ModernHorses.WATER_PUSH : push;
	}
}
