package ganymedes01.etfuturum.mixins.early.fullfaces;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.core.utils.BlockSupport;
import net.minecraft.block.BlockLever;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A lever holds on any full face, glass included (SPEC-modern-movement.md §53.4), on walls, floors and ceilings. */
@Mixin(BlockLever.class)
public class MixinBlockLever {

	@WrapOperation(method = {"canPlaceBlockOnSide", "canPlaceBlockAt", "onBlockPlaced", "onNeighborBlockChange"},
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isSideSolid(IIILnet/minecraftforge/common/util/ForgeDirection;)Z", remap = false))
	private boolean etfu$fullFacesHold(World world, int x, int y, int z, ForgeDirection face, Operation<Boolean> original) {
		return original.call(world, x, y, z, face) || BlockSupport.isFaceFull(world, x, y, z, face);
	}
}
