package io.github.asgerjon.mcfly.tree;

import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The cherry tree's trunk (SPEC-modern-movement.md §90.6), 26.3's `CherryTrunkPlacer`: one to three branches (a third
 * straight up the middle); a trunk up to the highest branch; the side branches out a random way and its opposite, two
 * sideways logs (one when the branch ends no higher), then stepping toward the branch's end, up or out by how far each
 * is left; foliage over each branch's end (and the middle's top).
 */
public final class CherryTrunkPlacer extends TrunkPlacer {

	private final IntRange branchCount;
	private final IntRange branchHorizontalLength;
	private final IntRange branchStartOffsetFromTop;
	private final IntRange secondBranchStartOffsetFromTop;
	private final IntRange branchEndOffsetFromTop;

	public CherryTrunkPlacer(int baseHeight, int heightRandA, int heightRandB, IntRange branchCount, IntRange branchHorizontalLength, int branchStartMin,
		int branchStartMax, IntRange branchEndOffsetFromTop) {
		super(baseHeight, heightRandA, heightRandB);
		this.branchCount = branchCount;
		this.branchHorizontalLength = branchHorizontalLength;
		this.branchStartOffsetFromTop = new IntRange(branchStartMin, branchStartMax);
		this.secondBranchStartOffsetFromTop = new IntRange(branchStartMin, branchStartMax - 1);
		this.branchEndOffsetFromTop = branchEndOffsetFromTop;
	}

	@Override
	public List<FoliagePlacer.Attachment> placeTrunk(World world, Random random, int treeHeight, int x, int y, int z, Tree tree) {
		placeBelowTrunk(world, random, x, y - 1, z, tree);
		int first = Math.max(0, treeHeight - 1 + branchStartOffsetFromTop.sample(random));
		int second = Math.max(0, treeHeight - 1 + secondBranchStartOffsetFromTop.sample(random));
		if (second >= first) {
			second++;
		}
		int branches = branchCount.sample(random);
		boolean middle = branches == 3;
		boolean bothSides = branches >= 2;
		int trunkHeight = middle ? treeHeight : bothSides ? Math.max(first, second) + 1 : first + 1;
		for (int i = 0; i < trunkHeight; i++) {
			placeLog(world, random, x, y + i, z, tree);
		}
		List<FoliagePlacer.Attachment> attachments = new ArrayList<>();
		if (middle) {
			attachments.add(new FoliagePlacer.Attachment(x, y + trunkHeight, z, 0, false));
		}
		int direction = random.nextInt(4);
		attachments.add(branch(world, random, treeHeight, x, y, z, tree, direction, first, first < trunkHeight - 1));
		if (bothSides) {
			attachments.add(branch(world, random, treeHeight, x, y, z, tree, (direction + 2) % 4, second, second < trunkHeight - 1));
		}
		return attachments;
	}

	private FoliagePlacer.Attachment branch(World world, Random random, int treeHeight, int x, int y, int z, Tree tree, int direction, int offsetFromOrigin,
		boolean middleContinuesUpwards) {
		int[] step = HORIZONTAL[direction];
		int axis = axisOf(direction);
		int lx = x, ly = y + offsetFromOrigin, lz = z;
		int endOffset = treeHeight - 1 + branchEndOffsetFromTop.sample(random);
		boolean awayFromTrunk = middleContinuesUpwards || endOffset < offsetFromOrigin;
		int distanceToTrunk = branchHorizontalLength.sample(random) + (awayFromTrunk ? 1 : 0);
		int ex = x + step[0] * distanceToTrunk, ey = y + endOffset, ez = z + step[1] * distanceToTrunk;
		for (int i = 0; i < (awayFromTrunk ? 2 : 1); i++) {
			lx += step[0];
			lz += step[1];
			placeLog(world, random, lx, ly, lz, tree, axis);
		}
		int vertical = ey > ly ? 1 : -1;
		while (true) {
			int distance = Math.abs(ex - lx) + Math.abs(ey - ly) + Math.abs(ez - lz);
			if (distance == 0) {
				return new FoliagePlacer.Attachment(ex, ey + 1, ez, 0, false);
			}
			float chanceToGrowVertically = (float) Math.abs(ey - ly) / distance;
			boolean growVertically = random.nextFloat() < chanceToGrowVertically;
			if (growVertically) {
				ly += vertical;
			} else {
				lx += step[0];
				lz += step[1];
			}
			placeLog(world, random, lx, ly, lz, tree, growVertically ? 0 : axis);
		}
	}
}
