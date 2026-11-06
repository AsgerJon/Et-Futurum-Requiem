package io.github.asgerjon.mcfly.base;

import net.minecraft.block.Block;
import net.minecraft.item.ItemSlab;

/** The item of one of our slabs, single or double (SPEC-modern-movement.md §84): 1.7.10's slab item, paired from the block. */
public class McFlySlabItem extends ItemSlab {

	public McFlySlabItem(Block block) {
		super(block, ((McFlySlab) block).single(), ((McFlySlab) block).single().doubled(), ((McFlySlab) block).isDouble());
	}
}
