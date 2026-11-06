package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import ganymedes01.etfuturum.ModBlocks;
import io.github.asgerjon.unittas.TasTestCase;
import ganymedes01.etfuturum.world.end.modern.EndDecoration;
import ganymedes01.etfuturum.world.end.modern.ModernEnd;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * The outer End's features as 26.3's (SPEC-modern-movement.md §76.11, stage 6), against 26.3's own: the reference
 * dumps (`end_features_20261029.txt`) the step indices 26.3 sorts the End's features into, and three outer chunks as
 * 26.3 finishes them (the 3 by 3 around each made whole): the first highlands chunk out from (64, 0) with chorus, the
 * first small islands chunk whose island draw passes, the first highlands chunk with a return gateway. Each must be
 * the same here, block for block: its end stone column by column, and every other block (chorus plants, flowers and
 * their age, a gateway and its bedrock).
 */
@TasSuite("end")
public class EndFeatureTests extends TasTestCase {

	private static final String DUMP = "/assets/etfuturum/unittas/end_features_20261029.txt";

	@Override
	protected void setUp() {
		if (!ModernEnd.isActive()) {
			skipTest("modernEnd is off (or Hardcore Ender Expansion makes the End)");
		}
		if (!io.github.asgerjon.mcfly.registry.McFlyBlocks.CHORUS_PLANT.isEnabled() || !io.github.asgerjon.mcfly.registry.McFlyBlocks.CHORUS_FLOWER.isEnabled() || !io.github.asgerjon.mcfly.registry.McFlyBlocks.END_GATEWAY.isEnabled()) {
			skipTest("chorus or the gateway is off");
		}
	}

	@Override
	protected int timeoutSeconds() {
		return 180;
	}

	/** One dumped chunk: its end stone runs by column, its other blocks ("x y z name [age]"). */
	private static final class Chunk {
		final int cx;
		final int cz;
		final String kind;
		final Map<Integer, String> runs = new LinkedHashMap<>();
		final TreeSet<String> blocks = new TreeSet<>();

		Chunk(int cx, int cz, String kind) {
			this.cx = cx;
			this.cz = cz;
			this.kind = kind;
		}
	}

	private static final class Dump {
		final Map<String, Integer> indices = new LinkedHashMap<>();
		final Map<String, Chunk> chunks = new LinkedHashMap<>();
	}

