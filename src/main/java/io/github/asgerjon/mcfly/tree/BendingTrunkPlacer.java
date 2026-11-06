package io.github.asgerjon.mcfly.tree;

import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The azalea's trunk (SPEC-modern-movement.md §90.5), 26.3's `BendingTrunkPlacer`: a random horizontal direction; logs
 * up, stepping that way at the top one or two, foliage at each from `minHeightForLeaves` up; then a bend of more logs
 * that way, foliage at each.
 */
public final class BendingTrunkPlacer extends TrunkPlacer {

	/** 26.3's horizontal directions in `Plane.HORIZONTAL`'s order: north, east, south, west. */
	private static final int[][] HORIZONTAL = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
	private final int minHeightForLeaves;
	private final IntRange bendLength;

	public BendingTrunkPlacer(int baseHeight, int heightRandA, int heightRandB, int minHeightForLeaves, IntRange bendLength) {
		super(baseHeight, heightRandA, heightRandB);
		this.minHeightForLeaves = minHeightForLeaves;
		this.bendLength = bendLength;
	}

	@Override
	public List<FoliagePlacer.Attachment> placeTrunk(World world, Random random, int treeHeight, int x, int y, int z, Tree tree) {
		int[] direction = HORIZONTAL[random.nextInt(4)];
		int logHeight = treeHeight - 1;
		placeBelowTrunk(world, random, x, y - 1, z, tree);
		List<FoliagePlacer.Attachment> foliage = new ArrayList<>();
		int px = x, py = y, pz = z;
		for (int i = 0; i <= logHeight; i++) {
			if (i + 1 >= logHeight + random.nextInt(2)) {
				px += direction[0];
				pz += direction[1];
			}
			if (Tree.validTreePos(world, px, py, pz)) {
				placeLog(world, random, px, py, pz, tree);
			}
			if (i >= minHeightForLeaves) {
				foliage.add(new FoliagePlacer.Attachment(px, py, pz, 0, false));
			}
			py++;
		}
		int bend = bendLength.sample(random);
		for (int i = 0; i <= bend; i++) {
			if (Tree.validTreePos(world, px, py, pz)) {
				placeLog(world, random, px, py, pz, tree);
			}
			foliage.add(new FoliagePlacer.Attachment(px, py, pz, 0, false));
			px += direction[0];
			pz += direction[1];
		}
		return foliage;
	}
}
