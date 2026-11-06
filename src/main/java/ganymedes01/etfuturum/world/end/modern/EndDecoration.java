package ganymedes01.etfuturum.world.end.modern;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.world.generate.ModernWorldgenRandom;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * The outer End's features as 26.3 places them (`ChunkGenerator.applyBiomeDecoration` with the End's placed features;
 * SPEC-modern-movement.md §76.11), for one chunk at 26.3's own positions: each feature's random seeded from the
 * chunk's and the feature's index in its step (26.3's sorted order for the End's biomes), its placement drawn as 26.3's
 * modifiers draw it. Small islands get `end_island_decorated` (step 0), highlands `end_gateway_return` (step 4) and
 * `chorus_plant` (step 9); midlands and barrens nothing; every chunk the end cities reaching into it (step 4's
 * structures, before its features: §76.12). Every position stays in the chunk, so a feature runs for a
 * chunk exactly when the chunk's own biome has it (26.3 tries those of the 3 by 3's biomes, but its biome filter then
 * wants the chunk's).
 */
public final class EndDecoration {

	/** 26.3's step indices: RAW_GENERATION, SURFACE_STRUCTURES, VEGETAL_DECORATION. */
	public static final int ISLAND_STEP = 0;
	public static final int GATEWAY_STEP = 4;
	public static final int CHORUS_STEP = 9;
	/** Each feature's index in its step's sorted list (26.3's FeatureSorter over the End's five biomes). */
	public static final int ISLAND_INDEX = 0;
	public static final int GATEWAY_INDEX = 0;
	public static final int CHORUS_INDEX = 0;

	/** Where every outer return gateway leads: above the arrival platform, exactly. */
	private static final int[] RETURN_EXIT = {100, 50, 0};

	private EndDecoration() {
	}

	/** 26.3's decoration of chunk (cx, cz)'s outer features. */
	public static void decorate(World world, ModernEndTerrain terrain, int cx, int cz) {
		BiomeGenBase biome = ModernEndBiomes.at(terrain, cx * 16, cz * 16);
		int x0 = cx * 16;
		int z0 = cz * 16;
		ModernWorldgenRandom random = new ModernWorldgenRandom();
		long decorationSeed = random.setDecorationSeed(world.getSeed(), x0, z0);
		if (biome == ModernEndBiomes.smallIslands) {
			// end_island_decorated: 1 in 14; 1 or 2 (3 to 1); anywhere in the chunk; y 55 to 70
			random.setFeatureSeed(decorationSeed, ISLAND_INDEX, ISLAND_STEP);
			if (random.nextFloat() < 1.0F / 14) {
				int count = random.nextInt(4) < 3 ? 1 : 2;
				for (int i = 0; i < count; i++) {
					int x = random.nextInt(16) + x0;
					int z = random.nextInt(16) + z0;
					int y = random.nextInt(70 - 55 + 1) + 55;
					EndGateways.smallIsland(world, random, x, y, z);
				}
			}
		}
		// step 4's structures before its features: the cities reaching into this chunk (any biome: their pieces cross)
		random.setFeatureSeed(decorationSeed, EndCities.STEP_INDEX, GATEWAY_STEP);
		EndCities.placeInChunk(world, terrain, cx, cz, random);
		if (biome != ModernEndBiomes.highlands) {
			return;
		}
		// end_gateway_return: 1 in 700; on the surface, 3 to 9 above; leading exactly to (100, 50, 0)
		random.setFeatureSeed(decorationSeed, GATEWAY_INDEX, GATEWAY_STEP);
		if (random.nextFloat() < 1.0F / 700) {
			int x = random.nextInt(16) + x0;
			int z = random.nextInt(16) + z0;
			int height = motionBlockingHeight(world, x, z);
			if (height > 0) {
				int y = height + random.nextInt(9 - 3 + 1) + 3;
				EndGateways.placeShell(world, x, y, z, RETURN_EXIT, true);
			}
		}
		// chorus_plant: 0 to 4; on the surface; on end stone in the air
		if (io.github.asgerjon.mcfly.registry.McFlyBlocks.CHORUS_PLANT.isEnabled() && io.github.asgerjon.mcfly.registry.McFlyBlocks.CHORUS_FLOWER.isEnabled()) {
			random.setFeatureSeed(decorationSeed, CHORUS_INDEX, CHORUS_STEP);
			int count = random.nextInt(4 - 0 + 1);
			for (int i = 0; i < count; i++) {
				int x = random.nextInt(16) + x0;
				int z = random.nextInt(16) + z0;
				int height = motionBlockingHeight(world, x, z);
				if (height > 0 && world.isAirBlock(x, height, z) && world.getBlock(x, height - 1, z) == Blocks.end_stone) {
					generateChorus(world, x, height, z, random, 8);
				}
			}
		}
	}

