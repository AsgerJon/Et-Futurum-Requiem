package io.github.asgerjon.mcfly.mixins.early.quickwins4.client;

import ganymedes01.etfuturum.client.renderer.block.CauldronModel;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.renderer.RenderBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.7.10's cauldron drawn as 26.3's model, its water at the same heights (SPEC-modern-movement.md §63.5). */
@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocks {

	@Inject(method = "renderBlockCauldron", at = @At("HEAD"), cancellable = true)
	private void etfu$modernCauldron(BlockCauldron block, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		if (!CauldronModel.isEnabled()) {
			return;
		}
		RenderBlocks renderer = (RenderBlocks) (Object) this;
		CauldronModel.render(renderer.blockAccess, x, y, z, block, renderer);
		int level = renderer.blockAccess.getBlockMetadata(x, y, z);
		if (level > 0) {
			// 1.7.10 draws the water in the cauldron's own colour (its water texture is blue already)
			CauldronModel.content(renderer, block, x, y, z, BlockCauldron.getRenderLiquidLevel(level), BlockLiquid.getLiquidIcon("water_still"));
		}
		cir.setReturnValue(true);
	}
}
