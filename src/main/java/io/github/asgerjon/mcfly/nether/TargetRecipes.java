package io.github.asgerjon.mcfly.nether;

import cpw.mods.fml.common.registry.GameRegistry;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;

/** 26.3's target recipe (SPEC-modern-movement.md §88.8): four redstone around a hay bale. */
public final class TargetRecipes {

	private TargetRecipes() {
	}

	public static void register() {
		if (McFlyBlocks.TARGET.isEnabled()) {
			GameRegistry.addRecipe(McFlyBlocks.TARGET.newItemStack(), " R ", "RHR", " R ", 'R', Items.redstone, 'H', Blocks.hay_block);
		}
	}
}
