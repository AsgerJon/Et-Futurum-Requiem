package ganymedes01.etfuturum.core.utils;

import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.block.Block;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Which block faces can hold a button or a lever (SPEC-modern-movement.md §53.4). Modern asks whether the face is full:
 * the block's support shape, which is its collision shape unless the block says otherwise, covers the whole face. So
 * glass, ice, glowstone and TNT hold them; leaves say they support nothing. Forge's isSideSolid (opaque full cubes,
 * slab tops, stair backs, farmland's sides) keeps holding what it held.
 */
public final class BlockSupport {

	private static final double EPSILON = 1.0E-4D;

	private BlockSupport() {
	}

	/** Forge's answer, and with buttonsOnFullFaces any full face. */
	public static boolean canAttachTo(World world, int x, int y, int z, ForgeDirection face) {
		return world.isSideSolid(x, y, z, face) || ConfigMixins.buttonsOnFullFaces && isFaceFull(world, x, y, z, face);
	}

	/** Whether the block's collision box covers the whole face; leaves never hold anything. */
	public static boolean isFaceFull(World world, int x, int y, int z, ForgeDirection face) {
		Block block = world.getBlock(x, y, z);
		if (block.isAir(world, x, y, z) || block.isLeaves(world, x, y, z)) {
			return false;
		}
		AxisAlignedBB box = block.getCollisionBoundingBoxFromPool(world, x, y, z);
		if (box == null) {
			return false;
		}
		boolean acrossX = box.minX <= x + EPSILON && box.maxX >= x + 1 - EPSILON;
		boolean acrossY = box.minY <= y + EPSILON && box.maxY >= y + 1 - EPSILON;
		boolean acrossZ = box.minZ <= z + EPSILON && box.maxZ >= z + 1 - EPSILON;
		switch (face) {
			case DOWN:
				return acrossX && acrossZ && box.minY <= y + EPSILON;
			case UP:
				return acrossX && acrossZ && box.maxY >= y + 1 - EPSILON;
			case NORTH:
				return acrossX && acrossY && box.minZ <= z + EPSILON;
			case SOUTH:
				return acrossX && acrossY && box.maxZ >= z + 1 - EPSILON;
			case WEST:
				return acrossY && acrossZ && box.minX <= x + EPSILON;
			case EAST:
				return acrossY && acrossZ && box.maxX >= x + 1 - EPSILON;
			default:
				return false;
		}
	}
}
