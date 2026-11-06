package io.github.asgerjon.mcfly.sculk.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.particle.McFlyParticle;
import net.minecraft.world.World;

/**
 * 26.3's sculk particles (SPEC-modern-movement.md §90.9, step H3c), their frames by age (26.3's `setSpriteFromAge`),
 * slowed 0.96 a tick, through blocks, lit as by block light 15:
 * - `sculk_charge` (`SculkChargeParticle`): 7 frames, 1.5 times the size, 8 to 19 ticks, turned by its roll;
 * - `sculk_charge_pop` (`SculkChargePopParticle`): 4 frames, 6 to 9 ticks;
 * - `sculk_soul` (`EmissiveRisingParticle`, emissive): 11 frames, 1.5 times the size, rising: the given speed plus a
 *   hundredth of 1.7.10's push, nudged up to 0.05 about, 8 / (0.2 to 1.0) + 4 ticks.
 */
@SideOnly(Side.CLIENT)
public final class SculkParticle extends McFlyParticle {

	private SculkParticle(World world, double x, double y, double z, double mx, double my, double mz, int maxAge, float scale, String texture, int frames) {
		super(world, x, y, z, mx, my, mz, maxAge, scale, 0xFFFFFFFF, texture, frames);
	}

	/** 26.3's `SingleQuadParticle` size: 0.1 x (0.5 to 1) x 2, times its scale. */
	private static float size(float scale) {
		return (particleRand.nextFloat() * 0.5F + 0.5F) * 2.0F * scale;
	}

	public static SculkParticle charge(World world, double x, double y, double z, double xd, double yd, double zd, float roll) {
		SculkParticle particle = new SculkParticle(world, x, y, z, 0.0D, 0.0D, 0.0D, particleRand.nextInt(12) + 8, size(1.5F),
			"minecraft:textures/particle/sculk_charge.png", 7);
		particle.setSpeed(xd, yd, zd);
		particle.roll = roll;
		particle.prevRoll = roll;
		return particle;
	}

	public static SculkParticle pop(World world, double x, double y, double z, double xd, double yd, double zd) {
		SculkParticle particle = new SculkParticle(world, x, y, z, 0.0D, 0.0D, 0.0D, particleRand.nextInt(4) + 6, size(1.0F),
			"minecraft:textures/particle/sculk_charge_pop.png", 4);
		particle.setSpeed(xd, yd, zd);
		return particle;
	}

	public static SculkParticle soul(World world, double x, double y, double z, double xd, double yd, double zd) {
		SculkParticle particle = new SculkParticle(world, x, y, z, xd, yd, zd, (int) (8.0D / (particleRand.nextFloat() * 0.8D + 0.2D)) + 4, size(1.5F),
			"minecraft:textures/particle/sculk_soul.png", 11);
		particle.setPosition(x + (particleRand.nextFloat() - particleRand.nextFloat()) * 0.05F, y + (particleRand.nextFloat() - particleRand.nextFloat()) * 0.05F,
			z + (particleRand.nextFloat() - particleRand.nextFloat()) * 0.05F);
		particle.prevPosX = particle.posX;
		particle.prevPosY = particle.posY;
		particle.prevPosZ = particle.posZ;
		return particle;
	}

	/** 26.3's `setParticleSpeed`: the speed given, 1.7.10's push dropped. */
	private void setSpeed(double xd, double yd, double zd) {
		motionX = xd;
		motionY = yd;
		motionZ = zd;
	}

	/** 26.3's `Particle.tick`: no gravity, slowed by its friction (0.96), no collision; the frame by age. */
	@Override
	public void onUpdate() {
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;
		if (particleAge++ >= particleMaxAge) {
			setDead();
			return;
		}
		posX += motionX;
		posY += motionY;
		posZ += motionZ;
		motionX *= 0.96D;
		motionY *= 0.96D;
		motionZ *= 0.96D;
		lightLevel = worldObj.getLightBrightness((int) posX, (int) posY, (int) posZ);
		currentTexture = particleAge * (textures - 1) / Math.max(1, particleMaxAge);
	}

	/** 26.3's `LightCoordsUtil.withBlock(..., 15)`: the sky's light kept, block light 15. */
	@Override
	public int getBrightnessForRender(float partialTicks) {
		return super.getBrightnessForRender(partialTicks) & 0xFFFF0000 | 15 << 4;
	}
}
