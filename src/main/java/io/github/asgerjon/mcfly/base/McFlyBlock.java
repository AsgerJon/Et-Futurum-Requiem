package io.github.asgerjon.mcfly.base;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

/**
 * A block of ours (SPEC-modern-movement.md §79.4, B9): a public constructor (1.7.10's is protected), in McFly's creative
 * tab, named under our domain (`etfuturum.kelp`), its texture by its name.
 */
public class McFlyBlock extends Block {

	public McFlyBlock(Material material) {
		super(material);
		setCreativeTab(McFlyTab.TAB);
	}

	/** Its unlocalised name under our domain and its texture by the same name. */
	public McFlyBlock setNames(String name) {
		setBlockName(McFly.name(name));
		setBlockTextureName(name);
		return this;
	}
}
