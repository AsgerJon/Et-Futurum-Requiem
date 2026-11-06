package ganymedes01.etfuturum.world.end.modern;

import ganymedes01.etfuturum.ModBlocks;
import io.github.asgerjon.mcfly.network.LevelEventMessage;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Random;

/**
 * The End's gateways as 26.3's (SPEC-modern-movement.md §76.10): each kill opens the next of 20 on a ring of 96 at y 75
 * (`EnderDragonFight.spawnNewGateway`), in its bedrock shell (`EndGatewayFeature`); the small island a gateway makes when
 * it finds no land (`EndIslandFeature`).
 */
public final class EndGateways {

	/** 26.3's level event for a kill's gateway: a huge explosion and `block.end_gateway.spawn`. */
	public static final int SPAWN_EVENT = 3000;

	private EndGateways() {
	}

	/** The next gateway (from the back of the seed's order): its burst, then it, with no exit yet (found on first use). */
	static void spawnNext(WorldServer world, EnderDragonFight fight) {
		int gateway = fight.takeGateway();
		if (gateway < 0) {
			return;
		}
		int x = MathHelper.floor_double(96.0 * Math.cos(2.0 * (-Math.PI + Math.PI / 20 * gateway)));
		int z = MathHelper.floor_double(96.0 * Math.sin(2.0 * (-Math.PI + Math.PI / 20 * gateway)));
		levelEvent(world, SPAWN_EVENT, x, 75, z);
		placeShell(world, x, 75, z, null, false);
	}

	/** 26.3's non-global level event: to the players within 64 in the dimension. */
	static void levelEvent(WorldServer world, int event, int x, int y, int z) {
		LevelEventMessage.sendAround(world, event, x, y, z, 0);
	}

	/**
	 * 26.3's EndGatewayFeature: the gateway in the middle of 3 by 5 by 3, the rest of its level air; bedrock 2 above and
	 * below it and in the cross off its level (its column, row and line), the rest air. Its exit if given.
	 */
	public static void placeShell(World world, int x, int y, int z, int[] exit, boolean exact) {
		Block gateway = io.github.asgerjon.mcfly.registry.McFlyBlocks.END_GATEWAY.get();
		for (int dz = -1; dz <= 1; dz++) {
			for (int dy = -2; dy <= 2; dy++) {
				for (int dx = -1; dx <= 1; dx++) {
					boolean sameX = dx == 0;
					boolean sameY = dy == 0;
					boolean sameZ = dz == 0;
					boolean end = Math.abs(dy) == 2;
					if (sameX && sameY && sameZ) {
						world.setBlock(x, y, z, gateway, 0, 3);
						if (exit != null && world.getTileEntity(x, y, z) instanceof io.github.asgerjon.mcfly.end.EndGatewayBlockEntity exitGateway) {
							exitGateway.setExitPosition(exit, exact);
						}
					} else if (sameY) {
						world.setBlock(x + dx, y + dy, z + dz, Blocks.air, 0, 3);
					} else if (end && sameX && sameZ) {
						world.setBlock(x + dx, y + dy, z + dz, Blocks.bedrock, 0, 3);
					} else if ((sameX || sameZ) && !end) {
						world.setBlock(x + dx, y + dy, z + dz, Blocks.bedrock, 0, 3);
					} else {
						world.setBlock(x + dx, y + dy, z + dz, Blocks.air, 0, 3);
					}
				}
			}
		}
	}

	/** 26.3's EndIslandFeature: a cone of end stone, radius 4 to 6 at the top, down a layer at a time while over 0.5. */
	public static void smallIsland(World world, Random random, int x, int y, int z) {
		float size = random.nextInt(3) + 4.0F;
		for (int dy = 0; size > 0.5F; dy--) {
			for (int dx = MathHelper.floor_float(-size); dx <= MathHelper.ceiling_float_int(size); dx++) {
				for (int dz = MathHelper.floor_float(-size); dz <= MathHelper.ceiling_float_int(size); dz++) {
					if (dx * dx + dz * dz <= (size + 1.0F) * (size + 1.0F)) {
						world.setBlock(x + dx, y + dy, z + dz, Blocks.end_stone, 0, 3);
					}
				}
			}
			size -= random.nextInt(2) + 0.5F;
		}
	}

	/** 26.3's BlockPos.asLong: x in the top 26 bits, z the next 26, y the low 12. */
	public static long asLong(int x, int y, int z) {
		return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | (long) y & 0xFFFL;
	}
}
