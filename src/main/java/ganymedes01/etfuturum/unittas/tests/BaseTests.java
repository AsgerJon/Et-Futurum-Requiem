package ganymedes01.etfuturum.unittas.tests;

import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import ganymedes01.etfuturum.unittas.TasTestCase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Our own base (SPEC-modern-movement.md §79, B1 and B2): McFly's base class started with the game; our options in a
 * file of our own (config/mcfly.cfg), none of them left in EFR's files, carried once from EFR's files and taken out of
 * them, and editable on the in-game config screen. McFly's classes are reached by name, so these compile (and fail)
 * before they exist.
 */
public class BaseTests extends TasTestCase {

	/** The options this fork added to EFR's config files (122, as of 2026-11-04; §79 B2). */
	static final String[] OUR_OPTIONS = {
		"aggressiveDataWatcherId", "arrowHarmingAfterTotemFix", "arrowsInheritMotion", "attackIndicator",
		"bannerGridPatterns", "beaconBeamThroughTransparentBlocks", "buttonsOnFullFaces", "channeling",
		"channelingID", "chatHistoryAcrossRestarts", "chatHistoryBetweenWorlds", "chatLogBetweenWorlds",
		"chorusOnlyOnOuterIslands", "commandBlockCreativeOnly", "commandSuggestions", "connectionAcceptFix",
		"coordinateSuggestions", "depthStrider", "depthStriderID", "dropVehiclesTogether", "effectPanelAlphaFix",
		"enableBannerPatterns", "enableBreezeRod", "enableFloatingItems", "enableGlowInkSac", "enableGlowItemFrames",
		"enableGlowSquidSpawns", "enableGlowSquids", "enableKelp", "enableLodestone", "enableLuck",
		"enableModernBreathing", "enableModernSneaking", "enableModernSwimming", "enableOffhand",
		"enablePhantomSpawns", "enablePhantoms", "enablePolarBearSpawns", "enablePolarBears", "enablePowderSnow",
		"enableShield", "enableSpyglass", "enableTrialEffects", "enableTrident", "enableTurtleShell", "endBarrensID",
		"endCreditsOnce", "endHighlandsID", "endMidlandsID", "firstPersonCameraAtEyes", "fovEffectScale",
		"fovEffects", "giveAbove64", "guiScaleWheel", "impaling", "impalingID", "itemsBackOnClose", "loyalty",
		"loyaltyID", "mainHandLeft", "modernAnvilCosts", "modernBedUse", "modernBlockUsePackets", "modernCauldrons",
		"modernChat", "modernChestPlacement", "modernCombat", "modernCreativeFilling", "modernCreativeInventoryTab",
		"modernEnd", "modernExplosionOrder", "modernHeldItems", "modernHorsesInWater", "modernHunger",
		"modernInventoryFigure", "modernItemFrames", "modernKeyRepeat", "modernLightning", "modernMobArms",
		"modernMobSpawnLight", "modernOptions", "modernPanic", "modernPistonEvents", "modernPlacementShape",
		"modernPotionColors", "modernRarity", "modernSignText", "modernStepSounds", "modernStrongholds",
		"modernTntMinecarts", "modernWeatherPace", "modernWitherSkulls", "namedEffects", "netheriteUpgradeTemplate",
		"noPotionGlint", "offhandDataWatcherId", "offlineSkins", "pickBlockFromInventory", "placementFacingByLook",
		"portalShatters", "poseDataWatcherId", "potionsByName", "rightClickArmorSwap", "riptide", "riptideID",
		"shiftClickEquipsElytra", "shiftClickIntoCraftingGrid", "skinCape", "skinCustomization", "skinHat",
		"skinJacket", "skinLeftPantsLeg", "skinLeftSleeve", "skinPartsDataWatcherId", "skinRightPantsLeg",
		"skinRightSleeve", "smallEndIslandsID", "strongholdCount", "sweepingEdge", "sweepingEdgeID",
		"thinBlockStepSoundFix", "undeadHorseLeads"
	};

