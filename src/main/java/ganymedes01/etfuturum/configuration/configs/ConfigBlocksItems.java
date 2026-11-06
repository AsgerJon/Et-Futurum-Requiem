package ganymedes01.etfuturum.configuration.configs;

import ganymedes01.etfuturum.compat.ModsList;
import ganymedes01.etfuturum.configuration.ConfigBase;
import net.minecraftforge.common.config.Property;

import java.io.File;

public class ConfigBlocksItems extends ConfigBase {

	public static boolean enableArmourStand;
	public static int netheriteToolDurability;
	public static int netheriteEnchantability;
	public static int netheriteHarvestLevel;
	public static float netheriteSpeed;
	public static float netheriteDamageBase;
	public static int netheriteArmourDurabilityFactor;
	public static int netheritePickaxeDurability;
	public static int netheriteSwordDurability;
	public static int netheriteHoeDurability;
	public static int netheriteAxeDurability;
	public static int netheriteSpadeDurability;
	public static int netheriteHelmetDurability;
	public static int netheriteChestplateDurability;
	public static int netheriteLeggingsDurability;
	public static int netheriteBootsDurability;
	public static int netheriteHelmetProtection;
	public static int netheriteChestplateProtection;
	public static int netheriteLeggingsProtection;
	public static int netheriteBootsProtection;

	public static boolean enableMutton;
	public static boolean enableSponge;
	public static boolean enablePrismarine;
	public static boolean enableInvertedDaylightSensor;
	public static boolean enableOldBaseDaylightSensor;
	public static boolean enableCoarseDirt;
	public static boolean enableSlimeBlock;
	public static boolean enableBeetroot;
	public static boolean enableChorusFruit;
	public static boolean enableGrassPath;
	public static boolean enableCryingObsidian;
	public static boolean enableBrewingStands;
	public static boolean enableColourfulBeacons;
	public static boolean enableBarrel;
	public static boolean enableCoopersMallet;
	public static boolean enableLantern;
	public static boolean enableSmoker;
	public static boolean enableBlastFurnace;
	public static boolean enableBarrier;
	public static boolean enableLightBlock;
	public static boolean enableNetherGold;
	public static boolean enableQuartzBricks;
	public static boolean enableLilyOfTheValley;
	public static boolean enableCornflower;
	public static boolean enableWitherRose;
	public static boolean enableSweetBerryBushes;
	public static boolean enableLavaCauldrons;
	public static boolean enableNewNetherBricks;
	public static boolean enableNetherwartBlock;
	public static boolean enableMagmaBlock;
	public static boolean enableNetherite;
	public static boolean enableBoneBlock;
	public static boolean enableConcrete;
	public static boolean enableTotemUndying;
	public static boolean enableRawOres;
	public static boolean enableNewDyes;
	public static boolean enableBlueIce;
	public static boolean enableCopper;
	public static boolean enableDeepslate;
	public static boolean enableDeepslateOres;
	public static boolean enableNewTileEntities;
	public static boolean enableSuspiciousStew;
	public static boolean enableGlazedTerracotta;
	public static boolean enableIronNugget;
	public static boolean enablePigstep;
	public static boolean enableOtherside;
	public static boolean enablePrecipice;
	public static boolean enableCreatorMusicBox;
	public static boolean enableCreator;
	public static boolean enableLavaChicken;
	public static boolean enableTears;
	public static boolean enable5;
	public static boolean enableEnchantingTable;
	public static boolean enableAnvil;
	public static boolean enableComposter;
	public static boolean enableSmithingTable;
	public static boolean enableLightningRod;
	public static boolean enableStonecutter;
	public static boolean enableFletchingTable;
	public static boolean enableCartographyTable;
	public static boolean enableLoom;
	public static boolean enableDyedBeds;
	public static boolean enableTarget;
	public static boolean enableHoney;

	public static boolean enableBubbleColumns;

