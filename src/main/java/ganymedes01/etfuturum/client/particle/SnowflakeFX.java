package ganymedes01.etfuturum.client.particle;

import net.minecraft.client.particle.EntityFX;
import net.minecraft.world.World;

/**
 * 26.3's {@code SnowflakeParticle} (SPEC-modern-movement.md §62.5): the generic sprites 7 down to 0 over its life,
 * tinted white-blue, gravity 0.225 without 1.7.10's 0.98 drag, then x/z * 0.95 and y * 0.9 each tick.
 */
public class SnowflakeFX extends EntityFX {

	public SnowflakeFX(World world, double x, double y, double z, double motionX, double motionY, double motionZ) {
		super(world, x, y, z);
		this.particleGravity = 0.225F;
		this.motionX = motionX + (rand.nextFloat() * 2.0F - 1.0F) * 0.05F;
		this.motionY = motionY + (rand.nextFloat() * 2.0F - 1.0F) * 0.05F;
		this.motionZ = motionZ + (rand.nextFloat() * 2.0F - 1.0F) * 0.05F;
		// 1.7.10 draws 0.1 * particleScale: 26.3's quadSize 0.1 * (1 + r * r)
		this.particleScale = 1.0F + rand.nextFloat() * rand.nextFloat();
		this.particleMaxAge = (int) (16.0D / (rand.nextFloat() * 0.8D + 0.2D)) + 2;
		this.particleRed = 0.923F;
		this.particleGreen = 0.964F;
		this.particleBlue = 0.999F;
		setParticleTextureIndex(7);
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
		setParticleTextureIndex(7 - particleAge * 7 / particleMaxAge);
		motionY -= 0.04D * particleGravity;
		moveEntity(motionX, motionY, motionZ);
		if (onGround) {
			motionX *= 0.7D;
			motionZ *= 0.7D;
		}
		motionX *= 0.95D;
		motionY *= 0.9D;
		motionZ *= 0.95D;
	}
}
