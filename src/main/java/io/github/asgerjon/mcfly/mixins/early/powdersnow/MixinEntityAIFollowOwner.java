package io.github.asgerjon.mcfly.mixins.early.powdersnow;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.entities.Freezing;
import net.minecraft.block.Block;
import net.minecraft.entity.ai.EntityAIFollowOwner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 26.3's pets teleport to their owner only onto walkable path nodes (SPEC-modern-movement.md §62.11): powder snow
 * counts as filled for 1.7.10's spot check, so a wolf or an ocelot isn't put into it.
 */
@Mixin(EntityAIFollowOwner.class)
public abstract class MixinEntityAIFollowOwner {

	@WrapOperation(method = "updateTask", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;isNormalCube()Z"))
	private boolean etfu$powderSnowIsNoSpot(Block block, Operation<Boolean> original) {
		return original.call(block) || Freezing.isPowderSnow(block);
	}
}
