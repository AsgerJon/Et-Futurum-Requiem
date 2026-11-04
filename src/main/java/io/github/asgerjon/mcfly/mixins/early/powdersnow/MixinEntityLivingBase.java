package io.github.asgerjon.mcfly.mixins.early.powdersnow;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.blocks.BlockPowderSnow;
import ganymedes01.etfuturum.entities.Freezing;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

/**
 * The living side of powder snow (SPEC-modern-movement.md §62.2, §62.3, §62.11): the freezing step, climbing out for
 * walkers, and no dismounting into it.
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends Entity {

	@Shadow
	protected boolean isJumping;

	public MixinEntityLivingBase(World world) {
		super(world);
	}

	/** 26.3 {@code aiStep}'s "freezing", once the living update (and its move) has run. */
	@Inject(method = "onLivingUpdate", at = @At("TAIL"))
	private void etfu$freezing(CallbackInfo ci) {
		Freezing.tick((EntityLivingBase) (Object) this);
	}

	/** 26.3: a walker that was in powder snow and is jumping or pushing against a block goes up at 0.2, as on a ladder. */
	@Inject(method = "moveEntityWithHeading", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;moveEntity(DDD)V", ordinal = 2, shift = At.Shift.AFTER))
	private void etfu$climbOutOfPowderSnow(float strafe, float forward, CallbackInfo ci) {
		boolean asked = ((ganymedes01.etfuturum.ducks.IFreezingEntity) this).etfu$takeClimbRequest();
		if ((isCollidedHorizontally || isJumping || asked) && Freezing.wasInPowderSnow(this) && Freezing.canWalkOnPowderSnow(this)) {
			motionY = 0.2D;
		}
	}

	/** 26.3 {@code DismountHelper}: a spot with powder snow in it is no place to get off. */
	@SuppressWarnings({"rawtypes", "unchecked"})
	@WrapOperation(method = "dismountEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;func_147461_a(Lnet/minecraft/util/AxisAlignedBB;)Ljava/util/List;"))
	private List etfu$notIntoPowderSnow(World world, AxisAlignedBB spot, Operation<List> original) {
		List colliding = original.call(world, spot);
		return colliding.isEmpty() && BlockPowderSnow.anyIn(world, spot) ? Collections.singletonList(spot) : colliding;
	}
}
