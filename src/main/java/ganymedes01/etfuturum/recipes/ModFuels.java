package ganymedes01.etfuturum.recipes;

import cpw.mods.fml.common.IFuelHandler;
import cpw.mods.fml.common.registry.GameRegistry;
import ganymedes01.etfuturum.ModBlocks;
import net.minecraft.item.ItemStack;

/**
 * Furnace fuels added by EFR: the dried kelp block burns 4001 ticks, modern's value, which cooks 20 items.
 */
public class ModFuels implements IFuelHandler {

	public static void init() {
		GameRegistry.registerFuelHandler(new ModFuels());
	}

	@Override
	public int getBurnTime(ItemStack fuel) {
		if (ModBlocks.DRIED_KELP_BLOCK.isEnabled() && fuel.getItem() == ModBlocks.DRIED_KELP_BLOCK.getItem()) {
			return 4001;
		}
		return 0;
	}
}
