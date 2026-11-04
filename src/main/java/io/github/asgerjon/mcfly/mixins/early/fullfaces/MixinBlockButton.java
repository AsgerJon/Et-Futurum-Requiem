package io.github.asgerjon.mcfly.mixins.early.fullfaces;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.core.utils.BlockSupport;
import net.minecraft.block.BlockButton;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A button holds on any full face, glass included (SPEC-modern-movement.md §53.4): every check of the block it hangs on,
 * when it is placed and when a neighbour changes. The floor and ceiling buttons (floorceilbutton) ask BlockSupport too.
 */
@Mixin(BlockButton.class)
public class MixinBlockButton {

	@WrapOperation(method = {"canPlaceBlockOnSide", "canPlaceBlockAt", "onBlockPlaced", "func_150045_e", "onNeighborBlockChange"},
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isSideSolid(IIILnet/minecraftforge/common/util/ForgeDirection;)Z", remap = false))
	private boolean etfu$fullFacesHold(World world, int x, int y, int z, ForgeDirection face, Operation<Boolean> original) {
		return original.call(world, x, y, z, face) || BlockSupport.isFaceFull(world, x, y, z, face);
	}
}
