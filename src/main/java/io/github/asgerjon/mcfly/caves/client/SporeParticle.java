package io.github.asgerjon.mcfly.caves.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.particle.McFlyParticle;
import net.minecraft.world.World;

/**
 * The spore blossom's spores (SPEC-modern-movement.md §90.8), on 1.7.10's falling drip cell (112, 26.3's `drip_fall`),
 * green (0.32, 0.5, 0.22):
 * - falling (26.3's `SporeBlossomFallProvider`): a drip falling at 0.005, 64 / (0.1 to 0.9) ticks, gone on the ground;
 * - in the air (26.3's `SporeBlossomAirProvider`, a `SuspendedParticle`): 26.3's random particle push (up 0.1 more),
 *   sinking at 0.01 x 0.04, no slowing, passing through blocks, 500 to 1000 ticks, an eighth lower than asked.
 */
@SideOnly(Side.CLIENT)
public final class SporeParticle extends McFlyParticle {

	private static final String SHEET = "minecraft:textures/particle/particles.png";
	private final boolean falling;

	private SporeParticle(World world, double x, double y, double z, boolean falling, int maxAge, float scale) {
		super(world, x, y, z, 0.0D, 0.0D, 0.0D, maxAge, scale, 0xFF527F38, SHEET, 1);
		this.falling = falling;
		particleRed = 0.32F;
		particleGreen = 0.5F;
		particleBlue = 0.22F;
		setSize(0.01F, 0.01F);
		setParticleTextureIndex(112);
	}

	public static SporeParticle falling(World world, double x, double y, double z, double xd, double yd, double zd) {
		SporeParticle particle = new SporeParticle(world, x, y, z, true, (int) (64.0F / (0.1F + particleRand.nextFloat() * 0.8F)),
			(particleRand.nextFloat() * 0.5F + 0.5F) * 2.0F);
		particle.motionX = particle.motionY = particle.motionZ = 0.0D;
		particle.noClip = false;
		return particle;
	}

	public static SporeParticle air(World world, double x, double y, double z, double xd, double yd, double zd) {
		float scale = (particleRand.nextFloat() * 0.5F + 0.5F) * 2.0F * (particleRand.nextFloat() * 0.6F + 0.6F);
		SporeParticle particle = new SporeParticle(world, x, y - 0.125D, z, false, 500 + particleRand.nextInt(501), scale);
		// 26.3's `Particle(level, x, y, z, xa, ya, za)` with (0, -0.8, 0)
		double mx = (Math.random() * 2.0D - 1.0D) * 0.4D, my = -0.8D + (Math.random() * 2.0D - 1.0D) * 0.4D, mz = (Math.random() * 2.0D - 1.0D) * 0.4D;
		double speed = (Math.random() + Math.random() + 1.0D) * 0.15D;
		double length = Math.sqrt(mx * mx + my * my + mz * mz);
		particle.motionX = mx / length * speed * 0.4D;
		particle.motionY = my / length * speed * 0.4D + 0.1D;
		particle.motionZ = mz / length * speed * 0.4D;
		particle.noClip = true;
		return particle;
	}

	@Override
	public void onUpdate() {
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;
		if (particleAge++ >= particleMaxAge) {
			setDead();
			return;
		}
		if (falling) {
			motionY -= 0.005D;
			moveEntity(motionX, motionY, motionZ);
			if (onGround) {
				setDead();
				return;
			}
			motionX *= 0.98D;
			motionY *= 0.98D;
			motionZ *= 0.98D;
		} else {
			motionY -= 0.04D * 0.01D;
			moveEntity(motionX, motionY, motionZ);
		}
	}
}
