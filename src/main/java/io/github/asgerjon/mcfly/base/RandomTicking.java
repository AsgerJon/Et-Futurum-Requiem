package io.github.asgerjon.mcfly.base;

import net.minecraft.world.World;

import java.util.Random;

/**
 * A block with 26.3's random tick apart from its scheduled one (SPEC-modern-movement.md §84): 1.7.10 calls `updateTick`
 * for both, so a block that does different things in each (26.3's chorus flower grows in its random tick and checks its
 * footing in its scheduled one) has the world's random tick call this instead (`MixinWorldServerRandomTick`).
 */
public interface RandomTicking {

	void randomTick(World world, int x, int y, int z, Random random);
}
