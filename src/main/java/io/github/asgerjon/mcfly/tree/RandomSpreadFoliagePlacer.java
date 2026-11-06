package io.github.asgerjon.mcfly.tree;

import net.minecraft.world.World;

import java.util.Random;

/** The azalea's leaves (SPEC-modern-movement.md §90.5), 26.3's `RandomSpreadFoliagePlacer`: tries at offsets rand(r) - rand(r). */
public final class RandomSpreadFoliagePlacer extends FoliagePlacer {

	private final IntRange foliageHeight;
	private final int attempts;

	public RandomSpreadFoliagePlacer(IntRange radius, IntRange offset, IntRange foliageHeight, int attempts) {
		super(radius, offset);
		this.foliageHeight = foliageHeight;
		this.attempts = attempts;
	}

	@Override
	public int foliageHeight(Random random) {
		return foliageHeight.sample(random);
	}

	@Override
	public void createFoliage(World world, Random random, Tree tree, int treeHeight, Attachment attachment, int height, int radius) {
		offset(random);
		for (int i = 0; i < attempts; i++) {
			tryPlaceLeaf(world, random, tree, attachment.x + random.nextInt(radius) - random.nextInt(radius),
				attachment.y + random.nextInt(height) - random.nextInt(height), attachment.z + random.nextInt(radius) - random.nextInt(radius));
		}
	}
}
