package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigEntities;
import ganymedes01.etfuturum.entities.EntityPhantom;
import ganymedes01.etfuturum.entities.PhantomSpawning;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.init.Blocks;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

/**
 * Phantoms spawn over the sleepless (SPEC-modern-movement.md §68.3; 26.3's `PhantomSpawner`, `DifficultyInstance`,
 * `time_since_rest`). The spawner's 60 to 119 s wait is skipped (`PhantomSpawning.tryOnce`: one try for every player);
 * the player at the spawn of the flat world (its sea level 3), under the open sky.
 */
public class PhantomSpawnTests extends TasTestCase {

	@Override
	protected int timeoutSeconds() {
		return 90;
	}

	@Override
	protected void setUp() {
		if (!McFlyConfig.enablePhantoms || !McFlyConfig.enablePhantomSpawns) {
			skipTest("phantoms or their spawning are off");
		}
		difficulty(EnumDifficulty.NORMAL);
	}

	@Override
	protected void tearDown() {
		onServer(() -> {
			for (EntityPhantom phantom : entitiesNear(EntityPhantom.class, 200.0D)) {
				phantom.setDead();
			}
			world().setBlockToAir(0, 6, 0);
			world().getGameRules().setOrCreateGameRule("doInsomnia", "true");
			world().setWorldTime(6000L);
			PhantomSpawning.setTimeSinceRest(player(), 0);
		});
	}

	/** The statistic counts a tick at a time while the player is awake. */
	public void testRestCountsAwake() {
		long[] before = onServer(() -> new long[]{PhantomSpawning.timeSinceRest(player()), world().getTotalWorldTime()});
		serverTicks(20);
		long[] after = onServer(() -> new long[]{PhantomSpawning.timeSinceRest(player()), world().getTotalWorldTime()});
		assertEqual(after[1] - before[1], after[0] - before[0], "ticks awake counted");
	}

	/** Dying puts it back to 0 (26.3's `die`). */
	public void testRestResetsOnDeath() {
		onServer(() -> PhantomSpawning.setTimeSinceRest(player(), 5000));
		assertTrue(onServer(() -> PhantomSpawning.timeSinceRest(player())) >= 5000, "the time since rest wasn't set");
		onServer(() -> {
			MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(player(), DamageSource.outOfWorld));
		});
		assertTrue(onServer(() -> PhantomSpawning.timeSinceRest(player())) < 5, "the time since rest after dying");
	}

	/** Sleeping puts it back to 0 (26.3's `startSleeping`) and it stays there while asleep. */
	public void testRestResetsWhenSleeping() {
		onServer(() -> {
			world().setWorldTime(18000L);
			world().setBlock(2, 4, 0, Blocks.bed, 3, 3);
			world().setBlock(3, 4, 0, Blocks.bed, 3 | 8, 3);
			PhantomSpawning.setTimeSinceRest(player(), 5000);
		});
		assertTrue(onServer(() -> PhantomSpawning.timeSinceRest(player())) >= 5000, "the time since rest wasn't set");
		String slept = onServer(() -> String.valueOf(player().sleepInBedAt(3, 4, 0)));
		assertEqual("OK", slept, "the player's try to sleep");
		serverTicks(10);
		assertEqual(0, onServer(() -> PhantomSpawning.timeSinceRest(player())), "the time since rest asleep");
		onServer(() -> {
			player().wakeUpPlayer(true, true, false);
			world().setBlockToAir(2, 4, 0);
			world().setBlockToAir(3, 4, 0);
		});
	}

	/** 26.3's local difficulty: the base x (0.75 + the world's age + the chunk's time + the moon), Easy's local part halved. */
	public void testLocalDifficulty() {
		assertAlmostEqual(0.75D, PhantomSpawning.effectiveDifficulty(EnumDifficulty.EASY, 0L, 0L, 1.0F), 1.0E-5D);
		assertAlmostEqual(6.75D, PhantomSpawning.effectiveDifficulty(EnumDifficulty.HARD, 1512000L, 3600000L, 1.0F), 1.0E-5D);
		assertAlmostEqual(2.75D, PhantomSpawning.effectiveDifficulty(EnumDifficulty.NORMAL, 792000L, 1800000L, 0.5F), 1.0E-5D);
		assertAlmostEqual(0.0D, PhantomSpawning.effectiveDifficulty(EnumDifficulty.PEACEFUL, 1512000L, 3600000L, 1.0F), 1.0E-5D);
	}

	/** At night under the sky, 3 days awake: phantoms 20 to 34 above the player, within 10 to each side, size 0, anchored 5 above. */
	public void testSpawnsOverTheSleepless() {
		night(10000000);
		double[] at = null;
		for (int i = 0; i < 60 && at == null; i++) {
			// read in the same server step as the try, before the phantom has flown off where it came
			at = onServer(() -> {
				if (PhantomSpawning.tryOnce(world()) == 0) {
					return null;
				}
				EntityPhantom phantom = entitiesNear(EntityPhantom.class, 200.0D).get(0);
				int[] anchor = phantom.getAnchor();
				return new double[]{phantom.posX - player().posX, phantom.posY - player().posY, phantom.posZ - player().posZ,
					phantom.getPhantomSize(), anchor == null ? -1 : anchor[1] - MathHelper.floor_double(phantom.posY)};
			});
		}
		assertTrue(at != null, "no phantom in 60 tries");
		assertTrue(Math.abs(at[0]) <= 11.0D && Math.abs(at[2]) <= 11.0D && at[1] >= 19.0D && at[1] <= 36.0D,
			"a phantom at " + at[0] + ", " + at[1] + ", " + at[2] + " from the player");
		assertEqual(0, (int) at[3], "the new phantom's size");
		assertEqual(5, (int) at[4], "its anchor above where it came (-1: none)");
	}

	/** None by day, under a roof, after a rest, or with `doInsomnia` off. */
	public void testNoneWhenItShouldnt() {
		night(10000000);
		onServer(() -> world().setWorldTime(6000L));
		serverTicks(2);
		assertEqual(0, tries(40), "phantoms by day");
		night(10000000);
		onServer(() -> world().setBlock(0, 6, 0, Blocks.stone));
		assertEqual(0, tries(40), "phantoms under a roof");
		onServer(() -> world().setBlockToAir(0, 6, 0));
		night(0);
		assertEqual(0, tries(40), "phantoms after a rest");
		night(10000000);
		onServer(() -> world().getGameRules().setOrCreateGameRule("doInsomnia", "false"));
		assertEqual(0, tries(40), "phantoms with doInsomnia off");
	}

	// ---------------------------------------------------------------- helpers

	private void night(int awake) {
		onServer(() -> {
			world().setWorldTime(18000L);
			PhantomSpawning.setTimeSinceRest(player(), awake);
		});
		serverTicks(2);
	}

	/** This many tries; the phantoms there after. */
	private int tries(int times) {
		for (int i = 0; i < times; i++) {
			onServer(() -> PhantomSpawning.tryOnce(world()));
		}
		return onServer(() -> entitiesNear(EntityPhantom.class, 200.0D).size());
	}
}
