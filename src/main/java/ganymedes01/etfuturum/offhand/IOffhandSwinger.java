package ganymedes01.etfuturum.offhand;

/**
 * The hand a swing belongs to (SPEC-modern-movement.md §36.4), on every living entity: 1.7.10 has one swing
 * animation per entity, so the hand is remembered beside it when the swing (re)starts.
 */
public interface IOffhandSwinger {
	/** The hand of the current swing, or of the last one once it is over. */
	Hand etfu$getSwingingHand();

	/** The hand the next swingItem call is for; back to the main hand once that call is over. */
	Hand etfu$getNextSwingHand();

	void etfu$setNextSwingHand(Hand hand);
}
