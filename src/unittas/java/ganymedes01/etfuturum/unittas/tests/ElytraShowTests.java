package ganymedes01.etfuturum.unittas.tests;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import io.github.asgerjon.mcfly.elytra.BoostingRocket;
import io.github.asgerjon.mcfly.elytra.FallFlying;
import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The elytra, shown off (the user, 2026-11-05: "where's the... finesse, the showmanship..."): at sunset, off a quartz
 * tower, a glide down a slalom of 9 glowstone rings, then (the user: "have the player turn around and fly back through
 * each circle this time with rockets") a turn and the way back up through every ring, on rockets, a fly-by past the
 * tower's top, a landing and a firework for the bow; seen in the third person (the wings, the body's roll, 26.3's flight
 * sound).
 * <p>
 * The course is worked out when the wings open: the path a planned look (`KEYS`) flies from the player's own state by
 * 26.3's glide (`updateFallFlyingMovement`, its sine table), with a ring at each of `RING_TICKS`, square to the flight.
 * A pilot flies it on every client tick (the user's hint: a PID). Out, gliding: it follows the planned path 8 ticks
 * ahead, the plan's pitch as its base, a PID on the climb angle to that point. Back, on rockets: it goes for each ring's
 * middle, a PID on the climb angle to it over the angle itself, and uses a rocket whenever none pushes it and it's slow
 * or the ring is more than a little above. The yaw: the heading to the point plus a lead on the motion's lag, eased;
 * with the point behind it, it keeps turning the way it turns (near 180 the sign flips tick to tick and it never
 * turns). The rockets are plain (26.3's rocket with stars hurts the glider when it bursts). Tuned offline
 * (`unittas_mcfly-sims/return_sim.py`, root, git-excluded: rockets of a random 20 to 31 ticks, felt 1 to 3 ticks
 * late): 40 runs out of 40 through all 18. It passes at 17 of the 18 and alive.
 */
@TasSuite("showcase")
public class ElytraShowTests extends TasTestCase {

	private static final int GROUND = 4;
	private static final int TOWER_X = 700, TOWER_Z = 700, TOWER_TOP = 90;
	/** The planned look over time: (tick, yaw, pitch), eased between: dives for speed, swoops, an S down the sky. */
	private static final float[][] KEYS = {{0, -90, 40}, {20, -90, 30}, {36, -90, -6}, {56, -60, 8}, {76, -10, 28},
		{92, 20, -8}, {116, 60, 10}, {140, 95, 30}, {156, 120, -6}, {180, 150, 10}, {204, 170, 28}, {220, 190, -6},
		{244, 230, 8}, {268, 260, 24}, {284, 270, -4}, {320, 270, 8}};
	/** The ticks of the plan the rings stand at. */
	private static final int[] RING_TICKS = {34, 62, 90, 120, 154, 186, 216, 250, 282};
	/** How far the plan goes: the last ring, then a little. */
	private static final int PLAN_TICKS = 322;
	private static final double RING_RADIUS = 4.5;
	/** Through a ring: its plane crossed this near its middle (its blocks start at 4; the gliding body is 0.6 across). */
	private static final double CLEAR = 3.5;

	/** The show runs over a minute (out and back is about 55 s of flight): past unitTAS's 60 s a test. */
	@Override
	protected int timeoutSeconds() {
		return 240;
	}

	@Override
	protected void setUp() {
		if (Item.itemRegistry.getObject("etfuturum:elytra") == null) {
			skipTest("the elytra is off");
		}
		onServer(() -> {
			world().setWorldTime(12600);
			for (int y = GROUND; y <= TOWER_TOP; y++) {
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						world().setBlock(TOWER_X + dx, y, TOWER_Z + dz, Blocks.quartz_block, y == TOWER_TOP ? 1 : 2, 2);
					}
				}
			}
		});
	}

	public void testTheShow() {
		onServer(() -> {
			player().setGameType(WorldSettings.GameType.SURVIVAL);
			player().setHealth(player().getMaxHealth());
			player().setCurrentItemOrArmor(3, new ItemStack((Item) Item.itemRegistry.getObject("etfuturum:elytra")));
			player().inventory.setInventorySlotContents(player().inventory.currentItem, plainRockets(16));
			player().inventoryContainer.detectAndSendChanges();
			player().playerNetServerHandler.setPlayerLocation(TOWER_X + 0.5, TOWER_TOP + 1, TOWER_Z + 0.5, -90.0F, 5.0F);
		});
		waitUntilOnClient("on the tower", () -> Math.abs(mc().thePlayer.posX - (TOWER_X + 0.5)) < 0.01, 100);
		// the pose: seen from the front, then from behind
		onClient(() -> mc().gameSettings.thirdPersonView = 2);
		ticks(40);
		onClient(() -> mc().gameSettings.thirdPersonView = 1);
		ticks(20);
		// off the edge, and open the wings on the way down
		hold(mc().gameSettings.keyBindForward, 8);
		ticks(6);
		hold(mc().gameSettings.keyBindJump, 2);
		assertTrue(onClient(() -> FallFlying.isFallFlying(mc().thePlayer)), "the wings didn't open off the tower");

		// the pilot takes over from the next client tick; its first works out the course
		Pilot pilot = new Pilot();
		Witness witness = new Witness();
		onServer(() -> net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(witness));
		onClient(() -> FMLCommonHandler.instance().bus().register(pilot));
		try {
			waitUntilOnClient("the course worked out", () -> pilot.rings != null, 20);
			double[][] rings = pilot.rings;
			double[][] normals = pilot.normals;
			onServer(() -> {
				for (int r = 0; r < rings.length; r++) {
					ring(rings[r], normals[r]);
				}
			});
			waitUntilOnClient("the landing", () -> pilot.landed, 2000);
		} finally {
			onClient(() -> FMLCommonHandler.instance().bus().unregister(pilot));
			onServer(() -> net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(witness));
		}
		ticks(10);
		// the bow: seen from the front again, and a firework straight up
		onClient(() -> mc().gameSettings.thirdPersonView = 2);
		onServer(() -> world().spawnEntityInWorld(new EntityFireworkRocket(world(), player().posX + 2.0, player().posY, player().posZ, firework(0xFF7A00, 0xFFD000, 0xC74EBD))));
		ticks(60);
		onClient(() -> mc().gameSettings.thirdPersonView = 0);
		boolean alive = onServer(() -> player().isEntityAlive());
		String report = onClient(() -> pilot.report()) + "; server: " + onServer(() -> witness.toString());
		int through = onClient(() -> pilot.through);
		int all = 2 * RING_TICKS.length;
		System.out.println("[showcase] through " + through + " of " + all + ", alive " + alive + "; " + report);
		assertTrue(alive && through >= all - 1, "rings flown through: " + through + " of " + all + ", alive " + alive + "; " + report);
	}

	/** The server's side of the rockets: each use in the air (gliding there or not) and each rocket that joined. */
	public static final class Witness {

		private final StringBuilder uses = new StringBuilder();
		private int spawned;

		@SubscribeEvent
		public void onUse(net.minecraftforge.event.entity.player.PlayerInteractEvent event) {
			if (!event.world.isRemote && event.action == net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.RIGHT_CLICK_AIR) {
				uses.append(FallFlying.isFallFlying(event.entityPlayer) ? 'g' : 'n');
			}
		}

		@SubscribeEvent
		public void onJoin(net.minecraftforge.event.entity.EntityJoinWorldEvent event) {
			if (!event.world.isRemote && event.entity instanceof BoostingRocket) {
				spawned++;
			}
		}

		@Override
		public String toString() {
			return "uses in the air (g gliding, n not) [" + uses + "], rockets spawned " + spawned;
		}
	}

	/**
	 * Flies the course on the client, every client tick: at the tick's start it sets the look (before the player's
	 * update moves it) and uses a rocket when it wants one, at its end it sees what came of it. Its first tick works out
	 * the course from the player's state then.
	 */
	public static final class Pilot {

		private enum Phase {OUT, TURN, BACK, FLYBY, LAND}

		/** Out: follow the plan this many ticks ahead of the nearest point of it. */
		private static final int LOOKAHEAD = 8;
		private static final double OUT_LEAD = 0.8, OUT_LEAD_MAX = 20.0, OUT_KP = 1.2, OUT_KI = 0.01, OUT_KD = 0.1, OUT_I_MAX = 150.0;
		/**
		 * D was 2.0 on both legs and shook the pitch (the user: "you are vibrating *way* too hard! reduce that D"): in the
		 * offline model 21 pitch reversals in 100 ticks at 2.0, 5 at 0.1, every ring still threaded (worst 0.4 off).
		 */
		/** Back: for each ring's middle. */
		private static final double BACK_LEAD = 0.8, BACK_LEAD_MAX = 25.0, BACK_KP = 1.5, BACK_KI = 0.02, BACK_KD = 0.1, BACK_I_MAX = 200.0;
		private static final double YAW_RATE = 6.0, PITCH_UP = 40.0, PITCH_DOWN = 55.0, PITCH_RATE = 4.0;
		/** A rocket when none pushes it and it's slower than this, or the point is this many degrees above. */
		private static final double ROCKET_SPEED = 0.8, ROCKET_CLIMB = 4.0;
		/** The ticks after a rocket's use before another: the rocket is felt 1 to 3 ticks late. */
		private static final int ROCKET_GAP = 6;
		/** The turn after the last ring: this far on past it and this far to its side, 4 up. */
		private static final double TURN_OUT = 30.0, TURN_SIDE = 18.0;
		/** After the course: down at 20, level 10 up, a gentle flare 2 up. */
		private static final double APPROACH = 20.0, LEVEL = 2.0, FLARE = -5.0;

		volatile double[][] rings;
		volatile double[][] normals;
		volatile boolean landed;
		volatile int through;

		private Phase phase = Phase.OUT;
		private double[][] points;
		private float[] plannedPitch;
		private int tick = -1;
		private int nearest;
		/** The rings in the order to fly them: out 0 to 8, back 8 to 0; the next of them. */
		private int[] order;
		private int next;
		/** Turn, back and the fly-by: the points to go for; the one now. */
		private final List<double[]> targets = new ArrayList<>();
		private int target;
		private float yaw, pitch, lastYaw, lastPitch;
		private int turning;
		private double integral;
		private double lastError = Double.NaN;
		private int usedAt = -100;
		private int rockets;
		private boolean boosting;
		private double[] before;
		private double[] closest;
		private final StringBuilder passes = new StringBuilder();
		private final StringBuilder track = new StringBuilder();
		private final StringBuilder boosts = new StringBuilder();

		@SubscribeEvent
		public void onClientTick(TickEvent.ClientTickEvent event) {
			Minecraft mc = Minecraft.getMinecraft();
			EntityClientPlayerMP player = mc.thePlayer;
			if (player == null || landed || mc.isGamePaused()) {
				return;
			}
			if (event.phase == TickEvent.Phase.START) {
				steer(mc, player);
			} else if (tick >= 0) {
				watch(player);
			}
		}

		private static double[] centre(EntityClientPlayerMP player) {
			return new double[]{player.posX, player.boundingBox.minY + 0.3, player.posZ};
		}

		private void steer(Minecraft mc, EntityClientPlayerMP player) {
			double[] at = centre(player);
			if (tick < 0) {
				plan(at, player.motionX, player.motionY, player.motionZ);
				yaw = player.rotationYaw;
				pitch = player.rotationPitch;
				tick = 0;
			}
			before = at;
			double mx = player.motionX, my = player.motionY, mz = player.motionZ;
			double moveHor = Math.sqrt(mx * mx + mz * mz);
			double speed = Math.sqrt(mx * mx + my * my + mz * mz);
			boosting = boostedBy(mc, player);
			double yawCommand;
			double pitchCommand;
			if (phase == Phase.OUT) {
				// the nearest point of the plan, searched forward only
				int best = nearest;
				for (int j = nearest + 1; j < Math.min(points.length, nearest + 60); j++) {
					if (distance(points[j], at) < distance(points[best], at)) {
						best = j;
					}
				}
				nearest = best;
				double[] carrot = points[Math.min(nearest + LOOKAHEAD, points.length - 1)];
				double[] aim = aim(carrot, at, mx, my, mz, moveHor, OUT_LEAD, OUT_LEAD_MAX, OUT_KP, OUT_KI, OUT_KD, OUT_I_MAX);
				yawCommand = aim[0];
				pitchCommand = plannedPitch[nearest] + aim[1];
				if (nearest >= points.length - 1 - LOOKAHEAD) {
					// the plan ran out before the last ring was passed: on to the turn
					startTurn();
				}
			} else if (phase == Phase.LAND) {
				double above = player.boundingBox.minY - GROUND;
				yawCommand = yaw;
				pitchCommand = above < 2 ? FLARE : above < 10 ? LEVEL : APPROACH;
			} else {
				double[] point = targets.get(target);
				double[] aim = aim(point, at, mx, my, mz, moveHor, BACK_LEAD, BACK_LEAD_MAX, BACK_KP, BACK_KI, BACK_KD, BACK_I_MAX);
				yawCommand = aim[0];
				// aim[2] is the angle down to the point: its own base
				pitchCommand = aim[2] + aim[1];
				if (!boosting && tick - usedAt > ROCKET_GAP && (speed < ROCKET_SPEED || -aim[2] > ROCKET_CLIMB)
					&& player.getHeldItem() != null && player.getHeldItem().getItem() == Items.fireworks) {
					mc.playerController.sendUseItem(player, mc.theWorld, player.getHeldItem());
					usedAt = tick;
					rockets++;
				}
			}
			pitchCommand = Math.max(-PITCH_UP, Math.min(PITCH_DOWN, pitchCommand));
			double error = wrap(yawCommand - yaw);
			if (Math.abs(error) > 150.0 && turning != 0) {
				error = turning * Math.abs(error);
			}
			turning = Math.abs(error) > 30.0 ? (error > 0 ? 1 : -1) : 0;
			yaw += (float) clamp(error, YAW_RATE);
			pitch += (float) clamp(pitchCommand - pitch, PITCH_RATE);
			lastYaw = player.rotationYaw;
			lastPitch = player.rotationPitch;
			player.rotationYaw = yaw;
			player.rotationYawHead = yaw;
			player.rotationPitch = pitch;
		}

		/**
		 * The yaw for a point (its heading plus a lead on the motion's lag) and the PID's share of the pitch (on the
		 * climb angle to it over the angle flown), and the angle down to the point: {yaw, pid, angle down}.
		 */
		private double[] aim(double[] point, double[] at, double mx, double my, double mz, double moveHor, double lead, double leadMax,
			double kp, double ki, double kd, double iMax) {
			double dx = point[0] - at[0];
			double dz = point[2] - at[2];
			double want = Math.toDegrees(Math.atan2(-dx, dz));
			double moving = Math.toDegrees(Math.atan2(-mx, mz));
			double yawCommand = want + clamp(lead * wrap(want - moving), leadMax);
			double slope = Math.toDegrees(Math.atan2(at[1] - point[1], Math.max(Math.sqrt(dx * dx + dz * dz), 1.0)));
			double flown = Math.toDegrees(Math.atan2(-my, Math.max(moveHor, 1e-3)));
			double error = slope - flown;
			integral = clamp(integral + error, iMax);
			double change = Double.isNaN(lastError) ? 0.0 : error - lastError;
			lastError = error;
			return new double[]{yawCommand, kp * error + ki * integral + kd * change, slope};
		}

		/** Every rocket the client has seen (for the report). */
		private final java.util.Set<Integer> seen = new java.util.HashSet<>();
		private final StringBuilder kinds = new StringBuilder();

		/** A rocket of ours pushing this player now (the client's copy of it, which pushes the client's player). */
		private boolean boostedBy(Minecraft mc, EntityClientPlayerMP player) {
			for (Object entity : mc.theWorld.loadedEntityList) {
				if (entity instanceof EntityFireworkRocket firework && seen.add(firework.getEntityId()) && kinds.length() < 400) {
					String boosts;
					try {
						boosts = String.valueOf(firework.getDataWatcher().getWatchableObjectInt(9));
					} catch (RuntimeException e) {
						boosts = "none";
					}
					kinds.append(String.format(" %s(9=%s, player %d, %.1f off)", firework.getClass().getSimpleName(), boosts, player.getEntityId(),
						Math.sqrt(firework.getDistanceSqToEntity(player))));
				}
				if (entity instanceof BoostingRocket rocket && rocket.isAttached() && !rocket.isDead && rocket.getDistanceSqToEntity(player) < 9.0) {
					return true;
				}
			}
			return false;
		}

		private void watch(EntityClientPlayerMP player) {
			double[] at = centre(player);
			// the update set the last look to this one: ease from the last tick's look to this one's when drawn
			player.prevRotationYaw = lastYaw;
			player.prevRotationYawHead = lastYaw;
			player.prevRotationPitch = lastPitch;
			for (int r = 0; r < rings.length; r++) {
				closest[r] = Math.min(closest[r], distance(at, rings[r]));
			}
			if ((phase == Phase.OUT || phase == Phase.BACK) && next < order.length) {
				int r = order[next];
				double[] c = rings[r];
				double[] n = normals[r];
				double s0 = (before[0] - c[0]) * n[0] + (before[2] - c[2]) * n[2];
				double s1 = (at[0] - c[0]) * n[0] + (at[2] - c[2]) * n[2];
				// either way: out along the flight, back against it
				if ((s0 < 0.0 && s1 >= 0.0) || (s0 > 0.0 && s1 <= 0.0)) {
					double f = s0 / (s0 - s1);
					double[] crossing = {before[0] + (at[0] - before[0]) * f, before[1] + (at[1] - before[1]) * f, before[2] + (at[2] - before[2]) * f};
					double off = distance(crossing, c);
					if (off < 15.0) {
						if (off < CLEAR) {
							through++;
						}
						passes.append(String.format(" %s%d:%.1f@%d", phase == Phase.OUT ? "out" : "back", r, off, tick));
						next++;
						integral = 0.0;
						lastError = Double.NaN;
						if (phase == Phase.OUT && next == rings.length) {
							startTurn();
						} else if (phase == Phase.BACK) {
							target++;
							if (next == order.length) {
								startFlyBy();
							}
						}
					}
				}
			}
			if (phase == Phase.TURN && distance(at, targets.get(0)) < 8.0) {
				phase = Phase.BACK;
				target = 1;
			}
			if (phase == Phase.FLYBY) {
				double[] point = targets.get(target);
				if (Math.hypot(at[0] - point[0], at[2] - point[2]) < 6.0) {
					target++;
					if (target == targets.size()) {
						phase = Phase.LAND;
					}
				} else if (target == targets.size() - 1 && at[0] < TOWER_X - 40) {
					phase = Phase.LAND;
				}
			}
			boolean pushed = boostedBy(Minecraft.getMinecraft(), player);
			if (pushed != boosting) {
				boosts.append(pushed ? " from " + tick : " to " + tick);
			}
			if (tick % 20 == 0) {
				track.append(String.format(" t%d %s (%.0f %.0f %.0f) %.0f/%.0f", tick, phase, at[0], at[1], at[2], yaw, pitch));
			}
			if (!FallFlying.isFallFlying(player) && player.onGround) {
				landed = true;
			}
			tick++;
		}

		/** Past the last ring: out and to its side, a little up, then back through the rings from the last. */
		private void startTurn() {
			phase = Phase.TURN;
			double[] c = rings[rings.length - 1];
			double[] n = normals[normals.length - 1];
			targets.clear();
			targets.add(new double[]{c[0] + n[0] * TURN_OUT - n[2] * TURN_SIDE, c[1] + 4.0, c[2] + n[2] * TURN_OUT + n[0] * TURN_SIDE});
			for (int r = rings.length - 1; r >= 0; r--) {
				targets.add(rings[r]);
			}
			target = 0;
			next = rings.length;
		}

		/** Through the first ring again, heading for the tower: past its top 22 to the south, then on west and down. */
		private void startFlyBy() {
			phase = Phase.FLYBY;
			targets.clear();
			targets.add(new double[]{TOWER_X, TOWER_TOP + 10, TOWER_Z + 22});
			targets.add(new double[]{TOWER_X - 60, 75, TOWER_Z + 22});
			target = 0;
		}

		/** The course: the path the planned look flies from this state, a ring at each ring tick, square to the flight. */
		private void plan(double[] at, double mx, double my, double mz) {
			double x = at[0], y = at[1], z = at[2];
			double[][] path = new double[PLAN_TICKS][];
			float[] pitches = new float[PLAN_TICKS];
			double[][] course = new double[RING_TICKS.length][];
			double[][] faces = new double[RING_TICKS.length][];
			int ring = 0;
			for (int t = 0; t < PLAN_TICKS; t++) {
				float[] look = look(t);
				double[] motion = glide(look[1], view(look[1], look[0]), mx, my, mz);
				mx = motion[0];
				my = motion[1];
				mz = motion[2];
				x += mx;
				y += my;
				z += mz;
				path[t] = new double[]{x, y, z};
				pitches[t] = look[1];
				if (ring < RING_TICKS.length && t == RING_TICKS[ring]) {
					double h = Math.sqrt(mx * mx + mz * mz);
					course[ring] = new double[]{x, y, z};
					faces[ring] = new double[]{mx / h, 0.0, mz / h};
					ring++;
				}
			}
			order = new int[2 * course.length];
			for (int r = 0; r < course.length; r++) {
				order[r] = r;
				order[course.length + r] = course.length - 1 - r;
			}
			points = path;
			plannedPitch = pitches;
			closest = new double[course.length];
			Arrays.fill(closest, Double.MAX_VALUE);
			normals = faces;
			rings = course;
		}

		String report() {
			StringBuilder near = new StringBuilder();
			for (int r = 0; r < rings.length; r++) {
				near.append(String.format(" %d (%.0f %.0f %.0f) %.1f", r, rings[r][0], rings[r][1], rings[r][2], closest[r]));
			}
			return String.format("passes (ring:off@tick)%s; rockets %d, rockets seen %d:%s, pushed%s; closest%s; flown%s", passes, rockets, seen.size(), kinds, boosts, near, track);
		}
	}

	private static double distance(double[] a, double[] b) {
		double dx = a[0] - b[0], dy = a[1] - b[1], dz = a[2] - b[2];
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	private static double clamp(double value, double limit) {
		return Math.max(-limit, Math.min(limit, value));
	}

	private static double wrap(double degrees) {
		double d = degrees % 360.0;
		if (d > 180.0) {
			d -= 360.0;
		} else if (d < -180.0) {
			d += 360.0;
		}
		return d;
	}

	private static float[] look(int tick) {
		for (int i = 0; i + 1 < KEYS.length; i++) {
			float[] a = KEYS[i];
			float[] b = KEYS[i + 1];
			if (tick >= a[0] && tick <= b[0]) {
				float f = (tick - a[0]) / (b[0] - a[0]);
				return new float[]{a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f};
			}
		}
		float[] last = KEYS[KEYS.length - 1];
		return new float[]{last[1], last[2]};
	}

	/** 26.3's `calculateViewVector`, its sine table's way. */
	private static double[] view(float pitch, float yaw) {
		float f = pitch * (float) (Math.PI / 180.0);
		float g = -yaw * (float) (Math.PI / 180.0);
		return new double[]{sin26(g) * cos26(f), -sin26(f), cos26(g) * cos26(f)};
	}

	/** 26.3's `updateFallFlyingMovement` (gravity 0.08). */
	private static double[] glide(float pitch, double[] look, double x, double y, double z) {
		float lean = pitch * (float) (Math.PI / 180.0);
		double lookHor = Math.sqrt(look[0] * look[0] + look[2] * look[2]);
		double moveHor = Math.sqrt(x * x + z * z);
		double lift = Math.cos(lean) * Math.cos(lean);
		y += 0.08 * (-1.0 + lift * 0.75);
		if (y < 0.0 && lookHor > 0.0) {
			double convert = y * -0.1 * lift;
			x += look[0] * convert / lookHor;
			y += convert;
			z += look[2] * convert / lookHor;
		}
		if (lean < 0.0F && lookHor > 0.0) {
			double convert = moveHor * -sin26(lean) * 0.04;
			x += -look[0] * convert / lookHor;
			y += convert * 3.2;
			z += -look[2] * convert / lookHor;
		}
		if (lookHor > 0.0) {
			x += (look[0] / lookHor * moveHor - x) * 0.1;
			z += (look[2] / lookHor * moveHor - z) * 0.1;
		}
		return new double[]{x * 0.99F, y * 0.98F, z * 0.99F};
	}

	private static float sin26(double angle) {
		return (float) Math.sin(((long) (angle * 10430.378350470453) & 65535L) / 10430.378350470453);
	}

	private static float cos26(double angle) {
		return (float) Math.sin(((long) (angle * 10430.378350470453 + 16384.0) & 65535L) / 10430.378350470453);
	}

	/** A glowstone ring of the radius round the middle, standing square to the flight there (its normal). */
	private static void ring(double[] middle, double[] normal) {
		double sideX = -normal[2];
		double sideZ = normal[0];
		for (int step = 0; step < 48; step++) {
			double angle = step * Math.PI * 2.0 / 48;
			int x = (int) Math.floor(middle[0] + RING_RADIUS * Math.cos(angle) * sideX);
			int y = (int) Math.floor(middle[1] + RING_RADIUS * Math.sin(angle));
			int z = (int) Math.floor(middle[2] + RING_RADIUS * Math.cos(angle) * sideZ);
			world().setBlock(x, y, z, Blocks.glowstone, 0, 2);
		}
	}

	/** Rockets of one flight with no stars, for the boosts (26.3's rocket with stars hurts the glider it pushes). */
	private static ItemStack plainRockets(int count) {
		ItemStack stack = new ItemStack(Items.fireworks, count);
		NBTTagCompound fireworks = new NBTTagCompound();
		fireworks.setByte("Flight", (byte) 1);
		NBTTagCompound tag = new NBTTagCompound();
		tag.setTag("Fireworks", fireworks);
		stack.setTagCompound(tag);
		return stack;
	}

	/** A firework of one flight that bursts big and coloured. */
	private static ItemStack firework(int... colours) {
		ItemStack stack = new ItemStack(Items.fireworks, 1);
		NBTTagCompound burst = new NBTTagCompound();
		burst.setByte("Type", (byte) 1);
		burst.setBoolean("Trail", true);
		burst.setBoolean("Flicker", true);
		burst.setIntArray("Colors", colours);
		burst.setIntArray("FadeColors", new int[]{0xFFFFFF});
		NBTTagList bursts = new NBTTagList();
		bursts.appendTag(burst);
		NBTTagCompound fireworks = new NBTTagCompound();
		fireworks.setByte("Flight", (byte) 1);
		fireworks.setTag("Explosions", bursts);
		NBTTagCompound tag = new NBTTagCompound();
		tag.setTag("Fireworks", fireworks);
		stack.setTagCompound(tag);
		return stack;
	}
}
