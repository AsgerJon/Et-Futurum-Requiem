package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.entities.EntityShulker;
import ganymedes01.etfuturum.loot.LootTableHolder;
import io.github.asgerjon.unittas.TasTestCase;
import ganymedes01.etfuturum.world.end.modern.EndCities;
import ganymedes01.etfuturum.world.end.modern.EndCityMarkers;
import ganymedes01.etfuturum.world.end.modern.ModernEnd;
import ganymedes01.etfuturum.world.end.modern.ModernEndTerrain;
import ganymedes01.etfuturum.world.structure.modern.ModernBlockStates;
import ganymedes01.etfuturum.world.structure.modern.ModernState;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldServer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The End's cities as 26.3's (SPEC-modern-movement.md §76.12, stages 7b and 7c), against 26.3's first city (nearest
 * the arrival platform, by its own locate; the reference's `end-city.txt`, kept as `end_city_20261029.txt`): the
 * city's step index, its start and every piece (template, turn, place, box, overwriting) as 26.3's, then its start
 * chunk and its ship's chunk block for block, their shulkers and the elytra's frame where 26.3 has them, and every
 * chest given the treasure table.
 */
@TasSuite("end")
public class EndCityTests extends TasTestCase {

	private static final String DUMP = "/assets/etfuturum/unittas/end_city_20261029.txt";

	@Override
	protected void setUp() {
		if (!ModernEnd.isActive()) {
			skipTest("modernEnd is off (or Hardcore Ender Expansion makes the End)");
		}
		if (!io.github.asgerjon.mcfly.registry.McFlyBlocks.PURPUR_BLOCK.isEnabled() || !io.github.asgerjon.mcfly.registry.McFlyBlocks.END_ROD.isEnabled() || !io.github.asgerjon.mcfly.registry.McFlyBlocks.CHORUS_PLANT.isEnabled()) {
			skipTest("purpur, end rods or chorus are off");
		}
	}

	@Override
	protected int timeoutSeconds() {
		return 240;
	}

	private static final class Chunk {
		int cx;
		int cz;
		String kind;
		final Map<Integer, String> runs = new LinkedHashMap<>();
		/** "x y z" to the state. */
		final Map<String, String> blocks = new TreeMap<>();
		final TreeSet<String> entities = new TreeSet<>();
	}

	private static final class Dump {
		int index = -1;
		int startX;
		int startZ;
		final List<String> pieces = new ArrayList<>();
		final Map<String, Chunk> chunks = new LinkedHashMap<>();
	}

	private static Dump readDump() throws Exception {
		Dump dump = new Dump();
		try (InputStream in = EndCityTests.class.getResourceAsStream(DUMP)) {
			assertTrue(in != null, "no " + DUMP);
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			Chunk chunk = null;
			for (String line; (line = reader.readLine()) != null; ) {
				String[] parts = line.split(" ");
				if (line.startsWith("index ")) {
					dump.index = Integer.parseInt(parts[1]);
				} else if (line.startsWith("start ")) {
					dump.startX = Integer.parseInt(parts[1]);
					dump.startZ = Integer.parseInt(parts[2]);
				} else if (line.startsWith("p ")) {
					dump.pieces.add(line.substring(2));
				} else if (line.startsWith("chunk ")) {
					chunk = new Chunk();
					chunk.cx = Integer.parseInt(parts[1]);
					chunk.cz = Integer.parseInt(parts[2]);
					chunk.kind = parts[3];
					dump.chunks.put(chunk.kind, chunk);
				} else if (line.startsWith("c ")) {
					int x = Integer.parseInt(parts[1]);
					int z = Integer.parseInt(parts[2]);
					chunk.runs.put((x & 15) << 4 | z & 15, line.substring(line.indexOf(' ', line.indexOf(' ', 2) + 1) + 1));
				} else if (line.startsWith("b ")) {
					chunk.blocks.put(parts[1] + " " + parts[2] + " " + parts[3], parts[4]);
				} else if (line.startsWith("e ")) {
					chunk.entities.add(entityKey(parts));
				}
			}
		}
		return dump;
	}

