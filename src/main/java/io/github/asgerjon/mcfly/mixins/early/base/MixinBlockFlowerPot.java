package io.github.asgerjon.mcfly.mixins.early.base;

import io.github.asgerjon.mcfly.base.McFlyPots;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFlowerPot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.7.10's flower pot takes McFly's pottable plants too (SPEC-modern-movement.md §90, step D2): its fixed list
 * (`func_149928_a`, the plant's block and the item's damage) widened by {@link McFlyPots#model}.
 */
@Mixin(BlockFlowerPot.class)
public abstract class MixinBlockFlowerPot {

	@Inject(method = "func_149928_a", at = @At("HEAD"), cancellable = true)
	private void mcfly$takesOurPlants(Block plant, int meta, CallbackInfoReturnable<Boolean> cir) {
		if (McFlyPots.model(plant, meta) != null) {
			cir.setReturnValue(true);
		}
	}
}
