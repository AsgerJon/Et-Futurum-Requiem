package ganymedes01.etfuturum.movement;

/**
 * A living entity that takes bubble column pushes after its movement step, as in modern (§28).
 */
public interface IBubbleColumnEntity {

	/**
	 * Records one column cell's effect on this entity for this tick, to be applied after the movement step.
	 */
	void etfu$queueBubbleColumn(boolean drag, boolean above);
}
