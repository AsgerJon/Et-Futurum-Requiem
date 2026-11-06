package io.github.asgerjon.mcfly.base;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
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

	/** 26.3's strength: 1.7.10's explosion resistance is kept as 26.3's number (1.7.10 divides the field by 5). */
	public McFlyBlock strength(float hardness, float resistance) {
		setHardness(hardness);
		this.blockResistance = resistance * 5.0F;
		return this;
	}

	/** The tool and its level for drops (and its speed). */
	public McFlyBlock tool(String tool, int level) {
		setHarvestLevel(tool, level);
		return this;
	}

	/** Its light, 0 to 15. */
	public McFlyBlock light(int level) {
		setLightLevel(level / 15.0F);
		return this;
	}

	public McFlyBlock sound(SoundType sound) {
		setStepSound(sound);
		return this;
	}

	/** 26.3's map colour, when it isn't its material's. */
	public McFlyBlock colour(MapColor colour) {
		this.colour = colour;
		return this;
	}

	private MapColor colour;

	@Override
	public MapColor getMapColor(int meta) {
		return colour != null ? colour : super.getMapColor(meta);
	}
}
