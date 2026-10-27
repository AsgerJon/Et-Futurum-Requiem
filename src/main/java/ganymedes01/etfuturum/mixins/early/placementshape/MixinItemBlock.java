package ganymedes01.etfuturum.mixins.early.placementshape;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.placement.PlacedShape;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Placing a block tests the player against the shape the block will have once placed (PlacedShape), as modern does
 * (1.13+). The world's check is then asked to skip its own, stale box and keep the rest of its answer.
 */
@Mixin(ItemBlock.class)
public class MixinItemBlock {

	@WrapOperation(method = "onItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;canPlaceEntityOnSide(Lnet/minecraft/block/Block;IIIZILnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Z"))
	private boolean etfu$testThePlacedShape(World world, Block block, int x, int y, int z, boolean skipCollision, int side, Entity placer, ItemStack stack, Operation<Boolean> original,
											@Local(argsOnly = true, ordinal = 0) float hitX, @Local(argsOnly = true, ordinal = 1) float hitY, @Local(argsOnly = true, ordinal = 2) float hitZ) {
		AxisAlignedBB box = skipCollision ? null : PlacedShape.boxAsPlaced(block, world, x, y, z, side, hitX, hitY, hitZ, ((ItemBlock) (Object) this).getMetadata(stack.getItemDamage()));
		if (box == null) {
			return original.call(world, block, x, y, z, skipCollision, side, placer, stack);
		}
		return world.checkNoEntityCollision(box, PlacedShape.excluded(placer)) && original.call(world, block, x, y, z, true, side, placer, stack);
	}
}
