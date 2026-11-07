package ganymedes01.etfuturum;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import cpw.mods.fml.common.registry.GameRegistry;


import ganymedes01.etfuturum.blocks.BaseBlock;
import ganymedes01.etfuturum.blocks.BaseFlower;
import ganymedes01.etfuturum.blocks.BaseLeaves;
import ganymedes01.etfuturum.blocks.BaseSlab;
import ganymedes01.etfuturum.blocks.BaseStairs;
import ganymedes01.etfuturum.blocks.BaseSubtypesBlock;
import ganymedes01.etfuturum.blocks.BaseWall;
import ganymedes01.etfuturum.blocks.BlockBarrier;
import ganymedes01.etfuturum.blocks.BlockBeeHive;
import ganymedes01.etfuturum.blocks.BlockBeetroot;
import ganymedes01.etfuturum.blocks.BlockBerryBush;
import ganymedes01.etfuturum.blocks.BlockBlueIce;
import ganymedes01.etfuturum.blocks.BlockBone;
import ganymedes01.etfuturum.blocks.BlockBubbleColumn;
import ganymedes01.etfuturum.blocks.BlockCartographyTable;
import ganymedes01.etfuturum.blocks.BlockCoarseDirt;
import ganymedes01.etfuturum.blocks.BlockComposter;
import ganymedes01.etfuturum.blocks.BlockConcretePowder;
import ganymedes01.etfuturum.blocks.BlockDirtPath;
import ganymedes01.etfuturum.blocks.BlockDyedBed;
import ganymedes01.etfuturum.blocks.BlockFletchingTable;
import ganymedes01.etfuturum.blocks.BlockFrostedIce;
import ganymedes01.etfuturum.blocks.BlockGlazedTerracotta;
import ganymedes01.etfuturum.blocks.BlockHoney;
import ganymedes01.etfuturum.blocks.BlockInvertedDaylightDetector;
import ganymedes01.etfuturum.blocks.BlockLavaCauldron;
import ganymedes01.etfuturum.blocks.BlockLight;
import ganymedes01.etfuturum.blocks.BlockLoom;
import ganymedes01.etfuturum.blocks.BlockMagma;
import ganymedes01.etfuturum.blocks.BlockNewAnvil;
import ganymedes01.etfuturum.blocks.BlockNewBeacon;
import ganymedes01.etfuturum.blocks.BlockNewBrewingStand;
import ganymedes01.etfuturum.blocks.BlockNewDaylightSensor;
import ganymedes01.etfuturum.blocks.BlockNewEnchantmentTable;
import ganymedes01.etfuturum.blocks.BlockOldGravel;
import ganymedes01.etfuturum.blocks.BlockOldRose;
import ganymedes01.etfuturum.blocks.BlockPinkPetals;
import ganymedes01.etfuturum.blocks.BlockPotionCauldron;
import ganymedes01.etfuturum.blocks.BlockSeaLantern;
import ganymedes01.etfuturum.blocks.BlockSilkedMushroom;
import ganymedes01.etfuturum.blocks.BlockSlime;
import ganymedes01.etfuturum.blocks.BlockSponge;
import ganymedes01.etfuturum.blocks.BlockStonecutter;
import ganymedes01.etfuturum.blocks.BlockWitherRose;
import ganymedes01.etfuturum.blocks.ISubBlocksBlock;
import ganymedes01.etfuturum.blocks.itemblocks.BaseItemBlock;
import ganymedes01.etfuturum.blocks.itemblocks.BaseLeavesItemBlock;
import ganymedes01.etfuturum.blocks.itemblocks.BasePotableItemBlock;
import ganymedes01.etfuturum.blocks.itemblocks.BaseSlabItemBlock;
import ganymedes01.etfuturum.blocks.itemblocks.BaseSubtypesPotableItemBlock;
import ganymedes01.etfuturum.blocks.itemblocks.ItemBlockDecorationWorkbench;
import ganymedes01.etfuturum.blocks.itemblocks.ItemBlockDyedBed;
import ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateAdamantiumOre;
import ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateArsMagicaOre;
import ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateBiomesOPlentyOre;
import ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateBluePowerOre;
import ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateCertusQuartzOre;
import ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateDraconiumOre;
import ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateDragonBlockOre;
import ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateDragonQuestOre;
import ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateProjectRedOre;
import ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateThaumcraftOre;
import ganymedes01.etfuturum.blocks.ores.modded.BlockGeneralModdedDeepslateOre;
import ganymedes01.etfuturum.blocks.rawore.modded.BlockGeneralModdedRawOre;
import ganymedes01.etfuturum.blocks.rawore.modded.BlockRawAdamantium;
import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.compat.ModsList;

