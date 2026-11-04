package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.potion.EffectRegistry;
import ganymedes01.etfuturum.potion.PotionContents;
import ganymedes01.etfuturum.unittas.TasSave;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.RegionFile;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Worlds made by vanilla 1.7.10, opened with the release jar (SPEC-modern-movement.md §75): run in unitTAS's save mode
 * (`./unittas_mcfly.sh oldsaves`), once on each save as vanilla left it and once after EFR saved it, against the pristine copy
 * read with 1.7.10's own NBT reader. The tests go in name order: the player and the buildings are checked before the
 * pictures move the player (who is put back where the save had them).
 */
public class OldSaveTests extends TasTestCase {

	/** 1.7.10's effect numbers, as vanilla saves them, and 26.3's names for them. */
	private static final String[] VANILLA_EFFECTS = {null, "speed", "slowness", "haste", "mining_fatigue", "strength",
		"instant_health", "instant_damage", "jump_boost", "nausea", "regeneration", "resistance", "fire_resistance",
		"water_breathing", "invisibility", "blindness", "night_vision", "hunger", "weakness", "poison", "wither",
		"health_boost", "absorption", "saturation"};

	/**
	 * Saved values that must come back as they were, by entity or tile entity id (vanilla's names); "*" for every
	 * living entity. The things EFR converts (items, effects, the beacon's effects, frames' turns) are compared by
	 * meaning instead.
	 */
	private static final Map<String, List<String>> KEPT = new HashMap<>();

	static {
		kept("*", "CustomName", "CustomNameVisible", "PersistenceRequired", "Leashed");
		kept("Villager", "Profession");
		kept("EntityHorse", "Type", "Variant", "Tame", "ChestedHorse");
		kept("Wolf", "Sitting", "CollarColor", "OwnerUUID", "Angry");
		kept("Ozelot", "CatType", "Sitting", "OwnerUUID");
		kept("Sheep", "Color", "Sheared");
		kept("Pig", "Saddle");
		kept("Creeper", "powered");
		kept("Enderman", "carried", "carriedData");
		kept("Slime", "Size");
		kept("LavaSlime", "Size");
		kept("Skeleton", "SkeletonType");
		kept("Zombie", "IsBaby", "IsVillager");
		kept("VillagerGolem", "PlayerCreated");
		kept("Painting", "Motive", "Direction", "TileX", "TileY", "TileZ");
		kept("ItemFrame", "TileX", "TileY", "TileZ");
		kept("MinecartCommandBlock", "Command");
		kept("MinecartSpawner", "EntityId");
		kept("MinecartTNT", "TNTFuse");
		kept("Sign", "Text1", "Text2", "Text3", "Text4");
		kept("Control", "Command");
		kept("MobSpawner", "EntityId");
		kept("Music", "note");
		kept("RecordPlayer", "Record");
		kept("FlowerPot", "Item", "Data");
		kept("Skull", "SkullType", "Rot");
		kept("Beacon", "Levels");
	}

	private static void kept(String id, String... keys) {
		KEPT.put(id, Arrays.asList(keys));
	}

	/** Blocks whose meta is live state (the daylight sensor's follows the sun): their id is checked, not their meta. */
	private static final List<Integer> LIVE_META = Arrays.asList(151);

	/**
	 * Errors other mods log for any world they didn't see made, not EFR's: GTNHLib's teams have no entry for a player
	 * who joined before it (it logs this for every new player too).
	 */
	private static final List<String> OTHER_MODS_ERRORS = Arrays.asList("[gtnhlib] Unable to find team for player");

	private static List<String> withoutOtherMods(List<String> errors) {
		List<String> ours = new ArrayList<>();
		for (String line : errors) {
			boolean other = false;
			for (String start : OTHER_MODS_ERRORS) {
				other |= line.startsWith(start);
			}
			if (!other) {
				ours.add(line);
			}
		}
		return ours;
	}

	/** Tile entities whose contents change by themselves (smelting, brewing): their kind is checked, not their items. */
	private static final List<String> TICKING_CONTENTS = Arrays.asList("Furnace", "Cauldron");

	@Override
	protected int timeoutSeconds() {
		return 300;
	}

	// ---------------------------------------------------------------- the tests

	/** Nothing logged as an error while the save loaded (FML's caught exceptions among them). */
	public void test1LoadedWithoutErrors() {
		TasSave save = save();
		note(save, "load warnings", save.loadWarnings);
		assertTrue(save.listenerProblem() == null, save.listenerProblem());
		assertEqual("[]", withoutOtherMods(save.loadErrors).toString(), "errors logged while " + save.folder + " loaded");
	}

