package io.github.asgerjon.mcfly.config;

import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * McFly's options (SPEC-modern-movement.md §79, B2): one file, `config/mcfly.cfg`, grouped by system. Read by EFR's
 * coremod before mixins are chosen (in this fork; by McFly's own once it has its own repository). The first time,
 * the values EFR's files held for these options are carried in and taken out of them. With modernOptions on, the
 * client's own choices live in options.txt as 26.3 keeps them (§54.10), their entries here read only when it has none.
 * The in-game config screen lists these under McFly (EFR's screen, `ConfigGUI`).
 */
public final class McFlyConfig {

	public static final String FILE_NAME = "mcfly.cfg";
	/** The mod id config screens and their change events go by (EFR's in this fork). */
	public static final String SCREEN_MOD_ID = "etfuturum";

	/** Poses, sneaking, swimming, crawling and water (the spec's parts 1 to 3). */
	static final String MOVEMENT = "movement";
	/** The elytra (part 4). */
	static final String ELYTRA = "elytra";
	/** The off-hand and the main hand (§36, §37). */
	static final String OFFHAND = "off-hand";
	/** The combat update (§46). */
	static final String COMBAT = "combat";
	/** Modern commands and chat (§39 to §52). */
	static final String COMMANDS = "commands and chat";
	/** Modern options and skins (§54). */
	static final String OPTIONS = "options and skins";
	/** The client's own choices, kept in options.txt with modernOptions on (§54.10); not on a server. */
	static final String CLIENT = "client";
	/** The End as 26.3 (§76). */
	static final String END = "the end";
	/** Effects and potions by name (§69, §70). */
	static final String EFFECTS = "effects and potions";
	/** The trident and its enchantments (§58). */
	static final String TRIDENT = "trident";
	/** Mobs (§60, §67, §68) and their modern behaviour. */
	static final String MOBS = "mobs";
	/** Blocks and items. */
	static final String BLOCKS = "blocks and items";
	/** What the player does with blocks, items and screens. */
	static final String PLAYER = "player";
	/** The world: generation, weather, explosions, portals. */
	static final String WORLD = "world";
	/** Fixes to 1.7.10's own behaviour. */
	static final String FIXES = "fixes";

	// ---------------------------------------------------------------- movement

	public static boolean enableDepthStrider;
	public static int depthStriderID;
	public static boolean enableFloatingItems;
	public static boolean enableKelp;
	public static boolean enableModernBreathing;
	public static boolean enableModernSneaking;
	public static boolean enableModernSwimming;
	public static int poseDataWatcherId;

	// ---------------------------------------------------------------- elytra

	public static boolean shiftClickEquipsElytra;

	// ---------------------------------------------------------------- off-hand

	public static boolean enableOffhand;
	public static int offhandDataWatcherId;

	// ---------------------------------------------------------------- combat

	public static boolean arrowsInheritMotion;
	public static boolean enableShield;
	public static boolean modernCombat;
	public static boolean enableSweepingEdge;
	public static int sweepingEdgeID;

	// ---------------------------------------------------------------- commands and chat

	public static boolean chatHistoryAcrossRestarts;
	public static boolean chatHistoryBetweenWorlds;
	public static boolean chatLogBetweenWorlds;
	public static boolean commandBlockCreativeOnly;
	public static boolean coordinateSuggestions;
	public static boolean giveAbove64;
	public static boolean modernChat;

	// ---------------------------------------------------------------- options and skins

	public static boolean firstPersonCameraAtEyes;
	public static boolean fovEffects;
	public static boolean guiScaleWheel;
	public static boolean modernHeldItems;
	public static boolean modernInventoryFigure;
	public static boolean modernKeyRepeat;
	public static boolean modernOptions;
	public static boolean offlineSkins;
	public static boolean skinCustomization;
	public static int skinPartsDataWatcherId;

	// ---------------------------------------------------------------- client

	public static String attackIndicator;
	public static boolean commandSuggestions;
	public static float fovEffectScale;
	public static boolean mainHandLeft;
	public static boolean skinCape;
	public static boolean skinHat;
	public static boolean skinJacket;
	public static boolean skinLeftPantsLeg;
	public static boolean skinLeftSleeve;
	public static boolean skinRightPantsLeg;
	public static boolean skinRightSleeve;

	// ---------------------------------------------------------------- the end

	public static boolean chorusOnlyOnOuterIslands;
	public static int endBarrensID;
	public static boolean endCreditsOnce;
	public static int endHighlandsID;
	public static int endMidlandsID;
	public static boolean modernEnd;
	public static int smallEndIslandsID;

	// ---------------------------------------------------------------- effects and potions

	public static boolean effectPanelAlphaFix;
	public static boolean enableLuck;
	public static boolean enableTrialEffects;
	public static boolean modernPotionColors;
	public static boolean namedEffects;
	public static boolean noPotionGlint;
	public static boolean potionsByName;

	// ---------------------------------------------------------------- trident

	public static boolean enableChanneling;
	public static int channelingID;
	public static boolean enableTrident;
	public static boolean enableImpaling;
	public static int impalingID;
	public static boolean enableLoyalty;
	public static int loyaltyID;
	public static boolean modernLightning;
	public static boolean enableRiptide;
	public static int riptideID;

	// ---------------------------------------------------------------- mobs

	public static int aggressiveDataWatcherId;
	public static boolean enableGlowSquidSpawns;
	public static boolean enableGlowSquids;
	public static boolean enablePhantomSpawns;
	public static boolean enablePhantoms;
	public static boolean enablePolarBearSpawns;
	public static boolean enablePolarBears;
	public static boolean modernHorsesInWater;
	public static boolean modernMobArms;
	public static boolean modernMobSpawnLight;
	public static boolean modernPanic;
	public static boolean undeadHorseLeads;

	// ---------------------------------------------------------------- blocks and items

	public static boolean enableBanners;
	public static boolean enableShulkerBoxes;
	public static boolean enableChorusBlocks;
	public static boolean enableElytra;
	public static boolean enableObservers;
	public static boolean enableCopper;
	public static boolean enableLightningRod;
	public static boolean enableRawOres;
	public static boolean enableNewElytraTakeoffLogic;
	public static int elytraDataWatcherFlag;
	public static boolean enableSpectatorMode;
	public static boolean enableLingeringPotions;
	public static boolean enableTippedArrows;
	public static boolean enableBlackstone;
	public static boolean enableBasalt;
	public static boolean enableNetherGold;
	public static boolean enableCryingObsidian;
	public static boolean enableRedNetherBricks;
	public static boolean enableQuartzBricks;
	public static boolean enableNetherite;
	public static boolean enableSmithingTable;
	public static boolean enableChain;
	public static boolean enableLantern;
	public static boolean enableSoulLighting;
	public static boolean enableSoulSoil;
	public static boolean enableTarget;
	public static boolean enableDeepslate;
	public static boolean enableDeepslateOres;
	public static boolean enableStones;
	public static boolean disableBaseBountifulStonesOnly;
	public static boolean enableRedSandstone;
	public static boolean enableSmoothStone;
	public static boolean enableSmoothSandstone;
	public static boolean enableSmoothQuartz;
	public static boolean enableExtraVanillaSlabs;
	public static boolean enableExtraVanillaStairs;
	public static boolean enableExtraVanillaWalls;
	public static boolean enableStoneBrickRecipes;
	public static boolean enableTuff;
	public static boolean enableCalcite;
	public static boolean enableVanillaWoodRedstone;
	public static boolean enableNewWoodRedstone;
	public static boolean enableVanillaSigns;
	public static boolean enableNewSigns;
	public static boolean enableVanillaDoors;
	public static boolean enableNewDoors;
	public static boolean enableVanillaTrapdoors;
	public static boolean enableNewTrapdoors;
	public static boolean enableIronTrapdoor;
	public static boolean enableDoorRecipeBuffs;
	public static boolean enableFloatingTrapDoors;
	public static boolean enableVanillaFences;
	public static boolean enableNewFences;
	public static boolean enableVanillaGates;
	public static boolean enableNewGates;
	public static boolean enableStrippedLogs;
	public static boolean enableBarkLogs;
	public static boolean enableCherryBlocks;
	public static boolean enableBambooBlocks;
	public static boolean enableGlowLichen;
	public static boolean enableGlowBerries;
	public static boolean enableAmethyst;
	public static int buddingAmethystMode;
	public static boolean enableMud;
	public static boolean enableMossAzalea;
	public static boolean enableMangrove;
	public static boolean enableCherryTrees;
	public static boolean enableDripstone;
	public static boolean enableLushCavePlants;
	public static boolean enableSculk;
	public static boolean enableNetherPlants;
	public static boolean enableNetherwartBlock;
	public static boolean beaconBeamThroughTransparentBlocks;
	public static boolean buttonsOnFullFaces;
	public static boolean dropVehiclesTogether;
	// the boats (spec 91.12, step J), EFR's options
	public static boolean enableNewBoats;
	public static boolean replaceOldBoats;
	public static boolean newBoatPassengerSeat;
	public static float newBoatMaxLandSpeed;
	public static float newBoatSpeed;
	public static String[] newBoatEntityBlacklist = new String[0];
	public static boolean newBoatEntityBlacklistAsWhitelist;
	public static boolean stopBoatRotationLock;
	public static boolean enableBannerPatterns;
	public static boolean enableBreezeRod;
	public static boolean enableGlowInkSac;
	public static boolean enableGlowItemFrames;
	public static boolean enableLodestone;
	public static boolean enablePowderSnow;
	public static boolean enableSpyglass;
	public static boolean enableTurtleShell;
	public static boolean modernCauldrons;
	public static boolean modernChestPlacement;
	public static boolean modernItemFrames;
	public static boolean modernPistonEvents;
	public static boolean modernRarity;
	public static boolean modernSignText;
	public static boolean modernStepSounds;
	public static boolean modernTntMinecarts;
	public static boolean modernWitherSkulls;
	public static boolean netheriteUpgradeTemplate;

	// ---------------------------------------------------------------- player

	public static boolean itemsBackOnClose;
	public static boolean modernAnvilCosts;
	public static boolean modernBedUse;
	public static boolean modernBlockUsePackets;
	public static boolean modernCreativeFilling;
	public static boolean modernCreativeInventoryTab;
	public static boolean modernHunger;
	public static boolean modernPlacementShape;
	public static boolean pickBlockFromInventory;
	public static boolean placementFacingByLook;
	public static boolean rightClickArmorSwap;
	public static boolean shiftClickIntoCraftingGrid;

	// ---------------------------------------------------------------- world

	public static boolean modernExplosionOrder;
	public static boolean modernStrongholds;
	public static boolean modernWeatherPace;
	public static boolean portalShatters;
	public static int strongholdCount;

	// ---------------------------------------------------------------- fixes

	public static boolean arrowHarmingAfterTotemFix;
	public static boolean connectionAcceptFix;
	public static boolean thinBlockStepSoundFix;

	/**
	 * With modernOptions on, the client's options.txt keeps the client options (§54.10): the client sets these to save
	 * options.txt and to read those values from it again. Null on a server.
	 */
	public static Runnable optionsTxtSave, optionsTxtLoad;

	/** Where each option was in EFR's files: its file (without `.cfg`) and category, for the one-time carry. */
	private static final Map<String, String[]> FROM_EFR = new LinkedHashMap<>();

	static {
		FROM_EFR.put("aggressiveDataWatcherId", new String[] {"functions", "settings"});
		FROM_EFR.put("arrowHarmingAfterTotemFix", new String[] {"functions", "settings"});
		FROM_EFR.put("arrowsInheritMotion", new String[] {"mixins", "backported features"});
		FROM_EFR.put("attackIndicator", new String[] {"functions", "client"});
		FROM_EFR.put("enableBanners", new String[] {"blocksitems", "misc blocks"});
		FROM_EFR.put("enableShulkerBoxes", new String[] {"blocksitems", "function blocks"});
		FROM_EFR.put("enableChorusBlocks", new String[] {"blocksitems", "natural blocks"});
		FROM_EFR.put("enableElytra", new String[] {"mixins", "backported features"});
		FROM_EFR.put("enableObservers", new String[] {"mixins", "backported features"});
		FROM_EFR.put("enableCopper", new String[] {"blocksitems", "natural blocks"});
		FROM_EFR.put("enableLightningRod", new String[] {"blocksitems", "function blocks"});
		FROM_EFR.put("enableRawOres", new String[] {"blocksitems", "misc items"});
		FROM_EFR.put("enableNewElytraTakeoffLogic", new String[] {"mixins", "backported features"});
		FROM_EFR.put("elytraDataWatcherFlag", new String[] {"functions", "settings"});
		FROM_EFR.put("enableSpectatorMode", new String[] {"mixins", "backported features"});
		FROM_EFR.put("enableLingeringPotions", new String[] {"blocksitems", "misc items"});
		FROM_EFR.put("enableTippedArrows", new String[] {"blocksitems", "misc items"});
		FROM_EFR.put("enableBlackstone", new String[] {"blocksitems", "misc blocks"});
		FROM_EFR.put("enableBasalt", new String[] {"blocksitems", "natural blocks"});
		FROM_EFR.put("enableNetherGold", new String[] {"blocksitems", "natural blocks"});
		FROM_EFR.put("enableCryingObsidian", new String[] {"blocksitems", "natural blocks"});
		FROM_EFR.put("enableRedNetherBricks", new String[] {"blocksitems", "misc blocks"});
		FROM_EFR.put("enableQuartzBricks", new String[] {"blocksitems", "misc blocks"});
		FROM_EFR.put("enableNetherite", new String[] {"blocksitems", "natural blocks"});
		FROM_EFR.put("enableSmithingTable", new String[] {"blocksitems", "function blocks"});
		FROM_EFR.put("enableChain", new String[] {"blocksitems", "misc blocks"});
		FROM_EFR.put("enableLantern", new String[] {"blocksitems", "misc blocks"});
		FROM_EFR.put("enableSoulLighting", new String[] {"blocksitems", "natural blocks"});
		FROM_EFR.put("enableSoulSoil", new String[] {"blocksitems", "natural blocks"});
		FROM_EFR.put("enableTarget", new String[] {"blocksitems", "function blocks"});
		FROM_EFR.put("enableDeepslate", new String[] {"blocksitems", "natural blocks"});
		FROM_EFR.put("enableDeepslateOres", new String[] {"blocksitems", "natural blocks"});
		FROM_EFR.put("enableStones", new String[] {"blocksitems", "natural blocks"});
		FROM_EFR.put("disableBaseBountifulStonesOnly", new String[] {"modcompat", "misc"});
		FROM_EFR.put("enableRedSandstone", new String[] {"blocksitems", "natural blocks"});
		FROM_EFR.put("enableSmoothStone", new String[] {"blocksitems", "misc blocks"});
		FROM_EFR.put("enableSmoothSandStone", new String[] {"blocksitems", "misc blocks"});
		FROM_EFR.put("enableSmoothQuartz", new String[] {"blocksitems", "misc blocks"});
		FROM_EFR.put("enableExtraVanillaSlabs", new String[] {"blocksitems", "misc blocks"});
		FROM_EFR.put("enableExtraVanillaStairs", new String[] {"blocksitems", "misc blocks"});
		FROM_EFR.put("enableExtraVanillaWalls", new String[] {"blocksitems", "misc blocks"});
		FROM_EFR.put("enableStoneBrickRecipes", new String[] {"functions", "changes"});
		FROM_EFR.put("enableTuff", new String[] {"blocksitems", "natural blocks"});
		FROM_EFR.put("enableCalcite", new String[] {"blocksitems", "natural blocks"});
		FROM_EFR.put("enableVanillaWoodRedstone", new String[] {"blocksitems", "function blocks"});
		FROM_EFR.put("enableVanillaSigns", new String[] {"blocksitems", "function blocks"});
		FROM_EFR.put("enableNewSigns", new String[] {"blocksitems", "function blocks"});
		FROM_EFR.put("enableNewWoodRedstone", new String[] {"blocksitems", "function blocks"});
		FROM_EFR.put("enableVanillaDoors", new String[] {"blocksitems", "function blocks"});
		FROM_EFR.put("enableNewDoors", new String[] {"blocksitems", "function blocks"});
		FROM_EFR.put("enableVanillaTrapdoors", new String[] {"blocksitems", "function blocks"});
		FROM_EFR.put("enableNewTrapdoors", new String[] {"blocksitems", "function blocks"});
		FROM_EFR.put("enableIronTrapdoor", new String[] {"blocksitems", "function blocks"});
		FROM_EFR.put("enableDoorRecipeBuffs", new String[] {"functions", "changes"});
		FROM_EFR.put("enableFloatingTrapDoors", new String[] {"functions", "changes"});
		FROM_EFR.put("enableVanillaFences", new String[] {"blocksitems", "misc blocks"});
		FROM_EFR.put("enableNewFences", new String[] {"blocksitems", "misc blocks"});
		FROM_EFR.put("enableVanillaGates", new String[] {"blocksitems", "misc blocks"});
		FROM_EFR.put("enableNewGates", new String[] {"blocksitems", "misc blocks"});
		FROM_EFR.put("enableStrippedLogs", new String[] {"blocksitems", "misc blocks"});
		FROM_EFR.put("enableBarkLogs", new String[] {"blocksitems", "misc blocks"});
		FROM_EFR.put("enableCherryBlocks", new String[] {"blocksitems", "natural blocks"});
		FROM_EFR.put("enableBambooBlocks", new String[] {"blocksitems", "natural blocks"});
		FROM_EFR.put("enableNewBoats", new String[] {"blocksitems", "entity items"});
		FROM_EFR.put("replaceOldBoats", new String[] {"blocksitems", "entity items"});
		FROM_EFR.put("newBoatPassengerSeat", new String[] {"blocksitems", "entity items"});
		FROM_EFR.put("newBoatMaxLandSpeed", new String[] {"blocksitems", "entity items"});
		FROM_EFR.put("newBoatSpeed", new String[] {"blocksitems", "entity items"});
		FROM_EFR.put("newBoatEntityBlacklist", new String[] {"blocksitems", "entity items"});
		FROM_EFR.put("newBoatEntityBlacklistAsWhitelist", new String[] {"blocksitems", "entity items"});
		FROM_EFR.put("stopBoatRotationLock", new String[] {"tweaks", "custom tweaks"});
		FROM_EFR.put("enableGlowLichen", new String[] {"blocksitems", "natural blocks"});
		FROM_EFR.put("enableGlowBerries", new String[] {"blocksitems", "natural blocks"});
		FROM_EFR.put("enableAmethyst", new String[] {"blocksitems", "natural blocks"});
		FROM_EFR.put("buddingAmethystMode", new String[] {"world", "generation"});
		FROM_EFR.put("enableMud", new String[] {"blocksitems", "natural blocks"});
		FROM_EFR.put("enableNetherwartBlock", new String[] {"blocksitems", "natural blocks"});
		FROM_EFR.put("beaconBeamThroughTransparentBlocks", new String[] {"mixins", "backported features"});
		FROM_EFR.put("buttonsOnFullFaces", new String[] {"mixins", "backported features"});
		FROM_EFR.put("channeling", new String[] {"enchantspotions", "enchantments"});
		FROM_EFR.put("channelingID", new String[] {"enchantspotions", "enchantments"});
		FROM_EFR.put("chatHistoryAcrossRestarts", new String[] {"mixins", "backported features"});
		FROM_EFR.put("chatHistoryBetweenWorlds", new String[] {"mixins", "backported features"});
		FROM_EFR.put("chatLogBetweenWorlds", new String[] {"mixins", "backported features"});
		FROM_EFR.put("chorusOnlyOnOuterIslands", new String[] {"world", "generation"});
		FROM_EFR.put("commandBlockCreativeOnly", new String[] {"mixins", "backported features"});
		FROM_EFR.put("commandSuggestions", new String[] {"functions", "client"});
		FROM_EFR.put("connectionAcceptFix", new String[] {"mixins", "fixes"});
		FROM_EFR.put("coordinateSuggestions", new String[] {"mixins", "backported features"});
		FROM_EFR.put("depthStrider", new String[] {"enchantspotions", "enchantments"});
		FROM_EFR.put("depthStriderID", new String[] {"enchantspotions", "enchantments"});
		FROM_EFR.put("dropVehiclesTogether", new String[] {"functions", "changes"});
		FROM_EFR.put("effectPanelAlphaFix", new String[] {"mixins", "backported features"});
		FROM_EFR.put("enableBannerPatterns", new String[] {"blocksitems", "misc items"});
		FROM_EFR.put("enableBreezeRod", new String[] {"blocksitems", "misc items"});
		FROM_EFR.put("enableFloatingItems", new String[] {"mixins", "backported features"});
		FROM_EFR.put("enableGlowInkSac", new String[] {"blocksitems", "misc items"});
		FROM_EFR.put("enableGlowItemFrames", new String[] {"blocksitems", "misc items"});
		FROM_EFR.put("enableGlowSquidSpawns", new String[] {"entities", "neutral"});
		FROM_EFR.put("enableGlowSquids", new String[] {"entities", "neutral"});
		FROM_EFR.put("enableKelp", new String[] {"blocksitems", "natural blocks"});
		FROM_EFR.put("enableLodestone", new String[] {"blocksitems", "function blocks"});
		FROM_EFR.put("enableLuck", new String[] {"enchantspotions", "potions"});
		FROM_EFR.put("enableModernBreathing", new String[] {"mixins", "backported features"});
		FROM_EFR.put("enableModernSneaking", new String[] {"mixins", "backported features"});
		FROM_EFR.put("enableModernSwimming", new String[] {"mixins", "backported features"});
		FROM_EFR.put("enableOffhand", new String[] {"mixins", "backported features"});
		FROM_EFR.put("enablePhantomSpawns", new String[] {"entities", "hostile"});
		FROM_EFR.put("enablePhantoms", new String[] {"entities", "hostile"});
		FROM_EFR.put("enablePolarBearSpawns", new String[] {"entities", "neutral"});
		FROM_EFR.put("enablePolarBears", new String[] {"entities", "neutral"});
		FROM_EFR.put("enablePowderSnow", new String[] {"blocksitems", "function blocks"});
		FROM_EFR.put("enableShield", new String[] {"blocksitems", "misc items"});
		FROM_EFR.put("enableSpyglass", new String[] {"blocksitems", "misc items"});
		FROM_EFR.put("enableTrialEffects", new String[] {"enchantspotions", "potions"});
		FROM_EFR.put("enableTrident", new String[] {"blocksitems", "misc items"});
		FROM_EFR.put("enableTurtleShell", new String[] {"blocksitems", "misc items"});
		FROM_EFR.put("endBarrensID", new String[] {"world", "biomes"});
		FROM_EFR.put("endCreditsOnce", new String[] {"mixins", "backported features"});
		FROM_EFR.put("endHighlandsID", new String[] {"world", "biomes"});
		FROM_EFR.put("endMidlandsID", new String[] {"world", "biomes"});
		FROM_EFR.put("firstPersonCameraAtEyes", new String[] {"mixins", "backported features"});
		FROM_EFR.put("fovEffectScale", new String[] {"functions", "client"});
		FROM_EFR.put("fovEffects", new String[] {"mixins", "backported features"});
		FROM_EFR.put("giveAbove64", new String[] {"mixins", "backported features"});
		FROM_EFR.put("guiScaleWheel", new String[] {"mixins", "misc"});
		FROM_EFR.put("impaling", new String[] {"enchantspotions", "enchantments"});
		FROM_EFR.put("impalingID", new String[] {"enchantspotions", "enchantments"});
		FROM_EFR.put("itemsBackOnClose", new String[] {"mixins", "backported features"});
		FROM_EFR.put("loyalty", new String[] {"enchantspotions", "enchantments"});
		FROM_EFR.put("loyaltyID", new String[] {"enchantspotions", "enchantments"});
		FROM_EFR.put("mainHandLeft", new String[] {"functions", "client"});
		FROM_EFR.put("modernAnvilCosts", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernBedUse", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernBlockUsePackets", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernCauldrons", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernChat", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernChestPlacement", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernCombat", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernCreativeFilling", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernCreativeInventoryTab", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernEnd", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernExplosionOrder", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernHeldItems", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernHorsesInWater", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernHunger", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernInventoryFigure", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernItemFrames", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernKeyRepeat", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernLightning", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernMobArms", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernMobSpawnLight", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernOptions", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernPanic", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernPistonEvents", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernPlacementShape", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernPotionColors", new String[] {"enchantspotions", "potions"});
		FROM_EFR.put("modernRarity", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernSignText", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernStepSounds", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernStrongholds", new String[] {"world", "generation"});
		FROM_EFR.put("modernTntMinecarts", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernWeatherPace", new String[] {"mixins", "backported features"});
		FROM_EFR.put("modernWitherSkulls", new String[] {"mixins", "backported features"});
		FROM_EFR.put("namedEffects", new String[] {"mixins", "backported features"});
		FROM_EFR.put("netheriteUpgradeTemplate", new String[] {"blocksitems", "function blocks"});
		FROM_EFR.put("noPotionGlint", new String[] {"mixins", "backported features"});
		FROM_EFR.put("offhandDataWatcherId", new String[] {"functions", "settings"});
		FROM_EFR.put("offlineSkins", new String[] {"mixins", "misc"});
		FROM_EFR.put("pickBlockFromInventory", new String[] {"mixins", "backported features"});
		FROM_EFR.put("placementFacingByLook", new String[] {"mixins", "backported features"});
		FROM_EFR.put("portalShatters", new String[] {"mixins", "backported features"});
		FROM_EFR.put("poseDataWatcherId", new String[] {"functions", "settings"});
		FROM_EFR.put("potionsByName", new String[] {"mixins", "backported features"});
		FROM_EFR.put("rightClickArmorSwap", new String[] {"mixins", "backported features"});
		FROM_EFR.put("riptide", new String[] {"enchantspotions", "enchantments"});
		FROM_EFR.put("riptideID", new String[] {"enchantspotions", "enchantments"});
		FROM_EFR.put("shiftClickEquipsElytra", new String[] {"mixins", "backported features"});
		FROM_EFR.put("shiftClickIntoCraftingGrid", new String[] {"mixins", "backported features"});
		FROM_EFR.put("skinCape", new String[] {"functions", "client"});
		FROM_EFR.put("skinCustomization", new String[] {"mixins", "backported features"});
		FROM_EFR.put("skinHat", new String[] {"functions", "client"});
		FROM_EFR.put("skinJacket", new String[] {"functions", "client"});
		FROM_EFR.put("skinLeftPantsLeg", new String[] {"functions", "client"});
		FROM_EFR.put("skinLeftSleeve", new String[] {"functions", "client"});
		FROM_EFR.put("skinPartsDataWatcherId", new String[] {"functions", "settings"});
		FROM_EFR.put("skinRightPantsLeg", new String[] {"functions", "client"});
		FROM_EFR.put("skinRightSleeve", new String[] {"functions", "client"});
		FROM_EFR.put("smallEndIslandsID", new String[] {"world", "biomes"});
		FROM_EFR.put("strongholdCount", new String[] {"world", "generation"});
		FROM_EFR.put("sweepingEdge", new String[] {"enchantspotions", "enchantments"});
		FROM_EFR.put("sweepingEdgeID", new String[] {"enchantspotions", "enchantments"});
		FROM_EFR.put("thinBlockStepSoundFix", new String[] {"mixins", "fixes"});
		FROM_EFR.put("undeadHorseLeads", new String[] {"mixins", "backported features"});
	}

	private static Configuration file;
	private static boolean server;

	private McFlyConfig() {
	}

	/**
	 * Reads `mcfly.cfg` in the game's config folder (made the first time, with what EFR's files held carried in), on a
	 * server without the client category.
	 */
	public static synchronized void load(File configDir, boolean dedicatedServer) {
		server = dedicatedServer;
		File ours = new File(configDir, FILE_NAME);
		// what EFR's files still hold of ours: all of it the first time, and each option moved later its first time (spec §80)
		Map<String, String> carried = takeFromEfr(configDir);
		file = new Configuration(ours);
		file.load();
		// carried only while ours lacks it (its first time): EFR still declares some of ours and puts them back with its
		// defaults each start, which would otherwise undo what was set in ours
		java.util.Set<String> had = new java.util.HashSet<>();
		for (String category : file.getCategoryNames()) {
			had.addAll(file.getCategory(category).keySet());
		}
		carried.keySet().removeAll(had);
		read(file);
		if (!carried.isEmpty()) {
			for (Map.Entry<String, String> entry : carried.entrySet()) {
				for (String category : file.getCategoryNames()) {
					if (file.getCategory(category).containsKey(entry.getKey())) {
						Property property = file.getCategory(category).get(entry.getKey());
						if (property.isList()) {
							property.set(entry.getValue().isEmpty() ? new String[0] : entry.getValue().split(","));
						} else {
							property.set(entry.getValue());
						}
					}
				}
			}
			read(file);
		}
		if (server && file.hasCategory(CLIENT)) {
			file.removeCategory(file.getCategory(CLIENT));
		}
		if (file.hasChanged() || !ours.exists()) {
			file.save();
		}
	}

	/**
	 * The values EFR's files (`etfuturum/*.cfg` in the config folder) hold for our options, by name, each taken out of
	 * its file (EFR's own options stay); those files saved again if anything was taken.
	 */
	public static Map<String, String> takeFromEfr(File configDir) {
		Map<String, String> carried = new LinkedHashMap<>();
		Map<String, Configuration> efrFiles = new HashMap<>();
		for (Map.Entry<String, String[]> entry : FROM_EFR.entrySet()) {
			String[] where = entry.getValue();
			Configuration efr = efrFiles.get(where[0]);
			if (efr == null) {
				File efrFile = new File(configDir, "etfuturum" + File.separator + where[0] + ".cfg");
				if (!efrFile.isFile()) {
					continue;
				}
				efr = new Configuration(efrFile);
				efr.load();
				efrFiles.put(where[0], efr);
			}
			if (efr.hasCategory(where[1]) && efr.getCategory(where[1]).containsKey(entry.getKey())) {
				ConfigCategory category = efr.getCategory(where[1]);
				Property property = category.get(entry.getKey());
				carried.put(entry.getKey(), property.isList() ? String.join(",", property.getStringList()) : property.getString());
				category.remove(entry.getKey());
				if (category.isEmpty()) {
					efr.removeCategory(category);
				}
			}
		}
		for (Configuration efr : efrFiles.values()) {
			if (efr.hasChanged()) {
				efr.save();
			}
		}
		return carried;
	}

	/** Every option's value from the file (made with its default where missing). */
	private static void read(Configuration c) {
		c.setCategoryComment(MOVEMENT, "Poses, sneaking, swimming, crawling and water (the spec's parts 1 to 3).");
		c.setCategoryComment(ELYTRA, "The elytra (part 4).");
		c.setCategoryComment(OFFHAND, "The off-hand and the main hand (§36, §37).");
		c.setCategoryComment(COMBAT, "The combat update (§46).");
		c.setCategoryComment(COMMANDS, "Modern commands and chat (§39 to §52).");
		c.setCategoryComment(OPTIONS, "Modern options and skins (§54).");
		c.setCategoryComment(CLIENT, "The client's own choices, kept in options.txt with modernOptions on (§54.10); not on a server.");
		c.setCategoryComment(END, "The End as 26.3 (§76).");
		c.setCategoryComment(EFFECTS, "Effects and potions by name (§69, §70).");
		c.setCategoryComment(TRIDENT, "The trident and its enchantments (§58).");
		c.setCategoryComment(MOBS, "Mobs (§60, §67, §68) and their modern behaviour.");
		c.setCategoryComment(BLOCKS, "Blocks and items.");
		c.setCategoryComment(PLAYER, "What the player does with blocks, items and screens.");
		c.setCategoryComment(WORLD, "The world: generation, weather, explosions, portals.");
		c.setCategoryComment(FIXES, "Fixes to 1.7.10's own behaviour.");

		enableDepthStrider = c.getBoolean("depthStrider", MOVEMENT, true, "Faster movement in water. Needs enableModernSwimming (movement), since the effect is part of the modern water movement.");
		depthStriderID = c.getInt("depthStriderID", MOVEMENT, 203, 0, Short.MAX_VALUE, "");
		enableFloatingItems = c.getBoolean("enableFloatingItems", MOVEMENT, true, "Items and experience orbs float in water, and items in lava, as since 1.13: no gravity while more than 0.1 blocks deep, a slight lift up to 0.06 blocks per tick, extra horizontal drag. This is what lets bubble columns throw items like modern. While on, it replaces the item floating of adjustedLiquidPhysics, which would hold items to 0.05 blocks per tick.");
		enableKelp = c.getBoolean("enableKelp", MOVEMENT, true, "Kelp, dried kelp and the dried kelp block. Kelp grows up through water, flowing or not, and every block it grew through is a water source once the kelp is removed: the modern way to fill a column with sources for a bubble column elevator.");
		enableModernBreathing = c.getBoolean("enableModernBreathing", MOVEMENT, true, "Modern air bubbles: they burst one by one with a pop that gets louder as you run out, empty outlines show the air you've used, and air comes back gradually (4 per tick) instead of all at once when you surface.\nModified Classes: net.minecraft.entity.EntityLivingBase");
		enableModernSneaking = c.getBoolean("enableModernSneaking", MOVEMENT, true, "Modern (1.14+) sneaking: a 1.5 block tall hitbox and lower eyes while crouching, so you fit under slabs, a smooth camera when the eye height changes, and automatic crouching where you can't stand up. Sneaking stops you at drops deeper than 0.6 blocks instead of a full block.\nModified Classes: net.minecraft.entity.Entity net.minecraft.entity.player.EntityPlayer\nModified Client Classes: net.minecraft.client.entity.EntityPlayerSP net.minecraft.client.renderer.EntityRenderer net.minecraft.client.renderer.entity.RenderManager net.minecraft.util.MovementInputFromOptions net.minecraft.client.renderer.entity.RenderPlayer net.minecraft.client.model.ModelBiped net.minecraft.client.renderer.ItemRenderer");
		enableModernSwimming = c.getBoolean("enableModernSwimming", MOVEMENT, true, "Modern (1.13+) swimming and crawling: sprint in water to swim with a 0.6 block tall hitbox, and crawl through 1 block tall gaps. Includes the 1.13+ water physics for players: water detection that works with the small hitbox, slower sinking, the modern sprint rules, and modern underwater vision (your eyes adjust to the water over 30 seconds; Respiration no longer brightens the fog).\nModified Classes: net.minecraft.entity.Entity net.minecraft.entity.EntityLivingBase net.minecraft.entity.player.EntityPlayer\nModified Client Classes: net.minecraft.client.entity.EntityPlayerSP net.minecraft.client.renderer.EntityRenderer net.minecraft.client.renderer.entity.RenderManager net.minecraft.util.MovementInputFromOptions net.minecraft.client.renderer.entity.RenderPlayer net.minecraft.client.model.ModelBiped net.minecraft.client.renderer.ItemRenderer");
		poseDataWatcherId = c.getInt("poseDataWatcherId", MOVEMENT, 26, 2, 31, "The DataWatcher id used to sync a player's pose (standing, crouching, swimming, gliding) when enableModernSneaking, enableModernSwimming or enableElytra is on, and the off-hand's using hand and main arm when enableOffhand is on. Vanilla players use 0, 1, 6-9 and 16-18. Must be the same on the server and all clients.\nIf you're getting a crash about a duplicate DataWatcher id, another mod uses this id too: change it to a free one.");

		enableElytra = c.getBoolean("enableElytra", ELYTRA, true, "The elytra as 26.3's: gliding with a 0.6 block tall box and low eyes, 26.3's takeoff, flight, wings, camera, sound and firework boosting; mended with phantom membrane.");
		enableNewElytraTakeoffLogic = c.getBoolean("enableNewElytraTakeoffLogic", ELYTRA, true, "26.3's takeoff (a jump in the air starts gliding); off, the player must also be falling, as 1.9 to 1.14.");
		elytraDataWatcherFlag = c.getInt("elytraDataWatcherFlag", ELYTRA, 7, 0, 31, "The bit of the entity's shared flags that says it's gliding (7, as 26.3's; 1.7.10's players use 0 to 4). Change it only if another mod uses bit 7.");
		shiftClickEquipsElytra = c.getBoolean("shiftClickEquipsElytra", ELYTRA, true, "Shift-clicking an elytra in the inventory puts it on when no chestplate or elytra is worn, as for armour." +
			"\nModified Classes: net.minecraft.inventory.ContainerPlayer");

		enableOffhand = c.getBoolean("enableOffhand", OFFHAND, true, "The off-hand (1.9+): a second hand slot, swapped with the main hand by a key (F by default), used when the main hand has nothing to do, shown beside the hotbar. Turns itself off when Backhand or Battlegear 2 is loaded, since a player can only have one off-hand." +
			"\nModified Classes: net.minecraft.entity.player.InventoryPlayer net.minecraft.entity.player.EntityPlayer net.minecraft.inventory.ContainerPlayer net.minecraft.network.NetHandlerPlayServer net.minecraft.server.management.ItemInWorldManager net.minecraft.item.ItemBow net.minecraft.entity.EntityLivingBase net.minecraft.inventory.Container" +
			"\nModified Client Classes: net.minecraft.client.gui.inventory.GuiContainer net.minecraft.client.gui.inventory.GuiInventory net.minecraft.client.gui.inventory.GuiContainerCreative net.minecraft.client.Minecraft net.minecraft.client.multiplayer.PlayerControllerMP net.minecraft.client.entity.EntityClientPlayerMP net.minecraft.client.network.NetHandlerPlayClient net.minecraft.client.entity.EntityOtherPlayerMP net.minecraft.client.renderer.ItemRenderer net.minecraft.client.renderer.entity.RenderPlayer net.minecraft.client.model.ModelBiped");
		offhandDataWatcherId = c.getInt("offhandDataWatcherId", OFFHAND, 27, 2, 31, "The DataWatcher id used to sync a player's off-hand item to other players when enableOffhand (off-hand). Vanilla players use 0, 1, 6-9 and 16-18; the pose byte uses poseDataWatcherId. Must be the same on the server and all clients.\nIf you're getting a crash about a duplicate DataWatcher id, another mod uses this id too: change it to a free one.");

		arrowsInheritMotion = c.getBoolean("arrowsInheritMotion", COMBAT, true, "An arrow shot by a player adds the player's own movement to its launch, as since 1.9 (the vertical part only while airborne): shooting while sprinting, riding or gliding carries the arrow further. The server learns the movement from the position packets, as modern's does." +
			"\nModified Classes: net.minecraft.entity.player.EntityPlayer net.minecraft.network.NetHandlerPlayServer net.minecraft.entity.projectile.EntityArrow");
		enableShield = c.getBoolean("enableShield", COMBAT, true, "The shield (1.9+): raised with right click in either hand, it blocks hits from the front once up for a quarter of a second; an axe disables it for 5 seconds. Needs modernCombat (combat), since blocking is part of the combat update.");
		modernCombat = c.getBoolean("modernCombat", COMBAT, true, "The combat update (1.9+): an attack does its full damage only once the weapon has charged, shown by the attack indicator under the crosshair (or beside the hotbar, or off, set by attackIndicator (client)), and each weapon has modern's damage and speed (a diamond sword 7 damage at 1.6 attacks a second, a diamond axe 9 at 1); a sword at full charge on the ground sweeps the mobs around the target; critical hits and sprint knockback need full charge, and critical hits no sprinting; the attack sounds follow the charge; the held item dips after a swing and rises as it charges; tooltips show a weapon's damage and speed; swords no longer block, so right click goes to the off-hand item. Needs EFR on the server too." +
			"\nModified Classes: net.minecraft.entity.player.EntityPlayer net.minecraft.item.ItemStack net.minecraft.item.ItemSword" +
			"\nModified Client Classes: net.minecraft.client.Minecraft net.minecraft.client.multiplayer.PlayerControllerMP net.minecraft.client.renderer.ItemRenderer");
		enableSweepingEdge = c.getBoolean("sweepingEdge", COMBAT, true, "Stronger sweeps for swords (1.11+): a sweep deals 1 plus half (I), two thirds (II) or three quarters (III) of the attack's damage instead of 1. Needs modernCombat (combat), since sweeps are part of the combat update.");
		sweepingEdgeID = c.getInt("sweepingEdgeID", COMBAT, 204, 0, Short.MAX_VALUE, "");

		chatHistoryAcrossRestarts = c.getBoolean("chatHistoryAcrossRestarts", COMMANDS, true, "The messages you typed also survive restarting the game: the last hundred are kept in etfuturum_chat_history.txt next to options.txt. Neither 1.7.10 nor modern does this." +
			"\nModified Client Classes: net.minecraft.client.gui.GuiNewChat");
		chatHistoryBetweenWorlds = c.getBoolean("chatHistoryBetweenWorlds", COMMANDS, true, "The messages you typed stay in the chat's history (the up arrow) when you leave a world, as in modern; 1.7.10 forgot them." +
			"\nModified Client Classes: net.minecraft.client.gui.GuiNewChat");
		chatLogBetweenWorlds = c.getBoolean("chatLogBetweenWorlds", COMMANDS, true, "The chat log itself stays when you leave a world, so what was said in the last world or server is still there in the next. Modern clears it." +
			"\nModified Client Classes: net.minecraft.client.gui.GuiNewChat");
		commandBlockCreativeOnly = c.getBoolean("commandBlockCreativeOnly", COMMANDS, true, "A command block opens only in creative, as in modern; outside creative a right click uses the held item instead. Saving still needs operator rights." +
			"\nModified Classes: net.minecraft.block.BlockCommandBlock");
		coordinateSuggestions = c.getBoolean("coordinateSuggestions", COMMANDS, true, "Tab on /setblock's x, y and z fills in the coordinates of the block you look at, one more coordinate per press (~ when you look at no block), as modern suggests them." +
			"\nModified Classes: net.minecraft.command.server.CommandSetBlock");
		giveAbove64 = c.getBoolean("giveAbove64", COMMANDS, true, "/give accepts counts above 64, as in modern: up to 100 stacks of the item, handed over as full stacks." +
			"\nModified Classes: net.minecraft.command.CommandGive");
		modernChat = c.getBoolean("modernChat", COMMANDS, true, "The chat works as modern's (1.13+): typing a command shows a list of suggestions as you type (Tab takes one, Up and Down choose), a grey preview of the selected one, the command's usage or what is wrong with it above the input, and the command coloured by its parts; chat messages can be 256 characters long. Every command of 1.7.10 and other mods is offered with its own tab completion; /effect is modern's (give, clear, effect ids, @-selectors), and its 1.7.10 form still works. Needs EFR on the server too; on a server without it the chat stays as it was. The list can be set to open only on Tab with Command Suggestions in Chat Settings." +
			"\nModified Classes: net.minecraft.command.CommandHandler net.minecraft.network.play.client.C01PacketChatMessage net.minecraft.server.management.ServerConfigurationManager" +
			"\nModified Client Classes: net.minecraft.client.gui.GuiChat net.minecraft.client.gui.GuiTextField net.minecraft.client.gui.ScreenChatOptions");

		firstPersonCameraAtEyes = c.getBoolean("firstPersonCameraAtEyes", OPTIONS, true, "The first-person camera sits at the player's eyes, as modern's does. 1.7.10 put it 0.1 blocks behind them, which made everything in view look a little smaller and farther than in modern." +
			"\nModified Client Classes: net.minecraft.client.renderer.EntityRenderer");
		fovEffects = c.getBoolean("fovEffects", OPTIONS, true, "Modern's FOV Effects setting (1.16+): a slider in Video Settings for how much the field of view follows speed, flying, a drawn bow and water, from off to vanilla's full effect. The value is fovEffectScale (client)." +
			"\nModified Classes: Client: net.minecraft.client.entity.EntityPlayerSP net.minecraft.client.renderer.EntityRenderer net.minecraft.client.gui.GuiVideoSettings net.minecraft.client.gui.GuiOptionsRowList");
		guiScaleWheel = c.getBoolean("guiScaleWheel", OPTIONS, true, "In Video Settings, after the first click on GUI Scale, the mouse wheel cycles the GUI scale (up: larger) instead of scrolling the list, until the screen is closed. A click lays the screen out again for the new scale and the button moves away from the cursor; the wheel does not." +
			"\nModified Classes: Client: net.minecraft.client.gui.GuiVideoSettings net.minecraft.client.gui.GuiSlot");
		modernHeldItems = c.getBoolean("modernHeldItems", OPTIONS, true, "Items held in third person (players, both hands, and zombies, skeletons and the like) are placed and turned as modern places them: flat items, tools, fishing rods, bows and blocks.");
		modernInventoryFigure = c.getBoolean("modernInventoryFigure", OPTIONS, true, "The player in the inventory screen stands where modern draws them (centred in the box, about 3 pixels higher) and looks toward the mouse from the box's middle.");
		modernKeyRepeat = c.getBoolean("modernKeyRepeat", OPTIONS, true, "A held keyboard key bound to Use fires at the keyboard's repeat rate, as since 1.13; a held mouse button keeps firing every four ticks, as in both. 1.7.10 counted one press per key-down. Screens keep vanilla's key repeat state, and repeats of other keys are dropped. Needed for the exploding-piston bedrock method, where the placing click has to land in one tick." +
			"\nModified Client Classes: net.minecraft.client.Minecraft");
		modernOptions = c.getBoolean("modernOptions", OPTIONS, true, "The options screens as in modern (26.3): Controls split into Mouse Settings and Key Binds, an Accessibility Settings screen (also on the title screen, beside the language button), Video Settings in three headed sections, and every option on the screen modern puts it on. EFR's client options (FOV Effects, Attack Indicator, Command Suggestions, the skin parts and the main hand) are then kept in options.txt under modern's names; their entriesNone in Et Futurum's functions.cfg are only read when options.txt doesn't have them yet." +
			"\nModified Client Classes: net.minecraft.client.settings.GameSettings net.minecraft.client.gui.GuiMainMenu net.minecraft.client.settings.KeyBinding net.minecraft.client.Minecraft net.minecraft.client.entity.EntityClientPlayerMP");
		offlineSkins = c.getBoolean("offlineSkins", OPTIONS, false, "For test servers. Players a server does not authenticate (offline mode, or a development client's single-player) get the skin of the Mojang account of the typed name, looked up on Mojang's public API when they log in, so every client, their own included, draws it; anyone can wear any name's skin. Set it on the server and on the clients. Answers are kept for a day in etfuturum_offline_skins.json next to the server's world (delete it to look a skin up again)." +
			"\nModified Classes: net.minecraft.server.network.NetHandlerLoginServer\nClient: net.minecraft.util.Session");
		skinCustomization = c.getBoolean("skinCustomization", OPTIONS, true, "The Skin Customization screen (1.8+), a button in Options: show or hide your cape, jacket, sleeves, pants legs and hat, and pick your main hand; the choices reach every player. The second-layer parts themselves need enablePlayerSkinOverlayNone in Et Futurum's functions.cfg (client)." +
			"\nModified Classes: net.minecraft.entity.player.EntityPlayer" +
			"\nModified Client Classes: net.minecraft.client.gui.GuiOptions net.minecraft.client.renderer.entity.RenderPlayer");
		skinPartsDataWatcherId = c.getInt("skinPartsDataWatcherId", OPTIONS, 28, 2, 31, "The DataWatcher id used to sync which parts of a player's skin are shown (Skin Customization) when skinCustomization (options and skins). Vanilla players use 0, 1, 6-9 and 16-18; change this only if another mod uses 28 on players.");

		attackIndicator = c.getString("attackIndicator", CLIENT, "crosshair", "Where the attack indicator shows how far your weapon has charged (modern's Attack Indicator option, 1.9+; Video Settings has the button): crosshair, under the crosshair; hotbar, beside the hotbar; off. Needs modernCombat (combat).", new String[]{"crosshair", "hotbar", "off"});
		commandSuggestions = c.getBoolean("commandSuggestions", CLIENT, true, "The chat's suggestion list opens by itself as you type a command; off, Tab opens it (modern's Command Suggestions, in Chat Settings). Needs modernChat (commands and chat).");
		fovEffectScale = c.getFloat("fovEffectScale", CLIENT, 1.0F, 0.0F, 1.0F, "How much the field of view follows speed, flying, a drawn bow and water: modern's FOV Effects setting (Video Settings has the slider; 1 = vanilla, 0 = the FOV never changes). Needs fovEffects (options and skins).");
		mainHandLeft = c.getBoolean("mainHandLeft", CLIENT, false, "Your main hand is your left hand (modern's Main Hand option, 1.9+): your main-hand item is in your left arm and the off-hand item in your right, in first person and for everyone else, swings go with the main arm, and the hotbar's off-hand slot sits to the right. It changes nothing about what the hands do. Sent to the server when you join a world and when this config is reloaded. Needs the off-hand (enableOffhand (off-hand)).");
		skinCape = c.getBoolean("skinCape", CLIENT, true, "Skin Customization: show your cape. All of these are set from Options -> Skin Customization... in the game and reach every player.");
		skinHat = c.getBoolean("skinHat", CLIENT, true, "Skin Customization: show the hat, the second layer over the head.");
		skinJacket = c.getBoolean("skinJacket", CLIENT, true, "Skin Customization: show the jacket, the second layer over the body (needs enablePlayerSkinOverlay).");
		skinLeftPantsLeg = c.getBoolean("skinLeftPantsLeg", CLIENT, true, "Skin Customization: show the left pants leg (needs enablePlayerSkinOverlay).");
		skinLeftSleeve = c.getBoolean("skinLeftSleeve", CLIENT, true, "Skin Customization: show the left sleeve (needs enablePlayerSkinOverlay).");
		skinRightPantsLeg = c.getBoolean("skinRightPantsLeg", CLIENT, true, "Skin Customization: show the right pants leg (needs enablePlayerSkinOverlay).");
		skinRightSleeve = c.getBoolean("skinRightSleeve", CLIENT, true, "Skin Customization: show the right sleeve (needs enablePlayerSkinOverlay).");

		chorusOnlyOnOuterIslands = c.getBoolean("chorusOnlyOnOuterIslands", END, true, "Chorus plants generate only on the outer End islands, beyond 1024 blocks from the centre (modern's main-island biome boundary, 1.9+). The vanilla End has no land out there, so with this on chorus only generates in the outer islands of the End dimension provider (Et Futurum's experiments.cfg, endDimensionProvider).");
		endBarrensID = c.getInt("endBarrensID", END, 207, -1, 255, "End Barrens, with modernEnd. -1 leaves the End's own biome there.");
		endCreditsOnce = c.getBoolean("endCreditsOnce", END, true, "The End's exit portal shows the credits only the first time a player goes through it, as since 1.9; after that it takes the player straight to their spawn point. 1.7.10 showed them every time. Whether a player has seen them is saved with the player." +
			"\nModified Classes: net.minecraft.entity.player.EntityPlayerMP" +
			"\nModified Client Classes: net.minecraft.client.network.NetHandlerPlayClient");
		endHighlandsID = c.getInt("endHighlandsID", END, 204, -1, 255, "The End's outer biomes with modernEnd (the end): End Highlands. -1 leaves the End's own biome there.");
		endMidlandsID = c.getInt("endMidlandsID", END, 205, -1, 255, "End Midlands, with modernEnd. -1 leaves the End's own biome there.");
		modernEnd = c.getBoolean("modernEnd", END, true, "The End as 26.3's (1.9+ and later): its terrain (the main island, the outer islands past 1024 blocks) and biomes, arriving on the platform as modern's; the dragon fight (the pillars and their crystals, the dragon, its breath, the respawn), gateways, the outer islands' chorus and small islands, end cities with their loot and the dragon head. With it, as 26.3: panes traced by their bars (arrows and the crosshair go through their gaps) and bedrock dropping nothing when broken. Off by itself with Hardcore Ender Expansion, which makes its own End. Old Ends keep what they have generated.");
		smallEndIslandsID = c.getInt("smallEndIslandsID", END, 206, -1, 255, "Small End Islands, with modernEnd. -1 leaves the End's own biome there.");

		effectPanelAlphaFix = c.getBoolean("effectPanelAlphaFix", EFFECTS, true, "Turns the alpha test on before the inventory's potion effect panel is drawn. Vanilla assumes it is on; when earlier rendering leaves it off, the first effect shows a black box and coloured corner marks (the hidden pixels of its sprites).");
		enableLuck = c.getBoolean("enableLuck", EFFECTS, true, "Luck and Bad Luck (1.9+): more or less luck fishing, as Luck of the Sea's levels; the Potion of Luck. Needs namedEffects.");
		enableTrialEffects = c.getBoolean("enableTrialEffects", EFFECTS, true, "The 1.21 effects and their potions: Wind Charged (a wind burst when killed), Weaving (cobwebs when killed, slowed less in them), Oozing (two slimes when killed), Infested (silverfish when hurt). Needs namedEffects.");
		modernPotionColors = c.getBoolean("modernPotionColors", EFFECTS, true, "Potion liquid colours as in modern versions (changed in 1.19.4): speed is cyan, strength gold, fire resistance orange, night vision lime, and so on. Colours the potion items, thrown potions, effect particles, lingering clouds and tipped arrows. The water bottle is the same blue in both.");
		namedEffects = c.getBoolean("namedEffects", EFFECTS, true, "Effects known by name, as 26.3 keeps them (spec §70): 1.7.10's 32 effect slots grow as effects register, an effect asking for a taken number gets a free one instead of replacing the effect there, and no effect has an id to set in a config.");
		noPotionGlint = c.getBoolean("noPotionGlint", EFFECTS, true, "Potion bottles have no enchantment glint, as since 1.9. 1.7.10 gave every potion with an effect the glint." +
			"\nModified Client Classes: net.minecraft.item.ItemPotion");
		potionsByName = c.getBoolean("potionsByName", EFFECTS, true, "Potions by name, as 26.3 keeps them (spec §69): every 26.3 potion whose effects exist (leaping, turtle master, slow falling and the rest 1.7.10 lacks), with 26.3's effects and lengths (splash potions too), names, colours and creative list. A potion 1.7.10 has stays as 1.7.10 keeps it; one it lacks is kept by name, its effects also where 1.7.10 reads them.");

		enableChanneling = c.getBoolean("channeling", TRIDENT, true, "The trident's Channeling (1.13+): in a thunderstorm, a thrown trident calls lightning down on the mob it hits under the open sky, or on the lightning rod it hits. Needs the trident (enableTrident (trident)).");
		channelingID = c.getInt("channelingID", TRIDENT, 206, 0, Short.MAX_VALUE, "");
		enableTrident = c.getBoolean("enableTrident", TRIDENT, true, "The trident (1.13+): a weapon of 9 damage, thrown after charging it for half a second. No recipe: until the drowned come, it comes from /give and /summon. Needs modernCombat (combat), for its attack speed and its look in first person.");
		enableImpaling = c.getBoolean("impaling", TRIDENT, true, "The trident's Impaling (1.13+): 2.5 more damage a level to aquatic mobs (in 1.7.10 the squid), in melee and thrown. Not with Sharpness, Smite or Bane of Arthropods. Needs the trident (enableTrident (trident)).");
		impalingID = c.getInt("impalingID", TRIDENT, 208, 0, Short.MAX_VALUE, "");
		enableLoyalty = c.getBoolean("loyalty", TRIDENT, true, "The trident's Loyalty (1.13+): a thrown trident, once it has hit, flies back to its thrower through blocks, faster at each level. Needs the trident (enableTrident (trident)).");
		loyaltyID = c.getInt("loyaltyID", TRIDENT, 207, 0, Short.MAX_VALUE, "");
		modernLightning = c.getBoolean("modernLightning", TRIDENT, true, "Lightning as in 26.3: copper it strikes is cleaned of its weathering, item frames, paintings and armour stands are left alone, a struck pig or villager turns into a zombie pigman or a witch that never despawns (not on Peaceful, where they are only struck), a struck mooshroom swaps red and brown keeping everything else and comes to no harm, a charged creeper gives only one head however many mobs it kills (wither skeletons' skulls included), and the bolt sounds as modern's. Natural lightning strikes the highest lightning rod within 128 blocks, else a mob under the open sky near where it falls, and now and then leaves a skeleton horse trap instead of striking." +
			"\nModified Classes: net.minecraft.entity.effect.EntityLightningBolt, net.minecraft.world.WorldServer, net.minecraft.entity.passive.EntityHorse");
		enableRiptide = c.getBoolean("riptide", TRIDENT, true, "The trident's Riptide (1.13+): in water or rain, letting go of a charged trident launches you instead of throwing it, spinning into the first mob in your way. Needs the trident (enableTrident (trident)).");
		riptideID = c.getInt("riptideID", TRIDENT, 205, 0, Short.MAX_VALUE, "");

		aggressiveDataWatcherId = c.getInt("aggressiveDataWatcherId", MOBS, 25, 2, 31, "The DataWatcher id used to sync whether a zombie or skeleton (and their kinds) is aggressive, which poses its arms as in modern, when modernMobArms (mobs). Vanilla zombies use 0, 1, 6-14 (Et Futurum's zombie villager 15), skeletons 0, 1, 6-11 and 13. Must be the same on the server and all clients.\nIf you're getting a crash about a duplicate DataWatcher id, another mod uses this id on those mobs too: change it to a free one.");
		enableGlowSquidSpawns = c.getBoolean("enableGlowSquidSpawns", MOBS, true, "Glow squids spawn in water at y 30 and below where no light reaches, in every overworld biome, as in 26.3 (they share the water mob cap with squids). Needs enableGlowSquids.");
		enableGlowSquids = c.getBoolean("enableGlowSquids", MOBS, true, "Glow squids (1.17+): squids that glow, going dark for a while when hurt; they drop glow ink sacs.");
		enablePhantomSpawns = c.getBoolean("enablePhantomSpawns", MOBS, true, "Phantoms spawn over players who haven't slept for 3 days, at night under the open sky, as in 26.3 (the doInsomnia game rule turns it off). Needs enablePhantoms.");
		enablePhantoms = c.getBoolean("enablePhantoms", MOBS, true, "Phantoms (1.13+): undead flyers that circle and swoop at players, burn in daylight, flee cats, and drop phantom membranes.");
		enablePolarBearSpawns = c.getBoolean("enablePolarBearSpawns", MOBS, true, "Polar bears spawn in Ice Plains, Ice Plains Spikes and Ice Mountains (on grass), and Frozen Ocean (on ice), as in 26.3's snowy biomes. Needs enablePolarBears.");
		enablePolarBears = c.getBoolean("enablePolarBears", MOBS, true, "Polar bears (1.10+): neutral, but a mother goes for a player near her cub; they hunt foxes, stand up roaring before a swipe and swim fast.");
		modernHorsesInWater = c.getBoolean("modernHorsesInWater", MOBS, true, "Horses move through water as in modern: pushed half as hard as 1.7.10's (0.02 a tick) and sinking slowly. Skeleton horses don't float: they sink to the bottom and walk along it, fast (they keep 0.96 of their speed a tick in water, others 0.8), and keep their rider under water. Zombie and skeleton horses don't drown. Skeleton horses' own sounds in water (ambient, swimming, jumping)." +
			"\nModified classes: net.minecraft.entity.passive.EntityHorse, net.minecraft.entity.EntityLivingBase, net.minecraft.entity.Entity, net.minecraft.entity.ai.EntityAISwimming");
		modernMobArms = c.getBoolean("modernMobArms", MOBS, true, "Zombies and skeletons hold their arms as in modern. Zombies (husks, zombie villagers, pigmen, giants): a little lower when idle, higher just before they attack, swinging down when they hit, a baby holding something with its arms down. Skeletons (strays, wither skeletons): arms down when idle, the bow aimed when they have a target, raised only when they chase to hit. The \"aggressive\" state is synced with aggressiveDataWatcherId (mobs).");
		modernMobSpawnLight = c.getBoolean("modernMobSpawnLight", MOBS, true, "Hostile mobs need block light 0 to spawn, as since 1.18: a lit cave stays empty however dim the light. The sky-light rule is unchanged, so the surface still spawns at night, and so is the thundering rule. Not applied in the Nether, as in modern." +
			"\nModified Classes: net.minecraft.entity.monster.EntityMob");
		modernPanic = c.getBoolean("modernPanic", MOBS, true, "Animals panic when hurt by cactus, fire, lava, magma, freezing, lightning, magic, explosions and other harm, not only by an attacker, and a burning animal runs for water within 5 blocks first, as in modern.");
		undeadHorseLeads = c.getBoolean("undeadHorseLeads", MOBS, true, "Zombie and skeleton horses can be put on a lead and tied to a fence, as in modern (a zombie horse ridden by a mob only when tamed). 1.7.10 leashes no undead horse." +
			"\nModified classes: net.minecraft.entity.passive.EntityHorse");

		enableLingeringPotions = c.getBoolean("enableLingeringPotions", BLOCKS, true, "Lingering potions as 26.3's: thrown, they leave a cloud of their effects (a quarter as long); a creeper with effects leaves one too.");
		enableTippedArrows = c.getBoolean("enableTippedArrows", BLOCKS, true, "Tipped arrows as 26.3's: 8 arrows around a lingering potion, their effects an eighth as long, shot from bows and dispensers.");
		enableBlackstone = c.getBoolean("enableBlackstone", BLOCKS, true, "Blackstone as 26.3's: blackstone, polished, its bricks, cracked and chiseled, their slabs, stairs and walls, the polished button and pressure plate (pressed by mobs only), gilded blackstone and its nuggets.");
		enableBasalt = c.getBoolean("enableBasalt", BLOCKS, true, "Basalt, polished basalt and smooth basalt as 26.3's.");
		enableNetherGold = c.getBoolean("enableNetherGold", BLOCKS, true, "Nether gold ore as 26.3's: 2 to 6 gold nuggets, 0 or 1 experience, smelted to a gold ingot.");
		enableCryingObsidian = c.getBoolean("enableCryingObsidian", BLOCKS, true, "Crying obsidian as 26.3's: light 10, its purple tears, not moved by pistons.");
		enableRedNetherBricks = c.getBoolean("enableRedNetherBricks", BLOCKS, true, "Red, cracked and chiseled nether bricks as 26.3's (one id), the red ones' slab, stairs and wall.");
		enableQuartzBricks = c.getBoolean("enableQuartzBricks", BLOCKS, true, "Quartz bricks as 26.3's.");
		enableSoulSoil = c.getBoolean("enableSoulSoil", BLOCKS, true, "Soul soil and soul fire as 26.3's: fire on soul sand or soul soil is soul fire (light 10, 2 damage, never spreading); lava over soul soil beside blue ice becomes basalt.");
		enableNetherPlants = c.getBoolean("enableNetherPlants", BLOCKS, true, "The Nether's plants and stems as 26.3's: nylium, crimson and warped fungi (and their huge fungi), roots, nether sprouts, weeping and twisting vines, shroomlight, crimson and warped stems (EFR's were experiments, off).");
		enableNetherwartBlock = c.getBoolean("enableNetherwartBlock", BLOCKS, true, "Nether wart blocks and warped wart blocks as 26.3's.");
		enableTarget = c.getBoolean("enableTarget", BLOCKS, true, "The target as 26.3's: a projectile's hit gives a weak signal by how near the middle it hit, 20 ticks from an arrow, 8 from anything else.");
		enableDeepslate = c.getBoolean("enableDeepslate", BLOCKS, true, "Deepslate as 26.3's: deepslate, cobbled and polished deepslate.");
		enableDeepslateOres = c.getBoolean("enableDeepslateOres", BLOCKS, true, "The deepslate ores as 26.3's: 4.5 hard, dropping as the stone ores do.");
		enableStones = c.getBoolean("enableStones", BLOCKS, true, "Granite, diorite, andesite and the polished ones as 26.3's, their slabs, stairs and walls.");
		disableBaseBountifulStonesOnly = c.getBoolean("disableBaseBountifulStonesOnly", BLOCKS, false, "Leaves out just the granite, diorite and andesite blocks, keeping their slabs, stairs and walls (made from any mod's by the ore names stoneGranite and the like): for Chisel or Botania, which have these stones but not the rest.");
		enableRedSandstone = c.getBoolean("enableRedSandstone", BLOCKS, true, "Red sandstone as 26.3's: red sandstone, chiseled, cut and smooth, their slabs, stairs and wall.");
		enableSmoothStone = c.getBoolean("enableSmoothStone", BLOCKS, true, "Smooth stone as 26.3's: smelted from stone; its slab is 1.7.10's stone slab.");
		enableSmoothSandstone = c.getBoolean("enableSmoothSandStone", BLOCKS, true, "Smooth sandstone as 26.3's, its slab and stairs.");
		enableSmoothQuartz = c.getBoolean("enableSmoothQuartz", BLOCKS, true, "Smooth quartz as 26.3's, its slab and stairs.");
		enableExtraVanillaSlabs = c.getBoolean("enableExtraVanillaSlabs", BLOCKS, true, "26.3's slabs of vanilla blocks 1.7.10 lacks: stone, mossy cobblestone, mossy stone brick and cut sandstone.");
		enableExtraVanillaStairs = c.getBoolean("enableExtraVanillaStairs", BLOCKS, true, "26.3's stairs of vanilla blocks 1.7.10 lacks: stone, mossy cobblestone and mossy stone brick.");
		enableExtraVanillaWalls = c.getBoolean("enableExtraVanillaWalls", BLOCKS, true, "26.3's walls of vanilla blocks 1.7.10 lacks: stone brick, mossy stone brick, sandstone, brick and nether brick.");
		enableStoneBrickRecipes = c.getBoolean("enableStoneBrickRecipes", BLOCKS, true, "26.3's recipes for the stone bricks 1.7.10 can't make: mossy cobblestone and mossy stone bricks with a vine, chiseled from two slabs, cracked by smelting.");
		enableTuff = c.getBoolean("enableTuff", BLOCKS, true, "Tuff as 26.3's: tuff, polished, its bricks, chiseled and chiseled bricks, their slabs, stairs and walls.");
		enableCalcite = c.getBoolean("enableCalcite", BLOCKS, true, "Calcite as 26.3's (amethyst geodes' middle layer).");
		enableVanillaWoodRedstone = c.getBoolean("enableVanillaWoodRedstone", BLOCKS, true, "Buttons and pressure plates of the vanilla woods (spruce to dark oak) as 26.3's.");
		enableVanillaSigns = c.getBoolean("enableVanillaSigns", BLOCKS, true, "Signs of the vanilla woods (spruce to dark oak) as 26.3's: two sides, edited again by use, 90-pixel lines, stacks of 16.");
		enableNewSigns = c.getBoolean("enableNewSigns", BLOCKS, true, "Signs of crimson, warped, mangrove, cherry and bamboo (and Biomes O' Plenty's and Witchery's woods) as 26.3's.");
		enableNewWoodRedstone = c.getBoolean("enableNewWoodRedstone", BLOCKS, true, "Buttons and pressure plates of crimson, warped, mangrove, cherry and bamboo as 26.3's.");
		enableVanillaDoors = c.getBoolean("enableVanillaDoors", BLOCKS, true, "Doors of the vanilla woods (spruce to dark oak) as 26.3's; vanilla's doors stack to 64.");
		enableNewDoors = c.getBoolean("enableNewDoors", BLOCKS, true, "Doors of crimson, warped, mangrove, cherry and bamboo as 26.3's.");
		enableVanillaTrapdoors = c.getBoolean("enableVanillaTrapdoors", BLOCKS, true, "Trapdoors of the vanilla woods (spruce to dark oak) as 26.3's.");
		enableNewTrapdoors = c.getBoolean("enableNewTrapdoors", BLOCKS, true, "Trapdoors of crimson, warped, mangrove, cherry and bamboo as 26.3's.");
		enableIronTrapdoor = c.getBoolean("enableIronTrapdoor", BLOCKS, true, "The iron trapdoor as 26.3's: opened by redstone alone.");
		enableDoorRecipeBuffs = c.getBoolean("enableDoorRecipeBuffs", BLOCKS, true, "Doors are made 3 at a time, as 26.3's (vanilla's wooden and iron doors too).");
		enableFloatingTrapDoors = c.getBoolean("enableFloatingTrapDoors", BLOCKS, true, "Trapdoors need nothing to hang on, as 26.3's (vanilla's too).");
		enableVanillaFences = c.getBoolean("enableVanillaFences", BLOCKS, true, "Fences of the vanilla woods (spruce to dark oak) as 26.3's, and vanilla's oak fence made as 26.3's.");
		enableNewFences = c.getBoolean("enableNewFences", BLOCKS, true, "Fences of crimson, warped, mangrove, cherry and bamboo as 26.3's.");
		enableVanillaGates = c.getBoolean("enableVanillaGates", BLOCKS, true, "Fence gates of the vanilla woods (spruce to dark oak) as 26.3's.");
		enableNewGates = c.getBoolean("enableNewGates", BLOCKS, true, "Fence gates of crimson, warped, mangrove, cherry and bamboo as 26.3's.");
		enableStrippedLogs = c.getBoolean("enableStrippedLogs", BLOCKS, true, "Stripped logs as 26.3's for the vanilla woods (EFR's ids), and stripping every log here with an axe.");
		enableBarkLogs = c.getBoolean("enableBarkLogs", BLOCKS, true, "The vanilla woods' wood as 26.3's: bark all round, on any axis (EFR's ids); with stripped logs, their stripped wood too.");
		enableCherryBlocks = c.getBoolean("enableCherryBlocks", BLOCKS, true, "Cherry's wood as 26.3's: its log, wood and stripped kinds, and its wood set (its planks, slab, stairs and the rest).");
		enableBambooBlocks = c.getBoolean("enableBambooBlocks", BLOCKS, true, "Bamboo as 26.3's: the block of bamboo and its stripped kind, the plant, and bamboo's wood set.");
		enableNewBoats = c.getBoolean("enableNewBoats", BLOCKS, true, "Boats and rafts as 26.3's, every wood's, with chest boats: floating, steering and paddles, two passengers (one in a chest boat), mobs getting in, under water 60 ticks throwing them off, bubble columns launching them, dispensers placing them.");
		replaceOldBoats = c.getBoolean("replaceOldBoats", BLOCKS, true, "Vanilla's boat is the oak boat: its item places one, and old boats become one. Off: the new oak boat is its own item (made of oak planks), and vanilla's boat stays 1.7.10's.");
		newBoatPassengerSeat = c.getBoolean("newBoatPassengerSeat", BLOCKS, true, "A boat takes a second passenger, as 26.3's (on an unseen seat that follows it). Off: one.");
		newBoatMaxLandSpeed = c.getFloat("newBoatMaxLandSpeed", BLOCKS, 1.0F, 0.1F, 1.0F, "The most of the ground's slipperiness a boat on land keeps (EFR's option; 1 is 26.3's: blue ice 0.989, ice 0.98, most blocks 0.6).");
		newBoatSpeed = c.getFloat("newBoatSpeed", BLOCKS, 1.0F, 0.1F, 2.0F, "How much faster a boat is rowed in water (EFR's option; 1 is 26.3's).");
		newBoatEntityBlacklist = c.getStringList("newBoatEntityBlacklist", BLOCKS, new String[0], "Mobs that don't get pushed into boats (EFR's option): an entity id, or classpath:<part of its class name>. Players and water mobs never are, as 26.3's.");
		newBoatEntityBlacklistAsWhitelist = c.getBoolean("newBoatEntityBlacklistAsWhitelist", BLOCKS, false, "newBoatEntityBlacklist lists the only mobs that do (EFR's option).");
		stopBoatRotationLock = c.getBoolean("stopBoatRotationLock", BLOCKS, false, "A boat's passengers turn freely, not kept within 105 degrees of it nor turned with it (EFR's option; off is 26.3's).");
		enableGlowLichen = c.getBoolean("enableGlowLichen", BLOCKS, true, "Glow lichen as 26.3's: faces on any side, light 7, sheared one a face, spread by bonemeal.");
		enableGlowBerries = c.getBoolean("enableGlowBerries", BLOCKS, true, "Cave vines and glow berries as 26.3's: berries by bonemeal or by chance as they grow (light 14), picked by hand, eaten or planted.");
		enableAmethyst = c.getBoolean("enableAmethyst", BLOCKS, true, "Amethyst as 26.3's: the block, budding amethyst growing buds into clusters, the shard, tinted glass (stops all light).");
		buddingAmethystMode = c.getInt("buddingAmethystMode", BLOCKS, 0, 0, 2, "What budding amethyst drops: 0 nothing, popped by pistons (26.3's), 1 itself to Silk Touch, 2 itself.");
		enableMud = c.getBoolean("enableMud", BLOCKS, true, "Mud as 26.3's: mud (a water bottle on dirt), packed mud, mud bricks, their slab, stairs and wall.");
		enableMossAzalea = c.getBoolean("enableMossAzalea", BLOCKS, true, "Moss and azalea as 26.3's: the moss block (bonemeal spreads a moss patch), its carpet, azalea and flowering azalea (bonemeal grows the azalea tree), their leaves, rooted dirt, hanging roots. (EFR's were an experiment, off: not carried.)");
		enableMangrove = c.getBoolean("enableMangrove", BLOCKS, true, "Mangrove as 26.3's: its logs, roots and muddy roots, leaves and propagule (standing or hanging), the mangrove trees from it. (EFR's were an experiment, off: not carried; its mangrove planks and the rest of the wood set are still EFR's option.)");
		enableCherryTrees = c.getBoolean("enableCherryTrees", BLOCKS, true, "Cherry leaves and the cherry sapling as 26.3's, the cherry tree from it (on EFR's cherry log: EFR's enableCherryBlocks still has the wood set).");
		enableDripstone = c.getBoolean("enableDripstone", BLOCKS, true, "Dripstone as 26.3's: the dripstone block and pointed dripstone (its thickness from its neighbours; stalactites fall and hurt, stalagmites hurt a fall; growth under water; drips that fill cauldrons and turn mud to clay). (EFR's was an experiment, off: not carried.)");
		enableLushCavePlants = c.getBoolean("enableLushCavePlants", BLOCKS, true, "The lush caves' plants as 26.3's: the spore blossom, the big dripleaf (it tilts under you), its stem, the small dripleaf.");
		enableSculk = c.getBoolean("enableSculk", BLOCKS, true, "Sculk as 26.3's, on McFly's game events and vibrations: the sculk sensor and the calibrated sculk sensor (they hear what goes on round them and give a signal by its distance and kind). (EFR's sculk was an experiment, off: not carried.)");
		enableChain = c.getBoolean("enableChain", BLOCKS, true, "The chain as 26.3's iron chain: along the face it's placed on, 26.3's model.");
		enableLantern = c.getBoolean("enableLantern", BLOCKS, true, "Lanterns as 26.3's: standing or hanging by where you look, held by what supports the middle of a face (fences, walls, chains too).");
		enableSoulLighting = c.getBoolean("enableSoulLighting", BLOCKS, true, "The soul torch and soul lantern as 26.3's: light 10, soul fire flames.");
		enableSmithingTable = c.getBoolean("enableSmithingTable", BLOCKS, true, "The smithing table as 26.3's: the netherite upgrade (template, diamond gear, ingot), keeping the gear's damage, enchantments and name.");
		enableNetherite = c.getBoolean("enableNetherite", BLOCKS, true, "Netherite as 26.3's: ancient debris smelted to scrap, the ingot, the block, the tools and armour (fire resistant: lava and fire don't burn them, fire doesn't wear the armour).");
		enableSpectatorMode = c.getBoolean("enableSpectatorMode", PLAYER, true, "Spectator mode (/gamemode spectator, sp or 3): flying through blocks unseen, touching nothing, looking into containers without changing them, following an entity by attacking it.");
		enableShulkerBoxes = c.getBoolean("enableShulkerBoxes", BLOCKS, true, "Shulker boxes as 26.3's: 27 slots that keep their contents when broken, dyed in the grid, washed in a cauldron, placed by dispensers, broken by pistons.");
		enableObservers = c.getBoolean("enableObservers", BLOCKS, true, "The observer as 26.3's: a 2-tick pulse out of its back when the block it faces changes; two facing each other make 26.3's clock.");
		enableCopper = c.getBoolean("enableCopper", BLOCKS, true, "Copper as 26.3's: the block, cut, chiseled, grates, bulbs, slabs, stairs, doors and trapdoors, ageing as they sit, waxed with honeycomb, scraped with an axe, cleaned by lightning; copper ore and raw copper.");
		enableLightningRod = c.getBoolean("enableLightningRod", BLOCKS, true, "The lightning rod as 26.3's (copper must be on): lightning strikes it within 128 blocks, it gives a signal for 8 ticks, it ages as copper does.");
		enableRawOres = c.getBoolean("enableRawOres", BLOCKS, true, "Raw copper, iron and gold as 26.3's: what their ores drop without Silk Touch (copper 2 to 5, iron and gold 1, Fortune's ore bonus on top), smelted to ingots, 9 to a block.");
		enableChorusBlocks = c.getBoolean("enableChorusBlocks", BLOCKS, true, "The End's blocks as 26.3's: chorus plants, flowers and fruit, purpur (block, pillar, stairs, slab), end stone bricks (stairs, slab, wall), end rods.");
		enableBanners = c.getBoolean("enableBanners", BLOCKS, true, "Banners as 26.3's: placed standing or on a wall, patterned in the loom, copied, washed, put on shields.");
		beaconBeamThroughTransparentBlocks = c.getBoolean("beaconBeamThroughTransparentBlocks", BLOCKS, true, "A beacon is on, and its beam shows, through water, ice, glass, leaves, slabs and anything else that is not a fully opaque block, and through bedrock, as since 1.8; 1.7.10 asked the sky map, which any water above the beacon breaks, and so beacons also work under the Nether's roof now. The same rule decides where the coloured beam stops." +
			"\nModified Classes: net.minecraft.tileentity.TileEntityBeacon");
		buttonsOnFullFaces = c.getBoolean("buttonsOnFullFaces", BLOCKS, true, "Buttons and levers can be placed on any full block face, as in modern: glass, stained glass, ice, glowstone, TNT, beacons, spawners and the like, not just on solid opaque blocks. Leaves still can't hold them, as in modern." +
			"\nModified Classes: net.minecraft.block.BlockButton net.minecraft.block.BlockLever");
		dropVehiclesTogether = c.getBoolean("dropVehiclesTogether", BLOCKS, true, "Vehicles drop as one, as since 1.19: a broken chest boat drops its chest boat item, and a minecart with a chest, furnace, hopper or TNT its own minecart item (plus what a chest or hopper held); a TNT minecart that goes off drops nothing. Off: the vehicle and its block apart, as in 1.7.10. Minecarts need a restart; boats take it when placed or loaded.");
		enableBannerPatterns = c.getBoolean("enableBannerPatterns", BLOCKS, true, "The banner pattern items (26.3's, from paper and a thing), for the loom's patterns that need one.");
		enableBreezeRod = c.getBoolean("enableBreezeRod", BLOCKS, true, "The breeze rod (1.21+), the Potion of Wind Charging's reagent; in creative until breezes come.");
		enableGlowInkSac = c.getBoolean("enableGlowInkSac", BLOCKS, true, "The glow ink sac, dropped by glow squids: makes a sign's text glow, and goes into glow item frames.");
		enableGlowItemFrames = c.getBoolean("enableGlowItemFrames", BLOCKS, true, "Glow item frames: an item frame and a glow ink sac; the item in it shows at full light. Needs enableGlowInkSac.");
		enableLodestone = c.getBoolean("enableLodestone", BLOCKS, true, "The lodestone, crafted from eight chiseled stone bricks around an iron ingot. A compass used on it points to it as a lodestone compass, as in modern versions: the compass keeps its kind, with the target as NBT, and spins once the lodestone is gone.");
		enablePowderSnow = c.getBoolean("enablePowderSnow", BLOCKS, true, "Powder snow and its bucket, as in modern versions: entities sink into it and freeze (leather armour, and some mobs, are immune; leather boots walk on top), a burning entity melts it, a full freeze hurts. Without world generation (no snowy slopes in 1.7.10).");
		enableSpyglass = c.getBoolean("enableSpyglass", BLOCKS, true, "The spyglass (1.17+): looked through (held right click) it zooms in ten times, with the scope over the screen and the mouse slowed; others see it held to the eye. Made from an amethyst shard over two copper ingots (needs copper and amethyst).");
		enableTurtleShell = c.getBoolean("enableTurtleShell", BLOCKS, true, "The turtle shell and turtle scutes (1.13+): a helmet giving Water Breathing for 10 s while the head is out of water, crafted and mended with scutes; the Potion of the Turtle Master's reagent. In creative until turtles come.");
		modernCauldrons = c.getBoolean("modernCauldrons", BLOCKS, true, "Cauldrons as in modern: the model (thin walls, a floor, four legs) and its collision (you stand in it 1/16 lower), an empty bucket takes a full water cauldron, a lava cauldron's lava and puts water in it, a lava bucket replaces water at any level.");
		modernChestPlacement = c.getBoolean("modernChestPlacement", BLOCKS, true, "Chests side by side, as 26.3 places them (spec §65): a chest joins a single chest of its kind at its left or right facing the same way, and nothing else; sneaking keeps it single, sneak-clicking a chest's side joins that one; chests can stand side by side in any number. Old worlds' double chests are kept as they were.");
		modernItemFrames = c.getBoolean("modernItemFrames", BLOCKS, true, "Item frames as in modern: on floors and ceilings too, 8 turns of 45 degrees (old frames' 4 turns are read as the same look), read by a comparator through a block (the turn + 1)." +
			"\nModified classes: net.minecraft.entity.EntityHanging, net.minecraft.entity.item.EntityItemFrame, net.minecraft.item.ItemHangingEntity, net.minecraft.block.BlockRedstoneComparator, net.minecraft.client.renderer.tileentity.RenderItemFrame");
		modernPistonEvents = c.getBoolean("modernPistonEvents", BLOCKS, true, "Piston block events act on the piston as it stands when they run, as modern: a piston placed into the spot of one that was just blown up inherits its pending retraction and clears the block in front of its own face, which is the exploding-piston way of breaking bedrock. 1.7.10 used the facing from when the event was queued. With betterPistons on, its piston code applies this rule itself." +
			"\nModified Classes: net.minecraft.block.BlockPistonBase");
		modernRarity = c.getBoolean("modernRarity", BLOCKS, true, "The held item's name above the hotbar is in its rarity's colour (italic when renamed), and enchanting raises an item's rarity a step (common to rare, rare to epic), as in modern. 1.7.10 drew the name white, and an enchanted item was rare whatever it was.");
		modernSignText = c.getBoolean("modernSignText", BLOCKS, true, "Signs take dyes (the text's colour), glow ink sacs (glowing text), ink sacs (no glow) and honeycomb (waxed: takes nothing more), as in modern; vanilla's and EFR's signs. With EFR's new dyes on, bone meal, lapis, cocoa beans and ink sacs aren't dyes for signs (the ink sac takes the glow off)." +
			"\nModified classes: net.minecraft.tileentity.TileEntitySign, net.minecraft.block.BlockSign, net.minecraft.client.renderer.tileentity.TileEntitySignRenderer");
		modernStepSounds = c.getBoolean("modernStepSounds", BLOCKS, true, "A player's footsteps on carpets, moss carpets, snow layers and nether roots and sprouts play that block together with the block under it, quieter and lower, and on lily pads, glow lichen, pink petals and small amethyst buds only that block, as in modern (1.20+). 1.7.10 played only the block under them (or nothing, from water under a lily pad), except snow layers." +
			"\nModified classes: net.minecraft.entity.Entity");
		modernTntMinecarts = c.getBoolean("modernTntMinecarts", BLOCKS, true, "A TNT minecart broken by fire, an explosion or a burning projectile, or while moving, is lit with a short fuse (0 to 2 seconds) instead of blowing up at once, and a burning arrow sets it off at once, as in modern. 1.7.10 blew it up when broken and only set it alight from a burning arrow.");
		modernWitherSkulls = c.getBoolean("modernWitherSkulls", BLOCKS, true, "A wither skull hits as its own damage, from the skull (\"was shot by a skull from\"), so it breaks armour stands as modern. 1.7.10 dealt it as the wither's melee.");
		netheriteUpgradeTemplate = c.getBoolean("netheriteUpgradeTemplate", BLOCKS, true, "Netherite gear takes the Netherite Upgrade smithing template besides the diamond gear and the netherite ingot, as since 1.20: the smithing table gets modern's three input slots. The template is found in Nether fortress chests (this version has no bastions) and copied with seven diamonds, a block of netherrack and a template. Off: the two-slot table of 1.16 and no template item. Needs enableNetherite and enableSmithingTable.");

		itemsBackOnClose = c.getBoolean("itemsBackOnClose", PLAYER, true, "Closing a crafting table, the inventory's crafting grid, an anvil, an enchanting table or a smithing table puts what was in it, and the stack on the cursor, back into the inventory, as modern does; only what doesn't fit drops, and everything drops if the player is dead or disconnected. The client no longer spawns a copy of each dropped item of its own either." +
			"\nModified Classes: net.minecraft.inventory.Container net.minecraft.inventory.ContainerWorkbench net.minecraft.inventory.ContainerPlayer net.minecraft.inventory.ContainerRepair net.minecraft.inventory.ContainerEnchantment");
		modernAnvilCosts = c.getBoolean("modernAnvilCosts", PLAYER, true, "The anvil prices as in modern (1.8+): an enchantment costs its rarity's price (halved from a book) times the level it ends up at, renaming costs 1 level, mending with material 1 a unit, and an item's prior-work cost doubles with each use instead of growing by 2. 1.7.10 charged again for every enchantment the item already had, so a Mending book on a fresh pickaxe cost 6 instead of 2." +
			"\nModified Classes: net.minecraft.inventory.ContainerRepair");
		modernBedUse = c.getBoolean("modernBedUse", PLAYER, true, "Right-clicking a bed works as in modern: a bed out of reach (either half's middle more than 3 blocks across or 2 up or down) says \"You may not rest now; the bed is too far away\"; a bed in reach sets your spawn point even when you can't sleep yet (by day, or with monsters near), with \"Respawn point set\" when the spot changed. 1.7.10 said nothing from afar, measured from the head's corner, and set the spawn point only once you had slept or got into bed at night." +
			"\nModified Classes: net.minecraft.entity.player.EntityPlayer net.minecraft.block.BlockBed");
		modernBlockUsePackets = c.getBoolean("modernBlockUsePackets", PLAYER, true, "The client sends every right-click on a block to the server before trying it itself, as since 1.13. 1.7.10 sent nothing when it thought the held block could not be placed there, so a click could never reach the server in the tick that freed the spot. The server decides either way and corrects the client when it placed something the client did not expect. Needed for the exploding-piston bedrock method. The server's placement check then counts the placing player's own body (1.7.10 left that to the client; since 1.8 the server checks it), so a block can't be placed where you stand." +
			"\nModified Classes: net.minecraft.world.World" +
			"\nModified Client Classes: net.minecraft.client.multiplayer.PlayerControllerMP");
		modernCreativeFilling = c.getBoolean("modernCreativeFilling", PLAYER, true, "In creative, filling a bucket or a bottle (from water, lava, a cauldron, a cow) keeps the empty one and adds the filled one if you have none yet, as in modern. 1.7.10 kept buckets empty, used bottles up and gave no milk in creative.");
		modernCreativeInventoryTab = c.getBoolean("modernCreativeInventoryTab", PLAYER, true, "The creative screen's survival inventory tab laid out as modern's (1.9+): the armour slots in two columns with the player drawn between them, following the mouse, the off-hand slot at their left and modern's background for the tab." +
			"\nModified Client Classes: net.minecraft.client.gui.inventory.GuiContainerCreative");
		modernHunger = c.getBoolean("modernHunger", PLAYER, true, "Hunger and healing as in modern (1.9 to 1.11): with saturation left and a full food bar, health comes back every half second from saturation; the slow healing below that costs 6 exhaustion instead of 3. Exhaustion costs are modern's, 1.7.10's in brackets: sprinting 0.1 per metre (same), walking and sneaking nothing (0.01), swimming 0.01 (0.015), a jump 0.05 and a sprint jump 0.2 (0.2 and 0.8), a hit landed 0.1 (0.3), damage taken 0.1 (0.3) and none from starving, breaking a block 0.005 (0.025), the Hunger effect 0.005 per level per tick (0.025)." +
			"\nModified Classes: net.minecraft.util.FoodStats net.minecraft.entity.player.EntityPlayer net.minecraft.util.DamageSource net.minecraft.potion.Potion net.minecraft.block.Block");
		modernPlacementShape = c.getBoolean("modernPlacementShape", PLAYER, true, "Placing a ladder or a trapdoor checks the player against the shape the block will have once placed (its orientation, and for a trapdoor its half and whether power opens it), as since 1.13, instead of the block's last computed bounds, often a full block: standing a little way from a wall no longer stops a ladder going onto it, and a trapdoor against a powered block, which is placed open as in modern, only needs its upright strip clear." +
			"\nModified Classes: net.minecraft.item.ItemBlock net.minecraft.block.BlockTrapDoor" +
			"\nModified Client Classes: net.minecraft.item.ItemBlock");
		pickBlockFromInventory = c.getBoolean("pickBlockFromInventory", PLAYER, true, "Pick block (middle click) finds the block anywhere in your inventory and swaps it into the hotbar, as since 1.8. 1.7.10 only looks at the hotbar.");
		placementFacingByLook = c.getBoolean("placementFacingByLook", PLAYER, true, "Pistons, dispensers, droppers and observers face by where you look when placed, vertical when you look mostly up or down, as since 1.13. 1.7.10 decides the vertical cases from where you stand relative to the block." +
			"\nModified Classes: net.minecraft.block.BlockPistonBase");
		rightClickArmorSwap = c.getBoolean("rightClickArmorSwap", PLAYER, true, "Right-clicking with a piece of armor in hand puts it on and takes the worn piece into the hand, as since 1.19.4. 1.7.10 only puts it on when the slot is empty. Armor with a right-click action of its own keeps that action.");
		shiftClickIntoCraftingGrid = c.getBoolean("shiftClickIntoCraftingGrid", PLAYER, true, "Shift-clicking a stack from the inventory or the hotbar in the crafting table puts it into the crafting grid first, onto matching stacks or the first empty cell, as since 1.12; only when the grid takes nothing does it move between the inventory and the hotbar as before." +
			"\nModified Classes: net.minecraft.inventory.ContainerWorkbench");

		modernExplosionOrder = c.getBoolean("modernExplosionOrder", WORLD, true, "Explosions destroy their blocks in random order, as since 1.9. 1.7.10 used a fixed order for a given layout, so whether a block notices a neighbour going before it goes itself (a piston losing its lever, say) was decided by the layout rather than by chance. Needed for the exploding-piston bedrock method to be a matter of tries, as in modern." +
			"\nModified Classes: net.minecraft.world.Explosion");
		modernStrongholds = c.getBoolean("modernStrongholds", WORLD, true, "Strongholds placed as in modern: strongholdCount of them in rings around the world's middle, the first ring of 3 about 1400 to 2700 blocks out, then rings of 6, 10, 15 and more, each farther. 1.7.10 has only 3, 640 to 1150 blocks out. In an existing world the strongholds already generated stay where they are.");
		modernWeatherPace = c.getBoolean("modernWeatherPace", WORLD, true, "Rain and snow work on the world at modern's pace: a chunk's ice, snow layer and cauldron filling come once in 48 ticks instead of 1.7.10's 16.");
		portalShatters = c.getBoolean("portalShatters", WORLD, true, "A nether portal whose frame breaks shatters with the glass breaking sound and particles, as in modern, instead of vanishing silently." +
			"\nModified Classes: net.minecraft.block.BlockPortal");
		strongholdCount = c.getInt("strongholdCount", WORLD, 128, 1, 4096, "How many strongholds there are when modernStrongholds is on (modern: 128). A superflat preset's own stronghold count still wins.");

		arrowHarmingAfterTotemFix = c.getBoolean("arrowHarmingAfterTotemFix", FIXES, true, "Fixes MC-206307: a tipped arrow of Harming whose own hit has just used up a totem of undying doesn't then deal its Harming, which comes after the hit and could kill right after the totem saved you. Its other effects still apply. The same remedy as Carpet Fixes' arrowEffectsBypassTotemsFix; false keeps the bug, as in 26.3.");
		connectionAcceptFix = c.getBoolean("connectionAcceptFix", FIXES, true, "Fixes the rare hang on the loading screen when entering a world, and a dedicated server never finishing a player's login: the server's network tick could drop a new connection in the instant between it being listed and its channel becoming active, after which it was never read. 1.8 fixed the same race." +
			"\nModified Classes: net.minecraft.network.NetworkSystem net.minecraft.network.NetworkManager");
		thinBlockStepSoundFix = c.getBoolean("thinBlockStepSoundFix", FIXES, true, "Fixes MC-8820 (open since 2013): walking on a block thinner than 0.2, such as a bottom-half trapdoor or a carpet, plays the step sound of the block under it, and nothing at all over air. With the fix, the block you stand on gives the sound. false keeps the bug, as in 26.3." +
			"\nModified classes: net.minecraft.entity.Entity");

		if (modernOptions && optionsTxtLoad != null) {
			optionsTxtLoad.run();
		}
	}

	/** The config screen's categories of ours (EFR's screen lists them under McFly in this fork). */
	@SuppressWarnings("rawtypes")
	public static List<IConfigElement> screenCategories() {
		List<IConfigElement> categories = new ArrayList<>();
		for (String category : file.getCategoryNames()) {
			categories.add(new ConfigElement(file.getCategory(category)));
		}
		return categories;
	}

	/** Closing the config screen with a change: our file saved and its values read again. */
	public static final ScreenListener SCREEN_LISTENER = new ScreenListener();

	/** Public, as FML's generated event handlers call it from their own package. */
	public static final class ScreenListener {
		private ScreenListener() {
		}

		@SubscribeEvent
		public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
			if (SCREEN_MOD_ID.equals(event.modID) && file != null) {
				read(file);
				if (file.hasChanged()) {
					file.save();
				}
			}
		}
	}

	/** With modernOptions on, the values just set go to the client's options.txt instead of this file. */
	private static boolean savedInOptionsTxt() {
		if (modernOptions && optionsTxtSave != null) {
			optionsTxtSave.run();
			return true;
		}
		return false;
	}

	/** One value saved in this file. */
	private static void save(String category, String key, Object value) {
		if (file == null || !file.hasCategory(category) || !file.getCategory(category).containsKey(key)) {
			return;
		}
		file.getCategory(category).get(key).set(String.valueOf(value));
		file.save();
	}

	/** The shown skin parts in SkinPart order. */
	public static boolean[] skinPartsShown() {
		return new boolean[] {skinCape, skinJacket, skinLeftSleeve, skinRightSleeve, skinLeftPantsLeg, skinRightPantsLeg, skinHat};
	}

	/** The FOV Effects slider's value, kept here as modern keeps it in its options. */
	public static void saveFovEffectScale(float value) {
		fovEffectScale = value;
		if (!savedInOptionsTxt()) {
			save(CLIENT, "fovEffectScale", value);
		}
	}

	/** The Attack Indicator button in Video Settings (crosshair, hotbar, off), kept here as modern keeps it in its options. */
	public static void saveAttackIndicator(String value) {
		attackIndicator = value;
		if (!savedInOptionsTxt()) {
			save(CLIENT, "attackIndicator", value);
		}
	}

	/** The Command Suggestions button in Chat Settings, kept here as modern keeps it in its options. */
	public static void saveCommandSuggestions(boolean value) {
		commandSuggestions = value;
		if (!savedInOptionsTxt()) {
			save(CLIENT, "commandSuggestions", value);
		}
	}

	/** The Skin Customization screen's choices, kept here as modern keeps them in its options. */
	public static void saveSkinOptions(boolean[] shown, boolean leftHanded) {
		skinCape = shown[0];
		skinJacket = shown[1];
		skinLeftSleeve = shown[2];
		skinRightSleeve = shown[3];
		skinLeftPantsLeg = shown[4];
		skinRightPantsLeg = shown[5];
		skinHat = shown[6];
		mainHandLeft = leftHanded;
		if (savedInOptionsTxt()) {
			return;
		}
		String[] keys = {"skinCape", "skinJacket", "skinLeftSleeve", "skinRightSleeve", "skinLeftPantsLeg", "skinRightPantsLeg", "skinHat"};
		for (int i = 0; i < keys.length; i++) {
			save(CLIENT, keys[i], shown[i]);
		}
		save(CLIENT, "mainHandLeft", leftHanded);
	}
}
