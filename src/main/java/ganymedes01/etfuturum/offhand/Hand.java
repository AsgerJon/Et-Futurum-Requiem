package ganymedes01.etfuturum.offhand;

/** Which hand an action or an ongoing item use belongs to (SPEC-modern-movement.md §36.2). */
public enum Hand {
	MAIN,
	OFF;

	/** The arm this hand is on: the main hand is on the main arm, the off-hand on the other (§37). */
	public Arm arm(boolean leftHanded) {
		return (this == MAIN) == leftHanded ? Arm.LEFT : Arm.RIGHT;
	}
}
