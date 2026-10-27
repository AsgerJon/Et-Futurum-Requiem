package ganymedes01.etfuturum.elytra;

import ganymedes01.etfuturum.movement.ModernMovement;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Client-side drawing state of a player's elytra (§21.1, §21.3): the wing angles, eased towards their target every
 * tick, and the velocity at the start of the tick, so the body roll can be interpolated.
 */
public class ElytraAnimationState {

	private static final float PI = (float) Math.PI;

	private float rotX = PI / 12.0F;
	private float rotY;
	private float rotZ = -PI / 12.0F;
	private float rotXOld = rotX;
	private float rotYOld;
	private float rotZOld = rotZ;

	private double motionXOld;
	private double motionYOld;
	private double motionZOld;

	/**
	 * Remembers the velocity at the start of the tick.
	 */
	public void startTick(EntityPlayer player) {
		motionXOld = player.motionX;
		motionYOld = player.motionY;
		motionZOld = player.motionZ;
	}

	/**
	 * Moves each wing angle 30% of the way to its target: spread while gliding (folded in by a steep dive), raised while
	 * crouching, resting otherwise.
	 */
	public void tick(EntityPlayer player) {
		rotXOld = rotX;
		rotYOld = rotY;
		rotZOld = rotZ;
		float targetX;
		float targetY;
		float targetZ;
		if (FallFlying.isFallFlying(player)) {
			float spread = 1.0F;
			if (player.motionY < 0.0D) {
				double length = Math.sqrt(player.motionX * player.motionX + player.motionY * player.motionY + player.motionZ * player.motionZ);
				spread = 1.0F - (float) Math.pow(-player.motionY / length, 1.5D);
			}
			targetX = lerp(spread, PI / 12.0F, PI / 9.0F);
			targetY = 0.0F;
			targetZ = lerp(spread, -PI / 12.0F, -PI / 2.0F);
		} else if (ModernMovement.isVisuallyCrouching(player)) {
			targetX = PI * 2.0F / 9.0F;
			targetY = 0.08726646F;
			targetZ = -PI / 4.0F;
		} else {
			targetX = PI / 12.0F;
			targetY = 0.0F;
			targetZ = -PI / 12.0F;
		}
		rotX += (targetX - rotX) * 0.3F;
		rotY += (targetY - rotY) * 0.3F;
		rotZ += (targetZ - rotZ) * 0.3F;
	}

	public float getRotX(float partialTicks) {
		return lerp(partialTicks, rotXOld, rotX);
	}

	public float getRotY(float partialTicks) {
		return lerp(partialTicks, rotYOld, rotY);
	}

	public float getRotZ(float partialTicks) {
		return lerp(partialTicks, rotZOld, rotZ);
	}

	/**
	 * The velocity interpolated from the start of this tick to now: {x, y, z}.
	 */
	public double[] getMotion(EntityPlayer player, float partialTicks) {
		return new double[]{
				motionXOld + (player.motionX - motionXOld) * partialTicks,
				motionYOld + (player.motionY - motionYOld) * partialTicks,
				motionZOld + (player.motionZ - motionZOld) * partialTicks
		};
	}

	private static float lerp(float amount, float from, float to) {
		return from + amount * (to - from);
	}
}
