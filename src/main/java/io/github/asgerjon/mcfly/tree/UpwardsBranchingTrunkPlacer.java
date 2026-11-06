package io.github.asgerjon.mcfly.tree;

import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The mangrove's trunk (SPEC-modern-movement.md §90.6), 26.3's `UpwardsBranchingTrunkPlacer`: logs straight up; at each
 * placed log but the top, at its chance, a branch a random way, stepping out and up a log a step, foliage at each; foliage
 * over the top. It grows through what its can-grow-through blocks say (mud, mangrove roots and leaves, moss carpets, ...).
 */
public final class UpwardsBranchingTrunkPlacer extends TrunkPlacer {

	/** 26.3's `canGrowThrough`, as 1.7.10 blocks at a place. */
	public interface GrowsThrough {
		boolean test(World world, int x, int y, int z);
	}

	private final IntRange extraBranchSteps;
	private final float placeBranchPerLogProbability;
	private final IntRange extraBranchLength;
	private final GrowsThrough canGrowThrough;

	public UpwardsBranchingTrunkPlacer(int baseHeight, int heightRandA, int heightRandB, IntRange extraBranchSteps, float placeBranchPerLogProbability,
		IntRange extraBranchLength, GrowsThrough canGrowThrough) {
		super(baseHeight, heightRandA, heightRandB);
		this.extraBranchSteps = extraBranchSteps;
		this.placeBranchPerLogProbability = placeBranchPerLogProbability;
		this.extraBranchLength = extraBranchLength;
		this.canGrowThrough = canGrowThrough;
	}

	@Override
	public List<FoliagePlacer.Attachment> placeTrunk(World world, Random random, int treeHeight, int x, int y, int z, Tree tree) {
		List<FoliagePlacer.Attachment> attachments = new ArrayList<>();
		for (int heightPos = 0; heightPos < treeHeight; heightPos++) {
			int currentHeight = y + heightPos;
			if (placeLog(world, random, x, currentHeight, z, tree) && heightPos < treeHeight - 1 && random.nextFloat() < placeBranchPerLogProbability) {
				int[] branchDir = HORIZONTAL[random.nextInt(4)];
				int branchLen = extraBranchLength.sample(random);
				int branchPos = Math.max(0, branchLen - extraBranchLength.sample(random) - 1);
				int branchSteps = extraBranchSteps.sample(random);
				placeBranch(world, random, treeHeight, tree, attachments, x, z, currentHeight, branchDir, branchPos, branchSteps);
			}
			if (heightPos == treeHeight - 1) {
				attachments.add(new FoliagePlacer.Attachment(x, currentHeight + 1, z, 0, false));
			}
		}
		return attachments;
	}

	private void placeBranch(World world, Random random, int treeHeight, Tree tree, List<FoliagePlacer.Attachment> attachments, int x, int z, int currentHeight,
		int[] branchDir, int branchPos, int branchSteps) {
		int heightAlongBranch = currentHeight + branchPos;
		int logX = x;
		int logZ = z;
		for (int index = branchPos; index < treeHeight && branchSteps > 0; index++, branchSteps--) {
			if (index >= 1) {
				int placementHeight = currentHeight + index;
				logX += branchDir[0];
				logZ += branchDir[1];
				heightAlongBranch = placementHeight;
				if (placeLog(world, random, logX, placementHeight, logZ, tree)) {
					heightAlongBranch++;
				}
				attachments.add(new FoliagePlacer.Attachment(logX, placementHeight, logZ, 0, false));
			}
		}
		if (heightAlongBranch - currentHeight > 1) {
			attachments.add(new FoliagePlacer.Attachment(logX, heightAlongBranch, logZ, 0, false));
			attachments.add(new FoliagePlacer.Attachment(logX, heightAlongBranch - 2, logZ, 0, false));
		}
	}

	@Override
	protected boolean validTreePos(World world, int x, int y, int z) {
		return super.validTreePos(world, x, y, z) || y >= 0 && y < world.getHeight() && canGrowThrough.test(world, x, y, z);
	}
}
