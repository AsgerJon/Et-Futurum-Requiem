package io.github.asgerjon.mcfly.particle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.client.particle.DragonBreathFX;
import ganymedes01.etfuturum.client.particle.ElectricSparkFX;
import ganymedes01.etfuturum.client.particle.GlowFX;
import ganymedes01.etfuturum.client.particle.GlowSquidInkFX;
import ganymedes01.etfuturum.client.particle.SnowflakeFX;
import ganymedes01.etfuturum.client.particle.SweepAttackFX;
import ganymedes01.etfuturum.client.particle.TotemEmitterFX;
import ganymedes01.etfuturum.client.particle.TotemFX;
import io.github.asgerjon.mcfly.network.ParticleMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * The client's half of our particles by name (SPEC-modern-movement.md §79, B7): each name's particle (26.3's particle
 * providers), shown under 26.3's rules (`ClientLevel.doAddParticle`), and 26.3's level particles packet as
 * `ClientPacketListener.handleParticleEvent` shows it.
 */
@SideOnly(Side.CLIENT)
public final class McFlyParticlesClient {

	/** Makes the particle of a name at a spot, with the speed it's given. */
	public interface Factory {
		EntityFX create(World world, double x, double y, double z, double xd, double yd, double zd);
	}

	public static final Random RANDOM = new Random();

	private static final Map<String, Factory> FACTORIES = new HashMap<>();

	static {
		FACTORIES.put(McFlyParticles.DRAGON_BREATH, DragonBreathFX::new);
		FACTORIES.put(McFlyParticles.SWEEP_ATTACK, (world, x, y, z, xd, yd, zd) -> new SweepAttackFX(world, x, y, z));
		FACTORIES.put(McFlyParticles.TOTEM_OF_UNDYING, TotemFX::new);
		FACTORIES.put(McFlyParticles.SNOWFLAKE, SnowflakeFX::new);
		FACTORIES.put(McFlyParticles.GLOW_SQUID_INK, GlowSquidInkFX::new);
		FACTORIES.put(McFlyParticles.GLOW, (world, x, y, z, xd, yd, zd) -> new GlowFX(world, x, y, z));
		FACTORIES.put(McFlyParticles.ELECTRIC_SPARK, ElectricSparkFX::new);
	}

	private McFlyParticlesClient() {
	}

	/** As 26.3's `level.addParticle(particle, x, y, z, xd, yd, zd)`. */
	public static EntityFX add(String name, double x, double y, double z, double xd, double yd, double zd) {
		return add(name, false, false, x, y, z, xd, yd, zd);
	}