	private static Dump readDump() throws Exception {
		Dump dump = new Dump();
		try (InputStream in = EndFeatureTests.class.getResourceAsStream(DUMP)) {
			assertTrue(in != null, "no " + DUMP);
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			Chunk chunk = null;
			for (String line; (line = reader.readLine()) != null; ) {
				String[] parts = line.split(" ");
				if (line.startsWith("i ")) {
					dump.indices.put(parts[1] + " " + parts[2], Integer.parseInt(parts[3]));
				} else if (line.startsWith("chunk ")) {
					chunk = new Chunk(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), parts[3]);
					dump.chunks.put(chunk.kind, chunk);
				} else if (line.startsWith("c ")) {
					int x = Integer.parseInt(parts[1]);
					int z = Integer.parseInt(parts[2]);
					chunk.runs.put((x & 15) << 4 | z & 15, line.substring(line.indexOf(' ', line.indexOf(' ', 2) + 1) + 1));
				} else if (line.startsWith("b ")) {
					chunk.blocks.add(asHere(parts));
				}
			}
		}
		return dump;
	}

	/** The chunk at the dump's place in this End: the 4 by 4 loaded that populates the 3 by 3 around it, then read. */
	private static Chunk here(Chunk want) {
		WorldServer end = MinecraftServer.getServer().worldServerForDimension(1);
		// the 4 by 4 that populates exactly the 3 by 3 (a chunk populates once its +x and +z neighbours are there), so
		// no chunk further out is made whole before its own test looks at it
		for (int dx = -1; dx <= 2; dx++) {
			for (int dz = -1; dz <= 2; dz++) {
				end.getChunkFromChunkCoords(want.cx + dx, want.cz + dz);
			}
		}
		Chunk got = new Chunk(want.cx, want.cz, want.kind);
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
					if (!stone && block != Blocks.air) {
						String name = Block.blockRegistry.getNameForObject(block);
						if (block == io.github.asgerjon.mcfly.registry.McFlyBlocks.CHORUS_FLOWER.get()) {
							name += " " + end.getBlockMetadata(x, y, z);
						}
						got.blocks.add(x + " " + y + " " + z + " " + name);
					}
				}
				if (runs.length() > 0) {
					got.runs.put((x & 15) << 4 | z & 15, runs.toString());
				}
			}
		}
		return got;
	}

	/**
	 * A dumped block ("x y z id [age]") as it should be here: 26.3's block through the state table (the dump has no
	 * other properties, so the block alone, with a chorus flower's age).
	 */
	private static String asHere(String[] parts) {
		java.util.Map<String, String> properties = new java.util.TreeMap<>();
		if (parts.length > 5) {
			properties.put("age", parts[5]);
		}
		ganymedes01.etfuturum.world.structure.modern.ModernBlockStates.Placed placed = ganymedes01.etfuturum.world.structure.modern.ModernBlockStates
			.convert(new ganymedes01.etfuturum.world.structure.modern.ModernState(parts[4], properties));
		String name = placed == null ? "none for " + parts[4] : Block.blockRegistry.getNameForObject(placed.block);
		if (placed != null && placed.block == io.github.asgerjon.mcfly.registry.McFlyBlocks.CHORUS_FLOWER.get()) {
			name += " " + placed.meta;
		}
		return parts[1] + " " + parts[2] + " " + parts[3] + " " + name;
	}

	/** The differences: columns whose end stone differs, blocks only here, blocks only in 26.3's. */
	private static String compare(Chunk want, Chunk got) {
		List<String> wrong = new ArrayList<>();
		int badColumns = 0;
		for (int i = 0; i < 256; i++) {
			String a = want.runs.getOrDefault(i, "");
			String b = got.runs.getOrDefault(i, "");
			if (!a.equals(b)) {
				badColumns++;
				if (wrong.size() < 6) {
					wrong.add(String.format("(%d, %d): %s, 26.3's %s", want.cx * 16 + (i >> 4), want.cz * 16 + (i & 15), b.isEmpty() ? "air" : b, a.isEmpty() ? "air" : a));
				}
			}
		}
		TreeSet<String> onlyHere = new TreeSet<>(got.blocks);
		onlyHere.removeAll(want.blocks);
		TreeSet<String> onlyThere = new TreeSet<>(want.blocks);
		onlyThere.removeAll(got.blocks);
		return badColumns + " columns, " + onlyHere.size() + " blocks only here, " + onlyThere.size() + " only in 26.3's (of " + want.blocks.size() + ")"
			+ (wrong.isEmpty() ? "" : " " + wrong)
			+ (onlyHere.isEmpty() ? "" : " here " + new ArrayList<>(onlyHere).subList(0, Math.min(6, onlyHere.size())))
			+ (onlyThere.isEmpty() ? "" : " 26.3's " + new ArrayList<>(onlyThere).subList(0, Math.min(6, onlyThere.size())));
	}

	private void sameAs26_3(String kind) throws Exception {
		Dump dump = readDump();
		Chunk want = dump.chunks.get(kind);
		assertTrue(want != null, "the dump has no " + kind + " chunk");
		String found = onServer(() -> compare(want, here(want)));
		assertEqual("0 columns, 0 blocks only here, 0 only in 26.3's (of " + want.blocks.size() + ")", found,
			"the " + kind + " chunk (" + want.cx + ", " + want.cz + ")");
	}

	/** The step indices of the End's three outer features, as 26.3's FeatureSorter gives them. */
	public void testFeatureIndices() throws Exception {
		Dump dump = readDump();
		String want = "end_island_decorated 0 " + EndDecoration.ISLAND_INDEX + ", end_gateway_return 4 " + EndDecoration.GATEWAY_INDEX
			+ ", chorus_plant 9 " + EndDecoration.CHORUS_INDEX;
		String got = "end_island_decorated 0 " + dump.indices.get("end_island_decorated 0") + ", end_gateway_return 4 "
			+ dump.indices.get("end_gateway_return 4") + ", chorus_plant 9 " + dump.indices.get("chorus_plant 9");
		assertEqual(want, got, "the features' indices (EFR's, 26.3's)");
	}

	/** A highlands chunk with chorus: every plant and flower where 26.3 grows them. */
	public void testChorusAs26_3() throws Exception {
		sameAs26_3("chorus");
	}

	/** A small islands chunk with a small island: its end stone as 26.3's. */
	public void testSmallIslandAs26_3() throws Exception {
		sameAs26_3("island");
	}

	/** A highlands chunk with a return gateway: the gateway and its bedrock where 26.3 puts them. */
	public void testReturnGatewayAs26_3() throws Exception {
		sameAs26_3("gateway");
	}

	/** A return gateway leads exactly to (100, 50, 0), above the arrival platform. */
	public void testReturnGatewayLeadsHome() throws Exception {
		Dump dump = readDump();
		Chunk want = dump.chunks.get("gateway");
		assertTrue(want != null, "the dump has no gateway chunk");
		String found = onServer(() -> {
			here(want);
			WorldServer end = MinecraftServer.getServer().worldServerForDimension(1);
			for (int x = want.cx * 16; x < want.cx * 16 + 16; x++) {
				for (int z = want.cz * 16; z < want.cz * 16 + 16; z++) {
					for (int y = 0; y < 256; y++) {
						if (end.getTileEntity(x, y, z) instanceof io.github.asgerjon.mcfly.end.EndGatewayBlockEntity gateway) {
							int[] exit = gateway.exitPortal();
							return (exit == null ? "none" : exit[0] + " " + exit[1] + " " + exit[2]) + " exact " + gateway.exactTeleport();
						}
					}
				}
			}
			return "no gateway";
		});
		assertEqual("100 50 0 exact true", found, "the return gateway's exit");
	}
}
