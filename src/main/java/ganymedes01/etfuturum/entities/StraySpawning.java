package ganymedes01.etfuturum.entities;

import io.github.asgerjon.mcfly.registry.McFlyBlocks;
import ganymedes01.etfuturum.ModBlocks;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import java.util.function.BooleanSupplier;

/**
 * Strays' spawn rule (SPEC-modern-movement.md §63.2; 26.3 {@code Stray.checkStraySpawnRules}): the sky must be seen,
 * looking from the first block above that isn't powder snow, unless a spawner spawns it.
 */
public final class StraySpawning {

	private static final ThreadLocal<Boolean> FROM_SPAWNER = ThreadLocal.withInitial(() -> false);

	private StraySpawning() {
	}

	/** Runs the check as a spawner's (1.7.10's spawner asks {@code getCanSpawnHere} as natural spawning does). */
	public static boolean fromSpawner(BooleanSupplier check) {
		boolean before = FROM_SPAWNER.get();
		FROM_SPAWNER.set(true);
		try {
			return check.getAsBoolean();
		} finally {
			FROM_SPAWNER.set(before);
		}
	}

	public static boolean isFromSpawner() {
		return FROM_SPAWNER.get();
	}

	/** 26.3: up past any powder snow from the feet, then {@code canSeeSky} (full sky light) one below that. */
	public static boolean seesSky(Entity entity) {
		World world = entity.worldObj;
		int x = MathHelper.floor_double(entity.posX);
		int y = MathHelper.floor_double(entity.boundingBox.minY);
		int z = MathHelper.floor_double(entity.posZ);
		int check = y;
		do {
			check++;
		} while (McFlyBlocks.POWDER_SNOW.isEnabled() && world.getBlock(x, check, z) == McFlyBlocks.POWDER_SNOW.get());
		return world.getSavedLightValue(EnumSkyBlock.Sky, x, check - 1, z) >= 15;
	}
}
