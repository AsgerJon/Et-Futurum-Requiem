package io.github.asgerjon.mcfly.wood;

import cpw.mods.fml.common.registry.GameRegistry;
import io.github.asgerjon.mcfly.base.McFlyFence;
import io.github.asgerjon.mcfly.base.McFlyFence.Wood;
import io.github.asgerjon.mcfly.base.McFlyFenceGate;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyRecipes;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

/**
 * The wood sets' fences and gates (SPEC-modern-movement.md §91.5, step C), in EFR's ids: the vanilla woods' own fences
 * and gates (spruce to dark oak), `wood_fence` (crimson to bamboo, the planks' woods) and their gates, and Biomes O'
 * Plenty's and Witchery's as EFR made them; 26.3's recipes, EFR's ore names, vanilla's oak fence and gate burning.
 */
public final class WoodFences {

	private WoodFences() {
	}

	private static final String[] OLD = {"spruce", "birch", "jungle", "acacia", "dark_oak"};
	/** The vanilla woods' planks' colours (26.3's, as 1.7.10's ids: spruce podzol, birch sand, jungle dirt, acacia orange, dark oak brown). */
	private static final MapColor[] OLD_COLOUR = {MapColor.obsidianColor, MapColor.sandColor, MapColor.dirtColor, MapColor.adobeColor, MapColor.brownColor};
	private static final String[] BOP = {"sacredoak", "cherry", "dark", "fir", "ethereal", "magic", "mangrove", "palm", "redwood", "willow", "bamboo",
		"pine", "hellbark", "jacaranda", "mahogany"};
	private static final String[] WITCHERY = {"rowan", "alder", "hawthorn"};

	/** A vanilla wood's (spruce 0 to dark oak 4): Blocks.planks 1 to 5. */
	public static Wood old(int i) {
		return new Wood(OLD[i], Blocks.planks, i + 1, true, Block.soundTypeWood, "block.fence_gate", OLD_COLOUR[i], null, true);
	}

	/** The planks' woods (crimson 0 to bamboo 4, `wood_planks`). */
	public static Wood modern(Block planks, int i) {
		String[] gateSounds = {"block.nether_wood_fence_gate", "block.nether_wood_fence_gate", "block.fence_gate", "block.cherry_wood_fence_gate",
			"block.bamboo_wood_fence_gate"};
		return new Wood(WoodPlanks.WOODS[i], planks, i, WoodPlanks.burns(i), WoodPlanks.sound(i), gateSounds[i], planks.getMapColor(i), i == 4 ? "bamboo" : null,
			WoodPlanks.enabled(i));
	}

	private static Wood compat(String mod, Block planks, String wood, int meta) {
		return new Wood(mod + "_" + wood, planks, meta, true, Block.soundTypeWood, "block.fence_gate", MapColor.woodColor, null, true);
	}

	public static Block oldFence(int i) {
		return new McFlyFence(old(i));
	}

	public static Block oldGate(int i) {
		return new McFlyFenceGate(old(i));
	}

	public static Block modernFence(Block planks) {
		Wood[] woods = new Wood[WoodPlanks.WOODS.length];
		for (int i = 0; i < woods.length; i++) {
			woods[i] = modern(planks, i);
		}
		return new McFlyFence(woods);
	}

	public static Block modernGate(Block planks, int i) {
		return new McFlyFenceGate(modern(planks, i));
	}

	/** A mod's fence of all its woods (`bop_wood_fence`, `witchery_wood_fence`), on its planks by meta. */
	public static Block compatFence(String mod) {
		boolean bop = mod.equals("bop");
		Block planks = bop ? GameRegistry.findBlock("BiomesOPlenty", "planks") : GameRegistry.findBlock("witchery", "witchwood");
		String[] names = bop ? BOP : WITCHERY;
		Wood[] woods = new Wood[names.length];
		for (int i = 0; i < woods.length; i++) {
			woods[i] = compat(mod, planks, names[i], i);
		}
		return new McFlyFence(woods);
	}

	/** A mod's gate of one wood (`bop_sacredoak_fence_gate`), its planks' meta its wood's place in the list. */
	public static Block compatGate(String mod, int i) {
		boolean bop = mod.equals("bop");
		Block planks = bop ? GameRegistry.findBlock("BiomesOPlenty", "planks") : GameRegistry.findBlock("witchery", "witchwood");
		return new McFlyFenceGate(compat(mod, planks, (bop ? BOP : WITCHERY)[i], i));
	}

	public static int bopWoods() {
		return BOP.length;
	}

