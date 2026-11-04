package io.github.asgerjon.mcfly.banner;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

/**
 * Copying a banner's layers (SPEC-modern-movement.md §80; 26.3's `BannerDuplicateRecipe`): exactly two banners of one
 * colour, one with 1 to 6 layers and one plain, give the patterned one twice (26.3 gives it back besides the copy;
 * 1.7.10's grid has no remaining items for it, so both come out).
 */
public class BannerDuplicateRecipe implements IRecipe {

	@Override
	public boolean matches(InventoryCrafting grid, World world) {
		return source(grid) != null;
	}

	/** The patterned banner, when the grid holds it and a plain one of its colour and nothing else. */
	private static ItemStack source(InventoryCrafting grid) {
		ItemStack source = null;
		ItemStack target = null;
		for (int i = 0; i < grid.getSizeInventory(); i++) {
			ItemStack stack = grid.getStackInSlot(i);
			if (stack == null) {
				continue;
			}
			if (!Banners.isBanner(stack)) {
				return null;
			}
			int layers = Banners.count(stack);
			if (layers > Banners.MAX_LAYERS) {
				return null;
			}
			if (layers > 0) {
				if (source != null) {
					return null;
				}
				source = stack;
			} else {
				if (target != null) {
					return null;
				}
				target = stack;
			}
		}
		return source != null && target != null && Banners.base(source) == Banners.base(target) ? source : null;
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting grid) {
		ItemStack source = source(grid);
		if (source == null) {
			return null;
		}
		ItemStack copies = source.copy();
		copies.stackSize = 2;
		return copies;
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
