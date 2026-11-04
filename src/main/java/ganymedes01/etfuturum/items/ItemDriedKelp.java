package ganymedes01.etfuturum.items;

import io.github.asgerjon.mcfly.registry.McFlyTab;
import io.github.asgerjon.mcfly.McFly;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;

/**
 * Dried kelp (SPEC-modern-movement.md §29): 1 hunger, saturation modifier 0.3, eaten in 16 ticks instead of 32.
 */
public class ItemDriedKelp extends ItemFood {

	public ItemDriedKelp() {
		super(1, 0.3F, false);
		setCreativeTab(McFlyTab.TAB);
		setUnlocalizedName(McFly.name("dried_kelp"));
		setTextureName("dried_kelp");
	}

	@Override
	public int getMaxItemUseDuration(ItemStack stack) {
		return 16;
	}
}
