package ganymedes01.etfuturum.mixins.early.placementfacing;

import net.minecraft.block.BlockPistonBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pistons, dispensers, droppers and EFR's observers are placed by where the placer looks (SPEC-modern-movement.md
 * §33, modern since 1.13): they face back at the placer along the axis the look direction is nearest to, so looking
 * mostly down puts the piston facing up. 1.7.10 decided the vertical cases from where the placer stood instead.
 */
@Mixin(BlockPistonBase.class)
public class MixinBlockPistonBase {

	private static final int DOWN = 0, UP = 1, NORTH = 2, SOUTH = 3, WEST = 4, EAST = 5;

	@Inject(method = "determineOrientation", at = @At("HEAD"), cancellable = true)
	private static void etfu$faceByLook(World world, int x, int y, int z, EntityLivingBase placer, CallbackInfoReturnable<Integer> cir) {
		float pitch = placer.rotationPitch * (float) Math.PI / 180.0F;
		float yaw = -placer.rotationYaw * (float) Math.PI / 180.0F;
		float pitchSin = MathHelper.sin(pitch);
		float pitchCos = MathHelper.cos(pitch);
		float yawSin = MathHelper.sin(yaw);
		float yawCos = MathHelper.cos(yaw);
		float vertical = Math.abs(pitchSin);
		int lookingAt;
		if (Math.abs(yawSin) > Math.abs(yawCos)) {
			lookingAt = vertical > Math.abs(yawSin) * pitchCos ? (pitchSin < 0.0F ? UP : DOWN) : (yawSin > 0.0F ? EAST : WEST);
		} else {
			lookingAt = vertical > Math.abs(yawCos) * pitchCos ? (pitchSin < 0.0F ? UP : DOWN) : (yawCos > 0.0F ? SOUTH : NORTH);
		}
		cir.setReturnValue(lookingAt ^ 1);
	}
}
