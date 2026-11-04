package ganymedes01.etfuturum.client;

import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

/** 26.3's rarity rules (SPEC-modern-movement.md §61.3): the enchanting step, and the held item's name above the hotbar. */
public final class ModernRarity {

	private ModernRarity() {
	}

	/** Enchanted, an item's own rarity goes up a step: common and uncommon to rare, rare to epic. */
	public static EnumRarity enchanted(EnumRarity base) {
		switch (base) {
			case common:
			case uncommon:
				return EnumRarity.rare;
			case rare:
				return EnumRarity.epic;
			default:
				return base;
		}
	}

	/** The name as the hotbar shows it: in its rarity's colour, italic when renamed. */
	public static String heldName(ItemStack stack) {
		return stack.getRarity().rarityColor.toString() + (stack.hasDisplayName() ? EnumChatFormatting.ITALIC.toString() : "") + stack.getDisplayName();
	}
}
