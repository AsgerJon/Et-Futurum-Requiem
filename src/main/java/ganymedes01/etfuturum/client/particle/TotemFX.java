package ganymedes01.etfuturum.client.particle;

import io.github.asgerjon.mcfly.particle.McFlyParticle;
import io.github.asgerjon.mcfly.particle.McFlyParticlesClient;
import net.minecraft.world.World;

/**
 * Modern's totem particle (spec §55.2): the glitter frames from glitter_7 down to glitter_0 over its 60 to 71 ticks, full
 * bright, gravity 1.25, friction 0.6, settling on blocks, a quarter of them yellowish and the rest green, fading out in
 * its second half.
 */
public class TotemFX extends McFlyParticle {

	public TotemFX(World world, double x, double y, double z, double mx, double my, double mz) {
		super(world, x, y, z, mx, my, mz, 60 + McFlyParticlesClient.RANDOM.nextInt(12), 0.75F * (particleRand.nextFloat() * 0.5F + 0.5F) * 2.0F,
			0xFFFFFFFF, "textures/particle/glitter.png", 8);
		noClip = false;
		particleGravity = 1.25F;
		fadeAway = true;
		if (particleRand.nextInt(4) == 0) {
			setRBGColorF(0.6F + particleRand.nextFloat() * 0.2F, 0.6F + particleRand.nextFloat() * 0.3F, particleRand.nextFloat() * 0.2F);
		} else {
			setRBGColorF(0.1F + particleRand.nextFloat() * 0.2F, 0.4F + particleRand.nextFloat() * 0.3F, particleRand.nextFloat() * 0.2F);
		}
		currentTexture = 7;
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		// 1.7.10 slows every particle by 0.98 a tick; this one by modern's 0.6
		motionX *= 0.6D / 0.98D;
		motionY *= 0.6D / 0.98D;
		motionZ *= 0.6D / 0.98D;
		currentTexture = 7 - Math.min(particleAge, particleMaxAge) * 7 / particleMaxAge;
	}

	@Override
	public int getBrightnessForRender(float partialTicks) {
		return 0xF000F0;
	}
}
