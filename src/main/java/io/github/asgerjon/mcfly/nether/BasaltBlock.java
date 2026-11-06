package io.github.asgerjon.mcfly.nether;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.NamedMetas;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import java.util.List;

/**
 * Basalt and polished basalt (SPEC-modern-movement.md §88.3), EFR's `basalt`: 26.3's `RotatedPillarBlock`s in one, meta
 * & 3 the kind (0 basalt, 1 polished), meta & 12 the axis (0 y, 4 x, 8 z); 1.25 and 4.2, a pickaxe, basalt's sound.
 */
public class BasaltBlock extends BlockRotatedPillar implements NamedMetas {

	private static final String[] NAMES = {"basalt", "polished_basalt"};
	private IIcon[] sides;
	private IIcon[] ends;

	public BasaltBlock() {
		super(Material.rock);
		setCreativeTab(McFlyTab.TAB);
		setBlockName(McFly.name("basalt"));
		setHardness(1.25F);
		this.blockResistance = 4.2F * 5.0F;
		setStepSound(McFlySoundType.BASALT);
		setHarvestLevel("pickaxe", 0);
	}

	@Override
	public String nameOf(int meta) {
		return NAMES[(meta & 3) == 1 ? 1 : 0];
	}

	@Override
	public int damageDropped(int meta) {
		return (meta & 3) == 1 ? 1 : 0;
	}

	@Override
	public MapColor getMapColor(int meta) {
		return MapColor.blackColor;
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void getSubBlocks(Item item, CreativeTabs tab, List list) {
		list.add(new ItemStack(item, 1, 0));
		list.add(new ItemStack(item, 1, 1));
	}

	@Override
	protected IIcon getSideIcon(int meta) {
		return sides[(meta & 3) == 1 ? 1 : 0];
	}

	@Override
	protected IIcon getTopIcon(int meta) {
		return ends[(meta & 3) == 1 ? 1 : 0];
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		sides = new IIcon[]{register.registerIcon("basalt_side"), register.registerIcon("polished_basalt_side")};
		ends = new IIcon[]{register.registerIcon("basalt_top"), register.registerIcon("polished_basalt_top")};
		blockIcon = sides[0];
	}
}
