package io.github.asgerjon.mcfly.shulkerbox;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

/**
 * A dispenser places a shulker box (SPEC-modern-movement.md §81; 26.3's `ShulkerBoxDispenseBehavior`): in front of it,
 * opening the dispenser's way when the block under that spot is empty, else up. When it can't, nothing comes out (the
 * dispenser's fail click, as 26.3's optional behaviour).
 */
public class ShulkerBoxDispense implements IBehaviorDispenseItem {

	@Override
	public ItemStack dispense(IBlockSource source, ItemStack stack) {
		World world = source.getWorld();
		EnumFacing facing = BlockDispenser.func_149937_b(source.getBlockMetadata());
		int x = source.getXInt() + facing.getFrontOffsetX();
		int y = source.getYInt() + facing.getFrontOffsetY();
		int z = source.getZInt() + facing.getFrontOffsetZ();
		Block block = Block.getBlockFromItem(stack.getItem());
		boolean placed = false;
		if (world.isAirBlock(x, y, z) && block.canPlaceBlockAt(world, x, y, z) && world.setBlock(x, y, z, block, 0, 3)) {
			int side = world.isAirBlock(x, y - 1, z) ? facing.ordinal() : 1;
			ShulkerBoxBlock.placed(world, x, y, z, side, stack);
			stack.splitStack(1);
			placed = true;
		}
		world.playAuxSFX(placed ? 1000 : 1001, source.getXInt(), source.getYInt(), source.getZInt(), 0);
		return stack;
	}
}
