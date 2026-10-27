package ganymedes01.etfuturum.mixins.early.placementshape.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.placement.PlacedShape;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The client's own "can it go there" (func_150936_a) asks the same question as the common placement without the
 * click position; it is taken from what the player is looking at, which is what the click will carry.
 */
@Mixin(ItemBlock.class)
public class MixinItemBlock {

	@WrapOperation(method = "func_150936_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;canPlaceEntityOnSide(Lnet/minecraft/block/Block;IIIZILnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Z"))
	private boolean etfu$testThePlacedShape(World world, Block block, int x, int y, int z, boolean skipCollision, int side, Entity placer, ItemStack stack, Operation<Boolean> original) {
		float hitX = 0.5F, hitY = 0.5F, hitZ = 0.5F;
		MovingObjectPosition hit = Minecraft.getMinecraft().objectMouseOver;
		if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && hit.hitVec != null) {
			hitX = (float) (hit.hitVec.xCoord - hit.blockX);
			hitY = (float) (hit.hitVec.yCoord - hit.blockY);
			hitZ = (float) (hit.hitVec.zCoord - hit.blockZ);
		}
		AxisAlignedBB box = skipCollision ? null : PlacedShape.boxAsPlaced(block, world, x, y, z, side, hitX, hitY, hitZ, ((ItemBlock) (Object) this).getMetadata(stack.getItemDamage()));
		if (box == null) {
			return original.call(world, block, x, y, z, skipCollision, side, placer, stack);
		}
		return world.checkNoEntityCollision(box, placer) && original.call(world, block, x, y, z, true, side, placer, stack);
	}
}
