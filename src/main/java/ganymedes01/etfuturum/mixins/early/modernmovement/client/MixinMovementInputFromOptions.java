package ganymedes01.etfuturum.mixins.early.modernmovement.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.movement.IClientPosePlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovementInputFromOptions;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MovementInputFromOptions.class)
public class MixinMovementInputFromOptions {

	/**
	 * Slow the input to the sneaking speed while moving slowly (crouching or crawling) instead of while shift is held
	 * (§6.2). The 0.3 factor itself is left alone, so Swift Sneak keeps working.
	 */
	@ModifyExpressionValue(method = "updatePlayerMoveState", at = @At(value = "FIELD", target = "Lnet/minecraft/util/MovementInputFromOptions;sneak:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
	private boolean etfu$slowWhileMovingSlowly(boolean sneak) {
		Object player = Minecraft.getMinecraft().thePlayer;
		return player instanceof IClientPosePlayer ? ((IClientPosePlayer) player).etfu$isMovingSlowly() : sneak;
	}
}
