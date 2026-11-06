package io.github.asgerjon.mcfly.wood;

import io.github.asgerjon.mcfly.base.McFlyWall;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * 26.3's `FenceBlock.connectsTo` (SPEC-modern-movement.md §91.5, step C), for McFly's fences and every `BlockFence`
 * whose own joining is 1.7.10's (vanilla's oak and nether brick fences): a fence of the same kind (wooden with wooden,
 * `#wooden_fences`), a gate across that side, or a sturdy face there but for 26.3's exceptions.
 */
public final class FenceJoin {

	private FenceJoin() {
	}

	/**
	 * Whether the fence joins the neighbour at (x, y, z), which lies that way from it (null when the caller doesn't say:
	 * 1.7.10's `canConnectFenceTo` has no side; then any gate joins, as 1.7.10's did).
	 */
	public static boolean joins(Block fence, IBlockAccess world, int x, int y, int z, ForgeDirection direction) {
		Block neighbour = world.getBlock(x, y, z);
		if (neighbour instanceof BlockFence) {
			return wooden(neighbour) == wooden(fence);
		}
		if (neighbour instanceof BlockFenceGate) {
			if (direction == null) {
				return true;
			}
			// 1.7.10's gate meta: 0 south, 1 west, 2 north, 3 east; it spans the axis across its facing
			boolean gateFacesAlongZ = (world.getBlockMetadata(x, y, z) & 1) == 0;
			return gateFacesAlongZ == (direction.offsetX != 0);
		}
		if (McFlyWall.isExceptionForConnection(neighbour)) {
			return false;
		}
		ForgeDirection face = direction != null ? direction.getOpposite() : ForgeDirection.UNKNOWN;
		return direction != null ? neighbour.isSideSolid(world, x, y, z, face)
			: neighbour.getMaterial().isOpaque() && neighbour.renderAsNormalBlock();
	}

	private static boolean wooden(Block fence) {
		return fence.getMaterial() == Material.wood;
	}

	/** Whether the fence's joining is 1.7.10's own, so 26.3's takes it (vanilla's: the class itself, not a mod's subclass). */
	public static boolean ownJoining(BlockFence fence) {
		return fence.getClass() == BlockFence.class;
	}

	/** The side a neighbour that far away is on (x then z), or null when it's none of the four. */
	public static ForgeDirection direction(int dx, int dz) {
		if (dz == 0 && (dx == 1 || dx == -1)) {
			return dx == 1 ? ForgeDirection.EAST : ForgeDirection.WEST;
		}
		if (dx == 0 && (dz == 1 || dz == -1)) {
			return dz == 1 ? ForgeDirection.SOUTH : ForgeDirection.NORTH;
		}
		return null;
	}
}
