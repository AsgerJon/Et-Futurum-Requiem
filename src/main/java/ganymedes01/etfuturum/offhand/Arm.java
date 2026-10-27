package ganymedes01.etfuturum.offhand;

/** A player's arms, as the model and the renderers see them (SPEC-modern-movement.md §37). */
public enum Arm {
	LEFT,
	RIGHT;

	public Arm opposite() {
		return this == LEFT ? RIGHT : LEFT;
	}
}