	/** An entity as compared: its kind and block (and a frame's facing and item). */
	private static String entityKey(String[] parts) {
		String key = parts[1] + " " + MathHelper.floor_double(Double.parseDouble(parts[2])) + " " + MathHelper.floor_double(Double.parseDouble(parts[3])) + " "
			+ MathHelper.floor_double(Double.parseDouble(parts[4]));
		return parts.length > 6 ? key + " " + parts[5] + " " + parts[6] : key;
	}

	private static ModernState parse(String state) {
		int open = state.indexOf('[');
		Map<String, String> properties = new TreeMap<>();
		if (open >= 0) {
			for (String pair : state.substring(open + 1, state.length() - 1).split(",")) {
				String[] kv = pair.split("=");
				properties.put(kv[0], kv[1]);
			}
		}
		return new ModernState(open >= 0 ? state.substring(0, open) : state, properties);
	}

	private static String describe(Block block, int meta) {
		return Block.blockRegistry.getNameForObject(block) + ":" + meta;
	}

	private static long seed() {
		return MinecraftServer.getServer().worldServerForDimension(0).getSeed();
	}

	/** The city's index among 26.3's step-4 structures (it seeds the city's placing random). */
	public void testStepIndex() throws Exception {
		assertEqual(readDump().index, EndCities.STEP_INDEX, "the end city's index in step 4");
	}

	/** The start chunk is one, and its pieces are 26.3's, in order: template, turn, place, box, overwriting. */
	public void testPiecesAs26_3() throws Exception {
		Dump dump = readDump();
		assertTrue(dump.startX != 0 || dump.startZ != 0 || !dump.pieces.isEmpty(), "the dump has no city");
		List<String> got = onServer(() -> {
			EndCities.Start start = EndCities.startAt(seed(), ModernEndTerrain.forSeed(seed()), dump.startX, dump.startZ);
			List<String> lines = new ArrayList<>();
			if (start != null) {
				for (EndCities.Piece piece : start.pieces) {
					lines.add(piece.name + " " + piece.rotation().name() + " " + piece.x + " " + piece.y + " " + piece.z + " " + piece.box + " " + piece.overwrite);
				}
			}
			return lines;
		});
		List<String> wrong = new ArrayList<>();
		for (int i = 0; i < Math.max(got.size(), dump.pieces.size()) && wrong.size() < 6; i++) {
			String a = i < got.size() ? got.get(i) : "none";
			String b = i < dump.pieces.size() ? dump.pieces.get(i) : "none";
			if (!a.equals(b)) {
				wrong.add(i + ": " + a + ", 26.3's " + b);
			}
		}
		assertEqual(dump.pieces.size() + " pieces []", got.size() + " pieces " + wrong, "the city's pieces");
	}

	/** A dumped chunk here: the 4 by 4 loaded that populates the 3 by 3 around it, then its blocks against 26.3's. */
	private static String compareChunk(Chunk want) {
		WorldServer end = MinecraftServer.getServer().worldServerForDimension(1);
		// the 4 by 4 that populates exactly the 3 by 3 (a chunk populates once its +x and +z neighbours are there), so
		// no chunk further out is made whole before its own test looks at it
		for (int dx = -1; dx <= 2; dx++) {
			for (int dz = -1; dz <= 2; dz++) {
				end.getChunkFromChunkCoords(want.cx + dx, want.cz + dz);
			}
		}
		int badColumns = 0;
		int badBlocks = 0;
		List<String> wrong = new ArrayList<>();
		for (int x = want.cx * 16; x < want.cx * 16 + 16; x++) {
			for (int z = want.cz * 16; z < want.cz * 16 + 16; z++) {
				StringBuilder runs = new StringBuilder();
				int start = -1;
				for (int y = 0; y <= 256; y++) {
					Block block = y < 256 ? end.getBlock(x, y, z) : Blocks.air;
					boolean stone = block == Blocks.end_stone;
					if (stone && start < 0) {
						start = y;
					} else if (!stone && start >= 0) {
						runs.append(runs.length() > 0 ? " " : "").append(start).append('-').append(y - 1);
						start = -1;
					}
					if (y < 256) {
						String key = x + " " + y + " " + z;
						String state = want.blocks.get(key);
						String expected;
						if (state == null) {
							expected = stone ? describe(Blocks.end_stone, 0) : "air";
						} else {
							ModernBlockStates.Placed placed = ModernBlockStates.convert(parse(state));
							expected = placed == null ? "none for " + state : describe(placed.block, placed.meta);
						}
						String got = block == Blocks.air ? "air" : describe(block, end.getBlockMetadata(x, y, z));
						if (!stone && !got.equals(expected) && (state != null || block != Blocks.air)) {
							badBlocks++;
							if (wrong.size() < 6) {
								wrong.add("(" + key + ") " + got + ", 26.3's " + (state == null ? "air" : state));
							}
						}
					}
				}
				String a = want.runs.getOrDefault((x & 15) << 4 | z & 15, "");
				if (!a.equals(runs.toString())) {
					badColumns++;
					if (wrong.size() < 6) {
						wrong.add("column (" + x + ", " + z + "): " + runs + ", 26.3's " + a);
					}
				}
			}
		}
		return badColumns + " columns, " + badBlocks + " blocks" + (wrong.isEmpty() ? "" : " " + wrong);
	}

