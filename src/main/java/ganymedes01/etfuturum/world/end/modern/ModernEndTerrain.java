package ganymedes01.etfuturum.world.end.modern;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * The End's terrain as 26.3 generates it (SPEC-modern-movement.md §76.5), block for block at the same seed: the main
 * island and the outer islands past 1024 blocks, shaped by the old blended noise, computed at the corners of 8 by 4 by
 * 8 cells and interpolated, in 26.3's float arithmetic. One per seed (its noise is made once).
 */
public final class ModernEndTerrain {

	public static final int HEIGHT = 128;
	private static final int CELL_XZ = 8;
	private static final int CELL_Y = 4;
	private static final int CORNERS_Y = HEIGHT / CELL_Y + 1;

	/** 684.412 times the End's xz and y scale (0.25). */
	private static final double XZ_MULTIPLIER = 684.412 * 0.25;
	private static final double Y_MULTIPLIER = 684.412 * 0.25;
	private static final double XZ_FACTOR = 80.0;
	private static final double Y_FACTOR = 160.0;
	private static final double SMEAR = 4.0;

	private static final Map<Long, ModernEndTerrain> BY_SEED = new LinkedHashMap<Long, ModernEndTerrain>(4, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Long, ModernEndTerrain> eldest) {
			return size() > 4;
		}
	};

	private final Octaves minLimit;
	private final Octaves maxLimit;
	private final Octaves main;
	private final Simplex islandNoise;

	private ModernEndTerrain(long seed) {
		Random random = new Random(seed);
		double limitSmear = Y_MULTIPLIER * SMEAR;
		minLimit = new Octaves(random, 16, limitSmear, 0.99998474F);
		maxLimit = new Octaves(random, 16, limitSmear, 0.99998474F);
		main = new Octaves(random, 8, limitSmear / Y_FACTOR, 12.75);
		// the islands' own: the same seed, past the draws 1.9's End made for its five noises (66 layers of 262)
		Random islands = new Random(seed);
		for (int i = 0; i < 17292; i++) {
			islands.nextInt();
		}
		islandNoise = new Simplex(islands);
	}

	public static ModernEndTerrain forSeed(long seed) {
		synchronized (BY_SEED) {
			return BY_SEED.computeIfAbsent(seed, ModernEndTerrain::new);
		}
	}

	/** Where a block sits in a chunk's array: 1.7.10's chunk layout, (x * 16 + z) * 128 + y. */
	public static int index(int x, int y, int z) {
		return (x << 4 | z) * HEIGHT + y;
	}

	/** The chunk's blocks: true where 26.3 puts end stone. */
	public boolean[] fillChunk(int cx, int cz) {
		int baseX = cx << 4;
		int baseZ = cz << 4;
		float[][][] corners = new float[3][CORNERS_Y][3];
		for (int ix = 0; ix < 3; ix++) {
			for (int iz = 0; iz < 3; iz++) {
				int x = baseX + ix * CELL_XZ;
				int z = baseZ + iz * CELL_XZ;
				float islands = islands(x, z);
				for (int iy = 0; iy < CORNERS_Y; iy++) {
					corners[ix][iy][iz] = cornerDensity(x, iy * CELL_Y, z, islands);
				}
			}
		}
		boolean[] solid = new boolean[16 * 16 * HEIGHT];
		for (int cellX = 0; cellX < 2; cellX++) {
			for (int cellZ = 0; cellZ < 2; cellZ++) {
				for (int cellY = 0; cellY < CORNERS_Y - 1; cellY++) {
					fillCell(solid, corners, cellX, cellY, cellZ);
				}
			}
		}
		return solid;
	}

	/** One cell: z first, then x, then up from the bottom by a step a block (accumulated), squeezed, solid above 0. */
	private static void fillCell(boolean[] solid, float[][][] c, int cellX, int cellY, int cellZ) {
		float v000 = c[cellX][cellY][cellZ];
		float v100 = c[cellX + 1][cellY][cellZ];
		float v001 = c[cellX][cellY][cellZ + 1];
		float v101 = c[cellX + 1][cellY][cellZ + 1];
		float v010 = c[cellX][cellY + 1][cellZ];
		float v110 = c[cellX + 1][cellY + 1][cellZ];
		float v011 = c[cellX][cellY + 1][cellZ + 1];
		float v111 = c[cellX + 1][cellY + 1][cellZ + 1];
		float inverseXz = 1.0F / CELL_XZ;
		float inverseY = 1.0F / CELL_Y;
		for (int z = 0; z < CELL_XZ; z++) {
			float alphaZ = z * inverseXz;
			float v00 = lerp(alphaZ, v000, v001);
			float v01 = lerp(alphaZ, v010, v011);
			float v10 = lerp(alphaZ, v100, v101);
			float v11 = lerp(alphaZ, v110, v111);
			for (int x = 0; x < CELL_XZ; x++) {
				float alphaX = x * inverseXz;
				float bottom = lerp(alphaX, v00, v10);
				float top = lerp(alphaX, v01, v11);
				float step = (top - bottom) * inverseY;
				float value = bottom + step * 0;
				for (int y = 0; y < CELL_Y; y++) {
					if (squeeze(value) + 0.0F > 0.0F) {
						solid[index(cellX * CELL_XZ + x, cellY * CELL_Y + y, cellZ * CELL_XZ + z)] = true;
					}
					value += step;
				}
			}
		}
	}

	private static float squeeze(float value) {
		float c = Math.max(-1.0F, Math.min(1.0F, value));
		return c / 2.0F - c * c * c / 24.0F;
	}

	/** The density at a cell's corner, before interpolation: the slides at the bottom and top, times 0.64. */
	private float cornerDensity(int x, int y, int z, float islands) {
		float bottomSlide = gradient(y, 4, 32, 0.0F, 1.0F);
		float topSlide = gradient(y, 56, 312, 1.0F, 0.0F);
		float shaped = lerpShortcut(topSlide, -23.4375F, islands + blendedNoise(x, y, z));
		return lerpShortcut(bottomSlide, -0.234375F, shaped) * 0.64F;
	}

	/** A y gradient as 26.3's: from the from-value at one height to the to-value at the other, clamped. */
	private static float gradient(int y, int from, int to, float fromValue, float toValue) {
		float factor = (toValue - fromValue) / (to - from);
		int relative = Math.max(Math.min(from, to), Math.min(Math.max(from, to), y)) - from;
		return fromValue + relative * factor;
	}

	/** 26.3's lerp step: a weight of exactly 0 or 1 takes that end as it is. */
	private static float lerpShortcut(float alpha, float first, float second) {
		if (alpha == 0.0F) {
			return first;
		}
		return alpha == 1.0F ? second : lerp(alpha, first, second);
	}

	static float lerp(float alpha, float from, float to) {
		return from + alpha * (to - from);
	}

	/** The old blended noise: the min and max limit noises, chosen between by the main noise. */
	private float blendedNoise(int x, int y, int z) {
		float min = minLimit.sample(x, y, z, XZ_MULTIPLIER, Y_MULTIPLIER);
		float max = maxLimit.sample(x, y, z, XZ_MULTIPLIER, Y_MULTIPLIER);
		float choice = main.sample(x, y, z, XZ_MULTIPLIER / XZ_FACTOR, Y_MULTIPLIER / Y_FACTOR) + 0.5F;
		choice = Math.max(0.0F, Math.min(1.0F, choice));
		return lerpShortcut(choice, min, max);
	}

	/** The main island and the outer islands, the larger (also what the biomes go by). */
	public float islands(int x, int z) {
		float dx = -x;
		float dz = -z;
		float distance = (float) Math.sqrt(dx * dx + 0.0F * 0.0F + dz * dz);
		float mainIsland = (Math.max(-100.0F, Math.min(80.0F, 100.0F - distance)) + -8.0F) * 0.0078125F;
		float outer = (outerIslands(x / 8, z / 8) - 8.0F) / 128.0F;
		return Math.max(mainIsland, outer);
	}

	/** 26.3's outer islands at a section (8 blocks): the strongest island among the 25 by 25 chunks around. */
	private float outerIslands(int sectionX, int sectionZ) {
		int chunkX = sectionX / 2;
		int chunkZ = sectionZ / 2;
		int subX = sectionX % 2;
		int subZ = sectionZ % 2;
		float value = -100.0F;
		for (int xo = -12; xo <= 12; xo++) {
			for (int zo = -12; zo <= 12; zo++) {
				long islandX = chunkX + xo;
				long islandZ = chunkZ + zo;
				if (islandX * islandX + islandZ * islandZ > 4096L && islandNoise.get(islandX, islandZ) < -0.9F) {
					float size = (Math.abs((float) islandX) * 3439.0F + Math.abs((float) islandZ) * 147.0F) % 13.0F + 9.0F;
					float xd = subX - xo * 2;
					float zd = subZ - zo * 2;
					float island = 100.0F - (float) Math.sqrt(xd * xd + zd * zd) * size;
					island = Math.max(-100.0F, Math.min(80.0F, island));
					value = Math.max(value, island);
				}
			}
		}
		return value;
	}

	// ---------------------------------------------------------------- the noise (26.3's legacy, in its float steps)

	/** A gradient noise layer: its offsets and permutation, made from the random as 26.3 (and 1.7.10) make them. */
	private static class Layer {

		/** The 12 edges of a cube, then four of them again. */
		static final int[][] GRADIENTS = {{1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0}, {1, 0, 1}, {-1, 0, 1}, {1, 0, -1},
			{-1, 0, -1}, {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}, {1, 1, 0}, {0, -1, 1}, {-1, 1, 0}, {0, -1, -1}};
		static final double HALF_ROUND = Math.nextDown(16777216.0);

		final byte[] perms = new byte[256];
		final double offsetX;
		final double offsetY;
		final double offsetZ;

		Layer(Random random, double offsetScale) {
			offsetX = random.nextDouble() * offsetScale;
			offsetY = random.nextDouble() * offsetScale;
			offsetZ = random.nextDouble() * offsetScale;
			for (int i = 0; i < 256; i++) {
				perms[i] = (byte) i;
			}
			for (int i = 0; i < 256; i++) {
				int swap = random.nextInt(256 - i);
				byte held = perms[i];
				perms[i] = perms[swap + i];
				perms[swap + i] = held;
			}
		}

		int permute(int value) {
			return perms[value & 0xFF] & 0xFF;
		}

		int[] gradient(int value) {
			return GRADIENTS[permute(value) & 15];
		}

		static double wrap(double value) {
			return value >= -HALF_ROUND && value < HALF_ROUND ? value : value - Math.floor(value / 3.3554432E7 + 0.5) * 3.3554432E7;
		}

		static int floor(double value) {
			int floored = (int) value;
			return value < floored ? floored - 1 : floored;
		}

		static float smoothstep(float t) {
			return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
		}
	}

	/** The old blended noise's layer: Perlin noise with y's fraction "smeared" by its scale. */
	private static final class SmearedLayer extends Layer {

		final double fudgeScale;

		SmearedLayer(Random random, double fudgeScale) {
			super(random, 256.0);
			this.fudgeScale = fudgeScale;
		}

		/** At (x, y, z) already scaled: as 26.3 samples a block volume (the corners its interpolation reads). */
		float sample(double scaledX, double scaledY, double scaledZ) {
			double z = wrap(scaledZ) + offsetZ;
			int floorZ = floor(z);
			float relZ = (float) (z - floorZ);
			float alphaZ = smoothstep(relZ);
			double x = wrap(scaledX) + offsetX;
			int floorX = floor(x);
			float relX = (float) (x - floorX);
			int x0 = permute(floorX);
			int x1 = permute(floorX + 1);
			float alphaX = smoothstep(relX);
			double y = wrap(scaledY) + offsetY;
			int floorY = floor(y);
			double relY = y - floorY;
			float alphaY = smoothstep((float) relY);
			int xy00 = permute(x0 + floorY);
			int xy01 = permute(x0 + floorY + 1);
			int xy10 = permute(x1 + floorY);
			int xy11 = permute(x1 + floorY + 1);
			float fudged = (float) (relY - fudge(scaledY, relY));
			float d000 = edge(gradient(xy00 + floorZ), relX, relZ, fudged);
			float d100 = edge(gradient(xy10 + floorZ), relX - 1.0F, relZ, fudged);
			float d010 = edge(gradient(xy01 + floorZ), relX, relZ, fudged - 1.0F);
			float d110 = edge(gradient(xy11 + floorZ), relX - 1.0F, relZ, fudged - 1.0F);
			float d001 = edge(gradient(xy00 + floorZ + 1), relX, relZ - 1.0F, fudged);
			float d101 = edge(gradient(xy10 + floorZ + 1), relX - 1.0F, relZ - 1.0F, fudged);
			float d011 = edge(gradient(xy01 + floorZ + 1), relX, relZ - 1.0F, fudged - 1.0F);
			float d111 = edge(gradient(xy11 + floorZ + 1), relX - 1.0F, relZ - 1.0F, fudged - 1.0F);
			float a = lerp(alphaX, d000, d100);
			float b = lerp(alphaX, d010, d110);
			float c = lerp(alphaX, d001, d101);
			float d = lerp(alphaX, d011, d111);
			return lerp(alphaZ, lerp(alphaY, a, b), lerp(alphaY, c, d));
		}

		/** The gradient's dot with the corner's offset: x and z first, then y (26.3's volume order). */
		private static float edge(int[] g, float x, float z, float y) {
			float xz = g[0] * x + g[2] * z;
			float gy = g[1];
			return xz + gy * y;
		}

		private double fudge(double originalY, double relY) {
			double limit = originalY >= 0.0 && originalY < relY ? originalY : relY;
			return floor(limit / fudgeScale + 1.0E-7F) * fudgeScale;
		}
	}

	/** A stack of smeared layers: layer k at frequency 2^-k, its amplitude doubling, summed in floats in order. */
	private static final class Octaves {

		final SmearedLayer[] layers;
		final double[] frequencies;
		final float[] amplitudes;

		Octaves(Random random, int count, double fudgeScale, double valueFactor) {
			layers = new SmearedLayer[count];
			frequencies = new double[count];
			amplitudes = new float[count];
			double frequency = 1.0;
			double value = valueFactor / (Math.pow(2.0, count) - 1.0);
			for (int i = 0; i < count; i++) {
				layers[i] = new SmearedLayer(random, fudgeScale * frequency);
				frequencies[i] = frequency;
				amplitudes[i] = (float) value;
				frequency /= 2.0;
				value *= 2.0;
			}
		}

		float sample(int x, int y, int z, double xzScale, double yScale) {
			float sum = 0.0F;
			for (int i = 0; i < layers.length; i++) {
				double layerXz = xzScale * frequencies[i];
				double layerY = yScale * frequencies[i];
				sum += amplitudes[i] * layers[i].sample(x * layerXz, y * layerY, z * layerXz);
			}
			return sum;
		}
	}

	/** 26.3's 2D simplex noise for the islands (its offsets drawn but scaled to 0). */
	private static final class Simplex extends Layer {

		static final double SQRT_3 = Math.sqrt(3.0);
		static final double F2 = 0.5 * (SQRT_3 - 1.0);
		static final double G2 = (3.0 - SQRT_3) / 6.0;

		Simplex(Random random) {
			super(random, 0.0);
		}

		float get(double xIn, double yIn) {
			double xin = xIn + offsetX;
			double yin = yIn + offsetY;
			double s = (xin + yin) * F2;
			int i = floor(xin + s);
			int j = floor(yin + s);
			double t = (i + j) * G2;
			double x0 = xin - (i - t);
			double y0 = yin - (j - t);
			int i1 = x0 > y0 ? 1 : 0;
			int j1 = x0 > y0 ? 0 : 1;
			double x1 = x0 - i1 + G2;
			double y1 = y0 - j1 + G2;
			double x2 = x0 - 1.0 + 2.0 * G2;
			double y2 = y0 - 1.0 + 2.0 * G2;
			int ii = i & 0xFF;
			int jj = j & 0xFF;
			int g0 = permute(ii + permute(jj)) % 12;
			int g1 = permute(ii + i1 + permute(jj + j1)) % 12;
			int g2 = permute(ii + 1 + permute(jj + 1)) % 12;
			return (float) (70.0 * (corner(g0, x0, y0) + corner(g1, x1, y1) + corner(g2, x2, y2)));
		}

		private static double corner(int index, double x, double y) {
			double t = 0.5 - x * x - y * y - 0.0 * 0.0;
			if (t < 0.0) {
				return 0.0;
			}
			t *= t;
			int[] g = GRADIENTS[index];
			return t * t * (g[0] * x + g[1] * y + g[2] * 0.0);
		}
	}
}
