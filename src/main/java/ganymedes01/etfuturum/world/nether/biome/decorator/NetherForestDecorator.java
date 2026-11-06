package ganymedes01.etfuturum.world.nether.biome.decorator;

import ganymedes01.etfuturum.ModBlocks;

import ganymedes01.etfuturum.world.generate.decorate.WorldGenBlockSplatter;
import ganymedes01.etfuturum.world.generate.decorate.WorldGenTwistingVines;
import ganymedes01.etfuturum.world.generate.decorate.WorldGenWeepingVines;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.Random;

public class NetherForestDecorator extends NetherBiomeDecorator {

	// McFly's 26.3 huge fungus (spec §88.7), unplanted; the vegetation is the nylium's bonemeal's
	private final io.github.asgerjon.mcfly.nether.HugeFungus hugeFungusGenerator;
	private final int kind;
	private final WorldGenerator vineGenerator;
	private final WorldGenerator wartSplatterGenerator;
	private final WorldGenerator netherrackSplatterGenerator;

	private NetherForestDecorator(boolean crimson) {
		if (crimson) {
			kind = 0;
			hugeFungusGenerator = new io.github.asgerjon.mcfly.nether.HugeFungus(io.github.asgerjon.mcfly.registry.McFlyBlocks.NYLIUM.get(), 0, io.github.asgerjon.mcfly.registry.McFlyBlocks.CRIMSON_STEM.get(), 0, io.github.asgerjon.mcfly.registry.McFlyBlocks.NETHER_WART.get(), 0, io.github.asgerjon.mcfly.registry.McFlyBlocks.SHROOMLIGHT.get(), true, false);
			vineGenerator = new WorldGenWeepingVines();
			wartSplatterGenerator = new WorldGenBlockSplatter(io.github.asgerjon.mcfly.registry.McFlyBlocks.NETHER_WART.get(), 0, io.github.asgerjon.mcfly.registry.McFlyBlocks.NYLIUM.get(), 0, 4);
			netherrackSplatterGenerator = new WorldGenBlockSplatter(Blocks.netherrack, 0, io.github.asgerjon.mcfly.registry.McFlyBlocks.NYLIUM.get(), 0, 4);
		} else {
			kind = 1;
			hugeFungusGenerator = new io.github.asgerjon.mcfly.nether.HugeFungus(io.github.asgerjon.mcfly.registry.McFlyBlocks.NYLIUM.get(), 1, io.github.asgerjon.mcfly.registry.McFlyBlocks.WARPED_STEM.get(), 0, io.github.asgerjon.mcfly.registry.McFlyBlocks.NETHER_WART.get(), 1, io.github.asgerjon.mcfly.registry.McFlyBlocks.SHROOMLIGHT.get(), false, false);
			vineGenerator = new WorldGenTwistingVines();
			wartSplatterGenerator = new WorldGenBlockSplatter(io.github.asgerjon.mcfly.registry.McFlyBlocks.NETHER_WART.get(), 1, io.github.asgerjon.mcfly.registry.McFlyBlocks.NYLIUM.get(), 1, 6);
			netherrackSplatterGenerator = new WorldGenBlockSplatter(Blocks.netherrack, 0, io.github.asgerjon.mcfly.registry.McFlyBlocks.NYLIUM.get(), 1, 6);
		}
	}

	public static NetherForestDecorator newWarpedForestDecorator() {
		return new NetherForestDecorator(false);
	}

	public static NetherForestDecorator newCrimsonForestDecorator() {
		return new NetherForestDecorator(true);
	}

	@Override
	public void populate(World world, Random rand, int chunkX, int chunkZ) {
	}

	@Override
	public void decorate(World world, Random rand, int chunkX, int chunkZ) {
		{
			int x = chunkX + rand.nextInt(16) + 8;
			int y = MathHelper.getRandomIntegerInRange(rand, 32, world.provider.getActualHeight() - 8);
			int z = chunkZ + rand.nextInt(16) + 8;

			if (wartSplatterGenerator != null && rand.nextBoolean()) {
				wartSplatterGenerator.generate(world, rand, x, y, z);
			} else {
				netherrackSplatterGenerator.generate(world, rand, x, y, z);
			}
		}

		for (int attempt = 0; attempt < 12; attempt++) {
			int x = chunkX + rand.nextInt(16) + 8;
			int y = MathHelper.getRandomIntegerInRange(rand, 32, world.provider.getActualHeight() - 24);
			int z = chunkZ + rand.nextInt(16) + 8;

			while (y-- > 32) {
				if (!world.isAirBlock(x, y, z)) {
					break;
				}
			}

			if (world.getBlock(x, y, z) == io.github.asgerjon.mcfly.registry.McFlyBlocks.NYLIUM.get() && world.getBlockMetadata(x, y, z) == kind && world.isAirBlock(x, y + 1, z)) {
				hugeFungusGenerator.place(world, rand, x, y + 1, z);
			}
		}

		for (int attempt = 0; attempt < 48; attempt++) {
			int x = chunkX + rand.nextInt(16) + 8;
			int y = MathHelper.getRandomIntegerInRange(rand, 32, world.provider.getActualHeight() - 8);
			int z = chunkZ + rand.nextInt(16) + 8;

			vineGenerator.generate(world, rand, x, y, z);
		}

		for (int attempt = 0; attempt < 12; attempt++) {
			int x = chunkX + rand.nextInt(16) + 8;
			int y = MathHelper.getRandomIntegerInRange(rand, 32, world.provider.getActualHeight() - 8);
			int z = chunkZ + rand.nextInt(16) + 8;

			if (world.getBlock(x, y - 1, z) == io.github.asgerjon.mcfly.registry.McFlyBlocks.NYLIUM.get()) {
				((net.minecraft.block.IGrowable) io.github.asgerjon.mcfly.registry.McFlyBlocks.NYLIUM.get()).func_149853_b(world, rand, x, y - 1, z);
			}
		}
	}
}