	/**
	 * 26.3's MOTION_BLOCKING heightmap: one above the highest block that blocks motion or holds a fluid (chorus and end
	 * rods don't: they're forced not solid), or 0.
	 */
	static int motionBlockingHeight(World world, int x, int z) {
		for (int y = world.getHeight() - 1; y >= 0; y--) {
			Block block = world.getBlock(x, y, z);
			Material material = block.getMaterial();
			if ((material.blocksMovement() || material.isLiquid()) && !forcedNotSolid(block)) {
				return y + 1;
			}
		}
		return 0;
	}

	private static boolean forcedNotSolid(Block block) {
		return block == io.github.asgerjon.mcfly.registry.McFlyBlocks.CHORUS_PLANT.get() || block == io.github.asgerjon.mcfly.registry.McFlyBlocks.CHORUS_FLOWER.get() || block == io.github.asgerjon.mcfly.registry.McFlyBlocks.END_ROD.get();
	}

	/** 26.3's ChorusFlowerBlock.generatePlant: a plant at the place, grown up to 4 branchings, a dead flower on each tip. */
	public static void generateChorus(World world, int x, int y, int z, java.util.Random random, int maxHorizontalSpread) {
		world.setBlock(x, y, z, io.github.asgerjon.mcfly.registry.McFlyBlocks.CHORUS_PLANT.get(), 0, 2);
		growChorus(world, x, y, z, random, x, z, maxHorizontalSpread, 0);
	}

	/** 26.3's Plane.HORIZONTAL order: north, east, south, west. */
	private static final ForgeDirection[] HORIZONTAL = {ForgeDirection.NORTH, ForgeDirection.EAST, ForgeDirection.SOUTH, ForgeDirection.WEST};

	private static void growChorus(World world, int x, int y, int z, java.util.Random random, int startX, int startZ, int maxHorizontalSpread, int depth) {
		Block plant = io.github.asgerjon.mcfly.registry.McFlyBlocks.CHORUS_PLANT.get();
		int height = random.nextInt(4) + 1;
		if (depth == 0) {
			height++;
		}
		for (int i = 0; i < height; i++) {
			if (!allNeighborsEmpty(world, x, y + i + 1, z, null)) {
				return;
			}
			world.setBlock(x, y + i + 1, z, plant, 0, 2);
			world.setBlock(x, y + i, z, plant, 0, 2);
		}
		boolean placedStem = false;
		if (depth < 4) {
			int stems = random.nextInt(4);
			if (depth == 0) {
				stems++;
			}
			for (int i = 0; i < stems; i++) {
				ForgeDirection direction = HORIZONTAL[random.nextInt(HORIZONTAL.length)];
				int tx = x + direction.offsetX;
				int ty = y + height;
				int tz = z + direction.offsetZ;
				if (Math.abs(tx - startX) < maxHorizontalSpread && Math.abs(tz - startZ) < maxHorizontalSpread && world.isAirBlock(tx, ty, tz)
					&& world.isAirBlock(tx, ty - 1, tz) && allNeighborsEmpty(world, tx, ty, tz, direction.getOpposite())) {
					placedStem = true;
					world.setBlock(tx, ty, tz, plant, 0, 2);
					world.setBlock(tx - direction.offsetX, ty, tz - direction.offsetZ, plant, 0, 2);
					growChorus(world, tx, ty, tz, random, startX, startZ, maxHorizontalSpread, depth + 1);
				}
			}
		}
		if (!placedStem) {
			world.setBlock(x, y + height, z, io.github.asgerjon.mcfly.registry.McFlyBlocks.CHORUS_FLOWER.get(), 5, 2);
		}
	}

	/** Air on every side but the one it came from. */
	private static boolean allNeighborsEmpty(World world, int x, int y, int z, ForgeDirection ignore) {
		for (ForgeDirection direction : HORIZONTAL) {
			if (direction != ignore && !world.isAirBlock(x + direction.offsetX, y, z + direction.offsetZ)) {
				return false;
			}
		}
		return true;
	}
}
