package io.github.asgerjon.mcfly.mixins.early.horses;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.entities.ModernHorses;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Undead horses take a lead (SPEC-modern-movement.md §64.6): 26.3's `Mob.canBeLeashed` is "not an enemy", so every
 * horse does; its zombie horse only when tamed or not ridden by a mob (`ZombieHorse.canBeLeashed`, for its jockeys).
 * 1.7.10's `allowLeashing` is `!isUndead() && super`: the undead test answers "no" except for that zombie horse.
 */
@Mixin(EntityHorse.class)
public abstract class MixinEntityHorseLeads extends EntityAnimal {

	protected MixinEntityHorseLeads(World world) {
		super(world);
	}

	@Shadow
	public abstract int getHorseType();

	@Shadow
	public abstract boolean isTame();

	@ModifyExpressionValue(method = "allowLeashing", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/EntityHorse;func_110256_cu()Z"))
	private boolean etfu$undeadTakeLeads(boolean undead) {
		return undead && getHorseType() == ModernHorses.ZOMBIE && !isTame() && riddenByEntity instanceof EntityLiving;
	}
}
