package ganymedes01.etfuturum.world.end.modern;

import net.minecraft.world.ChunkPosition;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** The End's biomes by position (SPEC-modern-movement.md §76.5): 26.3's, one per chunk. */
public class WorldChunkManagerModernEnd extends WorldChunkManager {

	private final ModernEndTerrain terrain;

	public WorldChunkManagerModernEnd(ModernEndTerrain terrain) {
		this.terrain = terrain;
	}

	@Override
	public BiomeGenBase getBiomeGenAt(int x, int z) {
		return ModernEndBiomes.at(terrain, x, z);
	}

	@Override
	public float[] getRainfall(float[] rainfall, int x, int z, int width, int depth) {
		if (rainfall == null || rainfall.length < width * depth) {
			rainfall = new float[width * depth];
		}
		Arrays.fill(rainfall, 0, width * depth, 0.0F);
		return rainfall;
	}

	@Override
	public float getTemperatureAtHeight(float temperature, int height) {
		return temperature;
	}

	/** In 4-block steps (the generation's scale). */
	@Override
	public BiomeGenBase[] getBiomesForGeneration(BiomeGenBase[] biomes, int x, int z, int width, int depth) {
		if (biomes == null || biomes.length < width * depth) {
			biomes = new BiomeGenBase[width * depth];
		}
		for (int j = 0; j < depth; j++) {
			for (int i = 0; i < width; i++) {
				biomes[j * width + i] = getBiomeGenAt((x + i) * 4, (z + j) * 4);
			}
		}
		return biomes;
	}

	@Override
	public BiomeGenBase[] loadBlockGeneratorData(BiomeGenBase[] biomes, int x, int z, int width, int depth) {
		return getBiomeGenAt(biomes, x, z, width, depth, true);
	}

	@Override
	public BiomeGenBase[] getBiomeGenAt(BiomeGenBase[] biomes, int x, int z, int width, int depth, boolean cache) {
		if (biomes == null || biomes.length < width * depth) {
			biomes = new BiomeGenBase[width * depth];
		}
		for (int j = 0; j < depth; j++) {
			for (int i = 0; i < width; i++) {
				biomes[j * width + i] = getBiomeGenAt(x + i, z + j);
			}
		}
		return biomes;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean areBiomesViable(int x, int z, int radius, List allowed) {
		for (int cx = (x - radius) >> 4; cx <= (x + radius) >> 4; cx++) {
			for (int cz = (z - radius) >> 4; cz <= (z + radius) >> 4; cz++) {
				if (!allowed.contains(getBiomeGenAt(cx << 4, cz << 4))) {
					return false;
				}
			}
		}
		return true;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public ChunkPosition findBiomePosition(int x, int z, int range, List biomes, Random random) {
		ChunkPosition found = null;
		int count = 0;
		for (int cx = (x - range) >> 4; cx <= (x + range) >> 4; cx++) {
			for (int cz = (z - range) >> 4; cz <= (z + range) >> 4; cz++) {
				if (biomes.contains(getBiomeGenAt(cx << 4, cz << 4)) && (found == null || random.nextInt(++count) == 0)) {
					found = new ChunkPosition((cx << 4) + 8, 0, (cz << 4) + 8);
				}
			}
		}
		return found;
	}

	@Override
	public void cleanupCache() {
	}
}
