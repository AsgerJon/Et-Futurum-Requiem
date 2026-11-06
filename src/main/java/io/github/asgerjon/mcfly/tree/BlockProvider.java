package io.github.asgerjon.mcfly.tree;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import java.util.Random;

/** What block a tree puts at a place (SPEC-modern-movement.md §90.5), 26.3's block state providers: one, or a weighted pick. */
public interface BlockProvider {

	/** A block and its meta. */
	final class State {
		public final Block block;
		public final int meta;

		public State(Block block, int meta) {
			this.block = block;
			this.meta = meta;
		}
	}

	State get(Random random);

	static BlockProvider of(Block block, int meta) {
		State state = new State(block, meta);
		return random -> state;
	}

	/** A provider whose block goes only where its rule holds (26.3's `RuleBasedStateProvider` with no fallback). */
	interface Ruled extends BlockProvider {
		boolean applies(World world, int x, int y, int z);
	}

	/**
	 * 26.3's `soil_beneath_tree`: dirt, but not over `#cannot_replace_below_tree_trunk` (dirt, coarse dirt and podzol,
	 * rooted dirt, mud, muddy mangrove roots, moss; EFR's coarse dirt).
	 */
	static BlockProvider soilBeneathTree() {
		State dirt = new State(Blocks.dirt, 0);
		return new Ruled() {
			@Override
			public State get(java.util.Random random) {
				return dirt;
			}

			@Override
			public boolean applies(World world, int x, int y, int z) {
				Block block = world.getBlock(x, y, z);
				return !(block == Blocks.dirt || block == io.github.asgerjon.mcfly.registry.McFlyBlocks.ROOTED_DIRT.get()
					|| block == io.github.asgerjon.mcfly.registry.McFlyBlocks.MUD.get() || block == io.github.asgerjon.mcfly.registry.McFlyBlocks.MUDDY_MANGROVE_ROOTS.get()
					|| block == io.github.asgerjon.mcfly.registry.McFlyBlocks.MOSS_BLOCK.get()
					|| (io.github.asgerjon.mcfly.McFly.DOMAIN + ":coarse_dirt").equals(Block.blockRegistry.getNameForObject(block)));
			}
		};
	}

	/** 26.3's `weighted`: each state with its weight. */
	static BlockProvider weighted(State[] states, int[] weights) {
		int total = 0;
		for (int weight : weights) {
			total += weight;
		}
		int sum = total;
		return random -> {
			int pick = random.nextInt(sum);
			for (int i = 0; i < states.length; i++) {
				pick -= weights[i];
				if (pick < 0) {
					return states[i];
				}
			}
			return states[states.length - 1];
		};
	}
}
