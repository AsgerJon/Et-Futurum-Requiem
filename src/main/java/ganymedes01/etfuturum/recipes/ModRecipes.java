package ganymedes01.etfuturum.recipes;

import io.github.asgerjon.mcfly.dye.DyeColor;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import io.github.asgerjon.mcfly.registry.McFlyItems;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import cpw.mods.fml.common.registry.GameRegistry;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.api.DeepslateOreRegistry;
import ganymedes01.etfuturum.api.RawOreRegistry;
import ganymedes01.etfuturum.blocks.ores.BaseDeepslateOre;
import ganymedes01.etfuturum.blocks.ores.BaseSubtypesDeepslateOre;
import ganymedes01.etfuturum.blocks.ores.modded.BlockGeneralModdedDeepslateOre;
import ganymedes01.etfuturum.blocks.rawore.modded.BlockGeneralModdedRawOre;
import ganymedes01.etfuturum.compat.ExternalContent;
import ganymedes01.etfuturum.compat.ModsList;

import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.configuration.configs.ConfigExperiments;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.configuration.configs.ConfigModCompat;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.items.ItemSuspiciousStew;
import ganymedes01.etfuturum.items.rawore.modded.ItemGeneralModdedRawOre;
import ganymedes01.etfuturum.lib.Reference;

