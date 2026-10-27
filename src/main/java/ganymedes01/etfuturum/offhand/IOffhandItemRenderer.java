package ganymedes01.etfuturum.offhand;

/** The first-person renderer's off-hand side (SPEC-modern-movement.md §36.5). */
public interface IOffhandItemRenderer {
	/** The off-hand's raise height drops to 0, as after using an item that consumes it (modern's itemUsed). */
	void etfu$resetOffhandEquippedProgress();

	/** The hand being drawn in first person: the off-hand during its pass, the main hand otherwise. */
	Hand etfu$drawnHand();
}
