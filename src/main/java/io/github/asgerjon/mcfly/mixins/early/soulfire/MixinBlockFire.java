package io.github.asgerjon.mcfly.mixins.early.soulfire;

import io.github.asgerjon.mcfly.nether.SoulFireBlock;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import net.minecraft.block.BlockFire;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * Fire on a soul base is soul fire (SPEC-modern-movement.md §88.6), as 26.3's `BaseFireBlock.getState`: vanilla fire
 * placed there (flint and steel, a fire charge, fire's own spread) becomes McFly's soul fire as it's added; EFR's soul
 * fire, `minecraft:fire` over a soul base as it saved it, on its next tick (fire ticks randomly).
 */
@Mixin(BlockFire.class)
public abstract class MixinBlockFire {

	private static boolean mcfly$toSoulFire(Object self, World world, int x, int y, int z) {
		if (self != Blocks.fire || !McFlyBlocks.SOUL_FIRE.isEnabled() || world.isRemote || world.getBlock(x, y, z) != Blocks.fire
			|| !SoulFireBlock.isSoulBase(world.getBlock(x, y - 1, z))) {
			return false;
		}
		world.setBlock(x, y, z, McFlyBlocks.SOUL_FIRE.get(), 0, 3);
		return true;
	}

	@Inject(method = "onBlockAdded", at = @At("HEAD"), cancellable = true)
	private void mcfly$soulOnAdd(World world, int x, int y, int z, CallbackInfo ci) {
		if (mcfly$toSoulFire(this, world, x, y, z)) {
			ci.cancel();
		}
	}

	@Inject(method = "updateTick", at = @At("HEAD"), cancellable = true)
	private void mcfly$soulOnTick(World world, int x, int y, int z, Random random, CallbackInfo ci) {
		if (mcfly$toSoulFire(this, world, x, y, z)) {
			ci.cancel();
		}
	}
}
