package io.github.asgerjon.mcfly.nether;

import cpw.mods.fml.common.registry.GameRegistry;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/**
 * 26.3's recipes for the Nether's plants and stems (SPEC-modern-movement.md §88.7; `netherplants_recipes_26_3.txt`):
 * four stems to three hyphae (stripped too), any stem or hyphae to four planks (EFR's `wood_planks`, 0 crimson, 1
 * warped, till the wood sets' item), nine nether wart to a block.
 */
public final class NetherPlantRecipes {

	private NetherPlantRecipes() {
	}

	public static void register() {
		McFlyBlocks[] stems = {McFlyBlocks.CRIMSON_STEM, McFlyBlocks.WARPED_STEM};
		for (int wood = 0; wood < 2; wood++) {
			if (!stems[wood].isEnabled()) {
				continue;
			}
			GameRegistry.addRecipe(stems[wood].newItemStack(3, 1), "##", "##", '#', stems[wood].newItemStack(1, 0));
			GameRegistry.addRecipe(stems[wood].newItemStack(3, 3), "##", "##", '#', stems[wood].newItemStack(1, 2));
			if (io.github.asgerjon.mcfly.registry.McFlyBlocks.WOOD_PLANKS.isEnabled()) {
				for (int kind = 0; kind < 4; kind++) {
					GameRegistry.addShapelessRecipe(io.github.asgerjon.mcfly.registry.McFlyBlocks.WOOD_PLANKS.newItemStack(4, wood), stems[wood].newItemStack(1, kind));
				}
			}
		}
		if (McFlyBlocks.NETHER_WART.isEnabled()) {
			ItemStack wart = new ItemStack(Items.nether_wart);
			GameRegistry.addShapelessRecipe(McFlyBlocks.NETHER_WART.newItemStack(), wart, wart, wart, wart, wart, wart, wart, wart, wart);
		}
	}
}
