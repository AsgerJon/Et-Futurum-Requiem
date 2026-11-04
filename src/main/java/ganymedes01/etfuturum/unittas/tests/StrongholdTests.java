package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.structure.MapGenStronghold;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Strongholds in 26.3's rings (SPEC-modern-movement.md §64.4): 128 of them, rings of 3, 6, 10, 15, 21, 28, 36 and 9
 * at 4 d + 6 d c chunks (d = 32, c the ring) give or take 1.25 d, then nudged into a biome within 112 blocks; the
 * nearest to the spawn in the first ring. A fresh generator on the test world (its one biome takes any nudge).
 */
public class StrongholdTests extends TasTestCase {

	private static final int[] RINGS = {3, 6, 10, 15, 21, 28, 36, 9};
	/** 1.25 d each way, the 112-block nudge (7 chunks) and rounding. */
	private static final double SLACK = 40.0D + 8.0D;

	@Override
	protected void tearDown() {
		McFlyConfig.modernStrongholds = true;
		McFlyConfig.strongholdCount = 128;
	}

	public void testRings() throws Exception {
		ChunkCoordIntPair[] found = positions(new MapGenStronghold());
		assertEqual(128, found.length, "strongholds");
		int at = 0;
		for (int ring = 0; ring < RINGS.length; ring++) {
			double middle = 4 * 32 + 6 * 32 * ring;
			for (int i = 0; i < RINGS[ring]; i++, at++) {
				double distance = Math.hypot(found[at].chunkXPos, found[at].chunkZPos);
				assertTrue(Math.abs(distance - middle) <= SLACK, "stronghold " + at + " (ring " + ring + ") at " + (int) distance
						+ " chunks, not " + (int) middle + " give or take " + (int) SLACK);
			}
		}
		// the first ring's three about a third of a turn apart (the nudge moves each by up to 7 of ~128 chunks)
		for (int i = 0; i < 3; i++) {
			double a = Math.atan2(found[i].chunkZPos, found[i].chunkXPos), b = Math.atan2(found[(i + 1) % 3].chunkZPos, found[(i + 1) % 3].chunkXPos);
			double apart = Math.toDegrees(Math.abs(Math.atan2(Math.sin(b - a), Math.cos(b - a))));
			assertTrue(Math.abs(apart - 120.0D) < 15.0D, "the first ring's strongholds " + i + " and " + (i + 1) % 3 + " are " + (int) apart + " degrees apart");
		}
	}

	/** The eye of ender's pick: the nearest to the spawn is in the first ring. */
	public void testNearestIsInTheFirstRing() throws Exception {
		MapGenStronghold generator = new MapGenStronghold();
		positions(generator);
		ChunkPosition nearest = onServer(() -> generator.func_151545_a(world(), 0, 64, 0));
		assertIsNotNone(nearest, "no stronghold found");
		double chunks = Math.hypot(nearest.chunkPosX, nearest.chunkPosZ) / 16.0D;
		assertTrue(chunks >= 4 * 32 - SLACK && chunks <= 4 * 32 + SLACK, "the nearest stronghold " + (int) chunks + " chunks out");
	}

	/**
	 * 1.7.10 works the positions out at once on the server thread, the first time a chunk generates (26.3 in the
	 * background): 128 biome searches on a normal world's biomes take a moment; the time goes to stronghold-time.txt.
	 */
	public void testTimeOnANormalWorld() throws Exception {
		long[] took = onServer(() -> {
			net.minecraft.world.biome.WorldChunkManager biomes = new net.minecraft.world.biome.WorldChunkManager(world().getSeed(), net.minecraft.world.WorldType.DEFAULT);
			java.util.Random random = new java.util.Random(world().getSeed());
			java.util.List<net.minecraft.world.biome.BiomeGenBase> allowed = new MapGenStronghold().field_151546_e;
			long start = System.nanoTime();
			int found = 0;
			for (int i = 0; i < 128; i++) {
				double angle = random.nextDouble() * Math.PI * 2.0D, dist = 128 + 192 * (i / 16);
				int x = (int) Math.round(Math.cos(angle) * dist), z = (int) Math.round(Math.sin(angle) * dist);
				if (biomes.findBiomePosition((x << 4) + 8, (z << 4) + 8, 112, allowed, new java.util.Random(random.nextLong())) != null) {
					found++;
				}
			}
			return new long[]{(System.nanoTime() - start) / 1_000_000L, found};
		});
		java.nio.file.Files.write(new java.io.File(resultsFolder(), "stronghold-time.txt").toPath(),
				("128 biome searches on a normal world: " + took[0] + " ms, " + took[1] + " found a biome\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
		assertTrue(took[0] < 5000L, "128 biome searches took " + took[0] + " ms");
	}

	public void testCountOption() throws Exception {
		McFlyConfig.strongholdCount = 20;
		assertEqual(20, positions(new MapGenStronghold()).length, "with strongholdCount 20");
	}

	public void testOptionOff() throws Exception {
		McFlyConfig.modernStrongholds = false;
		ChunkCoordIntPair[] found = positions(new MapGenStronghold());
		assertEqual(3, found.length, "1.7.10's strongholds");
		for (ChunkCoordIntPair pair : found) {
			double distance = Math.hypot(pair.chunkXPos, pair.chunkZPos);
			assertTrue(distance >= 40.0D - 8.0D && distance <= 72.0D + 8.0D, "a 1.7.10 stronghold at " + (int) distance + " chunks");
		}
	}

	// ---------------------------------------------------------------- helpers

	/** The generator's positions on the test world (worked out on its first check, as when a chunk generates). */
	private ChunkCoordIntPair[] positions(MapGenStronghold generator) throws Exception {
		return onServer(() -> {
			Field world = MapGenBase.class.getDeclaredField("worldObj");
			world.setAccessible(true);
			world.set(generator, world());
			Method check = MapGenStronghold.class.getDeclaredMethod("canSpawnStructureAtCoords", int.class, int.class);
			check.setAccessible(true);
			check.invoke(generator, 0, 0);
			Field coords = MapGenStronghold.class.getDeclaredField("structureCoords");
			coords.setAccessible(true);
			return (ChunkCoordIntPair[]) coords.get(generator);
		});
	}
}
