package ganymedes01.etfuturum.client.particle;

import net.minecraft.world.World;

/**
 * The glow squid's ink (SPEC-modern-movement.md §67.1, 26.3's `SquidInkParticle.GlowInkProvider`): a puff of colour
 * 0.2, 0.8, 0.6, half a block across, 6 to 30 ticks, shrinking through the sheet's 8 puffs (26.3's `generic_7` to
 * `generic_0`), slowing by 0.92 a tick, fading over its second half, sinking a little in air.
 */
public class GlowSquidInkFX extends EtFuturumFXParticle {

	public GlowSquidInkFX(World world, double x, double y, double z, double motionX, double motionY, double motionZ) {
		super(world, x, y, z, 0.0D, 0.0D, 0.0D, (int) (0.5F * 12.0F / (particleRand.nextFloat() * 0.8F + 0.2F)), 5.0F, 0xFF33CC99,
			"minecraft:textures/particle/particles.png", 1);
		this.motionX = motionX;
		this.motionY = motionY;
		this.motionZ = motionZ;
		particleGravity = 0.0F;
		fadeAway = true;
		setParticleTextureIndex(7);
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		motionX *= 0.92D / 0.98D;
		motionY *= 0.92D / 0.98D;
		motionZ *= 0.92D / 0.98D;
		if (worldObj.isAirBlock((int) Math.floor(posX), (int) Math.floor(posY), (int) Math.floor(posZ))) {
			motionY -= 0.0074D;
		}
		setParticleTextureIndex(Math.max(0, 7 - particleAge * 8 / Math.max(1, particleMaxAge)));
	}
}
