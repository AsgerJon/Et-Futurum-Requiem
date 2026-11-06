package io.github.asgerjon.mcfly.tree;

import net.minecraft.world.World;

import java.util.Random;

/**
 * How a tree's roots go (SPEC-modern-movement.md §90.6), 26.3's `RootPlacer`: the trunk starts its offset above the
 * sapling's place; a root where the tree may go, and on it, at the above-root chance and into air, the above-root block
 * (mangrove's moss carpet).
 */
public abstract class RootPlacer {

	private final IntRange trunkOffsetY;
	protected final BlockProvider rootProvider;
	private final BlockProvider aboveRootProvider;
	private final float aboveRootChance;

	protected RootPlacer(IntRange trunkOffsetY, BlockProvider rootProvider, BlockProvider aboveRootProvider, float aboveRootChance) {
		this.trunkOffsetY = trunkOffsetY;
		this.rootProvider = rootProvider;
		this.aboveRootProvider = aboveRootProvider;
		this.aboveRootChance = aboveRootChance;
	}

	/** 26.3's `getTrunkOrigin`: the trunk's first block's height. */
	int trunkOriginY(int y, Random random) {
		return y + trunkOffsetY.sample(random);
	}

	/** The roots from the sapling's place (x, y, z) to the trunk's start at trunkY; false (nothing placed) when they don't fit. */
	public abstract boolean placeRoots(World world, Random random, int x, int y, int z, int trunkY, Tree tree);

	protected boolean canPlaceRoot(World world, int x, int y, int z) {
		return Tree.validTreePos(world, x, y, z);
	}

	protected void placeRoot(World world, Random random, int x, int y, int z, Tree tree) {
		if (!canPlaceRoot(world, x, y, z)) {
			return;
		}
		tree.setRoot(world, x, y, z, rootProvider.get(random));
		if (aboveRootProvider != null && random.nextFloat() < aboveRootChance && y + 1 < world.getHeight() && world.isAirBlock(x, y + 1, z)) {
			tree.setRoot(world, x, y + 1, z, aboveRootProvider.get(random));
		}
	}
}
