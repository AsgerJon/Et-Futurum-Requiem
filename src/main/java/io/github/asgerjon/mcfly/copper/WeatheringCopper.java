package io.github.asgerjon.mcfly.copper;

import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A block of 26.3's weathering copper or its waxed twin (SPEC-modern-movement.md §87): its stage at a place (0
 * unaffected, 1 exposed, 2 weathered, 3 oxidized) and its wax, and the same block at another stage or wax there, its
 * shape kept (a slab's half, a stair's facing, a door's halves, a bulb's light, a rod's facing and power).
 */
public interface WeatheringCopper {

	int stage(IBlockAccess world, int x, int y, int z);

	boolean waxed(IBlockAccess world, int x, int y, int z);

	/** This block at the stage and wax given, here, its shape kept (with flag 3, as 26.3's `setBlockAndUpdate`). */
	void become(World world, int x, int y, int z, int stage, boolean waxed);

	/** Whether a random tick here ages it: a door's lower half only, as 26.3's. */
	default boolean agesHere(IBlockAccess world, int x, int y, int z) {
		return true;
	}
}