	public void testStartChunkAs26_3() throws Exception {
		Chunk want = readDump().chunks.get("start");
		assertTrue(want != null, "the dump has no start chunk");
		assertEqual("0 columns, 0 blocks", onServer(() -> compareChunk(want)), "the start chunk (" + want.cx + ", " + want.cz + ")");
	}

	public void testShipChunkAs26_3() throws Exception {
		Chunk want = readDump().chunks.get("ship");
		if (want == null) {
			skipTest("26.3's first city has no ship");
		}
		assertEqual("0 columns, 0 blocks", onServer(() -> compareChunk(want)), "the ship's chunk (" + want.cx + ", " + want.cz + ")");
	}

	/** The shulkers and the elytra's frame in those chunks where 26.3 has them; every chest there given the treasure. */
	public void testMarkersAs26_3() throws Exception {
		Dump dump = readDump();
		for (Chunk want : dump.chunks.values()) {
			String found = onServer(() -> {
				compareChunk(want);
				WorldServer end = MinecraftServer.getServer().worldServerForDimension(1);
				TreeSet<String> entities = new TreeSet<>();
				for (Object object : end.loadedEntityList) {
					Entity entity = (Entity) object;
					int x = MathHelper.floor_double(entity.posX);
					int z = MathHelper.floor_double(entity.posZ);
					if (x >> 4 != want.cx || z >> 4 != want.cz) {
						continue;
					}
					int y = MathHelper.floor_double(entity.posY);
					if (entity instanceof EntityShulker) {
						entities.add("shulker " + x + " " + y + " " + z);
					} else if (entity instanceof EntityItemFrame frame) {
						String[] facings = {"south", "west", "north", "east"};
						String item = frame.getDisplayedItem() == null ? "air" : frame.getDisplayedItem().getItem() == io.github.asgerjon.mcfly.registry.McFlyItems.ELYTRA.get() ? "elytra" : "other";
						entities.add("item_frame " + x + " " + y + " " + z + " " + facings[frame.hangingDirection] + " " + item);
					}
				}
				int chests = 0;
				int withTable = 0;
				for (Object object : end.loadedTileEntityList) {
					TileEntity entity = (TileEntity) object;
					if (entity.xCoord >> 4 == want.cx && entity.zCoord >> 4 == want.cz && entity.getBlockType() == Blocks.chest) {
						chests++;
						if (entity instanceof LootTableHolder holder && EndCityMarkers.TREASURE.equals(holder.etfu$lootTable())) {
							withTable++;
						}
					}
				}
				return entities + " chests " + chests + " with the table " + withTable;
			});
			int chests = 0;
			for (String state : want.blocks.values()) {
				if (state.startsWith("minecraft:chest[")) {
					chests++;
				}
			}
			assertEqual(want.entities + " chests " + chests + " with the table " + chests, found, "the " + want.kind + " chunk's entities and chests");
		}
	}
}
