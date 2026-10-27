package ganymedes01.etfuturum.movement;

import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.elytra.FallFlying;
import ganymedes01.etfuturum.spectator.SpectatorMode;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;

/**
 * Shared logic for the modern swimming, crawling and sneaking backport. Behaviour follows SPEC-modern-movement.md
 * (section numbers in the comments refer to it).
 */
public final class ModernMovement {

	public static final float PLAYER_WIDTH = 0.6F;

	private ModernMovement() {
	}

	/**
	 * The elytra's hitbox and eyes come from the pose system too (§16).
	 */
	public static boolean isPoseSystemEnabled() {
		return ConfigMixins.enableModernSneaking || ConfigMixins.enableModernSwimming || ConfigMixins.enableElytra;
	}

	/**
	 * Whether the pose system may control this player's hitbox right now. It stands aside while vanilla or other code
	 * owns the size: sleeping and dying shrink the player to 0.2, and a width other than 0.6 means another mod (morphs,
	 * transformations) has resized the player.
	 */
	public static boolean canUsePoses(EntityPlayer player) {
		return !player.isPlayerSleeping()
				&& player.getHealth() > 0.0F
				&& player.width == PLAYER_WIDTH;
	}

	public static boolean isElytraFlying(EntityPlayer player) {
		return FallFlying.isFallFlying(player);
	}

	/**
	 * Drawn in the swimming position (§14, §17): the swimming pose, or the gliding pose for the one tick after gliding
	 * stops, until the pose catches up.
	 */
	public static boolean isVisuallySwimming(EntityPlayer player) {
		PlayerPose pose = ((IPosePlayer) player).etfu$getPose();
		return pose == PlayerPose.SWIMMING || pose == PlayerPose.FALL_FLYING && !isElytraFlying(player);
	}

	/**
	 * Picks the pose for this tick (§4). Runs at the end of the player tick, on the server and for the local player.
	 */
	public static PlayerPose choosePose(EntityPlayer player, PlayerPose current) {
		if (!canFit(player, PlayerPose.SWIMMING)) {
			return current;
		}

		PlayerPose desired = getDesiredPose(player);
		if (player.isRiding() || isSpectator(player) || canFit(player, desired)) {
			return desired;
		}
		if (ConfigMixins.enableModernSneaking && canFit(player, PlayerPose.CROUCHING)) {
			return PlayerPose.CROUCHING;
		}
		if (ConfigMixins.enableModernSwimming) {
			return PlayerPose.SWIMMING;
		}
		return desired;
	}

	private static PlayerPose getDesiredPose(EntityPlayer player) {
		if (ConfigMixins.enableModernSwimming && ((IPosePlayer) player).etfu$isSwimming()) {
			return PlayerPose.SWIMMING;
		}
		if (ConfigMixins.enableElytra && isElytraFlying(player)) {
			return PlayerPose.FALL_FLYING;
		}
		if (ConfigMixins.enableModernSneaking && player.isSneaking() && !player.capabilities.isFlying) {
			return PlayerPose.CROUCHING;
		}
		return PlayerPose.STANDING;
	}

	/**
	 * Updates the swimming flag (§5), right after this tick's water scan. Starting needs the eyes under water (as of
	 * last tick's scan) and water at the feet; carrying on only needs to keep touching water. Sprinting is required
	 * for both.
	 */
	public static void updateSwimming(EntityPlayer player) {
		IPosePlayer posePlayer = (IPosePlayer) player;
		if (player.capabilities.isFlying || isSpectator(player)) {
			posePlayer.etfu$setSwimming(false);
			return;
		}
		boolean canSwim = player.isSprinting() && player.isInWater() && !player.isRiding();
		if (posePlayer.etfu$isSwimming()) {
			posePlayer.etfu$setSwimming(canSwim);
		} else {
			posePlayer.etfu$setSwimming(canSwim && posePlayer.etfu$wasEyeInWater() && isWaterAtFeet(player));
		}
	}

	private static boolean isWaterAtFeet(EntityPlayer player) {
		int x = MathHelper.floor_double(player.posX);
		int y = MathHelper.floor_double(player.boundingBox.minY);
		int z = MathHelper.floor_double(player.posZ);
		return player.worldObj.getBlock(x, y, z).getMaterial() == Material.water;
	}

	/**
	 * Eyes under water: the eye check from the last water scan, while touching water.
	 */
	public static boolean isUnderWater(EntityPlayer player) {
		return ((IPosePlayer) player).etfu$isEyeInWater() && player.isInWater();
	}

	private static boolean isSpectator(EntityPlayer player) {
		return ConfigMixins.enableSpectatorMode && SpectatorMode.isSpectator(player);
	}

