package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import ganymedes01.etfuturum.ModBlocks;
import io.github.asgerjon.mcfly.end.EndGatewayBlockEntity;
import io.github.asgerjon.unittas.TasTestCase;
import ganymedes01.etfuturum.world.end.modern.EnderDragonFight;
import ganymedes01.etfuturum.world.end.modern.ModernEnd;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The End's gateways as 26.3's (SPEC-modern-movement.md §76.10, stage 5): a kill opens the next of 20 at the seed's place
 * in its shell; going through finds land out past 1024 blocks and makes a return gateway 10 above it; the return leads
 * back; a used gateway cools down; an ender pearl goes through itself.
 */
@TasSuite("end")
public class EndGatewayTests extends TasTestCase {

	@Override
	protected void setUp() {
		if (!ModernEnd.isActive() || !io.github.asgerjon.mcfly.registry.McFlyBlocks.END_GATEWAY.isEnabled()) {
			skipTest("modernEnd or the gateway is off");
		}
	}

	@Override
	protected int timeoutSeconds() {
		return 120;
	}

	private static WorldServer end() {
		return MinecraftServer.getServer().worldServerForDimension(1);
	}

	private void toTheEnd() {
		onServer(() -> {
			if (player().dimension != 1) {
				player().travelToDimension(1);
			}
		});
		waitUntilOnClient("the End", () -> mc().thePlayer.dimension == 1 && !(mc().currentScreen instanceof GuiDownloadTerrain), 200);
	}

