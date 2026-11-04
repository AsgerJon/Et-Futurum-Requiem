package ganymedes01.etfuturum.unittas.tests;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigEntities;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.ducks.IAggressive;
import ganymedes01.etfuturum.entities.EntityStray;
import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelSkeleton;
import net.minecraft.client.model.ModelZombie;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * Skeletons' arms as 26.3's (SPEC-modern-movement.md §64.1): down when idle (holding a bow too: 26.3's mobs have no
 * item pose), aimed with
 * the head while aggressive with a bow, raised like 1.7.10's zombie only while aggressive without one; and when a
 * skeleton turns aggressive. Angles at age 0, no walking, no swing, the head straight, so the bob is z 0.1 (mirrored).
 */
public class SkeletonArmsTests extends TasTestCase {

	private static final int GROUND = 4;
	private static final double HALF_PI = Math.PI / 2.0D;
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
			for (EntitySkeleton skeleton : entitiesNear(EntitySkeleton.class, 300.0D)) {
				skeleton.setDead();
			}
			player().setGameType(WorldSettings.GameType.CREATIVE);
			player().setHealth(player().getMaxHealth());
		});
	}

	// ---------------------------------------------------------------- the model

	/** An idle skeleton with a bow: both arms down (26.3's mobs hold items without the player's item pose); no zombie arms. */
	public void testIdleSkeletonArms() {
		float[] arms = onClient(() -> arms(new ModelSkeleton(), skeleton(0, Items.bow, false)));
		assertArms(arms, new double[]{0.0D, 0.0D, 0.1D, 0.0D, 0.0D, -0.1D}, "an idle skeleton with a bow");
	}

	/** Aggressive with a bow: both arms aimed (26.3's BOW_AND_ARROW, 1.7.10's aimedBow). */
	public void testAimingSkeletonArms() {
		float[] arms = onClient(() -> arms(new ModelSkeleton(), skeleton(0, Items.bow, true)));
		assertArms(arms, new double[]{-HALF_PI, -0.1D, 0.1D, -HALF_PI, 0.5D, -0.1D}, "a skeleton aiming its bow");
	}

	/** Aggressive with a sword (a wither skeleton chasing): the raised arms, as 1.7.10's zombie. */
	public void testChasingWitherSkeletonArms() {
		float[] arms = onClient(() -> arms(new ModelSkeleton(), skeleton(1, Items.stone_sword, true)));
		assertArms(arms, new double[]{-HALF_PI, -0.1D, 0.1D, -HALF_PI, 0.1D, -0.1D}, "a wither skeleton chasing");
		float[] idle = onClient(() -> arms(new ModelSkeleton(), skeleton(1, Items.stone_sword, false)));
		assertArms(idle, new double[]{0.0D, 0.0D, 0.1D, 0.0D, 0.0D, -0.1D}, "an idle wither skeleton");
	}

	/** The stray's clothes (a zombie model) follow its body, and its renderer gives them the body's held item. */
	public void testStrayClothes() throws Exception {
		if (!ConfigEntities.enableStray) {
			skipTest("strays are off");
		}
		float[] clothes = onClient(() -> {
			EntityStray stray = clientEntity(EntityStray::new);
			stray.setCurrentItemOrArmor(0, new ItemStack(Items.bow));
			return arms(new ModelZombie(0.5F, true), stray);
		});
		assertArms(clothes, new double[]{0.0D, 0.0D, 0.1D, 0.0D, 0.0D, -0.1D}, "the stray's clothes");
		int held = onClient(() -> {
			EntityStray stray = clientEntity(EntityStray::new);
			stray.setCurrentItemOrArmor(0, new ItemStack(Items.bow));
			Render renderer = RenderManager.instance.getEntityClassRenderObject(EntityStray.class);
			Field main = field(renderer.getClass(), "modelBipedMain");
			((ModelBiped) main.get(renderer)).heldItemRight = 1;
			Method pass = method(renderer.getClass(), "shouldRenderPass", EntityLivingBase.class, int.class, float.class);
			pass.invoke(renderer, stray, 1, 0.0F);
			return ((ModelBiped) field(renderer.getClass(), "clothesModel").get(renderer)).heldItemRight;
		});
		assertEqual(1, held, "the clothes' held item flag");
	}

	/** With the option off, 1.7.10's raised arms. */
	public void testOptionOff() {
		McFlyConfig.modernMobArms = false;
		try {
			float[] arms = onClient(() -> arms(new ModelSkeleton(), skeleton(0, Items.bow, false)));
			assertArms(arms, new double[]{-HALF_PI, -0.1D, 0.1D, -HALF_PI, 0.1D, -0.1D}, "the option off");
		} finally {
			McFlyConfig.modernMobArms = true;
		}
	}

	// ---------------------------------------------------------------- aggressive

	/** A skeleton with a bow turns aggressive with a survival player near, not with a creative one. */
	public void testSkeletonWithATargetIsAggressive() {
		int[] at = {180, 300};
		bringPlayer(at);
		EntitySkeleton skeleton = spawn(EntitySkeleton::new, at[0] + 0.5D, GROUND, at[1] + 6.5D);
		onServer(() -> {
			skeleton.setCurrentItemOrArmor(0, new ItemStack(Items.bow));
			skeleton.setCombatTask();
		});
		serverTicks(40);
		assertFalse(onServer(() -> aggressive(skeleton)), "aggressive with only a creative player near");
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
		waitUntil("the skeleton aggressive", () -> {
			player().setHealth(player().getMaxHealth());
			return aggressive(skeleton);
		}, 100);
		waitUntilOnClient("the skeleton aggressive on the client", () -> aggressive(mc().theWorld.getEntityByID(skeleton.getEntityId())), 20);
		onServer(() -> player().setGameType(WorldSettings.GameType.CREATIVE));
		waitUntil("the skeleton calm again with its target gone", () -> !aggressive(skeleton), 200);
	}

	/** A wither skeleton (melee) chasing a survival player turns aggressive. */
	public void testWitherSkeletonChasingIsAggressive() {
		int[] at = {220, 300};
		bringPlayer(at);
		onServer(() -> player().setGameType(WorldSettings.GameType.SURVIVAL));
		EntitySkeleton wither = spawn(world -> {
			EntitySkeleton made = new EntitySkeleton(world);
			made.setSkeletonType(1);
			made.setCurrentItemOrArmor(0, new ItemStack(Items.stone_sword));
			made.setCombatTask();
			return made;
		}, at[0] + 0.5D, GROUND, at[1] + 5.5D);
		waitUntil("the wither skeleton aggressive", () -> {
			player().setHealth(player().getMaxHealth());
			return aggressive(wither);
		}, 100);
	}

	// ---------------------------------------------------------------- helpers

	private static <E extends Entity> E clientEntity(Function<World, E> make) {
		E entity = make.apply(mc().theWorld);
		entity.setLocationAndAngles(mc().thePlayer.posX, mc().thePlayer.posY, mc().thePlayer.posZ, 0.0F, 0.0F);
		return entity;
	}

	/** A client-side skeleton of the type, holding the item, aggressive or not (its own synced flag). */
	private static EntitySkeleton skeleton(int type, net.minecraft.item.Item item, boolean aggressive) {
		EntitySkeleton skeleton = clientEntity(EntitySkeleton::new);
		skeleton.setSkeletonType(type);
		skeleton.setCurrentItemOrArmor(0, new ItemStack(item));
		if (aggressive) {
			if (!(skeleton instanceof IAggressive)) {
				fail("skeletons have no aggressive flag");
			}
			((IAggressive) skeleton).etfu$setAggressive(true);
		}
		return skeleton;
	}

	/** The arms a model gives the entity standing still at age 0, no swing, head straight, holding what it holds (as RenderBiped sets it). */
	private static float[] arms(ModelBiped model, EntityLivingBase entity) {
		model.onGround = 0.0F;
		model.isChild = entity.isChild();
		model.heldItemRight = entity.getHeldItem() != null ? 1 : 0;
		model.setLivingAnimations(entity, 0.0F, 0.0F, 0.0F);
		model.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, entity);
		return new float[]{model.bipedRightArm.rotateAngleX, model.bipedRightArm.rotateAngleY, model.bipedRightArm.rotateAngleZ,
				model.bipedLeftArm.rotateAngleX, model.bipedLeftArm.rotateAngleY, model.bipedLeftArm.rotateAngleZ};
	}

	/** Right x y z, left x y z. */
	private static void assertArms(float[] arms, double[] wanted, String what) {
		StringBuilder seen = new StringBuilder(what + ": right " + arms[0] + " " + arms[1] + " " + arms[2] + ", left " + arms[3] + " " + arms[4] + " " + arms[5]
				+ "; wanted right " + (float) wanted[0] + " " + (float) wanted[1] + " " + (float) wanted[2] + ", left " + (float) wanted[3] + " "
				+ (float) wanted[4] + " " + (float) wanted[5]);
		for (int i = 0; i < 6; i++) {
			assertTrue(Math.abs(arms[i] - wanted[i]) < CLOSE, seen.toString());
		}
	}

	private static boolean aggressive(Entity entity) {
		return entity instanceof IAggressive && ((IAggressive) entity).etfu$isAggressive();
	}

	private static Field field(Class<?> type, String name) throws NoSuchFieldException {
		for (Class<?> at = type; at != null; at = at.getSuperclass()) {
			try {
				Field found = at.getDeclaredField(name);
				found.setAccessible(true);
				return found;
			} catch (NoSuchFieldException ignored) {
			}
		}
		throw new NoSuchFieldException(name);
	}

	private static Method method(Class<?> type, String name, Class<?>... args) throws NoSuchMethodException {
		for (Class<?> at = type; at != null; at = at.getSuperclass()) {
			try {
				Method found = at.getDeclaredMethod(name, args);
				found.setAccessible(true);
				return found;
			} catch (NoSuchMethodException ignored) {
			}
		}
		throw new NoSuchMethodException(name);
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
			// night: no skeleton burns
			world().setWorldTime(18000);
		});
		waitUntilOnClient("arriving", () -> Math.abs(mc().thePlayer.posX - x) < 0.01D && Math.abs(mc().thePlayer.posZ - z) < 0.01D
				&& mc().theWorld.getChunkFromBlockCoords((int) Math.floor(x), (int) Math.floor(z)).isChunkLoaded, 60);
		ticks(2);
	}
}
