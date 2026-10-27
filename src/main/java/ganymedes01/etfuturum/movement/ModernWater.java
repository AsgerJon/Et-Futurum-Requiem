package ganymedes01.etfuturum.movement;

import ganymedes01.etfuturum.mixins.early.modernmovement.AccessorBlockLiquid;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.IFluidBlock;

/**
 * Modern water detection for players (§9). 1.7.10 squeezes the hitbox by 0.4 at the top and bottom before looking for
 * water, which breaks on the 0.6 tall swimming hitbox. This scans the real hitbox (shrunk by 0.001) against real water
 * surface heights instead.
 */
public final class ModernWater {

	private static final double FLUID_BOX_MARGIN = 0.001D;
	private static final double CURRENT_STRENGTH = 0.014D;
	private static final double MIN_CURRENT = 0.0045D;
	public static final double JUMP_THRESHOLD = 0.4D;

	private ModernWater() {
	}

	/**
	 * Scans the water around the player, stores the water height and whether the eyes are in water, and applies the
	 * current. Returns whether the player is touching water.
	 */
	public static boolean update(EntityPlayer player, boolean pushedByWater) {
		IPosePlayer posePlayer = (IPosePlayer) player;
		World world = player.worldObj;
		AxisAlignedBB box = player.boundingBox.contract(FLUID_BOX_MARGIN, FLUID_BOX_MARGIN, FLUID_BOX_MARGIN);
		int minX = MathHelper.floor_double(box.minX);
		int minY = MathHelper.floor_double(box.minY);
		int minZ = MathHelper.floor_double(box.minZ);
		int maxX = MathHelper.ceiling_double_int(box.maxX) - 1;
		int maxY = MathHelper.ceiling_double_int(box.maxY) - 1;
		int maxZ = MathHelper.ceiling_double_int(box.maxZ) - 1;

		if (!world.checkChunksExist(minX - 1, minY, minZ - 1, maxX + 1, maxY, maxZ + 1)) {
			posePlayer.etfu$setWaterState(0.0D, false);
			return false;
		}

		double feetY = player.boundingBox.minY;
		int eyeBlockX = MathHelper.floor_double(player.posX);
		int eyeBlockZ = MathHelper.floor_double(player.posZ);
		PlayerPose pose = posePlayer.etfu$getAppliedPose();
		double eyeY = feetY + (pose != null ? pose.eyeHeight : PlayerPose.STANDING_EYE_HEIGHT);

		double waterHeight = 0.0D;
		boolean eyesInWater = false;
		double currentHeight = 0.0D;
		double currentX = 0.0D;
		double currentY = 0.0D;
		double currentZ = 0.0D;
		int currentCount = 0;

		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					Block block = world.getBlock(x, y, z);
					if (block.getMaterial() != Material.water) {
						continue;
					}
					double surface = getSurfaceHeight(world, x, y, z, block, false);
					if (y + surface < box.minY) {
						continue;
					}

					if (x == eyeBlockX && z == eyeBlockZ && eyeY >= y && eyeY <= y + getSurfaceHeight(world, x, y, z, block, true)) {
						eyesInWater = true;
					}

					waterHeight = Math.max(y + surface - feetY, waterHeight);
					if (pushedByWater) {
						Vec3 flow = getFlow(world, x, y, z, block, player);
						currentHeight = Math.max(waterHeight, currentHeight);
						double scale = currentHeight < JUMP_THRESHOLD ? currentHeight : 1.0D;
						currentX += flow.xCoord * scale;
						currentY += flow.yCoord * scale;
						currentZ += flow.zCoord * scale;
						currentCount++;
					}
				}
			}
		}

		posePlayer.etfu$setWaterState(waterHeight, eyesInWater);
		if (currentCount > 0) {
			applyCurrent(player, currentX, currentY, currentZ, currentCount);
		}
		return waterHeight > 0.0D;
	}

	/**
	 * Players are pushed by the average current of the water blocks they touch (§9.4), and a nearly still player still
	 * gets a minimum push so currents can move them.
	 */
	private static void applyCurrent(EntityPlayer player, double x, double y, double z, int count) {
		if (x * x + y * y + z * z < 1.0E-5F) {
			return;
		}
		x = x / count * CURRENT_STRENGTH;
		y = y / count * CURRENT_STRENGTH;
		z = z / count * CURRENT_STRENGTH;
		double length = Math.sqrt(x * x + y * y + z * z);
		if (Math.abs(player.motionX) < 0.003D && Math.abs(player.motionZ) < 0.003D && length < MIN_CURRENT) {
			x = x / length * MIN_CURRENT;
			y = y / length * MIN_CURRENT;
			z = z / length * MIN_CURRENT;
		}
		player.motionX += x;
		player.motionY += y;
		player.motionZ += z;
	}

	/**
	 * The flow direction of one water block. Vanilla water uses its raw, normalised flow, as modern does; Forge fluid
	 * blocks report theirs through velocityToAddToEntity.
	 */
	private static Vec3 getFlow(World world, int x, int y, int z, Block block, EntityPlayer player) {
		if (block instanceof BlockLiquid) {
			return ((AccessorBlockLiquid) block).etfu$getFlowVector(world, x, y, z);
		}
		Vec3 flow = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
		block.velocityToAddToEntity(world, x, y, z, player, flow);
		return flow;
	}

	/**
	 * How deep a hitbox sits in this liquid (§30, modern's fluid height): the highest liquid surface among the blocks
	 * the box (shrunk by 0.001) overlaps, measured from the box's bottom; 0 when it overlaps none.
	 */
	public static double getFluidHeight(World world, AxisAlignedBB box, Material material) {
		AxisAlignedBB shrunk = box.contract(FLUID_BOX_MARGIN, FLUID_BOX_MARGIN, FLUID_BOX_MARGIN);
		int minX = MathHelper.floor_double(shrunk.minX);
		int minY = MathHelper.floor_double(shrunk.minY);
		int minZ = MathHelper.floor_double(shrunk.minZ);
		int maxX = MathHelper.ceiling_double_int(shrunk.maxX) - 1;
		int maxY = MathHelper.ceiling_double_int(shrunk.maxY) - 1;
		int maxZ = MathHelper.ceiling_double_int(shrunk.maxZ) - 1;
		if (!world.checkChunksExist(minX, minY, minZ, maxX, maxY, maxZ)) {
			return 0.0D;
		}
		double height = 0.0D;
		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					Block block = world.getBlock(x, y, z);
					if (block.getMaterial() == material) {
						height = Math.max(height, y + getSurfaceHeight(world, x, y, z, block, false) - shrunk.minY);
					}
				}
			}
		}
		return height;
	}

	/**
	 * Whether a point is under the surface of this liquid: the block there has the material and the point is below
	 * its surface height (§30, modern's fluid-on-eyes test).
	 */
	public static boolean isPointInMaterial(World world, double x, double y, double z, Material material) {
		int blockX = MathHelper.floor_double(x);
		int blockY = MathHelper.floor_double(y);
		int blockZ = MathHelper.floor_double(z);
		Block block = world.getBlock(blockX, blockY, blockZ);
		return block.getMaterial() == material && y < blockY + getSurfaceHeight(world, blockX, blockY, blockZ, block, false);
	}

	/**
	 * The height of the water surface inside this block (§9.1): full if the same liquid continues above, otherwise
	 * 8/9 for a source and less for flowing water. For the eye and camera checks (§27, modern's
	 * {@code getHeightForCamera}), a source under a block with a solid bottom counts as full. Blocks that stand in for
	 * a source with the liquid's material (EFR's bubble column cells, §28, and kelp, §29) keep their meta at 0 or 8+,
	 * which reads as a source here as it does in vanilla's own water code.
	 */
	public static double getSurfaceHeight(World world, int x, int y, int z, Block block, boolean forCamera) {
		if (block instanceof IFluidBlock && !(block instanceof BlockLiquid)) {
			return Math.abs(((IFluidBlock) block).getFilledPercentage(world, x, y, z));
		}
		if (world.getBlock(x, y + 1, z).getMaterial() == block.getMaterial()) {
			return 1.0D;
		}
		int meta = world.getBlockMetadata(x, y, z);
		boolean source = meta == 0 || meta >= 8 && !(block instanceof BlockLiquid);
		if (forCamera && source && world.isSideSolid(x, y + 1, z, ForgeDirection.DOWN)) {
			return 1.0D;
		}
		int amount = meta == 0 || meta >= 8 ? 8 : 8 - meta;
		return amount / 9.0D;
	}
}
