package io.github.asgerjon.mcfly.mixins.early.horses;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import ganymedes01.etfuturum.entities.ModernHorses;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Undead horses in water (SPEC-modern-movement.md §64.6): a skeleton horse keeps its rider under water (26.3's
 * `dismounts_underwater` has every horse but it), zombie and skeleton horses breathe under water (`#undead` is in
 * `can_breathe_under_water`), and the skeleton horse's own sounds in water for its ambient sound and its jump.
 */
@Mixin(EntityHorse.class)
public abstract class MixinEntityHorse extends EntityAnimal {

	protected MixinEntityHorse(World world) {
		super(world);
	}

	@Shadow
	public abstract int getHorseType();

	@Shadow
	public abstract boolean func_110256_cu();

	/**
	 * 1.7.10's swim task, which every horse gets before its type is known, also sets the horse's paths to swim: they
	 * start at the water's top, and the horse jumps towards a point above it. 26.3's skeleton horse has no `FloatGoal`,
	 * so its paths never swim; so here once it is a skeleton horse (spawn eggs, saved horses, commands all set the type).
	 */
	@Inject(method = "setHorseType", at = @At("TAIL"))
	private void etfu$skeletonHorsesDontSwim(int type, CallbackInfo ci) {
		getNavigator().setCanSwim(type != ModernHorses.SKELETON);
	}

	@Override
	public boolean shouldDismountInWater(Entity rider) {
		return getHorseType() != ModernHorses.SKELETON && super.shouldDismountInWater(rider);
	}

	@Override
	public boolean canBreatheUnderwater() {
		return func_110256_cu() || super.canBreatheUnderwater();
	}

	/** 26.3's `SkeletonHorse.getAmbientSound`: `ambient_water` with its eyes in water. */
	@ModifyReturnValue(method = "getLivingSound", at = @At("RETURN"))
	private String etfu$ambientInWater(String sound) {
		return getHorseType() == ModernHorses.SKELETON && isInsideOfMaterial(Material.water) ? ModernHorses.AMBIENT_WATER : sound;
	}

	/** 26.3's `SkeletonHorse.playJumpSound`: `jump_water` (0.4, 1.0, as the dry one) in water. */
	@ModifyArg(method = "moveEntityWithHeading", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/EntityHorse;playSound(Ljava/lang/String;FF)V"), index = 0)
	private String etfu$jumpInWater(String sound) {
		return getHorseType() == ModernHorses.SKELETON && isInWater() ? ModernHorses.JUMP_WATER : sound;
	}
}
