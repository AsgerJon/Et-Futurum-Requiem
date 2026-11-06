package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.command.modern.arguments.BlockIds;
import io.github.asgerjon.mcfly.gameevent.GameEvent;
import io.github.asgerjon.mcfly.gameevent.GameEventListener;
import io.github.asgerjon.mcfly.gameevent.GameEvents;
import io.github.asgerjon.mcfly.mixins.early.redstone.AccessorWorldServerTicks;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * McFly's sculk sensors (SPEC-modern-movement.md §90.9, step H2), as 26.3's (`SculkSensorBlock`,
 * `CalibratedSculkSensorBlock`, their block entities): their stored form; hearing out to 8 (16), power by distance, the
 * comparator's frequency; active 30 ticks (10), cooldown 10; their own placing ignored and nothing while active; a step
 * on them; the calibrated one's back filter; the clicking event and amethyst's resonance; saved with a vibration in
 * flight. On a stone floor at y 7 (x 6 to 30, z 6 to 14); the player stands on the flat ground at (20.5, 4, -14.5), out
 * of even the calibrated one's 16 but near enough that the block entities, ticks and mobs run.
 */
@TasSuite("sculk")
public class SculkSensorTests extends TasTestCase {

	private static final int X = 10, Y = 8, Z = 10;

	@Override
	protected void setUp() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			player().playerNetServerHandler.setPlayerLocation(20.5D, 4.0D, -14.5D, 0.0F, 0.0F);
		});
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			clear();
			player().setGameType(WorldSettings.GameType.CREATIVE);
			player().inventory.setInventorySlotContents(player().inventory.currentItem, null);
		});
	}

	/** Air from y 8 to 12 over x 6 to 30, z 6 to 14, stone at y 7; its mobs gone. */
	private static void clear() {
		for (int x = X - 4; x <= X + 20; x++) {
			for (int z = Z - 4; z <= Z + 4; z++) {
				for (int y = Y - 1; y <= Y + 4; y++) {
					world().setBlock(x, y, z, y == Y - 1 ? Blocks.stone : Blocks.air, 0, 2);
				}
			}
		}
		for (Object entity : world().getEntitiesWithinAABB(Entity.class, AxisAlignedBB.getBoundingBox(X - 5, Y - 2, Z - 5, X + 21, Y + 6, Z + 5))) {
			if (!(entity instanceof net.minecraft.entity.player.EntityPlayer)) {
				((Entity) entity).setDead();
			}
		}
	}

	private static Block sensor() {
		return (Block) Block.blockRegistry.getObject("etfuturum:sculk_sensor");
	}

	private static Block calibrated() {
		return (Block) Block.blockRegistry.getObject("etfuturum:calibrated_sculk_sensor");
	}

	private void needSensors() {
		Block sensor = sensor(), calibrated = calibrated();
		assertTrue(sensor != null && sensor != Blocks.air && calibrated != null && calibrated != Blocks.air, "no sculk_sensor and calibrated_sculk_sensor blocks");
	}

	/** Its block entity's saved form. */
	private static NBTTagCompound saved(int x, int y, int z) {
		TileEntity tile = world().getTileEntity(x, y, z);
		NBTTagCompound tag = new NBTTagCompound();
		if (tile != null) {
			tile.writeToNBT(tag);
		}
		return tag;
	}

	/** The phase from the meta: the plain one's whole meta, the calibrated one's meta >> 2. */
	private static int phase(int x, int y, int z) {
		int meta = world().getBlockMetadata(x, y, z);
		return world().getBlock(x, y, z) == calibrated() ? meta >> 2 : meta;
	}

	/** When its scheduled tick is due (world time), or -1. */
	private static long due(int x, int y, int z) {
		Block block = world().getBlock(x, y, z);
		for (NextTickListEntry entry : ((AccessorWorldServerTicks) world()).mcfly$pendingTicks()) {
			if (entry.xCoord == x && entry.yCoord == y && entry.zCoord == z && entry.func_151351_a() == block) {
				return entry.scheduledTime;
			}
		}
		return -1;
	}

	/** Writes down what it hears, with the world time. */
	private static final class Recorder implements GameEventListener {
		final double x, y, z;
		final List<String> heard = new ArrayList<>();
		final List<Long> times = new ArrayList<>();

		Recorder(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		@Override
		public double[] position(World world) {
			return new double[]{x, y, z};
		}

		@Override
		public int radius() {
			return 16;
		}

		@Override
		public boolean handle(World world, GameEvent event, GameEvent.Context context, double ex, double ey, double ez) {
			heard.add(event.name.replace("minecraft:", ""));
			times.add(world.getTotalWorldTime());
			return true;
		}

		long timeOf(String name) {
			int i = heard.indexOf(name);
			return i < 0 ? -1 : times.get(i);
		}
	}

	/** Posts the event at this spot on the server, then waits for the sensor at (x, y, z) to turn active (or 40 ticks). */
	private void postAndWait(String event, double ex, double ey, double ez, int x, int y, int z) {
		onServer(() -> GameEvents.post(world(), GameEvent.get(event), ex, ey, ez, null));
		waitUntil("the sensor turning active", () -> onServer(() -> phase(x, y, z) == 1), 40);
	}

	// ---------------------------------------------------------------- stored form

	/**
	 * 26.3's sensors in McFly's stored form: the ids; 1.5 to break, light 1, the hoe its tool, 5 experience (not to Silk
	 * Touch); a block entity of McFly's with `last_vibration_frequency` and `listener`; the calibrated one's facing in
	 * meta & 3, its phase above.
	 */
	public void testStoredForm() {
		needSensors();
		String got = onServer(() -> {
			Block sensor = sensor();
			world().setBlock(X, Y, Z, sensor, 0, 3);
			world().setBlock(X + 2, Y, Z, calibrated(), 2, 3);
			NBTTagCompound plain = saved(X, Y, Z), cal = saved(X + 2, Y, Z);
			return sensor.getBlockHardness(world(), X, Y, Z) + " " + sensor.getLightValue() + " " + sensor.getHarvestTool(0) + " "
				+ sensor.getExpDrop(world(), 0, 0) + " silk " + sensor.canSilkHarvest(world(), player(), X, Y, Z, 0) + "; " + plain.getString("id") + " "
				+ plain.hasKey("last_vibration_frequency") + " " + plain.hasKey("listener") + "; " + cal.getString("id") + " phase " + phase(X + 2, Y, Z);
		});
		assertEqual("1.5 1 hoe 5 silk true; etfuturum.sculk_sensor true true; etfuturum.calibrated_sculk_sensor phase 0", got, "the stored form");
	}

	/** 26.3's state of a meta: its id, then its facing and phase where it has them; "nothing" if the command layer can't name it. */
	private static String name(Block block, int meta) {
		BlockIds.Found found = BlockIds.find(block, meta);
		if (found == null) {
			return "nothing";
		}
		Map<String, String> state = found.state().properties();
		StringBuilder name = new StringBuilder(found.type().id.replace("minecraft:", ""));
		for (String property : new String[]{"facing", "sculk_sensor_phase"}) {
			if (state.containsKey(property)) {
				name.append(",").append(state.get(property));
			}
		}
		return name.toString();
	}

	/** McFly's metas by 26.3's names (the modern commands): the plain one's phase; the calibrated one's facing (meta & 3: south, west, north, east) and phase (meta >> 2). */
	public void testNames() {
		needSensors();
		String got = onServer(() -> name(sensor(), 0) + " " + name(sensor(), 1) + " " + name(sensor(), 2) + " " + name(calibrated(), 0) + " "
			+ name(calibrated(), 1 | 4) + " " + name(calibrated(), 2 | 8) + " " + name(calibrated(), 3));
		assertEqual("sculk_sensor,inactive sculk_sensor,active sculk_sensor,cooldown calibrated_sculk_sensor,south,inactive calibrated_sculk_sensor,west,active "
			+ "calibrated_sculk_sensor,north,cooldown calibrated_sculk_sensor,east,inactive", got, "26.3's names");
	}

	// ---------------------------------------------------------------- hearing

	/**
	 * A block placed 4 off: active, power 8 (26.3's `getRedstoneStrengthForDistance`), the comparator 13 (block_place's
	 * frequency), weak power to the sides, strong only into the block below (asked from below: side 1); 30 ticks active
	 * (its tick due 30 after the clicking), then cooldown with power 0, then inactive.
	 */
	public void testHearsAndPowers() {
		needSensors();
		// a place of its own: a tick another test left due at a sensor's place would run on this one (1.7.10 and 26.3 keep one tick per place and block)
		int x = X + 6, z = Z + 2;
		Recorder recorder = new Recorder(x + 0.5D, Y + 0.5D, z + 0.5D);
		onServer(() -> {
			world().setBlock(x, Y, z, sensor(), 0, 3);
			GameEvents.register(world(), recorder);
		});
		try {
			postAndWait("block_place", x + 4.5D, Y + 0.5D, z + 0.5D, x, Y, z);
			String active = onServer(() -> {
				Block sensor = sensor();
				NBTTagCompound tag = saved(x, Y, z);
				return "phase " + phase(x, Y, z) + ", power " + tag.getInteger("power") + ", frequency " + tag.getInteger("last_vibration_frequency")
					+ ", comparator " + sensor.getComparatorInputOverride(world(), x, Y, z, 0) + ", weak east " + sensor.isProvidingWeakPower(world(), x, Y, z, 5)
					+ " north " + sensor.isProvidingWeakPower(world(), x, Y, z, 2) + ", strong below " + sensor.isProvidingStrongPower(world(), x, Y, z, 1)
					+ " east " + sensor.isProvidingStrongPower(world(), x, Y, z, 5) + ", active for " + (due(x, Y, z) - recorder.timeOf("sculk_sensor_tendrils_clicking"));
			});
			assertEqual("phase 1, power 8, frequency 13, comparator 13, weak east 8 north 8, strong below 8 east 0, active for 30", active, "active");
			waitUntil("the cooldown", () -> onServer(() -> phase(x, Y, z) == 2), 40);
			String cooldown = onServer(() -> "phase " + phase(x, Y, z) + ", power " + saved(x, Y, z).getInteger("power") + ", comparator "
				+ sensor().getComparatorInputOverride(world(), x, Y, z, 0));
			assertEqual("phase 2, power 0, comparator 0", cooldown, "in cooldown");
			waitUntil("inactive again", () -> onServer(() -> phase(x, Y, z) == 0), 20);
		} finally {
			onServer(() -> GameEvents.unregister(world(), recorder));
		}
	}

	/** It doesn't take its own placing, nor anything while active (the second event's frequency isn't kept). */
	public void testIgnoresItsOwnPlacingAndWhileActive() {
		needSensors();
		ItemStack item = new ItemStack(sensor());
		onServer(() -> {
			player().inventory.setInventorySlotContents(player().inventory.currentItem, item);
			item.tryPlaceItemIntoWorld(player(), world(), X, Y - 1, Z, 1, 0.5F, 1.0F, 0.5F);
		});
		serverTicks(5);
		String placed = onServer(() -> (world().getBlock(X, Y, Z) == sensor() ? "placed" : "not placed") + ", phase " + phase(X, Y, Z));
		assertEqual("placed, phase 0", placed, "after its own placing");
		postAndWait("explode", X + 3.5D, Y + 0.5D, Z + 0.5D, X, Y, Z);
		onServer(() -> GameEvents.post(world(), GameEvent.get("step"), X + 2.5D, Y + 0.5D, Z + 0.5D, null));
		serverTicks(5);
		String kept = onServer(() -> "frequency " + saved(X, Y, Z).getInteger("last_vibration_frequency"));
		assertEqual("frequency 15", kept, "while active");
	}

	/** A pig set on it: a step forced in (26.3's `stepOn`), from its own block: power 15, frequency 1. */
	public void testStepOnIt() {
		needSensors();
		onServer(() -> {
			world().setBlock(X, Y, Z, sensor(), 0, 3);
			EntityPig pig = new EntityPig(world());
			pig.setLocationAndAngles(X + 0.5D, Y + 0.5D, Z + 0.5D, 0.0F, 0.0F);
			world().spawnEntityInWorld(pig);
		});
		waitUntil("the sensor turning active", () -> onServer(() -> phase(X, Y, Z) == 1), 40);
		String got = onServer(() -> {
			NBTTagCompound tag = saved(X, Y, Z);
			return "power " + tag.getInteger("power") + ", frequency " + tag.getInteger("last_vibration_frequency");
		});
		assertEqual("power 15, frequency 1", got, "stepped on");
	}

	// ---------------------------------------------------------------- the calibrated one

	/**
	 * Out to 16 (an event 12 off: the calibrated one takes it, power 4; a plain one there doesn't); active 10 ticks; with
	 * a signal of 15 into its back (a redstone block behind it), only frequency 15 (block_place refused, explode taken);
	 * no signal out of its back.
	 */
	public void testCalibrated() {
		needSensors();
		Recorder recorder = new Recorder(X + 0.5D, Y + 0.5D, Z + 0.5D);
		onServer(() -> {
			// facing 2 (north): its back is south, z + 1
			world().setBlock(X, Y, Z, calibrated(), 2, 3);
			world().setBlock(X, Y, Z - 3, sensor(), 0, 3);
			GameEvents.register(world(), recorder);
		});
		try {
			postAndWait("block_place", X + 12.5D, Y + 0.5D, Z + 0.5D, X, Y, Z);
			String far = onServer(() -> "power " + saved(X, Y, Z).getInteger("power") + ", the plain one's phase " + phase(X, Y, Z - 3) + ", active for "
				+ (due(X, Y, Z) - recorder.timeOf("sculk_sensor_tendrils_clicking")) + ", back out " + calibrated().isProvidingWeakPower(world(), X, Y, Z, 2)
				+ ", front out " + calibrated().isProvidingWeakPower(world(), X, Y, Z, 3));
			assertEqual("power 4, the plain one's phase 0, active for 10, back out 0, front out 4", far, "out to 16");
			waitUntil("inactive again", () -> onServer(() -> phase(X, Y, Z) == 0), 40);
			onServer(() -> world().setBlock(X, Y, Z + 1, Blocks.redstone_block, 0, 3));
			onServer(() -> GameEvents.post(world(), GameEvent.get("block_place"), X + 3.5D, Y + 0.5D, Z + 0.5D, null));
			serverTicks(8);
			String refused = onServer(() -> "phase " + phase(X, Y, Z));
			assertEqual("phase 0", refused, "block_place with 15 behind");
			postAndWait("explode", X + 3.5D, Y + 0.5D, Z + 0.5D, X, Y, Z);
			String taken = onServer(() -> "frequency " + saved(X, Y, Z).getInteger("last_vibration_frequency"));
			assertEqual("frequency 15", taken, "explode with 15 behind");
		} finally {
			onServer(() -> GameEvents.unregister(world(), recorder));
		}
	}

	// ---------------------------------------------------------------- what it posts

	/** Turning active posts `sculk_sensor_tendrils_clicking` at it; an amethyst block next to it posts `resonate_13` (block_place's frequency). */
	public void testClickingAndResonance() {
		needSensors();
		Block amethyst = (Block) Block.blockRegistry.getObject("etfuturum:amethyst_block");
		Recorder recorder = new Recorder(X + 0.5D, Y + 0.5D, Z + 0.5D);
		onServer(() -> {
			world().setBlock(X, Y, Z, sensor(), 0, 3);
			if (amethyst != null) {
				world().setBlock(X - 1, Y, Z, amethyst, 0, 3);
			}
			GameEvents.register(world(), recorder);
		});
		try {
			postAndWait("block_place", X + 4.5D, Y + 0.5D, Z + 0.5D, X, Y, Z);
			String got = onServer(() -> recorder.heard.contains("sculk_sensor_tendrils_clicking") + " " + (amethyst == null ? "(no amethyst)"
				: String.valueOf(recorder.heard.contains("resonate_13"))));
			assertEqual("true true", got, "clicking and resonance");
		} finally {
			onServer(() -> GameEvents.unregister(world(), recorder));
		}
	}

	/** Saved with a vibration in flight (one 12 off a calibrated one: 12 ticks' travel): the event and its ticks left in `listener`; loaded back, the same. */
	public void testSavedInFlight() {
		needSensors();
		onServer(() -> world().setBlock(X, Y, Z, calibrated(), 2, 3));
		onServer(() -> GameEvents.post(world(), GameEvent.get("block_place"), X + 12.5D, Y + 0.5D, Z + 0.5D, null));
		serverTicks(4);
		String got = onServer(() -> {
			NBTTagCompound tag = saved(X, Y, Z);
			NBTTagCompound listener = tag.getCompoundTag("listener");
			String flying = listener.hasKey("event") ? listener.getCompoundTag("event").getString("game_event") : "(none)";
			int left = listener.getInteger("event_delay");
			TileEntity loaded = TileEntity.createAndLoadEntity(tag);
			NBTTagCompound again = new NBTTagCompound();
			if (loaded != null) {
				loaded.writeToNBT(again);
			}
			String flyingAgain = again.getCompoundTag("listener").hasKey("event") ? again.getCompoundTag("listener").getCompoundTag("event").getString("game_event")
				: "(none)";
			return flying + " " + (left > 0 && left < 12 ? "with ticks left" : "left " + left) + "; loaded " + flyingAgain + " "
				+ (again.getCompoundTag("listener").getInteger("event_delay") == left ? "the same" : "not the same");
		});
		assertEqual("minecraft:block_place with ticks left; loaded minecraft:block_place the same", got, "saved in flight");
	}
}
