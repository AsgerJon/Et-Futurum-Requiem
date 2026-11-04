package ganymedes01.etfuturum.client.particle;

import net.minecraft.world.World;

/** 26.3's electric spark: a glow particle, white with a touch of pink, gone in 2 or 3 ticks, its push a quarter of what it's given. */
public class ElectricSparkFX extends EtFuturumFXParticle {

	public ElectricSparkFX(World world, double x, double y, double z, double mx, double my, double mz) {
		super(world, x, y, z, 0, 0, 0, 2 + CustomParticles.rand.nextInt(2),
				(CustomParticles.rand.nextFloat() * 0.5F + 0.5F) * 2.0F * 0.75F, 0xFFFFE6FF, "textures/particle/glow.png", 1);
		motionX = mx * 0.25D;
		motionY = my * 0.25D;
		motionZ = mz * 0.25D;
		this.particleGravity = 0;
	}

	@Override
	public int getBrightnessForRender(float partialTicks) {
		return 0xF000F0;
	}
}
