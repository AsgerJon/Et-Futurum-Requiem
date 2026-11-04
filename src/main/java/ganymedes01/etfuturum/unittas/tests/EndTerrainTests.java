package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.unittas.TasTestCase;
import ganymedes01.etfuturum.world.end.modern.ChunkProviderModernEnd;
import ganymedes01.etfuturum.world.end.modern.ModernEndBiomes;
import ganymedes01.etfuturum.world.end.modern.ModernEndTerrain;
import ganymedes01.etfuturum.world.end.modern.WorldProviderModernEnd;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.DimensionManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The End's terrain, biomes and arrival as 26.3's (SPEC-modern-movement.md §76.5, stage 0): its chunks block for block
 * against 26.3's own (`end_terrain_20261029.txt`, the reference's dump of chunks generated to 26.3's terrain step at
 * the test world's seed), its biomes on the dump's grid, the End using the new provider, and arriving onto the platform.
 */
public class EndTerrainTests extends TasTestCase {

	private static final String DUMP = "/assets/etfuturum/unittas/end_terrain_20261029.txt";

	@Override
	protected void setUp() {
		if (!ganymedes01.etfuturum.world.end.modern.ModernEnd.isActive()) {
			skipTest("modernEnd is off (or Hardcore Ender Expansion makes the End)");
		}
	}

	/** 26.3's dump: each chunk's columns with blocks (runs "y0-y1"), and the biome grid. */
	private static final class Dump {
		final Map<Long, Map<Integer, String>> chunks = new LinkedHashMap<>();
		final List<Object[]> biomes = new ArrayList<>();
	}

