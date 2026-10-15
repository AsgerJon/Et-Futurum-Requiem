package ganymedes01.etfuturum.movement;

public interface IPosePlayer {

	/**
	 * The pose as synced through the DataWatcher. For the local player and on the server this is chosen every tick;
	 * for other players it is whatever the server sent.
	 */
	PlayerPose etfu$getPose();

	/**
	 * The pose whose hitbox is currently applied, or null while the pose system is standing aside (sleeping, dead,
	 * gliding with the old elytra code, or another mod has resized the player).
	 */
	PlayerPose etfu$getAppliedPose();

	/**
	 * How far the body is tilted into the swimming/crawling position, 0 to 1, interpolated between ticks (§11 step 7).
	 */
	float etfu$getSwimAmount(float partialTicks);

	/**
	 * The swimming flag (§5): doing the swim movement. Not the same as the swimming pose, which is also used for
	 * crawling. Synced together with the pose.
	 */
	boolean etfu$isSwimming();

	void etfu$setSwimming(boolean swimming);

	/**
	 * Stores the result of this tick's water scan (§9.2). The previous eyes-in-water value is kept, since starting to
	 * swim checks last tick's value like modern does.
	 */
	void etfu$setWaterState(double waterHeight, boolean eyesInWater);

	/**
	 * How far the water reaches above the feet, 0 when not touching water.
	 */
	double etfu$getWaterHeight();

	boolean etfu$isEyeInWater();

	boolean etfu$wasEyeInWater();
}
