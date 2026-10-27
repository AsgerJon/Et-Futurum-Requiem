package ganymedes01.etfuturum.movement;

/**
 * The client's EntityRenderer with the smoothed camera eye height (§13.1), for the camera-in-water test (§27).
 */
public interface IClientCamera {

	/**
	 * The camera's eye height above the feet for this frame: smoothed per tick, interpolated by the partial ticks.
	 */
	float etfu$getCameraEyeHeight(float partialTicks);
}
