package io.github.asgerjon.mcfly.base;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

/**
 * A pillar of ours (SPEC-modern-movement.md §84), 26.3's `RotatedPillarBlock` on 1.7.10's: along the clicked face's axis
 * (meta 0 up, 4 east-west, 8 north-south), its side texture `<name>` and its ends `<name>_top`.
 */
public class McFlyPillar extends BlockRotatedPillar {

	public McFlyPillar(Material material, String name) {
		super(material);
		setCreativeTab(McFlyTab.TAB);
		setBlockName(McFly.name(name));
		setBlockTextureName(name);
	}

	/** 26.3's strength: 1.7.10's explosion resistance is kept as 26.3's number (1.7.10 divides the field by 5). */
	public McFlyPillar strength(float hardness, float resistance) {
		setHardness(hardness);
		this.blockResistance = resistance * 5.0F;
		return this;
	}

	@Override
	protected IIcon getSideIcon(int meta) {
		return blockIcon;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister icons) {
		blockIcon = icons.registerIcon(getTextureName());
		field_150164_N = icons.registerIcon(getTextureName() + "_top");
	}
}
