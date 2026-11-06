package io.github.asgerjon.mcfly.stone;

import cpw.mods.fml.common.registry.GameRegistry;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;

/**
 * 26.3's deepslate recipes (SPEC-modern-movement.md §89, step B): cobbled deepslate to polished, smelted to deepslate
 * (0.1); each deepslate ore smelted as its stone ore is, for its experience.
 */
public final class DeepslateRecipes {

	private DeepslateRecipes() {
	}

	/** 26.3's for the set: bricks from polished, tiles from bricks, cracked by smelting, chiseled from cobbled slabs; each slab, stairs and wall. */
	private static void set() {
		ItemStack cobbled = McFlyBlocks.COBBLED_DEEPSLATE.newItemStack(), polished = McFlyBlocks.POLISHED_DEEPSLATE.newItemStack();
		ItemStack bricks = McFlyBlocks.DEEPSLATE_BRICKS.newItemStack(1, 0), tiles = McFlyBlocks.DEEPSLATE_BRICKS.newItemStack(1, 2);
		GameRegistry.addRecipe(McFlyBlocks.DEEPSLATE_BRICKS.newItemStack(4, 0), "##", "##", '#', polished);
		GameRegistry.addRecipe(McFlyBlocks.DEEPSLATE_BRICKS.newItemStack(4, 2), "##", "##", '#', bricks);
		GameRegistry.addSmelting(bricks, McFlyBlocks.DEEPSLATE_BRICKS.newItemStack(1, 1), 0.1F);
		GameRegistry.addSmelting(tiles, McFlyBlocks.DEEPSLATE_BRICKS.newItemStack(1, 3), 0.1F);
		GameRegistry.addRecipe(McFlyBlocks.DEEPSLATE_BRICKS.newItemStack(1, 4), "#", "#", '#', McFlyBlocks.DEEPSLATE_SLAB.newItemStack(1, 0));
		ItemStack[] bases = {cobbled, polished, bricks, tiles};
		McFlyBlocks[] slabs = {McFlyBlocks.DEEPSLATE_SLAB, McFlyBlocks.DEEPSLATE_SLAB, McFlyBlocks.DEEPSLATE_BRICK_SLAB, McFlyBlocks.DEEPSLATE_BRICK_SLAB};
		McFlyBlocks[] stairs = {McFlyBlocks.COBBLED_DEEPSLATE_STAIRS, McFlyBlocks.POLISHED_DEEPSLATE_STAIRS, McFlyBlocks.DEEPSLATE_BRICK_STAIRS,
			McFlyBlocks.DEEPSLATE_TILE_STAIRS};
		McFlyBlocks[] walls = {McFlyBlocks.DEEPSLATE_WALL, McFlyBlocks.DEEPSLATE_WALL, McFlyBlocks.DEEPSLATE_BRICK_WALL, McFlyBlocks.DEEPSLATE_BRICK_WALL};
		for (int i = 0; i < 4; i++) {
			GameRegistry.addRecipe(slabs[i].newItemStack(6, i % 2), "###", '#', bases[i]);
			GameRegistry.addRecipe(stairs[i].newItemStack(4), "#  ", "## ", "###", '#', bases[i]);
			GameRegistry.addRecipe(walls[i].newItemStack(6, i % 2), "###", "###", '#', bases[i]);
		}
	}

	public static void register() {
		if (McFlyBlocks.DEEPSLATE.isEnabled()) {
			GameRegistry.addRecipe(McFlyBlocks.POLISHED_DEEPSLATE.newItemStack(4), "SS", "SS", 'S', McFlyBlocks.COBBLED_DEEPSLATE.newItemStack());
			GameRegistry.addSmelting(McFlyBlocks.COBBLED_DEEPSLATE.newItemStack(), McFlyBlocks.DEEPSLATE.newItemStack(), 0.1F);
			// 26.3's #stone_crafting_materials and #stone_tool_materials hold cobbled deepslate (not EFR's ore names, which
			// made pistons, levers and more of it too)
			ItemStack c = McFlyBlocks.COBBLED_DEEPSLATE.newItemStack();
			net.minecraft.item.Item stick = net.minecraft.init.Items.stick;
			GameRegistry.addRecipe(new ItemStack(net.minecraft.init.Blocks.furnace), "###", "# #", "###", '#', c);
			GameRegistry.addRecipe(new ItemStack(net.minecraft.init.Items.brewing_stand), " B ", "###", 'B', net.minecraft.init.Items.blaze_rod, '#', c);
			GameRegistry.addRecipe(new ItemStack(net.minecraft.init.Items.stone_pickaxe), "XXX", " # ", " # ", 'X', c, '#', stick);
			GameRegistry.addRecipe(new ItemStack(net.minecraft.init.Items.stone_axe), "XX", "X#", " #", 'X', c, '#', stick);
			GameRegistry.addRecipe(new ItemStack(net.minecraft.init.Items.stone_hoe), "XX", " #", " #", 'X', c, '#', stick);
			GameRegistry.addRecipe(new ItemStack(net.minecraft.init.Items.stone_shovel), "X", "#", "#", 'X', c, '#', stick);
			GameRegistry.addRecipe(new ItemStack(net.minecraft.init.Items.stone_sword), "X", "X", "#", 'X', c, '#', stick);
		}
		if (McFlyBlocks.DEEPSLATE_BRICKS.isEnabled()) {
			set();
		}
		for (McFlyBlocks ore : McFlyBlocks.VALUES) {
			if (ore.isEnabled() && ore.get() instanceof DeepslateBlocks.Ore && ore != McFlyBlocks.DEEPSLATE_LIT_REDSTONE_ORE) {
				ItemStack stone = new ItemStack(((DeepslateBlocks.Ore) ore.get()).stoneOre());
				ItemStack result = FurnaceRecipes.smelting().getSmeltingResult(stone);
				if (result != null) {
					GameRegistry.addSmelting(ore.newItemStack(), result.copy(), FurnaceRecipes.smelting().func_151398_b(result));
				}
			}
		}
	}
}
