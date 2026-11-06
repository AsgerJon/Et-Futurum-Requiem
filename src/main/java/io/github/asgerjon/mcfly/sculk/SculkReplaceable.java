package io.github.asgerjon.mcfly.sculk;

import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import java.util.HashSet;
import java.util.Set;

/**
 * 26.3's `#sculk_replaceable` (SPEC-modern-movement.md §90.9, step H3), as far as 1.7.10 and McFly have it: the
 * overworld's base stones and soils, sand and gravel, clay and terracotta, the Nether's stones, soils and nylia, end
 * stone and the sandstones; what sculk may spread into.
 */
public final class SculkReplaceable {

	private static Set<Block> blocks;

	private SculkReplaceable() {
	}

	public static boolean is(Block block, int meta) {
		if (blocks == null) {
			blocks = build();
		}
		if (McFlyBlocks.STONE.isEnabled() && block == McFlyBlocks.STONE.get()) {
			// granite, diorite and andesite, not their polished ones (`stone` 1, 3, 5; 0 EFR's look-alike of granite)
			return meta == 0 || meta == 1 || meta == 3 || meta == 5;
		}
		return blocks.contains(block);
	}

	private static Set<Block> build() {
		Set<Block> set = new HashSet<>();
		// 1.7.10's: stone, the soils, sand (both), gravel, clay, terracotta, netherrack, soul sand, end stone, sandstone
		for (Block block : new Block[]{Blocks.stone, Blocks.dirt, Blocks.grass, Blocks.mycelium, Blocks.sand, Blocks.gravel, Blocks.clay, Blocks.hardened_clay,
			Blocks.stained_hardened_clay, Blocks.netherrack, Blocks.soul_sand, Blocks.end_stone, Blocks.sandstone}) {
			set.add(block);
		}
		// McFly's
		for (McFlyBlocks entry : new McFlyBlocks[]{McFlyBlocks.TUFF, McFlyBlocks.DEEPSLATE, McFlyBlocks.CALCITE, McFlyBlocks.DRIPSTONE_BLOCK, McFlyBlocks.MUD,
			McFlyBlocks.ROOTED_DIRT, McFlyBlocks.MOSS_BLOCK, McFlyBlocks.MUDDY_MANGROVE_ROOTS, McFlyBlocks.BASALT, McFlyBlocks.SMOOTH_BASALT,
			McFlyBlocks.BLACKSTONE, McFlyBlocks.SOUL_SOIL, McFlyBlocks.NYLIUM, McFlyBlocks.RED_SANDSTONE}) {
			if (entry.isEnabled()) {
				set.add(entry.get());
			}
		}
		return set;
	}
}
