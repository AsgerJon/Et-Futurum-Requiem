package io.github.asgerjon.mcfly.mixins.early.nether;

import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockNetherrack;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Random;

/**
 * Netherrack takes bonemeal (SPEC-modern-movement.md §88.7), as 26.3's `NetherrackBlock`: with an open block above and
 * nylium among the 26 around it, it becomes that nylium (crimson or warped; both near, either at random).
 */
@Mixin(BlockNetherrack.class)
public abstract class MixinBlockNetherrack extends Block implements IGrowable {

	protected MixinBlockNetherrack(Material material) {
		super(material);
	}

	private static int mcfly$nylia(World world, int x, int y, int z) {
		int found = 0;
		Block nylium = McFlyBlocks.NYLIUM.get();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (world.getBlock(x + dx, y + dy, z + dz) == nylium) {
						found |= 1 << (world.getBlockMetadata(x + dx, y + dy, z + dz) & 1);
					}
				}
			}
		}
		return found;
	}

	@Override
	public boolean func_149851_a(World world, int x, int y, int z, boolean client) {
		return McFlyBlocks.NYLIUM.isEnabled() && !world.getBlock(x, y + 1, z).isOpaqueCube() && mcfly$nylia(world, x, y, z) != 0;
	}

	@Override
	public boolean func_149852_a(World world, Random random, int x, int y, int z) {
		return true;
	}

	@Override
	public void func_149853_b(World world, Random random, int x, int y, int z) {
		int found = mcfly$nylia(world, x, y, z);
		int kind = found == 3 ? (random.nextBoolean() ? 1 : 0) : found == 2 ? 1 : 0;
		world.setBlock(x, y, z, McFlyBlocks.NYLIUM.get(), kind, 3);
	}
}