	/**
	 * As 26.3's `ClientLevel.doAddParticle`: the type's overrideLimiter or the one asked for shows it whatever the
	 * setting and however far; otherwise not beyond 32 blocks of the camera, and not on Minimal (with alwaysShow, Minimal
	 * counts as Decreased 1 time in 10; Decreased as Minimal 1 time in 3). The particle, or null.
	 */
	public static EntityFX add(String name, boolean overrideLimiter, boolean alwaysShow, double x, double y, double z, double xd, double yd, double zd) {
		Factory factory = FACTORIES.get(name);
		if (factory == null) {
			throw new IllegalArgumentException("no particle " + name);
		}
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.theWorld == null) {
			return null;
		}
		int level = particleLevel(mc, alwaysShow);
		if (!(McFlyParticles.overridesLimiter(name) || overrideLimiter)) {
			EntityLivingBase camera = mc.renderViewEntity;
			if (camera == null || camera.getDistanceSq(x, y, z) > 1024.0D || level == 2) {
				return null;
			}
		}
		EntityFX particle = factory.create(mc.theWorld, x, y, z, xd, yd, zd);
		mc.effectRenderer.addEffect(particle);
		return particle;
	}

	/** 26.3's `calculateParticleLevel` on 1.7.10's setting: 0 All, 1 Decreased, 2 Minimal. */
	private static int particleLevel(Minecraft mc, boolean alwaysShow) {
		int level = mc.gameSettings.particleSetting;
		if (alwaysShow && level == 2 && RANDOM.nextInt(10) == 0) {
			level = 1;
		}
		if (level == 1 && RANDOM.nextInt(3) == 0) {
			level = 2;
		}
		return level;
	}

	/** 26.3's `ClientPacketListener.handleParticleEvent`. */
	public static void handle(ParticleMessage packet) {
		if (Minecraft.getMinecraft().theWorld == null || !McFlyParticles.exists(packet.name)) {
			return;
		}
		if (packet.count == 0) {
			add(packet.name, packet.overrideLimiter, packet.alwaysShow, packet.x, packet.y, packet.z,
				packet.xMaxSpeed * packet.xDist, packet.yMaxSpeed * packet.yDist, packet.zMaxSpeed * packet.zDist);
			return;
		}
		for (int i = 0; i < packet.count; i++) {
			double x, y, z, xa, ya, za;
			if (packet.randomization != McFlyParticles.DEFAULT) {
				x = packet.x + RANDOM.nextDouble() * packet.xDist;
				y = packet.y + RANDOM.nextDouble() * packet.yDist;
				z = packet.z + RANDOM.nextDouble() * packet.zDist;
				xa = packet.xMaxSpeed;
				ya = packet.yMaxSpeed;
				za = packet.zMaxSpeed;
				if (packet.randomization == McFlyParticles.ALTERNATIVE_WITH_SPEED) {
					xa *= RANDOM.nextDouble();
					ya *= RANDOM.nextDouble();
					za *= RANDOM.nextDouble();
				}
			} else {
				x = packet.x + RANDOM.nextGaussian() * packet.xDist;
				y = packet.y + RANDOM.nextGaussian() * packet.yDist;
				z = packet.z + RANDOM.nextGaussian() * packet.zDist;
				xa = RANDOM.nextGaussian() * packet.xMaxSpeed;
				ya = RANDOM.nextGaussian() * packet.yMaxSpeed;
				za = RANDOM.nextGaussian() * packet.zMaxSpeed;
			}
			add(packet.name, packet.overrideLimiter, packet.alwaysShow, x, y, z, xa, ya, za);
		}
	}

	// ---------------------------------------------------------------- the ways our code shows them

	/**
	 * 26.3's electric sparks (level event 3002): along a block on an axis (0 x, 1 y, 2 z, as a struck lightning rod
	 * gives 10 to 19 of them), or, for axis -1, 3 to 5 on each of its faces (copper lightning has cleaned).
	 */
	public static void electricSparks(int x, int y, int z, int axis) {
		if (axis >= 0 && axis <= 2) {
			electricSparksAlongAxis(x, y, z, axis, 10, 19);
			return;
		}
		for (int side = 0; side < 6; side++) {
			int[] step = {0, 0, 0};
			step[side / 2 == 0 ? 1 : side / 2 == 1 ? 2 : 0] = side % 2 == 0 ? -1 : 1;
			int count = 3 + RANDOM.nextInt(3);
			for (int i = 0; i < count; i++) {
				double[] at = new double[3];
				double[] speed = new double[3];
				for (int a = 0; a < 3; a++) {
					at[a] = 0.5D + (step[a] == 0 ? RANDOM.nextDouble() - 0.5D : step[a] * 0.55D);
					speed[a] = step[a] == 0 ? RANDOM.nextDouble() - 0.5D : 0.0D;
				}
				add(McFlyParticles.ELECTRIC_SPARK, x + at[0], y + at[1], z + at[2], speed[0], speed[1], speed[2]);
			}
		}
	}

	/** min to max sparks along a block on an axis, each pushed along it; within an eighth of a block of the middle across it. */
	public static void electricSparksAlongAxis(int x, int y, int z, int axis, int min, int max) {
		int count = min + RANDOM.nextInt(max - min + 1);
		for (int i = 0; i < count; i++) {
			double[] at = new double[3];
			double[] speed = new double[3];
			for (int a = 0; a < 3; a++) {
				at[a] = 0.5D + (RANDOM.nextDouble() * 2.0D - 1.0D) * (a == axis ? 0.5D : 0.125D);
				speed[a] = a == axis ? RANDOM.nextDouble() * 2.0D - 1.0D : 0.0D;
			}
			add(McFlyParticles.ELECTRIC_SPARK, x + at[0], y + at[1], z + at[2], speed[0], speed[1], speed[2]);
		}
	}

	/** A used totem's particles around the entity, for 30 ticks (spec §55.2; 26.3's tracking emitter). */
	public static void totemEmitter(Entity entity) {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.theWorld != null) {
			mc.effectRenderer.addEffect(new TotemEmitterFX(mc.theWorld, entity));
		}
	}
}
