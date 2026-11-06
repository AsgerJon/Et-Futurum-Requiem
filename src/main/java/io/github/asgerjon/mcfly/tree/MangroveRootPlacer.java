package io.github.asgerjon.mcfly.tree;

import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The mangrove's roots (SPEC-modern-movement.md §90.6), 26.3's `MangroveRootPlacer` and `MangroveRootPlacement`: the
 * column from the sapling's place to the trunk must be free; a root under the trunk and one each way beside it, and from
 * each of those a root grown down or on that way (skewing down near its width limit), each step where the tree may go or
 * through what roots grow through, no longer than its length limit (a root past it and the tree doesn't fit); roots in
 * mud or muddy roots are muddy roots.
 */
public final class MangroveRootPlacer extends RootPlacer {

	/** 26.3's `#mangrove_roots_can_grow_through` and `muddy_roots_in`, as 1.7.10 blocks at a place. */
	public interface Blocks {
		boolean canGrowThrough(World world, int x, int y, int z);

		boolean muddyRootsIn(World world, int x, int y, int z);
	}

	private final Blocks blocks;
	private final BlockProvider muddyRootsProvider;
	private final int maxRootWidth;
	private final int maxRootLength;
	private final float randomSkewChance;

	public MangroveRootPlacer(IntRange trunkOffsetY, BlockProvider rootProvider, BlockProvider aboveRootProvider, float aboveRootChance, Blocks blocks,
		BlockProvider muddyRootsProvider, int maxRootWidth, int maxRootLength, float randomSkewChance) {
		super(trunkOffsetY, rootProvider, aboveRootProvider, aboveRootChance);
		this.blocks = blocks;
		this.muddyRootsProvider = muddyRootsProvider;
		this.maxRootWidth = maxRootWidth;
		this.maxRootLength = maxRootLength;
		this.randomSkewChance = randomSkewChance;
	}

	@Override
	public boolean placeRoots(World world, Random random, int x, int y, int z, int trunkY, Tree tree) {
		for (int cy = y; cy < trunkY; cy++) {
			if (!canPlaceRoot(world, x, cy, z)) {
				return false;
			}
		}
		List<int[]> rootPositions = new ArrayList<>();
		rootPositions.add(new int[]{x, trunkY - 1, z});
		for (int dir = 0; dir < 4; dir++) {
			int[] step = TrunkPlacer.HORIZONTAL[dir];
			int[] beside = {x + step[0], trunkY, z + step[1]};
			List<int[]> inDirection = new ArrayList<>();
			if (!simulateRoots(world, random, beside, step, x, trunkY, z, inDirection, 0)) {
				return false;
			}
			rootPositions.addAll(inDirection);
			rootPositions.add(beside);
		}
		for (int[] root : rootPositions) {
			placeRoot(world, random, root[0], root[1], root[2], tree);
		}
		return true;
	}

	private boolean simulateRoots(World world, Random random, int[] pos, int[] step, int ox, int oy, int oz, List<int[]> rootPositions, int layer) {
		if (layer == maxRootLength || rootPositions.size() > maxRootLength) {
			return false;
		}
		for (int[] next : potentialRootPositions(pos, step, random, ox, oy, oz)) {
			if (canPlaceRoot(world, next[0], next[1], next[2])) {
				rootPositions.add(next);
				if (!simulateRoots(world, random, next, step, ox, oy, oz, rootPositions, layer + 1)) {
					return false;
				}
			}
		}
		return true;
	}

	private List<int[]> potentialRootPositions(int[] pos, int[] step, Random random, int ox, int oy, int oz) {
		int[] below = {pos[0], pos[1] - 1, pos[2]};
		int[] nextTo = {pos[0] + step[0], pos[1], pos[2] + step[1]};
		int width = Math.abs(pos[0] - ox) + Math.abs(pos[1] - oy) + Math.abs(pos[2] - oz);
		List<int[]> positions = new ArrayList<>(2);
		if (width > maxRootWidth - 3 && width <= maxRootWidth) {
			positions.add(below);
			if (random.nextFloat() < randomSkewChance) {
				positions.add(new int[]{nextTo[0], nextTo[1] - 1, nextTo[2]});
			}
		} else if (width > maxRootWidth) {
			positions.add(below);
		} else if (random.nextFloat() < randomSkewChance) {
			positions.add(below);
		} else {
			positions.add(random.nextBoolean() ? nextTo : below);
		}
		return positions;
	}

	@Override
	protected boolean canPlaceRoot(World world, int x, int y, int z) {
		return super.canPlaceRoot(world, x, y, z) || y >= 0 && y < world.getHeight() && blocks.canGrowThrough(world, x, y, z);
	}

	@Override
	protected void placeRoot(World world, Random random, int x, int y, int z, Tree tree) {
		if (blocks.muddyRootsIn(world, x, y, z)) {
			tree.setRoot(world, x, y, z, muddyRootsProvider.get(random));
		} else {
			super.placeRoot(world, random, x, y, z, tree);
		}
	}
}
