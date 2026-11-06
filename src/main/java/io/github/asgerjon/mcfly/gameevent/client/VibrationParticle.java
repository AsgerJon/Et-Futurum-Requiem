package io.github.asgerjon.mcfly.gameevent.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.asgerjon.mcfly.particle.McFlyParticle;
import net.minecraft.world.World;

/**
 * A vibration on its way (SPEC-modern-movement.md §90.9, step H1), 26.3's `VibrationSignalParticle`: from where it was
 * made straight to its listener in floor(distance) ticks (26.3's travel time), its 7 frames a tick each, lit by itself,
 * then gone.
 */
@SideOnly(Side.CLIENT)
public final class VibrationParticle extends McFlyParticle {

	private final double startX, startY, startZ, dx, dy, dz;

	public VibrationParticle(World world, double x, double y, double z, double dx, double dy, double dz) {
		super(world, x, y, z, 0.0D, 0.0D, 0.0D, Math.max(1, (int) Math.floor(Math.sqrt(dx * dx + dy * dy + dz * dz))), 1.5F, 0xFFFFFFFF,
			"textures/particle/vibration.png", 7);
		startX = x;
		startY = y;
		startZ = z;
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		motionX = motionY = motionZ = 0.0D;
		noClip = true;
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
		// 26.3's sprite, a frame a tick
		currentTexture = particleAge % 7;
		double along = (double) particleAge / particleMaxAge;
		setPosition(startX + dx * along, startY + dy * along, startZ + dz * along);
	}

	@Override
	public int getBrightnessForRender(float partialTicks) {
		return 0xF000F0;
	}
}
