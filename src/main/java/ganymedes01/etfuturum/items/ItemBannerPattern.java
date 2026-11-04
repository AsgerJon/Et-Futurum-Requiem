package ganymedes01.etfuturum.items;

import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;

/**
 * A banner pattern item, as 26.3's (SPEC-modern-movement.md §66.6): a stack of one, its rarity, its one pattern for the
 * loom (by 26.3's name; EFR's `EnumBannerPattern` looked up when needed, not while items are registered).
 */
public class ItemBannerPattern extends BaseItem {

	private final String pattern;
	private final EnumRarity rarity;

	public ItemBannerPattern(String name, String pattern, EnumRarity rarity) {
		super(name);
		this.pattern = pattern;
		this.rarity = rarity;
		setMaxStackSize(1);
	}

	/** The pattern it gives the loom, by 26.3's name ("flower"). */
	public String pattern() {
		return pattern;
	}

	@Override
	public EnumRarity getRarity(ItemStack stack) {
		return rarity;
	}
}
