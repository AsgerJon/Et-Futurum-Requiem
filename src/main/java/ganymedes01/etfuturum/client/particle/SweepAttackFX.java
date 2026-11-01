package ganymedes01.etfuturum.client.particle;

import net.minecraft.world.World;

/**
 * Modern's sweep (spec §46.11): an arc two blocks across, facing the camera, at full brightness in a random grey (0.4
 * to 1), for 4 ticks, through the frames sweep_0 to sweep_7 as modern's sprite set steps them by age (0, 1, 3, 5, 7).
 */
public class SweepAttackFX extends EtFuturumFXParticle {

	public SweepAttackFX(World world, double x, double y, double z) {
		super(world, x, y, z, 0, 0, 0, 4, 10.0F, 0xFFFFFFFF, "textures/particle/sweep.png", 8);
		float grey = particleRand.nextFloat() * 0.6F + 0.4F;
		particleRed = particleGreen = particleBlue = grey;
		motionX = motionY = motionZ = 0.0D;
		particleGravity = 0.0F;
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		currentTexture = Math.min(particleAge, particleMaxAge) * (textures - 1) / particleMaxAge;
	}

	@Override
	public int getBrightnessForRender(float partialTicks) {
		return 0xF000F0;
	}
}
