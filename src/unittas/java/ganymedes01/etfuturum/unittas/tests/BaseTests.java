package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.mcfly.McFly;
import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import io.github.asgerjon.unittas.TasTestCase;

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
@TasSuite("base")
public class BaseTests extends TasTestCase {

	/** The options this fork added to EFR's config files (122, as of 2026-11-04; §79 B2). */
	static final String[] OUR_OPTIONS = {
		"aggressiveDataWatcherId", "arrowHarmingAfterTotemFix", "arrowsInheritMotion", "attackIndicator",
		"beaconBeamThroughTransparentBlocks", "buttonsOnFullFaces", "channeling",
		"channelingID", "chatHistoryAcrossRestarts", "chatHistoryBetweenWorlds", "chatLogBetweenWorlds",
		"chorusOnlyOnOuterIslands", "commandBlockCreativeOnly", "commandSuggestions", "connectionAcceptFix",
		"coordinateSuggestions", "depthStrider", "depthStriderID", "dropVehiclesTogether", "effectPanelAlphaFix",
		"enableBannerPatterns", "enableBanners", "enableBreezeRod", "enableFloatingItems", "enableGlowInkSac", "enableGlowItemFrames",
		"enableGlowSquidSpawns", "enableGlowSquids", "enableKelp", "enableLingeringPotions", "enableLodestone", "enableLuck",
		"enableModernBreathing", "enableModernSneaking", "enableModernSwimming", "enableOffhand",
		"enablePhantomSpawns", "enablePhantoms", "enablePolarBearSpawns", "enablePolarBears", "enablePowderSnow",
		"enableShield", "enableShulkerBoxes", "enableSpectatorMode", "enableSpyglass", "enableTippedArrows", "enableTrialEffects", "enableTrident", "enableTurtleShell", "endBarrensID",
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
		"OverlayMessage", "FreezingMessage", "StrayConversionMessage", "EffectTableMessage",
		"EndBossBarMessage", "DragonBreathBurstMessage", "LevelEventMessage", "ParticleMessage"};

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
		assertEqual("missing from ours [], on EFR's []", "missing from ours " + missing + ", on EFR's " + onEfr, "our 17 messages' channel");
	}

	/**
	 * B5: our sound events are McFly's own list (55, as of 2026-11-04; 67 with our blocks' 12 since B9; 68 with the lingering potion's throw, §82), handed to MCLib's asset director, and each
	 * resolves to a sound on the client (fetched from the launcher's assets).
	 */
	@SuppressWarnings("unchecked")
	public void testOurSoundsResolve() throws Exception {
		Class<?> sounds = mcfly("io.github.asgerjon.mcfly.sound.McFlySounds");
		Map<String, String> events = (Map<String, String>) sounds.getField("EVENTS").get(null);
		List<String> silent = onClient(() -> {
			List<String> missing = new ArrayList<>();
			for (String event : events.keySet()) {
				if (mc().getSoundHandler().getSound(new net.minecraft.util.ResourceLocation(McFly.MC_ASSETS, event)) == null) {
					missing.add(event);
				}
			}
			return missing;
		});
		// copper's grate and bulb, 3 each, and its 9 events (spec §87); tuff's three and calcite's, 3 each (§89, step E); cave vines' 3, the vine's step (glow lichen's), amethyst's 13 (§90.4), mud's 9 and the bottle's splash (§90 C), moss and azalea's 21 (§90.5), mangrove's and cherry's 12 (§90.6), dripstone's 6 and its 5 (§90.7), the lush plants' 9 and the tilt's 2 (§90.8), berries
		// picked, shears on a growing plant (§90.3), the sculk sensor's 3 and its clicking's 2, amethyst's resonance (§90.9, H2), sculk's 3, the catalyst's 3 and its bloom, the vein's 3, the spread and the charge (H3), the shrieker's 3, its shriek and the warden's 4 replies (H4); cherry's and bamboo's wood, 3 each (§91.3), nether wood's 3 (§91.4), iron's 3 (§91.6), the stone set's and bamboo's plate's clicks 6 (§91.7); the signs' waxed refusal (91.8);
		// the four hanging sign types' 12 and their waxed refusal (91.9); the shelf's 3 and 6 (91.10); bamboo's 3 and the
		// shoot's 2 (it steps as the stalk, 91.11)
		assertEqual("316 events, none silent []", events.size() + " events, none silent " + silent, "McFly's sound events");
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
	 * a tab lists an egg for each (EFR's items tab, and McFly's own since B9).
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
			// and McFly's own tab, once there is one (§79.4)
			try {
				((net.minecraft.creativetab.CreativeTabs) Class.forName(REGISTRY + "McFlyTab").getField("TAB").get(null)).displayAllReleventItems(items);
			} catch (ClassNotFoundException ignored) {
			}
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

	/** Our particles by their 26.3 names, with the class each comes out as on the client (§79 B7). */
	static final String[][] OUR_PARTICLES = {
		{"minecraft:sweep_attack", "SweepAttackFX"}, {"minecraft:glow", "GlowFX"}, {"minecraft:glow_squid_ink", "GlowSquidInkFX"},
		{"minecraft:snowflake", "SnowflakeFX"}, {"minecraft:electric_spark", "ElectricSparkFX"}, {"minecraft:dragon_breath", "DragonBreathFX"},
		{"minecraft:totem_of_undying", "TotemFX"}
	};

	/** The ways EFR's particle helper spawned ours before B7; none of them is left there. */
	static final String[] OUR_PARTICLE_METHODS = {"spawnElectricSparks", "spawnElectricSparksAlongAxis", "spawnSweepAttack",
		"spawnGlowSquidGlow", "spawnGlowSquidInk", "spawnTotemParticles", "spawnSnowflake"};

	/** How many particles of each class (by simple name) the client shows within a block of a spot. */
	private static Map<String, Integer> particlesNear(double x, double y, double z) throws Exception {
		java.lang.reflect.Field layers = cpw.mods.fml.relauncher.ReflectionHelper.findField(net.minecraft.client.particle.EffectRenderer.class, "fxLayers", "field_78876_b");
		Map<String, Integer> found = new java.util.TreeMap<>();
		for (List<?> layer : (List<?>[]) layers.get(mc().effectRenderer)) {
			for (Object particle : layer) {
				net.minecraft.entity.Entity fx = (net.minecraft.entity.Entity) particle;
				if (Math.abs(fx.posX - x) < 1 && Math.abs(fx.posY - y) < 1 && Math.abs(fx.posZ - z) < 1) {
					found.merge(particle.getClass().getSimpleName(), 1, Integer::sum);
				}
			}
		}
		return found;
	}

	/**
	 * B7: each of our particles, sent from the server by its 26.3 name (26.3's level particles packet, on our channel:
	 * `McFlyParticles.send` as `ServerLevel.sendParticles`, count 0 being one particle at the spot), reaches the client
	 * as that particle; EFR's particle helper spawns none of ours any more.
	 */
	public void testParticlesSentByName() throws Exception {
		Class<?> particles = mcfly("io.github.asgerjon.mcfly.particle.McFlyParticles");
		Method send = particles.getMethod("send", net.minecraft.world.WorldServer.class, String.class, boolean.class, boolean.class,
			double.class, double.class, double.class, int.class, double.class, double.class, double.class, double.class);
		List<String> wrong = new ArrayList<>();
		int i = 0;
		for (String[] particle : OUR_PARTICLES) {
			// a spot of its own for each, 3 blocks in front, in the air, the next 2 blocks along
			int n = i++;
			double[] at = onServer(() -> new double[]{Math.floor(player().posX) + 0.5 + 2 * n - 6, Math.floor(player().boundingBox.minY) + 2.5, Math.floor(player().posZ) + 3.5});
			int sentTo = onServer(() -> (Integer) send.invoke(null, world(), particle[0], true, true, at[0], at[1], at[2], 0, 0.0, 0.0, 0.0, 0.0));
			// each tick until it's seen (a spark lives 2 or 3 ticks)
			Map<String, Integer> seen = new java.util.TreeMap<>();
			for (int tick = 0; tick < 20 && !seen.containsKey(particle[1]); tick++) {
				ticks(1);
				seen = onClient(() -> particlesNear(at[0], at[1], at[2]));
			}
			if (sentTo != 1 || !seen.containsKey(particle[1])) {
				wrong.add(particle[0] + " sent to " + sentTo + ", seen " + seen);
			}
		}
		for (Method method : ganymedes01.etfuturum.client.particle.CustomParticles.class.getDeclaredMethods()) {
			if (Arrays.asList(OUR_PARTICLE_METHODS).contains(method.getName())) {
				wrong.add("EFR's CustomParticles." + method.getName());
			}
		}
		assertEqual("[]", wrong.toString(), "our 7 particles by name");
	}

	/**
	 * B7: 26.3's level particles packet's spread, on the client as `ClientPacketListener.handleParticleEvent` makes it:
	 * a count above 0 gives that many particles, each around the spot by a Gaussian times the spread.
	 */
	public void testParticleCountAndSpread() throws Exception {
		Class<?> particles = mcfly("io.github.asgerjon.mcfly.particle.McFlyParticles");
		Method send = particles.getMethod("send", net.minecraft.world.WorldServer.class, String.class, boolean.class, boolean.class,
			double.class, double.class, double.class, int.class, double.class, double.class, double.class, double.class);
		double[] at = onServer(() -> new double[]{Math.floor(player().posX) + 0.5, Math.floor(player().boundingBox.minY) + 2.5, Math.floor(player().posZ) - 3.5});
		onServer(() -> send.invoke(null, world(), "minecraft:electric_spark", true, true, at[0], at[1], at[2], 12, 0.1, 0.1, 0.1, 0.0));
		// each tick until they're seen (a spark lives 2 or 3 ticks), the most seen at once
		int most = 0;
		for (int tick = 0; tick < 20 && most < 12; tick++) {
			ticks(1);
			Integer now = onClient(() -> particlesNear(at[0], at[1], at[2])).get("ElectricSparkFX");
			most = Math.max(most, now == null ? 0 : now);
		}
		assertEqual(12, most, "sparks within a block of the spot, the most at once");
	}

	/** Our classes that used EFR's small helpers before B8 (§79 B8, surveyed 2026-11-04). */
	static final String[] OUR_HELPER_USERS = {
		"ganymedes01.etfuturum.client.ChatHistoryFile", "io.github.asgerjon.mcfly.lodestone.client.LodestoneNeedle",
		"ganymedes01.etfuturum.client.options.OptionsTxt", "ganymedes01.etfuturum.offhand.Offhand", "ganymedes01.etfuturum.skins.OfflineSkins",
		"ganymedes01.etfuturum.blocks.BlockKelp", "ganymedes01.etfuturum.blocks.BlockPowderSnowCauldron", "ganymedes01.etfuturum.blocks.BlockPowderSnow",
		"io.github.asgerjon.mcfly.banner.Banners", "ganymedes01.etfuturum.client.gui.inventory.GuiLoom",
		"ganymedes01.etfuturum.items.equipment.ItemTurtleShell", "io.github.asgerjon.mcfly.registry.McFlyEntities",
		"ganymedes01.etfuturum.client.renderer.block.BlockKelpRenderer"
	};

	/** EFR's small helpers (§79 B8), as class files name them. */
	static final String[] EFR_HELPERS = {
		"ganymedes01/etfuturum/core/utils/Logger", "ganymedes01/etfuturum/core/utils/Utils", "ganymedes01/etfuturum/lib/RenderIDs",
		"ganymedes01/etfuturum/client/renderer/block/BlockModelBase"
	};

	/**
	 * B8: none of our classes that used EFR's small helpers (its logger, `Utils`, `RenderIDs`, `BlockModelBase`) refers
	 * to them any more, inner classes included (read from the class files, as built); McFly has its own names for the
	 * domain our things live under and for MCLib's sound assets.
	 */
	public void testNoEfrHelpersInOurs() throws Exception {
		List<String> wrong = new ArrayList<>();
		for (String name : OUR_HELPER_USERS) {
			List<String> classes = new ArrayList<>();
			classes.add(name);
			for (int i = 0; i < classes.size(); i++) {
				byte[] bytes = net.minecraft.launchwrapper.Launch.classLoader.getClassBytes(classes.get(i));
				if (bytes == null) {
					wrong.add("no class file for " + classes.get(i));
					continue;
				}
				String text = new String(bytes, StandardCharsets.ISO_8859_1);
				for (String helper : EFR_HELPERS) {
					if (text.contains(helper)) {
						wrong.add(classes.get(i).substring(classes.get(i).lastIndexOf('.') + 1) + " uses " + helper.substring(helper.lastIndexOf('/') + 1));
					}
				}
				// its inner classes, as its InnerClasses table names them
				Matcher inner = Pattern.compile(Pattern.quote(name.replace('.', '/')) + "\\$[A-Za-z0-9_$]+").matcher(text);
				while (inner.find()) {
					String innerName = inner.group().replace('/', '.');
					if (!classes.contains(innerName)) {
						classes.add(innerName);
					}
				}
			}
		}
		Class<?> base = mcfly(BASE);
		String names = "";
		for (String field : new String[]{"DOMAIN", "MC_ASSETS"}) {
			try {
				names += " " + base.getField(field).get(null);
			} catch (NoSuchFieldException e) {
				names += " (no " + field + ")";
			}
		}
		assertEqual("[] etfuturum minecraft_1.21.10", wrong + names, "EFR's helpers in our classes, McFly's own names");
	}

	/** Our item, block and particle classes that stood on EFR's bases, sound types or tabs before B9 (§79.4). */
	static final String[] OUR_BASE_USERS = {
		"io.github.asgerjon.mcfly.copper.LightningRodBlock", "io.github.asgerjon.mcfly.lodestone.LodestoneBlock", "ganymedes01.etfuturum.blocks.BlockPowderSnow",
		"ganymedes01.etfuturum.blocks.BlockKelp", "ganymedes01.etfuturum.blocks.BlockDriedKelp", "ganymedes01.etfuturum.items.ItemDriedKelp",
		"ganymedes01.etfuturum.items.ItemNetheriteUpgradeTemplate", "ganymedes01.etfuturum.items.equipment.ItemShield",
		"ganymedes01.etfuturum.items.equipment.ItemTrident", "ganymedes01.etfuturum.items.ItemPowderSnowBucket", "ganymedes01.etfuturum.items.ItemSpyglass",
		"ganymedes01.etfuturum.items.ItemBannerPattern", "ganymedes01.etfuturum.items.equipment.ItemTurtleShell",
		"ganymedes01.etfuturum.items.ItemGlowItemFrame", "io.github.asgerjon.mcfly.registry.McFlyItems",
		"ganymedes01.etfuturum.client.particle.ElectricSparkFX", "ganymedes01.etfuturum.client.particle.GlowFX",
		"ganymedes01.etfuturum.client.particle.GlowSquidInkFX", "ganymedes01.etfuturum.client.particle.SweepAttackFX",
		"ganymedes01.etfuturum.client.particle.TotemFX"
	};

	/** EFR's bases, sound types and tabs (§79.4), as class files name them. */
	static final String[] EFR_BASES = {
		"ganymedes01/etfuturum/items/BaseItem", "ganymedes01/etfuturum/items/BaseFood", "ganymedes01/etfuturum/blocks/BaseBlock",
		"ganymedes01/etfuturum/client/particle/EtFuturumFXParticle", "ganymedes01/etfuturum/client/sound/ModSounds",
		"creativeTabItems", "creativeTabBlocks"
	};

	/** What a class file and its inner classes name, of the names given. */
	private static List<String> namedIn(String name, String[] names) {
		List<String> found = new ArrayList<>();
		List<String> classes = new ArrayList<>();
		classes.add(name);
		for (int i = 0; i < classes.size(); i++) {
			byte[] bytes;
			try {
				bytes = net.minecraft.launchwrapper.Launch.classLoader.getClassBytes(classes.get(i));
			} catch (IOException e) {
				bytes = null;
			}
			String shortName = classes.get(i).substring(classes.get(i).lastIndexOf('.') + 1);
			if (bytes == null) {
				found.add("no class file for " + shortName);
				continue;
			}
			String text = new String(bytes, StandardCharsets.ISO_8859_1);
			for (String helper : names) {
				if (text.contains(helper)) {
					found.add(shortName + " uses " + helper.substring(helper.lastIndexOf('/') + 1));
				}
			}
			Matcher inner = Pattern.compile(Pattern.quote(name.replace('.', '/')) + "\\$[A-Za-z0-9_$]+").matcher(text);
			while (inner.find()) {
				String innerName = inner.group().replace('/', '.');
				if (!classes.contains(innerName)) {
					classes.add(innerName);
				}
			}
		}
		return found;
	}

	/** B9: none of our item, block and particle classes stands on EFR's bases, sound types or creative tabs. */
	public void testNoEfrBasesInOurs() {
		List<String> wrong = new ArrayList<>();
		for (String name : OUR_BASE_USERS) {
			wrong.addAll(namedIn(name, EFR_BASES));
		}
		assertEqual("[]", wrong.toString(), "EFR's bases in our classes");
	}

	/**
	 * B9: our blocks' and items' creative tab is McFly's own (or a vanilla one we chose; none for the blocks 26.3 shows in
	 * no tab, spec §84), none of them in EFR's two;
	 * McFly's tab lists our spawn eggs, EFR's items tab none of them; the tab is called McFly (its key translated as
	 * 1.7.10's creative screen does: `getTranslatedTabLabel` is only the key).
	 */
	public void testOurThingsInOurTab() throws Exception {
		Class<?> tabs = mcfly(REGISTRY + "McFlyTab");
		Object ours = tabs.getField("TAB").get(null);
		List<Object> efrs = Arrays.asList(ganymedes01.etfuturum.EtFuturum.creativeTabItems, ganymedes01.etfuturum.EtFuturum.creativeTabBlocks);
		List<String> wrong = new ArrayList<>();
		for (io.github.asgerjon.mcfly.registry.McFlyBlocks block : io.github.asgerjon.mcfly.registry.McFlyBlocks.values()) {
			if (block.isEnabled() && block.getItem() != null) {
				Object tab = block.get().getCreativeTabToDisplayOn();
				// those 26.3 shows in no tab (its double slabs, the gateway) are in none; the rest in ours
				if (block.inTab() ? tab != ours : tab != null) {
					wrong.add(block.name() + " in " + (tab == null ? "no tab" : ((net.minecraft.creativetab.CreativeTabs) tab).getTabLabel()));
				}
			}
		}
		for (io.github.asgerjon.mcfly.registry.McFlyItems item : io.github.asgerjon.mcfly.registry.McFlyItems.values()) {
			if (item.isEnabled()) {
				Object tab = item.get().getCreativeTab();
				if (tab != ours && (tab == null || efrs.contains(tab))) {
					wrong.add(item.name() + " in " + (tab == null ? "no tab" : ((net.minecraft.creativetab.CreativeTabs) tab).getTabLabel()));
				}
			}
		}
		List<Integer> eggs = io.github.asgerjon.mcfly.registry.McFlyEntities.eggIds();
		String[] seen = onClient(() -> {
			List<Integer> inOurs = new ArrayList<>();
			List<Integer> inEfrs = new ArrayList<>();
			List<net.minecraft.item.ItemStack> listed = new ArrayList<>();
			((net.minecraft.creativetab.CreativeTabs) ours).displayAllReleventItems(listed);
			for (net.minecraft.item.ItemStack stack : listed) {
				if (stack.getItem() == net.minecraft.init.Items.spawn_egg) {
					inOurs.add(stack.getItemDamage());
				}
			}
			listed.clear();
			ganymedes01.etfuturum.EtFuturum.creativeTabItems.displayAllReleventItems(listed);
			for (net.minecraft.item.ItemStack stack : listed) {
				if (stack.getItem() == net.minecraft.init.Items.spawn_egg && eggs.contains(stack.getItemDamage())) {
					inEfrs.add(stack.getItemDamage());
				}
			}
			return new String[]{inOurs.toString(), inEfrs.toString(), net.minecraft.client.resources.I18n.format(((net.minecraft.creativetab.CreativeTabs) ours).getTranslatedTabLabel())};
		});
		assertEqual("[] eggs " + eggs + ", in EFR's [], called McFly", wrong + " eggs " + seen[0] + ", in EFR's " + seen[1] + ", called " + seen[2],
			"McFly's creative tab");
	}

	/** Our blocks with their 26.3 sound type's name (§79.4; 26.3's `SoundType`), and the dried kelp block's 1.7.10 grass. */
	static final String[][] OUR_BLOCK_SOUNDS = {
		{"LIGHTNING_ROD", "copper"}, {"WAXED_OXIDIZED_LIGHTNING_ROD", "copper"}, {"LODESTONE", "lodestone"}, {"POWDER_SNOW", "powder_snow"},
		{"KELP", "wet_grass"}
	};

	/**
	 * B9: each of our blocks sounds as 26.3's sound type for it: `block.<type>.break`, `.step` and `.place` from
	 * MCLib's assets, each resolving to a sound on the client.
	 */
	public void testOurBlockSounds() throws Exception {
		List<String> wrong = new ArrayList<>();
		List<String> events = new ArrayList<>();
		for (String[] entry : OUR_BLOCK_SOUNDS) {
			io.github.asgerjon.mcfly.registry.McFlyBlocks block = io.github.asgerjon.mcfly.registry.McFlyBlocks.valueOf(entry[0]);
			if (!block.isEnabled()) {
				continue;
			}
			net.minecraft.block.Block.SoundType sound = block.get().stepSound;
			String prefix = io.github.asgerjon.mcfly.McFly.MC_ASSETS + ":block." + entry[1] + ".";
			String got = sound.getBreakSound() + " " + sound.getStepResourcePath() + " " + sound.func_150496_b();
			String want = prefix + "break " + prefix + "step " + prefix + "place";
			if (!want.equals(got)) {
				wrong.add(entry[0] + ": " + got);
			}
			events.add("block." + entry[1] + ".break");
			events.add("block." + entry[1] + ".step");
			events.add("block." + entry[1] + ".place");
		}
		List<String> silent = onClient(() -> {
			List<String> missing = new ArrayList<>();
			for (String event : events) {
				if (mc().getSoundHandler().getSound(new net.minecraft.util.ResourceLocation(io.github.asgerjon.mcfly.McFly.MC_ASSETS, event)) == null) {
					missing.add(event);
				}
			}
			return missing;
		});
		assertEqual("[] none silent []", wrong + " none silent " + silent, "our blocks' sounds");
	}

	/**
	 * No entity takes id 0, as in 26.3 (its counter's first id is 1). 1.7.10's counter starts at 0, and code taking 0 for
	 * "no entity" (vanilla's own spawn packets for a thrower or an angler, a rocket's boosted entity) took the run's first
	 * entity, often a fresh world's player, for none: the showcase's 16 rockets pushed nothing (2026-11-05). With the
	 * counter put back to 0 for a moment, a new entity and one given a new id (Forge's `resetEntityId`) still aren't 0.
	 */
	public void testNoEntityIdZero() {
		String zero = onServer(() -> {
			java.lang.reflect.Field counter = cpw.mods.fml.relauncher.ReflectionHelper.findField(net.minecraft.entity.Entity.class, "nextEntityID", "field_70152_a");
			int was = counter.getInt(null);
			try {
				counter.setInt(null, 0);
				net.minecraft.entity.Entity fresh = new net.minecraft.entity.passive.EntityPig(world());
				net.minecraft.entity.Entity renumbered = new net.minecraft.entity.passive.EntityPig(world());
				counter.setInt(null, 0);
				renumbered.resetEntityId();
				return "new " + fresh.getEntityId() + ", renumbered " + renumbered.getEntityId();
			} finally {
				// past every id handed out meanwhile
				counter.setInt(null, Math.max(was, counter.getInt(null)) + 8);
			}
		});
		assertFalse(zero.contains(" 0,") || zero.endsWith(" 0"), "an entity given id 0: " + zero);
	}

	/**
	 * Every block of ours and EFR's names a texture: one that never does asks 1.7.10 for
	 * `MISSING_ICON_BLOCK_<id>_<name>`, which isn't there (handoff item 44: the bubble column, seen in the game log; nothing
	 * draws its icon, but anything asking for it, WAILA, NEI, a mod's particles, would show the missing texture).
	 */
	public void testEveryBlockNamesItsTexture() {
		List<String> unnamed = onClient(() -> {
			List<String> found = new ArrayList<>();
			for (Object key : net.minecraft.block.Block.blockRegistry.getKeys()) {
				String name = String.valueOf(key);
				if (!name.startsWith("etfuturum:") && !name.startsWith(McFly.DOMAIN + ":")) {
					continue;
				}
				net.minecraft.block.Block block = (net.minecraft.block.Block) net.minecraft.block.Block.blockRegistry.getObject(name);
				for (int side = 0; side < 6; side++) {
					net.minecraft.util.IIcon icon;
					try {
						icon = block.getIcon(side, 0);
					} catch (RuntimeException e) {
						continue;
					}
					if (icon != null && icon.getIconName().startsWith("MISSING_ICON_")) {
						found.add(name);
						break;
					}
				}
			}
			return found;
		});
		assertEqual("[]", unnamed.toString(), "blocks that never named a texture (1.7.10 asks for MISSING_ICON_BLOCK_<id>_<name>)");
	}

	/**
	 * Our blocks that stop light without filling their space (slabs, stairs) take their neighbours' light, as 1.7.10's
	 * own do: `Block.registerBlocks` sets that for vanilla's blocks only, so a mod's are left reading their own space's
	 * light, 0, and a face inside the block draws dark (seen 2026-11-05 in `endblocks.purpur`: the bottom slab's top
	 * under a top slab, lit in 26.3). EFR's slab and stairs set it by hand; ours had lost it.
	 */
	public void testPartBlocksTakeNeighbourLight() {
		List<String> dark = new ArrayList<>();
		for (io.github.asgerjon.mcfly.registry.McFlyBlocks entry : io.github.asgerjon.mcfly.registry.McFlyBlocks.VALUES) {
			net.minecraft.block.Block block = entry.isEnabled() ? entry.get() : null;
			// all light stopped, so its own space reads 0 (powder snow's 1 and kelp's water 3 only dim it, as 26.3's)
			if (block != null && block.getLightOpacity() >= 15 && !block.isOpaqueCube() && !block.getUseNeighborBrightness()) {
				dark.add(entry.name());
			}
		}
		assertEqual("[]", dark.toString(), "our blocks that stop light, aren't full and read their own space's light");
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
