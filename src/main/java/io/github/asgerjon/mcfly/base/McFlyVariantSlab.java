package io.github.asgerjon.mcfly.base;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import java.util.List;

/**
 * A slab of ours with variants (SPEC-modern-movement.md §88.2): up to 8, the variant in the low three bits, `+8` the top
 * half (the double: the variant alone), each one of 26.3's slabs, looking like its block's meta, with its own strength.
 * Its item is {@link McFlySlabItem}, which takes the variant as its damage.
 */
public class McFlyVariantSlab extends McFlySlab {

	/** One variant: 26.3's slab, the block and meta it looks like, its strength. */
	public static final class Variant {
		final String name;
		final Block block;
		final int meta;
		final float hardness;
		final float resistance;

		public Variant(String name, Block block, int meta, float hardness, float resistance) {
			this.name = name;
			this.block = block;
			this.meta = meta;
			this.hardness = hardness;
			this.resistance = resistance;
		}
	}

	private final Variant[] variants;

	/** The single slab: its registry's name is the first variant's. */
	public McFlyVariantSlab(Variant... variants) {
		super(variants[0].block, variants[0].name);
		this.variants = variants;
		strength(variants[0].hardness, variants[0].resistance);
	}

	/** The double of this single slab. */
	public McFlyVariantSlab(McFlyVariantSlab single) {
		super(single);
		this.variants = single.variants;
		strength(variants[0].hardness, variants[0].resistance);
	}

	private Variant variant(int meta) {
		int i = meta & 7;
		return variants[i < variants.length ? i : 0];
	}

	@Override
	public String func_150002_b(int meta) {
		return "tile." + McFly.name(variant(meta).name);
	}

	@Override
	public float getBlockHardness(World world, int x, int y, int z) {
		return variant(world.getBlockMetadata(x, y, z)).hardness;
	}

	@Override
	public float getExplosionResistance(Entity entity, World world, int x, int y, int z, double explosionX, double explosionY, double explosionZ) {
		return variant(world.getBlockMetadata(x, y, z)).resistance;
	}

	@Override
	public int damageDropped(int meta) {
		int i = meta & 7;
		return i < variants.length ? i : 0;
	}

	@Override
	protected ItemStack createStackedBlock(int meta) {
		return new ItemStack(single(), isDouble() ? 2 : 1, damageDropped(meta));
	}

	@Override
	public MapColor getMapColor(int meta) {
		Variant variant = variant(meta);
		return variant.block.getMapColor(variant.meta);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		Variant variant = variant(meta);
		return variant.block.getIcon(side, variant.meta);
	}

	@Override
	@SideOnly(Side.CLIENT)
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void getSubBlocks(Item item, CreativeTabs tab, List list) {
		if (!isDouble()) {
			for (int i = 0; i < variants.length; i++) {
				list.add(new ItemStack(item, 1, i));
			}
		}
	}
}
