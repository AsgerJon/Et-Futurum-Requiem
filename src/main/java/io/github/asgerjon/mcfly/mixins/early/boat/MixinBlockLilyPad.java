package io.github.asgerjon.mcfly.mixins.early.boat;

import io.github.asgerjon.mcfly.boat.McFlyBoat;
import net.minecraft.block.BlockLilyPad;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Our boats go through lily pads as 1.7.10's boat does (SPEC-modern-movement.md 91.12): 1.7.10's pad blocks the whole
 * cell's side, so a boat would stop at the cell and never be in it to break it (26.3's `LilyPadBlock.entityInside`;
 * its pad is inset a sixteenth, and a boat stopped against it is in the cell).
 */
@Mixin(BlockLilyPad.class)
public abstract class MixinBlockLilyPad {

	@Inject(method = "addCollisionBoxesToList", at = @At("HEAD"), cancellable = true)
	private void mcfly$noPadForBoats(World world, int x, int y, int z, AxisAlignedBB mask, List<AxisAlignedBB> list, Entity collider, CallbackInfo ci) {
		if (collider instanceof McFlyBoat) {
			ci.cancel();
		}
	}
}
