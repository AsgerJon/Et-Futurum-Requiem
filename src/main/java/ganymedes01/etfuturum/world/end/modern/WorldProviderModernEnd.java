package ganymedes01.etfuturum.world.end.modern;

import net.minecraft.world.WorldProviderEnd;
import net.minecraft.world.chunk.IChunkProvider;

/**
 * The End as 26.3's (SPEC-modern-movement.md §76.5): its terrain and biomes. Still 1.7.10's `WorldProviderEnd` to
 * anything asking (the sky, mods' checks for the End).
 */
public class WorldProviderModernEnd extends WorldProviderEnd {

	@Override
	public void registerWorldChunkManager() {
		worldChunkMgr = new WorldChunkManagerModernEnd(ModernEndTerrain.forSeed(worldObj.getSeed()));
		dimensionId = 1;
		hasNoSky = true;
	}

	@Override
	public IChunkProvider createChunkGenerator() {
		return new ChunkProviderModernEnd(worldObj, worldObj.getSeed());
	}
}
