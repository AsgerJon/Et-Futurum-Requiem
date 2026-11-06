package io.github.asgerjon.mcfly.mixins.early.base;

import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The world a chunk cache is of (render caches and path finding hand blocks a cache, not the world). */
@Mixin(ChunkCache.class)
public interface AccessorChunkCache {

	@Accessor("worldObj")
	World mcfly$world();
}
