package io.github.asgerjon.mcfly.wood;

import cpw.mods.fml.common.registry.GameRegistry;
import io.github.asgerjon.mcfly.base.McFlyDoor;
import io.github.asgerjon.mcfly.base.McFlySetType;
import io.github.asgerjon.mcfly.base.McFlyTrapdoor;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyRecipes;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.util.Iterator;

/**
 * The wood sets' doors and trapdoors (SPEC-modern-movement.md §91.6, step D), in EFR's ids: the vanilla woods' (spruce
 * to dark oak), the planks' woods' (crimson to bamboo), Biomes O' Plenty's and Witchery's, EFR's iron trapdoor; 26.3's
 * recipes, EFR's ore names, vanilla's doors stacking to 64 and 3 a recipe.
 */
public final class WoodDoors {

	private WoodDoors() {
	}

	private static final String[] OLD = {"spruce", "birch", "jungle", "acacia", "dark_oak"};
	private static final MapColor[] OLD_COLOUR = {MapColor.obsidianColor, MapColor.sandColor, MapColor.dirtColor, MapColor.adobeColor, MapColor.brownColor};
	private static final MapColor[] NEW_COLOUR = {MapColor.netherrackColor, MapColor.cyanColor, MapColor.redColor, MapColor.clothColor, MapColor.yellowColor};

	public static Block oldDoor(int i) {
		return new McFlyDoor(OLD[i] + "_door", McFlySetType.WOOD, OLD_COLOUR[i], OLD[i] + "_door");
	}

	public static Block newDoor(int i) {
		return new McFlyDoor(WoodPlanks.WOODS[i] + "_door", McFlySetType.ofWood(i), NEW_COLOUR[i], WoodPlanks.WOODS[i] + "_door");
	}

	/** A mod's door (`bop_sacredoak_door`): its textures `biomesoplenty:sacredoak_door_top`, `_bottom`, its item's `biomesoplenty:sacredoak_door`. */
	public static Block compatDoor(String mod, String wood) {
		return new McFlyDoor(mod + "_" + wood + "_door", McFlySetType.WOOD, MapColor.woodColor, domain(mod) + ":" + wood + "_door");
	}

	/** Oak's and dark oak's trapdoors don't turn their texture (26.3's `template_trapdoor_*`); the rest do. */
	public static Block oldTrapdoor(int i) {
		return McFlyTrapdoor.wooden(OLD[i] + "_trapdoor", McFlySetType.WOOD, OLD_COLOUR[i], OLD[i] + "_trapdoor", !OLD[i].equals("dark_oak"));
	}

	public static Block newTrapdoor(int i) {
		return McFlyTrapdoor.wooden(WoodPlanks.WOODS[i] + "_trapdoor", McFlySetType.ofWood(i), NEW_COLOUR[i], WoodPlanks.WOODS[i] + "_trapdoor", true);
	}

	public static Block compatTrapdoor(String mod, String wood) {
		return McFlyTrapdoor.wooden(mod + "_" + wood + "_trapdoor", McFlySetType.WOOD, MapColor.woodColor, domain(mod) + ":" + wood + "_trapdoor", true);
	}

	/** EFR's iron trapdoor as 26.3's: 5 and 5, a pickaxe's (needed for drops), opened by redstone alone, iron's sounds. */
	public static Block ironTrapdoor() {
		McFlyTrapdoor trapdoor = new McFlyTrapdoor("iron_trapdoor", Material.iron, McFlySetType.IRON, MapColor.ironColor, "iron_trapdoor", false);
		trapdoor.strength(5.0F, 5.0F);
		trapdoor.setHarvestLevel("pickaxe", 0);
		return trapdoor;
	}

	private static String domain(String mod) {
		return mod.equals("bop") ? "biomesoplenty" : mod;
	}

	// ---------------------------------------------------------------- recipes, ore names, vanilla's

