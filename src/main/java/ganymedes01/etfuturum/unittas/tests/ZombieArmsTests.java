package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.client.model.ModelVillagerZombie;
import ganymedes01.etfuturum.configuration.configs.ConfigEntities;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.ducks.IAggressive;
import ganymedes01.etfuturum.entities.EntityHusk;
import ganymedes01.etfuturum.entities.EntityStray;
import ganymedes01.etfuturum.entities.EntityZombieVillager;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelSkeleton;
import net.minecraft.client.model.ModelZombie;
import net.minecraft.client.model.ModelZombieVillager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityGiantZombie;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

import java.util.function.Function;

/**
 * Zombies' arms as 26.3's (SPEC-modern-movement.md §63.9): the model's angles (raised to -80 degrees, -120 when about
 * to attack; the swing lowering them; a baby holding something arms down; skeletons left as 1.7.10's), and when a
 * zombie turns aggressive (26.3's {@code ZombieAttackGoal}). Angles at age 0 and no walking, so the idle bob is only
 * z 0.1 (mirrored on the left arm).
 */
public class ZombieArmsTests extends TasTestCase {

	private static final int GROUND = 4;
	private static final double RAISED = -Math.PI / 2.25D, AGGRESSIVE = -Math.PI / 1.5D, OLD = -Math.PI / 2.0D;
	private static final double CLOSE = 1.0E-4D;

	@Override
	protected int timeoutSeconds() {
		return 120;
	}

	@Override
	protected void tearDown() {
		McFlyConfig.modernMobArms = true;
		onServer(() -> {
			world().setWorldTime(6000);
			for (Class<? extends Entity> type : new Class[]{EntityZombie.class, EntitySkeleton.class, EntityGiantZombie.class}) {
				for (Entity entity : entitiesNear(type, 300.0D)) {
					entity.setDead();
				}
			}
			player().setGameType(WorldSettings.GameType.CREATIVE);
			player().setHealth(player().getMaxHealth());
		});
	}

	// ---------------------------------------------------------------- the model

	/** An idle zombie: both arms at -80 degrees, turned 0.1 inwards, bobbed 0.1 outwards. */
	public void testIdleZombieArms() {
		float[] arms = onClient(() -> arms(new ModelZombie(), clientEntity(EntityZombie::new), 0.0F));
		assertArms(arms, RAISED, -0.1D, 0.1D, "an idle zombie");
	}

	/** The attack swing lowers the arms in 26.3 (1.7.10 raised them): at swing 0.5, x = -80 degrees + 1.2 - 0.4 sin(0.75 pi). */
	public void testSwingLowersArms() {
		float[] arms = onClient(() -> arms(new ModelZombie(), clientEntity(EntityZombie::new), 0.5F));
		double x = RAISED + 1.2D - 0.4D * Math.sin(0.75D * Math.PI);
		assertArms(arms, x, 0.5D, 0.1D, "a zombie half through its swing");
	}

	/** A baby holding something holds its arms down; a baby with empty hands raises them. */
	public void testBabies() {
		float[] holding = onClient(() -> {
			EntityZombie baby = clientEntity(EntityZombie::new);
			baby.setChild(true);
			baby.setCurrentItemOrArmor(0, new ItemStack(Items.iron_sword));
			return arms(new ModelZombie(), baby, 0.0F);
		});
		assertArms(holding, 0.0D, 0.1D, 0.1D, "a baby zombie holding a sword");
		float[] empty = onClient(() -> {
			EntityZombie baby = clientEntity(EntityZombie::new);
			baby.setChild(true);
			return arms(new ModelZombie(), baby, 0.0F);
		});
		assertArms(empty, RAISED, -0.1D, 0.1D, "a baby zombie with empty hands");
	}

