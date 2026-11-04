package ganymedes01.etfuturum.world.end.modern;

import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The main island's ten obsidian pillars as 26.3's (SPEC-modern-movement.md §76.6, `EndSpikeFeature`): on a circle of
 * radius 42, their sizes shuffled by the seed, two of them caged, each with an end crystal on bedrock at its top.
 */
public final class EndSpikes {

	private static final Map<Long, List<Spike>> BY_KEY = new LinkedHashMap<Long, List<Spike>>(4, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Long, List<Spike>> eldest) {
			return size() > 4;
		}
	};

	private EndSpikes() {
	}

	public static final class Spike {
		public final int x;
		public final int z;
		public final int radius;
		public final int height;
		public final boolean guarded;

		public Spike(int x, int z, int radius, int height, boolean guarded) {
			this.x = x;
			this.z = z;
			this.radius = radius;
			this.height = height;
			this.guarded = guarded;
		}

		/** Its column from the bottom of the world to the top (where the fight counts its crystal). */
		public AxisAlignedBB column() {
			return AxisAlignedBB.getBoundingBox(x - radius, -64.0, z - radius, x + radius, 320.0, z + radius);
		}

		/** Whether its middle is in the area that chunk's population decorates (16 from its corner plus 8). */
		boolean decoratedBy(int cx, int cz) {
			int fromX = (cx << 4) + 8;
			int fromZ = (cz << 4) + 8;
			return x >= fromX && x < fromX + 16 && z >= fromZ && z < fromZ + 16;
		}
	}

	/** The pillars for a seed, in 26.3's order (the order a respawn rebuilds them in). */
	public static List<Spike> forSeed(long seed) {
		long key = new Random(seed).nextLong() & 0xFFFFL;
		synchronized (BY_KEY) {
			return BY_KEY.computeIfAbsent(key, EndSpikes::make);
		}
	}

	private static List<Spike> make(long key) {
		int[] sizes = new int[10];
		for (int i = 0; i < 10; i++) {
			sizes[i] = i;
		}
		// 26.3's shuffle (Collections.shuffle's steps): from the last place down, swap with a random earlier one
		Random random = new Random(key);
		for (int i = sizes.length; i > 1; i--) {
			int swap = random.nextInt(i);
			int held = sizes[i - 1];
			sizes[i - 1] = sizes[swap];
			sizes[swap] = held;
		}
		List<Spike> spikes = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			int x = floor(42.0 * Math.cos(2.0 * (-Math.PI + Math.PI / 10 * i)));
			int z = floor(42.0 * Math.sin(2.0 * (-Math.PI + Math.PI / 10 * i)));
			int size = sizes[i];
			spikes.add(new Spike(x, z, 2 + size / 3, 76 + size * 3, size == 1 || size == 2));
		}
		return Collections.unmodifiableList(spikes);
	}

	private static int floor(double value) {
		int floored = (int) value;
		return value < floored ? floored - 1 : floored;
	}

	/** The pillars whose middles that chunk's population places (1.7.10's decorated area). */
	public static void decorate(World world, int cx, int cz, Random random) {
		for (Spike spike : forSeed(world.getSeed())) {
			if (spike.decoratedBy(cx, cz)) {
				place(world, spike, random, false, null);
			}
		}
	}

	/**
	 * One pillar: obsidian within radius^2 + 1 of its middle below its height, air above y 65 in the rest of its box,
	 * its cage, then its crystal on bedrock in fire (invulnerable and beaming when a respawn rebuilds it).
	 */
	public static void place(World world, Spike spike, Random random, boolean invulnerable, ChunkCoordinates beamTarget) {
		int r = spike.radius;
		for (int x = spike.x - r; x <= spike.x + r; x++) {
			for (int z = spike.z - r; z <= spike.z + r; z++) {
				int dx = x - spike.x;
				int dz = z - spike.z;
				boolean inside = dx * dx + dz * dz <= r * r + 1;
				for (int y = 0; y <= spike.height + 10; y++) {
					if (inside && y < spike.height) {
						world.setBlock(x, y, z, Blocks.obsidian, 0, 2);
					} else if (y > 65) {
						world.setBlock(x, y, z, Blocks.air, 0, 2);
					}
				}
			}
		}
		if (spike.guarded) {
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					for (int dy = 0; dy <= 3; dy++) {
						if (Math.abs(dx) == 2 || Math.abs(dz) == 2 || dy == 3) {
							world.setBlock(spike.x + dx, spike.height + dy, spike.z + dz, Blocks.iron_bars, 0, 2);
						}
					}
				}
			}
		}
		EntityEnderCrystal crystal = new EntityEnderCrystal(world);
		// 1.7.10 keeps an entity's y above its feet by its yOffset; set from the feet as 26.3 places it
		crystal.setLocationAndAngles(spike.x + 0.5, spike.height + 1, spike.z + 0.5, random.nextFloat() * 360.0F, 0.0F);
		EndCrystals.setBeamTarget(crystal, beamTarget);
		EndCrystals.setInvulnerable(crystal, invulnerable);
		world.spawnEntityInWorld(crystal);
		world.setBlock(spike.x, spike.height, spike.z, Blocks.bedrock, 0, 2);
		world.setBlock(spike.x, spike.height + 1, spike.z, Blocks.fire, 0, 2);
	}
}