	public static void register() {
		McFlyBlocks[] oldDoors = {McFlyBlocks.DOOR_SPRUCE, McFlyBlocks.DOOR_BIRCH, McFlyBlocks.DOOR_JUNGLE, McFlyBlocks.DOOR_ACACIA, McFlyBlocks.DOOR_DARK_OAK};
		McFlyBlocks[] newDoors = {McFlyBlocks.CRIMSON_DOOR, McFlyBlocks.WARPED_DOOR, McFlyBlocks.MANGROVE_DOOR, McFlyBlocks.CHERRY_DOOR, McFlyBlocks.BAMBOO_DOOR};
		McFlyBlocks[] oldTrapdoors = {McFlyBlocks.TRAPDOOR_SPRUCE, McFlyBlocks.TRAPDOOR_BIRCH, McFlyBlocks.TRAPDOOR_JUNGLE, McFlyBlocks.TRAPDOOR_ACACIA,
			McFlyBlocks.TRAPDOOR_DARK_OAK};
		McFlyBlocks[] newTrapdoors = {McFlyBlocks.CRIMSON_TRAPDOOR, McFlyBlocks.WARPED_TRAPDOOR, McFlyBlocks.MANGROVE_TRAPDOOR, McFlyBlocks.CHERRY_TRAPDOOR,
			McFlyBlocks.BAMBOO_TRAPDOOR};
		for (int i = 0; i < 5; i++) {
			recipes(oldDoors[i], oldTrapdoors[i], new ItemStack(Blocks.planks, 1, i + 1));
			if (McFlyBlocks.WOOD_PLANKS.isEnabled() && WoodPlanks.enabled(i)) {
				recipes(newDoors[i], newTrapdoors[i], McFlyBlocks.WOOD_PLANKS.newItemStack(1, i));
			}
		}
		if (McFlyBlocks.IRON_TRAPDOOR.isEnabled()) {
			McFlyRecipes.shaped(McFlyBlocks.IRON_TRAPDOOR.newItemStack(), "##", "##", '#', "ingotIron");
		}
		OreDictionary.registerOre("doorWood", new ItemStack(Items.wooden_door));
		OreDictionary.registerOre("trapdoorWood", new ItemStack(Blocks.trapdoor));
		// vanilla's doors: 26.3's stacks of 64, and 3 a recipe (vanilla's oak door of any plankWood no wood's own takes)
		if (McFlyConfig.enableVanillaDoors) {
			Items.wooden_door.setMaxStackSize(64);
			Items.iron_door.setMaxStackSize(64);
		}
		if (McFlyConfig.enableDoorRecipeBuffs) {
			removeFirstRecipeFor(Items.wooden_door);
			GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(Items.wooden_door, 3), "##", "##", "##", '#', "plankWood"));
			removeFirstRecipeFor(Items.iron_door);
			GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(Items.iron_door, 3), "##", "##", "##", '#', "ingotIron"));
		}
	}

	/** 26.3's: a door 3 of six planks, a trapdoor 2 of six. */
	private static void recipes(McFlyBlocks door, McFlyBlocks trapdoor, ItemStack planks) {
		if (door.isEnabled()) {
			McFlyRecipes.shaped(door.newItemStack(McFlyConfig.enableDoorRecipeBuffs ? 3 : 1), "##", "##", "##", '#', planks);
			OreDictionary.registerOre("doorWood", door.newItemStack());
		}
		if (trapdoor.isEnabled()) {
			McFlyRecipes.shaped(trapdoor.newItemStack(2), "###", "###", '#', planks);
			OreDictionary.registerOre("trapdoorWood", trapdoor.newItemStack());
		}
	}

	@SuppressWarnings("unchecked")
	private static void removeFirstRecipeFor(Item item) {
		Iterator<IRecipe> recipes = CraftingManager.getInstance().getRecipeList().iterator();
		while (recipes.hasNext()) {
			ItemStack made = recipes.next().getRecipeOutput();
			if (made != null && made.getItem() == item) {
				recipes.remove();
				return;
			}
		}
	}
}
