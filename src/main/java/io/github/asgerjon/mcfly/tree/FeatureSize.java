package io.github.asgerjon.mcfly.tree;

/** How wide a tree's room must be at each height (SPEC-modern-movement.md §90.5), 26.3's `TwoLayersFeatureSize`. */
public final class FeatureSize {

	final int limit;
	final int lowerSize;
	final int upperSize;
	/** 26.3's `min_clipped_height`, or -1 for none. */
	final int minClippedHeight;

	public FeatureSize(int limit, int lowerSize, int upperSize) {
		this(limit, lowerSize, upperSize, -1);
	}

	public FeatureSize(int limit, int lowerSize, int upperSize, int minClippedHeight) {
		this.limit = limit;
		this.lowerSize = lowerSize;
		this.upperSize = upperSize;
		this.minClippedHeight = minClippedHeight;
	}

	int sizeAt(int treeHeight, int dy) {
		return dy < limit ? lowerSize : upperSize;
	}
}
