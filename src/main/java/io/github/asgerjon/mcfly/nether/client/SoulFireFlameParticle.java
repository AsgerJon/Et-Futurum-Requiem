package io.github.asgerjon.mcfly.nether.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.particle.McFlyParticle;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.World;

/**
 * 26.3's `soul_fire_flame` (SPEC-modern-movement.md §88.5), its `FlameParticle`: moved by what it's given (a hundredth
 * of a random push on top), slowed to 0.96 each tick, lasting 8 / (0.2 to 1) + 4 ticks, shrinking to half by its end
 * (1 - (age / lifetime)^2 / 2), lit by itself; 26.3's picture.
 */
@SideOnly(Side.CLIENT)
public class SoulFireFlameParticle extends McFlyParticle {

	private final float baseScale;

	public SoulFireFlameParticle(World world, double x, double y, double z, double xd, double yd, double zd) {
		super(world, x, y, z, xd, yd, zd, (int) (8.0D / (particleRand.nextFloat() * 0.8D + 0.2D)) + 4, (particleRand.nextFloat() * 0.5F + 0.5F) * 2.0F,
			0xFFFFFFFF, "minecraft:textures/particle/soul_fire_flame.png", 1);
		this.baseScale = particleScale;
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
		moveEntity(motionX, motionY, motionZ);
		motionX *= 0.96D;
		motionY *= 0.96D;
		motionZ *= 0.96D;
	}

	@Override
	public void renderParticle(Tessellator tessellator, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
		float age = (particleAge + partialTicks) / particleMaxAge;
		particleScale = baseScale * (1.0F - age * age * 0.5F);
		super.renderParticle(tessellator, partialTicks, rx, rxz, rz, ryz, rxy);
	}

	@Override
	public int getBrightnessForRender(float partialTicks) {
		int light = super.getBrightnessForRender(partialTicks);
		return (light & 0xFFFF0000) | 240;
	}
}
