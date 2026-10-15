package ganymedes01.etfuturum.movement;

/**
 * The player poses that change the hitbox, as in modern versions. Width is always 0.6.
 * Crawling has no pose of its own: it is the swimming pose while not touching water.
 */
public enum PlayerPose {
	STANDING(1.8F, 1.62F),
	CROUCHING(1.5F, 1.27F),
	SWIMMING(0.6F, 0.4F),
	FALL_FLYING(0.6F, 0.4F);

	public static final float STANDING_EYE_HEIGHT = 1.62F;
	private static final PlayerPose[] VALUES = values();

	public final float height;
	public final float eyeHeight;

	PlayerPose(float height, float eyeHeight) {
		this.height = height;
		this.eyeHeight = eyeHeight;
	}

	/**
	 * How far this pose moves the eyes from standing height. 1.7.10 players have different default eye heights per
	 * class (0.12, 1.62, 1.82), so poses are applied as an offset, never as an absolute eye height.
	 */
	public float eyeHeightOffset() {
		return eyeHeight - STANDING_EYE_HEIGHT;
	}

	public static PlayerPose byId(int id) {
		return id >= 0 && id < VALUES.length ? VALUES[id] : STANDING;
	}
}
