package ganymedes01.etfuturum.blocks;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.ducks.IModernChest;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Chests side by side, as 26.3's (SPEC-modern-movement.md §65; 26.3's `ChestBlock`, `ChestType`): a chest has a facing
 * (1.7.10's meta, 2 to 5, as everything reads it) and a type, single, left or right, kept in its block entity
 * (`MixinTileEntityChest`). A left chest's other half is at its facing's clockwise side, a right one's at the
 * counter-clockwise side; only that one is its double. A chest with no type (an old world's) is converted from
 * 1.7.10's own pairing when it first ticks or a neighbour asks.
 */
public final class ModernChests {

	public static final int UNKNOWN = 0;
	public static final int SINGLE = 1;
	public static final int LEFT = 2;
	public static final int RIGHT = 3;
	public static final String[] NAMES = {"unknown", "single", "left", "right"};

	/** By side/facing (2 north, 3 south, 4 west, 5 east): the step to that side. */
	private static final int[] DX = {0, 0, 0, 0, -1, 1};
	private static final int[] DZ = {0, 0, -1, 1, 0, 0};

	/** The clicked face from `onBlockPlaced`, for `onBlockPlacedBy` right after (26.3's clicked face). */
	public static final ThreadLocal<Integer> CLICKED = new ThreadLocal<>();

	private ModernChests() {
	}

	public static boolean isOn() {
		return McFlyConfig.modernChestPlacement;
	}

	/** The chest's type here: single, left, right, or unknown (an old world's, not converted yet; or no chest). */
	public static String typeOf(IBlockAccess world, int x, int y, int z) {
		TileEntity tile = world.getTileEntity(x, y, z);
		return tile instanceof IModernChest chest ? NAMES[chest.etfu$chestType()] : "unknown";
	}

	// ---------------------------------------------------------------- directions

	/** 26.3's `getClockWise`: north to east, east to south, south to west, west to north. */
	static int clockwise(int facing) {
		return switch (facing) {
			case 2 -> 5;
			case 5 -> 3;
			case 3 -> 4;
			default -> 2;
		};
	}

	static int counterClockwise(int facing) {
		return switch (facing) {
			case 2 -> 4;
			case 4 -> 3;
			case 3 -> 5;
			default -> 2;
		};
	}

	static int opposite(int side) {
		return side ^ 1;
	}

	/** 26.3's `getConnectedDirection`: where the other half is, or -1 for a single chest. */
	static int partnerSide(int facing, int type) {
		return type == LEFT ? clockwise(facing) : type == RIGHT ? counterClockwise(facing) : -1;
	}

	private static boolean sameAxis(int a, int b) {
		return a >> 1 == b >> 1;
	}

	// ---------------------------------------------------------------- the type

	private static TileEntityChest chest(IBlockAccess world, int x, int y, int z, Block kind) {
		if (world.getBlock(x, y, z) != kind) {
			return null;
		}
		TileEntity tile = world.getTileEntity(x, y, z);
		return tile instanceof TileEntityChest chest ? chest : null;
	}

	private static int type(TileEntityChest chest) {
		return ((IModernChest) chest).etfu$chestType();
	}

	/** Sets the type, the 1.7.10 neighbour fields to be worked out again, and (on the server) sends it to clients. */
	static void setType(TileEntityChest chest, int type) {
		if (type(chest) == type) {
			return;
		}
		((IModernChest) chest).etfu$setChestType(type);
		chest.updateContainingBlockInfo();
		World world = chest.getWorldObj();
		if (world != null && !world.isRemote) {
			chest.markDirty();
			world.markBlockForUpdate(chest.xCoord, chest.yCoord, chest.zCoord);
		}
	}

	/** The type, converting an old world's chest first (in a world; a client's chunk cache only reads). */
	private static int known(IBlockAccess world, TileEntityChest chest, Block kind) {
		if (type(chest) == UNKNOWN && world instanceof World real) {
			convert(real, chest, kind);
		}
		return type(chest);
	}

	/**
	 * An old world's chest, with no type: as 1.7.10 joined it, its first neighbour of its kind in 1.7.10's order (west,
	 * east, north, south), when that neighbour is at its left or right and faces the same way; else single.
	 */
	public static void convert(World world, TileEntityChest chest, Block kind) {
		if (type(chest) != UNKNOWN) {
			return;
		}
		int facing = world.getBlockMetadata(chest.xCoord, chest.yCoord, chest.zCoord);
		for (int side : new int[]{4, 5, 2, 3}) {
			TileEntityChest neighbour = chest(world, chest.xCoord + DX[side], chest.yCoord, chest.zCoord + DZ[side], kind);
			if (neighbour == null) {
				continue;
			}
			int own = side == clockwise(facing) ? LEFT : side == counterClockwise(facing) ? RIGHT : SINGLE;
			int theirs = type(neighbour);
			boolean faces = world.getBlockMetadata(neighbour.xCoord, neighbour.yCoord, neighbour.zCoord) == facing;
			if (own != SINGLE && faces && (theirs == UNKNOWN || theirs == (own == LEFT ? RIGHT : LEFT))) {
				setType(chest, own);
				setType(neighbour, own == LEFT ? RIGHT : LEFT);
				return;
			}
			break;
		}
		setType(chest, SINGLE);
	}

	/** The other half, when both halves agree they are one double; else null. */
	public static TileEntityChest partner(IBlockAccess world, TileEntityChest chest, Block kind) {
		int type = known(world, chest, kind);
		int facing = world.getBlockMetadata(chest.xCoord, chest.yCoord, chest.zCoord);
		int side = partnerSide(facing, type);
		if (side < 0) {
			return null;
		}
		TileEntityChest other = chest(world, chest.xCoord + DX[side], chest.yCoord, chest.zCoord + DZ[side], kind);
		if (other == null || world.getBlockMetadata(other.xCoord, other.yCoord, other.zCoord) != facing) {
			return null;
		}
		return known(world, other, kind) == (type == LEFT ? RIGHT : LEFT) ? other : null;
	}

	/** The side the other half is at, for 1.7.10's neighbour fields and bounds, or -1. */
	public static int partnerSideOf(IBlockAccess world, TileEntityChest chest, Block kind) {
		TileEntityChest other = partner(world, chest, kind);
		return other == null ? -1 : partnerSide(world.getBlockMetadata(chest.xCoord, chest.yCoord, chest.zCoord), type(chest));
	}

	// ---------------------------------------------------------------- placing

	/**
	 * 26.3's `getStateForPlacement`: facing the placer; sneak-clicking the side of a single chest that faces across the
	 * clicked axis joins it, with its facing; otherwise, not sneaking, a single chest of the same facing at the left
	 * (then the right) makes it a half.
	 */
	public static void place(World world, int x, int y, int z, EntityLivingBase placer, Block kind) {
		Integer clickedSide = CLICKED.get();
		CLICKED.remove();
		int clicked = clickedSide == null ? 1 : clickedSide;
		int look = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
		int facing = new int[]{2, 5, 3, 4}[look];
		boolean sneaking = placer.isSneaking();
		int type = SINGLE;
		if (clicked >= 2 && sneaking) {
			int towards = opposite(clicked);
			Integer theirs = candidateFacing(world, x + DX[towards], y, z + DZ[towards], kind);
			if (theirs != null && !sameAxis(theirs, clicked)) {
				facing = theirs;
				type = counterClockwise(facing) == towards ? RIGHT : LEFT;
			}
		}
		if (type == SINGLE && !sneaking) {
			Integer left = candidateFacing(world, x + DX[clockwise(facing)], y, z + DZ[clockwise(facing)], kind);
			Integer right = candidateFacing(world, x + DX[counterClockwise(facing)], y, z + DZ[counterClockwise(facing)], kind);
			if (left != null && left == facing) {
				type = LEFT;
			} else if (right != null && right == facing) {
				type = RIGHT;
			}
		}
		world.setBlockMetadataWithNotify(x, y, z, facing, 3);
		TileEntityChest chest = chest(world, x, y, z, kind);
		if (chest == null) {
			return;
		}
		setType(chest, type);
		int side = partnerSide(facing, type);
		if (side >= 0) {
			TileEntityChest other = chest(world, x + DX[side], y, z + DZ[side], kind);
			if (other != null && type(other) == SINGLE) {
				setType(other, type == LEFT ? RIGHT : LEFT);
			}
		}
	}

	/** 26.3's `candidatePartnerFacing`: a single chest of this kind's facing, or null. */
	private static Integer candidateFacing(World world, int x, int y, int z, Block kind) {
		TileEntityChest chest = chest(world, x, y, z, kind);
		return chest != null && known(world, chest, kind) == SINGLE ? world.getBlockMetadata(x, y, z) : null;
	}

	// ---------------------------------------------------------------- neighbours

	/**
	 * 26.3's `updateShape`, on a neighbour change (1.7.10 doesn't say which side, so all four): a single chest next to
	 * a half of the same facing that points at it becomes its other side; a half whose other side holds no chest of its
	 * kind any more becomes single.
	 */
	public static void neighbourChanged(World world, int x, int y, int z, Block kind) {
		TileEntityChest chest = chest(world, x, y, z, kind);
		if (chest == null) {
			return;
		}
		int type = known(world, chest, kind);
		int facing = world.getBlockMetadata(x, y, z);
		for (int side = 2; side <= 5; side++) {
			TileEntityChest neighbour = chest(world, x + DX[side], y, z + DZ[side], kind);
			if (neighbour != null) {
				int theirs = known(world, neighbour, kind);
				int theirFacing = world.getBlockMetadata(neighbour.xCoord, neighbour.yCoord, neighbour.zCoord);
				if (type == SINGLE && theirs != SINGLE && theirFacing == facing && partnerSide(theirFacing, theirs) == opposite(side)) {
					type = theirs == LEFT ? RIGHT : LEFT;
					setType(chest, type);
				}
			} else if (partnerSide(facing, type) == side) {
				type = SINGLE;
				setType(chest, type);
			}
		}
	}

	// ---------------------------------------------------------------- inventory and shape

	/**
	 * 1.7.10's `func_149951_m` with only the other half: the double's inventory (the half at the west or north first,
	 * as 1.7.10 ordered it), the chest's alone for a single one; none when a half is blocked (a solid block above, a
	 * sitting cat).
	 */
	public static IInventory inventory(World world, int x, int y, int z, Block kind, Blocked blocked) {
		TileEntityChest chest = chest(world, x, y, z, kind);
		if (chest == null || blocked.blocked(world, x, y, z)) {
			return null;
		}
		TileEntityChest other = partner(world, chest, kind);
		if (other == null) {
			return chest;
		}
		if (blocked.blocked(world, other.xCoord, other.yCoord, other.zCoord)) {
			return null;
		}
		int side = partnerSide(world.getBlockMetadata(x, y, z), type(chest));
		return side == 4 || side == 2 ? new InventoryLargeChest("container.chestDouble", other, chest) : new InventoryLargeChest("container.chestDouble", chest, other);
	}

	/** Whether a chest's lid can't open here (1.7.10's checks). */
	@FunctionalInterface
	public interface Blocked {
		boolean blocked(World world, int x, int y, int z);
	}

	/** The chest's box: 26.3's single box, or the half box reaching the other half. */
	public static float[] bounds(IBlockAccess world, int x, int y, int z, Block kind) {
		TileEntityChest chest = chest(world, x, y, z, kind);
		int side = chest == null ? -1 : partnerSideOf(world, chest, kind);
		float minX = side == 4 ? 0.0F : 0.0625F;
		float maxX = side == 5 ? 1.0F : 0.9375F;
		float minZ = side == 2 ? 0.0F : 0.0625F;
		float maxZ = side == 3 ? 1.0F : 0.9375F;
		return new float[]{minX, 0.0F, minZ, maxX, 0.875F, maxZ};
	}
}
