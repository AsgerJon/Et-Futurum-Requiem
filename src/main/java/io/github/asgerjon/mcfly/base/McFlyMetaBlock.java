package io.github.asgerjon.mcfly.base;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.List;

/**
 * A full block of ours with variants by meta (SPEC-modern-movement.md §88.2), each one of 26.3's blocks: its name, its
 * look (one texture, a side and an end as 26.3's `cube_column`, or a side, top and bottom), its strength and map colour. Its item is
 * {@link McFlyMetaItem}; an unknown meta reads as the first variant.
 */
public class McFlyMetaBlock extends McFlyBlock implements NamedMetas {

	/** One variant: 26.3's block it is. */
	public static final class Variant {
		final String name;
		final String side;
		final String end;
		final String bottom;
		final float hardness;
		final float resistance;
		final MapColor colour;

		/** A block of one texture, its name's. */
		public Variant(String name, float hardness, float resistance, MapColor colour) {
			this(name, name, null, hardness, resistance, colour);
		}

		/** A column: its sides and its ends (top and bottom) by these textures. */
		public Variant(String name, String side, String end, float hardness, float resistance, MapColor colour) {
			this(name, side, end, end, hardness, resistance, colour);
		}

		/** Three faces (26.3's `cube_bottom_top`: red sandstone's): its sides, its top and its bottom by these textures. */
		public Variant(String name, String side, String top, String bottom, float hardness, float resistance, MapColor colour) {
			this.name = name;
			this.side = side;
			this.end = top;
			this.bottom = bottom;
			this.hardness = hardness;
			this.resistance = resistance;
			this.colour = colour;
		}
	}

	private final Variant[] variants;
	@SideOnly(Side.CLIENT)
	private IIcon[] sides;
	@SideOnly(Side.CLIENT)
	private IIcon[] ends;
	@SideOnly(Side.CLIENT)
	private IIcon[] bottoms;

	public McFlyMetaBlock(Material material, SoundType sound, Variant... variants) {
		super(material);
		this.variants = variants;
		setNames(variants[0].name);
		setStepSound(sound);
		setHardness(variants[0].hardness);
		this.blockResistance = variants[0].resistance * 5.0F;
	}

	@Override
	public McFlyMetaBlock tool(String tool, int level) {
		super.tool(tool, level);
		return this;
	}

	protected Variant variant(int meta) {
		return variants[meta >= 0 && meta < variants.length ? meta : 0];
	}

	public int variants() {
		return variants.length;
	}

	@Override
	public String nameOf(int meta) {
		return variant(meta).name;
	}

	@Override
	public boolean isVariant(int meta) {
		return meta >= 0 && meta < variants.length;
	}

	@Override
	public float getBlockHardness(World world, int x, int y, int z) {
		return variant(world.getBlockMetadata(x, y, z)).hardness;
	}

	/** 26.3's resistance of the variant there (1.7.10's explosions take this as it is). */
	@Override
	public float getExplosionResistance(Entity entity, World world, int x, int y, int z, double explosionX, double explosionY, double explosionZ) {
		return variant(world.getBlockMetadata(x, y, z)).resistance;
	}

	@Override
	public int damageDropped(int meta) {
		return meta >= 0 && meta < variants.length ? meta : 0;
	}

	@Override
	public int getDamageValue(World world, int x, int y, int z) {
		return damageDropped(world.getBlockMetadata(x, y, z));
	}

	@Override
	public MapColor getMapColor(int meta) {
		return variant(meta).colour;
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void getSubBlocks(Item item, CreativeTabs tab, List list) {
		for (int meta = 0; meta < variants.length; meta++) {
			list.add(new ItemStack(item, 1, meta));
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		sides = new IIcon[variants.length];
		ends = new IIcon[variants.length];
		bottoms = new IIcon[variants.length];
		for (int i = 0; i < variants.length; i++) {
			sides[i] = register.registerIcon(variants[i].side);
			ends[i] = variants[i].end == null ? sides[i] : register.registerIcon(variants[i].end);
			bottoms[i] = variants[i].bottom == null ? sides[i] : variants[i].bottom.equals(variants[i].end) ? ends[i] : register.registerIcon(variants[i].bottom);
		}
		blockIcon = sides[0];
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		if (sides == null) {
			return blockIcon;
		}
		int i = damageDropped(meta);
		return side == 0 ? bottoms[i] : side == 1 ? ends[i] : sides[i];
	}
}