	/** The player as the save had them: dimension, position, game mode, XP, inventory and armour, effects. */
	public void test2PlayerAsSaved() {
		TasSave save = save();
		NBTTagCompound saved = pristineLevel(save).getCompoundTag("Data").getCompoundTag("Player");
		assertTrue(!saved.hasNoTags(), "the pristine level.dat has no player");
		List<String> wrong = onServer(() -> {
			List<String> found = new ArrayList<>();
			EntityPlayerMP p = player();
			differ(found, "dimension", saved.getInteger("Dimension"), p.dimension);
			NBTTagList pos = saved.getTagList("Pos", 6);
			double moved = Math.sqrt(sq(p.posX - pos.func_150309_d(0)) + sq(p.posY - pos.func_150309_d(1)) + sq(p.posZ - pos.func_150309_d(2)));
			if (moved > 0.1) {
				found.add(String.format(Locale.ROOT, "position: saved %.2f %.2f %.2f, now %.2f %.2f %.2f", pos.func_150309_d(0),
					pos.func_150309_d(1), pos.func_150309_d(2), p.posX, p.posY, p.posZ));
			}
			differ(found, "game mode", saved.getInteger("playerGameType"), p.theItemInWorldManager.getGameType().getID());
			differ(found, "XP level", saved.getInteger("XpLevel"), p.experienceLevel);
			NBTTagList inventory = saved.getTagList("Inventory", 10);
			boolean[] filled = new boolean[p.inventory.mainInventory.length];
			for (int i = 0; i < inventory.tagCount(); i++) {
				NBTTagCompound item = inventory.getCompoundTagAt(i);
				int slot = item.getByte("Slot") & 255;
				ItemStack now = slot >= 100 ? p.inventory.armorInventory[slot - 100] : p.inventory.mainInventory[slot];
				if (slot < filled.length) {
					filled[slot] = true;
				}
				compareStacks(found, "slot " + slot, item, now);
			}
			for (int slot = 0; slot < filled.length; slot++) {
				if (!filled[slot] && p.inventory.mainInventory[slot] != null) {
					found.add("slot " + slot + ": empty in the save, now " + p.inventory.mainInventory[slot]);
				}
			}
			compareEffects(found, "the player's effects", saved.getTagList("ActiveEffects", 10), p.getActivePotionEffects());
			return found;
		});
		assertEqual("[]", wrong.toString(), "the player in " + save.folder);
	}

	/**
	 * Each building, its dimension's chunks loaded: every block (id and meta) as vanilla saved it, a tile entity of the
	 * same kind wherever vanilla had one, every entity of its chunks there once by UUID (never twice; those inside the
	 * building exactly once, except dropped items, which may merge), and nothing logged as an error meanwhile.
	 */
	public void test3BuildingsAsSaved() {
		TasSave save = save();
		int errorsBefore = save.log.errors.size();
		List<String> wrong = new ArrayList<>();
		for (Building building : buildings(save)) {
			Map<Long, NBTTagCompound> chunks = pristineChunks(save, building);
			wrong.addAll(onServer(() -> compareBuilding(building, chunks)));
		}
		List<String> logged = withoutOtherMods(new ArrayList<>(save.log.errors.subList(errorsBefore, save.log.errors.size())));
		for (String line : logged) {
			wrong.add("logged: " + line);
		}
		assertEqual("[]", limit(wrong), "the buildings of " + save.folder);
	}

	/**
	 * What the buildings hold, by meaning: containers' items (potions by their effects, enchantments), item frames' and
	 * dropped items, mobs' equipment and effects, the beacon's effects, and each kind's own saved values (KEPT).
	 */
	public void test4ContentsAsSaved() {
		TasSave save = save();
		List<String> wrong = new ArrayList<>();
		for (Building building : buildings(save)) {
			Map<Long, NBTTagCompound> chunks = pristineChunks(save, building);
			wrong.addAll(onServer(() -> compareContents(building, chunks)));
		}
		assertEqual("[]", limit(wrong), "what the buildings of " + save.folder + " hold");
	}

	/**
	 * The worlds drawn (the user, 2026-11-03: "include screenshots in the tests to validate that worlds actually render
	 * correctly and as expected"): the view on load, then each building from its viewpoint (the player put back after).
	 * Each picture: no missing texture showing (1.7.10's magenta and black), detail in the scene (not nothing drawn:
	 * sky alone, the Nether's fog, a loading screen; a check by colours let a fog-red picture through, 2026-11-04), and
	 * on the second load the same as on the first (EFR's save changes nothing seen).
	 * The pictures stay in the results for the user's look; their numbers go to oldsaves-notes.txt.
	 */
	public void test5Pictures() {
		TasSave save = save();
		String prefix = "oldsaves." + save.folder + ".load";
		List<String> wrong = new ArrayList<>();
		List<String> numbers = new ArrayList<>();
		// no particles while the pictures are taken: the player's own effect swirls drift past the camera (big, near)
		int particles = onClient(() -> {
			int was = mc().gameSettings.particleSetting;
			mc().gameSettings.particleSetting = 2;
			mc().effectRenderer.clearEffects(mc().theWorld);
			return was;
		});
		ticks(40);
		judge(save, prefix, "on_load", wrong, numbers);
		double[] home = onServer(() -> new double[] {player().dimension, player().posX, player().posY, player().posZ,
			player().rotationYaw, player().rotationPitch});
		// flying while the pictures are taken (a survival player would fall from a viewpoint in the air); put back after
		boolean[] abilities = onServer(() -> new boolean[] {player().capabilities.allowFlying, player().capabilities.isFlying});
		try {
			for (Building building : buildings(save)) {
				if (!building.hasView) {
					continue;
				}
				moveTo(building.dim, building.view[0], building.view[1], building.view[2], building.yaw, building.pitch);
				// the chunks around drawn (a dimension change starts the client's world afresh)
				ticks(100);
				judge(save, prefix, building.name, wrong, numbers);
			}
		} finally {
			moveTo((int) home[0], home[1], home[2], home[3], (float) home[4], (float) home[5]);
			onServer(() -> {
				player().capabilities.allowFlying = abilities[0];
				player().capabilities.isFlying = abilities[1];
				player().sendPlayerAbilities();
			});
			onClient(() -> {
				mc().gameSettings.particleSetting = particles;
			});
			ticks(20);
		}
		note(save, "pictures", numbers);
		assertEqual("[]", wrong.toString(), "the pictures of " + save.folder);
	}