	private static Dump readDump() throws Exception {
		Dump dump = new Dump();
		try (InputStream in = EndTerrainTests.class.getResourceAsStream(DUMP)) {
			assertTrue(in != null, "no " + DUMP);
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			Map<Integer, String> chunk = null;
			for (String line; (line = reader.readLine()) != null; ) {
				String[] parts = line.split(" ");
				if (line.startsWith("chunk ")) {
					chunk = new LinkedHashMap<>();
					dump.chunks.put(key(Integer.parseInt(parts[1]), Integer.parseInt(parts[2])), chunk);
				} else if (line.startsWith("c ")) {
					int x = Integer.parseInt(parts[1]);
					int z = Integer.parseInt(parts[2]);
					chunk.put((x & 15) << 4 | z & 15, line.substring(line.indexOf(' ', line.indexOf(' ', 2) + 1) + 1));
				} else if (line.startsWith("b ")) {
					dump.biomes.add(new Object[] {Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), parts[3]});
				}
			}
		}
		return dump;
	}

	private static long key(int cx, int cz) {
		return (long) cx << 32 | cz & 0xFFFFFFFFL;
	}

	/** A column's runs of blocks, as the dump writes them ("44-61 70-72"), or "" for none. */
	private static String runs(boolean[] solid, int x, int z) {
		StringBuilder out = new StringBuilder();
		int start = -1;
		for (int y = 0; y <= ModernEndTerrain.HEIGHT; y++) {
			boolean here = y < ModernEndTerrain.HEIGHT && solid[ModernEndTerrain.index(x, y, z)];
			if (here && start < 0) {
				start = y;
			} else if (!here && start >= 0) {
				out.append(out.length() > 0 ? " " : "").append(start).append('-').append(y - 1);
				start = -1;
			}
		}
		return out.toString();
	}

	/**
	 * Every column of the dump's 65 chunks (the main island every other chunk, 16 past 1024 blocks) as 26.3's: the
	 * same runs of end stone at the test world's seed.
	 */
	public void testTerrainAs26_3() throws Exception {
		Dump dump = readDump();
		long seed = onServer(() -> MinecraftServer.getServer().worldServerForDimension(0).getSeed());
		List<String> wrong = new ArrayList<>();
		int columns = 0;
		int badColumns = 0;
		for (Map.Entry<Long, Map<Integer, String>> entry : dump.chunks.entrySet()) {
			int cx = (int) (entry.getKey() >> 32);
			int cz = (int) (long) entry.getKey();
			boolean[] solid = ModernEndTerrain.forSeed(seed).fillChunk(cx, cz);
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					String want = entry.getValue().getOrDefault(x << 4 | z, "");
					String got = runs(solid, x, z);
					columns++;
					if (!want.equals(got)) {
						badColumns++;
						if (wrong.size() < 8) {
							wrong.add(String.format("(%d, %d): %s, 26.3's %s", cx * 16 + x, cz * 16 + z, got.isEmpty() ? "air" : got, want.isEmpty() ? "air" : want));
						}
					}
				}
			}
		}
		assertEqual("0 of " + columns + " columns wrong []", badColumns + " of " + columns + " columns wrong " + wrong, "the End's terrain");
	}

	/** The biome on the dump's grid (every 128 blocks from -3072 to 3072) as 26.3's. */
	public void testBiomesAs26_3() throws Exception {
		Dump dump = readDump();
		long seed = onServer(() -> MinecraftServer.getServer().worldServerForDimension(0).getSeed());
		List<String> wrong = new ArrayList<>();
		int bad = 0;
		for (Object[] point : dump.biomes) {
			BiomeGenBase biome = ModernEndBiomes.at(ModernEndTerrain.forSeed(seed), (Integer) point[0], (Integer) point[1]);
			String got = ModernEndBiomes.modernName(biome);
			if (!point[2].equals(got)) {
				bad++;
				if (wrong.size() < 8) {
					wrong.add(String.format("(%d, %d): %s, 26.3's %s", point[0], point[1], got, point[2]));
				}
			}
		}
		assertEqual("0 of " + dump.biomes.size() + " wrong []", bad + " of " + dump.biomes.size() + " wrong " + wrong, "the End's biomes");
	}

	/** The End's provider and generator are the new ones, and its chunks are the terrain above (chunk (0, 0)). */
	public void testTheEndUsesIt() {
		assertEqual(WorldProviderModernEnd.class.getName(), onServer(() -> DimensionManager.getWorld(1) != null
			? DimensionManager.getWorld(1).provider.getClass().getName()
			: MinecraftServer.getServer().worldServerForDimension(1).provider.getClass().getName()), "the End's provider");
		String generator = onServer(() -> {
			WorldServer end = MinecraftServer.getServer().worldServerForDimension(1);
			return end.theChunkProviderServer.currentChunkProvider.getClass().getName();
		});
		assertEqual(ChunkProviderModernEnd.class.getName(), generator, "the End's generator");
		String column = onServer(() -> {
			WorldServer end = MinecraftServer.getServer().worldServerForDimension(1);
			net.minecraft.world.chunk.Chunk chunk = end.theChunkProviderServer.currentChunkProvider.provideChunk(0, 1);
			boolean[] solid = new boolean[ModernEndTerrain.HEIGHT * 256];
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					for (int y = 0; y < ModernEndTerrain.HEIGHT; y++) {
						solid[ModernEndTerrain.index(x, y, z)] = chunk.getBlock(x, y, z) == Blocks.end_stone;
					}
				}
			}
			boolean[] want = ModernEndTerrain.forSeed(end.getSeed()).fillChunk(0, 1);
			return java.util.Arrays.equals(solid, want) ? "same" : "different at column (8, 8): " + runs(solid, 8, 8) + ", wanted " + runs(want, 8, 8);
		});
		assertEqual("same", column, "the End's chunk (0, 1) against the terrain");
		String biome = onServer(() -> MinecraftServer.getServer().worldServerForDimension(1).getBiomeGenForCoords(1100, 40).biomeName);
		assertTrue(!biome.equals(BiomeGenBase.sky.biomeName), "the outer End at (1100, 40) is still " + biome);
	}

	/**
	 * 26.3 makes the platform with the End's terrain too (its `end_platform` feature): obsidian at y 48 around (100, 0).
	 * The area cleared first (an arrival in an earlier test makes one), then its chunk populated again.
	 */
	public void testPlatformWithTheTerrain() {
		String found = onServer(() -> {
			WorldServer end = MinecraftServer.getServer().worldServerForDimension(1);
			for (int cx = 5; cx <= 7; cx++) {
				for (int cz = -2; cz <= 1; cz++) {
					end.theChunkProviderServer.loadChunk(cx, cz);
				}
			}
			for (int x = 98; x <= 102; x++) {
				for (int z = -2; z <= 2; z++) {
					for (int y = 48; y <= 51; y++) {
						end.setBlockToAir(x, y, z);
					}
				}
			}
			end.theChunkProviderServer.currentChunkProvider.populate(end.theChunkProviderServer, 5, -1);
			return platformProblems(end);
		});
		assertEqual("[]", found, "the platform when the End's chunks are made");
	}

	/** What's wrong with the platform: obsidian 5 by 5 at y 48 around (100, 0), air 3 above. */
	private static String platformProblems(WorldServer end) {
		List<String> problems = new ArrayList<>();
		for (int x = 98; x <= 102; x++) {
			for (int z = -2; z <= 2; z++) {
				if (end.getBlock(x, 48, z) != Blocks.obsidian) {
					problems.add("no obsidian at " + x + " 48 " + z);
				}
				for (int y = 49; y <= 51; y++) {
					if (!end.isAirBlock(x, y, z)) {
						problems.add(end.getBlock(x, y, z).getUnlocalizedName() + " at " + x + " " + y + " " + z);
					}
				}
			}
		}
		return problems.size() > 4 ? problems.subList(0, 4) + " and " + (problems.size() - 4) + " more" : problems.toString();
	}

	/**
	 * Through the portal as 26.3's: the platform made (a block in the way broken and dropped), the player at (100.5, 49,
	 * 0.5) facing west, its pitch kept (1.7.10: the block corner, the pitch and motion reset). A player's motion isn't
	 * carried: 1.7.10's client makes a new player entity on changing dimension (noted in the spec).
	 */
	public void testArrivalAs26_3() {
		// the area made and populated first (26.3 places no block in a chunk before its features), then dirt in the way
		onServer(() -> {
			WorldServer end = MinecraftServer.getServer().worldServerForDimension(1);
			for (int cx = 5; cx <= 7; cx++) {
				for (int cz = -2; cz <= 1; cz++) {
					end.theChunkProviderServer.loadChunk(cx, cz);
				}
			}
			end.setBlock(101, 50, 1, Blocks.dirt);
			player().inventory.clearInventory(net.minecraft.item.Item.getItemFromBlock(Blocks.dirt), -1);
		});
		// the pitch set and the trip made in one server step (the client's look packets would set it between)
		String where = onServer(() -> {
			player().rotationPitch = 30.0F;
			player().travelToDimension(1);
			return String.format("%.2f %.2f %.2f facing %.0f, pitch %.0f", player().posX, player().posY, player().posZ,
				net.minecraft.util.MathHelper.wrapAngleTo180_float(player().rotationYaw), player().rotationPitch);
		});
		assertEqual("100.50 49.00 0.50 facing 90, pitch 30", where, "where the player arrives");
		waitUntilOnClient("in the End", () -> mc().thePlayer.dimension == 1 && !(mc().currentScreen instanceof net.minecraft.client.gui.GuiDownloadTerrain), 200);
		ticks(2);
		assertEqual("pitch 30", onClient(() -> String.format("pitch %.0f", mc().thePlayer.rotationPitch)), "the pitch the player's client got");
		assertEqual("[]", onServer(() -> platformProblems(MinecraftServer.getServer().worldServerForDimension(1))), "the platform");
		boolean dropped = onServer(() -> {
			// on the platform the player soon picks it up (a dropped block waits 10 ticks)
			if (player().inventory.hasItem(net.minecraft.item.Item.getItemFromBlock(Blocks.dirt))) {
				return true;
			}
			for (Object entity : MinecraftServer.getServer().worldServerForDimension(1).loadedEntityList) {
				if (entity instanceof EntityItem item && item.getEntityItem().getItem() == net.minecraft.item.Item.getItemFromBlock(Blocks.dirt)
					&& item.getDistance(101.5, 50.5, 1.5) < 3.0) {
					return true;
				}
			}
			return false;
		});
		assertTrue(dropped, "the dirt in the platform's way dropped as an item");
	}

	/** Anything but a player arrives one block higher, at (100.5, 50, 0.5), keeping its motion (26.3's portal). */
	public void testItemArrivesHigher() {
		String found = onServer(() -> {
			EntityItem item = new EntityItem(player().worldObj, player().posX, player().posY + 1.0, player().posZ, new ItemStack(Blocks.dirt).setStackDisplayName("etfu end arrival"));
			item.delayBeforeCanPickup = 32767;
			item.motionX = 0.25;
			player().worldObj.spawnEntityInWorld(item);
			item.travelToDimension(1);
			for (Object entity : MinecraftServer.getServer().worldServerForDimension(1).loadedEntityList) {
				if (entity instanceof EntityItem arrived && "etfu end arrival".equals(arrived.getEntityItem().getDisplayName())) {
					// 1.7.10 keeps an entity's y above its bottom by its yOffset (an item's 0.125): the bottom is what's placed
					String at = String.format("%.2f %.2f %.2f moving %.2f", arrived.posX, arrived.boundingBox.minY, arrived.posZ, arrived.motionX);
					arrived.setDead();
					return at;
				}
			}
			return "not in the End";
		});
		assertEqual("100.50 50.00 0.50 moving 0.25", found, "the item through the portal");
	}
	/**
	 * Entities in a chunk loaded outside any player's view (the fight's scan loads such, and the respawn writes in them)
	 * survive the End unloading and loading again: a crystal far out, the player away, the End gone and back.
	 */
	public void testEntitiesSurviveTheEndReloading() {
		onServer(() -> {
			WorldServer end = MinecraftServer.getServer().worldServerForDimension(1);
			end.theChunkProviderServer.loadChunk(-6, -6);
			net.minecraft.entity.item.EntityEnderCrystal crystal = new net.minecraft.entity.item.EntityEnderCrystal(end);
			crystal.setLocationAndAngles(-90.5, 70.0, -90.5, 0.0F, 0.0F);
			end.spawnEntityInWorld(crystal);
		});
		waitUntil("the End unloaded", () -> DimensionManager.getWorld(1) == null, 400);
		String found = onServer(() -> {
			WorldServer end = MinecraftServer.getServer().worldServerForDimension(1);
			end.theChunkProviderServer.loadChunk(-6, -6);
			int crystals = 0;
			for (Object entity : end.loadedEntityList) {
				if (entity instanceof net.minecraft.entity.item.EntityEnderCrystal crystal && Math.abs(crystal.posX + 90.5) < 0.01) {
					crystals++;
					crystal.setDead();
				}
			}
			return crystals + " crystals";
		});
		assertEqual("1 crystals", found, "the crystal after the End unloaded and loaded again");
	}
}
