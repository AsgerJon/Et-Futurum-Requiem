package io.github.asgerjon.mcfly.copper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.base.McFlySlab;
import io.github.asgerjon.mcfly.base.RandomTicking;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

/**
 * The cut copper slab (SPEC-modern-movement.md §87), EFR's `cut_copper_slab` and `double_cut_copper_slab`: the
 * variant (stage, +4 waxed) in the low three bits, +8 the top half (the double: the variant alone). Drawn as the cut
 * copper of its variant; 26.3's ageing, a double slab's too.
 */
public class CopperSlabBlock extends McFlySlab implements WeatheringCopper, RandomTicking {

	private final Block full;

	public CopperSlabBlock(Block full) {
		super(full, "cut_copper_slab");
		this.full = full;
		setup();
	}

	public CopperSlabBlock(CopperSlabBlock single) {
		super(single);
		this.full = single.full;
		setup();
	}

	private void setup() {
		strength(3.0F, 6.0F);
		setTickRandomly(true);
	}

	private static int variant(int meta) {
		return meta & 7;
	}

	@Override
	public int stage(IBlockAccess world, int x, int y, int z) {
		return variant(world.getBlockMetadata(x, y, z)) % 4;
	}

	@Override
	public boolean waxed(IBlockAccess world, int x, int y, int z) {
		return variant(world.getBlockMetadata(x, y, z)) >= 4;
	}

	@Override
	public void become(World world, int x, int y, int z, int stage, boolean waxed) {
		int meta = world.getBlockMetadata(x, y, z);
		world.setBlockMetadataWithNotify(x, y, z, stage + (waxed ? 4 : 0) + (isDouble() ? 0 : meta & 8), 3);
	}

	@Override
	public void randomTick(World world, int x, int y, int z, Random random) {
		Weathering.randomTick(this, world, x, y, z, random);
	}

	/** The cut copper of a variant: copper_block's meta. */
	private static int cutCopper(int variant) {
		return CopperFullBlock.meta(variant % 4, true, variant >= 4);
	}

	@Override
	public String func_150002_b(int meta) {
		return "tile." + McFly.name(Weathering.name("cut_copper_slab", variant(meta) % 4, variant(meta) >= 4));
	}

	@Override
	public int damageDropped(int meta) {
		return variant(meta);
	}

	@Override
	protected ItemStack createStackedBlock(int meta) {
		return new ItemStack(single(), isDouble() ? 2 : 1, variant(meta));
	}

	@Override
	public int getDamageValue(World world, int x, int y, int z) {
		return variant(world.getBlockMetadata(x, y, z));
	}

	@Override
	public MapColor getMapColor(int meta) {
		return Weathering.colour(variant(meta) % 4);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return full.getIcon(side, cutCopper(variant(meta)));
	}

	@Override
	@SideOnly(Side.CLIENT)
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void getSubBlocks(Item item, CreativeTabs tab, List list) {
		if (!isDouble()) {
			for (int variant = 0; variant < 8; variant++) {
				list.add(new ItemStack(item, 1, variant));
			}
		}
	}
}