	/**
	 * Whether the player's hitbox for this pose, built up from the current feet position and shrunk by 1e-7 on every
	 * side, is free of block and entity collision boxes.
	 */
	public static boolean canFit(EntityPlayer player, PlayerPose pose) {
		AxisAlignedBB box = player.boundingBox;
		AxisAlignedBB poseBox = AxisAlignedBB.getBoundingBox(
				box.minX, box.minY, box.minZ,
				box.maxX, box.minY + pose.height, box.maxZ
		).contract(1.0E-7D, 1.0E-7D, 1.0E-7D);
		return player.worldObj.getCollidingBoundingBoxes(player, poseBox).isEmpty();
	}

	/**
	 * Whether the player should be drawn crouching (§14): the local player's own crouching flag, the synced pose for
	 * other players. Unlike the shift key, this includes crouching forced by a low ceiling and excludes creative
	 * flight.
	 */
	public static boolean isVisuallyCrouching(EntityPlayer player) {
		if (!ConfigMixins.enableModernSneaking) {
			return player.isSneaking();
		}
		if (player instanceof IClientPosePlayer) {
			return ((IClientPosePlayer) player).etfu$isCrouching();
		}
		return player instanceof IPosePlayer && ((IPosePlayer) player).etfu$getPose() == PlayerPose.CROUCHING;
	}

	/**
	 * Sneaking stops at drops deeper than this (§7.2). Modern ties it to the step height (0.6); 1.7.10 players step
	 * 0.5, so it's a constant here.
	 */
	public static final double SNEAK_EDGE_DROP = 0.6D;
	private static final double EDGE_STEP = 0.05D;

	/**
	 * Sneaking at an edge (§7.2): shrinks the horizontal movement in 0.05 steps until there is ground within 0.6
	 * below, first for x alone, then z alone, then both together. Returns {x, z}.
	 */
	public static double[] backOffFromEdge(EntityPlayer player, double x, double y, double z) {
		if (player.capabilities.isFlying || y > 0.0D || !player.isSneaking() || !isAboveGround(player)) {
			return new double[]{x, z};
		}
		double stepX = Math.signum(x) * EDGE_STEP;
		double stepZ = Math.signum(z) * EDGE_STEP;

		while (x != 0.0D && canFallAtLeast(player, x, 0.0D, SNEAK_EDGE_DROP)) {
			if (Math.abs(x) <= EDGE_STEP) {
				x = 0.0D;
				break;
			}
			x -= stepX;
		}
		while (z != 0.0D && canFallAtLeast(player, 0.0D, z, SNEAK_EDGE_DROP)) {
			if (Math.abs(z) <= EDGE_STEP) {
				z = 0.0D;
				break;
			}
			z -= stepZ;
		}
		while (x != 0.0D && z != 0.0D && canFallAtLeast(player, x, z, SNEAK_EDGE_DROP)) {
			x = Math.abs(x) <= EDGE_STEP ? 0.0D : x - stepX;
			z = Math.abs(z) <= EDGE_STEP ? 0.0D : z - stepZ;
		}
		return new double[]{x, z};
	}

	/**
	 * On the ground, or only just left it: fallen less than the edge drop and there's ground within the rest of it.
	 */
	private static boolean isAboveGround(EntityPlayer player) {
		return player.onGround || player.fallDistance < SNEAK_EDGE_DROP
				&& !canFallAtLeast(player, 0.0D, 0.0D, SNEAK_EDGE_DROP - player.fallDistance);
	}

	/**
	 * Whether the slab of space below the hitbox, moved by (x, z) and as deep as the given height, is empty.
	 */
	private static boolean canFallAtLeast(EntityPlayer player, double x, double z, double height) {
		AxisAlignedBB box = player.boundingBox;
		AxisAlignedBB below = AxisAlignedBB.getBoundingBox(
				box.minX + 1.0E-7D + x, box.minY - height - 1.0E-7D, box.minZ + 1.0E-7D + z,
				box.maxX - 1.0E-7D + x, box.minY, box.maxZ - 1.0E-7D + z
		);
		return player.worldObj.getCollidingBoundingBoxes(player, below).isEmpty();
	}

	/**
	 * Blends from one angle (radians) towards another by the given amount, going the short way round.
	 */
	public static float lerpAngle(float amount, float from, float to) {
		float difference = to - from;
		while (difference < (float) -Math.PI) {
			difference += (float) (Math.PI * 2.0D);
		}
		while (difference >= (float) Math.PI) {
			difference -= (float) (Math.PI * 2.0D);
		}
		return from + amount * difference;
	}

	/**
	 * The eye height the camera should move towards for this entity (§13.1): the applied pose's eye height for
	 * players using the pose system, standing height otherwise.
	 */
	public static float getTargetCameraEyeHeight(Object entity) {
		if (entity instanceof IPosePlayer) {
			PlayerPose pose = ((IPosePlayer) entity).etfu$getAppliedPose();
			if (pose != null) {
				return pose.eyeHeight;
			}
		}
		return PlayerPose.STANDING_EYE_HEIGHT;
	}
}
