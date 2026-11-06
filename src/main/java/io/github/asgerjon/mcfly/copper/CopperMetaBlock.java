package io.github.asgerjon.mcfly.copper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.base.RandomTicking;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Copper whose stage and wax are its meta (SPEC-modern-movement.md §87): the copper block (and cut copper), chiseled
 * copper, the grate, the bulb, each in EFR's stored form. 26.3's strength (3 and 6), a stone pickaxe for drops, its
 * colours by stage, its random tick's ageing (`Weathering`).
 */
public abstract class CopperMetaBlock extends McFlyBlock implements WeatheringCopper, RandomTicking {

	@SideOnly(Side.CLIENT)
	private Map<String, IIcon> icons;

	protected CopperMetaBlock(String name, SoundType sound) {
		super(Material.iron);
		setNames(name);
		setHardness(3.0F);
		this.blockResistance = 6.0F * 5.0F;
		setStepSound(sound);
		setHarvestLevel("pickaxe", 1);
		setTickRandomly(true);
	}

	/** The item's damages, in the creative tab's order. */
	public abstract int[] itemMetas();

	public abstract int stageOf(int meta);

	public abstract boolean waxedOf(int meta);

	/** The meta at a stage and wax, the rest of the old one's kept. */
	protected abstract int metaFor(int stage, boolean waxed, int oldMeta);

	/** 26.3's name of what this meta is (the item's for an item's damage). */
	public abstract String nameOf(int meta);

	/** The texture of this meta (26.3's waxed copper looks as the unwaxed). */
	protected String textureOf(int meta) {
		return nameOf(meta).replace("waxed_", "");
	}

	@Override
	public int stage(IBlockAccess world, int x, int y, int z) {
		return stageOf(world.getBlockMetadata(x, y, z));
	}

	@Override
	public boolean waxed(IBlockAccess world, int x, int y, int z) {
		return waxedOf(world.getBlockMetadata(x, y, z));
	}

	@Override
	public void become(World world, int x, int y, int z, int stage, boolean waxed) {
		world.setBlockMetadataWithNotify(x, y, z, metaFor(stage, waxed, world.getBlockMetadata(x, y, z)), 3);
	}

	@Override
	public void randomTick(World world, int x, int y, int z, Random random) {
		Weathering.randomTick(this, world, x, y, z, random);
	}

	/** The item: this meta's own (12 to 15 of chiseled copper as 4 to 7, a bulb without its light). */
	@Override
	public int damageDropped(int meta) {
		return metaFor(stageOf(meta), waxedOf(meta), 0);
	}

	@Override
	public MapColor getMapColor(int meta) {
		return Weathering.colour(stageOf(meta));
	}

	@Override
	@SideOnly(Side.CLIENT)
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void getSubBlocks(Item item, CreativeTabs tab, List list) {
		for (int meta : itemMetas()) {
			list.add(new ItemStack(item, 1, meta));
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		icons = new HashMap<>();
		for (int meta = 0; meta < 16; meta++) {
			String texture = textureOf(meta);
			if (!icons.containsKey(texture)) {
				icons.put(texture, register.registerIcon(texture));
			}
		}
		blockIcon = icons.get(textureOf(0));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		IIcon icon = icons == null ? null : icons.get(textureOf(meta));
		return icon != null ? icon : blockIcon;
	}

	/** Its item: one a damage, named as 26.3's. */
	public static class CopperItem extends net.minecraft.item.ItemBlock {

		public CopperItem(Block block) {
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
			return "tile." + io.github.asgerjon.mcfly.McFly.name(((CopperMetaBlock) field_150939_a).nameOf(stack.getItemDamage()));
		}
	}
}
