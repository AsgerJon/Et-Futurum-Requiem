package io.github.asgerjon.mcfly.mixins.early.base;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.asgerjon.mcfly.base.RandomTicking;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Random;

/** The world's random tick to a block's own `randomTick` where it has one (SPEC-modern-movement.md §84, `RandomTicking`). */
@Mixin(WorldServer.class)
public class MixinWorldServerRandomTick {

	@WrapOperation(method = "func_147456_g", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;updateTick(Lnet/minecraft/world/World;IIILjava/util/Random;)V"))
	private void mcfly$randomTick(Block block, World world, int x, int y, int z, Random random, Operation<Void> original) {
		if (block instanceof RandomTicking) {
			((RandomTicking) block).randomTick(world, x, y, z, random);
		} else {
			original.call(block, world, x, y, z, random);
		}
	}
}
