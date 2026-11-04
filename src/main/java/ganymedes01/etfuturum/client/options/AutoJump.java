package ganymedes01.etfuturum.client.options;

import ganymedes01.etfuturum.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovementInput;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

/**
 * 26.3's Auto-Jump (SPEC-modern-movement.md §54.12, from LocalPlayer.updateAutoJump), on 1.7.10's collision boxes:
 * after a move, whether the player should jump on the next tick to get up what is in front.
 */
public final class AutoJump {

	private AutoJump() {
	}

	/** After the player moved dx, dz this tick: whether to jump on the next one. */
	public static boolean wanted(EntityPlayerSP player, MovementInput input, double dx, double dz) {
		if (!player.onGround || player.isSneaking() || player.isRiding() || input.moveStrafe == 0.0F && input.moveForward == 0.0F
			|| onHoney(player)) {
			return false;
		}
		float speed = (float) player.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue();
		double moveX = dx;
		double moveZ = dz;
		double distanceSq = moveX * moveX + moveZ * moveZ;
		if (distanceSq <= 0.001D) {
			// standing against something: the way the keys push instead
			float inputX = speed * input.moveStrafe;
			float inputZ = speed * input.moveForward;
			float sin = MathHelper.sin(player.rotationYaw * (float) Math.PI / 180.0F);
			float cos = MathHelper.cos(player.rotationYaw * (float) Math.PI / 180.0F);
			moveX = inputX * cos - inputZ * sin;
			moveZ = inputZ * cos + inputX * sin;
			distanceSq = moveX * moveX + moveZ * moveZ;
			if (distanceSq <= 0.001D) {
				return false;
			}
		}
		double distance = Math.sqrt(distanceSq);
		double dirX = moveX / distance;
		double dirZ = moveZ / distance;
		Vec3 facing = player.getLookVec();
		if (facing.xCoord * dirX + facing.zCoord * dirZ < -0.15D) {
			// moving more than about 98.6 degrees away from where the player faces
			return false;
		}
		World world = player.worldObj;
		int x = MathHelper.floor_double(player.posX);
		int z = MathHelper.floor_double(player.posZ);
		int ceiling = MathHelper.floor_double(player.boundingBox.maxY);
		if (collisionBox(world, x, ceiling, z) != null || collisionBox(world, x, ++ceiling, z) != null) {
			return false;
		}
		float jumpHeight = 1.2F;
		if (player.isPotionActive(Potion.jump)) {
			jumpHeight += (player.getActivePotionEffect(Potion.jump).getAmplifier() + 1) * 0.75F;
		}
		double feet = player.boundingBox.minY;
		double lookAhead = Math.max(speed * 7.0F, distance);
		double beginX = player.posX;
		double beginZ = player.posZ;
		double endX = beginX + dx + dirX * lookAhead;
		double endZ = beginZ + dz + dirZ * lookAhead;
		AxisAlignedBB around = AxisAlignedBB.getBoundingBox(Math.min(beginX, endX), feet, Math.min(beginZ, endZ),
			Math.max(beginX, endX), feet + player.height, Math.max(beginZ, endZ)).expand(player.width, 0.0D, player.width);
		double lineY = feet + 0.51F;
		// the two lines at the hitbox's left and right edges: the move direction crossed with up, half a width out
		double sideX = -dirZ * player.width * 0.5F;
		double sideZ = dirX * player.width * 0.5F;
		float obstacle = Float.MIN_VALUE;
		@SuppressWarnings("unchecked")
		List<AxisAlignedBB> boxes = world.getCollidingBoundingBoxes(player, around);
		for (AxisAlignedBB box : boxes) {
			if (crosses(box, beginX - sideX, beginZ - sideZ, endX - sideX, endZ - sideZ, lineY)
				|| crosses(box, beginX + sideX, beginZ + sideZ, endX + sideX, endZ + sideZ, lineY)) {
				obstacle = (float) box.maxY;
				int blockX = MathHelper.floor_double((box.minX + box.maxX) / 2.0D);
				int blockY = MathHelper.floor_double((box.minY + box.maxY) / 2.0D);
				int blockZ = MathHelper.floor_double((box.minZ + box.maxZ) / 2.0D);
				for (int steps = 1; steps < jumpHeight; steps++) {
					AxisAlignedBB above = collisionBox(world, blockX, blockY + steps, blockZ);
					if (above != null) {
						obstacle = (float) above.maxY;
						if (obstacle - feet > jumpHeight) {
							return false;
						}
					}
					if (steps > 1 && collisionBox(world, x, ++ceiling, z) != null) {
						return false;
					}
				}
				break;
			}
		}
		if (obstacle == Float.MIN_VALUE) {
			return false;
		}
		float rise = (float) (obstacle - feet);
		return rise > 0.5F && rise <= jumpHeight;
	}

	/** Whether the box overlaps the flat line from (x1, y, z1) to (x2, y, z2), as 26.3's AABB.intersects(Vec3, Vec3). */
	private static boolean crosses(AxisAlignedBB box, double x1, double z1, double x2, double z2, double y) {
		return box.minX < Math.max(x1, x2) && box.maxX > Math.min(x1, x2) && box.minY < y && box.maxY > y
			&& box.minZ < Math.max(z1, z2) && box.maxZ > Math.min(z1, z2);
	}

	private static AxisAlignedBB collisionBox(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		return block.getCollisionBoundingBoxFromPool(world, x, y, z);
	}

	/** 26.3's jump factor: honey, at the feet or just below, cuts jumps, and so auto-jumps. */
	private static boolean onHoney(EntityPlayerSP player) {
		if (!ModBlocks.HONEY_BLOCK.isEnabled()) {
			return false;
		}
		Block honey = ModBlocks.HONEY_BLOCK.get();
		int x = MathHelper.floor_double(player.posX);
		int z = MathHelper.floor_double(player.posZ);
		int feet = MathHelper.floor_double(player.boundingBox.minY);
		return player.worldObj.getBlock(x, feet, z) == honey
			|| player.worldObj.getBlock(x, MathHelper.floor_double(player.boundingBox.minY - 0.5000001D), z) == honey;
	}
}
