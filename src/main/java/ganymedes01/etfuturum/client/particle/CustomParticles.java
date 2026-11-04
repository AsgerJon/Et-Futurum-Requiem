package ganymedes01.etfuturum.client.particle;

import ganymedes01.etfuturum.core.utils.RandomXoshiro256StarStar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.Random;

public class CustomParticles {

	protected static Random rand = new RandomXoshiro256StarStar();

	public static EntityFX spawnInvisibleBlockParticle(World world, double x, double y, double z) {
		EntityFX particle = new InnerBlockParticleFX(world, x, y, z);
		return spawnParticle(world, particle);
	}

	public static EntityFX spawnCopperWaxOnParticle(World world, double x, double y, double z) {
		EntityFX particle = new CopperGlowFX(world, x, y, z, 0xFFFFA32C);
		return spawnParticle(world, particle);
	}

	public static EntityFX spawnCopperWaxOffParticle(World world, double x, double y, double z) {
		EntityFX particle = new CopperGlowFX(world, x, y, z, 0xFFFFFFFF);
		return spawnParticle(world, particle);

	}

	public static EntityFX spawnCopperScrapeParticle(World world, double x, double y, double z) {
		EntityFX particle = new CopperGlowFX(world, x, y, z, 0xFF78DAC1);
		return spawnParticle(world, particle);
	}

	/**
	 * 26.3's electric sparks (level event 3002): along a block on an axis (0 x, 1 y, 2 z, as a struck lightning rod
	 * gives 10 to 19 of them), or, for axis -1, 3 to 5 on each of its faces (copper lightning has cleaned).
	 */
	public static void spawnElectricSparks(World world, int x, int y, int z, int axis) {
		if (axis >= 0 && axis <= 2) {
			spawnElectricSparksAlongAxis(world, x, y, z, axis, 10, 19);
			return;
		}
		for (int side = 0; side < 6; side++) {
			int[] step = {0, 0, 0};
			step[side / 2 == 0 ? 1 : side / 2 == 1 ? 2 : 0] = side % 2 == 0 ? -1 : 1;
			int count = 3 + rand.nextInt(3);
			for (int i = 0; i < count; i++) {
				double[] at = new double[3];
				double[] speed = new double[3];
				for (int a = 0; a < 3; a++) {
					at[a] = 0.5D + (step[a] == 0 ? rand.nextDouble() - 0.5D : step[a] * 0.55D);
					speed[a] = step[a] == 0 ? rand.nextDouble() - 0.5D : 0.0D;
				}
				spawnParticle(world, new ElectricSparkFX(world, x + at[0], y + at[1], z + at[2], speed[0], speed[1], speed[2]));
			}
		}
	}

	/** min to max sparks along a block on an axis, each pushed along it; within an eighth of a block of the middle across it. */
	public static void spawnElectricSparksAlongAxis(World world, int x, int y, int z, int axis, int min, int max) {
		int count = min + rand.nextInt(max - min + 1);
		for (int i = 0; i < count; i++) {
			double[] at = new double[3];
			double[] speed = new double[3];
			for (int a = 0; a < 3; a++) {
				at[a] = 0.5D + (rand.nextDouble() * 2.0D - 1.0D) * (a == axis ? 0.5D : 0.125D);
				speed[a] = a == axis ? rand.nextDouble() * 2.0D - 1.0D : 0.0D;
			}
			spawnParticle(world, new ElectricSparkFX(world, x + at[0], y + at[1], z + at[2], speed[0], speed[1], speed[2]));
		}
	}

	public static EntityFX spawnBlackHeartParticle(World world, double x, double y, double z) {
		EntityFX particle = new BlackHeartFX(world, x, y, z);
		return spawnParticle(world, particle);
	}

	/** A sword's sweep (spec §46.11), shown whatever the particle setting, as modern's sweep is. */
	public static EntityFX spawnSweepAttack(World world, double x, double y, double z) {
		return spawnParticle(world, new SweepAttackFX(world, x, y, z));
	}

	/** The glow squid's glow, a speck off its body (spec §67.1). */
	public static EntityFX spawnGlowSquidGlow(World world, double x, double y, double z) {
		return spawnParticle(world, new GlowFX(world, x, y, z));
	}

	/** A puff of the glow squid's ink (spec §67.1). */
	public static EntityFX spawnGlowSquidInk(World world, double x, double y, double z, double motionX, double motionY, double motionZ) {
		return spawnParticle(world, new GlowSquidInkFX(world, x, y, z, motionX, motionY, motionZ));
	}

	/** A used totem's particles around the entity, for 30 ticks (spec §55.2). */
	public static EntityFX spawnTotemParticles(World world, Entity entity) {
		return spawnParticle(world, new TotemEmitterFX(world, entity));
	}

