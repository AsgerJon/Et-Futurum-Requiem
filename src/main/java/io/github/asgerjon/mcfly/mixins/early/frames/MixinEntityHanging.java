package io.github.asgerjon.mcfly.mixins.early.frames;

import ganymedes01.etfuturum.entities.ModernFrames;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Item frames on floors (`hangingDirection` 4) and ceilings (5) (SPEC-modern-movement.md §67.3): placed as 1.7.10
 * places a wall's, 0.5625 out from its block's middle, flat, 12 by 12 pixels less 1/32 a side; it stays while its
 * block is solid, no block is in its box and no other hanging entity overlaps it (26.3's `survives`). Walls, and
 * paintings, as 1.7.10.
 */
@Mixin(EntityHanging.class)
public abstract class MixinEntityHanging extends Entity {

	@Shadow
	public int hangingDirection;
	@Shadow
	public int field_146063_b;
	@Shadow
	public int field_146064_c;
	@Shadow
	public int field_146062_d;

	protected MixinEntityHanging(World world) {
		super(world);
	}

	@Shadow
	public abstract int getWidthPixels();

	@Inject(method = "setDirection", at = @At("HEAD"), cancellable = true)
	private void etfu$floorOrCeiling(int direction, CallbackInfo ci) {
		if (!ModernFrames.isFloorOrCeiling(direction) || !((Object) this instanceof EntityItemFrame)) {
			return;
		}
		hangingDirection = direction;
		prevRotationYaw = rotationYaw = 0.0F;
		prevRotationPitch = rotationPitch = direction == ModernFrames.FLOOR ? -90.0F : 90.0F;
		float half = getWidthPixels() / 32.0F, thin = 0.5F / 32.0F, inset = -0.03125F;
		float x = field_146063_b + 0.5F;
		float y = field_146064_c + 0.5F + (direction == ModernFrames.FLOOR ? 0.5625F : -0.5625F);
		float z = field_146062_d + 0.5F;
		setPosition(x, y, z);
		boundingBox.setBounds(x - half - inset, y - thin - inset, z - half - inset, x + half + inset, y + thin + inset, z + half + inset);
		ci.cancel();
	}

	@Inject(method = "onValidSurface", at = @At("HEAD"), cancellable = true)
	private void etfu$floorOrCeilingSurface(CallbackInfoReturnable<Boolean> cir) {
		if (!ModernFrames.isFloorOrCeiling(hangingDirection)) {
			return;
		}
		if (!worldObj.getBlock(field_146063_b, field_146064_c, field_146062_d).getMaterial().isSolid()
			|| !worldObj.getCollidingBoundingBoxes(this, boundingBox).isEmpty()) {
			cir.setReturnValue(false);
			return;
		}
		for (Object other : worldObj.getEntitiesWithinAABBExcludingEntity(this, boundingBox)) {
			if (other instanceof EntityHanging) {
				cir.setReturnValue(false);
				return;
			}
		}
		cir.setReturnValue(true);
	}
}
