package io.github.asgerjon.mcfly.mixins.early.soulfire;

import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Basalt from lava (SPEC-modern-movement.md §88.6), 26.3's `LiquidBlock.shouldSpreadLiquid`: lava over soul soil with
 * blue ice above or beside it becomes basalt, with the fizz (1.7.10's own mixing with water still first, as 26.3 looks
 * for water before ice; the lava in its mixing check, `func_149805_n`, called as it's added and as a neighbour changes).
 */
@Mixin(BlockLiquid.class)
public abstract class MixinBlockLiquid extends Block {

	protected MixinBlockLiquid(Material material) {
		super(material);
	}

	@Shadow
	protected abstract void func_149799_m(World world, int x, int y, int z);

	@Inject(method = "func_149805_n", at = @At("RETURN"))
	private void mcfly$basalt(World world, int x, int y, int z, CallbackInfo ci) {
		if (blockMaterial != Material.lava || !McFlyBlocks.BASALT.isEnabled() || !McFlyBlocks.SOUL_SOIL.isEnabled()
			|| world.getBlock(x, y - 1, z) != McFlyBlocks.SOUL_SOIL.get() || world.getBlock(x, y, z) != this) {
			return;
		}
		Block ice = ganymedes01.etfuturum.ModBlocks.BLUE_ICE.isEnabled() ? ganymedes01.etfuturum.ModBlocks.BLUE_ICE.get() : null;
		if (ice == null) {
			return;
		}
		int[][] around = {{0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
		for (int[] d : around) {
			if (world.getBlock(x + d[0], y + d[1], z + d[2]) == ice) {
				world.setBlock(x, y, z, McFlyBlocks.BASALT.get(), 0, 3);
				func_149799_m(world, x, y, z);
				return;
			}
		}
	}
}
