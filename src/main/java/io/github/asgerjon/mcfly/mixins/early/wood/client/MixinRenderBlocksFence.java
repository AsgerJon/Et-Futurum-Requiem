package io.github.asgerjon.mcfly.mixins.early.wood.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.asgerjon.mcfly.wood.FenceJoin;
import net.minecraft.block.BlockFence;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 1.7.10's fence renderer asks each side's joining: 26.3's rule with the side known (SPEC-modern-movement.md §91.5, step C). */
@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocksFence {

	@WrapOperation(method = "renderBlockFence", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/block/BlockFence;canConnectFenceTo(Lnet/minecraft/world/IBlockAccess;III)Z"))
	private boolean mcfly$joinsThatWay(BlockFence fence, IBlockAccess world, int nx, int ny, int nz, Operation<Boolean> original,
									   @Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 2) int z) {
		return FenceJoin.ownJoining(fence) ? FenceJoin.joins(fence, world, nx, ny, nz, FenceJoin.direction(nx - x, nz - z)) : original.call(fence, world, nx, ny, nz);
	}
}
