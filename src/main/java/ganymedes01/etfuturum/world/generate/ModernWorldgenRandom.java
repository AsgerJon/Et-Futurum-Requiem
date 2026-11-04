package ganymedes01.etfuturum.world.generate;

import java.util.Random;

/**
 * 26.3's feature random (`WorldgenRandom` over a `XoroshiroRandomSource`, what `ChunkGenerator.applyBiomeDecoration`
 * makes for every chunk, the End's too): the legacy random's methods (nextInt, nextFloat, nextLong as
 * java.util.Random's) on bits from Xoroshiro128++ (its top bits of nextLong), reseeded through 26.3's
 * `RandomSupport.upgradeSeedTo128bit`. With setDecorationSeed and setFeatureSeed, a chunk's features draw what 26.3's
 * do (SPEC-modern-movement.md §76.11).
 */
public class ModernWorldgenRandom extends Random {

	private static final long GOLDEN_RATIO_64 = -7046029254386353131L;
	private static final long SILVER_RATIO_64 = 7640891576956012809L;

	private long seedLo;
	private long seedHi;

	public ModernWorldgenRandom() {
		super(0L);
		setSeed(0L);
	}

	/** Xoroshiro reseeded (26.3's XoroshiroRandomSource.setSeed); Random's constructor calls it before this is made. */
	@Override
	public synchronized void setSeed(long seed) {
		long lo = seed ^ SILVER_RATIO_64;
		long hi = lo + GOLDEN_RATIO_64;
		seedLo = mixStafford13(lo);
		seedHi = mixStafford13(hi);
		if ((seedLo | seedHi) == 0L) {
			seedLo = GOLDEN_RATIO_64;
			seedHi = SILVER_RATIO_64;
		}
	}

	private static long mixStafford13(long z) {
		z = (z ^ z >>> 30) * -4658895280553007687L;
		z = (z ^ z >>> 27) * -7723592293110705685L;
		return z ^ z >>> 31;
	}

	/** Xoroshiro128++'s step. */
	private long nextXoroshiro() {
		long s0 = seedLo;
		long s1 = seedHi;
		long result = Long.rotateLeft(s0 + s1, 17) + s0;
		s1 ^= s0;
		seedLo = Long.rotateLeft(s0, 49) ^ s1 ^ s1 << 21;
		seedHi = Long.rotateLeft(s1, 28);
		return result;
	}

	/** 26.3's WorldgenRandom.next over a non-legacy source: the top bits of its nextLong. */
	@Override
	protected int next(int bits) {
		return (int) (nextXoroshiro() >>> 64 - bits);
	}

	/** 26.3's setDecorationSeed: a chunk's own seed, from the world's and its lowest block x and z. */
	public long setDecorationSeed(long seed, int blockX, int blockZ) {
		setSeed(seed);
		long xScale = nextLong() | 1L;
		long zScale = nextLong() | 1L;
		long result = blockX * xScale + blockZ * zScale ^ seed;
		setSeed(result);
		return result;
	}

	/** 26.3's setFeatureSeed: a feature's, from the chunk's, its index in the step's sorted features and the step. */
	public void setFeatureSeed(long decorationSeed, int index, int step) {
		setSeed(decorationSeed + index + 10000 * step);
	}
}
