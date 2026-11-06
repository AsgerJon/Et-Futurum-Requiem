package io.github.asgerjon.mcfly.tree;

import java.util.Random;

/** A number drawn from min to max, both included (SPEC-modern-movement.md §90.5), 26.3's `constant` and `uniform` int providers. */
public final class IntRange {

	private final int min;
	private final int max;

	public IntRange(int min, int max) {
		this.min = min;
		this.max = max;
	}

	public static IntRange of(int value) {
		return new IntRange(value, value);
	}

	public int sample(Random random) {
		return min == max ? min : min + random.nextInt(max - min + 1);
	}
}
