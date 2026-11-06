package io.github.asgerjon.mcfly.sculk;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.sound.McFlySoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * 26.3's sculk catalyst (`SculkCatalystBlock`; SPEC-modern-movement.md §90.9, step H3), under EFR's id and form: meta
 * 1 its bloom, which a tick in 8 turns off. 3 and 3, the hoe, light 6, map colour black, its sound type; only to Silk
 * Touch it drops itself, else 5 experience. Its block entity takes the experience of what dies near it.
 */
public class SculkCatalystBlock extends McFlyBlock {

	public static final int BLOOM_TICKS = 8;

	@SideOnly(Side.CLIENT)
	private IIcon top, bottom, sideBloom, topBloom;

	public SculkCatalystBlock() {
		super(Material.ground);
		setNames("sculk_catalyst");
		strength(3.0F, 3.0F).tool("hoe", 0).light(6).sound(McFlySoundType.SCULK_CATALYST).colour(MapColor.blackColor);
	}

	/** 26.3's `tick`: the bloom off. */
	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		if (!world.isRemote && world.getBlockMetadata(x, y, z) == 1) {
			world.setBlockMetadataWithNotify(x, y, z, 0, 3);
		}
	}

	@Override
	public boolean hasTileEntity(int meta) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, int meta) {
		return new SculkCatalystBlockEntity();
	}

	@Override
	public Item getItemDropped(int meta, Random random, int fortune) {
		return null;
	}

	@Override
	public boolean canSilkHarvest(World world, EntityPlayer player, int x, int y, int z, int meta) {
		return true;
	}

	@Override
	public int getExpDrop(IBlockAccess world, int meta, int fortune) {
		return 5;
	}

	@Override
	public int damageDropped(int meta) {
		return 0;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		boolean bloom = meta == 1;
		if (side == 1) {
			return bloom ? topBloom : top;
		}
		if (side == 0) {
			return bottom;
		}
		return bloom ? sideBloom : blockIcon;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		blockIcon = register.registerIcon("sculk_catalyst_side");
		top = register.registerIcon("sculk_catalyst_top");
		bottom = register.registerIcon("sculk_catalyst_bottom");
		sideBloom = register.registerIcon("sculk_catalyst_side_bloom");
		topBloom = register.registerIcon("sculk_catalyst_top_bloom");
	}
}
