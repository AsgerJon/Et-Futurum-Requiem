package io.github.asgerjon.mcfly.mixins.early.redstone;

import io.github.asgerjon.mcfly.redstone.ObserverBlock;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.3's shape updates as observers see them (SPEC-modern-movement.md §86): any change of a block, its id through
 * Forge's `markAndNotifyBlock` or its meta through `setBlockMetadataWithNotify`, with any of 1.7.10's flags (26.3's
 * flag 2 still updates shapes; only 26.3's 16 doesn't), tells the observers around it. Not while a chunk populates (no
 * shape updates in world generation: `MixinChunkPopulating`), not on the client.
 */
@Mixin(World.class)
public abstract class MixinWorldObserverUpdates {

	@Shadow
	public boolean isRemote;

	private static boolean mcfly$tellsObservers(int flags) {
		return (flags & 1) != 0 || (flags & 16) == 0;
	}

	@Inject(method = "markAndNotifyBlock", at = @At("TAIL"), remap = false)
	private void mcfly$blockSet(int x, int y, int z, Chunk chunk, Block oldBlock, Block newBlock, int flags, CallbackInfo ci) {
		if (!this.isRemote && mcfly$tellsObservers(flags) && !ObserverBlock.populating()) {
			ObserverBlock.blockChanged((World) (Object) this, x, y, z);
		}
	}

	@Inject(method = "setBlockMetadataWithNotify", at = @At("RETURN"))
	private void mcfly$metaSet(int x, int y, int z, int meta, int flags, CallbackInfoReturnable<Boolean> cir) {
		if (!this.isRemote && cir.getReturnValue() && mcfly$tellsObservers(flags) && !ObserverBlock.populating()) {
			ObserverBlock.blockChanged((World) (Object) this, x, y, z);
		}
	}
}
