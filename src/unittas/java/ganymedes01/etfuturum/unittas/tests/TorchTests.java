package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import ganymedes01.etfuturum.ModBlocks;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.init.Blocks;

/**
 * The redstone torch and an observer clock (SPEC-modern-movement.md §55.3). By 26.3's code an observer's 2-tick pulse
 * ends before a torch fed by it gets its tick, so the torch doesn't follow the clock: pinned here. The clock runs along x
 * at z 0: the first observer looks east at the second, which looks back; the first's output goes into the stone west of
 * it, a torch stands on that stone. (The two parked tests went, the user 2026-11-05: the observer's own clock is
 * ObserverTests' now, §86.)
 */
@TasSuite("redstone")
public class TorchTests extends TasTestCase {

	private static final int X = 40, Y = 4, Z = 0, TORCH_X = 46;

	@Override
	protected void setUp() {
		if (!io.github.asgerjon.mcfly.registry.McFlyBlocks.OBSERVER.isEnabled()) {
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

	/** Places the two observers (the second's arrival starts the clock) and, if asked, the stone and torch at the output. */
	private void startClock(boolean torch) {
		onServer(() -> {
			if (torch) {
				world().setBlock(X - 1, Y, Z, Blocks.stone, 0, 3);
				world().setBlock(X - 1, Y + 1, Z, Blocks.redstone_torch, 5, 3);
			}
			// meta 5 looks east, 4 west (as pistons face)
			world().setBlock(X, Y, Z, io.github.asgerjon.mcfly.registry.McFlyBlocks.OBSERVER.get(), 5, 3);
			world().setBlock(X + 1, Y, Z, io.github.asgerjon.mcfly.registry.McFlyBlocks.OBSERVER.get(), 4, 3);
		});
	}

}
