package io.github.asgerjon.mcfly.workstation;

import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.ArrayList;

/**
 * 26.3's block loot `copy_components` of `custom_name` (SPEC-modern-movement.md 92.4): a named barrel, smoker or blast
 * furnace drops itself named so. 1.7.10 asks a player's break for its drops after the tile entity is gone (`harvestBlock`
 * after `removedByPlayer`), so the name is kept from `breakBlock`, where the tile entity is still there.
 */
final class NamedDrops {

	/** The last block broken with a name: where, and the name (one at a time: a harvest follows its break at once). */
	private static World world;
	private static int x, y, z;
	private static String name;

	private NamedDrops() {
	}

	/** From `breakBlock`: the tile entity's name, kept for the drops that follow. */
	static void broken(World at, int bx, int by, int bz, IInventory inventory) {
		if (at.isRemote) {
			return;
		}
		world = at;
		x = bx;
		y = by;
		z = bz;
		name = inventory != null && inventory.hasCustomInventoryName() ? inventory.getInventoryName() : null;
	}

	/** The block's item, named as its tile entity (still there, or just broken here) was. */
	static ArrayList<ItemStack> drops(World at, int dx, int dy, int dz, Block block) {
		ArrayList<ItemStack> drops = new ArrayList<>();
		Item item = Item.getItemFromBlock(block);
		if (item == null) {
			return drops;
		}
		ItemStack stack = new ItemStack(item);
		String named = null;
		if (at.getTileEntity(dx, dy, dz) instanceof IInventory inventory && inventory.hasCustomInventoryName()) {
			named = inventory.getInventoryName();
		} else if (at == world && dx == x && dy == y && dz == z) {
			named = name;
		}
		if (at == world && dx == x && dy == y && dz == z) {
			world = null;
			name = null;
		}
		if (named != null) {
			stack.setStackDisplayName(named);
		}
		drops.add(stack);
		return drops;
	}
}
