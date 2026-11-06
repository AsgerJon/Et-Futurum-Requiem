package io.github.asgerjon.mcfly.mixins.early.redstone;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counts the chunks populating on this thread (`ObserverBlock.POPULATING`: observers see nothing of world generation, as 26.3's; spec §86). */
@Mixin(Chunk.class)
public abstract class MixinChunkPopulating {

	@Inject(method = "populateChunk", at = @At("HEAD"))
	private void mcfly$populateStarts(IChunkProvider provider, IChunkProvider generator, int x, int z, CallbackInfo ci) {
		io.github.asgerjon.mcfly.redstone.ObserverBlock.POPULATING.get()[0]++;
	}

	@Inject(method = "populateChunk", at = @At("RETURN"))
	private void mcfly$populateEnds(IChunkProvider provider, IChunkProvider generator, int x, int z, CallbackInfo ci) {
		int[] depth = io.github.asgerjon.mcfly.redstone.ObserverBlock.POPULATING.get();
		depth[0] = Math.max(0, depth[0] - 1);
	}
}
