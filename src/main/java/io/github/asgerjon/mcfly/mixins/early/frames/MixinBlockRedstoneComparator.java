package io.github.asgerjon.mcfly.mixins.early.frames;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * A comparator reads an item frame through a block (SPEC-modern-movement.md §67.3; 26.3's
 * `ComparatorBlock.getInputSignal`): its input block is a solid conductor, and in the block beyond it there is exactly
 * one frame facing the comparator's way: the frame's signal (or the block's there, the higher). 1.7.10 reads only
 * blocks; this is its rule with the frame added, in full.
 */
@Mixin(BlockRedstoneComparator.class)
public abstract class MixinBlockRedstoneComparator extends BlockRedstoneDiode {

	protected MixinBlockRedstoneComparator(boolean powered) {
		super(powered);
	}

	@Inject(method = "getInputStrength", at = @At("HEAD"), cancellable = true)
	private void etfu$readFrames(World world, int x, int y, int z, int meta, CallbackInfoReturnable<Integer> cir) {
		int signal = super.getInputStrength(world, x, y, z, meta);
		int direction = getDirection(meta);
		int bx = x + Direction.offsetX[direction], bz = z + Direction.offsetZ[direction];
		Block block = world.getBlock(bx, y, bz);
		if (block.hasComparatorInputOverride()) {
			signal = block.getComparatorInputOverride(world, bx, y, bz, Direction.rotateOpposite[direction]);
		} else if (signal < 15 && block.isNormalCube()) {
			bx += Direction.offsetX[direction];
			bz += Direction.offsetZ[direction];
			block = world.getBlock(bx, y, bz);
			int frame = Integer.MIN_VALUE;
			List<?> frames = world.getEntitiesWithinAABB(EntityItemFrame.class, AxisAlignedBB.getBoundingBox(bx, y, bz, bx + 1, y + 1, bz + 1));
			EntityItemFrame only = null;
			int facing = 0;
			for (Object found : frames) {
				if (((EntityItemFrame) found).hangingDirection == direction) {
					only = (EntityItemFrame) found;
					facing++;
				}
			}
			if (facing == 1) {
				frame = only.getDisplayedItem() == null ? 0 : only.getRotation() % 8 + 1;
			}
			int value = Math.max(frame, block.hasComparatorInputOverride()
				? block.getComparatorInputOverride(world, bx, y, bz, Direction.rotateOpposite[direction]) : Integer.MIN_VALUE);
			if (value != Integer.MIN_VALUE) {
				signal = value;
			}
		}
		cir.setReturnValue(signal);
	}
}
