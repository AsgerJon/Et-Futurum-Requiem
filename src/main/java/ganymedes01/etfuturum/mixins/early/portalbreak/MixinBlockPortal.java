package ganymedes01.etfuturum.mixins.early.portalbreak;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPortal;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A nether portal whose frame breaks shatters: each portal block is destroyed with the block-break effect, the glass
 * sound and particles, as modern's shape update does (Block.updateOrDestroy calls destroyBlock). 1.7.10 replaced
 * it with air silently. Server only: the effect reaches the clients as a world event, so they would play it twice.
 */
@Mixin(BlockPortal.class)
public abstract class MixinBlockPortal {

	@WrapOperation(method = "onNeighborBlockChange", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(IIILnet/minecraft/block/Block;)Z"))
	private boolean etfu$shatter(World world, int x, int y, int z, Block block, Operation<Boolean> original) {
		if (block == Blocks.air && !world.isRemote) {
			return world.func_147480_a(x, y, z, false);
		}
		return original.call(world, x, y, z, block);
	}
}
