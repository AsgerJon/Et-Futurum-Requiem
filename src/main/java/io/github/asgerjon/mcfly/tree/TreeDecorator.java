package io.github.asgerjon.mcfly.tree;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * What's added to a placed tree (SPEC-modern-movement.md §90.6), 26.3's `TreeDecorator`: from what the tree put down,
 * its logs, leaves and roots, each sorted by height.
 */
public abstract class TreeDecorator {

	public abstract void place(Context context);

	/** 26.3's `TreeDecorator.Context`. */
	public static final class Context {
		final World world;
		final Random random;
		final List<int[]> logs;
		final List<int[]> leaves;
		final List<int[]> roots;

		Context(World world, Random random, List<int[]> logs, List<int[]> leaves, List<int[]> roots) {
			this.world = world;
			this.random = random;
			this.logs = sorted(logs);
			this.leaves = sorted(leaves);
			this.roots = sorted(roots);
		}

		private static List<int[]> sorted(List<int[]> positions) {
			List<int[]> copy = new ArrayList<>(positions);
			copy.sort(Comparator.comparingInt(p -> p[1]));
			return copy;
		}

		boolean isAir(int x, int y, int z) {
			return y >= 0 && y < world.getHeight() && world.isAirBlock(x, y, z);
		}

		void setBlock(int x, int y, int z, Block block, int meta) {
			world.setBlock(x, y, z, block, meta, 3);
		}
	}
}