import ganymedes01.etfuturum.recipes.crafting.RecipeFixedFireworks;
import ganymedes01.etfuturum.recipes.crafting.ShapedEtFuturumRecipe;
import ganymedes01.etfuturum.recipes.crafting.ShapelessEtFuturumRecipe;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.RecipeFireworks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionHelper;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.RecipeSorter;
import net.minecraftforge.oredict.RecipeSorter.Category;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ModRecipes {

	public static final String[] ore_dyes = new String[]{"dyeBlack", "dyeRed", "dyeGreen", "dyeBrown", "dyeBlue", "dyePurple", "dyeCyan", "dyeLightGray", "dyeGray", "dyePink", "dyeLime", "dyeYellow", "dyeLightBlue", "dyeMagenta", "dyeOrange", "dyeWhite"};
	public static final String[] dye_names = new String[]{"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
	public static final String[] woodTypes = new String[]{"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};
	public static final String[] modernWoodTypes = new String[]{"crimson", "warped", "mangrove", "cherry"};
	private static final boolean[] modernWoodTypesEnabled = new boolean[5];

	public static void init() {
		modernWoodTypesEnabled[0] = ConfigExperiments.enableCrimsonBlocks;
		modernWoodTypesEnabled[1] = ConfigExperiments.enableWarpedBlocks;
		modernWoodTypesEnabled[2] = ConfigExperiments.enableMangroveBlocks;
		modernWoodTypesEnabled[3] = io.github.asgerjon.mcfly.config.McFlyConfig.enableCherryBlocks;
		modernWoodTypesEnabled[4] = io.github.asgerjon.mcfly.config.McFlyConfig.enableBambooBlocks;
		registerOreDictionary();
	}

	public static void postInit() {
		if (McFlyConfig.enableBanners) {
			RecipeSorter.register(Reference.MOD_ID + ".BannerDuplicateRecipe", io.github.asgerjon.mcfly.banner.BannerDuplicateRecipe.class, Category.SHAPELESS, "after:minecraft:shapeless");
			RecipeSorter.register(Reference.MOD_ID + ".RecipeShieldDecoration", ganymedes01.etfuturum.recipes.crafting.RecipeShieldDecoration.class, Category.SHAPELESS, "after:minecraft:shapeless");
		}
		RecipeSorter.register(Reference.MOD_ID + ":shaped", ShapedEtFuturumRecipe.class, RecipeSorter.Category.SHAPED, "before:minecraft:shaped");
		RecipeSorter.register(Reference.MOD_ID + ":shapeless", ShapelessEtFuturumRecipe.class, RecipeSorter.Category.SHAPELESS, "before:minecraft:shapeless");

		registerRecipes();
		tweakRecipes();
		registerLoot();
	}

	private static void registerLoot() {
		if (ModItems.BEETROOT_SEEDS.isEnabled()) {
			ChestGenHooks.addItem(ChestGenHooks.MINESHAFT_CORRIDOR, new WeightedRandomChestContent(ModItems.BEETROOT_SEEDS.newItemStack(), 1, 2, 5));
			ChestGenHooks.addItem(ChestGenHooks.PYRAMID_DESERT_CHEST, new WeightedRandomChestContent(ModItems.BEETROOT_SEEDS.newItemStack(), 1, 2, 5));
			ChestGenHooks.addItem(ChestGenHooks.PYRAMID_JUNGLE_CHEST, new WeightedRandomChestContent(ModItems.BEETROOT_SEEDS.newItemStack(), 1, 2, 5));
			ChestGenHooks.addItem(ChestGenHooks.STRONGHOLD_CORRIDOR, new WeightedRandomChestContent(ModItems.BEETROOT_SEEDS.newItemStack(), 1, 2, 5));
			ChestGenHooks.addItem(ChestGenHooks.STRONGHOLD_LIBRARY, new WeightedRandomChestContent(ModItems.BEETROOT_SEEDS.newItemStack(), 1, 2, 5));
			ChestGenHooks.addItem(ChestGenHooks.STRONGHOLD_CROSSING, new WeightedRandomChestContent(ModItems.BEETROOT_SEEDS.newItemStack(), 1, 2, 5));
			ChestGenHooks.addItem(ChestGenHooks.DUNGEON_CHEST, new WeightedRandomChestContent(ModItems.BEETROOT_SEEDS.newItemStack(), 1, 2, 5));
		}
	}

	@SuppressWarnings("unchecked")
	private static void tweakRecipes() {
		if (ConfigFunctions.enableExtraBurnableBlocks) {
			Blocks.fire.setFireInfo(Blocks.deadbush, 60, 100);
		}

		if (ModItems.RABBIT_FOOT.isEnabled()) {
			PotionHelper.potionRequirements.put(Potion.jump.getId(), "0 & 1 & !2 & 3");
			PotionHelper.potionAmplifiers.put(Potion.jump.getId(), "5");
			Potion.jump.liquidColor = 0x22FF4C;
		}

		if (ConfigFunctions.enableLangReplacements) {
			Blocks.wooden_button.setBlockName(Utils.getUnlocalisedName("oak_button"));
			Blocks.wooden_pressure_plate.setBlockName(Utils.getUnlocalisedName("oak_pressure_plate"));
		}

		if (ConfigFunctions.fireworkRecipeFixes) {
			for (int i = 0; i < CraftingManager.getInstance().getRecipeList().size(); i++) {
				Object recipe = CraftingManager.getInstance().getRecipeList().get(i);
				if (recipe != null && recipe.getClass() == RecipeFireworks.class) {
					CraftingManager.getInstance().getRecipeList().remove(i);
					CraftingManager.getInstance().getRecipeList().add(i, new RecipeFixedFireworks());
					break;
				}
			}
		}
	}

	private static void registerOreDictionary() {
		OreDictionary.registerOre("chestWood", new ItemStack(Blocks.chest));
		OreDictionary.registerOre("bookshelfWood", new ItemStack(Blocks.bookshelf));
		OreDictionary.registerOre("doorWood", new ItemStack(Items.wooden_door));
		OreDictionary.registerOre("trapdoorWood", Blocks.trapdoor);
		OreDictionary.registerOre("buttonWood", new ItemStack(Blocks.wooden_button));
		OreDictionary.registerOre("pressurePlateWood", new ItemStack(Blocks.wooden_pressure_plate));
		OreDictionary.registerOre("bedWood", new ItemStack(Items.bed));
		OreDictionary.registerOre("doorIron", new ItemStack(Items.iron_door));
		OreDictionary.registerOre("buttonStone", new ItemStack(Blocks.stone_button));
		OreDictionary.registerOre("pressurePlateStone", new ItemStack(Blocks.stone_pressure_plate));
		OreDictionary.registerOre("pressurePlateIron", new ItemStack(Blocks.heavy_weighted_pressure_plate));
		OreDictionary.registerOre("pressurePlateGold", new ItemStack(Blocks.light_weighted_pressure_plate));

		OreDictionary.registerOre("bowlWood", new ItemStack(Items.bowl));
		OreDictionary.registerOre("soulSand", Blocks.soul_sand);

		for (ModBlocks bed : ModBlocks.BEDS) {
			registerOre("bedWood", bed.get());
		}

		registerOre("treeLeaves", io.github.asgerjon.mcfly.registry.McFlyBlocks.LEAVES.newItemStack(1, OreDictionary.WILDCARD_VALUE));

		registerOre("soulSoil", io.github.asgerjon.mcfly.registry.McFlyBlocks.SOUL_SOIL.get());

		registerOre("nuggetIron", ModItems.NUGGET_IRON.get());

		registerOre("foodMuttonraw", ModItems.MUTTON_RAW.get());
		registerOre("listAllmeatraw", ModItems.MUTTON_RAW.get());
		registerOre("listAllmuttonraw", ModItems.MUTTON_RAW.get());

		registerOre("foodMuttoncooked", ModItems.MUTTON_COOKED.get());
		registerOre("listAllmeatcooked", ModItems.MUTTON_COOKED.get());
		registerOre("listAllmuttoncooked", ModItems.MUTTON_COOKED.get());

		registerOre("shardPrismarine", ModItems.PRISMARINE_SHARD.get());
		registerOre("crystalPrismarine", ModItems.PRISMARINE_CRYSTALS.get());
		registerOre("blockPrismarine", ModBlocks.PRISMARINE_BLOCK.newItemStack(1, OreDictionary.WILDCARD_VALUE));

		if (io.github.asgerjon.mcfly.config.McFlyConfig.enableMangrove) {
			registerOre("treeSapling", io.github.asgerjon.mcfly.registry.McFlyBlocks.SAPLING.newItemStack(1, 0));
		}
		if (io.github.asgerjon.mcfly.config.McFlyConfig.enableCherryTrees) {
			registerOre("treeSapling", io.github.asgerjon.mcfly.registry.McFlyBlocks.SAPLING.newItemStack(1, 1));
		}

		for (int i = 0; i < modernWoodTypesEnabled.length; i++) {
			if (modernWoodTypesEnabled[i]) {
			}
		}


		registerOre("blockSlime", ModBlocks.SLIME.newItemStack());


		registerOre("cropBeetroot", ModItems.BEETROOT.get());
		registerOre("listAllseed", ModItems.BEETROOT_SEEDS.get());
		registerOre("seedBeetroot", ModItems.BEETROOT_SEEDS.get());

		registerOre("foodRabbitraw", ModItems.RABBIT_RAW.newItemStack());
		registerOre("listAllmeatraw", ModItems.RABBIT_RAW.newItemStack());
		registerOre("listAllrabbitraw", ModItems.RABBIT_RAW.newItemStack());

		registerOre("foodRabbitcooked", ModItems.RABBIT_COOKED.newItemStack());
		registerOre("listAllmeatcooked", ModItems.RABBIT_COOKED.newItemStack());
		registerOre("listAllrabbitcooked", ModItems.RABBIT_COOKED.newItemStack());


		registerOre("ingotNetherite", McFlyItems.NETHERITE_INGOT.get());
		registerOre("scrapDebris", McFlyItems.NETHERITE_SCRAP.get());
		registerOre("oreDebris", McFlyBlocks.ANCIENT_DEBRIS.get());
		registerOre("blockNetherite", McFlyBlocks.NETHERITE_BLOCK.get());

		registerOre("dye", ModItems.DYE.newItemStack(1, OreDictionary.WILDCARD_VALUE));
		registerOre("dyeWhite", ModItems.DYE.newItemStack());
		registerOre("dyeBlue", ModItems.DYE.newItemStack(1, 1));
		registerOre("dyeBrown", ModItems.DYE.newItemStack(1, 2));
		registerOre("dyeBlack", ModItems.DYE.newItemStack(1, 3));

		registerOre("oreCopper", McFlyBlocks.COPPER_ORE.newItemStack());
		registerOre("ingotCopper", McFlyItems.COPPER_INGOT.newItemStack());
		registerOre("blockCopper", McFlyBlocks.COPPER_BLOCK.newItemStack());
		registerOre("blockCopperCut", McFlyBlocks.COPPER_BLOCK.newItemStack(1, 4));


		// tuff isn't 26.3's #stone_crafting_materials (spec §89, step E): no `stone` ore name on it, which made buttons, plates and repeaters of it

		registerOre("record", ModItems.PIGSTEP_RECORD.get());
		registerOre("record", ModItems.OTHERSIDE_RECORD.get());
		registerOre("record", ModItems.PRECIPICE_RECORD.get());
		registerOre("record", ModItems.CREATOR_MUSIC_BOX_RECORD.get());
		registerOre("record", ModItems.CREATOR_RECORD.get());
		registerOre("record", ModItems.TEARS_RECORD.get());
		registerOre("record", ModItems.LAVA_CHICKEN_RECORD.get());
		registerOre("record", ModItems.FIVE_RECORD.get());




		for (String waxString : new String[]{"materialWax", "materialWaxcomb", "materialHoneycomb", "itemBeeswax"}) {
			registerOre(waxString, ModItems.HONEYCOMB.get());
		}

		if (ConfigBlocksItems.enableColourfulBeacons && ModsList.TINKERS_CONSTRUCT.isLoaded()) { //TCon lacks proper tagging for their glass; let's add the right tags so beacons can see it
			Block glassBlock = GameRegistry.findBlock("TConstruct", "GlassBlock.StainedClear");
			Block glassPane = GameRegistry.findBlock("TConstruct", "GlassPaneClearStained");
			OreDictionary.registerOre("blockGlass", new ItemStack(glassBlock, 1, OreDictionary.WILDCARD_VALUE));
			OreDictionary.registerOre("paneGlass", new ItemStack(glassPane, 1, OreDictionary.WILDCARD_VALUE));
			for (int i = 0; i < ore_dyes.length; i++) {
				String capitalizedColor = ore_dyes[15 - i].substring(3);
				OreDictionary.registerOre("blockGlass" + capitalizedColor, new ItemStack(glassBlock, 1, i));
				OreDictionary.registerOre("paneGlass" + capitalizedColor, new ItemStack(glassPane, 1, i));
			}
		}

		registerOre("rawCopper", McFlyItems.RAW_ORE.newItemStack());
		registerOre("blockRawCopper", McFlyBlocks.RAW_ORE_BLOCK.newItemStack());
		registerOre("rawIron", McFlyItems.RAW_ORE.newItemStack(1, 1));
		registerOre("blockRawIron", McFlyBlocks.RAW_ORE_BLOCK.newItemStack(1, 1));
		registerOre("rawGold", McFlyItems.RAW_ORE.newItemStack(1, 2));
		registerOre("blockRawGold", McFlyBlocks.RAW_ORE_BLOCK.newItemStack(1, 2));

		if (ConfigFunctions.registerRawItemAsOre) {
			registerOre("oreCopper", McFlyItems.RAW_ORE.newItemStack()); //Todo: the registration of copper raw ore should be conditional because it is a configurable meta value, I may make the raw ore class itself provide enabled metas.
			registerOre("oreIron", McFlyItems.RAW_ORE.newItemStack(1, 1));
			registerOre("oreGold", McFlyItems.RAW_ORE.newItemStack(1, 2));
		}
	}

	private static void registerRecipes() {
		if (!ModsList.GTNH.isLoaded()) {
			addShapedRecipe(ModBlocks.OLD_GRAVEL.newItemStack(4), "xy", "yx", 'x', ModBlocks.COARSE_DIRT.get(), 'y', Blocks.gravel);
		}

		if (!ModsList.GTNH.isLoaded()) {
		}

		if (McFlyItems.SHIELD.isEnabled()) {
			addShapedRecipe(McFlyItems.SHIELD.newItemStack(), "WoW", "WWW", " W ", 'W', "plankWood", 'o', "ingotIron");
		}

		// the spyglass (spec §64.5): an amethyst shard over two copper ingots
		// the glow item frame (spec §67.4): shapeless, an item frame and a glow ink sac
		if (McFlyItems.GLOW_ITEM_FRAME.isEnabled()) {
			GameRegistry.addShapelessRecipe(McFlyItems.GLOW_ITEM_FRAME.newItemStack(), new ItemStack(Items.item_frame), McFlyItems.GLOW_INK_SAC.newItemStack());
		}

		if (McFlyItems.SPYGLASS.isEnabled()) {
			addShapedRecipe(McFlyItems.SPYGLASS.newItemStack(), "a", "c", "c", 'a', "gemAmethyst", 'c', "ingotCopper");
		}

		if (ConfigFunctions.enableRecipeForTotem) {
			addShapedRecipe(ModItems.TOTEM_OF_UNDYING.newItemStack(), "EBE", "GBG", " G ", 'E', "gemEmerald", 'G', "ingotGold", 'B', "blockGold");
		}

		if (!ModsList.GTNH.isLoaded()) {
			addShapedRecipe(ModBlocks.SLIME.newItemStack(), "xxx", "xxx", "xxx", 'x', new ItemStack(Items.slime_ball));
			addShapelessRecipe(new ItemStack(Items.slime_ball, 9), ModBlocks.SLIME.get());

			addShapedRecipe(ModBlocks.COARSE_DIRT.newItemStack(4), "xy", "yx", 'x', new ItemStack(Blocks.dirt), 'y', new ItemStack(Blocks.gravel));
		}

		addSmelting(ModItems.MUTTON_RAW.get(), ModItems.MUTTON_COOKED.newItemStack(), 0.35F);

		if (!ModsList.GTNH.isLoaded()) {
			addShapedRecipe(new ItemStack(Items.iron_ingot), "xxx", "xxx", "xxx", 'x', "nuggetIron");
			addShapelessRecipe(ModItems.NUGGET_IRON.newItemStack(9), "ingotIron");
		}

		addShapedRecipe(ModBlocks.PRISMARINE_BLOCK.newItemStack(), "xx", "xx", 'x', "shardPrismarine");
		addShapedRecipe(ModBlocks.PRISMARINE_BLOCK.newItemStack(1, 1), "xxx", "xxx", "xxx", 'x', "shardPrismarine");
		addShapedRecipe(ModBlocks.PRISMARINE_BLOCK.newItemStack(1, 2), "xxx", "xyx", "xxx", 'x', "shardPrismarine", 'y', "dyeBlack");
		addShapedRecipe(ModBlocks.SEA_LANTERN.newItemStack(), "xyx", "yyy", "xyx", 'x', "shardPrismarine", 'y', "crystalPrismarine");

		addShapedRecipe(ModBlocks.PRISMARINE_STAIRS.newItemStack(4), "x  ", "xx ", "xxx", 'x', ModBlocks.PRISMARINE_BLOCK.newItemStack());
		addShapedRecipe(ModBlocks.PRISMARINE_STAIRS_BRICK.newItemStack(4), "x  ", "xx ", "xxx", 'x', ModBlocks.PRISMARINE_BLOCK.newItemStack(1, 1));
		addShapedRecipe(ModBlocks.PRISMARINE_STAIRS_DARK.newItemStack(4), "x  ", "xx ", "xxx", 'x', ModBlocks.PRISMARINE_BLOCK.newItemStack(1, 2));
		addShapedRecipe(ModBlocks.PRISMARINE_WALL.newItemStack(6), "xxx", "xxx", 'x', ModBlocks.PRISMARINE_BLOCK.newItemStack());

		for (int i = 0; i < 3; i++) {
			addShapedRecipe(ModBlocks.PRISMARINE_SLAB.newItemStack(6, i), "xxx", 'x', ModBlocks.PRISMARINE_BLOCK.newItemStack(1, i));
		}

		if (!ModsList.BOTANIA.isLoaded()) {
			addShapedRecipe(ModItems.PRISMARINE_SHARD.newItemStack(4), "xy", "zx", 'x', "gemQuartz", 'y', "dyeBlue", 'z', "dyeGreen");
			addShapedRecipe(ModItems.PRISMARINE_CRYSTALS.newItemStack(4), "xy", "yx", 'x', "gemQuartz", 'y', "dustGlowstone");
		}

		if (!ModsList.GTNH.isLoaded()) {

		}


		addShapedRecipe(new ItemStack(Items.stick), "b", "b", 'b', io.github.asgerjon.mcfly.registry.McFlyItems.BAMBOO.newItemStack());








		if (io.github.asgerjon.mcfly.config.McFlyConfig.enableVanillaFences && !ModsList.GTNH.isLoaded()) {
		}

		// banners (spec §80): six wool of one colour and a stick, that colour's banner; copying a banner's layers. No
		// patterns in the grid: the loom adds them, as in 26.3
		for (DyeColor colour : DyeColor.values()) {
			addShapedRecipe(McFlyBlocks.BANNER.newItemStack(1, colour.id()), "xxx", "xxx", " y ", 'x', new ItemStack(Blocks.wool, 1, colour.id()), 'y', "stickWood");
		}
		GameRegistry.addRecipe(new io.github.asgerjon.mcfly.banner.BannerDuplicateRecipe());
		// a banner on a shield (26.3's ShieldDecorationRecipe, spec §66.9)
		if (McFlyItems.SHIELD.isEnabled()) {
			GameRegistry.addRecipe(new ganymedes01.etfuturum.recipes.crafting.RecipeShieldDecoration());
		}
		// 26.3's banner pattern items: paper and a thing, shapeless (spec §66.6)
		Object[][] patternItems = {{McFlyItems.FLOWER_BANNER_PATTERN, new ItemStack(Blocks.red_flower, 1, 8)},
			{McFlyItems.CREEPER_BANNER_PATTERN, new ItemStack(Items.skull, 1, 4)}, {McFlyItems.SKULL_BANNER_PATTERN, new ItemStack(Items.skull, 1, 1)},
			{McFlyItems.MOJANG_BANNER_PATTERN, new ItemStack(Items.golden_apple, 1, 1)},
			{McFlyItems.FIELD_MASONED_BANNER_PATTERN, new ItemStack(Blocks.brick_block)}, {McFlyItems.BORDURE_INDENTED_BANNER_PATTERN, new ItemStack(Blocks.vine)}};
		for (Object[] patternItem : patternItems) {
			if (((McFlyItems) patternItem[0]).isEnabled()) {
				GameRegistry.addShapelessRecipe(((McFlyItems) patternItem[0]).newItemStack(), new ItemStack(Items.paper), patternItem[1]);
			}
		}

		addShapedRecipe(ModItems.WOODEN_ARMORSTAND.newItemStack(), "xxx", " x ", "xyx", 'x', "stickWood", 'y', new ItemStack(Blocks.stone_slab));
		// the turtle shell, from five scutes (spec §69.13)
		if (McFlyItems.TURTLE_HELMET.isEnabled()) {
			addShapedRecipe(McFlyItems.TURTLE_HELMET.newItemStack(), "xxx", "x x", 'x', McFlyItems.TURTLE_SCUTE.newItemStack());
		}

		addShapedRecipe(ModItems.RABBIT_STEW.newItemStack(), " R ", "CPM", " B ", 'R', ModItems.RABBIT_COOKED.newItemStack(), 'C', Items.carrot, 'P', Items.baked_potato, 'M', Blocks.brown_mushroom, 'B', "bowlWood");
		addShapedRecipe(ModItems.RABBIT_STEW.newItemStack(), " R ", "CPD", " B ", 'R', ModItems.RABBIT_COOKED.newItemStack(), 'C', Items.carrot, 'P', Items.baked_potato, 'D', Blocks.red_mushroom, 'B', "bowlWood");
		addSmelting(ModItems.RABBIT_RAW.get(), ModItems.RABBIT_COOKED.newItemStack(), 0.35F);
		addShapedRecipe(new ItemStack(Items.leather), "xx", "xx", 'x', ModItems.RABBIT_HIDE.get());

		addSmelting(ModBlocks.SPONGE.newItemStack(1, 1), ConfigWorld.tileReplacementMode == -1 ? ModBlocks.SPONGE.newItemStack() : new ItemStack(Blocks.sponge), 0.15F);

		addShapedRecipe(ModItems.BEETROOT_SOUP.newItemStack(), "xxx", "xxx", " y ", 'x', "cropBeetroot", 'y', "bowlWood");
		GameRegistry.addRecipe(new ShapelessOreRecipe(new ItemStack(Items.dye, 1, 1), "cropBeetroot"));

		addSmelting(McFlyBlocks.KELP.get(), McFlyItems.DRIED_KELP.newItemStack(), 0.1F);
		addShapedRecipe(McFlyBlocks.DRIED_KELP_BLOCK.newItemStack(), "xxx", "xxx", "xxx", 'x', McFlyItems.DRIED_KELP.newItemStack());
		addShapelessRecipe(McFlyItems.DRIED_KELP.newItemStack(9), McFlyBlocks.DRIED_KELP_BLOCK.newItemStack());

		// the End's blocks' recipes are McFly's (spec §84, EndBlocks)


		addShapelessRecipe(new ItemStack(Items.dye, 1, 1), ModBlocks.ROSE.get());
		addShapedRecipe(new ItemStack(Blocks.double_plant, 1, 4), "xx", "xx", "xx", 'x', ModBlocks.ROSE.newItemStack());
		addShapedRecipe(ModBlocks.ROSE.newItemStack(12), "xx", 'x', new ItemStack(Blocks.double_plant, 1, 4));

		if (McFlyItems.TIPPED_ARROW.isEnabled() && McFlyItems.LINGERING_POTION.isEnabled()) {
			RecipeSorter.register(Reference.MOD_ID + ".io.github.asgerjon.mcfly.potion.TippedArrowRecipe", io.github.asgerjon.mcfly.potion.TippedArrowRecipe.class, Category.SHAPED, "after:minecraft:shaped");
			GameRegistry.addRecipe(new io.github.asgerjon.mcfly.potion.TippedArrowRecipe(McFlyItems.TIPPED_ARROW.newItemStack(), "xxx", "xyx", "xxx", 'x', Items.arrow, 'y', McFlyItems.LINGERING_POTION.newItemStack(1, OreDictionary.WILDCARD_VALUE)));
		}

		if (!ModsList.GTNH.isLoaded()) {
			for (int i = 0; i < ModBlocks.BEDS.length; i++) {
				int j = i == 14 ? 15 : i;
				addShapedRecipe(ModBlocks.BEDS[i].newItemStack(1), "###", "XXX", '#', new ItemStack(Blocks.wool, 1, j), 'X', "plankWood");
				if (i > 0) {
					addShapelessRecipe(ModBlocks.BEDS[i].newItemStack(1), ModBlocks.BEDS[0].get(), ore_dyes[~j & 15]);
				}
			}
			addShapelessRecipe(new ItemStack(Items.bed, 1), ModBlocks.BEDS[0].newItemStack(), ore_dyes[1]);

			addShapedRecipe(ModBlocks.MAGMA.newItemStack(), "xx", "xx", 'x', new ItemStack(Items.magma_cream));
		}

		if (!ModsList.GTNH.isLoaded()) {
			addShapedRecipe(io.github.asgerjon.mcfly.registry.McFlyBlocks.NETHER_WART.newItemStack(), "xxx", "xxx", "xxx", 'x', Items.nether_wart);
			addShapedRecipe(ModBlocks.BONE.newItemStack(), "xxx", "xxx", "xxx", 'x', new ItemStack(Items.dye, 1, 15));
		}
		addShapelessRecipe(new ItemStack(Items.dye, 9, 15), ModBlocks.BONE.newItemStack());

		if (!ModsList.GTNH.isLoaded()) {
			for (int i = 0; i < ore_dyes.length; i++) {
				int dye = ~i & 15;
				addShapelessRecipe(ModBlocks.CONCRETE_POWDER.newItemStack(8, i),
						ore_dyes[dye], new ItemStack(Blocks.sand, 1, 0), new ItemStack(Blocks.sand, 1, 0), new ItemStack(Blocks.sand, 1, 0),
						new ItemStack(Blocks.sand, 1, 0), Blocks.gravel, Blocks.gravel, Blocks.gravel, Blocks.gravel);
				addSmelting(new ItemStack(Blocks.stained_hardened_clay, 1, i), ModBlocks.TERRACOTTA[i].newItemStack(), 0.1F);
			}
		}


		if (!ModsList.GTNH.isLoaded()) {
			addShapedRecipe(ModBlocks.BARREL.newItemStack(), "xsx", "x x", "xsx", 'x', "plankWood", 's', "slabWood");
			addShapedRecipe(ModBlocks.IRON_BARREL.newItemStack(), "xxx", "xsx", "xxx", 'x', "ingotIron", 's', ModBlocks.BARREL.get());
			addShapedRecipe(ModBlocks.GOLD_BARREL.newItemStack(), "xxx", "xsx", "xxx", 'x', "ingotGold", 's', ModBlocks.IRON_BARREL.get());
			addShapedRecipe(ModBlocks.DIAMOND_BARREL.newItemStack(), "ggg", "xsx", "ggg", 'x', "gemDiamond", 's', ModBlocks.GOLD_BARREL.get(), 'g', "blockGlassColorless");
			addShapedRecipe(ModBlocks.COPPER_BARREL.newItemStack(), "xxx", "xsx", "xxx", 'x', "ingotCopper", 's', ModBlocks.BARREL.get());
			addShapedRecipe(ModBlocks.SILVER_BARREL.newItemStack(), "xxx", "xsx", "xxx", 'x', "ingotSilver", 's', ModBlocks.COPPER_BARREL.get());
			addShapedRecipe(ModBlocks.STEEL_BARREL.newItemStack(), "xxx", "xsx", "xxx", 'x', "ingotSteel", 's', ModBlocks.COPPER_BARREL.get());
			addShapedRecipe(ModBlocks.OBSIDIAN_BARREL.newItemStack(), "xxx", "xsx", "xxx", 'x', Blocks.obsidian, 's', ModBlocks.DIAMOND_BARREL.get());
			addShapedRecipe(ModBlocks.DARKSTEEL_BARREL.newItemStack(), "xxx", "xsx", "xxx", 'x', "ingotDarkSteel", 's', ModBlocks.STEEL_BARREL.get());
			addShapedRecipe(ModBlocks.NETHERITE_BARREL.newItemStack(), "xxx", "nsn", "xxx", 'x', Blocks.obsidian, 's', ModBlocks.OBSIDIAN_BARREL.get(), 'n', io.github.asgerjon.mcfly.registry.McFlyItems.NETHERITE_INGOT.get());

			addShapedRecipe(ModItems.BARREL_UPGRADE.newItemStack(), "XXX", "XYX", "XXX", 'X', "ingotIron", 'Y', "slabWood");
			addShapedRecipe(ModItems.BARREL_UPGRADE.newItemStack(1, 1), "XXX", "XYX", "XXX", 'X', "ingotCopper", 'Y', "slabWood");
			addShapedRecipe(ModItems.BARREL_UPGRADE.newItemStack(1, 2), "XXX", "XXX", "XYX", 'X', "ingotGold", 'Y', "ingotIron");
			addShapedRecipe(ModItems.BARREL_UPGRADE.newItemStack(1, 3), "XGG", "GYG", "GGX", 'X', "gemDiamond", 'Y', "ingotGold", 'G', "blockGlassColorless");
			addShapedRecipe(ModItems.BARREL_UPGRADE.newItemStack(1, 4), "XXX", "XXX", "XYX", 'X', "ingotGold", 'Y', "ingotSteel");
			addShapedRecipe(ModItems.BARREL_UPGRADE.newItemStack(1, 5), "XXX", "XXX", "XYX", 'X', Blocks.obsidian, 'Y', "gemDiamond");
			addShapedRecipe(ModItems.BARREL_UPGRADE.newItemStack(1, 6), "XXX", "XXX", "XYX", 'X', io.github.asgerjon.mcfly.registry.McFlyItems.NETHERITE_INGOT.get(), 'Y', Blocks.obsidian);
			addShapedRecipe(ModItems.BARREL_UPGRADE.newItemStack(1, 7), "XXX", "XXX", "XYX", 'X', "ingotDarkSteel", 'Y', Blocks.obsidian);		
			addShapedRecipe(ModItems.BARREL_UPGRADE.newItemStack(1, 8), "XGX", "XYX", "GGG", 'X', "ingotIron", 'Y', "ingotCopper", 'G', "blockGlassColorless");
			addShapedRecipe(ModItems.BARREL_UPGRADE.newItemStack(1, 9), "XXX", "XXX", "XYX", 'X', "ingotSilver", 'Y', "ingotCopper");
			addShapedRecipe(ModItems.BARREL_UPGRADE.newItemStack(1, 10), "XGX", "GGG", "XYX", 'X', "ingotGold", 'Y', "ingotSilver", 'G', "blockGlassColorless");

			addShapedRecipe(ModBlocks.BLUE_ICE.newItemStack(), "xxx", "xxx", "xxx", 'x', Blocks.packed_ice);
		}

		if (!ModsList.GTNH.isLoaded() && ModItems.COOPERS_MALLET.isEnabled()) {
			addShapedRecipe(ModItems.COOPERS_MALLET.newItemStack(), "cbr", " s ", " s ", 'c', Blocks.chest, 'b', "blockCopper", 'r', ModBlocks.BARREL.get(), 's', "stickWood");
		}

		addShapedRecipe(ModBlocks.SMOKER.newItemStack(), " l ", "lxl", " l ", 'x', Blocks.furnace, 'l', "logWood");

		addShapedRecipe(ModBlocks.BLAST_FURNACE.newItemStack(), "iii", "ixi", "sss", 'x', Blocks.furnace, 'i', "ingotIron", 's', io.github.asgerjon.mcfly.registry.McFlyBlocks.SMOOTH_STONE.isEnabled() ? io.github.asgerjon.mcfly.registry.McFlyBlocks.SMOOTH_STONE.get() : Blocks.stone);

		if (!ModsList.GTNH.isLoaded()) {
			// smooth stone is McFly's (spec §89, step D): the circuit plates of BluePower and Project Red come of it, not stone
			if (io.github.asgerjon.mcfly.registry.McFlyBlocks.SMOOTH_STONE.isEnabled()) {
				ItemStack stoneTile = ExternalContent.Items.BLUEPOWER_CIRCUIT_PLATE.isEnabled() ? ExternalContent.Items.BLUEPOWER_CIRCUIT_PLATE.newItemStack(2)
						: ExternalContent.Items.PROJECTRED_CIRCUIT_PLATE.isEnabled() ? ExternalContent.Items.PROJECTRED_CIRCUIT_PLATE.newItemStack(2)
						: null;
				if (stoneTile != null) {
					removeFurnaceRecipeFor(new ItemStack(Blocks.stone), stoneTile);
					addShapedRecipe(stoneTile, "x", 'x', io.github.asgerjon.mcfly.registry.McFlyBlocks.SMOOTH_STONE.newItemStack());
				}
			}
		}

		addShapelessRecipe(ModItems.DYE.newItemStack(), ModBlocks.LILY_OF_THE_VALLEY.newItemStack());
		addShapelessRecipe(ModItems.DYE.newItemStack(1, 1), ModBlocks.CORNFLOWER.newItemStack());
		addShapelessRecipe(ModItems.DYE.newItemStack(1, 3), ModBlocks.WITHER_ROSE.newItemStack());
		addShapelessRecipe(ModItems.DYE.newItemStack(), new ItemStack(Items.dye, 1, 15));
		addShapelessRecipe(ModItems.DYE.newItemStack(1, 1), new ItemStack(Items.dye, 1, 4));
		addShapelessRecipe(ModItems.DYE.newItemStack(1, 2), new ItemStack(Items.dye, 1, 3));
		addShapelessRecipe(ModItems.DYE.newItemStack(1, 3), new ItemStack(Items.dye, 1, 0));

		// copper's, the lightning rod's and the raw ores' recipes are McFly's (spec §87): `CopperRecipes`



		// tuff's recipes are McFly's (spec §89, step E): `StoneRecipes`

		// Mud Recipes

		// VANILLA

		if (!ModsList.GTNH.isLoaded()) {
		}


		for (int i = 0; i < getStewFlowers().size(); i++) {
			ItemStack stew = ModItems.SUSPICIOUS_STEW.newItemStack();

			PotionEffect effect = EtFuturum.getSuspiciousStewEffect(getStewFlowers().get(i));

			// the stew's effect by name (spec §70)
			ItemSuspiciousStew.addEffect(stew, Potion.potionTypes[effect.getPotionID()], effect.getDuration());
			addShapelessRecipe(stew, Blocks.red_mushroom, Blocks.brown_mushroom, Items.bowl, getStewFlowers().get(i));
		}

		if (!ModsList.GTNH.isLoaded()) {
			addShapedRecipe(McFlyBlocks.SHULKER_BOX.newItemStack(), "x", "c", "x", 'x', McFlyItems.SHULKER_SHELL.newItemStack(), 'c', new ItemStack(Blocks.chest));
		}
		// dyeing a shulker box (spec §81): any box and a dye, contents kept
		if (McFlyBlocks.SHULKER_BOX.isEnabled()) {
			GameRegistry.addRecipe(new io.github.asgerjon.mcfly.shulkerbox.ShulkerBoxDyeRecipe());
		}

		if (ConfigWorld.tileReplacementMode == -1) {
			//We keep the original enabled checks inside of the booleans and use the original addShapedRecipe function because we need to check it anyways for recipe removal
			if (ModBlocks.ANVIL.isEnabled()) {
				GameRegistry.addShapedRecipe(ModBlocks.ANVIL.newItemStack(), "BBB", " I ", "III", 'I', new ItemStack(Items.iron_ingot), 'B', new ItemStack(Blocks.iron_block));
				removeFirstRecipeFor(Blocks.anvil);
			}

			if (ModBlocks.BREWING_STAND.isEnabled()) {
				GameRegistry.addShapedRecipe(ModBlocks.BREWING_STAND.newItemStack(), " B ", "CCC", 'C', new ItemStack(Blocks.cobblestone), 'B', new ItemStack(Items.blaze_rod));
				removeFirstRecipeFor(Blocks.brewing_stand);
			}

			if (ModBlocks.BEACON.isEnabled()) {
				GameRegistry.addShapedRecipe(ModBlocks.BEACON.newItemStack(), "GGG", "GNG", "OOO", 'G', new ItemStack(Blocks.glass), 'N', new ItemStack(Items.nether_star), 'O', new ItemStack(Blocks.obsidian));
				removeFirstRecipeFor(Blocks.beacon);
			}

			if (ModBlocks.ENCHANTMENT_TABLE.isEnabled()) {
				GameRegistry.addRecipe(new ShapedOreRecipe(ModBlocks.ENCHANTMENT_TABLE.newItemStack(), " B ", "D#D", "###", '#', Blocks.obsidian, 'B', Items.book, 'D', "gemDiamond"));
				removeFirstRecipeFor(Blocks.enchanting_table);
			}

			if (ModBlocks.SPONGE.isEnabled()) {
				addShapelessRecipe(ModBlocks.SPONGE.newItemStack(), Blocks.sponge);
				addShapelessRecipe(new ItemStack(Blocks.sponge), ModBlocks.SPONGE.get()); //For recipes that want the vanilla sponge you can convert it
			}
		}


		// 26.3's: an iron ingot where 1.16 to 1.21.4 had a netherite ingot
		addShapedRecipe(McFlyBlocks.LODESTONE.newItemStack(), "SSS", "S#S", "SSS", 'S', new ItemStack(Blocks.stonebrick, 1, 3), '#', "ingotIron");

		addShapedRecipe(ModBlocks.FLETCHING_TABLE.newItemStack(), "FF", "PP", "PP", 'P', "plankWood", 'F', new ItemStack(Items.flint, 1, 0));

		addShapedRecipe(ModBlocks.STONECUTTER.newItemStack(), " I ", "SSS", 'S', "stone", 'I', "ingotIron");

		if (!ModsList.GTNH.isLoaded()) {
			addShapedRecipe(ModBlocks.COMPOSTER.newItemStack(), "S S", "S S", "SSS", 'S', "slabWood");
		}

		addShapedRecipe(ModBlocks.CARTOGRAPHY_TABLE.newItemStack(), "pp", "PP", "PP", 'P', "plankWood", 'p', new ItemStack(Items.paper, 1, 0));

		addShapedRecipe(ModBlocks.LOOM.newItemStack(), "SS", "PP", 'P', "plankWood", 'S', new ItemStack(Items.string, 1, 0));

		// amethyst's recipes and ore names are McFly's (spec §90.4): `CaveRecipes`

		if (!ModsList.GTNH.isLoaded()) {

		}

		addShapedRecipe(ModBlocks.HONEY_BLOCK.newItemStack(), "HH", "HH", 'H', ModItems.HONEY_BOTTLE.get());
		addShapedRecipe(ModBlocks.HONEYCOMB_BLOCK.newItemStack(), "HH", "HH", 'H', "materialHoneycomb");
		addShapedRecipe(ModBlocks.BEEHIVE.newItemStack(), "WWW", "HHH", "WWW", 'W', "plankWood", 'H', "materialHoneycomb");


		addShapedRecipe(ModItems.FIVE_RECORD.newItemStack(), "xxx", "xxx", "xxx", 'x', ModItems.DISC_FRAGMENT_5.get());

		if (ModsList.LOTR.isLoaded()) {//LoTR ores and ingots stupidly lack dictionary entries; let's add them so the code below can find them.
			if (ConfigModCompat.moddedRawOres) {
				registerOre("oreCopper", GameRegistry.findBlock("lotr", "tile.oreCopper"));
				registerOre("ingotCopper", GameRegistry.findItem("lotr", "item.copper"));

				registerOre("oreTin", GameRegistry.findBlock("lotr", "tile.oreTin"));
				registerOre("ingotTin", GameRegistry.findItem("lotr", "item.tin"));
				registerOre("oreSilver", GameRegistry.findBlock("lotr", "tile.oreSilver"));
				registerOre("ingotSilver", GameRegistry.findItem("lotr", "item.silver"));
				registerOre("oreMithril", GameRegistry.findBlock("lotr", "tile.oreMithril"));
				registerOre("ingotMithril", GameRegistry.findItem("lotr", "item.mithril"));
			}
		}

		if (ModsList.BIG_REACTORS.isLoaded()) {
			if (ConfigModCompat.moddedRawOres) {
				registerOre("oreUranium", ExternalContent.Blocks.BR_YELLORITE_ORE.get());
			}
		}

		addShapelessRecipe(new ItemStack(Items.dye, 1, 9), ModBlocks.PINK_PETALS.get());


		if (ConfigModCompat.moddedDeepslateOres) { //OreDict-based registration is only used when mod support is enabled
			DeepslateOreRegistry.addOreByOreDict("oreCoal", io.github.asgerjon.mcfly.registry.McFlyBlocks.DEEPSLATE_COAL_ORE.get());
			DeepslateOreRegistry.addOreByOreDict("oreLapis", io.github.asgerjon.mcfly.registry.McFlyBlocks.DEEPSLATE_LAPIS_ORE.get());
			DeepslateOreRegistry.addOreByOreDict("oreDiamond", io.github.asgerjon.mcfly.registry.McFlyBlocks.DEEPSLATE_DIAMOND_ORE.get());
			DeepslateOreRegistry.addOreByOreDict("oreEmerald", io.github.asgerjon.mcfly.registry.McFlyBlocks.DEEPSLATE_EMERALD_ORE.get());
			DeepslateOreRegistry.addOreByOreDict("oreCopper", McFlyBlocks.DEEPSLATE_COPPER_ORE.get());
			DeepslateOreRegistry.addOreByOreDict("oreIron", io.github.asgerjon.mcfly.registry.McFlyBlocks.DEEPSLATE_IRON_ORE.get());
			DeepslateOreRegistry.addOreByOreDict("oreGold", io.github.asgerjon.mcfly.registry.McFlyBlocks.DEEPSLATE_GOLD_ORE.get());
			DeepslateOreRegistry.addOreByOreDict("oreRedstone", io.github.asgerjon.mcfly.registry.McFlyBlocks.DEEPSLATE_REDSTONE_ORE.get());
		}

		addTagsAndDeepslate("Coal", io.github.asgerjon.mcfly.registry.McFlyBlocks.DEEPSLATE_COAL_ORE.newItemStack());
		addTagsAndDeepslate("Lapis", io.github.asgerjon.mcfly.registry.McFlyBlocks.DEEPSLATE_LAPIS_ORE.newItemStack());
		addTagsAndDeepslate("Diamond", io.github.asgerjon.mcfly.registry.McFlyBlocks.DEEPSLATE_DIAMOND_ORE.newItemStack());
		addTagsAndDeepslate("Emerald", io.github.asgerjon.mcfly.registry.McFlyBlocks.DEEPSLATE_EMERALD_ORE.newItemStack());
		addTagsAndDeepslate("Copper", McFlyBlocks.DEEPSLATE_COPPER_ORE.newItemStack());
		// copper ore to its deepslate one in the deepslate layer (EFR's old ore class did it in its constructor)
		if (McFlyBlocks.COPPER_ORE.isEnabled() && McFlyBlocks.DEEPSLATE_COPPER_ORE.isEnabled()) {
			DeepslateOreRegistry.addOre(McFlyBlocks.COPPER_ORE.get(), McFlyBlocks.DEEPSLATE_COPPER_ORE.get());
		}
		addTagsAndDeepslate("Iron", io.github.asgerjon.mcfly.registry.McFlyBlocks.DEEPSLATE_IRON_ORE.newItemStack());
		addTagsAndDeepslate("Gold", io.github.asgerjon.mcfly.registry.McFlyBlocks.DEEPSLATE_GOLD_ORE.newItemStack());
		addTagsAndDeepslate("Redstone", io.github.asgerjon.mcfly.registry.McFlyBlocks.DEEPSLATE_REDSTONE_ORE.newItemStack());

		registerGeneralDeepslateOres();
		registerModSupportDeepslateOres();

		registerGeneralRawOres();
		registerModSupportRawOres();
	}

	public static void registerGeneralDeepslateOres() {
		//Insert alternate Mythril spelling to list. Yes I know "mithril" is technically the primary spelling but "mythril" is used by most mods, so "mithril" is secondary to it here.
		for (BlockGeneralModdedDeepslateOre block : BlockGeneralModdedDeepslateOre.loaded) { //Future-proofing in case I add more than one
			for (int i = 0; i < block.ores.length; i++) {
				String type = block.ores[i];
				//This can run post-load, so don't register multiple tags if the deepslate ore already got one.
				//We have to do this since it is impossible to remove OreDictionary entries.
				//Well it's probably *technically* possible but I don't want to do it, PR an OD remover if you need EFR to do it.
				//For now just restart your game to clear entries that would no longer get a tag.
				for (int j = 0; j < 1; j++) { //If it's mythril, we'll run this once more, changing the spelling to mithril to account for both tags.
					if (Utils.listGeneralModdedDeepslateOre(type)) { //Make sure an ore is present.
//						registerOre(type, ModBlocks.MODDED_DEEPSLATE_ORE.newItemStack(1, i));
						DeepslateOreRegistry.addOreByOreDict(type, ModBlocks.MODDED_DEEPSLATE_ORE.get(), i);
						registerOre(type, ModBlocks.MODDED_DEEPSLATE_ORE.newItemStack(1, i));
						registerOre(type.replace("ore", "oreDeepslate"), ModBlocks.MODDED_DEEPSLATE_ORE.newItemStack(1, i));
					}
					//We put this outside of the if statement so additional tags added with CT get added.
					if (type.endsWith("Mythril")) {
						type = type.replace("Mythril", "Mithril"); //Redoes it once more for mithril spelling
						j = -1;
					}
				}
			}
		}
	}

	public static void unregisterGeneralRawOres() {
		Pair<List<ItemGeneralModdedRawOre>, List<BlockGeneralModdedRawOre>> pair = Pair.of(ItemGeneralModdedRawOre.loaded, BlockGeneralModdedRawOre.loaded);
		if (pair.getLeft().isEmpty()) {
			return;
		}
		for (int j = 0; j < pair.getLeft().size(); j++) {
			ItemGeneralModdedRawOre oreItem = pair.getLeft().get(j);
			BlockGeneralModdedRawOre oreBlock = pair.getRight().get(j);
			for (int i = 0; i < oreItem.ores.length; i++) {
				removeAllEFRRecipesFor(oreItem, i);
				removeAllEFRRecipesFor(oreBlock, i);
				ItemStack stack = new ItemStack(oreItem, 1, i);
				Iterator<ItemStack> iterator = FurnaceRecipes.smelting().getSmeltingList().keySet().iterator();
				while (iterator.hasNext()) {
					ItemStack smeltingInput = iterator.next();
					if (stack.getItem() == smeltingInput.getItem() && stack.getItemDamage() == smeltingInput.getItemDamage()) {
						iterator.remove();
					}
				}
			}
		}
	}

	public static void registerGeneralRawOres() {
		//Future-proofing in case I add more of these. Also will have only the meta-combined versions when enabled for the eventual NEID/EID support
		Pair<List<ItemGeneralModdedRawOre>, List<BlockGeneralModdedRawOre>> pair = Pair.of(ItemGeneralModdedRawOre.loaded, BlockGeneralModdedRawOre.loaded);
		if (pair.getLeft().size() != pair.getRight().size()) {
			throw new RuntimeException("Modded raw ore block count does not match modded raw ore item count!");
		}
		if (pair.getLeft().isEmpty()) {
			return;
		}
		for (int k = 0; k < pair.getLeft().size(); k++) {
			ItemGeneralModdedRawOre oreItem = pair.getLeft().get(k);
			BlockGeneralModdedRawOre oreBlock = pair.getRight().get(k);
			//Insert alternate Mythril spelling to list. Yes I know "mithril" is technically the primary spelling but "mythril" is used by most mods, so "mithril" is secondary to it here.
			for (int i = 0; i < oreItem.ores.length; i++) {
				String type = oreItem.ores[i];
				//This can run post-load, so don't register multiple tags if the raw ore already got one.
				//We have to do this since it is impossible to remove OreDictionary entries.
				//Well it's probably *technically* possible but I don't want to do it, PR an OD remover if you need EFR to do it.
				//For now just restart your game to clear entries that would no longer get a tag.
				boolean registeredRecipe = false;
				for (int j = 0; j < 1; j++) { //If it's mythril, we'll run this once more, changing the spelling to mithril to account for both tags.
					if (Utils.listGeneralModdedRawOre(type)) { //Make sure an ingot AND ore is present
						registerOre(type.replace("ingot", "raw"), new ItemStack(oreItem, 1, i));
						registerOre(type.replace("ingot", "blockRaw"), new ItemStack(oreBlock, 1, i));
						if (ConfigFunctions.registerRawItemAsOre) {
							registerOre(type.replace("ingot", "ore"), new ItemStack(oreItem, 1, i));
						}
						if (!registeredRecipe) {
							addShapedRecipe(new ItemStack(oreBlock, 1, i), "xxx", "xxx", "xxx", 'x', new ItemStack(oreItem, 1, i));
							addShapedRecipe(new ItemStack(oreItem, 9, i), "x", 'x', new ItemStack(oreBlock, 1, i));
							registeredRecipe = true;
						}
						addSmelting(new ItemStack(oreItem, 1, i), Utils.getFirstStackFromTag(type), 0.7F);
					}
					if (type.endsWith("Mythril")) {
						type = type.replace("Mythril", "Mithril"); //Redoes it once more for mithril spelling
						j = -1;
					}
				}
			}
		}
	}

	private static void registerModSupportDeepslateOres() {
		if (Utils.enableModdedDeepslateOres()) {
			for (BaseDeepslateOre ore : BaseDeepslateOre.loaded) {
				ItemStack baseStack = new ItemStack(ore.getBase(), 1, ore.getBaseMeta());
				ItemStack stack = new ItemStack(ore);
				for (String tag : EtFuturum.getOreStrings(baseStack)) {
					if (tag.startsWith("ore")) {
						addTagsAndDeepslate(tag.replace("ore", ""), stack);
					} else {
						registerOre(tag, stack);
					}
				}
				if (validateItems(ore)) {
					DeepslateOreRegistry.addOre(ore.getBase(), ore.getBaseMeta(), ore, 0);
				}
			}
			for (BaseSubtypesDeepslateOre ore : BaseSubtypesDeepslateOre.loaded) {
				for (int i = 0; i < ore.getTypes().length; i++) {
					ItemStack baseStack = new ItemStack(ore.getBase(i), 1, ore.getBaseMeta(i));
					ItemStack stack = new ItemStack(ore, 1, i);
					for (String tag : EtFuturum.getOreStrings(baseStack)) {
						if (tag.startsWith("ore")) {
							addTagsAndDeepslate(tag.replace("ore", ""), stack);
						} else {
							registerOre(tag, stack);
						}
					}
					if (validateItems(ore)) {
						DeepslateOreRegistry.addOre(ore.getBase(i), ore.getBaseMeta(i), ore, i);
					}
				}
			}
		}
	}

	private static void registerModSupportRawOres() {
		if (ModItems.RAW_ADAMANTIUM.isEnabled() && ModBlocks.RAW_ADAMANTIUM_BLOCK.isEnabled()) {
			addTagsAndRawOre("Adamantium", ModItems.RAW_ADAMANTIUM.newItemStack());
			addTagsAndRawOre("Adamantite", ModItems.RAW_ADAMANTIUM.newItemStack());
			addTagsAndRawOre("Adamantine", ModItems.RAW_ADAMANTIUM.newItemStack());
			registerOre("blockRawAdamantium", ModBlocks.RAW_ADAMANTIUM_BLOCK.newItemStack());
			registerOre("blockRawAdamantite", ModBlocks.RAW_ADAMANTIUM_BLOCK.newItemStack());
			registerOre("blockRawAdamantine", ModBlocks.RAW_ADAMANTIUM_BLOCK.newItemStack());
			addShapedRecipe(ModBlocks.RAW_ADAMANTIUM_BLOCK.newItemStack(), "xxx", "xxx", "xxx", 'x', ModItems.RAW_ADAMANTIUM.newItemStack());
			addShapedRecipe(ModItems.RAW_ADAMANTIUM.newItemStack(9), "x", 'x', ModBlocks.RAW_ADAMANTIUM_BLOCK.newItemStack());
			addSmelting(ModItems.RAW_ADAMANTIUM.newItemStack(), ExternalContent.Items.SIMPLEORES_ADAMANTIUM_INGOT.newItemStack(),
					ExternalContent.Items.SIMPLEORES_ADAMANTIUM_INGOT.get().getSmeltingExperience(ExternalContent.Items.SIMPLEORES_ADAMANTIUM_INGOT.newItemStack()));
			RawOreRegistry.addOre("oreAdamantium", ModItems.RAW_ADAMANTIUM.get(), 0);
		}
	}

	private static void addTagsAndDeepslate(String tagNoPrefix, ItemStack stack) {
		registerOre("ore" + StringUtils.capitalize(tagNoPrefix), stack);
		registerOre("oreDeepslate" + StringUtils.capitalize(tagNoPrefix), stack);
	}

	private static void addTagsAndRawOre(String tagNoPrefix, ItemStack stack) {
		if (ConfigFunctions.registerRawItemAsOre) {
			registerOre("ore" + StringUtils.capitalize(tagNoPrefix), stack);
		}
		registerOre("raw" + StringUtils.capitalize(tagNoPrefix), stack);
	}

	private static List<ItemStack> getStewFlowers() {
		List<ItemStack> list = new ArrayList<>();

		list.add(new ItemStack(Blocks.red_flower, 1, 2));
		list.add(new ItemStack(Blocks.red_flower, 1, 3));
		list.add(new ItemStack(Blocks.red_flower, 1, 1));
		list.add(new ItemStack(Blocks.yellow_flower, 1));
		list.add(new ItemStack(Blocks.red_flower, 1, 8));
		list.add(new ItemStack(Blocks.red_flower, 1, 0));
		list.add(new ItemStack(Blocks.red_flower, 1, 4));
		list.add(new ItemStack(Blocks.red_flower, 1, 5));
		list.add(new ItemStack(Blocks.red_flower, 1, 6));
		list.add(new ItemStack(Blocks.red_flower, 1, 7));

		list.add(ModBlocks.LILY_OF_THE_VALLEY.newItemStack());
		list.add(ModBlocks.CORNFLOWER.newItemStack());
		list.add(ModBlocks.WITHER_ROSE.newItemStack());

		return list;
	}

	private static void removeFurnaceRecipeFor(ItemStack input, ItemStack output) {
		for (Map.Entry<ItemStack, ItemStack> set : FurnaceRecipes.smelting().getSmeltingList().entrySet()) {
			ItemStack setInput = set.getKey();
			ItemStack setOutput = set.getValue();
			int wildcard = OreDictionary.WILDCARD_VALUE;
			if (input.getItem() == setInput.getItem() && (input.getItemDamage() == wildcard || setInput.getItemDamage() == wildcard || (input.getItemDamage() == setInput.getItemDamage()))) {
				if (output.getItem() == setOutput.getItem() && (output.getItemDamage() == wildcard || setOutput.getItemDamage() == wildcard || (output.getItemDamage() == setOutput.getItemDamage()))) {
					FurnaceRecipes.smelting().getSmeltingList().remove(setInput, setOutput);
					return;
				}
			}
		}
	}

	private static void registerOre(String oreName, ItemStack ore) {
		if (validateItems(ore)) {
			OreDictionary.registerOre(oreName, ore);
		}
	}

	private static void registerOre(String oreName, Item ore) {
		if (validateItems(ore)) {
			OreDictionary.registerOre(oreName, ore);
		}
	}

	private static void registerOre(String oreName, Block ore) {
		if (validateItems(ore)) {
			OreDictionary.registerOre(oreName, ore);
		}
	}

	private static void addSmelting(Item input, ItemStack output, float exp) {
		if (validateItems(input) && validateItems(output)) {
			GameRegistry.addSmelting(input, output, exp);
		}
	}

	private static void addSmelting(Block input, ItemStack output, float exp) {
		if (validateItems(input) && validateItems(output)) {
			GameRegistry.addSmelting(input, output, exp);
		}
	}

	private static void addSmelting(ItemStack input, ItemStack output, float exp) {
		if (validateItems(input) && validateItems(output)) {
			GameRegistry.addSmelting(input, output, exp);
		}
	}

	private static void addShapedRecipe(ItemStack output, Object... objects) {
		if (validateItems(output) && validateItems(objects)) {
			GameRegistry.addRecipe(new ShapedEtFuturumRecipe(output, objects));
		}
	}

	private static void addShapelessRecipe(ItemStack output, Object... objects) {
		if (validateItems(output) && validateItems(objects)) {
			GameRegistry.addRecipe(new ShapelessEtFuturumRecipe(output, objects));
		}
	}

	public static boolean validateItems(Object... objects) {
		for (Object object : objects) {
			if (object == null || object == Blocks.air) return false;
			if (object instanceof String) continue;

			if (object instanceof ItemStack) {
				if (((ItemStack) object).getItem() == null || Item.itemRegistry.getNameForObject(((ItemStack) object).getItem()) == null) {
					return false;
				}
			}
			if (object instanceof Item) {
				if (Item.itemRegistry.getNameForObject(object) == null) {
					return false;
				}
			}
			if (object instanceof Block) {
				if (Block.blockRegistry.getNameForObject(object) == null) {
					return false;
				}
			}
		}
		return true;
	}

	private static void removeFirstRecipeFor(Block block) {
		removeFirstRecipeFor(Item.getItemFromBlock(block));
	}

	private static void removeFirstRecipeFor(Block block, int meta) {
		removeFirstRecipeFor(Item.getItemFromBlock(block), meta);
	}

	private static void removeFirstRecipeFor(Item item) {
		removeFirstRecipeFor(item, OreDictionary.WILDCARD_VALUE);
	}

	private static void removeFirstRecipeFor(Item item, int meta) {
		Iterator<IRecipe> iterator = CraftingManager.getInstance().getRecipeList().iterator();
		while (iterator.hasNext()) {
			IRecipe recipe = iterator.next();
			if (recipe != null) {
				ItemStack stack = recipe.getRecipeOutput();
				if (stack != null && stack.getItem() != null && stack.getItem() == item && (meta == OreDictionary.WILDCARD_VALUE || meta == stack.getItemDamage())) {
					iterator.remove();
					return;
				}
			}
		}
	}

	private static void removeAllEFRRecipesFor(Block block) {
		removeAllEFRRecipesFor(Item.getItemFromBlock(block));
	}

	private static void removeAllEFRRecipesFor(Block block, int meta) {
		removeAllEFRRecipesFor(Item.getItemFromBlock(block), meta);
	}

	private static void removeAllEFRRecipesFor(Item item) {
		removeAllEFRRecipesFor(item, OreDictionary.WILDCARD_VALUE);
	}

	private static void removeAllEFRRecipesFor(Item item, int meta) {
		Iterator<IRecipe> iterator = CraftingManager.getInstance().getRecipeList().iterator();
		while (iterator.hasNext()) {
			IRecipe recipe = iterator.next();
			if ((recipe instanceof ShapedEtFuturumRecipe || recipe instanceof ShapelessEtFuturumRecipe)) {
				ItemStack stack = recipe.getRecipeOutput();
				if (stack != null && stack.getItem() != null && stack.getItem() == item && (meta == OreDictionary.WILDCARD_VALUE || meta == stack.getItemDamage())) {
					iterator.remove();
				}
			}
		}
	}
}
