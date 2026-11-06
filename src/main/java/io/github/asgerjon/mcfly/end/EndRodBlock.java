package io.github.asgerjon.mcfly.end;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.base.McFlyBlock;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.particle.McFlyParticles;
import io.github.asgerjon.mcfly.particle.McFlyParticlesClient;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 26.3's end rod (`EndRodBlock`, `RodBlock`; SPEC-modern-movement.md §84): meta its facing as 1.7.10's sides (0 down to
 * 5 east), placed facing the clicked face but the other way on an end rod facing that face; 4 by 4 across its whole
 * length, collision too; light 14, nothing to break it, wood sounds; pushable; 1 tick in 5 an end rod particle drifts
 * off it. Not a block mobs walk through.
 */
public class EndRodBlock extends McFlyBlock implements ModelledBlock {

	public EndRodBlock() {
		super(Material.circuits);
		setNames("end_rod");
		setHardness(0.0F);
		this.blockResistance = 0.0F;
		this.lightValue = 14;
		setStepSound(soundTypeWood);
	}

	@Override
	public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
		ForgeDirection clicked = ForgeDirection.getOrientation(side);
		// the block clicked on: an end rod facing the clicked face turns this one the other way (26.3's
		// getStateForPlacement)
		int bx = x - clicked.offsetX;
		int by = y - clicked.offsetY;
		int bz = z - clicked.offsetZ;
		if (world.getBlock(bx, by, bz) == this && world.getBlockMetadata(bx, by, bz) == side) {
			return clicked.getOpposite().ordinal();
		}
		return side;
	}

	/** 26.3's `RodBlock` shape: 4 by 4 (6 to 10) across, the whole block along its facing's axis. */
	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		bounds(world.getBlockMetadata(x, y, z));
	}

	private void bounds(int meta) {
		float a = 6.0F / 16.0F;
		float b = 10.0F / 16.0F;
		switch (ForgeDirection.getOrientation(meta)) {
			case NORTH:
			case SOUTH:
				setBlockBounds(a, a, 0.0F, b, b, 1.0F);
				break;
			case WEST:
			case EAST:
				setBlockBounds(0.0F, a, a, 1.0F, b, b);
				break;
			default:
				setBlockBounds(a, 0.0F, a, b, 1.0F, b);
				break;
		}
	}

	@Override
	public void setBlockBoundsForItemRender() {
		bounds(1);
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		setBlockBoundsBasedOnState(world, x, y, z);
		return super.getCollisionBoundingBoxFromPool(world, x, y, z);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List list, Entity entity) {
		setBlockBoundsBasedOnState(world, x, y, z);
		super.addCollisionBoxesToList(world, x, y, z, mask, list, entity);
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
		setBlockBoundsBasedOnState(world, x, y, z);
		return super.getSelectedBoundingBoxFromPool(world, x, y, z);
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	/** 26.3's push reaction is normal: pistons move it (1.7.10's circuits material would break it). */
	@Override
	public int getMobilityFlag() {
		return 0;
	}

	/** 26.3's `isPathfindable` false. */
	@Override
	public boolean getBlocksMovement(IBlockAccess world, int x, int y, int z) {
		return false;
	}

	/** 26.3's `animateTick`, its random asked in the same order. */
	@Override
	@SideOnly(Side.CLIENT)
	public void randomDisplayTick(World world, int x, int y, int z, Random random) {
		ForgeDirection facing = ForgeDirection.getOrientation(world.getBlockMetadata(x, y, z));
		double px = x + 0.55 - random.nextFloat() * 0.1F;
		double py = y + 0.55 - random.nextFloat() * 0.1F;
		double pz = z + 0.55 - random.nextFloat() * 0.1F;
		double r = 0.4F - (random.nextFloat() + random.nextFloat()) * 0.4F;
		if (random.nextInt(5) == 0) {
			McFlyParticlesClient.add(McFlyParticles.END_ROD, px + facing.offsetX * r, py + facing.offsetY * r, pz + facing.offsetZ * r,
				random.nextGaussian() * 0.005, random.nextGaussian() * 0.005, random.nextGaussian() * 0.005);
		}
	}

	@Override
	public int getRenderType() {
		return McFlyRenderIds.MODEL;
	}

	/** 26.3's blockstate: the model turned to its facing (up as it is, down x 180, the sides x 90 and y by side). */
	@Override
	public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
		return Collections.singletonList(part(world.getBlockMetadata(x, y, z)));
	}

	private static ModelPart part(int meta) {
		switch (ForgeDirection.getOrientation(meta)) {
			case DOWN:
				return new ModelPart("end_rod", 180, 0, false);
			case NORTH:
				return new ModelPart("end_rod", 90, 0, false);
			case SOUTH:
				return new ModelPart("end_rod", 90, 180, false);
			case WEST:
				return new ModelPart("end_rod", 90, 270, false);
			case EAST:
				return new ModelPart("end_rod", 90, 90, false);
			default:
				return new ModelPart("end_rod");
		}
	}

	@Override
	public List<ModelPart> itemParts(int meta) {
		return Collections.singletonList(part(1));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon modelIcon(String texture) {
		return blockIcon;
	}
}
