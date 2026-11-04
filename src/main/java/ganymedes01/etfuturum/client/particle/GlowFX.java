package ganymedes01.etfuturum.client.particle;

import net.minecraft.world.World;

/**
 * The glow squid's glow (SPEC-modern-movement.md §67.1, 26.3's `GlowParticle.GlowSquidProvider`): a speck drifting off
 * its body, half pale green (0.6, 1.0, 0.8), half dark teal (0.08, 0.4, 0.4), slowing by 0.96 a tick, going through
 * blocks, lighting itself up to full over its 4 to 40 ticks.
 */
public class GlowFX extends EtFuturumFXParticle {

	public GlowFX(World world, double x, double y, double z) {
		super(world, x, y, z, 0.0D, 0.0D, 0.0D, (int) (4.0F / (particleRand.nextFloat() * 0.9F + 0.1F)),
			(particleRand.nextFloat() * 0.5F + 0.5F) * 2.0F * 0.75F, particleRand.nextBoolean() ? 0xFF99FFCC : 0xFF146666,
			"textures/particle/glow.png", 1);
		// 26.3's Particle(x, y, z, xa, ya, za) with xa, za = 0.5 - random and ya = 0, then the provider's cuts
		double xd = 0.5D - particleRand.nextDouble() + (particleRand.nextDouble() * 2.0D - 1.0D) * 0.4D;
		double yd = (particleRand.nextDouble() * 2.0D - 1.0D) * 0.4D;
		double zd = 0.5D - particleRand.nextDouble() + (particleRand.nextDouble() * 2.0D - 1.0D) * 0.4D;
		double speed = (particleRand.nextDouble() + particleRand.nextDouble() + 1.0D) * 0.15D;
		double length = Math.sqrt(xd * xd + yd * yd + zd * zd);
		motionX = xd / length * speed * 0.4D * 0.1D;
		motionY = (yd / length * speed * 0.4D + 0.1D) * 0.2D;
		motionZ = zd / length * speed * 0.4D * 0.1D;
		particleGravity = 0.0F;
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		// 1.7.10's particles keep 0.98 a tick, 26.3's glow 0.96
		motionX *= 0.96D / 0.98D;
		motionY *= 0.96D / 0.98D;
		motionZ *= 0.96D / 0.98D;
	}

	/** 26.3's `addSmoothBlockEmission`: its own light rising to full over its life. */
	@Override
	public int getBrightnessForRender(float partialTicks) {
		int lit = super.getBrightnessForRender(partialTicks);
		int own = Math.min(15, (int) ((particleAge + partialTicks) / particleMaxAge * 15.0F));
		return (lit & ~0xFFFF) | Math.max(lit >> 4 & 15, own) << 4;
	}
}
