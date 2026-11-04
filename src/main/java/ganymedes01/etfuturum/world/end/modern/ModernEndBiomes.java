package ganymedes01.etfuturum.world.end.modern;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import org.apache.logging.log4j.LogManager;

import java.util.Random;

/**
 * The End's biomes as 26.3's (SPEC-modern-movement.md §76.5): the End's own (1.7.10's `sky`) within 64 chunks of the
 * middle, and beyond by the islands' value at the chunk: highlands, midlands, small islands, barrens. Their features
 * come with stage 6; endermen spawn in all of them as in the End.
 */
public final class ModernEndBiomes {

	public static BiomeGenBase highlands = BiomeGenBase.sky;
	public static BiomeGenBase midlands = BiomeGenBase.sky;
	public static BiomeGenBase smallIslands = BiomeGenBase.sky;
	public static BiomeGenBase barrens = BiomeGenBase.sky;

	private ModernEndBiomes() {
	}

	/** Made once mods are known (preInit), with their ids from `ConfigWorld`; a taken id leaves the End's own there. */
	public static void init() {
		highlands = make(McFlyConfig.endHighlandsID, "End Highlands", 0xB5B3A1);
		midlands = make(McFlyConfig.endMidlandsID, "End Midlands", 0xCDCBB4);
		smallIslands = make(McFlyConfig.smallEndIslandsID, "Small End Islands", 0x86846F);
		barrens = make(McFlyConfig.endBarrensID, "End Barrens", 0x9A9883);
	}

	private static BiomeGenBase make(int id, String name, int colour) {
		if (id < 0 || id >= BiomeGenBase.getBiomeGenArray().length) {
			return BiomeGenBase.sky;
		}
		BiomeGenBase taken = BiomeGenBase.getBiomeGenArray()[id];
		if (taken != null) {
			LogManager.getLogger("etfuturum").error("The biome id {} for {} is taken by {}: the End's own biome goes there instead (set another in world.cfg)",
				id, name, taken.biomeName);
			return BiomeGenBase.sky;
		}
		BiomeGenBase biome = new Biome(id).setColor(colour).setBiomeName(name).setDisableRain().setTemperatureRainfall(0.5F, 0.5F);
		BiomeDictionary.registerBiomeType(biome, BiomeDictionary.Type.END);
		return biome;
	}

	/** The biome at a block, as 26.3 picks it for its chunk. */
	public static BiomeGenBase at(ModernEndTerrain terrain, int x, int z) {
		long chunkX = x >> 4;
		long chunkZ = z >> 4;
		if (chunkX * chunkX + chunkZ * chunkZ <= 4096L) {
			return BiomeGenBase.sky;
		}
		double value = terrain.islands((int) (chunkX * 2 + 1) * 8, (int) (chunkZ * 2 + 1) * 8);
		if (value > 0.25) {
			return highlands;
		}
		if (value >= -0.0625) {
			return midlands;
		}
		return value < -0.21875 ? smallIslands : barrens;
	}

	/** 26.3's id for one of them. */
	public static String modernName(BiomeGenBase biome) {
		if (biome == highlands && biome != BiomeGenBase.sky) {
			return "minecraft:end_highlands";
		}
		if (biome == midlands && biome != BiomeGenBase.sky) {
			return "minecraft:end_midlands";
		}
		if (biome == smallIslands && biome != BiomeGenBase.sky) {
			return "minecraft:small_end_islands";
		}
		if (biome == barrens && biome != BiomeGenBase.sky) {
			return "minecraft:end_barrens";
		}
		return biome == BiomeGenBase.sky ? "minecraft:the_end" : biome.biomeName;
	}

	/** An outer End biome: end stone, the End's sky, endermen; decorated by the stages that add its features. */
	private static final class Biome extends BiomeGenBase {

		Biome(int id) {
			super(id);
			spawnableMonsterList.clear();
			spawnableCreatureList.clear();
			spawnableWaterCreatureList.clear();
			spawnableCaveCreatureList.clear();
			spawnableMonsterList.add(new SpawnListEntry(EntityEnderman.class, 10, 4, 4));
			topBlock = Blocks.end_stone;
			fillerBlock = Blocks.end_stone;
			theBiomeDecorator = new BiomeDecorator() {
				@Override
				public void decorateChunk(World world, Random random, BiomeGenBase biome, int x, int z) {
					// nothing yet (spec §76.3 stage 6: chorus, small islands, return gateways)
				}
			};
		}

		@Override
		public int getSkyColorByTemp(float temperature) {
			return 0;
		}
	}
}
