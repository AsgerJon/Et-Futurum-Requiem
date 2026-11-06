package io.github.asgerjon.mcfly.copper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * The copper bulb (SPEC-modern-movement.md §87), 26.3's `CopperBulbBlock`, in EFR's stored form: two blocks,
 * `copper_bulb` and `powered_copper_bulb` (powered is the block; the powered one has no item); meta stage, +4 lit, +8
 * waxed. A rising edge of power flips its light (`checkAndFlip`, with `block.copper_bulb.turn_on` or `turn_off`); lit,
 * it gives 15, 12, 8 or 4 light by stage, and a comparator reads 15.
 */
public class CopperBulbBlock extends CopperMetaBlock {

	private static final int[] ITEMS = {0, 1, 2, 3, 8, 9, 10, 11};
	private static final int[] LIGHT = {15, 12, 8, 4};

	private final boolean powered;

	public CopperBulbBlock(boolean powered) {
		super("copper_bulb", McFlySoundType.COPPER_BULB);
		this.powered = powered;
		if (powered) {
			setBlockName(McFly.name("copper_bulb_powered"));
			setCreativeTab(null);
		}
	}

	public static boolean lit(int meta) {
		return meta % 8 >= 4;
	}

	@Override
	public int[] itemMetas() {
		return ITEMS;
	}

	@Override
	public int stageOf(int meta) {
		return meta % 4;
	}

	@Override
	public boolean waxedOf(int meta) {
		return meta >= 8;
	}

	@Override
	protected int metaFor(int stage, boolean waxed, int oldMeta) {
		return stage + (oldMeta & 4) + (waxed ? 8 : 0);
	}

	@Override
	public String nameOf(int meta) {
		return Weathering.name("copper_bulb", stageOf(meta), waxedOf(meta));
	}

	@Override
	protected String textureOf(int meta) {
		return Weathering.name("copper_bulb", stageOf(meta), false) + (lit(meta) ? "_lit" : "") + (powered ? "_powered" : "");
	}

	@Override
	public int getLightValue(IBlockAccess world, int x, int y, int z) {
		if (world.getBlock(x, y, z) != this) {
			return 0;
		}
		int meta = world.getBlockMetadata(x, y, z);
		return lit(meta) ? LIGHT[stageOf(meta)] : 0;
	}

	@Override
	public boolean hasComparatorInputOverride() {
		return true;
	}

	@Override
	public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
		return lit(world.getBlockMetadata(x, y, z)) ? 15 : 0;
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		checkAndFlip(world, x, y, z);
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
		checkAndFlip(world, x, y, z);
	}

	/** 26.3's `checkAndFlip`: power changed, and from none, the light flips; the block becomes the powered one or not. */
	private void checkAndFlip(World world, int x, int y, int z) {
		if (world.isRemote) {
			return;
		}
		boolean signal = world.isBlockIndirectlyGettingPowered(x, y, z);
		if (signal == powered) {
			return;
		}
		int meta = world.getBlockMetadata(x, y, z);
		if (!powered) {
			meta ^= 4;
			world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, McFly.MC_ASSETS + (lit(meta) ? ":block.copper_bulb.turn_on" : ":block.copper_bulb.turn_off"), 1.0F, 1.0F);
		}
		world.setBlock(x, y, z, (signal ? McFlyBlocks.POWERED_COPPER_BULB : McFlyBlocks.COPPER_BULB).get(), meta, 3);
	}

	@Override
	public Item getItemDropped(int meta, Random random, int fortune) {
		return Item.getItemFromBlock(McFlyBlocks.COPPER_BULB.get());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Item getItem(World world, int x, int y, int z) {
		return Item.getItemFromBlock(McFlyBlocks.COPPER_BULB.get());
	}

	@Override
	public int damageDropped(int meta) {
		return stageOf(meta) + (waxedOf(meta) ? 8 : 0);
	}
}
