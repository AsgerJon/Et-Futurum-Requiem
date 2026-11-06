package ganymedes01.etfuturum.unittas.tests;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The observer as our own (SPEC-modern-movement.md §86), as 26.3's `ObserverBlock`, against 26.3 itself where it
 * matters: its clock as the reference mod recorded it in 26.3 (`observer_clock_26_3.txt`). Along x at z 20 on the flat
 * world; meta 5 looks east, 4 west (as a piston faces), 8 while powered.
 */
@TasSuite("redstone")
public class ObserverTests extends TasTestCase {

	private static final int X = 60, Y = 4, Z = 20;
	private static final String[] EFR_CLASSES = {"ganymedes01.etfuturum.blocks.BlockObserver", "ganymedes01.etfuturum.blocks.IBlockObserver",
		"ganymedes01.etfuturum.ducks.IObserverWorldExtension", "ganymedes01.etfuturum.client.renderer.block.BlockObserverRenderer",
		"ganymedes01.etfuturum.mixins.early.observer.MixinWorld", "ganymedes01.etfuturum.mixins.early.observer.MixinWorldServer",
		"ganymedes01.etfuturum.mixins.early.observer.MixinChunk"};

	private static Block observer() {
		Object block = Block.blockRegistry.getObject("etfuturum:observer");
		assertTrue(block instanceof Block && block != Blocks.air, "no etfuturum:observer");
		return (Block) block;
	}

	@Override
	protected void setUp() {
		onServer(() -> {
			for (int x = X - 4; x <= X + 4; x++) {
				for (int y = Y; y <= Y + 2; y++) {
					for (int z = Z - 3; z <= Z + 3; z++) {
						world().setBlock(x, y, z, Blocks.air, 0, 2);
					}
				}
			}
		});
	}

	@Override
	protected void tearDown() {
		setUp();
	}

	/** The block McFly's under EFR's id; none of EFR's observer classes left. */
	public void testObserverIsOurs() throws Exception {
		List<String> wrong = new ArrayList<>();
		String type = observer().getClass().getName();
		if (!type.startsWith("io.github.asgerjon.mcfly.")) {
			wrong.add(type);
		}
		for (String name : EFR_CLASSES) {
			if (net.minecraft.launchwrapper.Launch.classLoader.getClassBytes(name) != null) {
				wrong.add(name.substring(name.lastIndexOf('.') + 1) + " still there");
			}
		}
		assertEqual("[]", wrong.toString(), "the observer's classes");
	}

	/** Two facing observers, placed in one step, against 26.3's own clock tick by tick (2 on, 4 off, taking turns). */
	public void testClockAs26_3() throws Exception {
		String[] reference = reference();
		String[] got = trace(new int[][]{{X, Y, Z}, {X + 1, Y, Z}}, reference[0].length(), () -> {
			world().setBlock(X, Y, Z, observer(), 5, 3);
			world().setBlock(X + 1, Y, Z, observer(), 4, 3);
		});
		assertEqual("first " + reference[0] + " second " + reference[1], "first " + got[0] + " second " + got[1], "the clock (26.3's, then ours)");
	}

	/** 26.3's two lines: the first observer's and the second's ('#' powered at a tick's end). */
	private static String[] reference() throws java.io.IOException {
		String first = null;
		String second = null;
		try (InputStream in = ObserverTests.class.getResourceAsStream("/assets/etfuturum/unittas/observer_clock_26_3.txt")) {
			assertTrue(in != null, "no observer_clock_26_3.txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			for (String line; (line = reader.readLine()) != null; ) {
				if (line.startsWith("first:")) {
					first = line.substring("first:".length()).trim();
				} else if (line.startsWith("second:")) {
					second = line.substring("second:".length()).trim();
				}
			}
		}
		assertTrue(first != null && second != null, "26.3's clock file has no traces");
		return new String[]{first, second};
	}

	/**
	 * Each observer's state at the end of each of the ticks after `first` (a server step) ran: '#' powered (meta 8), '_'
	 * not. Taken by a server tick handler, one sample a tick by its make (as 26.3's trace, at the tick's end), not by test
	 * steps: those are meant to take a tick each, but unitTAS 0.1.0-dev4 can run a step queued fast in the tick its last
	 * one ran in (`runAll` drains while steps come), and the full run of 2026-11-05 06:30 had one sampled twice.
	 */
	private String[] trace(int[][] observers, int ticks, Runnable first) {
		Sampler sampler = new Sampler(observers, ticks);
		onServer(() -> {
			first.run();
			// from the next tick's end: a handler added while the bus sends this tick's event isn't sent it
			FMLCommonHandler.instance().bus().register(sampler);
		});
		try {
			waitUntil("the observers traced", () -> sampler.done, ticks + 40);
		} finally {
			onServer(() -> FMLCommonHandler.instance().bus().unregister(sampler));
		}
		return onServer(() -> sampler.traces());
	}

