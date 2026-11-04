package io.github.asgerjon.mcfly.base;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import net.minecraft.item.Item;

/**
 * A plain item of ours (SPEC-modern-movement.md §79.4, B9): in McFly's creative tab, named under our domain
 * (`etfuturum.glow_ink_sac`), its texture by its name (`textures/items/glow_ink_sac.png`).
 */
public class McFlyItem extends Item {

	public McFlyItem() {
		setCreativeTab(McFlyTab.TAB);
	}

	public McFlyItem(String name) {
		this();
		setNames(name);
	}

	/** Its unlocalised name under our domain and its texture by the same name. */
	public McFlyItem setNames(String name) {
		setUnlocalizedName(McFly.name(name));
		setTextureName(name);
		return this;
	}
}
