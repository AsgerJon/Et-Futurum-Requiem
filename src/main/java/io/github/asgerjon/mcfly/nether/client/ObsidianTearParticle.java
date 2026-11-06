package io.github.asgerjon.mcfly.nether.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.particle.McFlyParticle;
import net.minecraft.world.World;

/**
 * Crying obsidian's tear (SPEC-modern-movement.md §88.3), 26.3's `DripParticle` obsidian tear in its three stages as one
 * particle on 1.7.10's drip cells (113 hanging, 112 falling, 114 landed): it hangs 100 ticks barely moving
 * (`dripping_obsidian_tear`), falls at 0.01 a tick² for 64 / (0.2 to 1) ticks till it lands (`falling_obsidian_tear`),
 * then lies 28 / (0.2 to 1) ticks (`landing_obsidian_tear`, 26.3's default 0.06). Purple (0.512, 0.031, 0.891), lit by
 * itself (block light 15).
 */
@SideOnly(Side.CLIENT)
public class ObsidianTearParticle extends McFlyParticle {

	private static final String SHEET = "minecraft:textures/particle/particles.png";
	private static final int HANGING = 0, FALLING = 1, LANDED = 2;

	private int stage = HANGING;
	private double gravity;

	public ObsidianTearParticle(World world, double x, double y, double z) {
		super(world, x, y, z, 0.0D, 0.0D, 0.0D, 100, (particleRand.nextFloat() * 0.5F + 0.5F) * 2.0F, 0xFFFFFFFF, SHEET, 1);
		motionX = motionY = motionZ = 0.0D;
		setSize(0.01F, 0.01F);
		noClip = false;
		particleRed = 0.51171875F;
		particleGreen = 0.03125F;
		particleBlue = 0.890625F;
		// 26.3's: 0.06, times 0.02 for a hanging drip, times 0.01 for a tear
		gravity = 0.06D * 0.02D * 0.01D;
		setParticleTextureIndex(113);
	}

	@Override
	public void onUpdate() {
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;
		if (particleAge++ >= particleMaxAge) {
			if (stage == HANGING) {
				// it lets go: 26.3's falling tear where the drip was, with its motion
				stage = FALLING;
				gravity = 0.01D;
				particleAge = 0;
				particleMaxAge = (int) (64.0D / (particleRand.nextFloat() * 0.8D + 0.2D));
				setParticleTextureIndex(112);
			} else {
				setDead();
				return;
			}
		}
		motionY -= gravity;
		moveEntity(motionX, motionY, motionZ);
		if (stage == HANGING) {
			motionX *= 0.02D;
			motionY *= 0.02D;
			motionZ *= 0.02D;
		} else if (stage == FALLING && onGround) {
			// landed: 26.3's landing tear, still, at the default gravity
			stage = LANDED;
			gravity = 0.06D;
			particleAge = 0;
			particleMaxAge = (int) (28.0D / (particleRand.nextFloat() * 0.8D + 0.2D));
			motionX = motionY = motionZ = 0.0D;
			setParticleTextureIndex(114);
			return;
		}
		motionX *= 0.98D;
		motionY *= 0.98D;
		motionZ *= 0.98D;
	}

	/** Lit by itself: block light 15, the sky's as where it is (26.3's `isGlowing`). */
	@Override
	public int getBrightnessForRender(float partialTicks) {
		int light = super.getBrightnessForRender(partialTicks);
		return (light & 0xFFFF0000) | 240;
	}
}
