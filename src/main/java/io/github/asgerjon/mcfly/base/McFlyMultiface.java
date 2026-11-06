package io.github.asgerjon.mcfly.base;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.model.ModelPart;
import io.github.asgerjon.mcfly.model.ModelledBlock;
import io.github.asgerjon.mcfly.registry.McFlyRenderIds;
import io.github.asgerjon.mcfly.registry.McFlyTab;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A block of faces (SPEC-modern-movement.md §90.3), 26.3's `MultifaceBlock` and `MultifaceSpreader`: up to six faces,
 * each lying on a side of its space against a full face of the neighbour there, kept in a {@link McFlyMultifaceTile} (bit
 * i the face toward `ForgeDirection` i: down, up, north, south, west, east, 26.3's `Direction` order too). A face goes
 * when its neighbour no longer holds it, the block with its last face, without drops (26.3's `updateShape`); shears take
 * one a face, nothing else does. Its shape is each face's 1/16 slab, the ray picking the face looked at; drawn as 26.3's
 * multipart, one model turned to each face; outlined as the faces' union (`model.client.ShapeOutline`). No waterlogging
 * (1.7.10 has none), so it spreads into air only.
 */
public abstract class McFlyMultiface extends Block implements ITileEntityProvider, ModelledBlock, IShearable {

	public static final float THICKNESS = 1.0F / 16.0F;
	private static final int[][] OFFSET = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

	protected McFlyMultiface(Material material) {
		super(material);
		this.isBlockContainer = true;
		setCreativeTab(McFlyTab.TAB);
	}

	// ---------------------------------------------------------------- faces

	public static boolean has(int faces, int face) {
		return (faces & 1 << face) != 0;
	}

	/** The faces at a place (none where no tile entity of ours is). */
	public static int faces(IBlockAccess world, int x, int y, int z) {
		TileEntity tile = world.getTileEntity(x, y, z);
		return tile instanceof McFlyMultifaceTile multiface ? multiface.faces() : 0;
	}

	/** These faces there: none left, the block is air (no drops, as 26.3's state without faces). */
	public void setFaces(World world, int x, int y, int z, int faces) {
		if (faces == 0) {
			world.setBlockToAir(x, y, z);
			return;
		}
		TileEntity tile = world.getTileEntity(x, y, z);
		if (tile instanceof McFlyMultifaceTile multiface) {
			multiface.setFaces(faces);
			world.markBlockForUpdate(x, y, z);
		}
	}

	/** Whether the neighbour toward that face holds a face there: its own facing side full (26.3's `canAttachTo`). */
	public static boolean canAttach(IBlockAccess world, int x, int y, int z, int face) {
		int nx = x + OFFSET[face][0], ny = y + OFFSET[face][1], nz = z + OFFSET[face][2];
		return world.getBlock(nx, ny, nz).isSideSolid(world, nx, ny, nz, ForgeDirection.getOrientation(face).getOpposite());
	}

	/** 26.3's `isValidStateForPlacement`: the face not there yet, and held. */
	public boolean canPlaceFace(IBlockAccess world, int x, int y, int z, int face) {
		return (world.getBlock(x, y, z) != this || !has(faces(world, x, y, z), face)) && canAttach(world, x, y, z, face);
	}

	/** Adds a face there, into air (a new block) or this block; false if it can't go there. */
	public boolean addFace(World world, int x, int y, int z, int face, int flags) {
		Block there = world.getBlock(x, y, z);
		if (there != this && !there.isAir(world, x, y, z) || !canPlaceFace(world, x, y, z, face)) {
			return false;
		}
		int old = there == this ? faces(world, x, y, z) : 0;
		if (there != this && !world.setBlock(x, y, z, this, 0, flags)) {
			return false;
		}
		setFaces(world, x, y, z, old | 1 << face);
		return true;
	}

	/** Whether a lichen in hand may still go into this block: a face free (26.3's `canBeReplaced`). */
	public static boolean hasVacantFace(int faces) {
		return faces != 63;
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
		if (world.isRemote) {
			return;
		}
		int faces = faces(world, x, y, z), kept = faces;
		for (int face = 0; face < 6; face++) {
			if (has(faces, face) && !canAttach(world, x, y, z, face)) {
				kept &= ~(1 << face);
			}
		}
		if (kept != faces || kept == 0) {
			setFaces(world, x, y, z, kept);
		}
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new McFlyMultifaceTile();
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		super.breakBlock(world, x, y, z, block, meta);
		world.removeTileEntity(x, y, z);
	}

	@Override
	public boolean onBlockEventReceived(World world, int x, int y, int z, int id, int data) {
		super.onBlockEventReceived(world, x, y, z, id, data);
		TileEntity tile = world.getTileEntity(x, y, z);
		return tile != null && tile.receiveClientEvent(id, data);
	}

	// ---------------------------------------------------------------- spreading (26.3's MultifaceSpreader, its default config)

	private MultifaceSpreader spreader;

	/** Its spreader (26.3's `getSpreader`): the default config unless it says. */
	public MultifaceSpreader spreader() {
		if (spreader == null) {
			spreader = new MultifaceSpreader(this);
		}
		return spreader;
	}

	/** 26.3's `isValidBonemealTarget`: some face can spread somewhere. */
	public boolean canSpread(IBlockAccess world, int x, int y, int z) {
		int faces = faces(world, x, y, z);
		for (int from = 0; from < 6; from++) {
			if (spreader().canSpreadInAnyDirection(world, x, y, z, this, faces, from)) {
				return true;
			}
		}
		return false;
	}

	/** 26.3's `spreadFromRandomFaceTowardRandomDirection`: faces in a random order, each toward directions in a random order, the first spread made. */
	public boolean spreadFromRandomFace(World world, int x, int y, int z, Random random) {
		return spreader().spreadFromRandomFaceTowardRandomDirection(world, x, y, z, this, faces(world, x, y, z), random) != null;
	}

	// ---------------------------------------------------------------- shape

	/** A face's 1/16 slab in the block's own space. */
	public static AxisAlignedBB faceBox(int face) {
		float t = THICKNESS;
		switch (face) {
			case 0:
				return AxisAlignedBB.getBoundingBox(0, 0, 0, 1, t, 1);
			case 1:
				return AxisAlignedBB.getBoundingBox(0, 1 - t, 0, 1, 1, 1);
			case 2:
				return AxisAlignedBB.getBoundingBox(0, 0, 0, 1, 1, t);
			case 3:
				return AxisAlignedBB.getBoundingBox(0, 0, 1 - t, 1, 1, 1);
			case 4:
				return AxisAlignedBB.getBoundingBox(0, 0, 0, t, 1, 1);
			default:
				return AxisAlignedBB.getBoundingBox(1 - t, 0, 0, 1, 1, 1);
		}
	}

	/** The faces' slabs at a place, in the block's own space. */
	public static List<AxisAlignedBB> faceBoxes(int faces) {
		List<AxisAlignedBB> boxes = new ArrayList<>();
		for (int face = 0; face < 6; face++) {
			if (has(faces, face)) {
				boxes.add(faceBox(face));
			}
		}
		return boxes;
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		int faces = faces(world, x, y, z);
		if (faces == 0) {
			setBlockBounds(0, 0, 0, 1, 1, 1);
			return;
		}
		double minX = 1, minY = 1, minZ = 1, maxX = 0, maxY = 0, maxZ = 0;
		for (AxisAlignedBB box : faceBoxes(faces)) {
			minX = Math.min(minX, box.minX);
			minY = Math.min(minY, box.minY);
			minZ = Math.min(minZ, box.minZ);
			maxX = Math.max(maxX, box.maxX);
			maxY = Math.max(maxY, box.maxY);
			maxZ = Math.max(maxZ, box.maxZ);
		}
		setBlockBounds((float) minX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ);
	}

	@Override
	public void setBlockBoundsForItemRender() {
		setBlockBounds(0, 0, 0, 1, 1, 1);
	}

	/** The ray against each face's slab: the face looked at (26.3's shape is their union). */
	@Override
	public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 start, Vec3 end) {
		MovingObjectPosition nearest = null;
		double best = Double.MAX_VALUE;
		for (AxisAlignedBB box : faceBoxes(faces(world, x, y, z))) {
			MovingObjectPosition hit = box.getOffsetBoundingBox(x, y, z).calculateIntercept(start, end);
			if (hit != null) {
				double distance = hit.hitVec.squareDistanceTo(start);
				if (distance < best) {
					best = distance;
					nearest = new MovingObjectPosition(x, y, z, hit.sideHit, hit.hitVec);
				}
			}
		}
		return nearest;
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

	@Override
	public int getRenderType() {
		return McFlyRenderIds.MODEL_FLAT_ITEM;
	}

	// ---------------------------------------------------------------- drops: one a face to shears, nothing otherwise

	@Override
	public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune) {
		return new ArrayList<>();
	}

	@Override
	public boolean isShearable(ItemStack item, IBlockAccess world, int x, int y, int z) {
		return true;
	}

	@Override
	public ArrayList<ItemStack> onSheared(ItemStack item, IBlockAccess world, int x, int y, int z, int fortune) {
		ArrayList<ItemStack> drops = new ArrayList<>();
		int count = Integer.bitCount(faces(world, x, y, z));
		if (count > 0) {
			drops.add(new ItemStack(this, count));
		}
		return drops;
	}

	// ---------------------------------------------------------------- the model: one turned to each face

	/** The model of a face toward north, turned to each face by 26.3's blockstate (down, up, north, south, west, east). */
	protected abstract ModelPart[] faceParts();

	@Override
	public List<ModelPart> modelParts(IBlockAccess world, int x, int y, int z) {
		int faces = faces(world, x, y, z);
		ModelPart[] parts = faceParts();
		List<ModelPart> found = new ArrayList<>();
		for (int face = 0; face < 6; face++) {
			if (has(faces, face)) {
				found.add(parts[face]);
			}
		}
		return found;
	}

	@Override
	public List<ModelPart> itemParts(int meta) {
		return Collections.singletonList(faceParts()[2]);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public net.minecraft.util.IIcon modelIcon(String texture) {
		return blockIcon;
	}
}
