package io.github.asgerjon.mcfly.tree;

import net.minecraft.init.Blocks;

/**
 * Vines hanging from a tree's leaves (SPEC-modern-movement.md §90.6), 26.3's `LeaveVineDecorator`: each leaf, each way
 * at its chance, into air beside it, a vine on the leaf's face, hanging down through air up to 4 more. 1.7.10's vine
 * meta is the face it's on: 8 east, 2 west, 1 south, 4 north.
 */
public final class LeaveVineDecorator extends TreeDecorator {

	private final float probability;

	public LeaveVineDecorator(float probability) {
		this.probability = probability;
	}

	@Override
	public void place(Context context) {
		for (int[] leaf : context.leaves) {
			int x = leaf[0], y = leaf[1], z = leaf[2];
			if (context.random.nextFloat() < probability && context.isAir(x - 1, y, z)) {
				hang(context, x - 1, y, z, 8);
			}
			if (context.random.nextFloat() < probability && context.isAir(x + 1, y, z)) {
				hang(context, x + 1, y, z, 2);
			}
			if (context.random.nextFloat() < probability && context.isAir(x, y, z - 1)) {
				hang(context, x, y, z - 1, 1);
			}
			if (context.random.nextFloat() < probability && context.isAir(x, y, z + 1)) {
				hang(context, x, y, z + 1, 4);
			}
		}
	}

	private static void hang(Context context, int x, int y, int z, int face) {
		context.setBlock(x, y, z, Blocks.vine, face);
		for (int below = y - 1, left = 4; context.isAir(x, below, z) && left > 0; below--, left--) {
			context.setBlock(x, below, z, Blocks.vine, face);
		}
	}
}
