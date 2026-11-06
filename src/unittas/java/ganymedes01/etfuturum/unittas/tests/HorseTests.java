package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.unittas.TasSuite;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import io.github.asgerjon.unittas.TasTestCase;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLeashKnot;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * Horses in water and undead horses on leads (SPEC-modern-movement.md §64.6; 26.3's `SkeletonHorse`, `ZombieHorse`,
 * `AbstractHorse`, `LivingEntity.travelInWater`, `floatInLiquidWhileRidden`, the entity type tags). The user rode a
 * skeleton horse that swam with them on it, where 26.3's walks along the bottom, and it took no lead.
 * <p>
 * Built on the flat world near the spawn, cleared after each test:
 * <ul>
 * <li>the deep pool: water x 8..16, z 8..30, y 4..9 (6 deep, its top at 9.89);</li>
 * <li>the tunnel: water x 20..22, z 8..30, y 4..5, under a glass roof at y 6 (a floating horse is held under, its
 * rider's head in the glass, out of the water);</li>
 * <li>two shallow pools, 2 deep: water x 26..30, z 8..12; lava x 26..30, z 20..24.</li>
 * </ul>
 */
@TasSuite("quick5")
public class HorseTests extends TasTestCase {

	private static final int Y = 4;
	private static final String MODERN = McFly.MC_ASSETS + ":";
	private static final int PLAIN = 0, ZOMBIE = 3, SKELETON = 4;

	@Override
	protected int timeoutSeconds() {
		return 90;
	}

	@Override
	protected void tearDown() {
		keyUp(mc().gameSettings.keyBindForward);
		onServer(() -> {
			if (player().ridingEntity != null) {
				player().mountEntity(null);
			}
			fill(6, Y, 6, 32, Y + 7, 32, Blocks.air, 0);
			fill(-1, Y, -6, 4, Y, -3, Blocks.air, 0);
		});
	}

	// ---------------------------------------------------------------- floating

	/** An unridden skeleton horse sinks (no float, sinking by 0.005: -0.025 a tick at most) to the bottom; a horse floats. */
	public void testSkeletonHorseSinksToTheBottom() {
		needWater();
		deepPool();
		EntityHorse skeleton = horse(SKELETON, 10.5D, 7.0D, 15.5D, false, false);
		EntityHorse plain = horse(PLAIN, 14.5D, 7.0D, 15.5D, false, false);
		serverTicks(30);
		double sinking = onServer(() -> skeleton.motionY);
		assertAlmostEqual(-0.025D, sinking, 0.006D);
		waitUntil("the skeleton horse on the bottom", () -> skeleton.onGround && skeleton.posY < Y + 0.1D, 300);
		double plainY = onServer(() -> plain.posY);
		assertTrue(plainY > 7.5D, "the horse sank too, to " + plainY);
	}

	/**
	 * A skeleton horse on the bottom sent to a spot at the water's top stays on the bottom: its paths don't swim (1.7.10's
	 * swim task sets every horse's paths to start at the top, where it would jump up to them).
	 */
	public void testSkeletonHorseStaysOnTheBottom() {
		needWater();
		deepPool();
		EntityHorse skeleton = horse(SKELETON, 12.5D, Y, 12.5D, false, false);
		waitUntil("the skeleton horse on the bottom", () -> skeleton.onGround, 40);
		double highest = Y;
		for (int tick = 0; tick < 100; tick++) {
			if (tick % 20 == 0) {
				onServer(() -> skeleton.getNavigator().tryMoveToXYZ(12.5D, 10.0D, 20.5D, 1.0D));
			}
			serverTicks(1);
			highest = Math.max(highest, onServer(() -> skeleton.posY));
		}
		assertTrue(highest < Y + 0.6D, "the skeleton horse went up to " + highest);
	}

	/** A ridden skeleton horse goes down with its rider, who stays on it under water; a horse puts its rider off. */
	public void testRiderStaysOnTheSkeletonHorseUnderWater() {
		needWater();
		deepPool();
		EntityHorse skeleton = horse(SKELETON, 12.5D, 7.0D, 15.5D, true, false);
		ride(skeleton);
		waitUntil("the skeleton horse on the bottom with its rider", () -> skeleton.onGround && skeleton.posY < Y + 0.1D && player().ridingEntity == skeleton, 300);
		assertTrue(onServer(() -> player().isInsideOfMaterial(Material.water)), "the rider's eyes out of the water");
		serverTicks(40);
		assertTrue(onServer(() -> player().ridingEntity == skeleton), "the rider was put off under water");
		EntityHorse plain = horse(PLAIN, 9.5D, Y, 25.5D, true, true);
		assertTrue(onServer(() -> plain.shouldDismountInWater(player())), "a horse keeps its rider under water");
		assertFalse(onServer(() -> skeleton.shouldDismountInWater(player())), "the skeleton horse puts its rider off");
	}

	/**
	 * The ridden float (`riddenHorsesInWater`, 26.3's `floatInLiquidWhileRidden`): a ridden horse with no AI of its own
	 * goes up in water deeper than 0.4; not in lava, and a skeleton horse not at all.
	 */
	public void testRiddenHorsesFloatInWaterOnly() {
		if (!ConfigMixins.riddenHorsesInWater) {
			skipTest("riddenHorsesInWater is off");
		}
		shallowPools();
		double water = riddenRise(PLAIN, 28.5D, 10.5D);
		dismount();
		double skeleton = riddenRise(SKELETON, 28.5D, 10.5D);
		dismount();
		double lava = riddenRise(PLAIN, 28.5D, 22.5D);
		String seen = "went up: a horse in water " + water + ", a skeleton horse in water " + skeleton + ", a horse in lava " + lava;
		assertTrue(water > 0.8D, seen + "; the horse didn't float in water");
		assertTrue(skeleton < 0.1D, seen + "; the skeleton horse floated");
		assertTrue(lava < 0.1D, seen + "; the horse floated in lava");
	}

	// ---------------------------------------------------------------- moving

	/**
	 * Holding forward under water (the tunnel), a ridden horse keeps 0.8 of its speed a tick and is pushed 0.02 × 0.98,
	 * so its top speed is 0.078 a tick; a skeleton horse keeps 0.96, so 0.47 (26.3's `getWaterSlowDown`).
	 */
	public void testRiddenSpeedUnderWater() {
		needWater();
		tunnel();
		double[] plain = waterDrag(PLAIN, 20.9D);
		dismount();
		double[] skeleton = waterDrag(SKELETON, 21.5D);
		String seen = "a horse kept " + plain[0] + " (top " + plain[1] + "), a skeleton horse " + skeleton[0] + " (top " + skeleton[1] + ")";
		assertTrue(Math.abs(plain[0] - 0.8D) < 0.01D, seen + "; wanted 0.8 for the horse");
		assertTrue(Math.abs(plain[1] - 0.0784D) < 0.01D, seen + "; wanted a top speed of 0.078 for the horse");
		assertTrue(Math.abs(skeleton[0] - 0.96D) < 0.01D, seen + "; wanted 0.96 for the skeleton horse");
		assertTrue(Math.abs(skeleton[1] - 0.4704D) < 0.05D, seen + "; wanted a top speed of 0.47 for the skeleton horse");
	}

	// ---------------------------------------------------------------- breathing

	/** Under water a second, zombie and skeleton horses keep all their air, a horse loses some. */
	public void testUndeadHorsesBreatheUnderWater() {
		needWater();
		deepPool();
		EntityHorse zombie = horse(ZOMBIE, 9.5D, Y, 20.5D, false, true);
		EntityHorse skeleton = horse(SKELETON, 12.5D, Y, 20.5D, false, true);
		EntityHorse plain = horse(PLAIN, 15.5D, Y, 20.5D, false, true);
		serverTicks(30);
		int[] air = onServer(() -> new int[]{zombie.getAir(), skeleton.getAir(), plain.getAir()});
		String seen = "air: zombie horse " + air[0] + ", skeleton horse " + air[1] + ", horse " + air[2];
		assertTrue(onServer(() -> plain.isInsideOfMaterial(Material.water)), "the horse's eyes out of the water");
		assertTrue(air[2] < 300, seen + ": the horse breathes under water");
		assertEqual(300, air[0], seen);
		assertEqual(300, air[1], seen);
	}

	// ---------------------------------------------------------------- leads

	/**
	 * A skeleton horse and a zombie horse take a lead, and a skeleton horse on one is tied to a fence; a zombie horse
	 * ridden by a mob only when tamed (26.3's zombie horse jockeys); a horse as always.
	 */
	public void testUndeadHorsesTakeLeads() {
		if (!McFlyConfig.undeadHorseLeads) {
			skipTest("undeadHorseLeads is off");
		}
		setHeld(new ItemStack(Items.lead, 16));
		EntityHorse plain = horse(PLAIN, 3.5D, Y, 3.5D, false, false);
		EntityHorse skeleton = horse(SKELETON, -2.5D, Y, 3.5D, false, false);
		EntityHorse zombie = horse(ZOMBIE, 3.5D, Y, -2.5D, false, false);
		// still: an untamed horse bucks off a rider now and then (1.7.10's EntityAIRunAroundLikeCrazy)
		EntityHorse jockey = horse(ZOMBIE, -3.5D, Y, -2.5D, false, true);
		onServer(() -> {
			EntityZombie rider = new EntityZombie(world());
			rider.setLocationAndAngles(-3.5D, Y, -2.5D, 0.0F, 0.0F);
			rider.tasks.taskEntries.clear();
			rider.targetTasks.taskEntries.clear();
			world().spawnEntityInWorld(rider);
			rider.mountEntity(jockey);
		});
		serverTicks(2);
		assertTrue(onServer(() -> jockey.riddenByEntity instanceof EntityZombie), "the zombie isn't on the zombie horse: " + onServer(() -> String.valueOf(jockey.riddenByEntity)));
		List<String> unleashed = new ArrayList<>();
		for (EntityHorse horse : new EntityHorse[]{plain, skeleton, zombie}) {
			if (!leash(horse)) {
				unleashed.add(name(horse));
			}
		}
		assertEqual(new ArrayList<String>(), unleashed, "took no lead");
		assertFalse(leash(jockey), "an untamed zombie horse ridden by a zombie took a lead");
		onServer(() -> jockey.setHorseTamed(true));
		assertTrue(leash(jockey), "a tamed zombie horse ridden by a zombie took no lead");
		clearSounds();
		onServer(() -> {
			world().setBlock(1, Y, -4, Blocks.fence);
			Items.lead.onItemUse(player().getHeldItem(), player(), world(), 1, Y, -4, 1, 0.5F, 0.5F, 0.5F);
		});
		assertTrue(onServer(() -> skeleton.getLeashedToEntity() instanceof EntityLeashKnot), "the skeleton horse wasn't tied to the fence");
		// EFR's knot sound (`leashSounds`), 26.3's: the lead's
		if (ganymedes01.etfuturum.configuration.configs.ConfigSounds.leashSounds) {
			waitForSound(MODERN + "item.lead.tied", 10);
		}
	}

	// ---------------------------------------------------------------- sounds

	/** The skeleton horse's ambient sound with its eyes in water is `ambient_water`; out of it 1.7.10's idle. */
	public void testSkeletonHorseAmbientInWater() {
		needWater();
		deepPool();
		EntityHorse under = horse(SKELETON, 12.5D, Y, 9.5D, false, true);
		EntityHorse dry = horse(SKELETON, 4.5D, Y, 2.5D, false, true);
		onServer(() -> player().playerNetServerHandler.setPlayerLocation(12.5D, Y, 4.5D, 0.0F, 0.0F));
		ticks(5);
		clearSounds();
		onServer(() -> under.playLivingSound());
		waitForSound(MODERN + "entity.skeleton_horse.ambient_water", 10);
		clearSounds();
		onServer(() -> dry.playLivingSound());
		waitForSound("minecraft:mob.horse.skeleton.idle", 10);
	}

	/** Moving through water off the ground, a ridden skeleton horse plays its swim sound (no 1.7.10 swim). */
	public void testSkeletonHorseSwimSound() {
		needWater();
		deepPool();
		EntityHorse skeleton = horse(SKELETON, 12.5D, 7.0D, 10.5D, true, false);
		ride(skeleton);
		look(0.0F, 0.0F);
		ticks(3);
		clearSounds();
		keyDown(mc().gameSettings.keyBindForward);
		waitForSound(MODERN + "entity.skeleton_horse.swim", 40);
		keyUp(mc().gameSettings.keyBindForward);
		assertFalse(onServer(() -> skeleton.onGround), "the skeleton horse is on the ground");
		assertNotIn("minecraft:game.neutral.swim", soundsHeard(), "1.7.10's swim sound played");
	}

	/**
	 * Walking on the bottom, a ridden skeleton horse plays its hooves on the block and no swim sound (26.3 plays the swim
	 * sound only when a step makes none; 1.7.10 plays both); a jump from the bottom plays `jump_water`.
	 */
	public void testSkeletonHorseOnTheBottom() {
		needWater();
		deepPool();
		EntityHorse skeleton = horse(SKELETON, 12.5D, Y, 10.5D, true, false);
		ride(skeleton);
		look(0.0F, 0.0F);
		waitUntil("the skeleton horse on the bottom with its rider", () -> skeleton.onGround && player().ridingEntity == skeleton, 40);
		clearSounds();
		keyDown(mc().gameSettings.keyBindForward);
		ticks(30);
		List<String> heard = soundsHeard();
		assertTrue(heard.contains("minecraft:mob.horse.wood") || heard.contains("minecraft:mob.horse.gallop"), "no hooves on the bottom; heard " + heard);
		assertNotIn("minecraft:game.neutral.swim", heard, "a swim sound on the bottom");
		assertNotIn(MODERN + "entity.skeleton_horse.swim", heard, "a swim sound on the bottom");
		assertTrue(onServer(() -> skeleton.onGround), "the skeleton horse left the bottom");
		clearSounds();
		onServer(() -> skeleton.setJumpPower(90));
		waitForSound(MODERN + "entity.skeleton_horse.jump_water", 10);
		assertNotIn("minecraft:mob.horse.jump", soundsHeard(), "the dry jump sound in water");
	}

	// ---------------------------------------------------------------- helpers

	private static void needWater() {
		if (!McFlyConfig.modernHorsesInWater) {
			skipTest("modernHorsesInWater is off");
		}
	}

	private static String name(EntityHorse horse) {
		return new String[]{"a horse", "a donkey", "a mule", "a zombie horse", "a skeleton horse"}[horse.getHorseType()];
	}

	/** A horse of the type there (tame: tamed and saddled; still: no AI of its own); read it inside onServer. */
	private EntityHorse horse(int type, double x, double y, double z, boolean tame, boolean still) {
		return onServer(() -> {
			EntityHorse horse = new EntityHorse(world());
			horse.setHorseType(type);
			horse.setLocationAndAngles(x, y, z, 0.0F, 0.0F);
			if (tame) {
				horse.setHorseTamed(true);
				horse.setHorseSaddled(true);
			}
			if (still) {
				horse.tasks.taskEntries.clear();
			}
			world().spawnEntityInWorld(horse);
			return horse;
		});
	}

	private void ride(EntityHorse horse) {
		onServer(() -> player().mountEntity(horse));
		waitUntilOnClient("riding", () -> mc().thePlayer.ridingEntity != null, 40);
	}

	private void dismount() {
		keyUp(mc().gameSettings.keyBindForward);
		onServer(() -> {
			Object riding = player().ridingEntity;
			player().mountEntity(null);
			if (riding instanceof EntityHorse horse) {
				horse.setDead();
			}
		});
		waitUntilOnClient("off the horse", () -> mc().thePlayer.ridingEntity == null, 40);
	}

	/** The player holding a lead uses it on the horse; whether the horse is then on the player's lead. */
	private boolean leash(EntityHorse horse) {
		return onServer(() -> {
			player().interactWith(horse);
			return horse.getLeashed() && horse.getLeashedToEntity() == player();
		});
	}

	/** How far a ridden horse with no AI of its own, put on the bottom of a 2 deep pool, goes up in 30 ticks. */
	private double riddenRise(int type, double x, double z) {
		EntityHorse horse = horse(type, x, Y, z, true, true);
		onServer(() -> horse.addPotionEffect(new PotionEffect(Potion.fireResistance.id, 600, 0)));
		ride(horse);
		serverTicks(30);
		return onServer(() -> horse.posY) - Y;
	}

	/**
	 * A ridden horse held forward in the tunnel: from its speed tick by tick (each tick's speed is the last plus the
	 * push, times what it keeps), what it keeps a tick and its top speed.
	 */
	private double[] waterDrag(int type, double x) {
		EntityHorse horse = horse(type, x, Y, 9.5D, true, false);
		ride(horse);
		look(0.0F, 0.0F);
		ticks(5);
		keyDown(mc().gameSettings.keyBindForward);
		waitUntil("moving", () -> speed(horse) > 0.02D, 40);
		long[] times = new long[10];
		List<Double> speeds = new ArrayList<>();
		for (int i = 0; i < times.length; i++) {
			serverTicks(1);
			double[] sample = onServer(() -> new double[]{world().getTotalWorldTime(), speed(horse)});
			times[i] = (long) sample[0];
			speeds.add(sample[1]);
		}
		keyUp(mc().gameSettings.keyBindForward);
		// the samples are a tick or two apart (a server step waits for a tick's end): with the input steady, the speed
		// at tick t is top + b × kept^t whatever the gaps; for each kept, top and b by least squares, the best kept kept
		double best = Double.MAX_VALUE, bestKept = 0.0D, bestTop = 0.0D;
		for (double kept = 0.5D; kept < 0.9995D; kept += 0.0005D) {
			int n = speeds.size();
			double[] decay = new double[n];
			double mx = 0.0D, my = 0.0D;
			for (int i = 0; i < n; i++) {
				decay[i] = Math.pow(kept, times[i] - times[0]);
				mx += decay[i] / n;
				my += speeds.get(i) / n;
			}
			double sxx = 0.0D, sxy = 0.0D;
			for (int i = 0; i < n; i++) {
				sxx += (decay[i] - mx) * (decay[i] - mx);
				sxy += (decay[i] - mx) * (speeds.get(i) - my);
			}
			double b = sxy / sxx, top = my - b * mx, left = 0.0D;
			for (int i = 0; i < n; i++) {
				double miss = speeds.get(i) - top - b * decay[i];
				left += miss * miss;
			}
			if (left < best) {
				best = left;
				bestKept = kept;
				bestTop = top;
			}
		}
		assertTrue(times[times.length - 1] - times[0] >= times.length - 1, "samples not a tick apart at least: " + java.util.Arrays.toString(times));
		return new double[]{bestKept, bestTop};
	}

	private static double speed(EntityHorse horse) {
		return Math.sqrt(horse.motionX * horse.motionX + horse.motionZ * horse.motionZ);
	}

	private void deepPool() {
		walledPool(8, 16, 8, 30, 6, Blocks.water, false);
	}

	private void tunnel() {
		walledPool(20, 22, 8, 30, 2, Blocks.water, true);
	}

	private void shallowPools() {
		walledPool(26, 30, 8, 12, 2, Blocks.water, false);
		walledPool(26, 30, 20, 24, 2, Blocks.lava, false);
	}

	/** Stone walls one higher than the liquid (and a glass roof, if asked) round the liquid, on the ground. */
	private void walledPool(int x1, int x2, int z1, int z2, int depth, Block liquid, boolean roof) {
		onServer(() -> {
			fill(x1 - 1, Y, z1 - 1, x2 + 1, Y + depth, z2 + 1, Blocks.stone, 0);
			fill(x1, Y, z1, x2, Y + depth - 1, z2, liquid, 0);
			fill(x1, Y + depth, z1, x2, Y + depth, z2, roof ? Blocks.glass : Blocks.air, 0);
		});
	}

	private static void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block block, int meta) {
		for (int x = x1; x <= x2; x++) {
			for (int y = y1; y <= y2; y++) {
				for (int z = z1; z <= z2; z++) {
					world().setBlock(x, y, z, block, meta, 2);
				}
			}
		}
	}
}
