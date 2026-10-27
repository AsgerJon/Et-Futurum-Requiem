package ganymedes01.etfuturum.core.utils;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/**
 * What a beacon's beam goes through, as modern's beacon scan has it (1.8+): anything but a fully opaque block, and
 * bedrock. 1.7.10 asked the sky map instead, which water, ice or leaves above the beacon break, and which bedrock
 * ends, so beacons never worked under the Nether's roof.
 */
public final class BeaconBeam {

	private BeaconBeam() {
	}

	public static boolean passes(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		return block == Blocks.bedrock || block.getLightOpacity(world, x, y, z) < 255;
	}

	/** Whether the beam from above the beacon reaches the top of the world, which is what turns the beacon on. */
	public static boolean reachesTheSky(World world, int x, int y, int z) {
		for (int i = y + 1; i < world.getHeight(); i++) {
			if (!passes(world, x, i, z)) {
				return false;
			}
		}
		return true;
	}
}