	/** The zombie's kinds: 1.7.10's zombie villager model, EFR's, the husk, the pigman, the giant. */
	public void testZombieKinds() {
		assertArms(onClient(() -> {
			EntityZombie villager = clientEntity(EntityZombie::new);
			villager.setVillager(true);
			return arms(new ModelZombieVillager(), villager, 0.0F);
		}), RAISED, -0.1D, 0.1D, "1.7.10's zombie villager");
		if (ConfigEntities.enableVillagerZombies) {
			assertArms(onClient(() -> arms(new ModelVillagerZombie(0.0F), clientEntity(EntityZombieVillager::new), 0.0F)),
					RAISED, -0.1D, 0.1D, "EFR's zombie villager");
		}
		if (ConfigEntities.enableHusk) {
			assertArms(onClient(() -> arms(new ModelZombie(), clientEntity(EntityHusk::new), 0.0F)), RAISED, -0.1D, 0.1D, "a husk");
		}
		assertArms(onClient(() -> arms(new ModelZombie(), clientEntity(EntityPigZombie::new), 0.0F)), RAISED, -0.1D, 0.1D, "a pigman");
		assertArms(onClient(() -> arms(new ModelZombie(), clientEntity(EntityGiantZombie::new), 0.0F)), RAISED, -0.1D, 0.1D, "a giant");
	}

	/**
	 * Skeletons (1.7.10's model is a zombie's) and the stray's clothes (a zombie model on a skeleton) don't get the
	 * zombie's arms: an idle one without an item has the humanoid's, arms down (their own rules: SkeletonArmsTests, §64.1).
	 */
	public void testSkeletonsAreNotZombies() {
		assertArms(onClient(() -> arms(new ModelSkeleton(), clientEntity(EntitySkeleton::new), 0.0F)), 0.0D, 0.0D, 0.1D, "a skeleton");
		if (ConfigEntities.enableStray) {
			assertArms(onClient(() -> arms(new ModelZombie(0.5F, true), clientEntity(EntityStray::new), 0.0F)), 0.0D, 0.0D, 0.1D,
					"the stray's clothes");
		}
	}

	/** With the option off, 1.7.10's arms. */
	public void testOptionOff() {
		McFlyConfig.modernMobArms = false;
		try {
			assertArms(onClient(() -> arms(new ModelZombie(), clientEntity(EntityZombie::new), 0.0F)), OLD, -0.1D, 0.1D, "the option off");
		} finally {
			McFlyConfig.modernMobArms = true;
		}
	}

	// ---------------------------------------------------------------- aggressive

	/**
	 * A zombie chasing a survival player turns aggressive (on the client too, its arms then at -120 degrees); on the tick
	 * it hits it isn't (the next attack 20 ticks off), and is again within 15.
	 */
	public void testChasingZombieTurnsAggressive() {
		int[] at = {100, 300};
		bringPlayer(at);
		EntityZombie zombie = spawn(EntityZombie::new, at[0] + 0.5D, GROUND, at[1] + 4.5D);
		serverTicks(20);
		assertFalse(onServer(() -> aggressive(zombie)), "a zombie with no one to chase is aggressive");
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
		waitUntilOnClient("the chasing zombie aggressive on the client", () -> aggressive(mc().theWorld.getEntityByID(zombie.getEntityId())), 100);
		float[] arms = onClient(() -> arms(new ModelZombie(), mc().theWorld.getEntityByID(zombie.getEntityId()), 0.0F));
		assertArms(arms, AGGRESSIVE, -0.1D, 0.1D, "an aggressive zombie");
		// the hit: healed every tick, so less than full health is a hit since the last look
		boolean hitSeen = false;
		onServer(() -> player().setHealth(player().getMaxHealth()));
		for (int tick = 0; tick < 100 && !hitSeen; tick++) {
			serverTicks(1);
			boolean[] state = onServer(() -> {
				boolean hit = player().getHealth() < player().getMaxHealth();
				player().setHealth(player().getMaxHealth());
				return new boolean[]{hit, aggressive(zombie)};
			});
			if (state[0]) {
				hitSeen = true;
				assertFalse(state[1], "aggressive on the tick it hits");
			}
		}
		assertTrue(hitSeen, "the zombie never hit the player");
		boolean again = false;
		for (int tick = 0; tick < 15 && !again; tick++) {
			serverTicks(1);
			again = onServer(() -> aggressive(zombie));
		}
		assertTrue(again, "not aggressive again within 15 ticks of its hit");
	}

