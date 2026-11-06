package io.github.asgerjon.mcfly.tree;

import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

/** How a tree's trunk goes (SPEC-modern-movement.md §90.5), 26.3's `TrunkPlacer`: its height, the block under it, its logs. */
public abstract class TrunkPlacer {

	/** 26.3's horizontal directions in `Plane.HORIZONTAL`'s order: north, east, south, west (x and z steps). */
	protected static final int[][] HORIZONTAL = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

	private final int baseHeight;
	private final int heightRandA;
	private final int heightRandB;

	protected TrunkPlacer(int baseHeight, int heightRandA, int heightRandB) {
		this.baseHeight = baseHeight;
		this.heightRandA = heightRandA;
		this.heightRandB = heightRandB;
	}

	/** The trunk's logs from its base at (x, y, z), and where foliage goes. */
	public abstract List<FoliagePlacer.Attachment> placeTrunk(World world, Random random, int treeHeight, int x, int y, int z, Tree tree);

	public int treeHeight(Random random) {
		return baseHeight + random.nextInt(heightRandA + 1) + random.nextInt(heightRandB + 1);
	}

	/** The tree's below-trunk block, put under the trunk whatever is there (26.3's `placeBelowTrunkBlock`). */
	protected static void placeBelowTrunk(World world, Random random, int x, int y, int z, Tree tree) {
		if (tree.belowTrunk != null && !(tree.belowTrunk instanceof BlockProvider.Ruled ruled && !ruled.applies(world, x, y, z))) {
			tree.setLog(world, x, y, z, tree.belowTrunk.get(random));
		}
	}

	/** Where this trunk may put a log (26.3's `validTreePos`; a placer may let it grow through more). */
	protected boolean validTreePos(World world, int x, int y, int z) {
		return Tree.validTreePos(world, x, y, z);
	}

	/** A log where the tree may go. */
	protected boolean placeLog(World world, Random random, int x, int y, int z, Tree tree) {
		return placeLog(world, random, x, y, z, tree, 0);
	}

	/** A log where the tree may go, its axis bits (4 along x, 8 along z: 26.3's `trySetValue(AXIS, ...)`) on its meta. */
	protected boolean placeLog(World world, Random random, int x, int y, int z, Tree tree, int axis) {
		if (!validTreePos(world, x, y, z)) {
			return false;
		}
		BlockProvider.State state = tree.trunk.get(random);
		tree.setLog(world, x, y, z, axis == 0 ? state : new BlockProvider.State(state.block, state.meta & 3 | axis));
		return true;
	}

	/** Free for the tree: where it may go, or a log already. */
	public boolean isFree(World world, int x, int y, int z) {
		return validTreePos(world, x, y, z) || y >= 0 && y < world.getHeight() && Tree.isLog(world, x, y, z);
	}

	/** 1.7.10's log axis bits for a horizontal direction (`HORIZONTAL`'s index): north and south along z, east and west along x. */
	protected static int axisOf(int direction) {
		return direction % 2 == 0 ? 8 : 4;
	}
}
