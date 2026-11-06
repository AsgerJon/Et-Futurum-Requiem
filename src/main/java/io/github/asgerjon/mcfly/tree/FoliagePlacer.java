package io.github.asgerjon.mcfly.tree;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import java.util.Random;

/** How a tree's leaves go (SPEC-modern-movement.md §90.5), 26.3's `FoliagePlacer`: its radius and height, leaves at an attachment. */
public abstract class FoliagePlacer {

	/** 26.3's `FoliageAttachment`: a place on the trunk, a radius offset, whether the trunk is two wide. */
	public static final class Attachment {
		final int x;
		final int y;
		final int z;
		final int radiusOffset;
		final boolean doubleTrunk;

		public Attachment(int x, int y, int z, int radiusOffset, boolean doubleTrunk) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.radiusOffset = radiusOffset;
			this.doubleTrunk = doubleTrunk;
		}
	}

	private final IntRange radius;
	private final IntRange offset;

	protected FoliagePlacer(IntRange radius, IntRange offset) {
		this.radius = radius;
		this.offset = offset;
	}

	public int radius(Random random) {
		return radius.sample(random);
	}

	protected int offset(Random random) {
		return offset.sample(random);
	}

	public abstract int foliageHeight(Random random);

	public abstract void createFoliage(World world, Random random, Tree tree, int treeHeight, Attachment attachment, int foliageHeight, int radius);

	/** A leaf where the tree may go and no persistent (placed) leaf is (26.3's `tryPlaceLeaf`). */
	protected static boolean tryPlaceLeaf(World world, Random random, Tree tree, int x, int y, int z) {
		if (y < 0 || y >= world.getHeight()) {
			return false;
		}
		Block there = world.getBlock(x, y, z);
		boolean persistent = there.isLeaves(world, x, y, z) && (world.getBlockMetadata(x, y, z) & 4) != 0;
		if (persistent || !Tree.validTreePos(world, x, y, z)) {
			return false;
		}
		tree.setLeaf(world, x, y, z, tree.foliage.get(random));
		return true;
	}

	/** 26.3's `shouldSkipLocation` (by distances from the middle): none skipped unless a placer says. */
	protected boolean shouldSkipLocation(Random random, int dx, int y, int dz, int radius, boolean doubleTrunk) {
		return false;
	}

	/** 26.3's `shouldSkipLocationSigned`: the distances from the middle (of a two-wide trunk, its nearer half). */
	protected boolean shouldSkipLocationSigned(Random random, int dx, int y, int dz, int radius, boolean doubleTrunk) {
		int minDx = doubleTrunk ? Math.min(Math.abs(dx), Math.abs(dx - 1)) : Math.abs(dx);
		int minDz = doubleTrunk ? Math.min(Math.abs(dz), Math.abs(dz - 1)) : Math.abs(dz);
		return shouldSkipLocation(random, minDx, y, minDz, radius, doubleTrunk);
	}

	/** 26.3's `placeLeavesRow`: a square of leaves at y above the place, but those skipped. */
	protected void placeLeavesRow(World world, Random random, Tree tree, int x, int y, int z, int radius, int dy, boolean doubleTrunk) {
		int offset = doubleTrunk ? 1 : 0;
		for (int dx = -radius; dx <= radius + offset; dx++) {
			for (int dz = -radius; dz <= radius + offset; dz++) {
				if (!shouldSkipLocationSigned(random, dx, dy, dz, radius, doubleTrunk)) {
					tryPlaceLeaf(world, random, tree, x + dx, y + dy, z + dz);
				}
			}
		}
	}

	/**
	 * 26.3's `placeLeavesRowWithHangingLeavesBelow`: the row, then round its edge, under each leaf of this tree, a hanging
	 * leaf at its chance and below that one more at the extension's (none 7 or more from the trunk's top).
	 */
	protected final void placeLeavesRowWithHangingLeavesBelow(World world, Random random, Tree tree, int x, int y, int z, int radius, int dy,
		boolean doubleTrunk, float hangingChance, float extensionChance) {
		placeLeavesRow(world, random, tree, x, y, z, radius, dy, doubleTrunk);
		int offset = doubleTrunk ? 1 : 0;
		int logY = y - 1;
		for (int along = 0; along < 4; along++) {
			int toEdge = (along + 1) % 4;
			int[] alongStep = TrunkPlacer.HORIZONTAL[along], edgeStep = TrunkPlacer.HORIZONTAL[toEdge];
			// east and south are the positive ways
			int offsetToEdge = toEdge == 1 || toEdge == 2 ? radius + offset : radius;
			int px = x + edgeStep[0] * offsetToEdge - alongStep[0] * radius, py = y + dy - 1, pz = z + edgeStep[1] * offsetToEdge - alongStep[1] * radius;
			for (int offsetAlongEdge = -radius; offsetAlongEdge < radius + offset; offsetAlongEdge++) {
				if (tree.isLeafSet(px, py + 1, pz) && tryPlaceExtension(world, random, tree, hangingChance, x, logY, z, px, py, pz)) {
					tryPlaceExtension(world, random, tree, extensionChance, x, logY, z, px, py - 1, pz);
				}
				px += alongStep[0];
				pz += alongStep[1];
			}
		}
	}

	/** 26.3's `tryPlaceExtension`: a leaf at its chance, none 7 or more (Manhattan) from the log's place. */
	private static boolean tryPlaceExtension(World world, Random random, Tree tree, float chance, int logX, int logY, int logZ, int x, int y, int z) {
		if (Math.abs(x - logX) + Math.abs(y - logY) + Math.abs(z - logZ) >= 7) {
			return false;
		}
		return !(random.nextFloat() > chance) && tryPlaceLeaf(world, random, tree, x, y, z);
	}
}
