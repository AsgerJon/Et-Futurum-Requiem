package ganymedes01.etfuturum.mixins.early.modernmovement;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.ModEnchantments;
import ganymedes01.etfuturum.movement.IPosePlayer;
import ganymedes01.etfuturum.movement.ModernWater;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Modern water movement for players (§11). Other living entities keep 1.7.10's, except that Depth Strider works for
 * anything wearing the boots, as in modern.
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends Entity {

	public MixinEntityLivingBase(World world) {
		super(world);
	}

	/**
	 * Horizontal water drag: 0.9 while sprinting (swimming), else 0.8 as before, then moved towards 0.546 by Depth
	 * Strider. Ordinals 0 and 2 are x and z in the water branch; y (ordinal 1) stays 0.8.
	 */
	@ModifyConstant(method = "moveEntityWithHeading", constant = @Constant(doubleValue = 0.800000011920929D, ordinal = 0))
	private double etfu$waterDragX(double drag) {
		return etfu$horizontalWaterDrag(drag);
	}

	@ModifyConstant(method = "moveEntityWithHeading", constant = @Constant(doubleValue = 0.800000011920929D, ordinal = 2))
	private double etfu$waterDragZ(double drag) {
		return etfu$horizontalWaterDrag(drag);
	}

	@Unique
	private double etfu$horizontalWaterDrag(double drag) {
		if ((Object) this instanceof EntityPlayer && this.isSprinting()) {
			drag = 0.9F;
		}
		float efficiency = etfu$waterMovementEfficiency();
		if (efficiency > 0.0F) {
			drag += (0.54600006F - drag) * efficiency;
		}
		return drag;
	}

	/**
	 * Acceleration in water (ordinal 0 is the water branch's moveFlying): Depth Strider moves it towards the walking
	 * speed by the water movement efficiency (§11 step 6.4).
	 */
	@ModifyArg(method = "moveEntityWithHeading", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;moveFlying(FFF)V", ordinal = 0), index = 2)
	private float etfu$waterAcceleration(float acceleration) {
		float efficiency = etfu$waterMovementEfficiency();
		if (efficiency > 0.0F) {
			acceleration += (((EntityLivingBase) (Object) this).getAIMoveSpeed() - acceleration) * efficiency;
		}
		return acceleration;
	}

	/**
	 * Water movement efficiency from Depth Strider on the boots: a third per level, at most 1, halved while not on
	 * the ground.
	 */
	@Unique
	private float etfu$waterMovementEfficiency() {
		if (ModEnchantments.depthStrider == null) {
			return 0.0F;
		}
		ItemStack boots = ((EntityLivingBase) (Object) this).getEquipmentInSlot(1);
		int level = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.depthStrider.effectId, boots);
		float efficiency = Math.min(level * 0.33333334F, 1.0F);
		return this.onGround ? efficiency : efficiency * 0.5F;
	}

	/**
	 * Sinking in water: gravity / 16 = 0.005 per tick instead of 0.02, and none at all while sprinting (swimming).
	 * Ordinal 0 is the water branch; ordinal 1 is lava.
	 */
	@ModifyConstant(method = "moveEntityWithHeading", constant = @Constant(doubleValue = 0.02D, ordinal = 0))
	private double etfu$waterSinking(double sinking) {
		if ((Object) this instanceof EntityPlayer) {
			return this.isSprinting() ? 0.0D : 0.005D;
		}
		return sinking;
	}

	/**
	 * Climbing a ladder or vine while in water, against a wall: move up at 0.2, before the drag is applied.
	 */
	@Inject(method = "moveEntityWithHeading", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;moveEntity(DDD)V", ordinal = 0, shift = At.Shift.AFTER))
	private void etfu$climbInWater(float strafe, float forward, CallbackInfo ci) {
		if ((Object) this instanceof EntityPlayer && this.isCollidedHorizontally && ((EntityLivingBase) (Object) this).isOnLadder()) {
			this.motionY = 0.2D;
		}
	}

	/**
	 * Jumping in water (§11 step 4): standing on the ground in water no deeper than 0.4 does a normal jump; otherwise
	 * jump swims up. The ground branch then only needs vanilla's onGround check.
	 */
	@ModifyExpressionValue(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isInWater()Z", ordinal = 0))
	private boolean etfu$swimUpUnlessShallow(boolean inWater) {
		if ((Object) this instanceof IPosePlayer) {
			double waterHeight = ((IPosePlayer) this).etfu$getWaterHeight();
			return inWater && waterHeight > 0.0D && !(this.onGround && waterHeight <= ModernWater.JUMP_THRESHOLD);
		}
		return inWater;
	}

	/**
	 * Tiny motion cut-off (§11 step 1). 1.7.10 zeroes each axis below 0.005; modern zeroes horizontal motion when its
	 * squared length is below 9e-6 and vertical motion below 0.003. Without this, slow sinking and the minimum current
	 * push (0.0045) would be wiped out every tick.
	 */
	@Inject(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(D)D", ordinal = 0))
	private void etfu$modernMotionCutoff(CallbackInfo ci) {
		if ((Object) this instanceof EntityPlayer) {
			if (this.motionX * this.motionX + this.motionZ * this.motionZ < 9.0E-6D) {
				this.motionX = 0.0D;
				this.motionZ = 0.0D;
			}
			if (Math.abs(this.motionY) < 0.003D) {
				this.motionY = 0.0D;
			}
		}
	}

	@ModifyConstant(method = "onLivingUpdate", constant = @Constant(doubleValue = 0.005D))
	private double etfu$skipVanillaCutoff(double cutoff) {
		return (Object) this instanceof EntityPlayer ? 0.0D : cutoff;
	}
}
