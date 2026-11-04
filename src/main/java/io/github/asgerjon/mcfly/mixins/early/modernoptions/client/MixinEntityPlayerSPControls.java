package io.github.asgerjon.mcfly.mixins.early.modernoptions.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.client.options.AutoJump;
import ganymedes01.etfuturum.client.options.ControlOptions;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MovementInput;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.3's Sprint Window and Auto-Jump (spec §54.12) on the client player. Each method also names Player API's local
 * copy (see modernmovement.client.MixinEntityPlayerSP).
 */
@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSPControls {

	@Shadow
	public MovementInput movementInput;

	/** 1 after a move that wants a jump: the jump comes on the next tick, as 26.3's autoJumpTime. */
	@Unique
	private int etfu$autoJumpTime;

	@Unique
	private double etfu$xBeforeMove;

	@Unique
	private double etfu$zBeforeMove;

	/**
	 * 1.7.10's own double-tap sprint start sets its timer to 7: the Sprint Window instead. The first 7 in the method;
	 * the second is the creative flight toggle's. EFR's modern sprint start reads the option itself.
	 */
	@Dynamic("Player API moves the body of onLivingUpdate to localOnLivingUpdate")
	@ModifyConstant(method = {"onLivingUpdate", "localOnLivingUpdate"}, constant = @Constant(intValue = 7, ordinal = 0))
	private int etfu$sprintWindow(int seven) {
		return ControlOptions.sprintWindow;
	}

	@Dynamic("Player API moves the body of onLivingUpdate to localOnLivingUpdate")
	@Inject(method = {"onLivingUpdate", "localOnLivingUpdate"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;onLivingUpdate()V"))
	private void etfu$beforeTheMove(CallbackInfo ci) {
		EntityPlayerSP player = (EntityPlayerSP) (Object) this;
		etfu$xBeforeMove = player.posX;
		etfu$zBeforeMove = player.posZ;
	}

	/** After the tick's move, as 26.3 after each move: whether to jump on the next tick. */
	@Dynamic("Player API moves the body of onLivingUpdate to localOnLivingUpdate")
	@Inject(method = {"onLivingUpdate", "localOnLivingUpdate"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;onLivingUpdate()V", shift = At.Shift.AFTER))
	private void etfu$afterTheMove(CallbackInfo ci) {
		EntityPlayerSP player = (EntityPlayerSP) (Object) this;
		if (ControlOptions.autoJump && etfu$autoJumpTime <= 0
			&& AutoJump.wanted(player, movementInput, player.posX - etfu$xBeforeMove, player.posZ - etfu$zBeforeMove)) {
			etfu$autoJumpTime = 1;
		}
	}

	/**
	 * The auto-jump: the player jumps as if the key were down, but the key itself stays up, so the creative flight
	 * toggle (which reads the key) never counts it, as 26.3.
	 */
	@Dynamic("Player API may move the body of updateEntityActionState to localUpdateEntityActionState")
	@ModifyExpressionValue(method = {"updateEntityActionState", "localUpdateEntityActionState"}, at = @At(value = "FIELD", target = "Lnet/minecraft/util/MovementInput;jump:Z", opcode = Opcodes.GETFIELD))
	private boolean etfu$autoJump(boolean jump) {
		if (etfu$autoJumpTime > 0) {
			etfu$autoJumpTime--;
			return true;
		}
		return jump;
	}
}
