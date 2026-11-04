package io.github.asgerjon.mcfly.potion;

import ganymedes01.etfuturum.potion.NamedPotions;
import ganymedes01.etfuturum.potion.PotionContents;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.ShapedOreRecipe;

/**
 * Tipped arrows (SPEC-modern-movement.md §82; 26.3's `TippedArrowRecipe`): 8 arrows around a lingering potion give 8
 * tipped arrows of its potion (by name, its contents copied; by damage, the same damage and custom effects).
 */
public class TippedArrowRecipe extends ShapedOreRecipe {

	public TippedArrowRecipe(ItemStack result, Object... recipe) {
		super(result, recipe);
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting grid) {
		ItemStack potion = grid.getStackInRowAndColumn(1, 1);
		if (potion == null || !McFlyItems.TIPPED_ARROW.isEnabled()) {
			return null;
		}
		if (McFlyConfig.potionsByName && NamedPotions.isReady()) {
			PotionContents contents = PotionContents.of(potion);
			if (contents.potion() != null) {
				ItemStack arrows = McFlyItems.TIPPED_ARROW.newItemStack(8, 0);
				PotionContents.write(arrows, false, contents.potion(), contents.customEffects(), contents.customColor(), contents.customName());
				return arrows;
			}
		}
		boolean custom = potion.hasTagCompound() && potion.getTagCompound().hasKey("CustomPotionEffects", 9);
		if (potion.getItemDamage() == 0 && !custom) {
			return null;
		}
		ItemStack arrows = McFlyItems.TIPPED_ARROW.newItemStack(8, potion.getItemDamage());
		if (custom) {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setTag("CustomPotionEffects", potion.getTagCompound().getTagList("CustomPotionEffects", 10).copy());
			arrows.setTagCompound(tag);
		}
		return arrows;
	}
}
