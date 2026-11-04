package ganymedes01.etfuturum.world.end.modern;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.ChunkProviderEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;

import java.util.List;

/**
 * The End's chunks as 26.3 makes them (SPEC-modern-movement.md §76.5, §76.6): its terrain and biomes, then for the
 * End's own biome 26.3's features (the pillars and the spawn platform), for the outer ones their decorators.
 */
public class ChunkProviderModernEnd implements IChunkProvider {

	private final World world;
	private final ModernEndTerrain terrain;

	public ChunkProviderModernEnd(World world, long seed) {
		this.world = world;
		this.terrain = ModernEndTerrain.forSeed(seed);
	}

	public ModernEndTerrain terrain() {
		return terrain;
	}

	@Override
	public boolean chunkExists(int x, int z) {
		return true;
	}

	@Override
	public Chunk provideChunk(int cx, int cz) {
		boolean[] solid = terrain.fillChunk(cx, cz);
		Block[] blocks = new Block[solid.length];
		byte[] metas = new byte[solid.length];
		for (int i = 0; i < solid.length; i++) {
			blocks[i] = solid[i] ? Blocks.end_stone : null;
		}
		BiomeGenBase biome = ModernEndBiomes.at(terrain, cx << 4, cz << 4);
		BiomeGenBase[] biomes = new BiomeGenBase[256];
		java.util.Arrays.fill(biomes, biome);
		// mods hook the End's block pass (1.7.10's End fires it as it puts its surface)
		ChunkProviderEvent.ReplaceBiomeBlocks event = new ChunkProviderEvent.ReplaceBiomeBlocks(this, cx, cz, blocks, metas, biomes, world);
		MinecraftForge.EVENT_BUS.post(event);
		Chunk chunk = new Chunk(world, blocks, metas, cx, cz);
		byte[] chunkBiomes = chunk.getBiomeArray();
		java.util.Arrays.fill(chunkBiomes, (byte) biome.biomeID);
		chunk.generateSkylightMap();
		return chunk;
	}

	@Override
	public Chunk loadChunk(int x, int z) {
		return provideChunk(x, z);
	}

	@Override
	public void populate(IChunkProvider provider, int cx, int cz) {
		BlockFalling.fallInstantly = true;
		MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Pre(provider, world, world.rand, cx, cz, false));
		int x = cx * 16;
		int z = cz * 16;
		BiomeGenBase biome = world.getBiomeGenForCoords(x + 16, z + 16);
		if (biome == BiomeGenBase.sky) {
			// the End's own biome: 26.3's features (spec §76.6), not 1.7.10's decorator (its random pillars and the
			// dragon it spawned at chunk (0, 0): the fight makes the dragon now)
			EndSpikes.decorate(world, cx, cz, world.rand);
			// 26.3's end_platform feature: placed with the terrain, from the chunk whose populated area holds it
			if (cx == 5 && cz == -1) {
				ModernEndArrival.makePlatform(world, false);
			}
		}
		// the outer biomes' features at 26.3's own places in this chunk (spec §76.11); what reaches into a neighbour not
		// made yet makes it, as 1.7.10's own features do
		EndDecoration.decorate(world, terrain, cx, cz);
		MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Post(provider, world, world.rand, cx, cz, false));
		BlockFalling.fallInstantly = false;
	}

	@Override
	public boolean saveChunks(boolean all, IProgressUpdate progress) {
		return true;
	}

	@Override
	public boolean unloadQueuedChunks() {
		return false;
	}

	@Override
	public boolean canSave() {
		return true;
	}

	@Override
	public String makeString() {
		return "ModernEndLevelSource";
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List getPossibleCreatures(EnumCreatureType type, int x, int y, int z) {
		return world.getBiomeGenForCoords(x, z).getSpawnableList(type);
	}

	@Override
	public ChunkPosition func_147416_a(World world, String structure, int x, int y, int z) {
		return null;
	}

	@Override
	public int getLoadedChunkCount() {
		return 0;
	}

	@Override
	public void recreateStructures(int x, int z) {
	}

	@Override
	public void saveExtraData() {
	}
}
