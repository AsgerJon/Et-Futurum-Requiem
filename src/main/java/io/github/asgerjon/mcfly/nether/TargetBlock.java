package io.github.asgerjon.mcfly.nether;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * The target (SPEC-modern-movement.md §88.8), 26.3's `TargetBlock` in EFR's stored form (the meta its power): a
 * projectile's hit gives 1 to 15 by how near it is to the middle of the face it hit (26.3's `getRedstoneStrength`), for
 * 20 ticks from an arrow and 8 from anything else, weak power only, unless it's already giving some; 0.5, a hoe's,
 * grass's sounds. Arrows reach it as they stick (1.7.10's `onEntityCollidedWithBlock`), throwables by the fork's
 * projectile fix.
 */
public class TargetBlock extends McFlyBlock {

	@SideOnly(Side.CLIENT)
	private IIcon top;

	public TargetBlock() {
		super(Material.grass);
		setNames("target");
		strength(0.5F, 0.5F).tool("hoe", 0).sound(soundTypeGrass).colour(MapColor.quartzColor);
	}

	@Override
	public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
		if (world.isRemote || !(entity instanceof IProjectile) || world.getBlockMetadata(x, y, z) != 0) {
			return;
		}
		int power = strength(entity.posX - x, entity.posY - y, entity.posZ - z);
		world.setBlockMetadataWithNotify(x, y, z, power, 3);
		world.scheduleBlockUpdate(x, y, z, this, entity instanceof EntityArrow ? 20 : 8);
	}

	/**
	 * 26.3's: the face hit is the axis the projectile is furthest out along; on it, the furthest of the other two from
	 * the middle (0 to 0.5), as 15 at the middle down to 1 at the edge.
	 */
	public static int strength(double px, double py, double pz) {
		double ox = px - 0.5, oy = py - 0.5, oz = pz - 0.5;
		double dx = Math.abs(clampFrac(px) - 0.5), dy = Math.abs(clampFrac(py) - 0.5), dz = Math.abs(clampFrac(pz) - 0.5);
		double max;
		if (Math.abs(oy) >= Math.abs(ox) && Math.abs(oy) >= Math.abs(oz)) {
			max = Math.max(dx, dz);
		} else if (Math.abs(oz) >= Math.abs(ox)) {
			max = Math.max(dx, dy);
		} else {
			max = Math.max(dy, dz);
		}
		return Math.max(1, (int) Math.ceil(15.0D * Math.max(0.0D, Math.min(1.0D, (0.5D - max) / 0.5D))));
	}

	/** Where in the block along an axis: 0 to 1 (outside, the nearest face). */
	private static double clampFrac(double v) {
		return Math.max(0.0D, Math.min(1.0D, v));
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		if (world.getBlockMetadata(x, y, z) != 0) {
			world.setBlockMetadataWithNotify(x, y, z, 0, 3);
		}
	}

	@Override
	public boolean canProvidePower() {
		return true;
	}

	@Override
	public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
		return world.getBlockMetadata(x, y, z);
	}

	@Override
	public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side) {
		return 0;
	}

	@Override
	public int damageDropped(int meta) {
		return 0;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register) {
		blockIcon = register.registerIcon("target_side");
		top = register.registerIcon("target_top");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return side < 2 ? top : blockIcon;
	}
}
