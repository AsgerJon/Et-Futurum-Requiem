package ganymedes01.etfuturum.recipes.crafting;

import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.items.ItemLingeringPotion;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.util.List;

public class RecipeTippedArrow extends ShapedOreRecipe {

	public RecipeTippedArrow(ItemStack result, Object... recipe) {
		super(result, recipe);
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting grid) {
		ItemStack potion = grid.getStackInRowAndColumn(1, 1);
		// a potion by name (spec §69): the arrows hold the same potion, as 26.3 copies its potion_contents
		if (io.github.asgerjon.mcfly.config.McFlyConfig.potionsByName && ganymedes01.etfuturum.potion.NamedPotions.isReady()) {
			ganymedes01.etfuturum.potion.PotionContents contents = ganymedes01.etfuturum.potion.PotionContents.of(potion);
			if (contents.potion() != null) {
				ItemStack arrows = ModItems.TIPPED_ARROW.newItemStack(8, 0);
				ganymedes01.etfuturum.potion.PotionContents.write(arrows, false, contents.potion(), contents.customEffects(), contents.customColor(), contents.customName());
				return arrows;
			}
		}
		List<PotionEffect> effects = ((ItemLingeringPotion) ModItems.LINGERING_POTION.get()).getEffects(potion);

		if (potion.getItemDamage() == 0 && effects.isEmpty())
			return null;

		ItemStack stack = ModItems.TIPPED_ARROW.newItemStack(8, potion.getItemDamage());
		if (!effects.isEmpty() && potion.hasTagCompound() && potion.getTagCompound().hasKey("CustomPotionEffects", 9)) {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setTag("CustomPotionEffects", potion.getTagCompound().getTagList("CustomPotionEffects", 10).copy());
			stack.setTagCompound(tag);
		}

		return stack;
	}
}