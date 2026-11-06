package io.github.asgerjon.mcfly.nether;

import cpw.mods.fml.common.registry.GameRegistry;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/**
 * 26.3's netherite recipes (SPEC-modern-movement.md §88.4; `netherite_recipes_26_3.txt` in the tests is its jar's
 * list): four scrap and four gold ingots to an ingot, nine ingots to a block and back, ancient debris smelted to scrap
 * at 2.0. The tools and armour come from the smithing table (step C2).
 */
public final class NetheriteRecipes {

	private NetheriteRecipes() {
	}

	public static void register() {
		if (!McFlyItems.NETHERITE_INGOT.isEnabled()) {
			return;
		}
		ItemStack scrap = McFlyItems.NETHERITE_SCRAP.newItemStack();
		ItemStack gold = new ItemStack(Items.gold_ingot);
		GameRegistry.addShapelessRecipe(McFlyItems.NETHERITE_INGOT.newItemStack(), scrap, scrap, scrap, scrap, gold, gold, gold, gold);
		GameRegistry.addRecipe(McFlyBlocks.NETHERITE_BLOCK.newItemStack(), "###", "###", "###", '#', McFlyItems.NETHERITE_INGOT.newItemStack());
		GameRegistry.addShapelessRecipe(McFlyItems.NETHERITE_INGOT.newItemStack(9), McFlyBlocks.NETHERITE_BLOCK.newItemStack());
		GameRegistry.addSmelting(McFlyBlocks.ANCIENT_DEBRIS.newItemStack(), McFlyItems.NETHERITE_SCRAP.newItemStack(), 2.0F);
	}
}
