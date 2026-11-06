package io.github.asgerjon.mcfly.copper;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.particle.McFlyParticles;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Random;

/**
 * 26.3's weathering copper (SPEC-modern-movement.md §87): its ageing (`ChangeOverTimeBlock.changeOverTime` and
 * `getNextState`), honeycomb's wax (`HoneycombItem`), the axe's wax off and scrape (`BlockTransformers`), and their
 * particles (level events 3003, 3004, 3005: `ParticleUtils.spawnParticlesOnBlockFaces`, 3 to 5 a face).
 */
public final class Weathering {

	/** The stages' prefixes in 26.3's names: unaffected, exposed, weathered, oxidized. */
	public static final String[] STAGES = {"", "exposed_", "weathered_", "oxidized_"};
	/** 26.3's colours by stage (orange, light grey terracotta, warped stem, warped nylium), 1.7.10's nearest. */
	private static final MapColor[] COLOURS = {MapColor.adobeColor, MapColor.silverColor, MapColor.cyanColor, MapColor.diamondColor};
	/** 26.3's `eachBlockOncePerDayChance`. */
	public static final float DAILY = 0.05688889F;
	private static final int SCAN = 4;

	public static final String WAX_ON = "minecraft:wax_on";
	public static final String WAX_OFF = "minecraft:wax_off";
	public static final String SCRAPE = "minecraft:scrape";

	private Weathering() {
	}

	public static MapColor colour(int stage) {
		return COLOURS[Math.max(0, Math.min(3, stage))];
	}

	/** The name 26.3 gives the kind at a stage, waxed or not ("cut_copper" at 1 waxed: "waxed_exposed_cut_copper"). */
	public static String name(String kind, int stage, boolean waxed) {
		return (waxed ? "waxed_" : "") + STAGES[stage] + kind;
	}

	/**
	 * A random tick at weathering copper: a chance of 26.3's daily one, then within 4 (Manhattan, not itself) of
	 * weathering copper of any kind and not waxed, one younger and nothing; else ((older + 1) / (older + same + 1))^2,
	 * times 0.75 unaffected, to the next stage.
	 */
	public static void randomTick(WeatheringCopper copper, World world, int x, int y, int z, Random random) {
		if (random.nextFloat() >= DAILY) {
			return;
		}
		int own = copper.stage(world, x, y, z);
		if (own < 0 || own >= 3 || copper.waxed(world, x, y, z) || !copper.agesHere(world, x, y, z)) {
			return;
		}
		int same = 0;
		int older = 0;
		for (int dx = -SCAN; dx <= SCAN; dx++) {
			for (int dy = -SCAN; dy <= SCAN; dy++) {
				for (int dz = -SCAN; dz <= SCAN; dz++) {
					int distance = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
					int ny = y + dy;
					if (distance == 0 || distance > SCAN || ny < 0 || ny >= world.getHeight() || !world.blockExists(x + dx, ny, z + dz)) {
						continue;
					}
					Block block = world.getBlock(x + dx, ny, z + dz);
					if (block instanceof WeatheringCopper other && !other.waxed(world, x + dx, ny, z + dz)) {
						int age = other.stage(world, x + dx, ny, z + dz);
						if (age < 0) {
							continue;
						}
						if (age < own) {
							return;
						}
						if (age > own) {
							older++;
						} else {
							same++;
						}
					}
				}
			}
		}
		float chance = (older + 1.0F) / (older + same + 1.0F);
		float actual = chance * chance * (own == 0 ? 0.75F : 1.0F);
		if (random.nextFloat() < actual) {
			copper.become(world, x, y, z, own + 1, false);
		}
	}

	/** Honeycomb's wax on unwaxed copper (26.3's `HoneycombItem.useOn`): whether it took. */
	public static boolean wax(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		if (!(block instanceof WeatheringCopper copper) || copper.waxed(world, x, y, z) || copper.stage(world, x, y, z) < 0) {
			return false;
		}
		if (!world.isRemote) {
			copper.become(world, x, y, z, copper.stage(world, x, y, z), true);
			world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, McFly.MC_ASSETS + ":item.honeycomb.wax_on", 1.0F, 1.0F);
			particles(world, x, y, z, WAX_ON);
		}
		return true;
	}

	/**
	 * An axe on copper (26.3's `BlockTransformers`): waxed, the wax off; weathered, a stage back. Whether it did either
	 * (the axe is the caller's to damage).
	 */
	public static boolean axe(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		if (!(block instanceof WeatheringCopper copper)) {
			return false;
		}
		int stage = copper.stage(world, x, y, z);
		if (stage < 0) {
			return false;
		}
		boolean waxed = copper.waxed(world, x, y, z);
		if (!waxed && stage == 0) {
			return false;
		}
		if (!world.isRemote) {
			copper.become(world, x, y, z, waxed ? stage : stage - 1, false);
			world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, McFly.MC_ASSETS + (waxed ? ":item.axe.wax_off" : ":item.axe.scrape"), 1.0F, 1.0F);
			particles(world, x, y, z, waxed ? WAX_OFF : SCRAPE);
		}
		return true;
	}

	/** 26.3's `spawnParticlesOnBlockFaces`: on each face 3 to 5, 0.55 out from the middle, a random speed along the face. */
	public static void particles(World world, int x, int y, int z, String particle) {
		if (!(world instanceof WorldServer server)) {
			return;
		}
		Random random = world.rand;
		int[][] faces = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
		for (int[] face : faces) {
			int count = 3 + random.nextInt(3);
			for (int i = 0; i < count; i++) {
				double px = x + 0.5 + (face[0] == 0 ? random.nextDouble() - 0.5 : face[0] * 0.55);
				double py = y + 0.5 + (face[1] == 0 ? random.nextDouble() - 0.5 : face[1] * 0.55);
				double pz = z + 0.5 + (face[2] == 0 ? random.nextDouble() - 0.5 : face[2] * 0.55);
				double xd = face[0] == 0 ? random.nextDouble() - 0.5 : 0.0;
				double yd = face[1] == 0 ? random.nextDouble() - 0.5 : 0.0;
				double zd = face[2] == 0 ? random.nextDouble() - 0.5 : 0.0;
				// a count of 0: one particle, the offsets its speed (26.3's packet's way)
				McFlyParticles.send(server, particle, px, py, pz, 0, xd, yd, zd, 1.0);
			}
		}
	}
}
