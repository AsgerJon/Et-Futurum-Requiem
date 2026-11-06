package io.github.asgerjon.mcfly.copper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import java.util.List;

/**
 * Blocks of raw copper, iron and gold (SPEC-modern-movement.md §87), EFR's `raw_ore_block`: meta 0 copper, 1 iron, 2
 * gold. 26.3's: 5 and 6, a stone pickaxe (gold's iron), stone's sound.
 */
public class RawOreBlock extends McFlyBlock {

	static final String[] NAMES = {"raw_copper_block", "raw_iron_block", "raw_gold_block"};
	private static final MapColor[] COLOURS = {MapColor.adobeColor, MapColor.ironColor, MapColor.goldColor};

	@SideOnly(Side.CLIENT)
	private IIcon[] icons;

	public RawOreBlock() {
		super(Material.rock);
		setBlockName(McFly.name("raw_ore_block"));
		setHardness(5.0F);
		this.blockResistance = 6.0F * 5.0F;
		setStepSound(soundTypeStone);
		setHarvestLevel("pickaxe", 1, 0);
		setHarvestLevel("pickaxe", 1, 1);
		setHarvestLevel("pickaxe", 2, 2);
	}

	@Override
	public int damageDropped(int meta) {
		return Math.min(meta, 2);
	}

	@Override
	public MapColor getMapColor(int meta) {
		return COLOURS[Math.min(meta, 2)];
	}

	@Override
	@SideOnly(Side.CLIENT)
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void getSubBlocks(Item item, CreativeTabs tab, List list) {
		for (int meta = 0; meta < 3; meta++) {
			list.add(new ItemStack(item, 1, meta));
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		icons = new IIcon[NAMES.length];
		for (int i = 0; i < NAMES.length; i++) {
			icons[i] = register.registerIcon(NAMES[i]);
		}
		blockIcon = icons[0];
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return icons[Math.min(meta, 2)];
	}

	/** Its item: copper, iron, gold, named as 26.3's. */
	public static class RawOreBlockItem extends ItemBlock {

		public RawOreBlockItem(net.minecraft.block.Block block) {
			super(block);
			setHasSubtypes(true);
			setMaxDamage(0);
		}

		@Override
		public int getMetadata(int meta) {
			return meta;
		}

		@Override
		public String getUnlocalizedName(ItemStack stack) {
			return "tile." + McFly.name(NAMES[Math.min(stack.getItemDamage(), 2)]);
		}
	}
}
