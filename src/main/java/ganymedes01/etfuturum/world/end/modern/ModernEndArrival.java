package ganymedes01.etfuturum.world.end.modern;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/**
 * Arriving in the End as 26.3's (SPEC-modern-movement.md §76.5, `EndPortalBlock`, `EndPlatformFeature`): the platform
 * made around (100, 49, 0) each time, a player on it at (100.5, 49, 0.5), anything else a block higher, facing west,
 * its pitch and motion kept.
 */
public final class ModernEndArrival {

	/**
	 * The arriving entity's pitch, kept from before 1.7.10 resets it (the server thread only): noted first when a player
	 * starts travelling (1.7.10 resets a player's pitch there), else when the transfer starts; a note is good for the
	 * tick it was made in.
	 */
	private static float pitchBefore;
	private static Entity pitchOf;
	private static int pitchTick = -1;

	private ModernEndArrival() {
	}

	public static void rememberPitch(Entity entity) {
		int tick = net.minecraft.server.MinecraftServer.getServer() == null ? 0 : net.minecraft.server.MinecraftServer.getServer().getTickCounter();
		if (pitchOf == entity && pitchTick == tick) {
			return;
		}
		pitchOf = entity;
		pitchBefore = entity.rotationPitch;
		pitchTick = tick;
	}

	/** Obsidian 5 by 5 at y 48 around (100, 0), air 3 above; with drops, what's in the way is broken first. */
	public static void makePlatform(World world, boolean dropInTheWay) {
		for (int dz = -2; dz <= 2; dz++) {
			for (int dx = -2; dx <= 2; dx++) {
				for (int dy = -1; dy < 3; dy++) {
					int x = 100 + dx;
					int y = 49 + dy;
					int z = dz;
					net.minecraft.block.Block wanted = dy == -1 ? Blocks.obsidian : Blocks.air;
					if (world.getBlock(x, y, z) != wanted) {
						if (dropInTheWay) {
							world.func_147480_a(x, y, z, true);
						}
						world.setBlock(x, y, z, wanted);
					}
				}
			}
		}
	}

	/** In place of 1.7.10's End platform step: the platform, then the entity on it. */
	public static void arrive(World world, Entity entity) {
		makePlatform(world, true);
		float pitch = pitchOf == entity ? pitchBefore : entity.rotationPitch;
		pitchOf = null;
		double motionX = entity.motionX;
		double motionY = entity.motionY;
		double motionZ = entity.motionZ;
		entity.setLocationAndAngles(100.5, entity instanceof EntityPlayerMP ? 49.0 : 50.0, 0.5, 90.0F, pitch);
		entity.motionX = motionX;
		entity.motionY = motionY;
		entity.motionZ = motionZ;
	}
}
