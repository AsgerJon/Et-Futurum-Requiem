package ganymedes01.etfuturum.potion;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Slow Falling (SPEC-modern-movement.md §68.4, 26.3's `getEffectiveGravity`): falling (motion y at most 0), gravity is
 * 0.01 instead of 1.7.10's 0.08 (0.07 given back before the move takes 0.08), and the fall distance is 0 every tick.
 * Sent to clients (a player's own fall is worked out there), as Levitation.
 */
public class PotionSlowFalling extends ModPotions {

	protected PotionSlowFalling(String name, int id, boolean isBad, int color) {
		super(name, id, isBad, color);
		setPotionName("potion." + name);
	}

	@Override
	public void performEffect(EntityLivingBase entity, int level) {
		entity.fallDistance = 0.0F;
		if (entity.motionY <= 0.0D && !(entity instanceof EntityPlayer player && player.capabilities.isFlying)
			&& !entity.isInWater() && !entity.handleLavaMovement()) {
			entity.motionY += 0.07D;
		}
	}

	@Override
	public boolean isReady(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean hasPacket() {
		return true;
	}
}
