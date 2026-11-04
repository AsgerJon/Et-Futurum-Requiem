package ganymedes01.etfuturum.world.end.modern;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/**
 * The exit portal as 26.3's (SPEC-modern-movement.md §76.6, `EndPodiumFeature`): a bedrock rim of radius 3.5 with end
 * stone under it, a bedrock floor inside radius 2.5 under the portal (air until the dragon dies), a bedrock pillar 4
 * high with a torch on each side. Active, it breaks (dropping) what's in its way, as 26.3's.
 */
public final class ModernEndPodium {

	private ModernEndPodium() {
	}

	public static void place(World world, int ox, int oy, int oz, boolean active) {
		for (int x = ox - 4; x <= ox + 4; x++) {
			for (int z = oz - 4; z <= oz + 4; z++) {
				for (int y = oy - 1; y <= oy + 32; y++) {
					double dx = x - ox;
					double dy = y - oy;
					double dz = z - oz;
					double distance = dx * dx + dy * dy + dz * dz;
					boolean insideRim = distance < 2.5 * 2.5;
					if (!insideRim && distance >= 3.5 * 3.5) {
						continue;
					}
					if (y < oy) {
						if (insideRim) {
							world.setBlock(x, y, z, Blocks.bedrock, 0, 3);
						} else {
							set(world, x, y, z, Blocks.end_stone, active);
						}
					} else if (y > oy) {
						set(world, x, y, z, Blocks.air, active);
					} else if (!insideRim) {
						world.setBlock(x, y, z, Blocks.bedrock, 0, 3);
					} else if (active) {
						set(world, x, y, z, Blocks.end_portal, true);
					} else {
						world.setBlock(x, y, z, Blocks.air, 0, 3);
					}
				}
			}
		}
		for (int y = 0; y < 4; y++) {
			world.setBlock(ox, oy + y, oz, Blocks.bedrock, 0, 3);
		}
		// 1.7.10's torch metas: 1 east-facing (on the west side's block) ... the torch on each side faces away from it
		world.setBlock(ox + 1, oy + 2, oz, Blocks.torch, 1, 3);
		world.setBlock(ox - 1, oy + 2, oz, Blocks.torch, 2, 3);
		world.setBlock(ox, oy + 2, oz + 1, Blocks.torch, 3, 3);
		world.setBlock(ox, oy + 2, oz - 1, Blocks.torch, 4, 3);
	}

	/** Active: what's not the block already is broken (dropping) first, as 26.3's dropPreviousAndSetBlock. */
	private static void set(World world, int x, int y, int z, Block block, boolean dropPrevious) {
		if (dropPrevious) {
			if (world.getBlock(x, y, z) != block) {
				world.func_147480_a(x, y, z, true);
				world.setBlock(x, y, z, block, 0, 3);
			}
		} else {
			world.setBlock(x, y, z, block, 0, 3);
		}
	}
}