	/** An angry pigman (1.7.10's has no AI tasks) chasing turns aggressive. */
	public void testAngryPigmanTurnsAggressive() {
		int[] at = {140, 300};
		bringPlayer(at);
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
		EntityPigZombie pigman = spawn(EntityPigZombie::new, at[0] + 0.5D, GROUND, at[1] + 4.5D);
		serverTicks(20);
		assertFalse(onServer(() -> aggressive(pigman)), "a calm pigman is aggressive");
		onServer(() -> pigman.attackEntityFrom(DamageSource.causePlayerDamage(player()), 1.0F));
		waitUntil("the angry pigman aggressive", () -> {
			player().setHealth(player().getMaxHealth());
			return aggressive(pigman);
		}, 100);
	}

	// ---------------------------------------------------------------- helpers

	private static <E extends Entity> E clientEntity(Function<World, E> make) {
		E entity = make.apply(mc().theWorld);
		entity.setLocationAndAngles(mc().thePlayer.posX, mc().thePlayer.posY, mc().thePlayer.posZ, 0.0F, 0.0F);
		return entity;
	}

	/** The arms a model gives the entity standing still at age 0 with the swing: right x y z, left x y z. */
	private static float[] arms(ModelBiped model, Entity entity, float swing) {
		model.onGround = swing;
		model.isChild = entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isChild();
		model.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, entity);
		return new float[]{model.bipedRightArm.rotateAngleX, model.bipedRightArm.rotateAngleY, model.bipedRightArm.rotateAngleZ,
				model.bipedLeftArm.rotateAngleX, model.bipedLeftArm.rotateAngleY, model.bipedLeftArm.rotateAngleZ};
	}

	/** Both arms: x the same, y and z mirrored on the left. */
	private static void assertArms(float[] arms, double x, double rightY, double rightZ, String what) {
		String seen = what + ": right " + arms[0] + " " + arms[1] + " " + arms[2] + ", left " + arms[3] + " " + arms[4] + " " + arms[5];
		assertTrue(Math.abs(arms[0] - x) < CLOSE && Math.abs(arms[3] - x) < CLOSE, seen + " (x wanted " + (float) x + ")");
		assertTrue(Math.abs(arms[1] - rightY) < CLOSE && Math.abs(arms[4] + rightY) < CLOSE, seen + " (right y wanted " + (float) rightY + ")");
		assertTrue(Math.abs(arms[2] - rightZ) < CLOSE && Math.abs(arms[5] + rightZ) < CLOSE, seen + " (right z wanted " + (float) rightZ + ")");
	}

	private static boolean aggressive(Entity entity) {
		return entity instanceof IAggressive && ((IAggressive) entity).etfu$isAggressive();
	}

	private void bringPlayer(int[] at) {
		double x = at[0] + 0.5D, z = at[1] + 0.5D;
		onServer(() -> {
			for (int dx = -6; dx <= 6; dx++) {
				for (int dz = -6; dz <= 10; dz++) {
					for (int y = GROUND; y <= GROUND + 3; y++) {
						world().setBlockToAir(at[0] + dx, y, at[1] + dz);
					}
				}
			}
			player().playerNetServerHandler.setPlayerLocation(x, GROUND, z, 0.0F, 0.0F);
			// night: no zombie burns
			world().setWorldTime(18000);
		});
		waitUntilOnClient("arriving", () -> Math.abs(mc().thePlayer.posX - x) < 0.01D && Math.abs(mc().thePlayer.posZ - z) < 0.01D
				&& mc().theWorld.getChunkFromBlockCoords((int) Math.floor(x), (int) Math.floor(z)).isChunkLoaded, 60);
		ticks(2);
	}
}