	public static boolean enableChain;
	public static boolean enableBlackstone;
	public static boolean enableBasalt;
	public static boolean enableSoulSoil;
	public static boolean enableSoulLighting;

	// Wilds Update

	// 1.20

	public static boolean enableDyedShulkerBoxes = true;
	public static boolean enablePotionCauldron;

	public static int endGatewaySpawnColor = 2;
	public static int endGatewayEntryColor = 2;

	public static boolean woodVariants;

	public static final String catBlockNatural = "natural blocks";
	public static final String catBlockFunc = "function blocks";
	public static final String catBlockMisc = "misc blocks";
	public static final String catItemEquipment = "equipment";
	public static final String catItemEntity = "entity items";
	public static final String catItemMisc = "misc items";

	public ConfigBlocksItems(File file) {
		super(file);
		setCategoryComment(catBlockNatural, "Blocks that can generate naturally in your world. Check world.cfg for generation values.");
		setCategoryComment(catBlockFunc, "Blocks that have a specific function, whether right clicked or otherwise.");
		setCategoryComment(catBlockMisc, "Blocks that don't fit in any other category.");
		setCategoryComment(catItemEquipment, "Tools, armor, and other equipment items.");
		setCategoryComment(catItemEntity, "Entity items. (Armor stand, boat, etc)");
		setCategoryComment(catItemMisc, "Items that don't fit in any other category.");

		configCats.add(getCategory(catBlockNatural));
		configCats.add(getCategory(catBlockFunc));
		configCats.add(getCategory(catBlockMisc));
		configCats.add(getCategory(catItemEquipment));
		configCats.add(getCategory(catItemEntity));
		configCats.add(getCategory(catItemMisc));
	}

