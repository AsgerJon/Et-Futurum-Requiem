package ganymedes01.etfuturum.mixins.early.backlytra;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.elytra.FallFlying;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Server movement checks while gliding (§18.4). Only "moved too quickly" changes; "moved wrongly" stays as it is.
 */
@Mixin(NetHandlerPlayServer.class)
public class MixinNetHandlerPlayServer {

	@Shadow
	public EntityPlayerMP playerEntity;

	/**
	 * "Moved too quickly" allows 300 instead of 100 (squared distance per packet) while gliding, or is skipped with the
	 * disableElytraMovementCheck game rule.
	 */
	@ModifyConstant(method = "processPlayer", constant = @Constant(doubleValue = 100.0D))
	private double etfu$glidingSpeedLimit(double limit) {
		if (!FallFlying.isFallFlying(playerEntity)) {
			return limit;
		}
		if (playerEntity.worldObj.getGameRules().getGameRuleBooleanValue("disableElytraMovementCheck")) {
			return Double.POSITIVE_INFINITY;
		}
		return 300.0D;
	}

	/**
	 * Gliding doesn't count as floating, so it never gets you kicked for flying. Counting as "near a block" resets the
	 * floating counter.
	 */
	@ModifyExpressionValue(method = "processPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldServer;checkBlockCollision(Lnet/minecraft/util/AxisAlignedBB;)Z"))
	private boolean etfu$glidingIsNotFloating(boolean nearBlock) {
		return nearBlock || FallFlying.isFallFlying(playerEntity);
	}
}
