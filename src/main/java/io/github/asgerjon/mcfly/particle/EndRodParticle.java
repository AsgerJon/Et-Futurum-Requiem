package io.github.asgerjon.mcfly.particle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.world.World;

/**
 * 26.3's end rod particle (`EndRodParticle`, `SimpleAnimatedParticle`; SPEC-modern-movement.md §84): the glitter frames
 * from glitter_7 down to glitter_0 over its 60 to 71 ticks, three quarters of the usual size, its own motion only (no
 * push of 1.7.10's), gravity 0.0125, friction 0.91, passing through blocks, full bright; over its second half it fades
 * and its colour eases a fifth a tick toward 0xF2EDC9.
 */
@SideOnly(Side.CLIENT)
public class EndRodParticle extends McFlyParticle {

	private static final float FADE_R = 0xF2 / 255.0F;
	private static final float FADE_G = 0xED / 255.0F;
	private static final float FADE_B = 0xC9 / 255.0F;

	public EndRodParticle(World world, double x, double y, double z, double mx, double my, double mz) {
		super(world, x, y, z, mx, my, mz, 60 + McFlyParticlesClient.RANDOM.nextInt(12), 0.75F * (particleRand.nextFloat() * 0.5F + 0.5F) * 2.0F,
			0xFFFFFFFF, "textures/particle/glitter.png", 8);
		motionX = mx;
		motionY = my;
		motionZ = mz;
		particleGravity = 0.0125F;
		fadeAway = true;
		currentTexture = 7;
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		// 1.7.10 slows every particle by 0.98 a tick; this one by 26.3's 0.91
		motionX *= 0.91D / 0.98D;
		motionY *= 0.91D / 0.98D;
		motionZ *= 0.91D / 0.98D;
		currentTexture = 7 - Math.min(particleAge, particleMaxAge) * 7 / particleMaxAge;
		if (particleAge > particleMaxAge / 2) {
			particleRed += (FADE_R - particleRed) * 0.2F;
			particleGreen += (FADE_G - particleGreen) * 0.2F;
			particleBlue += (FADE_B - particleBlue) * 0.2F;
		}
	}

	@Override
	public int getBrightnessForRender(float partialTicks) {
		return 0xF000F0;
	}
}