import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.configuration.configs.ConfigEnchantsPotions;
import ganymedes01.etfuturum.configuration.configs.ConfigEntities;
import ganymedes01.etfuturum.configuration.configs.ConfigExperiments;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.configuration.configs.ConfigModCompat;
import ganymedes01.etfuturum.configuration.configs.ConfigTweaks;
import ganymedes01.etfuturum.core.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAnvilBlock;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public enum ModBlocks {
	PRISMARINE_BLOCK(ConfigBlocksItems.enablePrismarine, new BaseSubtypesBlock(Material.rock, "prismarine", "prismarine_bricks", "dark_prismarine")
			.setHardness(1.5F).setResistance(10.0F)),
	SEA_LANTERN(ConfigBlocksItems.enablePrismarine, new BlockSeaLantern()),
	DAYLIGHT_DETECTOR_INVERTED(ConfigBlocksItems.enableInvertedDaylightSensor, new BlockInvertedDaylightDetector(), null),
	BROWN_MUSHROOM(ConfigFunctions.enableSilkTouchingMushrooms, new BlockSilkedMushroom(Blocks.brown_mushroom_block, "brown")),
	RED_MUSHROOM(ConfigFunctions.enableSilkTouchingMushrooms, new BlockSilkedMushroom(Blocks.red_mushroom_block, "red")),
	COARSE_DIRT(ConfigBlocksItems.enableCoarseDirt, new BlockCoarseDirt()),
	SLIME(ConfigBlocksItems.enableSlimeBlock, new BlockSlime()),
	SPONGE(ConfigBlocksItems.enableSponge, new BlockSponge()),
	BEETROOTS(ConfigBlocksItems.enableBeetroot, new BlockBeetroot(), null),
	GRASS_PATH(ConfigBlocksItems.enableGrassPath, new BlockDirtPath()),
	BONE(ConfigBlocksItems.enableBoneBlock, new BlockBone()),
	BLUE_ICE(ConfigBlocksItems.enableBlueIce, new BlockBlueIce()),
	CONCRETE(ConfigBlocksItems.enableConcrete, new BaseSubtypesBlock(Material.rock, "white_concrete", "orange_concrete", "magenta_concrete", "light_blue_concrete", "yellow_concrete", "lime_concrete", "pink_concrete",
			"gray_concrete", "light_gray_concrete", "cyan_concrete", "purple_concrete", "blue_concrete", "brown_concrete", "green_concrete", "red_concrete", "black_concrete").setNames("concrete")
			.setMapColorBaseBlock(Blocks.wool).setHardness(1.8F).setResistance(1.8F),
			BaseItemBlock.class),
	CONCRETE_POWDER(ConfigBlocksItems.enableConcrete, new BlockConcretePowder()),
	CORNFLOWER(ConfigBlocksItems.enableCornflower, new BaseFlower().setNames("cornflower")),
	LILY_OF_THE_VALLEY(ConfigBlocksItems.enableLilyOfTheValley, new BaseFlower().setNames("lily_of_the_valley")),
	WITHER_ROSE(ConfigBlocksItems.enableWitherRose, new BlockWitherRose()),
	SWEET_BERRY_BUSH(ConfigBlocksItems.enableSweetBerryBushes, new BlockBerryBush(), null),
	WHITE_GLAZED_TERRACOTTA(ConfigBlocksItems.enableGlazedTerracotta, new BlockGlazedTerracotta(0)),
	ORANGE_GLAZED_TERRACOTTA(ConfigBlocksItems.enableGlazedTerracotta, new BlockGlazedTerracotta(1)),
	MAGENTA_GLAZED_TERRACOTTA(ConfigBlocksItems.enableGlazedTerracotta, new BlockGlazedTerracotta(2)),
	LIGHT_BLUE_GLAZED_TERRACOTTA(ConfigBlocksItems.enableGlazedTerracotta, new BlockGlazedTerracotta(3)),
	YELLOW_GLAZED_TERRACOTTA(ConfigBlocksItems.enableGlazedTerracotta, new BlockGlazedTerracotta(4)),
	LIME_GLAZED_TERRACOTTA(ConfigBlocksItems.enableGlazedTerracotta, new BlockGlazedTerracotta(5)),
	PINK_GLAZED_TERRACOTTA(ConfigBlocksItems.enableGlazedTerracotta, new BlockGlazedTerracotta(6)),
	GRAY_GLAZED_TERRACOTTA(ConfigBlocksItems.enableGlazedTerracotta, new BlockGlazedTerracotta(7)),
	LIGHT_GRAY_GLAZED_TERRACOTTA(ConfigBlocksItems.enableGlazedTerracotta, new BlockGlazedTerracotta(8)),
	CYAN_GLAZED_TERRACOTTA(ConfigBlocksItems.enableGlazedTerracotta, new BlockGlazedTerracotta(9)),
	PURPLE_GLAZED_TERRACOTTA(ConfigBlocksItems.enableGlazedTerracotta, new BlockGlazedTerracotta(10)),
	BLUE_GLAZED_TERRACOTTA(ConfigBlocksItems.enableGlazedTerracotta, new BlockGlazedTerracotta(11)),
	BROWN_GLAZED_TERRACOTTA(ConfigBlocksItems.enableGlazedTerracotta, new BlockGlazedTerracotta(12)),
	GREEN_GLAZED_TERRACOTTA(ConfigBlocksItems.enableGlazedTerracotta, new BlockGlazedTerracotta(13)),
	RED_GLAZED_TERRACOTTA(ConfigBlocksItems.enableGlazedTerracotta, new BlockGlazedTerracotta(14)),
	BLACK_GLAZED_TERRACOTTA(ConfigBlocksItems.enableGlazedTerracotta, new BlockGlazedTerracotta(15)),




	PRISMARINE_SLAB(ConfigBlocksItems.enablePrismarine, new BaseSlab(false, Material.rock, "prismarine", "prismarine_bricks", "dark_prismarine")
			.setHardness(1.5F).setResistance(6.0F),
			BaseSlabItemBlock.class),
	DOUBLE_PRISMARINE_SLAB(ConfigBlocksItems.enablePrismarine, new BaseSlab(true, Material.rock, "prismarine", "prismarine_bricks", "dark_prismarine")
			.setHardness(1.5F).setResistance(6.0F),
			BaseSlabItemBlock.class),

	PRISMARINE_STAIRS(ConfigBlocksItems.enablePrismarine, new BaseStairs(PRISMARINE_BLOCK.get(), 0).setUnlocalizedNameWithPrefix("prismarine")),
	PRISMARINE_STAIRS_BRICK(ConfigBlocksItems.enablePrismarine, new BaseStairs(PRISMARINE_BLOCK.get(), 1).setUnlocalizedNameWithPrefix("prismarine_brick")),
	PRISMARINE_STAIRS_DARK(ConfigBlocksItems.enablePrismarine, new BaseStairs(PRISMARINE_BLOCK.get(), 2).setUnlocalizedNameWithPrefix("dark_prismarine")),


	PRISMARINE_WALL(ConfigBlocksItems.enablePrismarine, new BaseWall(Material.rock, "prismarine")
			.setHardness(1.5F).setResistance(10.0F)),
	


	MAGMA(ConfigBlocksItems.enableMagmaBlock, new BlockMagma()),
	COMPOSTER(ConfigBlocksItems.enableComposter, new BlockComposter()),
	STONECUTTER(ConfigBlocksItems.enableStonecutter, new BlockStonecutter(), ItemBlockDecorationWorkbench.class),
	FLETCHING_TABLE(ConfigBlocksItems.enableFletchingTable, new BlockFletchingTable(), ItemBlockDecorationWorkbench.class),
	CARTOGRAPHY_TABLE(ConfigBlocksItems.enableCartographyTable, new BlockCartographyTable(), ItemBlockDecorationWorkbench.class),
	LOOM(ConfigBlocksItems.enableLoom, new BlockLoom(), ItemBlockDecorationWorkbench.class),
	HONEY_BLOCK(ConfigBlocksItems.enableHoney, new BlockHoney()),
	HONEYCOMB_BLOCK(ConfigBlocksItems.enableHoney, new BaseBlock(Material.clay).setNames("honeycomb_block")
			.setBlockSound(ModSounds.soundCoralBlock).setHardness(0.6F).setResistance(0.6F)),
	BEEHIVE(ConfigEntities.enableBees, new BlockBeeHive().setHiveType("beehive", true)),
	BEE_NEST(ConfigEntities.enableBees, new BlockBeeHive().setHiveType("bee_nest", true)),
	BREWING_STAND(ConfigBlocksItems.enableBrewingStands, new BlockNewBrewingStand()),
	BEACON(ConfigBlocksItems.enableColourfulBeacons, new BlockNewBeacon()),
	ENCHANTMENT_TABLE(ConfigBlocksItems.enableEnchantingTable, new BlockNewEnchantmentTable()),
	ANVIL(ConfigBlocksItems.enableAnvil, new BlockNewAnvil(), ItemAnvilBlock.class),
	DAYLIGHT_DETECTOR(ConfigBlocksItems.enableInvertedDaylightSensor && ConfigBlocksItems.enableOldBaseDaylightSensor, new BlockNewDaylightSensor()),
	FROSTED_ICE(ConfigEnchantsPotions.enableFrostWalker, new BlockFrostedIce(), null),
	LAVA_CAULDRON(ConfigBlocksItems.enableLavaCauldrons, new BlockLavaCauldron(), null),
	POTION_CAULDRON(ConfigBlocksItems.enablePotionCauldron, new BlockPotionCauldron(), null),
	BUBBLE_COLUMN(ConfigBlocksItems.enableBubbleColumns, new BlockBubbleColumn(new Block[]{Blocks.soul_sand}, new Block[]{MAGMA.get()}), null),

	// the barrel is McFly's (spec 92.4); Iron Chests' metal barrels are gone

	PINK_PETALS(io.github.asgerjon.mcfly.config.McFlyConfig.enableCherryBlocks, new BlockPinkPetals(), ItemBlock.class), //Should not be potable
	//new wood logs

	// the planks, slabs, stairs and the mosaic are McFly's (spec §91.4)

	//legacy fences
	//This is left as-is because fences should really be meta states anyways, so new fences use a different class, so why touch this int-based constructor?
	//Gany, did you waste 4 ID slots just because 1.8 did?

	//new fence, this can just be one block, meta states are fine, the fences above were made by ganymedes01 and not me hence the lack of meta usage
	// TODO: Fix Bamboo Fence Rendering

	//MODDED FENCES

	//legacy buttons

	//new buttons (different ID format)

	//BOP WOOD BUTTONS

	//WITCHERY WOOD BUTTONS

	//legacy pressure plates

	//new pressure plates (different ID format)

	//BOP WOOD PRESSURE PLATE

	//WITCHERY PRESSURE PLATE

	//legacy fence gates

	//new fence gates (different ID format)

	//BOP FENCE GATE

	//WITCHERY FENCE GATE

	//legacy doors

	//new doors (different ID format)

	//BOP DOORS

	//WITCHERY DOORS

	//legacy trapdoors

	//new trapdoors (different ID format)

	//BOP TRAPDOORS

	//WITCHERY TRAPDOORS

	WHITE_BED(ConfigBlocksItems.enableDyedBeds, new BlockDyedBed(0), ItemBlockDyedBed.class),
	ORANGE_BED(ConfigBlocksItems.enableDyedBeds, new BlockDyedBed(1), ItemBlockDyedBed.class),
	MAGENTA_BED(ConfigBlocksItems.enableDyedBeds, new BlockDyedBed(2), ItemBlockDyedBed.class),
	LIGHT_BLUE_BED(ConfigBlocksItems.enableDyedBeds, new BlockDyedBed(3), ItemBlockDyedBed.class),
	YELLOW_BED(ConfigBlocksItems.enableDyedBeds, new BlockDyedBed(4), ItemBlockDyedBed.class),
	LIME_BED(ConfigBlocksItems.enableDyedBeds, new BlockDyedBed(5), ItemBlockDyedBed.class),
	PINK_BED(ConfigBlocksItems.enableDyedBeds, new BlockDyedBed(6), ItemBlockDyedBed.class),
	GRAY_BED(ConfigBlocksItems.enableDyedBeds, new BlockDyedBed(7), ItemBlockDyedBed.class),
	LIGHT_GRAY_BED(ConfigBlocksItems.enableDyedBeds, new BlockDyedBed(8), ItemBlockDyedBed.class),
	CYAN_BED(ConfigBlocksItems.enableDyedBeds, new BlockDyedBed(9), ItemBlockDyedBed.class),
	PURPLE_BED(ConfigBlocksItems.enableDyedBeds, new BlockDyedBed(10), ItemBlockDyedBed.class),
	BLUE_BED(ConfigBlocksItems.enableDyedBeds, new BlockDyedBed(11), ItemBlockDyedBed.class),
	BROWN_BED(ConfigBlocksItems.enableDyedBeds, new BlockDyedBed(12), ItemBlockDyedBed.class),
	GREEN_BED(ConfigBlocksItems.enableDyedBeds, new BlockDyedBed(13), ItemBlockDyedBed.class),
	BLACK_BED(ConfigBlocksItems.enableDyedBeds, new BlockDyedBed(15), ItemBlockDyedBed.class),

	//Legacy "classic" blocks

	ROSE(ConfigTweaks.enableRoses, new BlockOldRose()),
	OLD_GRAVEL(ConfigTweaks.enableOldGravel, new BlockOldGravel()),

	//Unfinished stuff


	//Creative-only stuff

	LIGHT(ConfigBlocksItems.enableLightBlock, new BlockLight()),
	BARRIER(ConfigBlocksItems.enableBarrier, new BlockBarrier()),

	//Mod support
	MODDED_RAW_ORE_BLOCK(Utils.enableModdedRawOres(), new BlockGeneralModdedRawOre(
			"raw_aluminum_block", "raw_tin_block", "raw_silver_block", "raw_lead_block", "raw_nickel_block", "raw_platinum_block", "raw_mythril_block", "raw_uranium_block",
			"raw_thorium_block", "raw_tungsten_block", "raw_titanium_block", "raw_zinc_block", "raw_magnesium_block", "raw_boron_block")),
	RAW_ADAMANTIUM_BLOCK(Utils.enableModdedRawOres(ModsList.SIMPLEORES), new BlockRawAdamantium()),

	MODDED_DEEPSLATE_ORE(Utils.enableModdedDeepslateOres(), new BlockGeneralModdedDeepslateOre("deepslate_aluminum_ore", "deepslate_tin_ore", "deepslate_silver_ore", "deepslate_lead_ore",
			"deepslate_nickel_ore", "deepslate_platinum_ore", "deepslate_mythril_ore", "deepslate_uranium_ore", "deepslate_thorium_ore", "deepslate_tungsten_ore", "deepslate_titanium_ore",
			"deepslate_zinc_ore", "deepslate_magnesium_ore", "deepslate_boron_ore")),

	DEEPSLATE_CERTUS_QUARTZ_ORE(Utils.enableModdedDeepslateOres(ModsList.APPLIED_ENERGISTICS_2), new BlockDeepslateCertusQuartzOre()),
	DEEPSLATE_DRACONIUM_ORE(Utils.enableModdedDeepslateOres(ModsList.DRACONIC_EVOLUTION), new BlockDeepslateDraconiumOre()),
	DEEPSLATE_ADAMANTIUM_ORE(Utils.enableModdedDeepslateOres(ModsList.SIMPLEORES), new BlockDeepslateAdamantiumOre()),

	DEEPSLATE_AM2_ORE(Utils.enableModdedDeepslateOres(ModsList.ARS_MAGICA_2), new BlockDeepslateArsMagicaOre()),
	DEEPSLATE_THAUMCRAFT_ORE(Utils.enableModdedDeepslateOres(ModsList.THAUMCRAFT), new BlockDeepslateThaumcraftOre()),
	DEEPSLATE_BOP_ORE(Utils.enableModdedDeepslateOres(ModsList.BIOMES_O_PLENTY), new BlockDeepslateBiomesOPlentyOre()),
	DEEPSLATE_PROJRED_ORE(Utils.enableModdedDeepslateOres(ModsList.PROJECT_RED_EXPLORATION), new BlockDeepslateProjectRedOre()),
	DEEPSLATE_BLUEPOWER_ORE(Utils.enableModdedDeepslateOres(ModsList.BLUEPOWER), new BlockDeepslateBluePowerOre()),
	DEEPSLATE_DBC_ORE(Utils.enableModdedDeepslateOres(ModsList.DRAGON_BLOCK_C), new BlockDeepslateDragonBlockOre()),
	DEEPSLATE_DQ_ORE(Utils.enableModdedDeepslateOres(ModsList.DRAGON_QUEST), new BlockDeepslateDragonQuestOre()),
	// glow lichen and cave vines are McFly's (spec §90.3)
	;



	public static final ModBlocks[] BEDS = new ModBlocks[]{WHITE_BED, ORANGE_BED, MAGENTA_BED, LIGHT_BLUE_BED, YELLOW_BED, LIME_BED, PINK_BED, GRAY_BED, LIGHT_GRAY_BED, CYAN_BED,
			PURPLE_BED, BLUE_BED, BROWN_BED, GREEN_BED, BLACK_BED};
	public static final ModBlocks[] TERRACOTTA = new ModBlocks[]{WHITE_GLAZED_TERRACOTTA, ORANGE_GLAZED_TERRACOTTA, MAGENTA_GLAZED_TERRACOTTA, LIGHT_BLUE_GLAZED_TERRACOTTA,
			YELLOW_GLAZED_TERRACOTTA, LIME_GLAZED_TERRACOTTA, PINK_GLAZED_TERRACOTTA, GRAY_GLAZED_TERRACOTTA, LIGHT_GRAY_GLAZED_TERRACOTTA, CYAN_GLAZED_TERRACOTTA,
			PURPLE_GLAZED_TERRACOTTA, BLUE_GLAZED_TERRACOTTA, BROWN_GLAZED_TERRACOTTA, GREEN_GLAZED_TERRACOTTA, RED_GLAZED_TERRACOTTA, BLACK_GLAZED_TERRACOTTA};

	/*
	 * Stand-in static final fields because some mods incorrectly referenced my code directly.
	 * They should be using GameRegistry.findBlock but it is what it is I guess.
	 */

	//Immersive Cavegen
	@Deprecated
	public static final Block deepslate = io.github.asgerjon.mcfly.registry.McFlyBlocks.DEEPSLATE.get();

	//D-Mod
	@Deprecated
	public static final Block sweet_berry_bush = SWEET_BERRY_BUSH.get();

	//WTF Cave Biomes
	@Deprecated
	public static final Block prismarine = PRISMARINE_BLOCK.get();
	@Deprecated
	public static final Block sea_lantern = SEA_LANTERN.get();
	@Deprecated
	// WTF Cave Biomes reads it; McFly's red sandstone (spec §89, step D)
	public static final Block red_sandstone = io.github.asgerjon.mcfly.registry.McFlyBlocks.RED_SANDSTONE.get();

	public static final ModBlocks[] VALUES = values();
	public static void init() {
		for (ModBlocks block : VALUES) {
			if (block.isEnabled()) {
				if (block.getItemBlock() != null || !block.getHasItemBlock()) {
					GameRegistry.registerBlock(block.get(), block.getItemBlock(), block.name().toLowerCase());
					//This part is used if the getItemBlock() is not ItemBlock.class, so we register a custom ItemBlock class as the ItemBlock
					//It is also used if the getItemBlock() == null and getHasItemBlock() is false, meaning we WANT to register it as null, making the block have no inventory item.
				} else {
					GameRegistry.registerBlock(block.get(), block.name().toLowerCase());
					//Used if getItemBlock() == null but getHasItemBlock() is true, registering it with a default inventory item.
				}
			}
		}
	}

	private final boolean isEnabled;
	private final Block theBlock;
	/**
	 * null == default ItemBlock
	 */
	private final Class<? extends ItemBlock> itemBlock;
	/**
	 * Determines if we should register the block with an ItemBlock.
	 * Set to false when the constructor that specifies the ItemBlock is specifically set to false.
	 */
	private boolean hasItemBlock;

	ModBlocks(Boolean enabled, Block block) {
		this(enabled, block,
				block instanceof BaseSlab ? BaseSlabItemBlock.class
//						: block instanceof BaseWall ? BaseWallItemBlock.class
						: block instanceof BaseFlower ? BasePotableItemBlock.class
						: block instanceof BaseLeaves ? BaseLeavesItemBlock.class
						: block instanceof ISubBlocksBlock ? BaseItemBlock.class
						: null);
		hasItemBlock = true;
	}

	ModBlocks(Boolean enabled, Block block, Class<? extends ItemBlock> iblock) {
		isEnabled = enabled;
		theBlock = block;
		itemBlock = iblock;
		hasItemBlock = iblock != null;
	}

	/**
	 * If this is false, the block is initialized without an inventory item, or ItemBlock.
	 */
	public boolean getHasItemBlock() {
		return hasItemBlock;
	}

	public Block get() {
		return theBlock;
	}

	public Class<? extends ItemBlock> getItemBlock() {
		return itemBlock;
	}

	public Item getItem() {
		return Item.getItemFromBlock(get());
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public ItemStack newItemStack() {
		return newItemStack(1);
	}

	public ItemStack newItemStack(int count) {
		return newItemStack(count, 0);
	}

	public ItemStack newItemStack(int count, int meta) {
		return new ItemStack(this.get(), count, meta);
	}
}
