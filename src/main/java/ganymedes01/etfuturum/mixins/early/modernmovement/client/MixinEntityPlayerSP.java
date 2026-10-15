package ganymedes01.etfuturum.mixins.early.modernmovement.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.movement.IClientPosePlayer;
import ganymedes01.etfuturum.movement.IPosePlayer;
import ganymedes01.etfuturum.movement.ModernMovement;
import ganymedes01.etfuturum.movement.PlayerPose;
import ganymedes01.etfuturum.spectator.SpectatorMode;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.potion.Potion;
import net.minecraft.util.MovementInput;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSP implements IClientPosePlayer {

	@Shadow
	public MovementInput movementInput;

	@Shadow
	protected Minecraft mc;

	@Shadow
	protected int sprintToggleTimer;

	@Unique
	private boolean etfu$crouching;

	@Unique
	private boolean etfu$wasShiftDown;

	@Unique
	private boolean etfu$hadForwardImpulse;

	@Unique
	private int etfu$waterVisionTime;

	/**
	 * Right before the keys are read: remember last tick's shift and forward input for the sprint rules, and update
	 * the crouching flag from them (§6.1).
	 */
	@Inject(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/MovementInput;updatePlayerMoveState()V"))
	private void etfu$beforeInput(CallbackInfo ci) {
		EntityPlayerSP player = (EntityPlayerSP) (Object) this;
		etfu$wasShiftDown = this.movementInput.sneak;
		etfu$hadForwardImpulse = this.movementInput.moveForward > 1.0E-5F;
		etfu$crouching = ConfigMixins.enableModernSneaking
				&& !player.capabilities.isFlying
				&& !((IPosePlayer) player).etfu$isSwimming()
				&& !player.isRiding()
				&& ModernMovement.canFit(player, PlayerPose.CROUCHING)
				&& (this.movementInput.sneak || !player.isPlayerSleeping() && !ModernMovement.canFit(player, PlayerPose.STANDING));
	}

	/**
	 * 1.7.10 dips the view about 0.08 blocks while sneaking by bumping ySize to 0.2. The crouching pose lowers the eyes
	 * by 0.35 instead (§13.3), so skip the bump. ySize's other job, smoothing step-ups, is untouched.
	 */
	@ModifyExpressionValue(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/util/MovementInput;sneak:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
	private boolean etfu$skipSneakCameraDip(boolean sneak) {
		return sneak && !ConfigMixins.enableModernSneaking;
	}

	/**
	 * With modern swimming, the sprint rules below replace 1.7.10's: its double-tap block (the first onGround read),
	 * its sprint-key start, its stop rule and its 600-tick sprint timeout are switched off.
	 */
	@ModifyExpressionValue(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;onGround:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
	private boolean etfu$skipVanillaDoubleTap(boolean onGround) {
		return onGround && !ConfigMixins.enableModernSwimming;
	}

	@WrapWithCondition(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;setSprinting(Z)V", ordinal = 0))
	private boolean etfu$skipVanillaSprintTimeout(EntityPlayerSP player, boolean sprinting) {
		return !ConfigMixins.enableModernSwimming;
	}

	@WrapWithCondition(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;setSprinting(Z)V", ordinal = 2))
	private boolean etfu$skipVanillaSprintKey(EntityPlayerSP player, boolean sprinting) {
		return !ConfigMixins.enableModernSwimming;
	}

	@WrapWithCondition(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;setSprinting(Z)V", ordinal = 3))
	private boolean etfu$skipVanillaSprintStop(EntityPlayerSP player, boolean sprinting) {
		return !ConfigMixins.enableModernSwimming;
	}

	/**
	 * Modern sprint rules (§6.3), at the same point in the tick as modern: after pushing out of blocks, before the
	 * creative flight toggle.
	 */
	@Inject(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;func_145771_j(DDD)Z", ordinal = 3, shift = At.Shift.AFTER))
	private void etfu$modernSprinting(CallbackInfo ci) {
		if (!ConfigMixins.enableModernSwimming) {
			return;
		}
		EntityPlayerSP player = (EntityPlayerSP) (Object) this;
		boolean forwardImpulse = this.movementInput.moveForward > 1.0E-5F;

		if (etfu$wasShiftDown || player.isUsingItem() && !player.isRiding() || mc.gameSettings.keyBindBack.getIsKeyPressed()) {
			this.sprintToggleTimer = 0;
		}

		if (etfu$canStartSprinting(player, forwardImpulse)) {
			if (!etfu$hadForwardImpulse) {
				if (this.sprintToggleTimer > 0) {
					player.setSprinting(true);
				} else {
					this.sprintToggleTimer = 7;
				}
			}
			if (mc.gameSettings.keyBindSprint.getIsKeyPressed()) {
				player.setSprinting(true);
			}
		}

		if (player.isSprinting()) {
			if (((IPosePlayer) player).etfu$isSwimming()) {
				if (!etfu$isSprintingPossible(player, true) || !player.isInWater()
						|| !forwardImpulse && !player.onGround && !this.movementInput.sneak) {
					player.setSprinting(false);
				}
			} else if (!etfu$isSprintingPossible(player, player.capabilities.isFlying) || !forwardImpulse || player.isCollidedHorizontally) {
				player.setSprinting(false);
			}
		}
	}

	@Unique
	private boolean etfu$canStartSprinting(EntityPlayerSP player, boolean forwardImpulse) {
		boolean underWater = etfu$isUnderWater(player);
		return !player.isSprinting()
				&& forwardImpulse
				&& etfu$isSprintingPossible(player, player.capabilities.isFlying)
				&& !player.isUsingItem()
				&& (!ModernMovement.isElytraFlying(player) || underWater)
				&& (!etfu$isMovingSlowly() || underWater);
	}

	/**
	 * Not blind, enough food (or able to fly), and not in shallow water unless that's allowed. Shallow water = touching
	 * water without the eyes under it.
	 */
	@Unique
	private boolean etfu$isSprintingPossible(EntityPlayerSP player, boolean allowShallowWater) {
		boolean inShallowWater = player.isInWater() && !etfu$isUnderWater(player);
		return !player.isPotionActive(Potion.blindness)
				&& (player.getFoodStats().getFoodLevel() > 6 || player.capabilities.allowFlying)
				&& (allowShallowWater || !inShallowWater);
	}

	/**
	 * The local player's "under water" is last tick's eye check, like modern's.
	 */
	@Unique
	private boolean etfu$isUnderWater(EntityPlayerSP player) {
		return ((IPosePlayer) player).etfu$wasEyeInWater();
	}

	/**
	 * Holding shift while in water pulls you down (§6.4), right before the movement step.
	 */
	@Inject(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;onLivingUpdate()V"))
	private void etfu$sinkWithShift(CallbackInfo ci) {
		EntityPlayerSP player = (EntityPlayerSP) (Object) this;
		if (ConfigMixins.enableModernSwimming && player.isInWater() && this.movementInput.sneak) {
			player.motionY -= 0.04D;
		}
		etfu$updateWaterVision(player);
	}

	/**
	 * The eyes adjust to water: +1 per tick with the eyes under water (+10 in spectator mode), -10 per tick out of it,
	 * kept between 0 and 600 (§6.4).
	 */
	@Unique
	private void etfu$updateWaterVision(EntityPlayerSP player) {
		if (player.isInsideOfMaterial(Material.water)) {
			int speed = ConfigMixins.enableSpectatorMode && SpectatorMode.isSpectator(player) ? 10 : 1;
			etfu$waterVisionTime = Math.min(etfu$waterVisionTime + speed, 600);
		} else if (etfu$waterVisionTime > 0) {
			etfu$waterVisionTime = Math.max(etfu$waterVisionTime - 10, 0);
		}
	}

	@Override
	public float etfu$getWaterVision() {
		EntityPlayerSP player = (EntityPlayerSP) (Object) this;
		if (!player.isInsideOfMaterial(Material.water)) {
			return 0.0F;
		}
		if (etfu$waterVisionTime >= 600) {
			return 1.0F;
		}
		float firstFiveSeconds = Math.min(etfu$waterVisionTime / 100.0F, 1.0F);
		float rest = etfu$waterVisionTime < 100 ? 0.0F : Math.min((etfu$waterVisionTime - 100) / 500.0F, 1.0F);
		return firstFiveSeconds * 0.6F + rest * 0.39999998F;
	}

	@Override
	public boolean etfu$isCrouching() {
		return etfu$crouching;
	}

	@Override
	public boolean etfu$isMovingSlowly() {
		EntityPlayerSP player = (EntityPlayerSP) (Object) this;
		boolean visuallyCrawling = ((IPosePlayer) player).etfu$getPose() == PlayerPose.SWIMMING && !player.isInWater();
		boolean crouching = ConfigMixins.enableModernSneaking ? etfu$crouching : this.movementInput.sneak;
		return crouching || visuallyCrawling;
	}
}