	@Override
	protected void syncConfigOptions() {
		//Natural Blocks
		enableNetherGold = getBoolean("enableNetherGold", catBlockNatural, true, "");
		enablePrismarine = getBoolean("enablePrismarine", catBlockNatural, true, "");
		enableCoarseDirt = getBoolean("enableCoarseDirt", catBlockNatural, true, "");
		enableChorusFruit = getBoolean("enableChorusBlocks", catBlockNatural, true, "Enables chorus plants and purpur blocks");
		enableGrassPath = getBoolean("enableGrassPath", catBlockNatural, true, "");
		enableCryingObsidian = getBoolean("enableCryingObsidian", catBlockNatural, true, "");
		enableNewNetherBricks = getBoolean("enableRedNetherBricks", catBlockMisc, true, "Note: Also enables cracked and chiseled nether bricks as they use this ID too");
		enableNetherwartBlock = getBoolean("enableNetherwartBlock", catBlockNatural, true, "");
		enableNetherite = getBoolean("enableNetherite", catBlockNatural, true, "");
		enableMagmaBlock = getBoolean("enableMagmaBlock", catBlockNatural, true, "");
		enableBoneBlock = getBoolean("enableBoneBlock", catBlockNatural, true, "");
		enableBlueIce = getBoolean("enableBlueIce", catBlockNatural, true, "");
		enableLilyOfTheValley = getBoolean("enableLilyOfTheValley", catBlockNatural, true, "");
		enableCornflower = getBoolean("enableCornflower", catBlockNatural, true, "");
		enableWitherRose = getBoolean("enableWitherRose", catBlockNatural, true, "");
		enableCopper = getBoolean("enableCopper", catBlockNatural, true, "Copper ore and copper blocks, variants, and waxed variants. (Slime balls are used if no mod introduces wax and if honey is disabled)");
		enableSweetBerryBushes = getBoolean("enableSweetBerryBushes", catBlockNatural, true, "");
		enableDeepslate = getBoolean("enableDeepslate", catBlockNatural, true, "");
		enableDeepslateOres = getBoolean("enableDeepslateOres", catBlockNatural, true, "Enable deepslate ores for copper ore and vanilla ores when deepslate generates over them.");
//		enableMoss = getBoolean("enableMoss", catBlockNatural, true, "Enables moss blocks and carpets");
//		enableCrimsonBlocks = getBoolean("enableCrimsonBlocks", catBlockMisc, true, "Enables the crimson nylium, wood, and plants. This must be on for the crimson forest biome to generate unless Netherlicious is installed.\nThe nether wart block is still a separate toggle, both this and the wart toggle must be turned off to disable the nether wart block, because crimson trees need the wart blocks.");
//		enableWarpedBlocks = getBoolean("enableWarpedBlocks", catBlockMisc, true, "Enables the warped nylium, wood, and plants. This must be on for the warped forest biome to generate unless Netherlicious is installed.");
		enableBlackstone = getBoolean("enableBlackstone", catBlockMisc, true, "This must be on for the basalt deltas biome to generate unless Netherlicious is installed.");
		enableSoulSoil = getBoolean("enableSoulSoil", catBlockNatural, true, "Not required for the Soul Sand Valley to generate.");
		enableSoulLighting = getBoolean("enableSoulLighting", catBlockNatural, true, "Soul torches and soul lanterns.");
		enableBasalt = getBoolean("enableBasalt", catBlockNatural, true, "This must be on for the basalt deltas biome to generate unless Netherlicious is installed.");
		if (ConfigExperiments.enableCrimsonBlocks) {
			enableNetherwartBlock = true;
		}

		boolean masterNetherToggle = getBoolean("masterNetherToggle", catBlockNatural, true,
				"NOTE: Currently the content below is referring to some things which are behind an experimental toggle." +
						"\nAdditionally the new nether is NOT compatible with Netherlicious yet. The comment below refers to the plans for when the features below are complete." +
						"\n" +
						"Set this to false to easily turn off all Nether blocks. This also turns off all Nether biomes because they require the blocks to generate.\n" +
						"My biomes have compat with Netherlicious (read world.cfg for more info) but if you don't want any compat or EFR Nether stuff at all turn this off.\n" +
						"This disables the following toggles: enableCrimsonBlocks, enableWarpedBlocks, enableBlackstone, enableSoulSoil, enableSoulLighting and enableBasalt.\n" +
						"Amethyst geodes use smooth basalt so go to world.cfg to change the outer block to something else or they won't generate.\n" +
						"This also turns off Nether wart blocks even though they are older, because Netherlicious also has those.");

		if (!masterNetherToggle) {
			ConfigExperiments.enableCrimsonBlocks = ConfigExperiments.enableWarpedBlocks = enableNetherwartBlock = enableBlackstone = enableSoulSoil = enableSoulLighting = enableBasalt = false;
		}

//		enableMangroveBlocks = getBoolean("enableMangroveBlocks", catBlockNatural, true, "Enables mangrove wood and all of its wood subtypes, and the roots (+ muddy versions).");

		// Check if we enable wood variants at all. Used by blocks where all the wood is 1 ID
		woodVariants = ConfigExperiments.enableCrimsonBlocks || ConfigExperiments.enableWarpedBlocks || ConfigExperiments.enableMangroveBlocks || io.github.asgerjon.mcfly.config.McFlyConfig.enableCherryBlocks || io.github.asgerjon.mcfly.config.McFlyConfig.enableBambooBlocks;

//		enableSculk = getBoolean("enableSculk", catBlockNatural, true, "Enables sculk-related blocks.");

		//Function Blocks
		enableSponge = getBoolean("enableSponge", catBlockFunc, true, "");
		enableBubbleColumns = getBoolean("enableBubbleColumns", catBlockFunc, true, "Bubble columns generated by soul sand (upward) and magma blocks (downward) underwater.");
		enableSlimeBlock = getBoolean("enableSlimeBlock", catBlockFunc, true, "Just bouncy, does not pull blocks.");
		enableBarrel = getBoolean("enableBarrel", catBlockFunc, true, "");
		enableCoopersMallet = getBoolean("enableCoopersMallet", catBlockFunc, true, "Adds a mallet that converts a chest into a barrel in place, and back, keeping the inventory. Requires enableBarrel.");
		enableSmoker = getBoolean("enableSmoker", catBlockFunc, true, "Will attempt to seek and auto-add recipes to itself. Look at ConfigFunctions.cfg \"autoAddSmoker\" for more info.\nCompatible with CraftTweaker. In the same way that you'd use furnace.addRecipe or furnace.remove, you can use \"mods.etfuturum.smoker\" instead of \"furnace\".");
		enableBlastFurnace = getBoolean("enableBlastFurnace", catBlockFunc, true, "Will attempt to seek and auto-add recipes to itself. Look at ConfigFunctions.cfg \"autoAddBlastFurance\" for more info.\nCompatible with CraftTweaker. In the same way that you'd use furnace.addRecipe or furnace.remove, you can use \"mods.etfuturum.blastFurnace\" instead of \"furnace\".");
		enableLavaCauldrons = getBoolean("enableLavaCauldrons", catBlockFunc, true, "Allow lava buckets to fill cauldrons");
		enablePotionCauldron = getBoolean("enablePotionCauldron", catBlockFunc, true, "A port of potion cauldrons from Bedrock Edition. Used to make tipped arrows and store potions.");
		//Note the above option has an extra check in preInit to ensure Iron Chests is loaded. We can't do this here because Loader doesn't even exist yet since we initialize these configs while Mixins and ASM are being applied.
		enableStonecutter = getBoolean("enableStonecutter", catBlockFunc, true, "Currently DOES NOT HAVE ANY FUNCTIONALITY. Decoration ONLY!");
		enableLightningRod = getBoolean("enableLightningRod", catBlockFunc, true, "The lightning rod (1.17+), from three copper ingots in a column: natural lightning within 128 blocks strikes the highest one, and a struck rod gives a redstone signal for 8 ticks. It weathers and can be waxed like other copper. Needs enableCopper, and modernLightning in mixins.cfg for lightning to find it and power it.");
		enableSmithingTable = getBoolean("enableSmithingTable", catBlockFunc, true, "If this is disabled, netherite items will not be craftable unless added by CraftTweaker. This introduces the smithing GUI from versions prior to 1.20. It is compatible with CraftTweaker." +
				"\nThe mod prefix is \"mods.etfuturum.smithingTable\", and the functions are \"addRecipe\" or \"addRecipeNoNBT\". \"addRecipe\" will copy all NBT data from the first slot to the output. (and damage if the first slot and output are damageable items) You can remove recipes using the \"remove\" function and an ItemStack." +
				"\nCurrently CraftTweaker support is limited, so some features from CraftTweaker may not work. .noReturn() and .anyDamage().onlyDamaged() are known to not work." +
				"\nHowever anyDamage() by itself does work, and is required for tool inputs to actually copy their damage over to the output." +
				"\nExamples:" +
				"\nmods.etfuturum.smithingTable.addRecipe(<IC2:itemToolBronzeSword>, <minecraft:iron_sword>.anyDamage(), <ore:ingotBronze>); //(Use \"ingotBronze\" on iron sword to convert it to a bronze sword)" +
				"\nmods.etfuturum.smithingTable.addRecipeNoNBT(<etfuturum:sponge:1>, <minecraft:sponge>, <minecraft:water_bucket>); //Take a vanilla sponge and a water bucket, you will get an Et Futurum Requiem wet sponge.");
		enableFletchingTable = getBoolean("enableFletchingTable", catBlockFunc, true, "Currently DOES NOT HAVE ANY FUNCTIONALITY. Decoration ONLY!");
		enableComposter = getBoolean("enableComposter", catBlockFunc, true, "Allows certain items to be composted, which has a chance of adding a layer to the compost bin. Once full, bone meal may be harvested. It is compatible with CraftTweaker." +
				"\nA value greater than 100 can add more than one layer to the composter. (The composter has 6 layers so max value is 600) EG 150 = 1 layer guaranteed and a 50% chance to fill another layer. And 600 = instantly fill the whole composter, or any remaining layers." +
				"\nThe mod prefix is \"mods.etfuturum.composting\", and the function is \"addCompostable\" and an ItemStack or OreDictionary tag, then an integer for how likely the item is to add a compost layer. 100 = 100%, or guaranteed. You can remove compostables using the \"remove\" function and an ItemStack or an OreDictionary tag." +
				"\nYou can use the \"removeAll\" function to remove all composting recipes, if you wanted to overhaul the composter's useage." +
				"\nExamples:" +
				"\nmods.etfuturum.composting.addCompostable(<minecraft:planks:*>, 100); //(Makes all planks have a 100% chance to fill one layer.)" +
				"\nmods.etfuturum.composting.addCompostable(<minecraft:bedrock>, 150); //(Makes bedrock fill one layer and have a 50% chance to fill another layer.)" +
				"\nmods.etfuturum.composting.remove(<minecraft:leaves:*>); //(Removes all leaves as a compostable item)" +
				"\n\nAdditionally the bone meal drop when harvesting a full composting bin can be changed. It drops one item from the \"composting\" loot table, and can be modified with CraftTweaker's \"addChestLoot\" function." +
				"\nBy default bone meal is the only item in this loot table, with a weight of 10. You can learn more about CraftTweaker loot table modification here: https://www.mcdrama.net/archiveformal/MineTweaker3/wiki/tutorials/1710/loot_and_seeds.html"
		);
		enableCartographyTable = getBoolean("enableCartographyTable", catBlockFunc, true, "Currently DOES NOT HAVE ANY FUNCTIONALITY. Decoration ONLY!");
		enableLoom = getBoolean("enableLoom", catBlockFunc, true, "Currently DOES NOT HAVE ANY FUNCTIONALITY. Decoration ONLY!");
		enableDyedBeds = getBoolean("enableDyedBeds", catBlockFunc, true, "Ability to craft differently colored beds out of wool. Mixed wool colors = red bed");

		enableEnchantingTable = getBoolean("enableNewEnchantingTable", catBlockFunc, true, "Uses lapis as payment and has enchant previews and adjusted level costs. Requires tile entity replacement to be enabled in \"function.cfg\". It is compatible with CraftTweaker for adding and removing fuels." +
				"\nThe mod prefix is \"mods.etfuturum.enchantingFuel\", and the function is \"addFuel\" and an ItemStack or OreDictionary tag. You can remove fuels using the \"remove\" function and an ItemStack or an OreDictionary tag." +
				"\nExamples:" +
				"\nmods.etfuturum.enchantingFuel.addFuel(<etfuturum:amethyst_shard>); //(Adds amethyst shards as an enchanting fuel)" +
				"\nmods.etfuturum.enchantingFuel.remove(<minecraft:dye:4>); //(Removes lapis lazuli as an enchanting fuel)");
		enableAnvil = getBoolean("enableNewAnvil", catBlockFunc, true, "Enables new anvil behavior, such as less expensive item renaming");
		enableBrewingStands = getBoolean("enableNewBrewingStand", catBlockFunc, true, "Makes the brewing stand have a fuel slot like in 1.9+. The fuel slot is compatible with CraftTweaker and takes blaze powder by default. Blaze powder can brew 30 potion cycles." +
				"\nThe mod prefix is \"mods.etfuturum.brewingFuel\", and the function is \"addFuel\" and an ItemStack or OreDictionary tag, then an integer for how many brew cycles. (Any brewing, regardless of if 1 or all 3 slots are filled, is still one \"cycle\") You can remove fuels using the \"remove\" function and an ItemStack or an OreDictionary tag." +
				"\nExamples:" +
				"\nmods.etfuturum.brewingFuel.addFuel(<minecraft:gunpowder>, 10); //(Makes gunpowder have 10 brewing cycles)" +
				"\nmods.etfuturum.brewingFuel.remove(<minecraft:blaze_powder>); //(Removes blaze powder as a brewing fuel)");
		enableColourfulBeacons = getBoolean("enableNewBeacon", catBlockFunc, true, "Beacon beam can be colored using stained glass");
		enableInvertedDaylightSensor = getBoolean("enableInvertedSensor", catBlockFunc, true, "Inverted Daylight Sensor");
		enableOldBaseDaylightSensor = getBoolean("enableOldBaseDaylightSensor", catBlockFunc, false, "Enable the old Et Futurum daylight sensor block. Should be enabled if you still have the old Et Futurum copy of the non-inverted daylight detector that need to be converted.");

		enableTarget = getBoolean("enableTarget", catBlockFunc, true, "Enables target block from 1.16");

		//Misc Blocks
		enableConcrete = getBoolean("enableConcrete", catBlockMisc, true, "");
		enableLantern = getBoolean("enableLantern", catBlockMisc, true, "");
		enableQuartzBricks = getBoolean("enableQuartzBricks", catBlockMisc, true, "");
		enableGlazedTerracotta = getBoolean("enableGlazedTerracotta", catBlockMisc, true, "");
		enableBarrier = getBoolean("enableBarrier", catBlockMisc, true, "A solid, indestructible and invisible block. Can be seen when holding it in Creative mode.");
		enableLightBlock = getBoolean("enableLightBlock", catBlockMisc, true, "Invisible light blocks. Only has a selection box when held, right click to change light level. Otherwise functionally identical to air and can be replaced by placing blocks into it. Invisible, but can be seen when holding it in Creative mode.");
		enableChain = getBoolean("enableChain", catBlockMisc, true, "");
		enableHoney = getBoolean("enableHoney", catBlockMisc, true, "Enables honey blocks, honeycomb blocks, honeycombs, and honey bottles. See entities.cfg for toggling bee nests, beehives, and bees.");

		//Misc Items
		enableMutton = getBoolean("enableMutton", catItemMisc, true, "");
		enableBeetroot = getBoolean("enableBeetroot", catItemMisc, true, "");
		enableIronNugget = getBoolean("enableIronNugget", catItemMisc, true, "");
		enableRawOres = getBoolean("enableRawOres", catItemMisc, true, "If true, vanilla and Et Futurum copper ores will drop raw ore items.");

		enableTotemUndying = getBoolean("enableTotemUndying", catItemMisc, true, "");
		enableSuspiciousStew = getBoolean("enableSuspiciousStew", catItemMisc, true, "");
		enableNewDyes = getBoolean("enableNewDyes", catItemMisc, true, "");

		enablePigstep = getBoolean("enablePigstep", catItemMisc, true, "Appears in Nether fortress chest loot.");
		enableOtherside = getBoolean("enableOtherside", catItemMisc, true, "Appears in stronghold corridor and dungeon chests.");
		enablePrecipice = getBoolean("enablePrecipice", catItemMisc, true, "Appears in stronghold corridor and dungeon chests.");
		enableCreatorMusicBox = getBoolean("enableCreatorMusicBox", catItemMisc, true, "Appears in stronghold corridor and dungeon chests.");
		enableCreator = getBoolean("enableCreator", catItemMisc, true, "Appears in stronghold corridor and dungeon chests.");
		enableTears = getBoolean("enableTears", catItemMisc, true, "Appears in stronghold corridor and dungeon chests.");
		enableLavaChicken = getBoolean("enableLavaChicken", catItemMisc, true, "Appears only when zombie juvenile variant riding a chicken.");
		enable5 = getBoolean("enable5", catItemMisc, true, "Appears in stronghold corridor and dungeon chests.");

		//Equipment Items
		netheriteToolDurability = getInt("netheriteToolDurability", catItemEquipment, 2031, 1, Integer.MAX_VALUE, "");
		netheriteEnchantability = getInt("netheriteEnchantability", catItemEquipment, 15, 1, Integer.MAX_VALUE, "");
		netheriteHarvestLevel = getInt("netheriteHarvestLevel", catItemEquipment, 4, 0, Integer.MAX_VALUE, "Netherite harvest level, Diamond is 3");
		netheriteSpeed = getFloat("netheriteSpeed", catItemEquipment, 9.0f, 0.1f, Float.MAX_VALUE, "Nethherite mining speed, Diamond is 8.0");
		netheriteDamageBase = getFloat("netheriteDamageBase", catItemEquipment, 4.0f, 0.0f, Float.MAX_VALUE, "Netherite base damage, Diamond is 3.0");
		netheriteArmourDurabilityFactor = getInt("netheriteArmourDurabilityFactor", catItemEquipment, 37, 1, Integer.MAX_VALUE, "Used by the game to generate the durability value for the armor set. The higher the better.");
		netheritePickaxeDurability = getInt("netheritePickaxeDurability", catItemEquipment, -1, -1, Integer.MAX_VALUE, "Override Netherite Pickaxe Durability, -1 to use netheriteToolDurability");
		netheriteSwordDurability = getInt("netheriteSwordDurability", catItemEquipment, -1, -1, Integer.MAX_VALUE, "Override Netherite Sword Durability, -1 to use netheriteToolDurability");
		netheriteHoeDurability = getInt("netheriteHoeDurability", catItemEquipment, -1, -1, Integer.MAX_VALUE, "Override Netherite Hoe Durability, -1 to use netheriteToolDurability");
		netheriteAxeDurability = getInt("netheriteAxeDurability", catItemEquipment, -1, -1, Integer.MAX_VALUE, "Override Netherite Axe Durability, -1 to use netheriteToolDurability");
		netheriteSpadeDurability = getInt("netheriteSpadeDurability", catItemEquipment, -1, -1, Integer.MAX_VALUE, "Override Netherite Shovel Durability, -1 to use netheriteToolDurability");
		netheriteHelmetDurability = getInt("netheriteHelmetDurability", catItemEquipment, -1, -1, Integer.MAX_VALUE, "Override Netherite Helmet Durability, -1 to no override");
		netheriteChestplateDurability = getInt("netheriteChestplateDurability", catItemEquipment, -1, -1, Integer.MAX_VALUE, "Override Netherite Chestplate Durability, -1 for no override");
		netheriteLeggingsDurability = getInt("netheriteLeggingsDurability", catItemEquipment, -1, -1, Integer.MAX_VALUE, "Override Netherite Leggings Durability, -1 for no override");
		netheriteBootsDurability = getInt("netheriteBootsDurability", catItemEquipment, -1, -1, Integer.MAX_VALUE, "Override Netherite Boots Durability, -1 for no override");
		netheriteHelmetProtection = getInt("netheriteHelmetProtection", catItemEquipment, 3, 1, Integer.MAX_VALUE, "Netherite Helmet Protection");
		netheriteChestplateProtection = getInt("netheriteChestplateProtection", catItemEquipment, 8, 1, Integer.MAX_VALUE, "Netherite Chestplate Protection");
		netheriteLeggingsProtection = getInt("netheriteLeggingsProtection", catItemEquipment, 6, 1, Integer.MAX_VALUE, "Netherite Leggings Protection");
		netheriteBootsProtection = getInt("netheriteBootsProtection", catItemEquipment, 3, 1, Integer.MAX_VALUE, "Netherite Boots Protection");

		//Entity Items
		enableArmourStand = getBoolean("enableArmorStand", catItemEntity, true, "");


		//      endGatewaySpawnColor = getInt("endGatewaySpawnColor", catAbandoned, 2, 0, 15, "The color of the end gateway beam when the gateway first appears.");
//      endGatewayEntryColor = getInt("endGatewayEntryColor", catAbandoned, 2, 0, 15, "The color of the end gateway beam when an entity enters it. Originally, this value was 4 (yellow) before version 1.11.");
	}

	@Override
	protected void initValues() {
		if (!ModsList.IRON_CHEST.isLoaded()) {
			ConfigModCompat.barrelIronChest = false;
		}
	}
}
