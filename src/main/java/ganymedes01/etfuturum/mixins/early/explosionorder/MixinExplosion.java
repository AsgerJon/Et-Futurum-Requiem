package ganymedes01.etfuturum.mixins.early.explosionorder;

import net.minecraft.world.ChunkPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

/**
 * An explosion destroys its blocks in random order (SPEC-modern-movement.md §35, modern since 1.9: the list is
 * shuffled with the world's random before the blocks go). 1.7.10 went through them in the order of a hash set of
 * their coordinates, which is fixed for a given layout. The order decides which neighbour updates see which blocks
 * still standing, e.g. whether a piston is still there to notice its lever going. Server only; clients get the list
 * in this order.
 */
@Mixin(Explosion.class)
public class MixinExplosion {

	@Shadow
	private World worldObj;

	@Shadow
	public List<ChunkPosition> affectedBlockPositions;

	@Inject(method = "doExplosionB", at = @At("HEAD"))
	private void etfu$shuffleTheBlocks(boolean spawnParticles, CallbackInfo ci) {
		if (!worldObj.isRemote) {
			Collections.shuffle(affectedBlockPositions, worldObj.rand);
		}
	}
}
