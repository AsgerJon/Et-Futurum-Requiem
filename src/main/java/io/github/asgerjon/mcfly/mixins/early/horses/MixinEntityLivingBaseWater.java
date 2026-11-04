package io.github.asgerjon.mcfly.mixins.early.horses;

import ganymedes01.etfuturum.entities.ModernHorses;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Horses' sinking and the skeleton horse's drag in water (SPEC-modern-movement.md §64.6) when modern swimming is off:
 * with it on, `modernmovement.MixinEntityLivingBase` holds these constants and asks {@link ModernHorses} itself (two
 * `@ModifyConstant` on one constant don't chain: the second is skipped, and its required injection fails the game).
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBaseWater extends Entity {

	protected MixinEntityLivingBaseWater(World world) {
		super(world);
	}

	/** Ordinals 0 and 2 are x and z in the water branch; y (ordinal 1) stays 0.8. */
	@ModifyConstant(method = "moveEntityWithHeading", constant = @Constant(doubleValue = 0.800000011920929D, ordinal = 0))
	private double etfu$skeletonHorseDragX(double drag) {
		return ModernHorses.waterDrag(this, drag);
	}

	@ModifyConstant(method = "moveEntityWithHeading", constant = @Constant(doubleValue = 0.800000011920929D, ordinal = 2))
	private double etfu$skeletonHorseDragZ(double drag) {
		return ModernHorses.waterDrag(this, drag);
	}

	/** Ordinal 0 is the water branch's sinking; ordinal 1 is lava's. */
	@ModifyConstant(method = "moveEntityWithHeading", constant = @Constant(doubleValue = 0.02D, ordinal = 0))
	private double etfu$horseSinking(double sinking) {
		return ModernHorses.waterSinking(this, sinking);
	}
}
