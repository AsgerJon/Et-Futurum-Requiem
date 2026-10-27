package ganymedes01.etfuturum.elytra;

import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.items.equipment.ItemArmorElytra;
import ganymedes01.etfuturum.potion.ModPotions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

/**
 * Modern elytra gliding ("fall flying"). Behaviour follows SPEC-modern-movement.md part 4 (section numbers in the
 * comments refer to it). The hitbox and eye height come from the pose system (PlayerPose.FALL_FLYING).
 */
public final class FallFlying {

	public static final DamageSource FLY_INTO_WALL = new DamageSource("flyIntoWall").setDamageBypassesArmor();

	private FallFlying() {
	}

	public static boolean isFallFlying(Entity entity) {
		return entity instanceof IElytraPlayer && ((IElytraPlayer) entity).etfu$isElytraFlying();
	}

	/**
	 * Can glide (§18.1): in the air, not riding, no Levitation, not creative-flying, and wearing an elytra that isn't
	 * one hit from breaking.
	 */
	public static boolean canGlide(EntityPlayer player) {
		if (player.onGround || player.isRiding() || player.capabilities.isFlying) {
			return false;
		}
		if (ModPotions.levitation != null && player.isPotionActive(ModPotions.levitation)) {
			return false;
		}
		ItemStack elytra = ItemArmorElytra.getElytra(player);
		return elytra != null && !ItemArmorElytra.isBroken(elytra);
	}

	/**
	 * Try to start (§18.2): not already gliding, can glide, and not touching water or lava. Turns the flag on and
	 * returns true if it worked. With the old takeoff logic option, the player also has to be falling.
	 */
	public static boolean tryToStart(EntityPlayer player) {
		if (!isFallFlying(player) && canGlide(player) && !player.isInWater() && !player.handleLavaMovement()
				&& (ConfigMixins.enableNewElytraTakeoffLogic || player.motionY < 0.0D)) {
			((IElytraPlayer) player).etfu$setElytraFlying(true);
			return true;
		}
		return false;
	}

	/**
	 * Stops gliding. The flag is turned on and off again so the DataWatcher always sends the change, even when it was
	 * already off here and only the client thought it was gliding.
	 */
	public static void stop(EntityPlayer player) {
		IElytraPlayer elytraPlayer = (IElytraPlayer) player;
		elytraPlayer.etfu$setElytraFlying(true);
		elytraPlayer.etfu$setElytraFlying(false);
	}

	/**
	 * Each tick while gliding, right before the travel step (§18.3): keep the fall distance from building up unless
	 * diving steeply; on the server, stop when gliding isn't possible any more and wear the elytra down by 1 every 20
	 * ticks.
	 */
	public static void beforeTravel(EntityPlayer player) {
		if (player.motionY > -0.5D && player.fallDistance > 1.0F) {
			player.fallDistance = 1.0F;
		}
		if (player.worldObj.isRemote) {
			return;
		}
		if (!canGlide(player)) {
			((IElytraPlayer) player).etfu$setElytraFlying(false);
			return;
		}
		int ticks = ((IElytraPlayer) player).etfu$getFallFlyingTicks() + 1;
		if (ticks % 20 == 0) {
			ItemArmorElytra.getElytra(player).damageItem(1, player);
		}
	}

	/**
	 * The gliding travel step (§19). Returns false when the normal movement should run instead: on a ladder or vine,
	 * which also stops gliding.
	 */
	public static boolean travel(EntityPlayer player) {
		if (player.isOnLadder()) {
			stop(player);
			return false;
		}
		double speedBefore = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
		updateMotion(player, speedBefore);
		player.moveEntity(player.motionX, player.motionY, player.motionZ);

		if (!player.worldObj.isRemote && player.isCollidedHorizontally) {
			double speedAfter = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
			float damage = (float) ((speedBefore - speedAfter) * 10.0D - 3.0D);
			if (damage > 0.0F) {
				player.playSound((int) damage > 4 ? "game.player.hurt.fall.big" : "game.player.hurt.fall.small", 1.0F, 1.0F);
				player.attackEntityFrom(FLY_INTO_WALL, damage);
			}
		}
		return true;
	}

	/**
	 * New velocity from the old one and the look direction; the movement keys don't matter (§19 step 2). 1.7.10 has
	 * no Slow Falling, so gravity is always 0.08.
	 */
	private static void updateMotion(EntityPlayer player, double speedH) {
		Vec3 look = player.getLookVec();
		float pitch = player.rotationPitch * ((float) Math.PI / 180.0F);
		double lookH = Math.sqrt(look.xCoord * look.xCoord + look.zCoord * look.zCoord);
		double gravity = 0.08D;
		double cos = Math.cos(pitch);
		double lift = cos * cos;

		player.motionY += gravity * (-1.0D + lift * 0.75D);

		if (player.motionY < 0.0D && lookH > 0.0D) {
			double dive = player.motionY * -0.1D * lift;
			player.motionX += look.xCoord * dive / lookH;
			player.motionY += dive;
			player.motionZ += look.zCoord * dive / lookH;
		}

		if (pitch < 0.0F && lookH > 0.0D) {
			double climb = speedH * -MathHelper.sin(pitch) * 0.04D;
			player.motionX -= look.xCoord * climb / lookH;
			player.motionY += climb * 3.2D;
			player.motionZ -= look.zCoord * climb / lookH;
		}

		if (lookH > 0.0D) {
			player.motionX += (look.xCoord / lookH * speedH - player.motionX) * 0.1D;
			player.motionZ += (look.zCoord / lookH * speedH - player.motionZ) * 0.1D;
		}

		player.motionX *= 0.99F;
		player.motionY *= 0.98F;
		player.motionZ *= 0.99F;
	}

	/**
	 * A firework rocket attached to a gliding entity pulls its velocity halfway towards 1.5 blocks per tick along the
	 * look direction, plus a small push (§20). Shared so other boosters can reuse it.
	 */
	public static void applyRocketBoost(EntityLivingBase entity) {
		Vec3 look = entity.getLookVec();
		entity.motionX += look.xCoord * 0.1D + (look.xCoord * 1.5D - entity.motionX) * 0.5D;
		entity.motionY += look.yCoord * 0.1D + (look.yCoord * 1.5D - entity.motionY) * 0.5D;
		entity.motionZ += look.zCoord * 0.1D + (look.zCoord * 1.5D - entity.motionZ) * 0.5D;
	}

	/**
	 * How much faster than walking the body is moving, for slowing the limb swing while gliding (§21.2): (|v|² / 0.2)³,
	 * at least 1. Always 1 when not gliding.
	 */
	public static float getLimbSlowdown(EntityLivingBase entity) {
		if (!isFallFlying(entity)) {
			return 1.0F;
		}
		float speed = (float) (entity.motionX * entity.motionX + entity.motionY * entity.motionY + entity.motionZ * entity.motionZ);
		speed /= 0.2F;
		speed = speed * speed * speed;
		return Math.max(speed, 1.0F);
	}
}