	/** The ring's places in the order kills open them: the back of the seed's shuffle of 0 to 19 first. */
	private static int[] ringPlace(long seed, int nth) {
		List<Integer> order = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			order.add(i);
		}
		Collections.shuffle(order, new Random(seed));
		int g = order.get(order.size() - 1 - nth);
		double angle = 2.0 * (-Math.PI + Math.PI / 20 * g);
		return new int[] {(int) Math.floor(96.0 * Math.cos(angle)), 75, (int) Math.floor(96.0 * Math.sin(angle))};
	}

	/** Whether there's a gateway, in its shell, at the place. */
	private static String shellProblems(WorldServer world, int x, int y, int z) {
		List<String> wrong = new ArrayList<>();
		if (world.getBlock(x, y, z) != io.github.asgerjon.mcfly.registry.McFlyBlocks.END_GATEWAY.get()) {
			wrong.add("no gateway at " + x + " " + y + " " + z + " (" + Block.blockRegistry.getNameForObject(world.getBlock(x, y, z)) + ")");
		}
		for (int[] at : new int[][] {{0, 2, 0}, {0, -2, 0}, {1, 1, 0}, {-1, -1, 0}, {0, 1, 1}, {0, -1, -1}}) {
			if (world.getBlock(x + at[0], y + at[1], z + at[2]) != Blocks.bedrock) {
				wrong.add("no bedrock at " + at[0] + " " + at[1] + " " + at[2]);
			}
		}
		if (!world.isAirBlock(x + 1, y, z) || !world.isAirBlock(x + 1, y + 1, z + 1)) {
			wrong.add("not air beside it or in the shell's corner");
		}
		return wrong.toString();
	}

	/** A dragon's death opens the next gateway (the seed's first), in its shell, its beam rising. */
	public void testAKillOpensAGateway() {
		toTheEnd();
		String found = onServer(() -> {
			WorldServer end = end();
			EnderDragonFight fight = EnderDragonFight.of(end);
			EntityDragon dragon = new EntityDragon(end);
			dragon.setLocationAndAngles(0.0, 100.0, 0.0, 0.0F, 0.0F);
			end.spawnEntityInWorld(dragon);
			fight.useDragon(dragon);
			int before = fight.gatewaysLeft();
			fight.setDragonKilled(dragon);
			dragon.setDead();
			int[] at = ringPlace(end.getSeed(), 20 - before);
			EndGatewayBlockEntity gateway = end.getTileEntity(at[0], at[1], at[2]) instanceof EndGatewayBlockEntity te ? te : null;
			return shellProblems(end, at[0], at[1], at[2]) + " spawning " + (gateway != null && gateway.isSpawning()) + ", left " + (before - fight.gatewaysLeft());
		});
		assertEqual("[] spawning true, left 1", found, "the kill's gateway");
	}

	/** Through a ring gateway: out past 1024 onto land, a return gateway 10 above that, and the way back to the ring. */
	public void testThroughAndBack() {
		toTheEnd();
		int[] ring = onServer(() -> {
			int[] at = ringPlace(end().getSeed(), 0);
			if (end().getBlock(at[0], at[1], at[2]) != io.github.asgerjon.mcfly.registry.McFlyBlocks.END_GATEWAY.get()) {
				ganymedes01.etfuturum.world.end.modern.EndGateways.placeShell(end(), at[0], at[1], at[2], null, false);
			}
			player().playerNetServerHandler.setPlayerLocation(at[0] + 0.5, at[1], at[2] + 0.5, 0.0F, 0.0F);
			return at;
		});
		// out along the gateway's direction: 1024, back past land, out past void (16 a step, 16 steps each way)
		waitUntil("out to the outer islands", () -> player().posX * player().posX + player().posZ * player().posZ > 700.0 * 700.0, 200);
		String out = onServer(() -> {
			EndGatewayBlockEntity entry = (EndGatewayBlockEntity) end().getTileEntity(ring[0], ring[1], ring[2]);
			int[] exit = entry.exitPortal();
			boolean onLand = end().getBlock((int) Math.floor(player().posX), (int) Math.floor(player().posY) - 1, (int) Math.floor(player().posZ)).isOpaqueCube();
			return (exit == null ? "no exit" : shellProblems(end(), exit[0], exit[1], exit[2])) + " on land " + onLand
				+ " far " + (Math.sqrt(player().posX * player().posX + player().posZ * player().posZ) > 700.0);
		});
		assertEqual("[] on land true far true", out, "the way out");
		// back through the return gateway: onto the tallest land within 5 of the ring gateway (or above it)
		onServer(() -> {
			EndGatewayBlockEntity entry = (EndGatewayBlockEntity) end().getTileEntity(ring[0], ring[1], ring[2]);
			int[] exit = entry.exitPortal();
			player().playerNetServerHandler.setPlayerLocation(exit[0] + 0.5, exit[1], exit[2] + 0.5, 0.0F, 0.0F);
		});
		waitUntil("back by the ring", () -> {
			double dx = player().posX - (ring[0] + 0.5);
			double dz = player().posZ - (ring[2] + 0.5);
			return dx * dx + dz * dz < 7.0 * 7.0;
		}, 200);
	}

	/** Used, a gateway cools down 40 ticks: nothing goes through it meanwhile. */
	public void testCooldown() {
		toTheEnd();
		String found = onServer(() -> {
			int[] at = ringPlace(end().getSeed(), 1);
			ganymedes01.etfuturum.world.end.modern.EndGateways.placeShell(end(), at[0], at[1], at[2], new int[] {100, 50, 0}, true);
			EndGatewayBlockEntity gateway = (EndGatewayBlockEntity) end().getTileEntity(at[0], at[1], at[2]);
			gateway.triggerCooldown();
			return gateway.isCoolingDown() + "";
		});
		assertEqual("true", found, "cooling down after use");
		serverTicks(45);
		assertEqual("false", onServer(() -> {
			int[] at = ringPlace(end().getSeed(), 1);
			return ((EndGatewayBlockEntity) end().getTileEntity(at[0], at[1], at[2])).isCoolingDown() + "";
		}), "cooled down 40 ticks later");
	}

	/** An ender pearl thrown into a gateway comes out of its exit (26.3's pearl goes through itself). */
	public void testPearlGoesThrough() {
		toTheEnd();
		String found = onServer(() -> {
			int[] at = ringPlace(end().getSeed(), 2);
			ganymedes01.etfuturum.world.end.modern.EndGateways.placeShell(end(), at[0], at[1], at[2], new int[] {100, 55, 0}, true);
			EntityEnderPearl pearl = new EntityEnderPearl(end(), player());
			pearl.setPosition(at[0] + 0.5, at[1] + 0.5, at[2] + 0.5);
			pearl.motionX = pearl.motionY = pearl.motionZ = 0.0;
			end().spawnEntityInWorld(pearl);
			return null;
		});
		// server ticks: the gateway's are what move it (and a pearl left longer lands and is gone)
		serverTicks(5);
		found = onServer(() -> {
			for (Object entity : end().loadedEntityList) {
				if (entity instanceof EntityEnderPearl pearl && pearl.getDistance(100.5, 55.0, 0.5) < 3.0) {
					pearl.setDead();
					return "at the exit";
				}
			}
			return "not at the exit";
		});
		assertEqual("at the exit", found, "the pearl");
	}
}
