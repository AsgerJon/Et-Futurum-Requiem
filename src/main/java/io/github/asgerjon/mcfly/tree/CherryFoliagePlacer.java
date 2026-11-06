package io.github.asgerjon.mcfly.tree;

import net.minecraft.world.World;

import java.util.Random;

/**
 * The cherry tree's leaves (SPEC-modern-movement.md §90.6), 26.3's `CherryFoliagePlacer`: from the top, rows of radius
 * r - 2, r - 1, then r down to the attachment; under it two rows (r, then r - 1) with leaves hanging below their edges;
 * the wide rows' corners missing, some next to them, and holes at the bottom row's edge, by its chances.
 */
public final class CherryFoliagePlacer extends FoliagePlacer {

	private final IntRange height;
	private final float wideBottomLayerHoleChance;
	private final float cornerHoleChance;
	private final float hangingLeavesChance;
	private final float hangingLeavesExtensionChance;

	public CherryFoliagePlacer(IntRange radius, IntRange offset, IntRange height, float wideBottomLayerHoleChance, float cornerHoleChance,
		float hangingLeavesChance, float hangingLeavesExtensionChance) {
		super(radius, offset);
		this.height = height;
		this.wideBottomLayerHoleChance = wideBottomLayerHoleChance;
		this.cornerHoleChance = cornerHoleChance;
		this.hangingLeavesChance = hangingLeavesChance;
		this.hangingLeavesExtensionChance = hangingLeavesExtensionChance;
	}

	@Override
	public int foliageHeight(Random random) {
		return height.sample(random);
	}

	@Override
	public void createFoliage(World world, Random random, Tree tree, int treeHeight, Attachment attachment, int foliageHeight, int radius) {
		int offset = offset(random);
		boolean doubleTrunk = attachment.doubleTrunk;
		int x = attachment.x, y = attachment.y + offset, z = attachment.z;
		int currentRadius = radius + attachment.radiusOffset - 1;
		placeLeavesRow(world, random, tree, x, y, z, currentRadius - 2, foliageHeight - 3, doubleTrunk);
		placeLeavesRow(world, random, tree, x, y, z, currentRadius - 1, foliageHeight - 4, doubleTrunk);
		for (int dy = foliageHeight - 5; dy >= 0; dy--) {
			placeLeavesRow(world, random, tree, x, y, z, currentRadius, dy, doubleTrunk);
		}
		placeLeavesRowWithHangingLeavesBelow(world, random, tree, x, y, z, currentRadius, -1, doubleTrunk, hangingLeavesChance, hangingLeavesExtensionChance);
		placeLeavesRowWithHangingLeavesBelow(world, random, tree, x, y, z, currentRadius - 1, -2, doubleTrunk, hangingLeavesChance, hangingLeavesExtensionChance);
	}

	@Override
	protected boolean shouldSkipLocation(Random random, int dx, int y, int dz, int currentRadius, boolean doubleTrunk) {
		if (y == -1 && (dx == currentRadius || dz == currentRadius) && random.nextFloat() < wideBottomLayerHoleChance) {
			return true;
		}
		boolean corner = dx == currentRadius && dz == currentRadius;
		return currentRadius > 2 ? corner || dx + dz > currentRadius * 2 - 2 && random.nextFloat() < cornerHoleChance
			: corner && random.nextFloat() < cornerHoleChance;
	}
}
