package io.github.asgerjon.mcfly.base;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.MapColor;

/**
 * Stairs of ours (SPEC-modern-movement.md §84): 1.7.10's stairs, which bend at corners as 26.3's do, with their block's
 * strength, sound and texture (26.3's `ofLegacyCopy`).
 */
public class McFlyStairs extends BlockStairs {

	private final Block base;
	private final int baseMeta;

	public McFlyStairs(Block base, String name) {
		this(base, 0, name);
	}

	/** Stairs of one variant of a block of variants (spec §88.2): its meta's look and map colour. */
	public McFlyStairs(Block base, int baseMeta, String name) {
		super(base, baseMeta);
		this.base = base;
		this.baseMeta = baseMeta;
		setCreativeTab(McFlyTab.TAB);
		setBlockName(McFly.name(name));
		setHarvestLevel(base.getHarvestTool(baseMeta), base.getHarvestLevel(baseMeta));
		// lit by its neighbours, as 1.7.10's own stairs are (Block.registerBlocks sets it for vanilla's alone)
		this.useNeighborBrightness = true;
	}

	/** 26.3's strength (`ofLegacyCopy` of the variant): 1.7.10's explosion resistance kept as 26.3's number. */
	public McFlyStairs strength(float hardness, float resistance) {
		setHardness(hardness);
		this.blockResistance = resistance * 5.0F;
		return this;
	}

	/** The tool and its level (stairs of a vanilla block: Forge gives vanilla's theirs only after ours are made). */
	public McFlyStairs tool(String tool, int level) {
		setHarvestLevel(tool, level);
		return this;
	}

	/** Its own sound (a variant's of a block of several: spec §89, deepslate tiles'). */
	public McFlyStairs sound(SoundType sound) {
		setStepSound(sound);
		return this;
	}

	@Override
	public MapColor getMapColor(int meta) {
		return base.getMapColor(baseMeta);
	}
}
