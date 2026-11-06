package io.github.asgerjon.mcfly.end;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

/**
 * 26.3's end gateway (`EndGatewayBlock`; SPEC-modern-movement.md §76.10, §84): its block entity does the work (going
 * through, the beam, the cooldown); the block passes through, gives light 15, can't be broken (3,600,000 resistance),
 * shows black on maps, is never pushed, drops nothing, is picked as nothing, and sends portal particles off each side
 * that shows. Under EFR's id `etfuturum:end_gateway`, its item kept for old saves' registries but in no tab (26.3 has
 * none).
 */
public class EndGatewayBlock extends BlockContainer {

	public EndGatewayBlock() {
		super(Material.portal);
		setBlockName(McFly.name("end_gateway"));
		setBlockUnbreakable();
		this.blockResistance = 3600000.0F * 5.0F;
		this.lightValue = 15;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new EndGatewayBlockEntity();
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		return null;
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	/** Drawn by its block entity's renderer only. */
	@Override
	public int getRenderType() {
		return -1;
	}

	/** 1.7.10's question, by the neighbour's place: a side shows unless that neighbour is opaque or another gateway. */
	@Override
	public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
		Block neighbour = world.getBlock(x, y, z);
		return !neighbour.isOpaqueCube() && neighbour != this;
	}

	@Override
	public MapColor getMapColor(int meta) {
		return MapColor.blackColor;
	}

	/** 26.3's push reaction `IMMOVEABLE`. */
	@Override
	public int getMobilityFlag() {
		return 2;
	}

	@Override
	public boolean canEntityDestroy(IBlockAccess world, int x, int y, int z, Entity entity) {
		return !(entity instanceof EntityDragon) && !(entity instanceof EntityWither);
	}

	@Override
	public int quantityDropped(Random random) {
		return 0;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Item getItem(World world, int x, int y, int z) {
		return null;
	}

	@Override
	@SuppressWarnings("rawtypes")
	@SideOnly(Side.CLIENT)
	public void getSubBlocks(Item item, CreativeTabs tab, List list) {
	}

	/** 26.3's `animateTick`: for each side that shows, a portal particle thrown out of it. */
	@Override
	@SideOnly(Side.CLIENT)
	public void randomDisplayTick(World world, int x, int y, int z, Random random) {
		TileEntity tile = world.getTileEntity(x, y, z);
		if (!(tile instanceof EndGatewayBlockEntity)) {
			return;
		}
		int count = ((EndGatewayBlockEntity) tile).getParticleAmount();
		for (int i = 0; i < count; i++) {
			double px = x + random.nextDouble();
			double py = y + random.nextDouble();
			double pz = z + random.nextDouble();
			double mx = (random.nextDouble() - 0.5) * 0.5;
			double my = (random.nextDouble() - 0.5) * 0.5;
			double mz = (random.nextDouble() - 0.5) * 0.5;
			int flip = random.nextInt(2) * 2 - 1;
			if (random.nextBoolean()) {
				pz = z + 0.5 + 0.25 * flip;
				mz = random.nextFloat() * 2.0F * flip;
			} else {
				px = x + 0.5 + 0.25 * flip;
				mx = random.nextFloat() * 2.0F * flip;
			}
			world.spawnParticle("portal", px, py, pz, mx, my, mz);
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister icons) {
		blockIcon = icons.registerIcon("obsidian");
	}
}
