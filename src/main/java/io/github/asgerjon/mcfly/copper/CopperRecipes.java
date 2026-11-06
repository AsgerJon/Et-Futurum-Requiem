package io.github.asgerjon.mcfly.copper;

import cpw.mods.fml.common.registry.GameRegistry;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * 26.3's copper, lightning rod and raw ore recipes (SPEC-modern-movement.md §87): 105, the Copper Age and stonecutting
 * aside (`copper_recipes_26_3.txt` in the tests is its jar's list). Each stage and wax of cut copper, its slab, stairs,
 * chiseled copper, grate and bulb from the block before it; doors, trapdoors and rods from ingots; honeycomb and any
 * unwaxed one to its waxed one; ores and raw ores smelted, raw ores 9 to a block and back.
 */
public final class CopperRecipes {

	private CopperRecipes() {
	}

	public static void register() {
		if (McFlyBlocks.COPPER_BLOCK.isEnabled() && McFlyItems.COPPER_INGOT.isEnabled()) {
			copper();
		}
		if (McFlyItems.RAW_ORE.isEnabled() && McFlyBlocks.RAW_ORE_BLOCK.isEnabled()) {
			Item raw = McFlyItems.RAW_ORE.get();
			Block block = McFlyBlocks.RAW_ORE_BLOCK.get();
			for (int kind = 0; kind < 3; kind++) {
				GameRegistry.addRecipe(new ItemStack(block, 1, kind), "###", "###", "###", '#', new ItemStack(raw, 1, kind));
				GameRegistry.addShapelessRecipe(new ItemStack(raw, 9, kind), new ItemStack(block, 1, kind));
			}
			GameRegistry.addSmelting(new ItemStack(raw, 1, RawOreItem.IRON), new ItemStack(Items.iron_ingot), 0.7F);
			GameRegistry.addSmelting(new ItemStack(raw, 1, RawOreItem.GOLD), new ItemStack(Items.gold_ingot), 1.0F);
			if (McFlyItems.COPPER_INGOT.isEnabled()) {
				GameRegistry.addSmelting(new ItemStack(raw, 1, RawOreItem.COPPER), McFlyItems.COPPER_INGOT.newItemStack(), 0.7F);
			}
		}
	}

	private static void copper() {
		Block copper = McFlyBlocks.COPPER_BLOCK.get();
		ItemStack ingot = McFlyItems.COPPER_INGOT.newItemStack();
		GameRegistry.addRecipe(new ItemStack(copper), "###", "###", "###", '#', ingot);
		GameRegistry.addShapelessRecipe(McFlyItems.COPPER_INGOT.newItemStack(9), new ItemStack(copper, 1, CopperFullBlock.meta(0, false, false)));
		GameRegistry.addShapelessRecipe(McFlyItems.COPPER_INGOT.newItemStack(9), new ItemStack(copper, 1, CopperFullBlock.meta(0, false, true)));
		for (McFlyBlocks ore : new McFlyBlocks[]{McFlyBlocks.COPPER_ORE, McFlyBlocks.DEEPSLATE_COPPER_ORE}) {
			if (ore.isEnabled()) {
				GameRegistry.addSmelting(ore.get(), McFlyItems.COPPER_INGOT.newItemStack(), 0.7F);
			}
		}
		Item honeycomb = CopperEvents.honeycomb();
		for (int stage = 0; stage < 4; stage++) {
			for (boolean waxed : new boolean[]{false, true}) {
				ItemStack full = new ItemStack(copper, 1, CopperFullBlock.meta(stage, false, waxed));
				ItemStack cut = new ItemStack(copper, 1, CopperFullBlock.meta(stage, true, waxed));
				ItemStack slab = new ItemStack(McFlyBlocks.CUT_COPPER_SLAB.get(), 1, stage + (waxed ? 4 : 0));
				GameRegistry.addRecipe(count(cut, 4), "##", "##", '#', full);
				GameRegistry.addRecipe(count(slab, 6), "###", '#', cut);
				GameRegistry.addRecipe(new ItemStack(CopperStairsBlock.of(stage, waxed), 4), "#  ", "## ", "###", '#', cut);
				GameRegistry.addRecipe(new ItemStack(McFlyBlocks.CHISELED_COPPER.get(), 1, stage + (waxed ? 4 : 0)), "#", "#", '#', slab);
				GameRegistry.addRecipe(new ItemStack(McFlyBlocks.COPPER_GRATE.get(), 4, stage + (waxed ? 4 : 0)), " M ", "M M", " M ", 'M', full);
				GameRegistry.addRecipe(new ItemStack(McFlyBlocks.COPPER_BULB.get(), 4, stage + (waxed ? 8 : 0)), " C ", "CBC", " R ", 'C', full,
					'B', Items.blaze_rod, 'R', Items.redstone);
			}
			if (honeycomb != null) {
				// honeycomb and the unwaxed to the waxed, for each kind
				wax(honeycomb, new ItemStack(copper, 1, CopperFullBlock.meta(stage, false, false)), new ItemStack(copper, 1, CopperFullBlock.meta(stage, false, true)));
				wax(honeycomb, new ItemStack(copper, 1, CopperFullBlock.meta(stage, true, false)), new ItemStack(copper, 1, CopperFullBlock.meta(stage, true, true)));
				wax(honeycomb, new ItemStack(McFlyBlocks.CUT_COPPER_SLAB.get(), 1, stage), new ItemStack(McFlyBlocks.CUT_COPPER_SLAB.get(), 1, stage + 4));
				wax(honeycomb, new ItemStack(CopperStairsBlock.of(stage, false)), new ItemStack(CopperStairsBlock.of(stage, true)));
				wax(honeycomb, new ItemStack(McFlyBlocks.CHISELED_COPPER.get(), 1, stage), new ItemStack(McFlyBlocks.CHISELED_COPPER.get(), 1, stage + 4));
				wax(honeycomb, new ItemStack(McFlyBlocks.COPPER_GRATE.get(), 1, stage), new ItemStack(McFlyBlocks.COPPER_GRATE.get(), 1, stage + 4));
				wax(honeycomb, new ItemStack(McFlyBlocks.COPPER_BULB.get(), 1, stage), new ItemStack(McFlyBlocks.COPPER_BULB.get(), 1, stage + 8));
				wax(honeycomb, new ItemStack(CopperDoorBlock.of(stage, false)), new ItemStack(CopperDoorBlock.of(stage, true)));
				wax(honeycomb, new ItemStack(CopperTrapdoorBlock.of(stage, false)), new ItemStack(CopperTrapdoorBlock.of(stage, true)));
				if (McFlyBlocks.LIGHTNING_ROD.isEnabled()) {
					wax(honeycomb, new ItemStack(LightningRodBlock.of(stage, false)), new ItemStack(LightningRodBlock.of(stage, true)));
				}
			}
		}
		GameRegistry.addRecipe(new ItemStack(CopperDoorBlock.of(0, false), 3), "##", "##", "##", '#', ingot);
		GameRegistry.addRecipe(new ItemStack(CopperTrapdoorBlock.of(0, false)), "##", "##", '#', ingot);
		if (McFlyBlocks.LIGHTNING_ROD.isEnabled()) {
			GameRegistry.addRecipe(new ItemStack(LightningRodBlock.of(0, false)), "#", "#", "#", '#', ingot);
		}
	}

	private static void wax(Item honeycomb, ItemStack unwaxed, ItemStack waxed) {
		GameRegistry.addShapelessRecipe(waxed, unwaxed, new ItemStack(honeycomb));
	}

	private static ItemStack count(ItemStack stack, int count) {
		ItemStack copy = stack.copy();
		copy.stackSize = count;
		return copy;
	}
}
