package ganymedes01.etfuturum.items;

import net.minecraft.item.ItemStack;

/**
 * Dried kelp (SPEC-modern-movement.md §29): 1 hunger, saturation modifier 0.3, eaten in 16 ticks instead of 32.
 */
public class ItemDriedKelp extends BaseFood {

	public ItemDriedKelp() {
		super(1, 0.3F, false);
		setNames("dried_kelp");
	}

	@Override
	public int getMaxItemUseDuration(ItemStack stack) {
		return 16;
	}
}
