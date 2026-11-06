package io.github.asgerjon.mcfly.wood;

import cpw.mods.fml.common.registry.GameRegistry;
import io.github.asgerjon.mcfly.base.McFlySign;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import io.github.asgerjon.mcfly.registry.McFlyRecipes;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The wood sets' signs (SPEC-modern-movement.md 91.8, step F), in EFR's ids: the vanilla woods' (`sign_spruce`,
 * `wall_sign_spruce`, the item `item_sign_spruce`), the planks' woods' (`crimson_sign`, its item block, and
 * `crimson_wall_sign`), Biomes O' Plenty's and Witchery's (as the new woods). Made in pairs; 26.3's recipes, `signWood`;
 * vanilla's oak sign's stacks of 16 (1.7.10's already) and 26.3's icon.
 */
public final class WoodSigns {

	private WoodSigns() {
	}

	private static final String[] OLD = {"spruce", "birch", "jungle", "acacia", "dark_oak"};
	private static final MapColor[] OLD_COLOUR = {MapColor.obsidianColor, MapColor.sandColor, MapColor.dirtColor, MapColor.adobeColor, MapColor.brownColor};
	private static final MapColor[] NEW_COLOUR = {MapColor.netherrackColor, MapColor.cyanColor, MapColor.redColor, MapColor.clothColor, MapColor.yellowColor};
	private static final Map<String, McFlySign[]> PAIRS = new HashMap<>();

	private static McFlySign[] made(String key, java.util.function.Supplier<McFlySign[]> make) {
		return PAIRS.computeIfAbsent(key, k -> make.get());
	}

	/** A vanilla wood's sign (spruce 0 to dark oak 4), standing or on a wall; its item `item_sign_<wood>`. */
	public static Block oldSign(int i, boolean standing) {
		McFlySign[] pair = made("old" + i, () -> {
			String name = OLD[i] + "_sign";
			McFlySign up = new McFlySign(name, true, "minecraft", OLD[i], Block.soundTypeWood, OLD_COLOUR[i], () -> Blocks.planks, i + 1);
			McFlySign wall = new McFlySign(name, false, "minecraft", OLD[i], Block.soundTypeWood, OLD_COLOUR[i], () -> Blocks.planks, i + 1);
			McFlySign.pair(up, wall, () -> McFlyItems.valueOf("ITEM_SIGN_" + OLD[i].toUpperCase()).get());
			return new McFlySign[]{up, wall};
		});
		return standing ? pair[0] : pair[1];
	}

	/** The item of a vanilla wood's sign. */
	public static Item oldItem(int i) {
		return new SignItem((McFlySign) oldSign(i, true));
	}

	/** A planks' wood's sign (crimson 0 to bamboo 4); its item the standing sign's. */
	public static Block newSign(int i, boolean standing) {
		McFlySign[] pair = made("new" + i, () -> {
			String name = WoodPlanks.WOODS[i] + "_sign";
			McFlySign up = new McFlySign(name, true, "minecraft", WoodPlanks.WOODS[i], WoodPlanks.sound(i), NEW_COLOUR[i], () -> McFlyBlocks.WOOD_PLANKS.get(), i);
			McFlySign wall = new McFlySign(name, false, "minecraft", WoodPlanks.WOODS[i], WoodPlanks.sound(i), NEW_COLOUR[i], () -> McFlyBlocks.WOOD_PLANKS.get(), i);
			McFlySign.pair(up, wall, () -> Item.getItemFromBlock(up));
			// its item is the standing sign's item block, in McFly's tab
			up.setCreativeTab(McFlyTab.TAB);
			return new McFlySign[]{up, wall};
		});
		return standing ? pair[0] : pair[1];
	}

	/** A mod's sign (`bop_sacredoak_sign`) on its planks by meta; its textures the mod's domain's. */
	public static Block compatSign(String mod, String wood, int meta, boolean standing) {
		McFlySign[] pair = made(mod + wood, () -> {
			String name = mod + "_" + wood + "_sign";
			String domain = mod.equals("bop") ? "biomesoplenty" : "witchery";
			McFlySign up = new McFlySign(name, true, domain, wood, Block.soundTypeWood, MapColor.woodColor, () -> planks(mod), meta);
			McFlySign wall = new McFlySign(name, false, domain, wood, Block.soundTypeWood, MapColor.woodColor, () -> planks(mod), meta);
			McFlySign.pair(up, wall, () -> Item.getItemFromBlock(up));
			// its item is the standing sign's item block, in McFly's tab
			up.setCreativeTab(McFlyTab.TAB);
			return new McFlySign[]{up, wall};
		});
		return standing ? pair[0] : pair[1];
	}

	// ---------------------------------------------------------------- hanging signs (91.9)

	/** The 11 woods with hanging signs: the vanilla woods' six, then the planks' woods' (WoodPlanks' order from 6). */
	public static final String[] HANGING = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "crimson", "warped", "mangrove", "cherry", "bamboo"};
	private static final MapColor[] HANGING_COLOUR = {MapColor.woodColor, MapColor.obsidianColor, MapColor.sandColor, MapColor.dirtColor, MapColor.adobeColor,
		MapColor.brownColor, MapColor.netherrackColor, MapColor.cyanColor, MapColor.redColor, MapColor.redColor, MapColor.yellowColor};

	/** Whether a wood's hanging signs are on: the vanilla woods' with their signs, the planks' woods' with theirs and the wood. */
	public static boolean hangingEnabled(int i) {
		return i < 6 ? io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaSigns
			: io.github.asgerjon.mcfly.config.McFlyConfig.enableNewSigns && WoodPlanks.enabled(i - 6);
	}

	/** 26.3's `WoodType.hangingSignSoundType`. */
	private static Block.SoundType hangingSound(int i) {
		switch (HANGING[i]) {
			case "cherry":
				return io.github.asgerjon.mcfly.sound.McFlySoundType.CHERRY_WOOD_HANGING_SIGN;
			case "bamboo":
				return io.github.asgerjon.mcfly.sound.McFlySoundType.BAMBOO_WOOD_HANGING_SIGN;
			case "crimson":
			case "warped":
				return io.github.asgerjon.mcfly.sound.McFlySoundType.NETHER_WOOD_HANGING_SIGN;
			default:
				return io.github.asgerjon.mcfly.sound.McFlySoundType.HANGING_SIGN;
		}
	}

	/** A wood's hanging sign, on a ceiling or between walls; its item the ceiling sign's, in McFly's tab. */
	public static Block hangingSign(int i, boolean ceiling) {
		Object[] pair = HANGING_PAIRS.computeIfAbsent(i, k -> {
			String name = HANGING[i] + "_hanging_sign";
			java.util.function.Supplier<Block> planks = i < 6 ? () -> Blocks.planks : () -> McFlyBlocks.WOOD_PLANKS.get();
			int planksMeta = i < 6 ? i : i - 6;
			io.github.asgerjon.mcfly.base.McFlyHangingSign up = new io.github.asgerjon.mcfly.base.McFlyHangingSign(name, true, HANGING[i], hangingSound(i),
				HANGING_COLOUR[i], planks, planksMeta);
			io.github.asgerjon.mcfly.base.McFlyHangingSign wall = new io.github.asgerjon.mcfly.base.McFlyHangingSign(name, false, HANGING[i], hangingSound(i),
				HANGING_COLOUR[i], planks, planksMeta);
			io.github.asgerjon.mcfly.base.McFlyHangingSign.pair(up, wall);
			up.setCreativeTab(McFlyTab.TAB);
			return new Object[]{up, wall};
		});
		return (Block) (ceiling ? pair[0] : pair[1]);
	}

	private static final Map<Integer, Object[]> HANGING_PAIRS = new HashMap<>();

	/** The log each wood's hanging sign is made of, stripped (26.3's recipes: stripped logs, stems, the bamboo block). */
	private static Object[] logOf(int i) {
		switch (HANGING[i]) {
			case "oak":
				return new Object[]{Blocks.log, 0};
			case "spruce":
				return new Object[]{Blocks.log, 1};
			case "birch":
				return new Object[]{Blocks.log, 2};
			case "jungle":
				return new Object[]{Blocks.log, 3};
			case "acacia":
				return new Object[]{Blocks.log2, 0};
			case "dark_oak":
				return new Object[]{Blocks.log2, 1};
			case "crimson":
				return new Object[]{McFlyBlocks.CRIMSON_STEM.get(), 0};
			case "warped":
				return new Object[]{McFlyBlocks.WARPED_STEM.get(), 0};
			case "mangrove":
				return new Object[]{McFlyBlocks.MANGROVE_LOG.get(), 0};
			case "cherry":
				return new Object[]{McFlyBlocks.CHERRY_LOG.get(), 0};
			default:
				return new Object[]{McFlyBlocks.BAMBOO_BLOCK.get(), 0};
		}
	}

	/** A wood's stripped log as {block, meta} (`Stripping`'s table: after its `register`), or null. */
	public static Object[] strippedLogOf(int i) {
		Object[] log = logOf(i);
		return log[0] == null ? null : Stripping.stripped((Block) log[0], (Integer) log[1]);
	}

	/** 26.3's: two chains over six stripped logs make 6 (after `Stripping.register`, whose table gives the stripped log). */
	private static void hangingRecipes() {
		if (!McFlyBlocks.CHAIN.isEnabled()) {
			return;
		}
		for (int i = 0; i < HANGING.length; i++) {
			McFlyBlocks sign = McFlyBlocks.valueOf(HANGING[i].toUpperCase() + "_HANGING_SIGN");
			Object[] stripped = strippedLogOf(i);
			if (sign.isEnabled() && stripped != null) {
				McFlyRecipes.shaped(sign.newItemStack(6), "X X", "###", "###", 'X', McFlyBlocks.CHAIN.newItemStack(), '#',
					new ItemStack((Block) stripped[0], 1, (Integer) stripped[1]));
			}
		}
	}

	/** A mod's planks, or oak's while the mod isn't loaded (its entries are off then). */
	private static Block planks(String mod) {
		Block planks = mod.equals("bop") ? GameRegistry.findBlock("BiomesOPlenty", "planks") : GameRegistry.findBlock("witchery", "witchwood");
		return planks != null ? planks : Blocks.planks;
	}

	// ---------------------------------------------------------------- recipes, ore names, vanilla's

	public static void register() {
		for (int i = 0; i < 5; i++) {
			McFlyItems old = McFlyItems.valueOf("ITEM_SIGN_" + OLD[i].toUpperCase());
			if (old.isEnabled()) {
				recipe(old.newItemStack(3), new ItemStack(Blocks.planks, 1, i + 1));
				OreDictionary.registerOre("signWood", old.newItemStack(1));
			}
			McFlyBlocks sign = McFlyBlocks.valueOf(WoodPlanks.WOODS[i].toUpperCase() + "_SIGN");
			if (sign.isEnabled()) {
				if (McFlyBlocks.WOOD_PLANKS.isEnabled()) {
					recipe(sign.newItemStack(3), McFlyBlocks.WOOD_PLANKS.newItemStack(1, i));
				}
				OreDictionary.registerOre("signWood", sign.newItemStack(1));
			}
		}
		OreDictionary.registerOre("signWood", new ItemStack(Items.sign));
		hangingRecipes();
		// vanilla's oak sign: 26.3's icon; its recipe of oak planks and any plankWood no wood's own takes (after ours)
		Items.sign.setTextureName("oak_sign");
		removeVanillaRecipeFor(Items.sign);
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(Items.sign, 3), "###", "###", " X ", '#', "plankWood", 'X', "stickWood"));
	}

	/** 26.3's: six planks over a stick, three signs. */
	private static void recipe(ItemStack result, ItemStack planks) {
		McFlyRecipes.shaped(result, "###", "###", " X ", '#', planks, 'X', "stickWood");
	}

	@SuppressWarnings("unchecked")
	private static void removeVanillaRecipeFor(Item item) {
		Iterator<IRecipe> recipes = CraftingManager.getInstance().getRecipeList().iterator();
		while (recipes.hasNext()) {
			IRecipe recipe = recipes.next();
			ItemStack made = recipe.getRecipeOutput();
			if (recipe instanceof ShapedRecipes && made != null && made.getItem() == item) {
				recipes.remove();
				return;
			}
		}
	}
}
