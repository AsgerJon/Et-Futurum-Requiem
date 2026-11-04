package ganymedes01.etfuturum.recipes.crafting;

import io.github.asgerjon.mcfly.banner.Banners;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import ganymedes01.etfuturum.ModItems;
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
			if (Banners.isBanner(stack)) {
				if (banner) {
					return false;
				}
				banner = true;
			} else if (isShield(stack) && Banners.count(stack) == 0) {
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
			if (Banners.isBanner(stack)) {
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
		NBTTagCompound entity = Banners.entityTag(result, true);
		entity.setInteger("Base", Banners.base(banner));
		NBTTagCompound bannerEntity = Banners.entityTag(banner, false);
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
