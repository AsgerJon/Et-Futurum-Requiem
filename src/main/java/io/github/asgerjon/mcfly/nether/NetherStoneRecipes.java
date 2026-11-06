package io.github.asgerjon.mcfly.nether;

import cpw.mods.fml.common.registry.GameRegistry;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/**
 * 26.3's recipes for the Nether's stones (SPEC-modern-movement.md §88.3; `netherstones_recipes_26_3.txt` in the tests is
 * its jar's list, stonecutting aside): blackstone to polished to bricks, cracked by smelting, chiseled from slabs; each
 * one's slab, stairs and wall; the button and plate; basalt to polished, smelted smooth; the gold ore smelted; red,
 * cracked and chiseled nether bricks; quartz bricks; and blackstone for the furnace, the brewing stand and stone tools
 * (26.3's `#stone_crafting_materials` and `#stone_tool_materials`).
 */
public final class NetherStoneRecipes {

	private NetherStoneRecipes() {
	}

	public static void register() {
		if (McFlyBlocks.BLACKSTONE.isEnabled()) {
			blackstone();
		}
		if (McFlyBlocks.BASALT.isEnabled()) {
			Block basalt = McFlyBlocks.BASALT.get();
			GameRegistry.addRecipe(new ItemStack(basalt, 4, 1), "SS", "SS", 'S', new ItemStack(basalt, 1, 0));
			GameRegistry.addSmelting(new ItemStack(basalt, 1, 0), McFlyBlocks.SMOOTH_BASALT.newItemStack(), 0.1F);
		}
		if (McFlyBlocks.NETHER_GOLD_ORE.isEnabled()) {
			GameRegistry.addSmelting(McFlyBlocks.NETHER_GOLD_ORE.newItemStack(), new ItemStack(Items.gold_ingot), 1.0F);
		}
		if (McFlyBlocks.RED_NETHERBRICK.isEnabled()) {
			Block bricks = McFlyBlocks.RED_NETHERBRICK.get();
			ItemStack red = new ItemStack(bricks, 1, 0);
			GameRegistry.addRecipe(new ItemStack(bricks, 1, 0), "NW", "WN", 'N', Items.netherbrick, 'W', Items.nether_wart);
			GameRegistry.addSmelting(new ItemStack(Blocks.nether_brick), new ItemStack(bricks, 1, 1), 0.1F);
			// 1.7.10's nether brick slab: the stone slab's 6
			GameRegistry.addRecipe(new ItemStack(bricks, 1, 2), "#", "#", '#', new ItemStack(Blocks.stone_slab, 1, 6));
			GameRegistry.addRecipe(McFlyBlocks.RED_NETHERBRICK_SLAB.newItemStack(6), "###", '#', red);
			GameRegistry.addRecipe(McFlyBlocks.RED_NETHERBRICK_STAIRS.newItemStack(4), "#  ", "## ", "###", '#', red);
			GameRegistry.addRecipe(McFlyBlocks.RED_NETHER_BRICK_WALL.newItemStack(6), "###", "###", '#', red);
		}
		if (McFlyBlocks.QUARTZ_BRICKS.isEnabled()) {
			GameRegistry.addRecipe(McFlyBlocks.QUARTZ_BRICKS.newItemStack(4), "##", "##", '#', new ItemStack(Blocks.quartz_block, 1, 0));
		}
	}

	private static void blackstone() {
		Block stone = McFlyBlocks.BLACKSTONE.get();
		Block slab = McFlyBlocks.BLACKSTONE_SLAB.get();
		Block wall = McFlyBlocks.BLACKSTONE_WALL.get();
		ItemStack blackstone = new ItemStack(stone, 1, 0);
		ItemStack polished = new ItemStack(stone, 1, 1);
		ItemStack bricks = new ItemStack(stone, 1, 2);
		GameRegistry.addRecipe(new ItemStack(stone, 4, 1), "SS", "SS", 'S', blackstone);
		GameRegistry.addRecipe(new ItemStack(stone, 4, 2), "##", "##", '#', polished);
		GameRegistry.addRecipe(new ItemStack(stone, 1, 4), "#", "#", '#', new ItemStack(slab, 1, 1));
		GameRegistry.addSmelting(bricks, new ItemStack(stone, 1, 3), 0.1F);
		ItemStack[] kinds = {blackstone, polished, bricks};
		McFlyBlocks[] stairs = {McFlyBlocks.BLACKSTONE_STAIRS, McFlyBlocks.POLISHED_BLACKSTONE_STAIRS, McFlyBlocks.POLISHED_BLACKSTONE_BRICK_STAIRS};
		for (int i = 0; i < 3; i++) {
			GameRegistry.addRecipe(new ItemStack(slab, 6, i), "###", '#', kinds[i]);
			GameRegistry.addRecipe(stairs[i].newItemStack(4), "#  ", "## ", "###", '#', kinds[i]);
			GameRegistry.addRecipe(new ItemStack(wall, 6, i), "###", "###", '#', kinds[i]);
		}
		GameRegistry.addShapelessRecipe(McFlyBlocks.POLISHED_BLACKSTONE_BUTTON.newItemStack(), polished);
		GameRegistry.addRecipe(McFlyBlocks.POLISHED_BLACKSTONE_PRESSURE_PLATE.newItemStack(), "##", '#', polished);
		// 26.3's #stone_crafting_materials and #stone_tool_materials hold blackstone
		GameRegistry.addRecipe(new ItemStack(Blocks.furnace), "###", "# #", "###", '#', blackstone);
		GameRegistry.addRecipe(new ItemStack(Items.brewing_stand), " B ", "###", 'B', Items.blaze_rod, '#', blackstone);
		GameRegistry.addRecipe(new ItemStack(Items.stone_pickaxe), "XXX", " # ", " # ", 'X', blackstone, '#', Items.stick);
		GameRegistry.addRecipe(new ItemStack(Items.stone_axe), "XX", "X#", " #", 'X', blackstone, '#', Items.stick);
		GameRegistry.addRecipe(new ItemStack(Items.stone_hoe), "XX", " #", " #", 'X', blackstone, '#', Items.stick);
		GameRegistry.addRecipe(new ItemStack(Items.stone_shovel), "X", "#", "#", 'X', blackstone, '#', Items.stick);
		GameRegistry.addRecipe(new ItemStack(Items.stone_sword), "X", "X", "#", 'X', blackstone, '#', Items.stick);
	}
}
