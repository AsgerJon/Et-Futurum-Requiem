package io.github.asgerjon.mcfly.tree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Blocks hung from a tree's leaves (SPEC-modern-movement.md §90.6), 26.3's `AttachedToLeavesDecorator` (mangrove's
 * hanging propagules): the leaves in a shuffled order, each a random one of its ways, at its chance, where that many
 * blocks that way are air and no other was put within the exclusion box.
 */
public final class AttachedToLeavesDecorator extends TreeDecorator {

	private final float probability;
	private final int exclusionRadiusXZ;
	private final int exclusionRadiusY;
	private final BlockProvider blockProvider;
	private final int requiredEmptyBlocks;
	/** The ways, as steps (26.3's directions: mangrove's down only). */
	private final int[][] directions;

	public AttachedToLeavesDecorator(float probability, int exclusionRadiusXZ, int exclusionRadiusY, BlockProvider blockProvider, int requiredEmptyBlocks,
		int[][] directions) {
		this.probability = probability;
		this.exclusionRadiusXZ = exclusionRadiusXZ;
		this.exclusionRadiusY = exclusionRadiusY;
		this.blockProvider = blockProvider;
		this.requiredEmptyBlocks = requiredEmptyBlocks;
		this.directions = directions;
	}

	@Override
	public void place(Context context) {
		Set<Long> blacklist = new HashSet<>();
		// 26.3's `Util.shuffledCopy`: Fisher-Yates from the end
		List<int[]> leaves = new ArrayList<>(context.leaves);
		for (int i = leaves.size(); i > 1; i--) {
			int j = context.random.nextInt(i);
			int[] swap = leaves.get(i - 1);
			leaves.set(i - 1, leaves.get(j));
			leaves.set(j, swap);
		}
		for (int[] leaf : leaves) {
			int[] step = directions[context.random.nextInt(directions.length)];
			int px = leaf[0] + step[0], py = leaf[1] + step[1], pz = leaf[2] + step[2];
			if (!blacklist.contains(Tree.key(px, py, pz)) && context.random.nextFloat() < probability && emptyAhead(context, leaf, step)) {
				for (int dx = -exclusionRadiusXZ; dx <= exclusionRadiusXZ; dx++) {
					for (int dy = -exclusionRadiusY; dy <= exclusionRadiusY; dy++) {
						for (int dz = -exclusionRadiusXZ; dz <= exclusionRadiusXZ; dz++) {
							blacklist.add(Tree.key(px + dx, py + dy, pz + dz));
						}
					}
				}
				BlockProvider.State state = blockProvider.get(context.random);
				context.setBlock(px, py, pz, state.block, state.meta);
			}
		}
	}

	private boolean emptyAhead(Context context, int[] leaf, int[] step) {
		for (int i = 1; i <= requiredEmptyBlocks; i++) {
			if (!context.isAir(leaf[0] + step[0] * i, leaf[1] + step[1] * i, leaf[2] + step[2] * i)) {
				return false;
			}
		}
		return true;
	}
}
