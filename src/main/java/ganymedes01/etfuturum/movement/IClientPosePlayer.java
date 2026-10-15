package ganymedes01.etfuturum.movement;

/**
 * Client-only state of the local player.
 */
public interface IClientPosePlayer {

	/**
	 * The local player's own crouching flag (§6.1). Unlike the pose, it is also on while shift is held where crouching
	 * fits, and it is updated before the movement input is read.
	 */
	boolean etfu$isCrouching();

	/**
	 * Crouching or crawling: the movement input is slowed to the sneaking speed (§6.2).
	 */
	boolean etfu$isMovingSlowly();

	/**
	 * How far the eyes have adjusted to being under water, 0 to 1: 60% over the first 5 seconds, the rest by 30
	 * seconds (§6.4). 0 while the eyes are out of the water.
	 */
	float etfu$getWaterVision();
}
