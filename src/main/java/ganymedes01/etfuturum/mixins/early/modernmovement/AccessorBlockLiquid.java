package ganymedes01.etfuturum.mixins.early.modernmovement;

import net.minecraft.block.BlockLiquid;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * The raw (normalised) flow direction of a vanilla liquid block. velocityToAddToEntity also goes through EFR's
 * liquidphysics scaling, which modern currents don't have.
 */
@Mixin(BlockLiquid.class)
public interface AccessorBlockLiquid {

	@Invoker("getFlowVector")
	Vec3 etfu$getFlowVector(IBlockAccess world, int x, int y, int z);
}
