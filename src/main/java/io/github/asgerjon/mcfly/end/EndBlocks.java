package io.github.asgerjon.mcfly.end;

import cpw.mods.fml.common.registry.GameRegistry;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.base.McFlyPillar;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/**
 * The End's plain blocks and the recipes of all of them (SPEC-modern-movement.md §84): purpur and its pillar, end stone
 * bricks, each as 26.3's `Blocks` makes it (strength, map colour, a pickaxe for drops, stone sounds); 26.3's recipes.
 */
public final class EndBlocks {

	private EndBlocks() {
	}

	/** A stone block of 26.3's: its strength (resistance as 26.3 counts it), its map colour, dropping only to a pickaxe. */
	public static Block stone(String name, float hardness, float resistance, MapColor colour) {
		McFlyBlock block = new McFlyBlock(Material.rock) {
			@Override
			public MapColor getMapColor(int meta) {
				return colour;
			}
		}.setNames(name);
		block.setHardness(hardness);
		block.setResistance(resistance * 5.0F / 3.0F);
		block.setStepSound(Block.soundTypePiston);
		block.setHarvestLevel("pickaxe", 0);
		return block;
	}

	/** 26.3's purpur pillar: purpur's strength and colour, along its axis. */
	public static Block purpurPillar() {
		McFlyPillar pillar = new McFlyPillar(Material.rock, "purpur_pillar") {
			@Override
			public MapColor getMapColor(int meta) {
				return MapColor.magentaColor;
			}
		}.strength(1.5F, 6.0F);
		pillar.setStepSound(Block.soundTypePiston);
		pillar.setHarvestLevel("pickaxe", 0);
		return pillar;
	}

	/** 26.3's recipes (`cleanroom/batch-end17/data`); stonecutting waits for the stonecutter's own stage. */
	public static void registerRecipes() {
		ItemStack popped = McFlyItems.CHORUS_FRUIT_POPPED.newItemStack();
		Block purpur = McFlyBlocks.PURPUR_BLOCK.get();
		Block pillar = McFlyBlocks.PURPUR_PILLAR.get();
		Block bricks = McFlyBlocks.END_BRICKS.get();
		GameRegistry.addShapedRecipe(McFlyBlocks.PURPUR_BLOCK.newItemStack(4), "FF", "FF", 'F', popped);
		GameRegistry.addShapedRecipe(McFlyBlocks.PURPUR_PILLAR.newItemStack(1), "#", "#", '#', McFlyBlocks.PURPUR_SLAB.get());
		for (Block from : new Block[]{purpur, pillar}) {
			GameRegistry.addShapedRecipe(McFlyBlocks.PURPUR_SLAB.newItemStack(6), "###", '#', from);
			GameRegistry.addShapedRecipe(McFlyBlocks.PURPUR_STAIRS.newItemStack(4), "#  ", "## ", "###", '#', from);
		}
		GameRegistry.addShapedRecipe(McFlyBlocks.END_BRICKS.newItemStack(4), "##", "##", '#', Blocks.end_stone);
		GameRegistry.addShapedRecipe(McFlyBlocks.END_BRICK_SLAB.newItemStack(6), "###", '#', bricks);
		GameRegistry.addShapedRecipe(McFlyBlocks.END_BRICK_STAIRS.newItemStack(4), "#  ", "## ", "###", '#', bricks);
		GameRegistry.addShapedRecipe(McFlyBlocks.END_BRICK_WALL.newItemStack(6), "###", "###", '#', bricks);
		GameRegistry.addShapedRecipe(McFlyBlocks.END_ROD.newItemStack(4), "/", "#", '/', Items.blaze_rod, '#', popped);
		GameRegistry.addSmelting(McFlyItems.CHORUS_FRUIT.get(), popped, 0.1F);
	}
}