	/** Samples the observers at the end of each server tick until it has its count. */
	public static final class Sampler {

		private final int[][] observers;
		private final int ticks;
		private final StringBuilder[] traces;
		volatile boolean done;

		Sampler(int[][] observers, int ticks) {
			this.observers = observers;
			this.ticks = ticks;
			this.traces = new StringBuilder[observers.length];
			for (int i = 0; i < observers.length; i++) {
				traces[i] = new StringBuilder();
			}
		}

		@SubscribeEvent
		public void onServerTick(TickEvent.ServerTickEvent event) {
			if (event.phase != TickEvent.Phase.END || done) {
				return;
			}
			for (int i = 0; i < observers.length; i++) {
				boolean powered = (world().getBlockMetadata(observers[i][0], observers[i][1], observers[i][2]) & 8) != 0;
				traces[i].append(powered ? '#' : '_');
			}
			done = traces[0].length() >= ticks;
		}

		String[] traces() {
			String[] out = new String[traces.length];
			for (int i = 0; i < traces.length; i++) {
				out[i] = traces[i].toString();
			}
			return out;
		}
	}

	/**
	 * One change of the block it faces gives one pulse, as the first of 26.3's clock (the second observer's placing is
	 * a change in front of the first): off, 2 ticks on, then off; a change beside or behind it gives none.
	 */
	public void testOnePulsePerChangeInFront() throws Exception {
		String pulse = reference()[0].substring(0, 3) + "____";
		onServer(() -> {
			world().setBlock(X, Y, Z, observer(), 5, 3);
			world().setBlock(X + 1, Y, Z, Blocks.stone, 0, 3);
		});
		serverTicks(10);
		String front = trace(new int[][]{{X, Y, Z}}, 7, () -> world().setBlock(X + 1, Y, Z, Blocks.dirt, 0, 3))[0];
		String sides = trace(new int[][]{{X, Y, Z}}, 7, () -> {
			world().setBlock(X, Y, Z + 1, Blocks.stone, 0, 3);
			world().setBlock(X - 1, Y, Z, Blocks.stone, 0, 3);
		})[0];
		assertEqual("front " + pulse + ", side and back _______", "front " + front + ", side and back " + sides, "the observer's pulses");
	}

	/**
	 * Its signal goes out of its back only, strongly: a lamp behind the block behind it lights during a pulse; a lamp
	 * beside it doesn't.
	 */
	public void testSignalOutOfTheBackStrongly() {
		onServer(() -> {
			world().setBlock(X, Y, Z, observer(), 5, 3);
			world().setBlock(X + 1, Y, Z, Blocks.stone, 0, 3);
			world().setBlock(X - 1, Y, Z, Blocks.stone, 0, 3);
			world().setBlock(X - 2, Y, Z, Blocks.redstone_lamp, 0, 3);
			world().setBlock(X, Y, Z + 1, Blocks.redstone_lamp, 0, 3);
		});
		serverTicks(10);
		onServer(() -> world().setBlock(X + 1, Y, Z, Blocks.dirt, 0, 3));
		boolean behind = false;
		boolean beside = false;
		for (int tick = 0; tick < 6; tick++) {
			boolean[] lit = onServer(() -> new boolean[]{world().getBlock(X - 2, Y, Z) == Blocks.lit_redstone_lamp, world().getBlock(X, Y, Z + 1) == Blocks.lit_redstone_lamp});
			behind |= lit[0];
			beside |= lit[1];
		}
		assertEqual("behind true, beside false", "behind " + behind + ", beside " + beside, "the lamps during a pulse");
	}

	/** Placed by a player, it looks the way the player looks (26.3: its face where the player looks, its back to them). */
	public void testPlacedLookingTheWayThePlayerLooks() {
		String metas = onServer(() -> {
			world().setBlock(X, Y - 1, Z, Blocks.stone, 0, 3);
			player().rotationYaw = -90.0F;
			player().rotationPitch = 0.0F;
			net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(observer());
			player().setGameType(net.minecraft.world.WorldSettings.GameType.SURVIVAL);
			stack.tryPlaceItemIntoWorld(player(), world(), X, Y - 1, Z, 1, 0.5F, 1.0F, 0.5F);
			player().setGameType(net.minecraft.world.WorldSettings.GameType.CREATIVE);
			return String.valueOf(world().getBlockMetadata(X, Y, Z) & 7);
		});
		assertEqual("5", metas, "the observer's facing, the player looking east (5 east)");
	}

	/** Placed powered with no tick due (as a piston leaves one), it unpowers at once (26.3's `onPlace`). */
	public void testPlacedPoweredUnpowers() {
		onServer(() -> world().setBlock(X, Y, Z, observer(), 5 | 8, 3));
		serverTicks(2);
		assertEqual(5, onServer(() -> world().getBlockMetadata(X, Y, Z)), "its meta 2 ticks after being placed powered");
	}
}
