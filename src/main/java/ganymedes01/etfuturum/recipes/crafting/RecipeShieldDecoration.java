package ganymedes01.etfuturum.recipes.crafting;

import io.github.asgerjon.mcfly.registry.McFlyItems;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.blocks.ModernBanners;
import ganymedes01.etfuturum.blocks.itemblocks.ItemBlockBanner;
import ganymedes01.etfuturum.tileentities.TileEntityBanner;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * 26.3's `ShieldDecorationRecipe` (SPEC-modern-movement.md §66.9): exactly a shield without layers and a banner (any,
 * a plain one too) make the shield, its own data kept, with the banner's colour and layers. The banner is used up.
 */
public class RecipeShieldDecoration implements IRecipe {

	private static boolean isShield(ItemStack stack) {
		return stack != null && McFlyItems.SHIELD.isEnabled() && stack.getItem() == McFlyItems.SHIELD.get();
	}

	@Override
	public boolean matches(InventoryCrafting grid, World world) {
		boolean shield = false;
		boolean banner = false;
		for (int i = 0; i < grid.getSizeInventory(); i++) {
			ItemStack stack = grid.getStackInSlot(i);
			if (stack == null) {
				continue;
			}
			if (ModernBanners.isBanner(stack)) {
				if (banner) {
					return false;
				}
				banner = true;
			} else if (isShield(stack) && TileEntityBanner.getPatterns(stack) == 0) {
				if (shield) {
					return false;
				}
				shield = true;
			} else {
				return false;
			}
		}
		return shield && banner;
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting grid) {
		ItemStack shield = null;
		ItemStack banner = null;
		for (int i = 0; i < grid.getSizeInventory(); i++) {
			ItemStack stack = grid.getStackInSlot(i);
			if (ModernBanners.isBanner(stack)) {
				banner = stack;
			} else if (isShield(stack)) {
				shield = stack;
			}
		}
		if (shield == null || banner == null) {
			return null;
		}
		ItemStack result = shield.copy();
		result.stackSize = 1;
		NBTTagCompound entity = ItemBlockBanner.getSubTag(result, "BlockEntityTag", true);
		entity.setInteger("Base", TileEntityBanner.getBaseColor(banner));
		NBTTagCompound bannerEntity = ItemBlockBanner.getSubTag(banner, "BlockEntityTag", false);
		if (bannerEntity != null && bannerEntity.hasKey("Patterns", 9)) {
			entity.setTag("Patterns", bannerEntity.getTagList("Patterns", 10).copy());
		} else {
			entity.removeTag("Patterns");
		}
		return result;
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
