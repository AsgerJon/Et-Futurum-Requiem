package ganymedes01.etfuturum.placement;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The collision boxes a block will have once placed, for the placement check: modern tests the placed state's shape
 * (1.13+), while 1.7.10 asked the block for its box before it existed, which for a block whose box depends on its
 * metadata is whatever it last computed, often a full cube. Known here: ladders (orientation from the side),
 * trapdoors (orientation from the side, half from the click height, and open when the spot is powered, as modern
 * places them), stairs (upside down from the click, facing from the placer's yaw, corners from the neighbours; §53.8)
 * and single slabs (half from the click). Any other block takes the vanilla check.
 */
public final class PlacedShape {

	/** 1.7.10's stair metadata for the placer facing south, west, north, east (BlockStairs.onBlockPlacedBy). */
	private static final int[] STAIR_FACING = {2, 1, 3, 0};

	private PlacedShape() {
	}

	/**
	 * The boxes of the block as it would be placed, or null for a block this doesn't know. The placer gives a stair its
	 * facing; null leaves it facing east.
	 */
	public static List<AxisAlignedBB> boxesAsPlaced(Block block, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata, Entity placer) {
		if (block instanceof BlockLadder ladder) {
			int meta = ladder.onBlockPlaced(world, x, y, z, side, hitX, hitY, hitZ, metadata);
			if (meta < 2 || meta > 5) {
				return null;
			}
			ladder.func_149797_b(meta);
			return Collections.singletonList(bounds(block, x, y, z));
		}
		if (block instanceof BlockTrapDoor trapdoor) {
			trapdoor.func_150117_b(placedTrapdoorMeta(trapdoor, world, x, y, z, side, hitX, hitY, hitZ, metadata));
			return Collections.singletonList(bounds(block, x, y, z));
		}
		if (block instanceof BlockStairs stairs) {
			int meta = stairs.onBlockPlaced(world, x, y, z, side, hitX, hitY, hitZ, metadata) & 4;
			if (placer != null) {
				meta |= STAIR_FACING[MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3];
			}
			// the stair's own box code, as its collision boxes, with the stair standing there already
			IBlockAccess placed = new WithBlock(world, x, y, z, stairs, meta);
			List<AxisAlignedBB> boxes = new ArrayList<>();
			stairs.func_150147_e(placed, x, y, z);
			boxes.add(bounds(stairs, x, y, z));
			boolean upper = stairs.func_150145_f(placed, x, y, z);
			boxes.add(bounds(stairs, x, y, z));
			if (upper && stairs.func_150144_g(placed, x, y, z)) {
				boxes.add(bounds(stairs, x, y, z));
			}
			stairs.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
			return boxes;
		}
		if (block instanceof BlockSlab slab && !slab.isOpaqueCube()) {
			boolean top = (slab.onBlockPlaced(world, x, y, z, side, hitX, hitY, hitZ, metadata) & 8) != 0;
			return Collections.singletonList(AxisAlignedBB.getBoundingBox(x, top ? y + 0.5D : y, z, x + 1, top ? y + 1 : y + 0.5D, z + 1));
		}
		return null;
	}

	/**
	 * Whether none of the boxes holds an entity that stops building, but the one left out. An overlap thinner than
	 * 1.0E-7 doesn't count, as modern's shapes merge edges that close (Shapes.EPSILON): a hitbox is its position plus
	 * a float half width (0.30000001), so one standing against a step can reach that far into the corner beside it.
	 */
	public static boolean free(World world, List<AxisAlignedBB> boxes, Entity leftOut) {
		for (AxisAlignedBB box : boxes) {
			if (!world.checkNoEntityCollision(box.contract(1.0E-7D, 1.0E-7D, 1.0E-7D), leftOut)) {
				return false;
			}
		}
		return true;
	}

	/** A trapdoor's placed metadata: vanilla's orientation and half, open when the spot is powered (modern's placement state). */
	public static int placedTrapdoorMeta(BlockTrapDoor trapdoor, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata) {
		int meta = trapdoor.onBlockPlaced(world, x, y, z, side, hitX, hitY, hitZ, metadata);
		return world.isBlockIndirectlyGettingPowered(x, y, z) ? meta | 4 : meta;
	}

	/** The entity the server's check leaves out: never the placing player once the modern block use packets count it. */
	public static Entity excluded(Entity placer) {
		return ConfigMixins.modernBlockUsePackets && placer instanceof EntityPlayer ? null : placer;
	}

	private static AxisAlignedBB bounds(Block block, int x, int y, int z) {
		return AxisAlignedBB.getBoundingBox(x + block.getBlockBoundsMinX(), y + block.getBlockBoundsMinY(), z + block.getBlockBoundsMinZ(),
				x + block.getBlockBoundsMaxX(), y + block.getBlockBoundsMaxY(), z + block.getBlockBoundsMaxZ());
	}

	/**
	 * The world with one more block in it, for a block's own shape code to read before the block is placed. The
	 * client-only reads are client-only here too, as in vanilla's own block accesses.
	 */
	private static final class WithBlock implements IBlockAccess {
		private final IBlockAccess world;
		private final int x, y, z, meta;
		private final Block block;

		WithBlock(IBlockAccess world, int x, int y, int z, Block block, int meta) {
			this.world = world;
			this.x = x;
			this.y = y;
			this.z = z;
			this.block = block;
			this.meta = meta;
		}

		private boolean here(int x, int y, int z) {
			return x == this.x && y == this.y && z == this.z;
		}

		@Override
		public Block getBlock(int x, int y, int z) {
			return here(x, y, z) ? block : world.getBlock(x, y, z);
		}

		@Override
		public TileEntity getTileEntity(int x, int y, int z) {
			return here(x, y, z) ? null : world.getTileEntity(x, y, z);
		}

		@Override
		@SideOnly(Side.CLIENT)
		public int getLightBrightnessForSkyBlocks(int x, int y, int z, int min) {
			return world.getLightBrightnessForSkyBlocks(x, y, z, min);
		}

		@Override
		public int getBlockMetadata(int x, int y, int z) {
			return here(x, y, z) ? meta : world.getBlockMetadata(x, y, z);
		}

		@Override
		public int isBlockProvidingPowerTo(int x, int y, int z, int direction) {
			return world.isBlockProvidingPowerTo(x, y, z, direction);
		}

		@Override
		public boolean isAirBlock(int x, int y, int z) {
			return !here(x, y, z) && world.isAirBlock(x, y, z);
		}

		@Override
		@SideOnly(Side.CLIENT)
		public BiomeGenBase getBiomeGenForCoords(int x, int z) {
			return world.getBiomeGenForCoords(x, z);
		}

		@Override
		@SideOnly(Side.CLIENT)
		public int getHeight() {
			return world.getHeight();
		}

		@Override
		@SideOnly(Side.CLIENT)
		public boolean extendedLevelsInChunkCache() {
			return world.extendedLevelsInChunkCache();
		}

		@Override
		public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean _default) {
			return world.isSideSolid(x, y, z, side, _default);
		}
	}
}
