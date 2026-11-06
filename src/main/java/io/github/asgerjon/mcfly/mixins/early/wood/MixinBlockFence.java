package io.github.asgerjon.mcfly.mixins.early.wood;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.asgerjon.mcfly.wood.FenceJoin;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.3's fence joining for every `BlockFence` whose joining is 1.7.10's own (SPEC-modern-movement.md §91.5, step C; it
 * replaces EFR's `fencewallconnect` overwrite): asked with no side (other callers), 26.3's rule with any gate joining;
 * asked by the fence's own outline and collision, which know the side, 26.3's rule exactly (vanilla's oak and nether
 * brick fences; another mod's fence with its own joining keeps it).
 */
@Mixin(BlockFence.class)
public abstract class MixinBlockFence {

	@Inject(method = "canConnectFenceTo", at = @At("HEAD"), cancellable = true)
	private void mcfly$joins(IBlockAccess world, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(FenceJoin.joins((Block) (Object) this, world, x, y, z, null));
	}

	@WrapOperation(method = {"setBlockBoundsBasedOnState", "addCollisionBoxesToList"}, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/block/BlockFence;canConnectFenceTo(Lnet/minecraft/world/IBlockAccess;III)Z"))
	private boolean mcfly$joinsThatWay(BlockFence fence, IBlockAccess world, int nx, int ny, int nz, Operation<Boolean> original,
									   @Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 2) int z) {
		return FenceJoin.ownJoining(fence) ? FenceJoin.joins(fence, world, nx, ny, nz, FenceJoin.direction(nx - x, nz - z)) : original.call(fence, world, nx, ny, nz);
	}
}
