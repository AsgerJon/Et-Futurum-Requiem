package io.github.asgerjon.mcfly.mixins.early.base.client;

import io.github.asgerjon.mcfly.model.client.PottedModels;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A flower pot holding a McFly plant drawn as 26.3's potted model (SPEC-modern-movement.md §90, step D2), in place of
 * 1.7.10's pot; the cracks of a pot being broken (an override texture) left to 1.7.10's.
 */
@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocksFlowerPot {

	@Shadow
	public IBlockAccess blockAccess;

	@Inject(method = "renderBlockFlowerpot", at = @At("HEAD"), cancellable = true)
	private void mcfly$pottedModel(BlockFlowerPot pot, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		if (!((RenderBlocks) (Object) this).hasOverrideBlockTexture() && PottedModels.draw(blockAccess, pot, x, y, z)) {
			cir.setReturnValue(true);
		}
	}
}
