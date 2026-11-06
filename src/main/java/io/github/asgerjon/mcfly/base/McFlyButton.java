package io.github.asgerjon.mcfly.base;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

/**
 * A button of ours (SPEC-modern-movement.md §88.2, §91.7), 26.3's `ButtonBlock` on 1.7.10's by its set type: a wooden
 * set's 30 ticks and arrows, a stone one's 20; 0.5; no map colour (26.3's `buttonProperties`); drawn as a block's meta.
 * 1.7.10's metas, with the floor and ceiling ones McFly's button mixin gives every button (0 and 5; 6 and 7 the other
 * facing). Its clicks are the set type's (the same mixin).
 */
public class McFlyButton extends BlockButton implements McFlySetType.Typed {

	private final McFlySetType type;
	private final Block look;
	private final int lookMeta;

	/** A stone set's (`wooden` false: 26.3's stone set) or a wooden one's (the oak set's). */
	public McFlyButton(String name, boolean wooden, Block look, int lookMeta) {
		this(name, wooden ? McFlySetType.WOOD : McFlySetType.STONE, look, lookMeta);
	}

	public McFlyButton(String name, McFlySetType type, Block look, int lookMeta) {
		super(type.byArrows);
		this.type = type;
		this.look = look;
		this.lookMeta = lookMeta;
		setCreativeTab(McFlyTab.TAB);
		setBlockName(McFly.name(name));
		setHardness(0.5F);
		this.blockResistance = 0.5F * 5.0F;
		setStepSound(type.sound);
		if (type.byArrows) {
			setHarvestLevel("axe", 0);
		}
	}

	@Override
	public McFlySetType setType() {
		return type;
	}

	@Override
	public MapColor getMapColor(int meta) {
		return MapColor.airColor;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return look.getIcon(1, lookMeta);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
	}
}