	/** 26.3's snowflake (spec §62.5), as the server sends it for an entity moving in powder snow. */
	public static EntityFX spawnSnowflake(World world, double x, double y, double z, double motionX, double motionY, double motionZ) {
		return spawnParticle(world, new SnowflakeFX(world, x, y, z, motionX, motionY, motionZ));
	}

	public static EntityFX spawnEndRodParticle(World world, double x, double y, double z) {
		EntityFX particle = new EndRodFX(world, x, y, z);
		return spawnParticle(world, particle);
	}

	public static EntityFX spawnFallingDustParticle(World world, double x, double y, double z, int color) {
		EntityFX particle = new FallingDustFX(world, x, y, z, color);
		return spawnParticle(world, particle);
	}

	public static EntityFX spawnDrippingParticle(World world, double x, double y, double z, int color) {
		return spawnDrippingParticleWithSound(world, x, y, z, null, color, false);
	}

	public static EntityFX spawnDrippingParticle(World world, double x, double y, double z, int color, boolean splashes) {
		return spawnDrippingParticleWithSound(world, x, y, z, null, color, splashes);
	}

	public static EntityFX spawnDrippingParticleWithSound(World world, double x, double y, double z, String sound, int color) {
		return spawnDrippingParticleWithSound(world, x, y, z, sound, color, false);
	}

	public static EntityFX spawnDrippingParticleWithSound(World world, double x, double y, double z, String sound, int color, boolean splashes) {
		EntityFX particle = new CustomDripFX(world, x, y, z, sound, color, splashes);
		return spawnParticle(world, particle);
	}

	public static EntityFX spawnBeeNectarParticle(World world, double x, double y, double z) {
		EntityFX particle = new BeeNectarFX(world, x, y, z);
		return spawnParticle(world, particle);
	}

	public static EntityFX spawnCrimsonSpore(World world, double x, double y, double z) {
		double d0 = rand.nextFloat() * 0.5D * rand.nextFloat() * 0.1D * 5.0D;
		EntityFX particle = new BiomeFX(world, x, y, z, rand.nextGaussian() * 0.05D, d0, rand.nextGaussian() * 0.05D, 0.02F);
		particle.setRBGColorF(0.9F, 0.4F, 0.5F);
		return spawnParticle(world, particle);
	}

	public static EntityFX spawnWarpedSpore(World world, double x, double y, double z) {
		double d0 = rand.nextFloat() * 0.5D * rand.nextFloat() * 0.1D * 5.0D;
		EntityFX particle = new BiomeFX(world, x, y, z, rand.nextGaussian() * 0.05D, d0, rand.nextGaussian() * 0.05D, 0.001F);
		particle.setRBGColorF(0.1F, 0.1F, 0.3F);
		return spawnParticle(world, particle);
	}

	public static EntityFX spawnAshParticle(World world, double x, double y, double z) {
		double d0 = rand.nextFloat() * -1.9D * rand.nextFloat() * 0.1D;
		double d1 = rand.nextFloat() * -0.5D * rand.nextFloat() * 0.1D * 5.0D;
		double d2 = rand.nextFloat() * -1.9D * rand.nextFloat() * 0.1D;
		EntityFX particle = new BiomeFX(world, x, y, z, d0, d1, d2, (float) MathHelper.getRandomDoubleInRange(rand, 0.001D, 0.02D));
		particle.setRBGColorF(0.7294118F, 0.69411767F, 0.7607843F);
		return spawnParticle(world, particle);
	}

	public static EntityFX spawnCherryLeaf(World world, double x, double y, double z) {
		EntityFX particle = new CherryLeafFX(world, x, y, z);
		return spawnParticle(world, particle);
	}

	public static EntityFX spawnSoulFlame(World world, double x, double y, double z, double mX, double mY, double mZ) {
		EntityFX particle = new SoulFlameFX(world, x, y, z, mX, mY, mZ);
		return spawnParticle(world, particle);
	}

	public static EntityFX spawnSoulFlame(World world, double x, double y, double z) {
		EntityFX particle = new SoulFlameFX(world, x, y, z);
		return spawnParticle(world, particle);
	}

	public static EntityFX spawnBubbleColumnUp(World world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
		EntityFX particle = new BubbleColumnFX(world, x, y, z, xSpeed, ySpeed, zSpeed);
		return spawnParticle(world, particle);
	}

	public static EntityFX spawnWaterCurrentDown(World world, double x, double y, double z) {
		EntityFX particle = new WaterCurrentDownFX(world, x, y, z);
		return spawnParticle(world, particle);
	}

	protected static EntityFX spawnParticle(World world, EntityFX entityFX) {
		if (world.isRemote) {
			Minecraft.getMinecraft().effectRenderer.addEffect(entityFX);
			return entityFX;
		}
		return null;
	}
}