	private static final String BASE = "io.github.asgerjon.mcfly.McFly";
	private static final String CONFIG = "io.github.asgerjon.mcfly.config.McFlyConfig";
	private static final Pattern KEY = Pattern.compile("^\\s*[BIDS]:(?:\"([^\"]+)\"|([^=<\\s]+))\\s*[=<]");

	private static Class<?> mcfly(String name) {
		try {
			return Class.forName(name);
		} catch (ClassNotFoundException e) {
			fail("no " + name);
			return null;
		}
	}

	/** The option names a Forge config file holds (`B:name=value`, `S:"a name"=value`, lists `S:name <`). */
	static Set<String> keys(File file) throws IOException {
		Set<String> found = new TreeSet<>();
		if (!file.isFile()) {
			return found;
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			for (String line; (line = reader.readLine()) != null; ) {
				Matcher matcher = KEY.matcher(line);
				if (matcher.find()) {
					found.add(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
				}
			}
		}
		return found;
	}

	private File configDir() {
		return onClient(() -> new File(mc().mcDataDir, "config"));
	}

	/** B1: McFly's base class has been through its start-up with the game, up to the server starting. */
	public void testBaseStarted() throws Exception {
		Class<?> base = mcfly(BASE);
		assertEqual("server started", base.getMethod("phase").invoke(null), "McFly's start-up phase reached");
	}

	/** B2: every option of ours is in config/mcfly.cfg, and none is left in EFR's files. */
	public void testOptionsInOwnFile() throws IOException {
		File dir = configDir();
		Set<String> ours = keys(new File(dir, "mcfly.cfg"));
		List<String> missing = new ArrayList<>();
		for (String option : OUR_OPTIONS) {
			if (!ours.contains(option)) {
				missing.add(option);
			}
		}
		Set<String> inEfr = new TreeSet<>();
		File[] efrFiles = new File(dir, "etfuturum").listFiles((folder, name) -> name.endsWith(".cfg"));
		for (File file : efrFiles == null ? new File[0] : efrFiles) {
			Set<String> held = keys(file);
			for (String option : OUR_OPTIONS) {
				if (held.contains(option)) {
					inEfr.add(file.getName() + ":" + option);
				}
			}
		}
		assertEqual("missing from mcfly.cfg [], left in EFR's files []", "missing from mcfly.cfg " + missing + ", left in EFR's files " + inEfr,
			"our 122 options' file");
	}

	/**
	 * B2: an option's value in EFR's old file is carried into ours once, and taken out of EFR's file (EFR's own options
	 * stay). On a folder of its own: an EFR mixins.cfg holding one of ours (modernEnd, set off) and one of EFR's.
	 */
	@SuppressWarnings("unchecked")
	public void testCarriedFromEfrOnce() throws Exception {
		Class<?> config = mcfly(CONFIG);
		File dir = new File(resultsFolder(), "carry-test/config");
		File efr = new File(dir, "etfuturum");
		if (!efr.isDirectory() && !efr.mkdirs()) {
			fail("can't make " + efr);
		}
		File mixins = new File(efr, "mixins.cfg");
		try (Writer out = new OutputStreamWriter(new FileOutputStream(mixins), StandardCharsets.UTF_8)) {
			out.write("# Configuration file\n\n\"backported features\" {\n    B:endPortalFix=true\n    B:modernEnd=false\n}\n\n");
		}
		Method take = config.getMethod("takeFromEfr", File.class);
		Map<String, String> carried = (Map<String, String>) take.invoke(null, dir);
		assertEqual("{modernEnd=false} [endPortalFix]", carried + " " + keys(mixins), "carried from EFR's file, and what that file holds after");
	}

	/**
	 * B3: our mixins come from our own mixin config (`mixins.mcfly.early.json`, McFly's package): the methods Mixin
	 * merged into vanilla classes (each marked with its mixin's name) come from McFly's package for ours, and none of our
	 * mixins is still applied from EFR's package (as the same name there). (Mixin's own list of configs holds only those
	 * not yet applied, so it can't show ours.)
	 */
	@SuppressWarnings("unchecked")
	public void testMixinsFromOurConfig() throws Exception {
		Class<?> list = mcfly("io.github.asgerjon.mcfly.mixinplugin.McFlyMixins");
		java.util.List<String> ours = (java.util.List<String>) list.getField("EARLY").get(null);
		String[] targets = {"net.minecraft.entity.player.EntityPlayer", "net.minecraft.entity.EntityLivingBase", "net.minecraft.entity.Entity",
			"net.minecraft.entity.boss.EntityDragon", "net.minecraft.entity.item.EntityEnderCrystal", "net.minecraft.tileentity.TileEntityBeacon",
			"net.minecraft.block.BlockPane", "net.minecraft.world.World", "net.minecraft.client.Minecraft", "net.minecraft.client.renderer.EntityRenderer",
			"net.minecraft.client.entity.EntityPlayerSP"};
		Set<String> fromOurs = new TreeSet<>();
		Set<String> fromEfrPackage = new TreeSet<>();
		for (String target : targets) {
			for (Method method : Class.forName(target).getDeclaredMethods()) {
				org.spongepowered.asm.mixin.transformer.meta.MixinMerged merged = method.getAnnotation(org.spongepowered.asm.mixin.transformer.meta.MixinMerged.class);
				if (merged == null) {
					continue;
				}
				String mixin = merged.mixin();
				if (mixin.startsWith("io.github.asgerjon.mcfly.mixins.early.")) {
					fromOurs.add(mixin);
				} else if (mixin.startsWith("ganymedes01.etfuturum.mixins.early.") && ours.contains(mixin.substring("ganymedes01.etfuturum.mixins.early.".length()))) {
					fromEfrPackage.add(mixin);
				}
			}
		}
		assertTrue(fromOurs.size() >= 5, "merges from McFly's package in the sampled classes: " + fromOurs);
		assertEqual("[]", fromEfrPackage.toString(), "our mixins still applied from EFR's package");
	}

	/** Our network messages (18, as of 2026-11-04; §79 B4). */
	static final String[] OUR_MESSAGES = {"SwapHandsMessage", "UseOffhandMessage", "PlayerSettingsMessage", "OfflineSkinMessage",
		"CommandTreeMessage", "CommandSuggestionRequestMessage", "CommandSuggestionsMessage", "PunchMessage", "ItemCooldownMessage",
		"SweepParticleMessage", "OverlayMessage", "FreezingMessage", "SnowflakeMessage", "StrayConversionMessage", "EffectTableMessage",
		"EndBossBarMessage", "DragonBreathBurstMessage", "LevelEventMessage"};

	/** The message classes a channel's codec knows (FML's SimpleNetworkWrapper, by reflection). */
	@SuppressWarnings("rawtypes")
	private static Set<String> messagesOf(Object wrapper) throws Exception {
		java.lang.reflect.Field codecField = wrapper.getClass().getDeclaredField("packetCodec");
		codecField.setAccessible(true);
		Object codec = codecField.get(wrapper);
		java.lang.reflect.Field types = cpw.mods.fml.common.network.FMLIndexedMessageToMessageCodec.class.getDeclaredField("types");
		types.setAccessible(true);
		Object map = types.get(codec);
		Set<String> names = new TreeSet<>();
		for (Object type : (Iterable) map.getClass().getMethod("keySet").invoke(map)) {
			names.add(((Class<?>) type).getSimpleName());
		}
		return names;
	}

	/** B4: our messages travel on a channel of our own, both ways; EFR's channel carries none of them. */
	public void testOurMessagesOnOurChannel() throws Exception {
		Class<?> network = mcfly("io.github.asgerjon.mcfly.network.McFlyNetwork");
		Set<String> onOurs = messagesOf(network.getField("CHANNEL").get(null));
		Set<String> onEfrs = messagesOf(ganymedes01.etfuturum.EtFuturum.networkWrapper);
		List<String> missing = new ArrayList<>();
		List<String> onEfr = new ArrayList<>();
		for (String message : OUR_MESSAGES) {
			if (!onOurs.contains(message)) {
				missing.add(message);
			}
			if (onEfrs.contains(message)) {
				onEfr.add(message);
			}
		}
		assertEqual("missing from ours [], on EFR's []", "missing from ours " + missing + ", on EFR's " + onEfr, "our 18 messages' channel");
	}

	/**
	 * B5: our sound events are McFly's own list (55, as of 2026-11-04), handed to MCLib's asset director, and each
	 * resolves to a sound on the client (fetched from the launcher's assets).
	 */
	@SuppressWarnings("unchecked")
	public void testOurSoundsResolve() throws Exception {
		Class<?> sounds = mcfly("io.github.asgerjon.mcfly.sound.McFlySounds");
		Map<String, String> events = (Map<String, String>) sounds.getField("EVENTS").get(null);
		List<String> silent = onClient(() -> {
			List<String> missing = new ArrayList<>();
			for (String event : events.keySet()) {
				if (mc().getSoundHandler().getSound(new net.minecraft.util.ResourceLocation(ganymedes01.etfuturum.lib.Reference.MCAssetVer, event)) == null) {
					missing.add(event);
				}
			}
			return missing;
		});
		assertEqual("55 events, none silent []", events.size() + " events, none silent " + silent, "McFly's sound events");
	}

	/** The blocks this fork added to EFR's list or rebuilt in it (13, as of 2026-11-04; §79 B6), by their entry names. */
	static final String[] OUR_BLOCKS = {
		"LIGHTNING_ROD", "EXPOSED_LIGHTNING_ROD", "WEATHERED_LIGHTNING_ROD", "OXIDIZED_LIGHTNING_ROD", "WAXED_LIGHTNING_ROD",
		"WAXED_EXPOSED_LIGHTNING_ROD", "WAXED_WEATHERED_LIGHTNING_ROD", "WAXED_OXIDIZED_LIGHTNING_ROD", "LODESTONE",
		"POWDER_SNOW", "POWDER_SNOW_CAULDRON", "KELP", "DRIED_KELP_BLOCK"
	};

	/** The items this fork added to EFR's list (22, as of 2026-11-04; §79 B6). */
	static final String[] OUR_ITEMS = {
		"DRIED_KELP", "NETHERITE_UPGRADE_SMITHING_TEMPLATE", "SHIELD", "TRIDENT", "POWDER_SNOW_BUCKET", "SPYGLASS",
		"GLOW_INK_SAC", "PHANTOM_MEMBRANE", "BREEZE_ROD", "FLOWER_BANNER_PATTERN", "CREEPER_BANNER_PATTERN",
		"SKULL_BANNER_PATTERN", "MOJANG_BANNER_PATTERN", "FIELD_MASONED_BANNER_PATTERN", "BORDURE_INDENTED_BANNER_PATTERN",
		"GLOBE_BANNER_PATTERN", "PIGLIN_BANNER_PATTERN", "FLOW_BANNER_PATTERN", "GUSTER_BANNER_PATTERN", "TURTLE_SCUTE",
		"TURTLE_HELMET", "GLOW_ITEM_FRAME"
	};

	/**
	 * Our entities (9, as of 2026-11-04; §79 B6) by their registry names, with the mod entity ids they had in EFR's
	 * list (the shulker and its bullet were EFR's entries; their classes are ours, rebuilt in §76.13).
	 */
	static final Object[][] OUR_ENTITIES = {
		{"shulker", 16}, {"shulker_candy", 17}, {"trident", 23}, {"polar_bear", 24}, {"glow_squid", 25},
		{"glow_item_frame", 26}, {"phantom", 27}, {"area_effect_cloud", 28}, {"dragon_fireball", 29}
	};

	/**
	 * The order EFR's list gave its spawn eggs their ids (each the next free one from 500), ours among them: an egg's
	 * id is its item's damage in a save, so the move must keep it.
	 */
	static final String[] EGG_ORDER = {
		"rabbit", "endermite", "husk", "stray", "villager_zombie", "shulker", "bee", "fox", "polar_bear", "phantom", "glow_squid"
	};

	private static final String REGISTRY = "io.github.asgerjon.mcfly.registry.";

	private static Set<String> names(Object[] entries) {
		Set<String> names = new LinkedHashSet<>();
		for (Object entry : entries) {
			names.add(((Enum<?>) entry).name());
		}
		return names;
	}

	/**
	 * B6: our blocks and items are McFly's lists' own (`McFlyBlocks`, `McFlyItems`), each one that's on registered by
	 * them under its old id (`etfuturum:` and the entry's name, as EFR's lists named them: old saves keep them), and
	 * none is left in EFR's lists.
	 */
	public void testOurBlocksAndItemsRegistered() throws Exception {
		List<String> wrong = new ArrayList<>();
		int on = 0;
		Object[][] lists = {
			{mcfly(REGISTRY + "McFlyBlocks"), OUR_BLOCKS, names(ganymedes01.etfuturum.ModBlocks.values()), cpw.mods.fml.common.registry.GameData.getBlockRegistry()},
			{mcfly(REGISTRY + "McFlyItems"), OUR_ITEMS, names(ganymedes01.etfuturum.ModItems.values()), cpw.mods.fml.common.registry.GameData.getItemRegistry()}
		};
		for (Object[] list : lists) {
			Class<?> ours = (Class<?>) list[0];
			Set<String> mcflys = names(ours.getEnumConstants());
			for (String name : (String[]) list[1]) {
				if (((Set<?>) list[2]).contains(name)) {
					wrong.add(name + " still in EFR's list");
				}
				if (!mcflys.contains(name)) {
					wrong.add(name + " not in " + ours.getSimpleName());
					continue;
				}
				Object entry = ours.getMethod("valueOf", String.class).invoke(null, name);
				if (!(Boolean) ours.getMethod("isEnabled").invoke(entry)) {
					continue;
				}
				on++;
				Object thing = ours.getMethod("get").invoke(entry);
				Object id = ((net.minecraft.util.RegistryNamespaced) list[3]).getNameForObject(thing);
				if (!("etfuturum:" + name.toLowerCase()).equals(id)) {
					wrong.add(name + " registered as " + id);
				}
			}
		}
		assertEqual("[]", wrong.toString(), "our blocks and items, " + on + " of them on");
	}

	/**
	 * B6: our entities are McFly's list's own (`McFlyEntities`), registered by it under their old names and mod entity
	 * ids (FML's own record), and EFR's list holds none of them.
	 */
	public void testOurEntitiesRegistered() throws Exception {
		Class<?> ours = mcfly(REGISTRY + "McFlyEntities");
		cpw.mods.fml.common.ModContainer efr = cpw.mods.fml.common.Loader.instance().getIndexedModList().get("etfuturum");
		Set<String> listed = new TreeSet<>();
		for (Object entry : ours.getEnumConstants()) {
			listed.add((String) ours.getMethod("entityName").invoke(entry));
		}
		List<String> wrong = new ArrayList<>();
		for (Object[] entity : OUR_ENTITIES) {
			String name = (String) entity[0];
			int id = (Integer) entity[1];
			if (!listed.contains(name)) {
				wrong.add(name + " not in McFlyEntities");
			}
			cpw.mods.fml.common.registry.EntityRegistry.EntityRegistration registration = cpw.mods.fml.common.registry.EntityRegistry.instance().lookupModSpawn(efr, id);
			if (registration == null || !name.equals(registration.getEntityName())) {
				wrong.add(id + " is " + (registration == null ? "nothing" : registration.getEntityName()));
			}
			if (ganymedes01.etfuturum.entities.ModEntityList.getData(id) != null) {
				wrong.add(name + " still in EFR's list");
			}
		}
		assertEqual("[]", wrong.toString(), "our 9 entities");
	}

	/**
	 * B6: the spawn eggs' ids come out in EFR's old order, ours among EFR's (an egg's id is its damage in a save), and
	 * the items tab still lists an egg for each.
	 */
	@SuppressWarnings("unchecked")
	public void testSpawnEggsKeepTheirIds() {
		Map<Integer, Class<?>> classes = new java.util.TreeMap<>();
		for (Object id : net.minecraft.entity.EntityList.entityEggs.keySet()) {
			Class<?> type = (Class<?>) net.minecraft.entity.EntityList.IDtoClassMapping.get(id);
			if ((Integer) id >= 500 && type != null) {
				classes.put((Integer) id, type);
			}
		}
		List<String> order = new ArrayList<>();
		List<Integer> ids = new ArrayList<>();
		for (Map.Entry<Integer, Class<?>> egg : classes.entrySet()) {
			cpw.mods.fml.common.registry.EntityRegistry.EntityRegistration registration =
				cpw.mods.fml.common.registry.EntityRegistry.instance().lookupModSpawn((Class<? extends net.minecraft.entity.Entity>) egg.getValue(), false);
			order.add(registration == null ? egg.getValue().getSimpleName() : registration.getEntityName());
			ids.add(egg.getKey());
		}
		List<String> expected = new ArrayList<>();
		for (String name : EGG_ORDER) {
			if (order.contains(name)) {
				expected.add(name);
			}
		}
		List<Integer> inTab = onClient(() -> {
			List<net.minecraft.item.ItemStack> items = new ArrayList<>();
			ganymedes01.etfuturum.EtFuturum.creativeTabItems.displayAllReleventItems(items);
			List<Integer> eggs = new ArrayList<>();
			for (net.minecraft.item.ItemStack stack : items) {
				if (stack.getItem() == net.minecraft.init.Items.spawn_egg) {
					eggs.add(stack.getItemDamage());
				}
			}
			return eggs;
		});
		List<Integer> notInTab = new ArrayList<>(ids);
		notInTab.removeAll(inTab);
		assertEqual(expected + " from " + (ids.isEmpty() ? "-" : ids.get(0)) + " on, none missing from the tab []",
			order + " from " + (ids.isEmpty() ? "-" : ids.get(0)) + " on, none missing from the tab " + notInTab, "spawn eggs " + ids);
	}

	/**
	 * B2: the in-game config screen lists our options under McFly, and a change made there (as the screen makes it,
	 * then the event it fires on closing) is saved in config/mcfly.cfg.
	 */
	@SuppressWarnings("rawtypes")
	public void testConfigScreenSavesOurs() throws Exception {
		Class<?> config = mcfly(CONFIG);
		Method elements = Class.forName("ganymedes01.etfuturum.configuration.ConfigGUI").getDeclaredMethod("getElements");
		elements.setAccessible(true);
		IConfigElement strongholds = null;
		for (Object entry : (List) elements.invoke(null)) {
			IConfigElement file = (IConfigElement) entry;
			if (file.getName().equals("McFly")) {
				for (Object category : file.getChildElements()) {
					for (Object option : ((IConfigElement) category).getChildElements()) {
						if (((IConfigElement) option).getName().equals("strongholdCount")) {
							strongholds = (IConfigElement) option;
						}
					}
				}
			}
		}
		assertIsNotNone(strongholds, "no strongholdCount under McFly on the config screen");
		IConfigElement option = strongholds;
		String before = String.valueOf(option.get());
		try {
			onClient(() -> {
				option.set("129");
				FMLCommonHandler.instance().bus().post(new ConfigChangedEvent.OnConfigChangedEvent("etfuturum", null, false, false));
			});
			String saved = null;
			File file = new File(configDir(), "mcfly.cfg");
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
				for (String line; (line = reader.readLine()) != null; ) {
					if (line.trim().startsWith("I:strongholdCount=")) {
						saved = line.trim().substring("I:strongholdCount=".length());
					}
				}
			}
			assertEqual("129 129", saved + " " + config.getField("strongholdCount").get(null), "strongholdCount in mcfly.cfg and read back");
		} finally {
			onClient(() -> {
				option.set(before);
				FMLCommonHandler.instance().bus().post(new ConfigChangedEvent.OnConfigChangedEvent("etfuturum", null, false, false));
			});
		}
	}
}
