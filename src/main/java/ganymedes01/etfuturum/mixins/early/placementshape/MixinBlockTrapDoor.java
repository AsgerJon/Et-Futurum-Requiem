package ganymedes01.etfuturum.mixins.early.placementshape;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A trapdoor placed on a powered spot is placed open, as modern's placement state has it. 1.7.10 placed it closed
 * and left it so until the next block update, which also made the placed shape differ from the one the placement
 * check could foresee (PlacedShape).
 */
@Mixin(BlockTrapDoor.class)
public class MixinBlockTrapDoor {

	@ModifyReturnValue(method = "onBlockPlaced", at = @At("RETURN"))
	private int etfu$openWhenPowered(int meta, @Local(argsOnly = true) World world, @Local(argsOnly = true, ordinal = 0) int x,
									 @Local(argsOnly = true, ordinal = 1) int y, @Local(argsOnly = true, ordinal = 2) int z) {
		return world.isBlockIndirectlyGettingPowered(x, y, z) ? meta | 4 : meta;
	}
}
