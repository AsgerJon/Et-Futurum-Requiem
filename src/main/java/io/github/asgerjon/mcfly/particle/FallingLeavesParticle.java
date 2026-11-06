package io.github.asgerjon.mcfly.particle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.world.World;

/**
 * 26.3's falling leaves (`FallingParticle`, `FallingLeavesParticle`; SPEC-modern-movement.md §90, step D2): 300 ticks;
 * falling from its start speed, faster each tick by its fall acceleration (x 1.2 x 0.0025), no slowing; pushed sideways
 * by its wind (a swirl, or flowing away, growing with its age); spinning, faster and faster; gone on the ground or once
 * stopped sideways. One of its leaf pictures, chosen once.
 */
@SideOnly(Side.CLIENT)
public class FallingLeavesParticle extends McFlyParticle {

	private static final int LIFETIME = 300;

	private float rotSpeed;
	private final float spinAcceleration;
	private final float windBig;
	private final boolean swirl;
	private final boolean flowAway;
	private final double xaFlowScale;
	private final double zaFlowScale;
	private final double swirlPeriod;
	private final float fallSpeedUp;

	public FallingLeavesParticle(World world, double x, double y, double z, String texture, int frames, float fallAcceleration, float sideAcceleration,
		boolean swirl, boolean flowAway, float scale, float startVelocity) {
		super(world, x, y, z, 0.0D, 0.0D, 0.0D, LIFETIME, 1.0F, 0xFFFFFFFF, texture, frames);
		rotSpeed = (float) Math.toRadians(particleRand.nextBoolean() ? -30.0D : 30.0D);
		spinAcceleration = (float) Math.toRadians(particleRand.nextBoolean() ? -5.0D : 5.0D);
		windBig = sideAcceleration;
		this.swirl = swirl;
		this.flowAway = flowAway;
		fallSpeedUp = fallAcceleration * 1.2F * 0.0025F;
		float size = scale * (particleRand.nextBoolean() ? 0.05F : 0.075F);
		// 26.3's quad size is half its side, as McFlyParticle's 0.1 x scale
		particleScale = size / 0.1F;
		setSize(size, size);
		noClip = false;
		motionX = 0.0D;
		motionY = -startVelocity;
		motionZ = 0.0D;
		float particleRandom = particleRand.nextFloat();
		xaFlowScale = Math.cos(Math.toRadians(particleRandom * 60.0F)) * windBig;
		zaFlowScale = Math.sin(Math.toRadians(particleRandom * 60.0F)) * windBig;
		swirlPeriod = Math.toRadians(1000.0F + particleRandom * 3000.0F);
		currentTexture = particleRand.nextInt(frames);
	}

	/** 26.3's `TintedLeavesProvider`: 0.07 falling, 10 sideways, a swirl, twice the size, 0.021 to start (its tint set by who makes it). */
	public static FallingLeavesParticle tinted(World world, double x, double y, double z, double xd, double yd, double zd) {
		return new FallingLeavesParticle(world, x, y, z, "textures/particle/leaf.png", 12, 0.07F, 10.0F, true, false, 2.0F, 0.021F);
	}

	/** 26.3's `CherryProvider`: 0.25 falling, 2 sideways, flowing away, size 1, no start speed; untinted. */
	public static FallingLeavesParticle cherry(World world, double x, double y, double z, double xd, double yd, double zd) {
		return new FallingLeavesParticle(world, x, y, z, "textures/particle/cherry.png", 12, 0.25F, 2.0F, false, true, 1.0F, 0.0F);
	}

	/** 26.3's `FallingParticle.tick`, its lifetime counted down as 1.7.10's age counts up. */
	@Override
	public void onUpdate() {
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;
		if (particleAge++ >= particleMaxAge) {
			setDead();
			return;
		}
		float relativeAge = Math.min(particleAge / (float) LIFETIME, 1.0F);
		double xa = 0.0D;
		double za = 0.0D;
		if (flowAway) {
			xa += xaFlowScale * Math.pow(relativeAge, 1.25D);
			za += zaFlowScale * Math.pow(relativeAge, 1.25D);
		}
		if (swirl) {
			xa += relativeAge * Math.cos(relativeAge * swirlPeriod) * windBig;
			za += relativeAge * Math.sin(relativeAge * swirlPeriod) * windBig;
		}
		motionX += xa * 0.0025F;
		motionZ += za * 0.0025F;
		motionY -= fallSpeedUp;
		rotSpeed += spinAcceleration / 20.0F;
		prevRoll = roll;
		roll += rotSpeed / 20.0F;
		moveEntity(motionX, motionY, motionZ);
		// 1.7.10's move, as 26.3's, stops a blocked side's speed and marks a landing
		if (onGround || particleAge > 1 && (motionX == 0.0D || motionZ == 0.0D)) {
			setDead();
		}
		lightLevel = worldObj.getLightBrightness((int) Math.floor(posX), (int) Math.floor(posY), (int) Math.floor(posZ));
	}
}
