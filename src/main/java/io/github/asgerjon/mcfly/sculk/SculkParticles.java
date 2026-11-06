package io.github.asgerjon.mcfly.sculk;

import io.github.asgerjon.mcfly.particle.McFlyParticles;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** Sculk's particles from the server (SPEC-modern-movement.md §90.9, step H3): 26.3's `sculk_soul` over a blooming catalyst. */
public final class SculkParticles {

	public static final String SCULK_SOUL = "minecraft:sculk_soul";

	private SculkParticles() {
	}

	/** 26.3's bloom: two souls rising, spread 0.2 on x and z. */
	static void soul(World world, double x, double y, double z) {
		if (world instanceof WorldServer && McFlyParticles.exists(SCULK_SOUL)) {
			McFlyParticles.send((WorldServer) world, SCULK_SOUL, x, y, z, 2, 0.2D, 0.0D, 0.2D, 0.0D);
		}
	}
}
