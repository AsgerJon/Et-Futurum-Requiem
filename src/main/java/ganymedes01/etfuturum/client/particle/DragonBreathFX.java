package ganymedes01.etfuturum.client.particle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.World;

/**
 * 26.3's dragon's breath particle (SPEC-modern-movement.md §76.8): purple, three quarters size growing in over the first
 * 1/32 of its life, the small dots of the particle sheet as it ages, no gravity, no collisions; its sideways speed times
 * 1.1 then 0.96 a tick while it doesn't climb.
 */
@SideOnly(Side.CLIENT)
public class DragonBreathFX extends EntityFX {

	private static final int[] SPRITES = {5, 6, 7};

	public DragonBreathFX(World world, double x, double y, double z, double vx, double vy, double vz) {
		super(world, x, y, z, 0.0, 0.0, 0.0);
		motionX = vx;
		motionY = vy;
		motionZ = vz;
		particleRed = 0.7176471F + rand.nextFloat() * (0.8745098F - 0.7176471F);
		particleGreen = 0.0F;
		particleBlue = 0.8235294F + rand.nextFloat() * (0.9764706F - 0.8235294F);
		particleScale *= 0.75F;
		particleMaxAge = (int) (20.0 / (rand.nextFloat() * 0.8 + 0.2));
		particleGravity = 0.0F;
		noClip = true;
		setParticleTextureIndex(SPRITES[0]);
	}

	public static void spawn(World world, double x, double y, double z, double vx, double vy, double vz) {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.effectRenderer != null && mc.renderViewEntity != null && mc.renderViewEntity.getDistanceSq(x, y, z) < 64.0 * 64.0) {
			mc.effectRenderer.addEffect(new DragonBreathFX(world, x, y, z, vx, vy, vz));
		}
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
		setParticleTextureIndex(SPRITES[Math.min(SPRITES.length - 1, particleAge * SPRITES.length / Math.max(1, particleMaxAge))]);
		double oldY = posY;
		moveEntity(motionX, motionY, motionZ);
		if (posY == oldY) {
			motionX *= 1.1;
			motionZ *= 1.1;
		}
		motionX *= 0.96F;
		motionZ *= 0.96F;
	}

	@Override
	public void renderParticle(Tessellator tessellator, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
		float scale = particleScale;
		particleScale = scale * Math.max(0.0F, Math.min(1.0F, (particleAge + partialTicks) / particleMaxAge * 32.0F));
		super.renderParticle(tessellator, partialTicks, rx, rxz, rz, ryz, rxy);
		particleScale = scale;
	}
}