	public static String bopWood(int i) {
		return BOP[i];
	}

	public static String witcheryWood(int i) {
		return WITCHERY[i];
	}

	// ---------------------------------------------------------------- recipes, ore names, vanilla's

	/** 26.3's: a fence 3 of planks round sticks, a gate 1 of sticks round planks. */
	private static void recipes(ItemStack fence, ItemStack gate, ItemStack planks) {
		if (fence != null) {
			McFlyRecipes.shaped(fence, "W#W", "W#W", 'W', planks, '#', "stickWood");
			OreDictionary.registerOre("fenceWood", new ItemStack(fence.getItem(), 1, fence.getItemDamage()));
		}
		if (gate != null) {
			McFlyRecipes.shaped(gate, "#W#", "#W#", 'W', planks, '#', "stickWood");
			OreDictionary.registerOre("fenceGateWood", new ItemStack(gate.getItem(), 1, gate.getItemDamage()));
		}
	}

	public static void register() {
		McFlyBlocks[] oldFences = {McFlyBlocks.FENCE_SPRUCE, McFlyBlocks.FENCE_BIRCH, McFlyBlocks.FENCE_JUNGLE, McFlyBlocks.FENCE_ACACIA, McFlyBlocks.FENCE_DARK_OAK};
		McFlyBlocks[] oldGates = {McFlyBlocks.FENCE_GATE_SPRUCE, McFlyBlocks.FENCE_GATE_BIRCH, McFlyBlocks.FENCE_GATE_JUNGLE, McFlyBlocks.FENCE_GATE_ACACIA,
			McFlyBlocks.FENCE_GATE_DARK_OAK};
		McFlyBlocks[] newGates = {McFlyBlocks.CRIMSON_FENCE_GATE, McFlyBlocks.WARPED_FENCE_GATE, McFlyBlocks.MANGROVE_FENCE_GATE, McFlyBlocks.CHERRY_FENCE_GATE,
			McFlyBlocks.BAMBOO_FENCE_GATE};
		for (int i = 0; i < 5; i++) {
			ItemStack planks = new ItemStack(Blocks.planks, 1, i + 1);
			recipes(oldFences[i].isEnabled() ? oldFences[i].newItemStack(3) : null, oldGates[i].isEnabled() ? oldGates[i].newItemStack() : null, planks);
			if (McFlyBlocks.WOOD_PLANKS.isEnabled() && WoodPlanks.enabled(i)) {
				recipes(McFlyBlocks.WOOD_FENCE.isEnabled() ? McFlyBlocks.WOOD_FENCE.newItemStack(3, i) : null, newGates[i].isEnabled() ? newGates[i].newItemStack() : null,
					McFlyBlocks.WOOD_PLANKS.newItemStack(1, i));
			}
		}
		// vanilla's oak fence: 26.3's, of oak planks and of any other plankWood (1.7.10's mods' planks); its gate is
		// 1.7.10's own recipe already (planks round sticks, any planks); both burn (26.3's 5 and 20)
		if (McFlyBlocks.FENCE_SPRUCE.isEnabled()) {
			removeFirstRecipeFor(Blocks.fence);
			removeFirstRecipeFor(Blocks.nether_brick_fence);
			GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(Blocks.fence, 3), "W#W", "W#W", 'W', "plankWood", '#', "stickWood"));
			GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(Blocks.nether_brick_fence, 6), "W#W", "W#W", 'W', Blocks.nether_brick, '#',
				"ingotBrickNether"));
		}
		Blocks.fire.setFireInfo(Blocks.fence, 5, 20);
		Blocks.fire.setFireInfo(Blocks.fence_gate, 5, 20);
		OreDictionary.registerOre("fenceWood", new ItemStack(Blocks.fence));
		OreDictionary.registerOre("fenceGateWood", new ItemStack(Blocks.fence_gate));
	}

	/** The first recipe making the block (1.7.10's own), gone. */
	@SuppressWarnings("unchecked")
	private static void removeFirstRecipeFor(Block block) {
		net.minecraft.item.Item item = net.minecraft.item.Item.getItemFromBlock(block);
		java.util.Iterator<net.minecraft.item.crafting.IRecipe> recipes = net.minecraft.item.crafting.CraftingManager.getInstance().getRecipeList().iterator();
		while (recipes.hasNext()) {
			ItemStack made = recipes.next().getRecipeOutput();
			if (made != null && made.getItem() == item) {
				recipes.remove();
				return;
			}
		}
	}
}
