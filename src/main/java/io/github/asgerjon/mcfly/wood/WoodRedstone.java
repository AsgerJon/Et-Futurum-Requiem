package io.github.asgerjon.mcfly.wood;

import cpw.mods.fml.common.registry.GameRegistry;
import io.github.asgerjon.mcfly.base.McFlyButton;
import io.github.asgerjon.mcfly.base.McFlyPressurePlate;
import io.github.asgerjon.mcfly.base.McFlySetType;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyRecipes;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/**
 * The wood sets' buttons and pressure plates (SPEC-modern-movement.md §91.7, step E), in EFR's ids: the vanilla woods'
 * (spruce to dark oak), the planks' woods' (crimson to bamboo), Biomes O' Plenty's and Witchery's; 26.3's recipes,
 * EFR's ore names (vanilla's oak button and plate are named oak's by EFR's `enableLangReplacements`, which its resource
 * pack's names share).
 */
public final class WoodRedstone {

	private WoodRedstone() {
	}

	private static final String[] OLD = {"spruce", "birch", "jungle", "acacia", "dark_oak"};
	private static final MapColor[] OLD_COLOUR = {MapColor.obsidianColor, MapColor.sandColor, MapColor.dirtColor, MapColor.adobeColor, MapColor.brownColor};
	private static final MapColor[] NEW_COLOUR = {MapColor.netherrackColor, MapColor.cyanColor, MapColor.redColor, MapColor.clothColor, MapColor.yellowColor};

	public static Block oldButton(int i) {
		return new McFlyButton(OLD[i] + "_button", McFlySetType.WOOD, Blocks.planks, i + 1);
	}

	public static Block newButton(Block planks, int i) {
		return new McFlyButton(WoodPlanks.WOODS[i] + "_button", McFlySetType.ofWood(i), planks, i);
	}

	public static Block oldPlate(int i) {
		return new McFlyPressurePlate(OLD[i] + "_pressure_plate", McFlySetType.WOOD, Blocks.planks, i + 1, OLD_COLOUR[i]);
	}

	public static Block newPlate(Block planks, int i) {
		return new McFlyPressurePlate(WoodPlanks.WOODS[i] + "_pressure_plate", McFlySetType.ofWood(i), planks, i, NEW_COLOUR[i]);
	}

	/** A mod's button (`bop_sacredoak_button`) on its planks by meta. */
	public static Block compatButton(String mod, String wood, int meta) {
		return new McFlyButton(mod + "_" + wood + "_button", McFlySetType.WOOD, planks(mod), meta);
	}

	public static Block compatPlate(String mod, String wood, int meta) {
		return new McFlyPressurePlate(mod + "_" + wood + "_pressure_plate", McFlySetType.WOOD, planks(mod), meta, MapColor.woodColor);
	}

	/** A mod's planks, or oak's while the mod isn't loaded (its entries are off then). */
	private static Block planks(String mod) {
		Block planks = mod.equals("bop") ? GameRegistry.findBlock("BiomesOPlenty", "planks") : GameRegistry.findBlock("witchery", "witchwood");
		return planks != null ? planks : Blocks.planks;
	}

	// ---------------------------------------------------------------- recipes, ore names, vanilla's

	public static void register() {
		McFlyBlocks[] oldButtons = {McFlyBlocks.BUTTON_SPRUCE, McFlyBlocks.BUTTON_BIRCH, McFlyBlocks.BUTTON_JUNGLE, McFlyBlocks.BUTTON_ACACIA,
			McFlyBlocks.BUTTON_DARK_OAK};
		McFlyBlocks[] newButtons = {McFlyBlocks.CRIMSON_BUTTON, McFlyBlocks.WARPED_BUTTON, McFlyBlocks.MANGROVE_BUTTON, McFlyBlocks.CHERRY_BUTTON,
			McFlyBlocks.BAMBOO_BUTTON};
		McFlyBlocks[] oldPlates = {McFlyBlocks.PRESSURE_PLATE_SPRUCE, McFlyBlocks.PRESSURE_PLATE_BIRCH, McFlyBlocks.PRESSURE_PLATE_JUNGLE,
			McFlyBlocks.PRESSURE_PLATE_ACACIA, McFlyBlocks.PRESSURE_PLATE_DARK_OAK};
		McFlyBlocks[] newPlates = {McFlyBlocks.CRIMSON_PRESSURE_PLATE, McFlyBlocks.WARPED_PRESSURE_PLATE, McFlyBlocks.MANGROVE_PRESSURE_PLATE,
			McFlyBlocks.CHERRY_PRESSURE_PLATE, McFlyBlocks.BAMBOO_PRESSURE_PLATE};
		for (int i = 0; i < 5; i++) {
			recipes(oldButtons[i], oldPlates[i], new ItemStack(Blocks.planks, 1, i + 1));
			if (McFlyBlocks.WOOD_PLANKS.isEnabled() && WoodPlanks.enabled(i)) {
				recipes(newButtons[i], newPlates[i], McFlyBlocks.WOOD_PLANKS.newItemStack(1, i));
			}
		}
		OreDictionary.registerOre("buttonWood", new ItemStack(Blocks.wooden_button));
		OreDictionary.registerOre("pressurePlateWood", new ItemStack(Blocks.wooden_pressure_plate));
	}

	/** 26.3's: a button 1 of a plank, a plate 1 of two. */
	private static void recipes(McFlyBlocks button, McFlyBlocks plate, ItemStack planks) {
		if (button.isEnabled()) {
			// shaped (1 by 1, anywhere in the grid): sorted before 1.7.10's oak button of any planks, which a shapeless one isn't
			McFlyRecipes.shaped(button.newItemStack(), "#", '#', planks);
			OreDictionary.registerOre("buttonWood", button.newItemStack());
		}
		if (plate.isEnabled()) {
			McFlyRecipes.shaped(plate.newItemStack(), "##", '#', planks);
			OreDictionary.registerOre("pressurePlateWood", plate.newItemStack());
		}
	}
}
