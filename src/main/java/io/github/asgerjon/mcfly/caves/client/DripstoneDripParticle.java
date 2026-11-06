package io.github.asgerjon.mcfly.caves.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.particle.McFlyParticle;
import net.minecraft.block.material.Material;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * A pointed dripstone's drip (SPEC-modern-movement.md §90.7), 26.3's `DripParticle` dripstone water and lava in their
 * stages as one particle on 1.7.10's drip cells (113 hanging, 112 falling): it hangs 40 ticks barely moving (at 0.06 x
 * 0.02 gravity; lava cooling from yellow to red, 26.3's `CoolingDripHangParticle`), falls at 0.06 for 64 / (0.2 to 1)
 * ticks, and on the ground is gone with 26.3's drip sound (0.3 to 1 loud) and, for water, a splash. Water blue (0.2, 0.3,
 * 1); falling lava (1, 0.2857, 0.0833). Gone in its own fluid.
 */
@SideOnly(Side.CLIENT)
public final class DripstoneDripParticle extends McFlyParticle {

	private static final String SHEET = "minecraft:textures/particle/particles.png";
	private final boolean lava;
	private boolean falling;
	private double gravity;

	public DripstoneDripParticle(World world, double x, double y, double z, boolean lava) {
		super(world, x, y, z, 0.0D, 0.0D, 0.0D, 40, (particleRand.nextFloat() * 0.5F + 0.5F) * 2.0F, 0xFFFFFFFF, SHEET, 1);
		this.lava = lava;
		motionX = motionY = motionZ = 0.0D;
		setSize(0.01F, 0.01F);
		noClip = false;
		gravity = 0.06D * 0.02D;
		if (lava) {
			particleRed = 1.0F;
			particleGreen = 16.0F / 16.0F;
			particleBlue = 4.0F / 8.0F;
		} else {
			particleRed = 0.2F;
			particleGreen = 0.3F;
			particleBlue = 1.0F;
		}
		setParticleTextureIndex(113);
	}

	public static DripstoneDripParticle water(World world, double x, double y, double z, double xd, double yd, double zd) {
		return new DripstoneDripParticle(world, x, y, z, false);
	}

	public static DripstoneDripParticle lava(World world, double x, double y, double z, double xd, double yd, double zd) {
		return new DripstoneDripParticle(world, x, y, z, true);
	}

	@Override
	public void onUpdate() {
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;
		if (!falling && lava) {
			// 26.3's cooling: green and blue fall as the hang's 40 ticks run out
			int lifetime = particleMaxAge - particleAge;
			particleGreen = 16.0F / (40 - lifetime + 16);
			particleBlue = 4.0F / (40 - lifetime + 8);
		}
		if (particleAge++ >= particleMaxAge) {
			if (!falling) {
				falling = true;
				gravity = 0.06D;
				particleAge = 0;
				particleMaxAge = (int) (64.0D / (particleRand.nextFloat() * 0.8D + 0.2D));
				setParticleTextureIndex(112);
				if (lava) {
					particleRed = 1.0F;
					particleGreen = 0.2857143F;
					particleBlue = 0.083333336F;
				}
			} else {
				setDead();
				return;
			}
		}
		motionY -= gravity;
		moveEntity(motionX, motionY, motionZ);
		if (!falling) {
			motionX *= 0.02D;
			motionY *= 0.02D;
			motionZ *= 0.02D;
		} else if (onGround) {
			setDead();
			if (!lava) {
				worldObj.spawnParticle("splash", posX, posY, posZ, 0.0D, 0.0D, 0.0D);
			}
			worldObj.playSound(posX, posY, posZ, McFly.MC_ASSETS + ":block.pointed_dripstone." + (lava ? "drip_lava" : "drip_water"),
				0.3F + particleRand.nextFloat() * 0.7F, 1.0F, false);
			return;
		}
		motionX *= 0.98D;
		motionY *= 0.98D;
		motionZ *= 0.98D;
		Material material = worldObj.getBlock(MathHelper.floor_double(posX), MathHelper.floor_double(posY), MathHelper.floor_double(posZ)).getMaterial();
		if (material == (lava ? Material.lava : Material.water)) {
			setDead();
		}
	}
}
