package ganymedes01.etfuturum.movement;

/**
 * Whether the first-person arm is being drawn (RenderPlayer.renderFirstPersonArm, for either hand). Modern draws
 * that arm in its reset pose (AvatarRenderer.renderHand: arm.resetPose()), so the pose animations of §14 (crouch
 * offsets, the swimming stroke) must not reach it: in 1.7.10 the same shared model is posed by setRotationAngles for
 * both, and the swimming stroke swept the first-person arm out of view.
 */
public final class FirstPersonArm {

	public static boolean rendering;

	private FirstPersonArm() {
	}
}
