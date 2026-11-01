package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.init.Blocks;

/**
 * The redstone torch and an observer clock (SPEC-modern-movement.md §55.3). By 26.3's code an observer's 2-tick pulse
 * ends before a torch fed by it gets its tick, so the torch doesn't follow the clock: pinned here. A burnt-out torch gets
 * another try 160 ticks later in 26.3, which 1.7.10 lacks. The clock runs along x at z 0: the first observer looks east
 * at the second, which looks back; the first's output goes into the stone west of it, a torch stands on that stone.
 */
public class TorchTests extends TasTestCase {

	private static final int X = 40, Y = 4, Z = 0, TORCH_X = 46;

	@Override
	protected void setUp() {
		if (!ModBlocks.OBSERVER.isEnabled()) {
			skipTest("observers are off in the config");
		}
		onServer(() -> {
			for (int x = X - 2; x <= TORCH_X + 2; x++) {
				for (int y = Y; y <= Y + 1; y++) {
					world().setBlock(x, y, Z, Blocks.air, 0, 2);
				}
			}
		});
	}

	/** Two facing observers keep pulsing in 2-tick pulses; the period is reported, not assumed. */
	public void testObserverClockKeepsPulsing() {
		startClock(false);
		String trace = recordObserver(40);
		int pulses = 0;
		for (String run : trace.split("_+")) {
			if (!run.isEmpty()) {
				pulses++;
				assertEqual(2, run.length(), "a pulse that isn't 2 ticks long in " + trace);
			}
		}
		assertGreater(pulses, 3);
	}

	/** A torch on the stone the clock powers stays lit: it never sees a pulse (26.3's order of ticks). */
	public void testTorchOnAnObserverClockStaysLit() {
		startClock(true);
		StringBuilder trace = new StringBuilder();
		for (int tick = 0; tick < 40; tick++) {
			serverTicks(1);
			trace.append(onServer(() -> world().getBlock(X - 1, Y + 1, Z) == Blocks.redstone_torch) ? 'L' : '.');
		}
		assertTrue(trace.indexOf(".") < 0, "the torch went out: " + trace + " (L lit, . out)");
	}

	/** A torch burnt out by a lever flicked fast relights by itself once the lever is off and its turn-offs age out. */
	public void testBurntOutTorchRelights() {
		onServer(() -> {
			world().setBlock(TORCH_X, Y, Z, Blocks.stone, 0, 3);
			world().setBlock(TORCH_X, Y + 1, Z, Blocks.redstone_torch, 5, 3);
			// a lever on the stone's east face (meta 1: on the block to its west)
			world().setBlock(TORCH_X + 1, Y, Z, Blocks.lever, 1, 3);
		});
		serverTicks(5);
		// 16 flicks, 3 ticks apart: the torch goes out 8 times within 60 ticks, the burnout limit, and the lever ends off
		for (int flick = 0; flick < 16; flick++) {
			onServer(() -> Blocks.lever.onBlockActivated(world(), TORCH_X + 1, Y, Z, player(), 0, 0.0F, 0.0F, 0.0F));
			serverTicks(3);
		}
		if (onServer(() -> (world().getBlockMetadata(TORCH_X + 1, Y, Z) & 8) != 0)) {
			onServer(() -> Blocks.lever.onBlockActivated(world(), TORCH_X + 1, Y, Z, player(), 0, 0.0F, 0.0F, 0.0F));
		}
		serverTicks(10);
		assertTrue(onServer(() -> world().getBlock(TORCH_X, Y + 1, Z) == Blocks.unlit_redstone_torch),
			"the torch didn't burn out (the test's setup)");
		waitUntil("the torch to relight", () -> world().getBlock(TORCH_X, Y + 1, Z) == Blocks.redstone_torch, 200);
	}

	/** Places the two observers (the second's arrival starts the clock) and, if asked, the stone and torch at the output. */
	private void startClock(boolean torch) {
		onServer(() -> {
			if (torch) {
				world().setBlock(X - 1, Y, Z, Blocks.stone, 0, 3);
				world().setBlock(X - 1, Y + 1, Z, Blocks.redstone_torch, 5, 3);
			}
			// meta 5 looks east, 4 west (as pistons face)
			world().setBlock(X, Y, Z, ModBlocks.OBSERVER.get(), 5, 3);
			world().setBlock(X + 1, Y, Z, ModBlocks.OBSERVER.get(), 4, 3);
		});
	}

	/** The first observer's state each server tick: '#' powered, '_' not. */
	private String recordObserver(int ticks) {
		StringBuilder trace = new StringBuilder();
		for (int tick = 0; tick < ticks; tick++) {
			serverTicks(1);
			trace.append(onServer(() -> (world().getBlockMetadata(X, Y, Z) & 8) != 0) ? '#' : '_');
		}
		// the first and the last run may be cut by the start and the end of the recording
		String whole = trace.substring(trace.indexOf("_") < 0 ? 0 : trace.indexOf("_"));
		return whole.endsWith("#") ? whole.substring(0, whole.lastIndexOf('_') + 1) : whole;
	}
}
