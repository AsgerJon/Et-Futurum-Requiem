package io.github.asgerjon.mcfly.mixins.early.elytra;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.asgerjon.mcfly.elytra.FallFlying;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The server's movement checks while gliding (SPEC-modern-movement.md §18.4, §85), as 26.3's
 * `ServerGamePacketListenerImpl.handleMovePlayer`: "moved too quickly" at 300 (the square of a packet's move) for a
 * glider instead of 100, none at all with the `disableElytraMovementCheck` game rule; gliding isn't floating, so it
 * never gets the player kicked for flying.
 */
@Mixin(NetHandlerPlayServer.class)
public class MixinNetHandlerPlayServer {

	@Shadow
	public EntityPlayerMP playerEntity;

	@ModifyConstant(method = "processPlayer", constant = @Constant(doubleValue = 100.0D))
	private double mcfly$glidingSpeedLimit(double limit) {
		if (!FallFlying.isFallFlying(playerEntity)) {
			return limit;
		}
		return playerEntity.worldObj.getGameRules().getGameRuleBooleanValue("disableElytraMovementCheck") ? Double.POSITIVE_INFINITY : 300.0D;
	}

	/** 1.7.10's floating count resets near a block: a glider counts as near one. */
	@ModifyExpressionValue(method = "processPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldServer;checkBlockCollision(Lnet/minecraft/util/AxisAlignedBB;)Z"))
	private boolean mcfly$glidingIsNotFloating(boolean nearBlock) {
		return nearBlock || FallFlying.isFallFlying(playerEntity);
	}
}
