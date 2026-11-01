package ganymedes01.etfuturum.unittas.tests;

import ganymedes01.etfuturum.unittas.TasTestCase;
import net.minecraft.client.renderer.ActiveRenderInfo;

/**
 * The first-person camera at the eyes (SPEC-modern-movement.md §53.5). 1.7.10 works out, each frame, the middle of the
 * near plane (0.05 in front of the camera) relative to the player's eyes: 0.05 ahead of them with the camera at the
 * eyes, as 26.3, and 0.05 behind them with 1.7.10's camera 0.1 back. The picture is checked by ./unittas-compare.sh's
 * calibration.first_person.
 */
public class CameraTests extends TasTestCase {

	private boolean viewBobbing;

	@Override
	protected void setUp() {
		viewBobbing = onClient(() -> mc().gameSettings.viewBobbing);
		onClient(() -> {
			mc().gameSettings.viewBobbing = false;
		});
	}

	@Override
	protected void tearDown() {
		onClient(() -> {
			mc().gameSettings.viewBobbing = viewBobbing;
		});
	}

	public void testCameraAtTheEyes() {
		// south: +z
		look(0.0F, 0.0F);
		ticks(5);
		assertAlmostEqual(0.05D, onClient(() -> (double) ActiveRenderInfo.objectZ), 0.02D);
		assertAlmostEqual(0.0D, onClient(() -> (double) ActiveRenderInfo.objectX), 0.02D);
		// east: +x
		look(-90.0F, 0.0F);
		ticks(5);
		assertAlmostEqual(0.05D, onClient(() -> (double) ActiveRenderInfo.objectX), 0.02D);
		assertAlmostEqual(0.0D, onClient(() -> (double) ActiveRenderInfo.objectZ), 0.02D);
	}
}
