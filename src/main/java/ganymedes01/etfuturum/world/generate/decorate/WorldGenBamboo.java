package ganymedes01.etfuturum.world.generate.decorate;

import ganymedes01.etfuturum.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.Random;

public class WorldGenBamboo extends WorldGenerator {

	private final Block bamboo;

	public WorldGenBamboo(Block bamboo) {
		this.bamboo = bamboo;
	}

	@Override
	public boolean generate(World world, Random rand, int x, int y, int z) {
		int height = MathHelper.getRandomIntegerInRange(rand, 6, 16);
		if (!bamboo.canBlockStay(world, x, y + 1, z)) {
			return false;
		}

		for (int i = 1; i <= height; i++) {
			if (!canReplace(world, x, y + i, z)) {
				return false;
			}
		}

		for (int i = 1; i <= height; i++) {
			// thick, the top two with large leaves, small below them, done growing at the top (spec 91.11: McFly's metas)
			int leaves = i >= height - 1 ? io.github.asgerjon.mcfly.wood.Bamboo.LARGE : i >= height - 2 ? io.github.asgerjon.mcfly.wood.Bamboo.SMALL
				: io.github.asgerjon.mcfly.wood.Bamboo.NONE;
			int meta = io.github.asgerjon.mcfly.wood.Bamboo.meta(1, leaves, i == height ? 1 : 0);
			world.setBlock(x, y + i, z, bamboo, meta, 2);
		}
		return true;
	}

	private boolean canReplace(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		return block.getMaterial() == Material.air || (!block.getMaterial().isLiquid() && block.isReplaceable(world, x, y, z));
	}
}
