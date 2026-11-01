package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Sneaking off a stair (SPEC-modern-movement.md §7.2, the user's report of 2026-10-30): modern stops a sneaking player
 * where the drop would be deeper than 0.6. A stair on flat ground, its high half to the south: off the top's back or
 * sides is a full block (stopped); forward down its own steps is two drops of 0.5 (allowed, in 26.3 too). Feet heights
 * are the client's, the side that moves the player.
 */
public class SneakEdgeTests extends TasTestCase {

	private static final int X = 20, Y = 4, Z = 2;
	private static final double GROUND = Y, TOP = Y + 1.0D;

	@Override
	protected void setUp() {
		onServer(() -> {
			for (int x = X - 2; x <= X + 2; x++) {
				for (int z = Z - 3; z <= Z + 3; z++) {
					world().setBlock(x, Y, z, Blocks.air, 0, 2);
				}
			}
			// meta 2: the high half at the south (z 0.5 to 1)
			world().setBlock(X, Y, Z, Blocks.oak_stairs, 2, 3);
		});
	}

	@Override
	protected void tearDown() {
		keyUp(mc().gameSettings.keyBindSneak);
		keyUp(mc().gameSettings.keyBindForward);
	}

	/** The user's case: up the stair from the front while sneaking and on: stopped at the top's back edge. */
	public void testUpAndOverTheBack() {
		double[] end = sneakForward(X + 0.5D, GROUND, Z - 1.5D, 0.0F, 100);
		assertTrue(maxFeetY(trace) >= TOP - 0.01D, "never got on top: " + trace);
		assertAlmostEqual(TOP, end[0], 0.01D);
		assertTrue(end[2] <= Z + 1.0D, "past the back edge: " + trace);
	}

	/** From the top to the east: a full block down, stopped. */
	public void testOffTheSide() {
		double[] end = sneakForward(X + 0.5D, TOP, Z + 0.75D, -90.0F, 60);
		assertAlmostEqual(TOP, end[0], 0.01D);
		assertTrue(end[1] <= X + 1.0D, "past the side edge: " + trace);
	}

	/** From the top to the north, down the stair's own steps: two drops of 0.5, which 26.3 allows too. */
	public void testDownTheFront() {
		double[] end = sneakForward(X + 0.5D, TOP, Z + 0.75D, 180.0F, 60);
		assertAlmostEqual(GROUND, end[0], 0.01D);
		assertTrue(end[3] < Z, "still over the stair: " + trace);
	}

	/** The feet and position every 5 ticks of the last walk. */
	private final List<String> trace = new ArrayList<>();

	/**
	 * Stands there facing yaw and sneaks forward for the ticks, then lets go of forward and waits for the player to stop,
	 * still sneaking (tearDown lets go of that). Returns {feet, box min x, box min z, box max z} at the end.
	 */
	private double[] sneakForward(double x, double feet, double z, float yaw, int ticks) {
		onServer(() -> player().playerNetServerHandler.setPlayerLocation(x, feet, z, yaw, 0.0F));
		look(yaw, 0.0F);
		waitUntilOnClient("standing at the start", () -> mc().thePlayer.onGround && Math.abs(mc().thePlayer.posX - x) < 0.01D
			&& Math.abs(mc().thePlayer.posZ - z) < 0.01D, 40);
		KeyBinding sneak = mc().gameSettings.keyBindSneak, forward = mc().gameSettings.keyBindForward;
		keyDown(sneak);
		ticks(2);
		keyDown(forward);
		trace.clear();
		for (int tick = 1; tick <= ticks; tick++) {
			ticks(1);
			if (tick % 5 == 0) {
				int at = tick;
				trace.add(onClient(() -> String.format(Locale.ROOT, "t%d %.3f/%.2f/%.2f", at, mc().thePlayer.boundingBox.minY,
					mc().thePlayer.posX, mc().thePlayer.posZ)));
			}
		}
		keyUp(forward);
		ticks(10);
		return onClient(() -> new double[]{mc().thePlayer.boundingBox.minY, mc().thePlayer.boundingBox.minX,
			mc().thePlayer.boundingBox.minZ, mc().thePlayer.boundingBox.maxZ});
	}

	private static double maxFeetY(List<String> trace) {
		double max = -1.0D;
		for (String line : trace) {
			max = Math.max(max, Double.parseDouble(line.split(" ")[1].split("/")[0]));
		}
		return max;
	}
}
