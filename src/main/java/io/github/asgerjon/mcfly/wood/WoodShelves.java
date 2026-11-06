package io.github.asgerjon.mcfly.wood;

import io.github.asgerjon.mcfly.base.McFlyShelf;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyRecipes;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

/**
 * The wood sets' shelves, new (SPEC-modern-movement.md 91.10, step H): `<wood>_shelf` for the 11 woods of the hanging
 * signs (`WoodSigns.HANGING`), its planks' map colour, on with that wood's hanging signs; 26.3's recipe (six stripped
 * logs, a row between, 6) and fire (30/20, crimson's and warped's none).
 */
public final class WoodShelves {

	private WoodShelves() {
	}

	/** The planks' map colours, by `WoodSigns.HANGING`'s order (cherry's white terracotta: cloth). */
	private static final MapColor[] COLOUR = {MapColor.woodColor, MapColor.obsidianColor, MapColor.sandColor, MapColor.dirtColor, MapColor.adobeColor,
		MapColor.brownColor, MapColor.netherrackColor, MapColor.cyanColor, MapColor.redColor, MapColor.clothColor, MapColor.yellowColor};

	public static Block shelf(int i) {
		return new McFlyShelf(WoodSigns.HANGING[i], COLOUR[i], i < 6 ? () -> Blocks.planks : () -> McFlyBlocks.WOOD_PLANKS.get(), i < 6 ? i : i - 6);
	}

	/** After `Stripping.register` (its table gives each wood's stripped log). */
	public static void register() {
		for (int i = 0; i < WoodSigns.HANGING.length; i++) {
			McFlyBlocks shelf = McFlyBlocks.valueOf(WoodSigns.HANGING[i].toUpperCase() + "_SHELF");
			if (!shelf.isEnabled()) {
				continue;
			}
			String wood = WoodSigns.HANGING[i];
			if (!wood.equals("crimson") && !wood.equals("warped")) {
				Blocks.fire.setFireInfo(shelf.get(), 30, 20);
			}
			Object[] stripped = WoodSigns.strippedLogOf(i);
			if (stripped != null) {
				McFlyRecipes.shaped(shelf.newItemStack(6), "###", "   ", "###", '#', new ItemStack((Block) stripped[0], 1, (Integer) stripped[1]));
			}
		}
	}
}
