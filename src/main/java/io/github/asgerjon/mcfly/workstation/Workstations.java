package io.github.asgerjon.mcfly.workstation;

import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyRecipes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;

/**
 * The workstations' crafting (SPEC-modern-movement.md §92): 26.3's recipes for each that's on.
 */
public final class Workstations {

	private Workstations() {
	}

	/** In init. */
	public static void register() {
		// 26.3's: four logs (`#logs`: EFR's `logWood`) round a furnace
		if (McFlyBlocks.SMOKER.isEnabled()) {
			McFlyRecipes.shaped(McFlyBlocks.SMOKER.newItemStack(), " # ", "#X#", " # ", '#', "logWood", 'X', Blocks.furnace);
		}
		// 26.3's: five iron ingots, a furnace, three smooth stone (EFR's stone stand-in gone)
		if (McFlyBlocks.BLAST_FURNACE.isEnabled() && McFlyBlocks.SMOOTH_STONE.isEnabled()) {
			McFlyRecipes.shaped(McFlyBlocks.BLAST_FURNACE.newItemStack(), "III", "IXI", "###", 'I', Items.iron_ingot, 'X', Blocks.furnace, '#',
				McFlyBlocks.SMOOTH_STONE.newItemStack());
		}
		// 26.3's: planks round two wooden slabs (`#planks`, `#wooden_slabs`)
		if (McFlyBlocks.BARREL.isEnabled()) {
			McFlyRecipes.shaped(McFlyBlocks.BARREL.newItemStack(), "PSP", "P P", "PSP", 'P', "plankWood", 'S', "slabWood");
		}
	}
}