	/** Most pixels of the missing texture's magenta a picture may show (the sweep's magenta wool isn't that colour). */
	private static final int MAGENTA_ALLOWED = 64;
	/** At least this share of neighbouring pixels differing in the scene (see judge). */
	private static final double DETAIL_NEEDED = 0.02;
	/** At most this mean difference (0 to 255 a channel) between a view's first and second load. */
	private static final double CHANGE_ALLOWED = 40.0;

	private void judge(TasSave save, String prefix, String view, List<String> wrong, List<String> numbers) {
		String path = screenshot(prefix + save.pass + "." + view);
		java.awt.image.BufferedImage picture = picture(path);
		if (picture == null) {
			wrong.add(view + ": no picture at " + path);
			return;
		}
		int magenta = 0;
		int w = picture.getWidth(), h = picture.getHeight();
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int rgb = picture.getRGB(x, y);
				int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
				// 1.7.10's missing texture is #F800F8 and black; shaded, its magenta keeps g at 0 and r equal to b
				if (g < 30 && r > 120 && b > 120 && Math.abs(r - b) < 40) {
					magenta++;
				}
			}
		}
		// the scene's detail: of neighbouring pixels above the hotbar and left of the hand, the share that differ (a
		// world drawn: 5 to 18 %; sky and clouds alone, or the Nether's fog with no chunk drawn: under 1 %)
		int pairs = 0, differ = 0;
		for (int y = 0; y < h * 3 / 4; y += 2) {
			for (int x = 0; x < w * 78 / 100 - 1; x += 2) {
				int a = picture.getRGB(x, y), c = picture.getRGB(x + 1, y);
				pairs++;
				if (Math.abs((a >> 16 & 255) - (c >> 16 & 255)) + Math.abs((a >> 8 & 255) - (c >> 8 & 255)) + Math.abs((a & 255) - (c & 255)) > 24) {
					differ++;
				}
			}
		}
		double detail = differ / (double) Math.max(1, pairs);
		String line = String.format(Locale.ROOT, "%s: %d missing-texture pixels, %.1f%% detail", path, magenta, detail * 100);
		if (magenta > MAGENTA_ALLOWED) {
			wrong.add(view + ": " + magenta + " pixels of the missing texture show (" + path + ")");
		}
		if (detail < DETAIL_NEEDED) {
			wrong.add(String.format(Locale.ROOT, "%s: %.1f%% detail, nothing drawn? (%s)", view, detail * 100, path));
		}
		if (save.pass == 2) {
			String first = "screenshots/" + (prefix + "1." + view).replaceAll("[^A-Za-z0-9_.-]", "_") + ".png";
			java.awt.image.BufferedImage before = picture(first);
			if (before != null && before.getWidth() == w && before.getHeight() == h) {
				long sum = 0;
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						int a = picture.getRGB(x, y), c = before.getRGB(x, y);
						sum += Math.abs((a >> 16 & 255) - (c >> 16 & 255)) + Math.abs((a >> 8 & 255) - (c >> 8 & 255)) + Math.abs((a & 255) - (c & 255));
					}
				}
				double change = sum / (3.0 * w * h);
				line += String.format(Locale.ROOT, ", %.1f mean change from the first load", change);
				if (change > CHANGE_ALLOWED) {
					wrong.add(String.format(Locale.ROOT, "%s: looks different after EFR's save (mean change %.1f; %s, %s)", view, change, first, path));
				}
			}
		}
		numbers.add(line);
	}

	private java.awt.image.BufferedImage picture(String path) {
		File file = new File(resultsFolder(), path);
		try {
			return file.isFile() ? javax.imageio.ImageIO.read(file) : null;
		} catch (IOException e) {
			return null;
		}
	}

	// ---------------------------------------------------------------- the comparisons (on the server thread)

	private static List<String> compareBuilding(Building building, Map<Long, NBTTagCompound> chunks) {
		List<String> found = new ArrayList<>();
		WorldServer world = loadChunks(building, chunks);
		String where = building.name + " (" + building.dim + ")";
		for (Map.Entry<Long, NBTTagCompound> chunk : chunks.entrySet()) {
			if (chunk.getValue() == null) {
				found.add(where + ": chunk " + chunkName(chunk.getKey()) + " isn't in the pristine save");
			}
		}
		// blocks
		int blockDifferences = 0;
		for (int x = building.x1; x <= building.x2; x++) {
			for (int z = building.z1; z <= building.z2; z++) {
				NBTTagCompound level = chunks.get(chunkKey(x >> 4, z >> 4));
				if (level == null) {
					continue;
				}
				for (int y = building.y1; y <= building.y2; y++) {
					int[] was = pristineBlock(level, x, y, z);
					int id = Block.getIdFromBlock(world.getBlock(x, y, z));
					int meta = world.getBlockMetadata(x, y, z);
					if (was[0] != id || (was[0] != 0 && was[1] != meta && !LIVE_META.contains(id))) {
						if (blockDifferences++ < 60) {
							found.add(where + " " + x + " " + y + " " + z + ": vanilla " + blockName(was[0]) + ":" + was[1]
								+ ", now " + blockName(id) + ":" + meta);
						}
					}
				}
			}
		}
		if (blockDifferences > 60) {
			found.add(where + ": " + (blockDifferences - 60) + " more blocks differ");
		}
		// tile entities
		for (NBTTagCompound level : chunks.values()) {
			if (level == null) {
				continue;
			}
			NBTTagList tiles = level.getTagList("TileEntities", 10);
			for (int i = 0; i < tiles.tagCount(); i++) {
				NBTTagCompound tile = tiles.getCompoundTagAt(i);
				int x = tile.getInteger("x"), y = tile.getInteger("y"), z = tile.getInteger("z");
				if (!building.contains(x, y, z)) {
					continue;
				}
				TileEntity now = world.getTileEntity(x, y, z);
				String nowId = now == null ? "nothing" : savedId(now);
				if (!tile.getString("id").equals(nowId)) {
					found.add(where + " " + x + " " + y + " " + z + ": tile entity " + tile.getString("id") + ", now " + nowId);
				}
			}
		}
		// entities: by UUID, riders' mounts too
		Map<UUID, Integer> live = new HashMap<>();
		for (Object loaded : world.loadedEntityList) {
			live.merge(((Entity) loaded).getUniqueID(), 1, Integer::sum);
		}
		for (NBTTagCompound level : chunks.values()) {
			if (level == null) {
				continue;
			}
			NBTTagList entities = level.getTagList("Entities", 10);
			for (int i = 0; i < entities.tagCount(); i++) {
				for (NBTTagCompound entity = entities.getCompoundTagAt(i); entity != null;
					 entity = entity.hasKey("Riding", 10) ? entity.getCompoundTag("Riding") : null) {
					UUID id = new UUID(entity.getLong("UUIDMost"), entity.getLong("UUIDLeast"));
					int count = live.getOrDefault(id, 0);
					String kind = entity.getString("id");
					boolean ours = building.contains(entity.getTagList("Pos", 6));
					boolean mayGo = !ours || kind.equals("Item") || kind.equals("XPOrb");
					if (count > 1 || (count == 0 && !mayGo)) {
						found.add(where + ": " + kind + " " + id + " (saved at " + posText(entity) + ") is here " + count + " times"
							+ (count == 0 ? "; " + nearest(world, kind, entity.getTagList("Pos", 6)) : ""));
					}
				}
			}
		}
		return found;
	}

	@SuppressWarnings("unchecked")
	private static List<String> compareContents(Building building, Map<Long, NBTTagCompound> chunks) {
		List<String> found = new ArrayList<>();
		WorldServer world = loadChunks(building, chunks);
		String where = building.name + " (" + building.dim + ")";
		Map<UUID, Entity> live = new HashMap<>();
		for (Object loaded : world.loadedEntityList) {
			live.put(((Entity) loaded).getUniqueID(), (Entity) loaded);
		}
		for (NBTTagCompound level : chunks.values()) {
			if (level == null) {
				continue;
			}
			NBTTagList tiles = level.getTagList("TileEntities", 10);
			for (int i = 0; i < tiles.tagCount(); i++) {
				NBTTagCompound tile = tiles.getCompoundTagAt(i);
				int x = tile.getInteger("x"), y = tile.getInteger("y"), z = tile.getInteger("z");
				TileEntity now = building.contains(x, y, z) ? world.getTileEntity(x, y, z) : null;
				if (now == null) {
					continue;
				}
				String id = tile.getString("id");
				String at = where + " " + id + " at " + x + " " + y + " " + z;
				NBTTagCompound nowSaved = new NBTTagCompound();
				now.writeToNBT(nowSaved);
				compareKept(found, at, id, tile, nowSaved);
				if (tile.hasKey("Items", 9) && now instanceof IInventory inventory && !TICKING_CONTENTS.contains(id)) {
					compareInventory(found, at, tile.getTagList("Items", 10), inventory);
				}
				if (now instanceof TileEntityBeacon beacon) {
					differ(found, at + " primary effect", vanillaEffect(tile.getInteger("Primary")), effectName(beacon.getPrimaryEffect()));
					differ(found, at + " secondary effect", vanillaEffect(tile.getInteger("Secondary")), effectName(beacon.getSecondaryEffect()));
				}
			}
			NBTTagList entities = level.getTagList("Entities", 10);
			for (int i = 0; i < entities.tagCount(); i++) {
				for (NBTTagCompound entity = entities.getCompoundTagAt(i); entity != null;
					 entity = entity.hasKey("Riding", 10) ? entity.getCompoundTag("Riding") : null) {
					if (!building.contains(entity.getTagList("Pos", 6))) {
						continue;
					}
					Entity now = live.get(new UUID(entity.getLong("UUIDMost"), entity.getLong("UUIDLeast")));
					if (now == null) {
						continue;
					}
					String id = entity.getString("id");
					String at = where + " " + id + " " + (entity.hasKey("CustomName") ? "'" + entity.getString("CustomName") + "' " : "")
						+ "saved at " + posText(entity);
					NBTTagCompound nowSaved = new NBTTagCompound();
					now.writeToNBT(nowSaved);
					compareKept(found, at, id, entity, nowSaved);
					if (now instanceof EntityLivingBase living) {
						compareKept(found, at, "*", entity, nowSaved);
						compareEffects(found, at + " effects", entity.getTagList("ActiveEffects", 10), living.getActivePotionEffects());
						NBTTagList equipment = entity.getTagList("Equipment", 10);
						for (int slot = 0; slot < equipment.tagCount() && slot < 5; slot++) {
							compareStacks(found, at + " equipment " + slot, equipment.getCompoundTagAt(slot), living.getEquipmentInSlot(slot));
						}
					}
					if (now instanceof EntityItemFrame frame) {
						compareStacks(found, at + " item", entity.getCompoundTag("Item"), frame.getDisplayedItem());
						int was = entity.getByte("ItemRotation");
						int turn = frame.getRotation();
						// 26.3's frames turn in eighths (spec §67): a 1.7.10 quarter turn is two of them
						if (turn != was && turn != was * 2) {
							found.add(at + " turn: saved " + was + " of 4, now " + turn);
						}
					}
					if (id.equals("Item")) {
						compareStacks(found, at + " item", entity.getCompoundTag("Item"), ((net.minecraft.entity.item.EntityItem) now).getEntityItem());
					}
					if (entity.hasKey("Items", 9) && now instanceof IInventory inventory) {
						compareInventory(found, at, entity.getTagList("Items", 10), inventory);
					}
				}
			}
		}
		return found;
	}

	/** For a missing entity: its chunk's state, and the nearest live one of its kind (it may have a new UUID). */
	private static String nearest(WorldServer world, String kind, NBTTagList pos) {
		double x = pos.func_150309_d(0), y = pos.func_150309_d(1), z = pos.func_150309_d(2);
		int cx = (int) Math.floor(x) >> 4, cz = (int) Math.floor(z) >> 4;
		String chunk = "chunk " + cx + " " + cz + (world.theChunkProviderServer.chunkExists(cx, cz) ? " loaded" : " not loaded");
		Entity best = null;
		double bestDistance = Double.MAX_VALUE;
		int ofKind = 0;
		for (Object loaded : world.loadedEntityList) {
			Entity e = (Entity) loaded;
			if (!kind.equals(net.minecraft.entity.EntityList.getEntityString(e))) {
				continue;
			}
			ofKind++;
			double d = e.getDistanceSq(x, y, z);
			if (d < bestDistance) {
				bestDistance = d;
				best = e;
			}
		}
		return chunk + ", " + ofKind + " of its kind loaded" + (best == null ? "" : String.format(Locale.ROOT,
			", the nearest %.1f blocks off (%s, %s)", Math.sqrt(bestDistance), best.getUniqueID(), best.isDead ? "dead" : "alive"));
	}

	/** The values KEPT names for this kind, as saved then and now (a key missing now counts). */
	private static void compareKept(List<String> found, String at, String id, NBTTagCompound was, NBTTagCompound now) {
		for (String key : KEPT.getOrDefault(id, java.util.Collections.emptyList())) {
			if (!was.hasKey(key)) {
				continue;
			}
			NBTBase then = was.getTag(key);
			NBTBase nowTag = now.getTag(key);
			if (!then.equals(nowTag)) {
				found.add(at + " " + key + ": saved " + then + ", now " + (nowTag == null ? "missing" : nowTag.toString()));
			}
		}
	}

	private static void compareInventory(List<String> found, String at, NBTTagList items, IInventory inventory) {
		boolean[] filled = new boolean[inventory.getSizeInventory()];
		for (int i = 0; i < items.tagCount(); i++) {
			NBTTagCompound item = items.getCompoundTagAt(i);
			int slot = item.getByte("Slot") & 255;
			if (slot >= filled.length) {
				found.add(at + " slot " + slot + ": past the inventory's " + filled.length + " slots");
				continue;
			}
			filled[slot] = true;
			compareStacks(found, at + " slot " + slot, item, inventory.getStackInSlot(slot));
		}
		for (int slot = 0; slot < filled.length; slot++) {
			if (!filled[slot] && inventory.getStackInSlot(slot) != null) {
				found.add(at + " slot " + slot + ": empty in the save, now " + inventory.getStackInSlot(slot));
			}
		}
	}

	/**
	 * A saved item and what the game has now: the same item and count; a potion's effects (by name and level) as
	 * vanilla's own brewing data gives for its damage value (or its custom effects), and splash or not; otherwise the
	 * same damage; enchantments and the name kept.
	 */
	private static void compareStacks(List<String> found, String at, NBTTagCompound was, ItemStack now) {
		if (was.hasNoTags()) {
			if (now != null) {
				found.add(at + ": nothing saved, now " + now);
			}
			return;
		}
		int id = was.getShort("id");
		Item item = Item.getItemById(id);
		String name = item == null ? "#" + id : Item.itemRegistry.getNameForObject(item);
		if (now == null) {
			found.add(at + ": " + name + " gone");
			return;
		}
		if (Item.getIdFromItem(now.getItem()) != id) {
			found.add(at + ": " + name + ", now " + Item.itemRegistry.getNameForObject(now.getItem()));
			return;
		}
		if (was.getByte("Count") != now.stackSize) {
			found.add(at + ": " + was.getByte("Count") + " " + name + ", now " + now.stackSize);
		}
		NBTTagCompound tag = was.getCompoundTag("tag");
		int damage = was.getShort("Damage");
		if (item instanceof ItemPotion) {
			List<String> then = new ArrayList<>();
			if (tag.hasKey("CustomPotionEffects", 9)) {
				NBTTagList custom = tag.getTagList("CustomPotionEffects", 10);
				for (int i = 0; i < custom.tagCount(); i++) {
					then.add(vanillaEffect(custom.getCompoundTagAt(i).getByte("Id")) + "/" + custom.getCompoundTagAt(i).getByte("Amplifier"));
				}
			} else {
				List<PotionEffect> base = PotionHelper.getPotionEffects(damage, false);
				if (base != null) {
					for (PotionEffect effect : base) {
						then.add(vanillaEffect(effect.getPotionID()) + "/" + effect.getAmplifier());
					}
				}
			}
			// the item's effects, as drinking takes them: a named potion's (26.3's), or an unnamed one's own (1.7.10's)
			List<String> nowEffects = new ArrayList<>();
			List<?> drunk = net.minecraft.init.Items.potionitem.getEffects(now);
			for (Object o : drunk == null ? java.util.Collections.emptyList() : drunk) {
				PotionEffect effect = (PotionEffect) o;
				nowEffects.add(effectName(effect.getPotionID()) + "/" + effect.getAmplifier());
			}
			if (!new TreeSet<>(then).equals(new TreeSet<>(nowEffects))) {
				found.add(at + ": potion " + damage + " gave " + new TreeSet<>(then) + ", now " + new TreeSet<>(nowEffects));
			}
			if (ItemPotion.isSplash(damage) != PotionContents.isSplash(now)) {
				found.add(at + ": potion " + damage + (ItemPotion.isSplash(damage) ? " was" : " wasn't") + " splash");
			}
		} else if (damage != now.getItemDamage()) {
			found.add(at + ": " + name + " damage " + damage + ", now " + now.getItemDamage());
		}
		for (String list : new String[] {"ench", "StoredEnchantments"}) {
			String then = enchantments(tag.getTagList(list, 10));
			String nowList = enchantments(now.hasTagCompound() ? now.getTagCompound().getTagList(list, 10) : new NBTTagList());
			if (!then.equals(nowList)) {
				found.add(at + ": " + name + " " + list + " " + then + ", now " + nowList);
			}
		}
		if (tag.getCompoundTag("display").hasKey("Name") && !tag.getCompoundTag("display").getString("Name").equals(now.getDisplayName())) {
			found.add(at + ": named '" + tag.getCompoundTag("display").getString("Name") + "', now '" + now.getDisplayName() + "'");
		}
	}

	private static String enchantments(NBTTagList list) {
		TreeSet<String> each = new TreeSet<>();
		for (int i = 0; i < list.tagCount(); i++) {
			each.add(list.getCompoundTagAt(i).getShort("id") + "/" + list.getCompoundTagAt(i).getShort("lvl"));
		}
		return each.toString();
	}

	/** Effects saved by 1.7.10's numbers and the ones live now: the same names and levels. */
	private static void compareEffects(List<String> found, String at, NBTTagList was, Collection<?> now) {
		TreeSet<String> then = new TreeSet<>();
		for (int i = 0; i < was.tagCount(); i++) {
			NBTTagCompound effect = was.getCompoundTagAt(i);
			// vanilla's number, or (a save EFR wrote) its name (spec §70)
			String name = effect.hasKey("id", 8) ? effect.getString("id") : vanillaEffect(effect.getByte("Id") & 255);
			then.add(name + "/" + effect.getByte("Amplifier"));
		}
		TreeSet<String> nowSet = new TreeSet<>();
		for (Object effect : now) {
			PotionEffect live = (PotionEffect) effect;
			nowSet.add(effectName(live.getPotionID()) + "/" + live.getAmplifier());
		}
		if (!then.equals(nowSet)) {
			found.add(at + ": saved " + then + ", now " + nowSet);
		}
	}

	private static String vanillaEffect(int number) {
		return number > 0 && number < VANILLA_EFFECTS.length ? "minecraft:" + VANILLA_EFFECTS[number] : number == 0 ? "none" : "#" + number;
	}

	private static String effectName(int number) {
		if (number == 0) {
			return "none";
		}
		Potion potion = number > 0 && number < Potion.potionTypes.length ? Potion.potionTypes[number] : null;
		String key = EffectRegistry.key(potion);
		return key == null ? "#" + number : key;
	}

	// ---------------------------------------------------------------- the pristine save

	private static final class Building {
		String name;
		int dim;
		int x1, y1, z1, x2, y2, z2;
		boolean hasView;
		final double[] view = new double[3];
		float yaw, pitch;

		boolean contains(int x, int y, int z) {
			return x >= x1 && x <= x2 && y >= y1 && y <= y2 && z >= z1 && z <= z2;
		}

		boolean contains(NBTTagList pos) {
			return pos.tagCount() == 3 && contains((int) Math.floor(pos.func_150309_d(0)), (int) Math.floor(pos.func_150309_d(1)),
				(int) Math.floor(pos.func_150309_d(2)));
		}
	}

	/** The building list the builder wrote: `name dim x1 y1 z1 x2 y2 z2 [view x y z yaw pitch]` a line. */
	private static List<Building> buildings(TasSave save) {
		assertTrue(save.buildings != null && save.buildings.isFile(), "no building list for " + save.folder + " (" + save.buildings + ")");
		List<Building> list = new ArrayList<>();
		try {
			for (String line : Files.readAllLines(save.buildings.toPath(), StandardCharsets.UTF_8)) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				String[] parts = line.split("\\s+");
				Building b = new Building();
				b.name = parts[0];
				b.dim = Integer.parseInt(parts[1]);
				int[] c = new int[6];
				for (int i = 0; i < 6; i++) {
					c[i] = Integer.parseInt(parts[2 + i]);
				}
				b.x1 = Math.min(c[0], c[3]);
				b.y1 = Math.min(c[1], c[4]);
				b.z1 = Math.min(c[2], c[5]);
				b.x2 = Math.max(c[0], c[3]);
				b.y2 = Math.max(c[1], c[4]);
				b.z2 = Math.max(c[2], c[5]);
				if (parts.length >= 14 && parts[8].equals("view")) {
					b.hasView = true;
					for (int i = 0; i < 3; i++) {
						b.view[i] = Double.parseDouble(parts[9 + i]);
					}
					b.yaw = Float.parseFloat(parts[12]);
					b.pitch = Float.parseFloat(parts[13]);
				}
				list.add(b);
			}
		} catch (IOException e) {
			throw new AssertionError("can't read " + save.buildings + ": " + e);
		}
		assertTrue(!list.isEmpty(), "the building list of " + save.folder + " is empty");
		return list;
	}

	private static NBTTagCompound pristineLevel(TasSave save) {
		File file = new File(save.pristine, "level.dat");
		try (InputStream in = new FileInputStream(file)) {
			return CompressedStreamTools.readCompressed(in);
		} catch (IOException e) {
			throw new AssertionError("can't read the pristine " + file + ": " + e);
		}
	}

	/** Each chunk the building touches, as the pristine save has it (its Level compound), or null. */
	private static Map<Long, NBTTagCompound> pristineChunks(TasSave save, Building building) {
		Map<Long, NBTTagCompound> chunks = new HashMap<>();
		File world = building.dim == 0 ? save.pristine : new File(save.pristine, "DIM" + building.dim);
		for (int cx = building.x1 >> 4; cx <= building.x2 >> 4; cx++) {
			for (int cz = building.z1 >> 4; cz <= building.z2 >> 4; cz++) {
				chunks.put(chunkKey(cx, cz), pristineChunk(world, cx, cz));
			}
		}
		return chunks;
	}

	private static NBTTagCompound pristineChunk(File world, int cx, int cz) {
		File region = new File(new File(world, "region"), "r." + (cx >> 5) + "." + (cz >> 5) + ".mca");
		if (!region.isFile()) {
			return null;
		}
		RegionFile file = new RegionFile(region);
		try (DataInputStream in = file.getChunkDataInputStream(cx & 31, cz & 31)) {
			return in == null ? null : CompressedStreamTools.read(in).getCompoundTag("Level");
		} catch (IOException e) {
			throw new AssertionError("can't read chunk " + cx + " " + cz + " of " + region + ": " + e);
		} finally {
			try {
				file.close();
			} catch (IOException ignored) {
				// read only
			}
		}
	}

	/** A block of a pristine chunk: {id, meta}; air where it has no section. */
	private static int[] pristineBlock(NBTTagCompound level, int x, int y, int z) {
		NBTTagList sections = level.getTagList("Sections", 10);
		for (int i = 0; i < sections.tagCount(); i++) {
			NBTTagCompound section = sections.getCompoundTagAt(i);
			if (section.getByte("Y") != y >> 4) {
				continue;
			}
			int index = (y & 15) << 8 | (z & 15) << 4 | (x & 15);
			int id = section.getByteArray("Blocks")[index] & 255;
			if (section.hasKey("Add", 7)) {
				id |= nibble(section.getByteArray("Add"), index) << 8;
			}
			return new int[] {id, nibble(section.getByteArray("Data"), index)};
		}
		return new int[] {0, 0};
	}

	private static int nibble(byte[] array, int index) {
		return (array[index >> 1] >> ((index & 1) << 2)) & 15;
	}

	/** The building's dimension with each of its chunks loaded. */
	private static WorldServer loadChunks(Building building, Map<Long, NBTTagCompound> chunks) {
		WorldServer world = MinecraftServer.getServer().worldServerForDimension(building.dim);
		for (long key : chunks.keySet()) {
			world.theChunkProviderServer.loadChunk((int) (key >> 32), (int) key);
		}
		return world;
	}

	// ---------------------------------------------------------------- moving the player

	/** The player to a spot in a dimension, without portals; waits for the client to be there. */
	private void moveTo(int dim, double x, double y, double z, float yaw, float pitch) {
		onServer(() -> {
			EntityPlayerMP p = player();
			if (p.dimension != dim) {
				WorldServer target = MinecraftServer.getServer().worldServerForDimension(dim);
				MinecraftServer.getServer().getConfigurationManager().transferPlayerToDimension(p, dim, new Teleporter(target) {
					@Override
					public void placeInPortal(Entity entity, double px, double py, double pz, float f) {
						entity.setLocationAndAngles(x, y, z, yaw, pitch);
					}

					@Override
					public boolean placeInExistingPortal(Entity entity, double px, double py, double pz, float f) {
						return true;
					}

					@Override
					public boolean makePortal(Entity entity) {
						return true;
					}

					@Override
					public void removeStalePortalLocations(long time) {
					}
				});
				// out of the End, 1.7.10's transfer never puts the player into the new world (vanilla leaves the End
				// by respawning): not in its entity list, the player isn't updated, so no chunks are sent to them
				if (!target.loadedEntityList.contains(player())) {
					target.spawnEntityInWorld(player());
					target.updateEntityWithOptionalForce(player(), false);
				}
			}
			player().capabilities.allowFlying = true;
			player().capabilities.isFlying = true;
			player().sendPlayerAbilities();
			player().playerNetServerHandler.setPlayerLocation(x, y, z, yaw, pitch);
			player().fallDistance = 0.0F;
		});
		waitUntilOnClient("the client in dimension " + dim, () -> mc().thePlayer != null && mc().thePlayer.dimension == dim
			&& !(mc().currentScreen instanceof net.minecraft.client.gui.GuiDownloadTerrain), 400);
		waitUntilOnClient("the client at " + x + " " + y + " " + z, () -> Math.abs(mc().thePlayer.posX - x) < 0.5
			&& Math.abs(mc().thePlayer.posZ - z) < 0.5, 100);
	}

	// ---------------------------------------------------------------- small helpers

	private static TasSave save() {
		TasSave save = TasSave.current();
		if (save == null) {
			skipTest("only in save mode (spec §75.3: ./unittas_mcfly.sh oldsaves)");
		}
		return save;
	}

	/** Warnings and such go to a file in the results for reading, not into the verdict. */
	private void note(TasSave save, String what, List<String> lines) {
		if (lines.isEmpty()) {
			return;
		}
		File file = new File(resultsFolder(), "oldsaves-notes.txt");
		StringBuilder text = new StringBuilder("== " + save.folder + ", load " + save.pass + ": " + what + "\n");
		for (String line : lines) {
			text.append(line).append('\n');
		}
		try {
			Files.write(file.toPath(), text.toString().getBytes(StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE,
				java.nio.file.StandardOpenOption.APPEND);
		} catch (IOException e) {
			// only notes
		}
	}

	private static void differ(List<String> found, String what, Object was, Object now) {
		if (!String.valueOf(was).equals(String.valueOf(now))) {
			found.add(what + ": saved " + was + ", now " + now);
		}
	}

	private static String limit(List<String> lines) {
		if (lines.size() <= 80) {
			return lines.toString();
		}
		return new ArrayList<>(lines.subList(0, 80)) + " and " + (lines.size() - 80) + " more";
	}

	private static String savedId(TileEntity tile) {
		NBTTagCompound saved = new NBTTagCompound();
		tile.writeToNBT(saved);
		return saved.getString("id");
	}

	private static String blockName(int id) {
		if (id == 0) {
			return "air";
		}
		Block block = Block.getBlockById(id);
		String name = block == null ? null : Block.blockRegistry.getNameForObject(block);
		return name == null ? "#" + id : name;
	}

	private static String posText(NBTTagCompound entity) {
		NBTTagList pos = entity.getTagList("Pos", 6);
		return pos.tagCount() < 3 ? "?" : String.format(Locale.ROOT, "%.1f %.1f %.1f", pos.func_150309_d(0), pos.func_150309_d(1), pos.func_150309_d(2));
	}

	private static long chunkKey(int cx, int cz) {
		return (long) cx << 32 | (cz & 0xFFFFFFFFL);
	}

	private static String chunkName(long key) {
		return (int) (key >> 32) + " " + (int) key;
	}

	private static double sq(double d) {
		return d * d;
	}
}
