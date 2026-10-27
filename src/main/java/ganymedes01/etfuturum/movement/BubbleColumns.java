package ganymedes01.etfuturum.movement;

import ganymedes01.etfuturum.blocks.BlockBubbleColumn;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Random;

/**
 * What a bubble column does to the entities in it (SPEC-modern-movement.md §28). The column block finds the entities
 * and the cells; this decides the pushes, who takes them and the surface effects.
 */
public final class BubbleColumns {

	private BubbleColumns() {
	}

	/**
	 * Whether the cell above is open: nothing to collide with and no liquid. The top cell of a column pushes harder
	 * and splashes.
	 */
	public static boolean isOpenAbove(World world, int x, int y, int z) {
		Block above = world.getBlock(x, y + 1, z);
		return !above.getMaterial().isLiquid() && above.getCollisionBoundingBoxFromPool(world, x, y + 1, z) == null;
	}

	/**
	 * Whether blocks act on this entity at all: not while flying in creative, and not without physics (spectators).
	 */
	public static boolean isAffected(Entity entity) {
		if (entity.noClip) {
			return false;
		}
		return !(entity instanceof EntityPlayer) || !((EntityPlayer) entity).capabilities.isFlying;
	}

	/**
	 * Which side moves this entity: the server for mobs and the client for its own player, as in modern. Everything
	 * else (items, boats, projectiles) is pushed on both sides, because 1.7.10 clients predict their movement between
	 * the server's position updates.
	 */
	public static boolean simulatesMotion(Entity entity) {
		if (!(entity instanceof EntityLivingBase)) {
			return true;
		}
		return entity.worldObj.isRemote ? entity instanceof EntityPlayer : !(entity instanceof EntityPlayerMP);
	}

	/**
	 * A cell with something above it: towards -0.3 by 0.03 per cell in a whirlpool, towards 0.7 by 0.06 per cell
	 * over soul sand. Being in a column also forgives the fall so far.
	 */
	public static void pushInside(Entity entity, boolean drag) {
		if (simulatesMotion(entity)) {
			entity.motionY = drag ? Math.max(-0.3D, entity.motionY - 0.03D) : Math.min(0.7D, entity.motionY + 0.06D);
		}
		entity.fallDistance = 0.0F;
	}

	/**
	 * The open top cell: towards -0.9 by 0.03 in a whirlpool, towards 1.8 by 0.1 over soul sand, which is what throws
	 * you out of the water.
	 */
	public static void pushAbove(Entity entity, boolean drag) {
		if (simulatesMotion(entity)) {
			entity.motionY = drag ? Math.max(-0.9D, entity.motionY - 0.03D) : Math.min(1.8D, entity.motionY + 0.1D);
		}
	}

	/**
	 * The surface effect of the top cell, sent by the server to everyone nearby every tick an entity is in it: two
	 * splashes and two bubbles at random spots on the cell's top.
	 */
	public static void sendSurfaceParticles(WorldServer world, int x, int y, int z) {
		Random random = world.rand;
		for (int i = 0; i < 2; i++) {
			world.func_147487_a("splash", x + random.nextDouble(), y + 1, z + random.nextDouble(), 1, 0.0D, 0.0D, 0.0D, 1.0D);
			world.func_147487_a("bubble", x + random.nextDouble(), y + 1, z + random.nextDouble(), 1, 0.0D, 0.01D, 0.0D, 0.2D);
		}
	}

	/**
	 * Modern plays the "inside" sound when a column cell overlaps the hitbox shrunk by 0.4 at the top and the bottom
	 * (the middle 0.2 of a hitbox shorter than 0.8). Whether this cell does.
	 */
	public static boolean overlapsCore(Entity entity, int y) {
		AxisAlignedBB box = entity.boundingBox;
		double minY = box.minY + 0.4D;
		double maxY = box.maxY - 0.4D;
		if (minY > maxY) {
			double swap = minY;
			minY = maxY;
			maxY = swap;
		}
		return y < maxY - 1.0E-6D && y + 1 > minY + 1.0E-6D;
	}

	/**
	 * Whether the entity's eyes are in a column cell: there it breathes, however deep it is.
	 */
	public static boolean isEyeInColumn(Entity entity) {
		double eyeY;
		if (entity instanceof IPosePlayer && ((IPosePlayer) entity).etfu$getAppliedPose() != null) {
			eyeY = entity.boundingBox.minY + ((IPosePlayer) entity).etfu$getAppliedPose().eyeHeight;
		} else {
			eyeY = entity.posY + entity.getEyeHeight();
		}
		int x = MathHelper.floor_double(entity.posX);
		int z = MathHelper.floor_double(entity.posZ);
		return entity.worldObj.getBlock(x, MathHelper.floor_double(eyeY), z) instanceof BlockBubbleColumn;
	}
}
