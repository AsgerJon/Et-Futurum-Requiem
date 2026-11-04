package io.github.asgerjon.mcfly.shulkerbox;

import io.github.asgerjon.mcfly.banner.Banners;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * Dyeing a shulker box (SPEC-modern-movement.md §81; 26.3's shulker box transmute recipes): a box of any colour and one
 * dye in the grid give the box in the dye's colour, its contents and name kept.
 */
public class ShulkerBoxDyeRecipe implements IRecipe {

	@Override
	public boolean matches(InventoryCrafting grid, World world) {
		return getCraftingResult(grid) != null;
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting grid) {
		ItemStack box = null;
		int colour = -1;
		for (int i = 0; i < grid.getSizeInventory(); i++) {
			ItemStack stack = grid.getStackInSlot(i);
			if (stack == null) {
				continue;
			}
			if (ShulkerBoxBlock.isBox(stack)) {
				if (box != null) {
					return null;
				}
				box = stack;
			} else if (Banners.isDye(stack)) {
				if (colour >= 0) {
					return null;
				}
				colour = Banners.dyeColour(stack);
			} else {
				return null;
			}
		}
		if (box == null || colour < 0) {
			return null;
		}
		ItemStack dyed = box.copy();
		dyed.stackSize = 1;
		if (!dyed.hasTagCompound()) {
			dyed.setTagCompound(new NBTTagCompound());
		}
		dyed.getTagCompound().setByte("Color", (byte) (colour + 1));
		return dyed;
	}

	@Override
	public int getRecipeSize() {
		return 2;
	}

	@Override
	public ItemStack getRecipeOutput() {
		return null;
	}
}
